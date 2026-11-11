import cv2
import time
import numpy as np
import torch
import torchvision
from torchvision.models.detection import FasterRCNN
from torchvision.models.detection.rpn import AnchorGenerator
import torchvision.transforms.functional as F
from ultralytics import YOLO

# ==========================================
# 1. Configurar Dispositivo
# ==========================================
device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
print(f"A usar dispositivo para redes profundas: {device}")

# ==========================================
# 2. Carregar Haar Cascade com Hiperparâmetros
# ==========================================
HAAR_PARAMS = {
    "scaleFactor": 1.05,
    "minNeighbors": 5,
    "minSize": (20, 20)
}
haar_path = cv2.data.haarcascades + 'haarcascade_frontalface_default.xml'
face_cascade = cv2.CascadeClassifier(haar_path)

# ==========================================
# 3. Carregar Faster R-CNN (VGG-16 Otimizada)
# ==========================================
def build_vgg16_faster_rcnn():
    backbone = torchvision.models.vgg16(weights=None).features
    backbone.out_channels = 512

    anchor_generator = AnchorGenerator(
        sizes=((32, 64, 128, 256, 512),),
        aspect_ratios=((0.5, 1.0, 2.0),)
    )

    roi_pooler = torchvision.ops.MultiScaleRoIAlign(
        featmap_names=['0'],
        output_size=7,
        sampling_ratio=2
    )

    return FasterRCNN(
        backbone,
        num_classes=2,
        rpn_anchor_generator=anchor_generator,
        box_roi_pool=roi_pooler
    )

vgg_model = build_vgg16_faster_rcnn()
vgg_weights_path = r"C:\Users\omari\OneDrive\Documentos\MRSI\2nd\AIA\1_AIA_104179_114060_114492\weights\vgg16_best_optimized.pt"
vgg_model.load_state_dict(torch.load(vgg_weights_path, map_location=device, weights_only=True))
vgg_model.to(device)
vgg_model.eval()

# ==========================================
# 4. Carregar YOLO (Fine-tuned)
# ==========================================
yolo_weights_path = r"C:\Users\omari\OneDrive\Documentos\MRSI\2nd\AIA\1_AIA_104179_114060_114492\weights\yolo26n_wider_100ep_best.pt"
yolo_model = YOLO(yolo_weights_path)

CONFIDENCE_THRESHOLD = 0.50

# Cores (BGR)
COLOR_HAAR = (255, 100, 0)   # Azul
COLOR_VGG = (0, 255, 0)      # Verde
COLOR_YOLO = (0, 165, 255)   # Laranja

cap = cv2.VideoCapture(1)

# Resolução de captura recomendada para caberem 3 painéis no ecrã (640x360 ou 640x480)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

while True:
    ret, frame = cap.read()
    if not ret:
        break

    # Cria 3 cópias independentes do frame original
    frame_haar = frame.copy()
    frame_vgg = frame.copy()
    frame_yolo = frame.copy()

    # ------------------------------------------
    # Painel 1: Haar Cascade (CPU)
    # ------------------------------------------
    t0 = time.perf_counter()
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    haar_faces = face_cascade.detectMultiScale(
        gray,
        scaleFactor=HAAR_PARAMS["scaleFactor"],
        minNeighbors=HAAR_PARAMS["minNeighbors"],
        minSize=HAAR_PARAMS["minSize"]
    )
    t_haar = (time.perf_counter() - t0) * 1000.0

    for (x, y, w, h) in haar_faces:
        cv2.rectangle(frame_haar, (x, y), (x + w, y + h), COLOR_HAAR, 2)
        cv2.putText(frame_haar, "Face", (x, max(20, y - 8)),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, COLOR_HAAR, 2)

    # ------------------------------------------
    # Painel 2: Faster R-CNN VGG-16 (GPU)
    # ------------------------------------------
    img_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    img_tensor = F.to_tensor(img_rgb).unsqueeze(0).to(device)

    t0 = time.perf_counter()
    with torch.no_grad():
        vgg_preds = vgg_model(img_tensor)[0]
    t_vgg = (time.perf_counter() - t0) * 1000.0

    vgg_boxes = vgg_preds['boxes'].cpu().numpy()
    vgg_scores = vgg_preds['scores'].cpu().numpy()

    for box, score in zip(vgg_boxes, vgg_scores):
        if score >= CONFIDENCE_THRESHOLD:
            x1, y1, x2, y2 = map(int, box)
            cv2.rectangle(frame_vgg, (x1, y1), (x2, y2), COLOR_VGG, 2)
            cv2.putText(frame_vgg, f"Face: {score*100:.1f}%", (x1, max(20, y1 - 8)),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, COLOR_VGG, 2)

    # ------------------------------------------
    # Painel 3: YOLO Fine-tuned (GPU)
    # ------------------------------------------
    t0 = time.perf_counter()
    yolo_results = yolo_model(frame, conf=CONFIDENCE_THRESHOLD, verbose=False, device=0 if torch.cuda.is_available() else 'cpu')
    t_yolo = (time.perf_counter() - t0) * 1000.0

    for box in yolo_results[0].boxes:
        coords = box.xyxy[0].cpu().numpy()
        score = float(box.conf[0])
        x1, y1, x2, y2 = map(int, coords)
        cv2.rectangle(frame_yolo, (x1, y1), (x2, y2), COLOR_YOLO, 2)
        cv2.putText(frame_yolo, f"Face: {score*100:.1f}%", (x1, max(20, y1 - 8)),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, COLOR_YOLO, 2)

    # ------------------------------------------
    # Barra de Título Superior em cada Painel
    # ------------------------------------------
    def draw_header(img, title, latency, color):
        cv2.rectangle(img, (0, 0), (img.shape[1], 40), (25, 25, 25), -1)
        cv2.putText(img, title, (15, 26), cv2.FONT_HERSHEY_SIMPLEX, 0.65, color, 2)
        cv2.putText(img, f"{latency:.1f} ms", (img.shape[1] - 110, 26), 
                    cv2.FONT_HERSHEY_SIMPLEX, 0.60, (220, 220, 220), 2)

    draw_header(frame_haar, "Haar Cascade (CPU)", t_haar, COLOR_HAAR)
    draw_header(frame_vgg, "Faster R-CNN VGG-16", t_vgg, COLOR_VGG)
    draw_header(frame_yolo, "YOLO Fine-Tuned", t_yolo, COLOR_YOLO)

    # Junta os 3 painéis lado a lado horizontalmente
    combined_display = np.hstack((frame_haar, frame_vgg, frame_yolo))

    # Opcional: redimensionar a janela combinada se exceder o monitor (ex.: 1920x480 -> 1440x360)
    # combined_display = cv2.resize(combined_display, (1440, 360))

    cv2.imshow('DEMO', combined_display)

    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()