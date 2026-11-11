import os
import time
import glob
import numpy as np
import torch
import torchvision
from torchvision.models.detection import FasterRCNN
from torchvision.models.detection.rpn import AnchorGenerator
import torchvision.transforms.functional as F
from PIL import Image
import matplotlib.pyplot as plt
import seaborn as sns
from tqdm import tqdm


device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
print(f"Using device: {device}")

WEIGHTS_DEFAULT = r"C:\Users\omari\OneDrive\Documentos\MRSI\2nd\AIA\project_1\AIA_TP1\weights\vgg16_best.pt"
WEIGHTS_OPTIMIZED = r"C:\Users\omari\OneDrive\Documentos\MRSI\2nd\AIA\project_1\AIA_TP1\weights\vgg16_best_optimized.pt"

TEST_IMAGES_DIR = "dataset_benchmark/images"
TEST_LABELS_DIR = "dataset_benchmark/labels"
OUTPUT_DIR = r"C:\Users\omari\OneDrive\Documentos\MRSI\2nd\AIA\project_1\AIA_TP1\models\vgg16\vgg16_evaluation_results"
os.makedirs(OUTPUT_DIR, exist_ok=True)

IOU_THRESHOLD = 0.5
SCORE_MIN_THRESHOLD = 0.05

# 2. MODEL DEFINITION
def build_default_vgg():
    backbone = torchvision.models.vgg16(weights=None).features
    backbone.out_channels = 512
    anchor_generator = AnchorGenerator(sizes=((32, 64, 128, 256, 512),), aspect_ratios=((0.5, 1.0, 2.0),))
    roi_pooler = torchvision.ops.MultiScaleRoIAlign(featmap_names=['0'], output_size=7, sampling_ratio=2)
    return FasterRCNN(backbone, num_classes=2, rpn_anchor_generator=anchor_generator, box_roi_pool=roi_pooler)

def build_optimized_vgg():
    backbone = torchvision.models.vgg16(weights=None).features
    backbone.out_channels = 512
    anchor_generator = AnchorGenerator(sizes=((16, 32, 64, 128, 256),), aspect_ratios=((0.5, 1.0, 2.0),))
    roi_pooler = torchvision.ops.MultiScaleRoIAlign(featmap_names=['0'], output_size=7, sampling_ratio=2)
    return FasterRCNN(backbone, num_classes=2, rpn_anchor_generator=anchor_generator, box_roi_pool=roi_pooler, rpn_nms_thresh=0.7)

def load_model(build_fn, weights_path):
    model = build_fn()
    model.load_state_dict(torch.load(weights_path, map_location=device, weights_only=True))
    model.to(device)
    model.eval()
    return model

# 3. ANNOTATION READING AND IOU
def load_yolo_labels(label_path, img_w, img_h):
    boxes = []
    if not os.path.exists(label_path):
        return torch.tensor([], dtype=torch.float32)
    with open(label_path, 'r') as f:
        for line in f.readlines():
            parts = line.strip().split()
            if len(parts) >= 5:
                _, x_c, y_c, w, h = map(float, parts[:5])
                x1 = (x_c - w / 2) * img_w
                y1 = (y_c - h / 2) * img_h
                x2 = (x_c + w / 2) * img_w
                y2 = (y_c + h / 2) * img_h
                boxes.append([x1, y1, x2, y2])
    return torch.tensor(boxes, dtype=torch.float32)

def compute_iou(box1, box2):
    x1, y1 = max(box1[0], box2[0]), max(box1[1], box2[1])
    x2, y2 = min(box1[2], box2[2]), min(box1[3], box2[3])
    inter_area = max(0, x2 - x1) * max(0, y2 - y1)
    box1_area = (box1[2] - box1[0]) * (box1[3] - box1[1])
    box2_area = (box2[2] - box2[0]) * (box2[3] - box2[1])
    union_area = box1_area + box2_area - inter_area
    return inter_area / union_area if union_area > 0 else 0

def read_gpu_utilization():
    if device.type != 'cuda':
        return None
    try:
        return float(torch.cuda.utilization(device))
    except (RuntimeError, OSError):
        return None

