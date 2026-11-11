"""Shared model identities and verified external checkpoints for all notebooks."""

import hashlib
from pathlib import Path
from urllib.request import urlopen


EXTERNAL_MODELS = {
    "yolov8_face": {
        "filename": "yolov8n-face-lindevs.pt",
        "url": "https://github.com/lindevs/yolov8-face/releases/latest/download/yolov8n-face-lindevs.pt",
        "sha256": "b038ca653b503453a94f6e12d76feca6840b2a97d7a1322b4498c5e922f29832",
        "source": "https://github.com/lindevs/yolov8-face",
    },
    "yolo26_face": {
        "filename": "yolo26n-face.pt",
        "url": "https://github.com/akanametov/yolo-face/releases/download/1.0.0/yolo26n-face.pt",
        "sha256": "c6a5405127a2e351292315a6a8084ea3e790dbec25b9d16a8e80d1e3f866efe1",
        "source": "https://github.com/akanametov/yolo-face",
        "asset_id": 343797372,
        "source_revision": "0037b0bce46e8fe3225963b07d2b9450b0f1e9e2",
    },
}


def file_sha256(path):
    digest = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def ensure_external_weights(project_root, keys=None):
    """Fetch missing external checkpoints, rejecting mismatched local/download bytes."""
    root = Path(project_root)
    paths = {}
    for key in (EXTERNAL_MODELS if keys is None else keys):
        metadata = EXTERNAL_MODELS[key]
        path = root / "weights" / metadata["filename"]
        if not path.is_file():
            print(f"Descarregar pesos: {metadata['filename']}", flush=True)
            with urlopen(metadata["url"], timeout=60) as response:
                payload = response.read()
            if hashlib.sha256(payload).hexdigest() != metadata["sha256"]:
                raise ValueError(f"SHA-256 inesperado no download de {metadata['filename']}")
            path.parent.mkdir(parents=True, exist_ok=True)
            partial = path.with_suffix(path.suffix + ".download")
            partial.write_bytes(payload)
            partial.replace(path)
        if file_sha256(path) != metadata["sha256"]:
            raise ValueError(f"Pesos diferentes da versão documentada: {path}")
        paths[key] = path
    return paths


def comparison_model_specs(project_root, fine_weights=None, include_yolo26_face=True):
    """Construct model specs using the documented 100-epoch checkpoint by default."""
    root = Path(project_root)
    new_fine = root / "weights/yolo26n_wider_100ep_best.pt"
    fine_path = Path(fine_weights) if fine_weights is not None else new_fine
    models = [
        dict(key="yolo_base", label="YOLO26n base (person)", weights=root / "weights/yolo26n.pt",
             class_name="person", source="Ultralytics / COCO", documented_epochs=None),
        dict(key="yolo_finetuned", label="YOLO26n fine-tuned", weights=fine_path,
             class_name="face", source="Fine-tuning do projeto no WIDER FACE", documented_epochs=None),
        dict(key="yolov8_face", label="YOLOv8n-Face (Lindevs)",
             weights=root / "weights/yolov8n-face-lindevs.pt", class_name="face",
             source=EXTERNAL_MODELS["yolov8_face"]["source"], documented_epochs=300),
    ]
    if include_yolo26_face:
        models.append(dict(
            key="yolo26_face", label="YOLO26n-Face (akanametov)",
            weights=root / "weights/yolo26n-face.pt", class_name="face",
            source=EXTERNAL_MODELS["yolo26_face"]["source"], documented_epochs=None,
        ))
    return models
