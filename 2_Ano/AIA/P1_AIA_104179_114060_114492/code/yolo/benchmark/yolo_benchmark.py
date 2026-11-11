"""Plot the four YOLO models using the saved full Kaggle benchmark.

Outputs: five benchmark SVGs and the recorded 100-epoch learning-rate schedule.
No models are trained or run during plot generation.
"""

import argparse
import csv
import json
from pathlib import Path
import sys

import numpy as np

PROJECT_ROOT = Path(__file__).resolve().parents[2]
if __package__ in (None, ""):
    sys.path.insert(0, str(PROJECT_ROOT))

from face_comparison import match_detections, pairwise_iou
from yolo_models import comparison_model_specs, file_sha256

RESULTS_DIR = PROJECT_ROOT / "runs/kaggle_face_comparison"
OUTPUT_DIR = RESULTS_DIR
TRAINING_CSV = PROJECT_ROOT / "runs/yolo_training_100_epochs/results.csv"
MODEL_SPECS = comparison_model_specs(
    PROJECT_ROOT, fine_weights=PROJECT_ROOT / "weights/yolo26n_wider_100ep_best.pt",
)
MODEL_LABELS = {
    "yolo_base": "YOLO26n base",
    "yolo_finetuned": "YOLO26n fine tuned (100 epochs)",
    "yolov8_face": "YOLOv8n-Face",
    "yolo26_face": "YOLO26n-Face",
}
COLORS = ["crimson", "forestgreen", "royalblue", "darkorange"]


def compute_iou(box1, box2):
    """Return the IoU of two xyxy boxes."""
    return float(pairwise_iou([box1], [box2])[0, 0])


def calculate_ap(all_detections, all_scores, all_ground_truths, iou_threshold=0.5):
    """Re-score raw predictions with the benchmark's 101-point AP protocol."""
    if not (len(all_detections) == len(all_scores) == len(all_ground_truths)):
        raise ValueError("Prediction, confidence and annotation lists must have equal lengths.")
    if not 0 < iou_threshold <= 1:
        raise ValueError("The IoU threshold must be in (0, 1].")
    correct, confidences, face_count = [], [], 0
    for boxes, scores, targets in zip(all_detections, all_scores, all_ground_truths):
        boxes = np.asarray(boxes, dtype=np.float32).reshape(-1, 4)
        scores = np.asarray(scores, dtype=np.float32).reshape(-1)
        targets = np.asarray(targets, dtype=np.float32).reshape(-1, 4)
        if (len(boxes) != len(scores) or not np.isfinite(boxes).all()
                or not np.isfinite(scores).all() or not np.isfinite(targets).all()
                or (scores < 0).any() or (scores > 1).any()
                or (targets[:, 2:] <= targets[:, :2]).any()):
            raise ValueError("Invalid boxes or confidence scores.")
        correct.append(match_detections(boxes, scores, targets, thresholds=[iou_threshold]))
        confidences.append(scores)
        face_count += len(targets)
    if not face_count:
        raise ValueError("AP is undefined without annotated faces.")
    recall_axis = np.linspace(0, 1, 1000)
    if not sum(len(scores) for scores in confidences):
        return 0.0, np.zeros_like(recall_axis), recall_axis
    from ultralytics.utils.metrics import ap_per_class

    scores = np.concatenate(confidences)
    metrics = ap_per_class(
        np.concatenate(correct), scores, np.zeros(len(scores)), np.zeros(face_count),
        plot=False, names={0: "face"},
    )
    return float(metrics[5][0, 0]), metrics[11][0], metrics[10]


