"""Common, single-class face detection evaluation for the comparison notebook.

Only valid, positive WIDER FACE boxes are scored. Invalid boxes are excluded,
not treated as ignore regions. These are custom AP@IoU metrics, not the official
WIDER FACE Easy/Medium/Hard protocol. Every model must receive the same images
and predict settings; the COCO baseline must select only its person class.
"""

from pathlib import Path
from time import perf_counter

import numpy as np
from PIL import Image


IOU_THRESHOLDS = np.linspace(0.50, 0.95, 10)


def read_wider_annotations(annotation_file):
    """Yield (relative image name, valid positive xywh boxes) from WIDER TXT."""
    lines = Path(annotation_file).read_text(encoding="utf-8").splitlines()
    index = 0
    while index < len(lines):
        image_name = lines[index].strip()
        index += 1
        if not image_name:
            continue
        if index >= len(lines):
            raise ValueError(f"Missing face count after {image_name}")
        try:
            face_count = int(lines[index].strip())
        except ValueError as exc:
            raise ValueError(f"Invalid face count after {image_name}") from exc
        index += 1
        if face_count < 0:
            raise ValueError(f"Negative face count after {image_name}")
        boxes = []
        for _ in range(face_count):
            if index >= len(lines):
                raise ValueError(f"Missing box annotation for {image_name}")
            try:
                values = list(map(int, lines[index].split()))
            except ValueError as exc:
                raise ValueError(f"Invalid box annotation for {image_name}") from exc
            index += 1
            if len(values) < 8:
                raise ValueError(f"Incomplete box annotation for {image_name}")
            x, y, width, height = values[:4]
            if values[7] == 0 and width > 0 and height > 0:
                boxes.append((x, y, width, height))
        if face_count == 0 and index < len(lines):
            # Some WIDER releases store one all-zero dummy row for empty images.
            parts = lines[index].split()
            if len(parts) >= 4:
                try:
                    dummy = list(map(int, parts))
                except ValueError:
                    dummy = []
                if dummy and all(value == 0 for value in dummy):
                    index += 1
        yield image_name.replace("\\", "/"), boxes


def clipped_boxes(boxes, width, height):
    """Convert xywh to clipped xyxy, dropping boxes with no visible area."""
    if width <= 0 or height <= 0:
        raise ValueError("Image dimensions must be positive")
    values = np.asarray(boxes, dtype=np.float32).reshape(-1, 4)
    if not len(values):
        return np.empty((0, 4), dtype=np.float32)
    values = values[np.isfinite(values).all(axis=1) & (values[:, 2:] > 0).all(axis=1)]
    corners = values.copy()
    corners[:, 2:] += corners[:, :2]
    corners[:, [0, 2]] = np.clip(corners[:, [0, 2]], 0, width)
    corners[:, [1, 3]] = np.clip(corners[:, [1, 3]], 0, height)
    return corners[(corners[:, 2:] > corners[:, :2]).all(axis=1)]


def prepare_yolo_split(image_root, annotation_file, labels_root):
    """Write matching face-only YOLO labels, including empty label files."""
    image_root, labels_root = Path(image_root), Path(labels_root)
    stats = {"images": 0, "faces": 0, "dropped_boxes": 0}
    for image_name, boxes in read_wider_annotations(annotation_file):
        with Image.open(image_root / image_name) as image:
            width, height = image.size
        corners = clipped_boxes(boxes, width, height)
        normalized = np.empty_like(corners)
        normalized[:, :2] = (corners[:, :2] + corners[:, 2:]) / 2
        normalized[:, 2:] = corners[:, 2:] - corners[:, :2]
        normalized /= np.array([width, height, width, height], dtype=np.float32)
        destination = labels_root / Path(image_name).with_suffix(".txt")
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(
            "\n".join("0 " + " ".join(f"{value:.8f}" for value in box) for box in normalized),
            encoding="utf-8",
        )
        stats["images"] += 1
        stats["faces"] += len(corners)
        stats["dropped_boxes"] += len(boxes) - len(corners)
    # Ultralytics may otherwise reuse labels generated with an earlier policy.
    cache_file = labels_root.with_suffix(".cache")
    cache_file.unlink(missing_ok=True)
    return stats


