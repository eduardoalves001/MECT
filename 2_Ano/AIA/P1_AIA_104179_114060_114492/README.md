# AIA - Face Detection


```text
+-- code/
|   +-- haar_cascade/
|   +-- vgg16/
|   +-- yolo/
|   +-- dataset.ipynb
+-- report/
+-- weights/
```

The `weights` folder contains the checkpoints used by the project.

### VGG16

- `vgg16_best.pt` - VGG16 detector trained with the standard training
  configuration.
- `vgg16_best_optimized.pt` - VGG16 detector trained with the
  hyperparameters selected through Optuna hyperparameter tuning.

### YOLO

- `yolo26n.pt` - Base YOLO26n checkpoint used to initialise the project's
  fine-tuning workflow.
- `yolo26n_wider_100ep_best.pt` - YOLO26n fine-tuned on WIDER FACE for 100
  epochs. 
- `yolo26n-face.pt` - YOLO26n face detector from
  [akanametov/yolo-face](https://github.com/akanametov/yolo-face).
- `yolov8n-face-lindevs.pt` - YOLOv8n face detector from
  [lindevs/yolov8-face](https://github.com/lindevs/yolov8-face).


## Datasets

### WIDER FACE

The WIDER FACE dataset is used for training and/or evaluating the face
detection models.

Official website and download page:

<http://shuoyang1213.me/WIDERFACE/>

### External test dataset

The external dataset used for testing and comparison is available on Kaggle:

<https://www.kaggle.com/datasets/fareselmenshawii/face-detection-dataset>