def load_benchmark_results(run_dir=RESULTS_DIR):
    """Read AP, PR curves and every raw network-latency sample from the same run."""
    run_dir = Path(run_dir)
    manifest = json.loads((run_dir / "manifest.json").read_text(encoding="utf-8"))
    dataset = manifest["dataset"]
    models = {model["key"]: model for model in manifest["models"]}
    if (manifest.get("training_performed") is not False
            or not manifest.get("evaluation_complete") or not manifest.get("prediction_complete")
            or len(manifest["models"]) != 4
            or set(models) != {spec["key"] for spec in MODEL_SPECS}
            or not all(model.get("prediction_complete") for model in models.values())
            or dataset["split"] != "all" or dataset["selected_splits"] != ["train", "val"]
            or dataset["limit"] is not None or manifest["precision"] != "FP32"
            or manifest["predict_kwargs"]["batch"] != 1):
        raise ValueError("A complete four-model Kaggle train + val evaluation is required.")
    names = (run_dir / "evaluation_images.txt").read_text(encoding="utf-8").splitlines()
    if (len(names) != dataset["images"] or len(set(names)) != len(names)
            or {name.split("/", 1)[0] for name in names} != {"train", "val"}):
        raise ValueError("The image list does not match the full Kaggle evaluation.")
    with (run_dir / "comparison.csv").open(encoding="utf-8-sig", newline="") as stream:
        rows = list(csv.DictReader(stream))
    by_name = {row["model"]: row for row in rows}
    if len(rows) != 4 or set(by_name) != {model["label"] for model in models.values()}:
        raise ValueError("The summary must contain exactly the four evaluated YOLO models.")
    results = {}
    with np.load(run_dir / "curves.npz", allow_pickle=False) as curves:
        for spec in MODEL_SPECS:
            key = spec["key"]
            model = models[key]
            row = by_name[model["label"]]
            if (model["class_name"] != spec["class_name"]
                    or file_sha256(spec["weights"]) != model["sha256"]
                    or (key == "yolo_finetuned" and model["completed_training_epochs"] != 100)
                    or int(row["images"]) != dataset["images"]
                    or int(row["faces"]) != dataset["faces"]):
                raise ValueError(f"Model or dataset mismatch: {key}")
            ap = float(row["ap50"])
            recall, precision = curves[key + "_recall"], curves[key + "_precision"]
            if (not np.isfinite(ap) or not 0 <= ap <= 1 or recall.ndim != 1
                    or recall.shape != precision.shape or len(recall) < 2
                    or not np.isfinite(recall).all() or not np.isfinite(precision).all()
                    or (np.diff(recall) < 0).any() or (recall < 0).any() or (recall > 1).any()
                    or (precision < 0).any() or (precision > 1).any()):
                raise ValueError(f"Invalid AP or precision-recall curve: {key}")
            with (run_dir / f"{key}_timing.csv").open(encoding="utf-8", newline="") as stream:
                timings = list(csv.DictReader(stream))
            if [item["image"] for item in timings] != names:
                raise ValueError(f"Incomplete or mismatched timing samples: {key}")
            times = np.array([float(item["inference_ms"]) for item in timings])
            if (not np.isfinite(times).all() or (times < 0).any() or times.mean() <= 0
                    or not np.isclose(times.mean(), float(row["mean_inference_ms"]))
                    or not np.isclose(times.mean(), model["timing"]["mean_inference_ms"])):
                raise ValueError(f"Invalid network-inference times: {key}")
            results[MODEL_LABELS[key]] = dict(
                key=key, ap=ap, precisions=precision.copy(), recalls=recall.copy(),
                inference_times=times, avg_time_ms=float(times.mean()),
            )
    return results, manifest