def pairwise_iou(targets, predictions):
    """Return the (ground truth, predictions) IoU matrix for xyxy boxes."""
    targets = np.asarray(targets, dtype=np.float32).reshape(-1, 4)
    predictions = np.asarray(predictions, dtype=np.float32).reshape(-1, 4)
    intersection = np.maximum(
        np.minimum(targets[:, None, 2:], predictions[None, :, 2:])
        - np.maximum(targets[:, None, :2], predictions[None, :, :2]),
        0,
    ).prod(axis=2)
    target_area = np.maximum(targets[:, 2:] - targets[:, :2], 0).prod(axis=1)
    prediction_area = np.maximum(predictions[:, 2:] - predictions[:, :2], 0).prod(axis=1)
    union = target_area[:, None] + prediction_area[None, :] - intersection
    return np.divide(intersection, union, out=np.zeros_like(intersection), where=union > 0)


def match_detections(predictions, confidences, targets, thresholds=IOU_THRESHOLDS):
    """Match in descending confidence order, with one target per detection.

    Return correctness in the original prediction order, independently for each
    IoU threshold. Duplicate detections are false positives after the first match.
    """
    predictions = np.asarray(predictions, dtype=np.float32).reshape(-1, 4)
    confidences = np.asarray(confidences, dtype=np.float32).reshape(-1)
    targets = np.asarray(targets, dtype=np.float32).reshape(-1, 4)
    thresholds = np.asarray(thresholds, dtype=np.float64).reshape(-1)
    if len(confidences) != len(predictions):
        raise ValueError("One confidence is required for each prediction")
    correct = np.zeros((len(predictions), len(thresholds)), dtype=bool)
    if not len(predictions) or not len(targets):
        return correct
    overlaps = pairwise_iou(targets, predictions)
    matched = np.zeros((len(targets), len(thresholds)), dtype=bool)
    for prediction_index in np.argsort(-confidences, kind="stable"):
        available = np.where(matched, -1.0, overlaps[:, prediction_index, None])
        best_target = available.argmax(axis=0)
        columns = np.arange(len(thresholds))
        valid = available[best_target, columns] >= thresholds
        correct[prediction_index] = valid
        matched[best_target[valid], columns[valid]] = True
    return correct


def summarize_detections(correct, confidences, face_count, operating_conf=0.25):
    """AP over low-confidence predictions and counts at a fixed operating point."""
    # Keep the dataset/geometry utilities importable without torch/Ultralytics.
    from ultralytics.utils.metrics import ap_per_class

    correct = np.asarray(correct, dtype=bool).reshape(-1, len(IOU_THRESHOLDS))
    confidences = np.asarray(confidences, dtype=np.float32).reshape(-1)
    if len(correct) != len(confidences):
        raise ValueError("One confidence is required for each detection")
    accepted = confidences >= operating_conf
    true_positives = int(correct[accepted, 0].sum())
    false_positives = int(accepted.sum()) - true_positives
    false_negatives = int(face_count) - true_positives
    precision = true_positives / max(true_positives + false_positives, 1)
    recall = true_positives / max(face_count, 1)
    f1 = 2 * precision * recall / max(precision + recall, np.finfo(float).eps)
    axis = np.linspace(0, 1, 1000)
    curves = {"recall": axis, "precision": np.zeros_like(axis), "confidence": axis, "f1": np.zeros_like(axis)}
    map50 = map50_95 = 0.0
    if face_count:
        values = ap_per_class(
            correct, confidences, np.zeros(len(confidences)), np.zeros(face_count),
            plot=False, names={0: "face"},
        )
        ap = values[5]
        map50, map50_95 = float(ap[0, 0]), float(ap[0].mean())
        curves = {"recall": values[10], "precision": values[11][0], "confidence": values[10], "f1": values[9][0]}
    metrics = {
        "precision": precision, "recall": recall, "f1": f1,
        "map50": map50, "map50_95": map50_95,
        "tp": true_positives, "fp": false_positives, "fn": false_negatives,
        "faces": int(face_count), "detections": len(confidences),
        "operating_conf": operating_conf, "match_iou": 0.5,
    }
    return metrics, curves