def evaluate_model(model, image_paths, model_name="Model"):
    all_detections = []
    total_gt = 0
    inference_times = []
    gpu_utilizations = []
    errors_count = 0

    dummy = torch.zeros((1, 3, 640, 640), device=device)
    with torch.no_grad():
        for _ in range(3): _ = model(dummy)

    pbar = tqdm(image_paths, desc=f"Evaluating {model_name}", unit="img")

    for idx, img_path in enumerate(pbar):
        try:
            img = Image.open(img_path).convert("RGB")
            img_w, img_h = img.size
            img_tensor = F.to_tensor(img).unsqueeze(0).to(device)

            label_file = os.path.splitext(os.path.basename(img_path))[0] + ".txt"
            gt_path = os.path.join(TEST_LABELS_DIR, label_file)
            gt_boxes = load_yolo_labels(gt_path, img_w, img_h)
            total_gt += len(gt_boxes)

            if device.type == 'cuda': torch.cuda.synchronize()
            start_t = time.perf_counter()
            with torch.no_grad():
                preds = model(img_tensor)[0]
            if device.type == 'cuda': torch.cuda.synchronize()
            end_t = time.perf_counter()

            inference_times.append((end_t - start_t) * 1000)
            gpu_utilization = read_gpu_utilization()
            if gpu_utilization is not None:
                gpu_utilizations.append(gpu_utilization)

            pred_boxes = preds['boxes'].cpu()
            pred_scores = preds['scores'].cpu()

            matched_gt = set()
            sorted_indices = torch.argsort(pred_scores, descending=True)

            for s_idx in sorted_indices:
                p_score = pred_scores[s_idx].item()
                if p_score < SCORE_MIN_THRESHOLD:
                    continue

                p_box = pred_boxes[s_idx]

                best_iou, best_gt_idx = 0, -1
                for g_idx, gt_box in enumerate(gt_boxes):
                    iou = compute_iou(p_box, gt_box)
                    if iou > best_iou:
                        best_iou = iou
                        best_gt_idx = g_idx

                if best_iou >= IOU_THRESHOLD and best_gt_idx not in matched_gt:
                    all_detections.append((p_score, 1))
                    matched_gt.add(best_gt_idx)
                else:
                    all_detections.append((p_score, 0))

        except Exception as e:
            errors_count += 1
            tqdm.write(f"Error processing image {img_path}: {e}")

        # Periodically clear the GPU cache to reduce memory usage.
        if idx % 100 == 0 and device.type == 'cuda':
            torch.cuda.empty_cache()

    if errors_count > 0:
        print(f"Total errors found in the dataset: {errors_count}")

    return all_detections, total_gt, np.array(inference_times), np.array(gpu_utilizations)

def calculate_ap(detections, total_gt):
    """Calculate AP as the area under the interpolated precision-recall curve."""
    if not detections or total_gt == 0:
        return 0.0, [0], [0]

    detections.sort(key=lambda x: x[0], reverse=True)
    
    scores = [d[0] for d in detections]
    tp_fp = [d[1] for d in detections]

    tp_cumsum = np.cumsum(tp_fp)
    fp_cumsum = np.cumsum([1 - x for x in tp_fp])

    recalls = tp_cumsum / total_gt
    precisions = tp_cumsum / (tp_cumsum + fp_cumsum)

    recalls = np.concatenate(([0.0], recalls, [1.0]))
    precisions = np.concatenate(([1.0], precisions, [0.0]))

    for i in range(len(precisions) - 2, -1, -1):
        precisions[i] = max(precisions[i], precisions[i + 1])

    try:
        ap = np.trapezoid(precisions, recalls)
    except AttributeError:
        ap = np.trapz(precisions, recalls)
    return ap, precisions, recalls