def generate_plots(results, output_dir=OUTPUT_DIR, preview_dir=None, output_format="svg"):
    """Generate five vector plots using the template's white-grid, 8 x 6 layout."""
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt

    if output_format not in {"svg", "pdf"}:
        raise ValueError("The output format must be svg or pdf.")
    if list(results) != list(MODEL_LABELS.values()):
        raise ValueError("All plots require the four YOLO models in the benchmark order.")
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    if preview_dir is not None:
        preview_dir = Path(preview_dir)
        preview_dir.mkdir(parents=True, exist_ok=True)
    paths = []

    def save(figure, filename):
        path = output_dir / Path(filename).with_suffix(f".{output_format}")
        figure.savefig(path, format=output_format)
        if preview_dir is not None:
            figure.savefig(preview_dir / path.with_suffix(".png").name, dpi=160)
        plt.close(figure)
        paths.append(path)

    # Matplotlib's built-in style matches the template without requiring Seaborn.
    with plt.style.context("seaborn-v0_8-whitegrid"), plt.rc_context({
            "font.family": "DejaVu Sans", "font.size": 11, "svg.fonttype": "none", "pdf.fonttype": 42}):
        figure, axis = plt.subplots(figsize=(8, 6))
        for (name, result), color in zip(results.items(), COLORS):
            axis.plot(result["recalls"], result["precisions"],
                      label=f"{name} (AP={result['ap']:.3f})", color=color, linewidth=2.5)
        axis.set_xlabel("Recall", fontsize=12)
        axis.set_ylabel("Precision", fontsize=12)
        axis.set_title("Precision-Recall Curves (IoU=0.5)", fontsize=14, fontweight="bold")
        axis.set_xlim(0, 1)
        axis.set_ylim(0, 1.02)
        axis.legend(fontsize=11, loc="lower left")
        figure.tight_layout()
        save(figure, "precision_recall_curve.svg")

        figure, axis = plt.subplots(figsize=(8, 6))
        distributions = [result["inference_times"] for result in results.values()]
        tick_labels = ["YOLO26n base", "YOLO26n\nfine tuned\n(100 epochs)",
                       "YOLOv8n-Face", "YOLO26n-Face"]
        # Keep every sample and all outliers; the centre line is the median.
        boxes = axis.boxplot(distributions, widths=0.4, patch_artist=True,
                             tick_labels=tick_labels,
                             flierprops={"markersize": 2.5, "alpha": 0.45},
                             medianprops={"color": "0.2", "linewidth": 1.5})
        for patch, color in zip(boxes["boxes"], COLORS):
            patch.set_facecolor(color)
            patch.set_alpha(0.85)
        axis.tick_params(axis="x", labelsize=11)
        axis.set_ylabel("Time per Image (ms)", fontsize=12)
        axis.set_title("Inference Latency Distribution (ms)", fontsize=14, fontweight="bold")
        figure.tight_layout()
        save(figure, "inference_latency_boxplot.svg")

        figure, axis = plt.subplots(figsize=(8, 6))
        positions = np.arange(len(results))
        ap_values = [100 * result["ap"] for result in results.values()]
        bars = axis.bar(positions, ap_values, color=COLORS, width=0.4,
                        edgecolor="0.2", alpha=0.85)
        axis.set_xticks(positions, tick_labels, fontsize=11)
        axis.set_ylabel("mAP50 / AP50 (%)", fontsize=12)
        axis.set_ylim(0, 100)
        axis.set_title("Average Precision Comparison (IoU=0.5)",
                       fontsize=14, fontweight="bold")
        axis.bar_label(bars, fmt="%.2f%%", padding=3, fontsize=11)
        figure.tight_layout()
        save(figure, "ap_comparison.svg")

        figure, axis = plt.subplots(figsize=(8, 6))
        mean_times = [result["avg_time_ms"] for result in results.values()]
        bars = axis.bar(positions, mean_times, color=COLORS, width=0.4,
                        edgecolor="0.2", alpha=0.85)
        axis.set_xticks(positions, tick_labels, fontsize=11)
        axis.set_ylabel("Mean Inference Time (ms)", fontsize=12)
        axis.set_ylim(0, max(mean_times) * 1.18)
        axis.set_title("Inference Time per Image (ms)", fontsize=14, fontweight="bold")
        axis.bar_label(bars, fmt="%.2f ms", padding=3, fontsize=11)
        figure.tight_layout()
        save(figure, "inference_time_comparison.svg")

        figure, axis = plt.subplots(figsize=(8, 6))
        markers = ["o", "s", "^", "D"]
        for (name, result), color, marker in zip(results.items(), COLORS, markers):
            fps = 1000 / result["avg_time_ms"]
            axis.scatter(fps, 100 * result["ap"], color=color, marker=marker,
                         s=100, label=name)
            axis.annotate(f"{fps:.2f} FPS", (fps, 100 * result["ap"]),
                          xytext=(0, 9), textcoords="offset points", ha="center", fontsize=10)
        axis.set_xlabel("Network Inference Speed (FPS)", fontsize=12)
        axis.set_ylabel("mAP50 / AP50 (%)", fontsize=12)
        axis.set_ylim(-2, 100)
        axis.margins(x=0.15)
        axis.set_title("AP50 vs. Inference Speed", fontsize=14, fontweight="bold")
        axis.legend(fontsize=11, loc="center left")
        figure.tight_layout()
        save(figure, "fps_vs_ap.svg")
    return paths