def evaluate_model(
    model, image_root, annotation_file, image_names, predict_kwargs,
    operating_conf=0.25, progress_every=100,
):
    """Score a model using the shared subset and inference configuration.

    Three warmups precede measurement. Latency includes preprocessing, model
    inference and postprocessing in predict(), with batch size one. Disk decode,
    ground-truth loading, matching and device-to-host result extraction are
    excluded. Returned FPS is 1000 / mean_ms, not full application throughput.
    """
    import torch

    names = list(image_names)
    if not names:
        raise ValueError("Evaluation image list is empty")
    if len(set(names)) != len(names):
        raise ValueError("Evaluation image list contains duplicates")
    annotations = dict(read_wider_annotations(annotation_file))
    missing = set(names) - annotations.keys()
    if missing:
        raise ValueError(f"Images absent from annotations: {sorted(missing)[:3]}")
    kwargs = dict(predict_kwargs)
    kwargs.setdefault("conf", 0.001)
    kwargs.setdefault("rect", False)
    kwargs.setdefault("quantize", None)  # Float32; avoids deprecated half=False warnings.
    kwargs.setdefault("verbose", False)
    kwargs.setdefault("save", False)
    if kwargs["conf"] > operating_conf:
        raise ValueError("Prediction confidence must not exceed operating confidence")
    if kwargs.get("stream", False):
        raise ValueError("Streaming prediction cannot be used for synchronized timing")
    if kwargs.get("batch", 1) != 1:
        raise ValueError("The shared latency protocol requires batch=1")
    kwargs["batch"] = 1
    image_root = Path(image_root)

    def load_bgr(image_name):
        with Image.open(image_root / image_name) as source:
            return np.asarray(source.convert("RGB"))[:, :, ::-1].copy()

    def synchronize():
        # Do not synchronize a GPU when the requested model runs on the CPU.
        device = getattr(model, "device", None)
        if getattr(device, "type", "cpu") == "cuda":
            torch.cuda.synchronize(device)

    first_image = load_bgr(names[0])
    for _ in range(3):
        model.predict(source=first_image, **kwargs)
    synchronize()
    all_correct, all_confidences, timings = [], [], []
    face_count = 0
    for index, image_name in enumerate(names, start=1):
        image = load_bgr(image_name)
        height, width = image.shape[:2]
        targets = clipped_boxes(annotations[image_name], width, height)
        synchronize()
        started = perf_counter()
        results = model.predict(source=image, **kwargs)
        synchronize()
        timings.append((perf_counter() - started) * 1000)
        if len(results) != 1:
            raise ValueError("Expected one prediction result per image")
        boxes = results[0].boxes
        predictions = boxes.xyxy.detach().cpu().numpy()
        confidences = boxes.conf.detach().cpu().numpy()
        all_correct.append(match_detections(predictions, confidences, targets))
        all_confidences.append(confidences)
        face_count += len(targets)
        if progress_every and (index % progress_every == 0 or index == len(names)):
            print(f"Avaliação: {index}/{len(names)} imagens", flush=True)
    metrics, curves = summarize_detections(
        np.concatenate(all_correct), np.concatenate(all_confidences), face_count, operating_conf,
    )
    mean_ms = float(np.mean(timings))
    metrics.update(
        images=len(names), mean_ms=mean_ms, median_ms=float(np.median(timings)),
        fps=1000.0 / max(mean_ms, np.finfo(float).eps),
    )
    return metrics, curves