if __name__ == '__main__':
    image_paths = glob.glob(os.path.join(TEST_IMAGES_DIR, "*.[jJ][pP][gG]")) + \
                  glob.glob(os.path.join(TEST_IMAGES_DIR, "*.[pP][nN][gG]"))

    if not image_paths:
        print(f"No images found in {TEST_IMAGES_DIR}.")
        exit()

    print(f"Evaluating {len(image_paths)} images in total...")

    print("\n--- 1/2: VGG16 Standard ---")
    model_default = load_model(build_default_vgg, WEIGHTS_DEFAULT)
    det_default, gt_default, times_default, gpu_default = evaluate_model(model_default, image_paths, "VGG16 Standard")
    ap_default, prec_default, rec_default = calculate_ap(det_default, gt_default)

    print("\n--- 2/2: VGG16 Optimized ---")
    model_opt = load_model(build_optimized_vgg, WEIGHTS_OPTIMIZED)
    det_opt, gt_opt, times_opt, gpu_opt = evaluate_model(model_opt, image_paths, "VGG16 Optimized")
    ap_opt, prec_opt, rec_opt = calculate_ap(det_opt, gt_opt)

    sns.set_theme(style="whitegrid")

    # precision-recall curve
    plt.figure(figsize=(8, 6))
    plt.plot(rec_default, prec_default, label=f'VGG16 Standard (AP={ap_default:.3f})', color='crimson', linewidth=2.5)
    plt.plot(rec_opt, prec_opt, label=f'VGG16 Optimized (AP={ap_opt:.3f})', color='forestgreen', linewidth=2.5)
    plt.xlabel('Recall', fontsize=12)
    plt.ylabel('Precision', fontsize=12)
    plt.title('Precision-Recall Curves (IoU=0.5)', fontsize=14, fontweight='bold')
    plt.legend(fontsize=11)
    plt.tight_layout()
    plt.savefig(os.path.join(OUTPUT_DIR, 'precision_recall_curve.svg'))
    plt.close()

    # inference latency boxplot
    plt.figure(figsize=(8, 6))
    sns.boxplot(data=[times_default, times_opt], palette=['crimson', 'forestgreen'], width=0.4)
    plt.xticks([0, 1], ['VGG16 Standard', 'VGG16 Optimized'], fontsize=11)
    plt.ylabel('Time per Image (ms)', fontsize=12)
    plt.title('Inference Latency Distribution (ms)', fontsize=14, fontweight='bold')
    plt.tight_layout()
    plt.savefig(os.path.join(OUTPUT_DIR, 'inference_latency_boxplot.svg'))
    plt.close()

    model_names = ['VGG16 Standard', 'VGG16 Optimized']
    mean_times = [np.mean(times_default), np.mean(times_opt)]
    mean_gpu_utilizations = [
        np.mean(gpu_default) if gpu_default.size else np.nan,
        np.mean(gpu_opt) if gpu_opt.size else np.nan,
    ]

    # inference time bar plot
    plt.figure(figsize=(8, 6))
    bars = plt.bar(model_names, mean_times, color=['crimson', 'forestgreen'], width=0.5)
    plt.ylabel('Mean time per image (ms)', fontsize=12)
    plt.title('Mean Inference Time', fontsize=14, fontweight='bold')
    for bar, value in zip(bars, mean_times):
        plt.text(
            bar.get_x() + bar.get_width() / 2,
            bar.get_height(),
            f'{value:.2f} ms',
            ha='center',
            va='bottom',
            fontweight='bold',
        )
    plt.tight_layout()
    plt.savefig(os.path.join(OUTPUT_DIR, 'mean_inference_time_bar.svg'))
    plt.close()

    # mean GPU utilization bar plot
    plt.figure(figsize=(8, 6))
    plot_gpu_values = np.nan_to_num(mean_gpu_utilizations, nan=0.0)
    bars = plt.bar(model_names, plot_gpu_values, color=['crimson', 'forestgreen'], width=0.5)
    plt.ylabel('Mean GPU utilization (%)', fontsize=12)
    plt.title('Mean GPU Utilization', fontsize=14, fontweight='bold')
    plt.ylim(0, 100)
    for bar, value in zip(bars, mean_gpu_utilizations):
        label = f'{value:.2f}%' if not np.isnan(value) else 'N/A'
        plt.text(
            bar.get_x() + bar.get_width() / 2,
            bar.get_height(),
            label,
            ha='center',
            va='bottom',
            fontweight='bold',
        )
    plt.tight_layout()
    plt.savefig(os.path.join(OUTPUT_DIR, 'mean_gpu_utilization_bar.svg'))
    plt.close()

    print("\nEvaluation completed successfully.")
    print(f"VGG16 Standard  -> AP@0.5 (mAP@0.5): {ap_default:.4f} | Mean inference time: {mean_times[0]:.2f} ms | Mean GPU utilization: {mean_gpu_utilizations[0]:.2f}%")
    print(f"VGG16 Optimized -> AP@0.5 (mAP@0.5): {ap_opt:.4f} | Mean inference time: {mean_times[1]:.2f} ms | Mean GPU utilization: {mean_gpu_utilizations[1]:.2f}%")
    print(f"SVG plots saved to: {OUTPUT_DIR}")