def plot_learning_rate(training_csv=TRAINING_CSV, output_dir=OUTPUT_DIR, preview_dir=None,
                       output_format="svg"):
    """Plot recorded epoch-end learning rates, combining identical parameter groups."""
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt

    if output_format not in {"svg", "pdf"}:
        raise ValueError("The output format must be svg or pdf.")
    training_csv = Path(training_csv)
    with training_csv.open(encoding="utf-8-sig", newline="") as stream:
        reader = csv.DictReader(stream)
        columns = sorted(
            (name for name in reader.fieldnames or []
             if name.startswith("lr/pg") and name[5:].isdigit()),
            key=lambda name: int(name[5:]),
        )
        rows = list(reader)
    epochs = np.array([int(row["epoch"]) for row in rows])
    if not np.array_equal(epochs, np.arange(1, 101)) or not columns:
        raise ValueError("The training CSV must contain epochs 1-100 and learning-rate columns.")
    groups = []
    for column in columns:
        values = np.array([float(row[column]) for row in rows])
        if not np.isfinite(values).all() or (values < 0).any():
            raise ValueError(f"Invalid learning-rate values: {column}")
        for group in groups:
            if np.array_equal(values, group["values"]):
                group["ids"].append(column[5:])
                break
        else:
            groups.append(dict(ids=[column[5:]], values=values))
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    path = output_dir / f"learning_rate_100_epochs.{output_format}"
    with plt.style.context("seaborn-v0_8-whitegrid"), plt.rc_context({
            "font.family": "DejaVu Sans", "font.size": 11, "svg.fonttype": "none", "pdf.fonttype": 42}):
        figure, axis = plt.subplots(figsize=(8, 6))
        curve_colors = ["forestgreen", "royalblue", "crimson", "darkorange"]
        for index, group in enumerate(groups):
            label = "Parameter groups " + ", ".join(group["ids"])
            axis.plot(epochs, group["values"], linewidth=2.5, label=label,
                      color=curve_colors[index % len(curve_colors)])
        axis.set_xlabel("Epoch", fontsize=12)
        axis.set_ylabel("Learning Rate", fontsize=12)
        axis.set_title("Learning Rate Schedule (100 Epochs)", fontsize=14, fontweight="bold")
        axis.set_xlim(1, 100)
        axis.set_xticks([1, 20, 40, 60, 80, 100])
        axis.set_ylim(bottom=0)
        axis.legend(title="YOLO26n fine tuned", fontsize=11, title_fontsize=11)
        figure.tight_layout()
        figure.savefig(path, format=output_format, metadata={
            "Title": "YOLO26n fine tuned - Learning Rate Schedule (100 Epochs)",
            "Description" if output_format == "svg" else "Subject":
                           "Recorded epoch-end learning rates; identical parameter groups are combined. "
                           + f"Source: {training_csv.resolve()}; SHA-256: {file_sha256(training_csv)}",
        })
        if preview_dir is not None:
            preview_dir = Path(preview_dir)
            preview_dir.mkdir(parents=True, exist_ok=True)
            figure.savefig(preview_dir / path.with_suffix(".png").name, dpi=160)
        plt.close(figure)
    return path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--run-dir", type=Path, default=RESULTS_DIR,
                        help="Saved full Kaggle evaluation (train + val).")
    parser.add_argument("--output-dir", type=Path,
                        help="Plot and summary directory; defaults to the selected run.")
    parser.add_argument("--training-csv", type=Path, default=TRAINING_CSV,
                        help="Recorded results of the 100-epoch fine tuned training run.")
    parser.add_argument("--preview-dir", type=Path,
                        help="Optional directory for PNG previews of the SVG plots.")
    args = parser.parse_args()
    args.output_dir = args.output_dir or args.run_dir
    print(f"Using saved YOLO results: {args.run_dir.resolve()}")
    results, manifest = load_benchmark_results(args.run_dir)
    paths = generate_plots(results, args.output_dir, args.preview_dir)
    paths.append(plot_learning_rate(args.training_csv, args.output_dir, args.preview_dir))
    rows = []
    for name, result in results.items():
        rows.append(dict(
            model=name, images=manifest["dataset"]["images"], faces=manifest["dataset"]["faces"],
            ap50=result["ap"], mean_inference_ms=result["avg_time_ms"],
            fps_inference=1000 / result["avg_time_ms"],
            median_inference_ms=float(np.median(result["inference_times"])),
            p95_inference_ms=float(np.percentile(result["inference_times"], 95)),
        ))
        print(f"{name} -> AP@0.5 (mAP@0.5): {result['ap']:.4f} | "
              f"Mean inference time: {result['avg_time_ms']:.2f} ms | "
              f"Samples: {len(result['inference_times'])}")
    with (args.output_dir / "benchmark_results.csv").open("w", encoding="utf-8-sig", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)
    source_files = ["manifest.json", "comparison.csv", "curves.npz", "evaluation_images.txt"]
    source_files += [f"{spec['key']}_timing.csv" for spec in MODEL_SPECS]
    metadata = dict(
        source_run=str(args.run_dir.resolve()), images=manifest["dataset"]["images"],
        faces=manifest["dataset"]["faces"], training_performed=False, inference_repeated=False,
        hardware=manifest["hardware"], precision=manifest["precision"], batch=1,
        timing_basis="Network inference; all samples and outliers are included.",
        fps_basis="1000 / mean network-inference time in ms; image I/O is excluded.",
        boxplot="Median, quartiles and whiskers at 1.5 IQR; all outliers are shown.",
        learning_rate_training_source=dict(
            csv=str(args.training_csv.resolve()), sha256=file_sha256(args.training_csv), epochs=100,
        ),
        source_files_sha256={name: file_sha256(args.run_dir / name) for name in source_files},
        models=[dict(key=result["key"], label=name,
                     weights_sha256=next(model["sha256"] for model in manifest["models"]
                                         if model["key"] == result["key"]))
                for name, result in results.items()],
    )
    (args.output_dir / "source.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8",
    )
    for path in paths:
        print(f"SVG plot saved to: {path.resolve()}")


if __name__ == "__main__":
    main()
