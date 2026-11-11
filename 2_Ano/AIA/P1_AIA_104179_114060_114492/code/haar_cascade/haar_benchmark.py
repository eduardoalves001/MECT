import os
import time
from pathlib import Path
import cv2
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from tqdm import tqdm

# ==========================================
# 1. CONFIGURAÇÕES GERAIS
# ==========================================
PROJECT_DIR = Path(__file__).resolve()
DATASET_DIR = PROJECT_DIR.parent.parent / "dataset" / "test_dataset" # podes ter que mudar
OUTPUT_DIR = PROJECT_DIR.parent.parent / "models" / "comparison" / "haar_benchmark_results"
IMAGES_DIR = DATASET_DIR / "images"

HAAR_PARAMS = {
    "scaleFactor": 1.05,
    "minNeighbors": 5,
    "minSize": (20, 20)
}

# ==========================================
# 2. AVALIAÇÃO E INFERÊNCIA DO HAAR CASCADE
# ==========================================
def evaluate_haar():
    """Corre a inferência do Haar Cascade em todas as imagens e recolhe métricas de latência e características."""
    print("\n--- A carregar o classificador Haar Cascade ---")
    face_cascade = cv2.CascadeClassifier(
        cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
    )
    print("a ", IMAGES_DIR)
    
    image_paths = list(IMAGES_DIR.rglob("*.*"))
    results_data = []

    # Limite para testes rápidos (podes remover depois)
    image_paths = image_paths[0:100]
    
    print(f"A processar {len(image_paths)} imagens para benchmark de tempo...")
    
    for img_path in tqdm(image_paths, desc="Haar Cascade", unit="imagem"):
        img = cv2.imread(str(img_path))
        if img is None:
            continue
            
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        img_h, img_w = gray.shape
        image_pixels = img_w * img_h
        
        # Medir o tempo de inferência puro da função detectMultiScale3
        start_time = time.perf_counter()
        
        faces, rejectLevels, levelWeights = face_cascade.detectMultiScale3(
            gray, 
            scaleFactor=HAAR_PARAMS["scaleFactor"], 
            minNeighbors=HAAR_PARAMS["minNeighbors"], 
            minSize=HAAR_PARAMS["minSize"],
            outputRejectLevels=True
        )
        inference_time_ms = (time.perf_counter() - start_time) * 1000.0
        
        n_faces = len(faces)
        
        # Calcular dimensões mínimas das faces detetadas e extrair confianças
        min_face_dim = 0
        confidences = []
        
        if n_faces > 0:
            dims = [min(w, h) for (x, y, w, h) in faces]
            min_face_dim = min(dims)
            
            # Converter levelWeights para uma lista normal de floats
            if levelWeights is not None:
                confidences = np.array(levelWeights).flatten().tolist()
                
        results_data.append({
            "image_name": img_path.name,
            "inference_time_ms": inference_time_ms,
            "n_faces": n_faces,
            "min_face_dim": min_face_dim,
            "image_pixels": image_pixels,
            "image_megapixels": image_pixels / 1e6,
            "confidences": confidences,
            "mean_confidence": np.mean(confidences) if confidences else 0.0,
            "max_confidence": np.max(confidences) if confidences else 0.0
        })
        
    return pd.DataFrame(results_data)

# ==========================================
# 3. GERAÇÃO DOS GRÁFICOS
# ==========================================
def generate_plots(df_results):
    """Gera gráficos vetoriais detalhados sobre o tempo de inferência do Haar Cascade."""
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    
    def save_plot(figure, filename):
        path = OUTPUT_DIR / f"{filename}.svg"
        figure.savefig(path, format="svg", bbox_inches="tight")
        plt.close(figure)
        print(f"Gráfico guardado: {path}")

    with plt.style.context("seaborn-v0_8-whitegrid"), plt.rc_context({
            "font.family": "DejaVu Sans", "font.size": 11, "svg.fonttype": "none"}):
        
        # --- 1. Inference Latency Distribution (Boxplot / Histograma) ---
        fig, ax = plt.subplots(figsize=(8, 6))
        ax.hist(df_results["inference_time_ms"], bins=30, color="crimson", edgecolor="0.2", alpha=0.85)
        mean_time = df_results["inference_time_ms"].mean()
        ax.axvline(mean_time, color="black", linestyle="--", linewidth=1.5, label=f"Média: {mean_time:.2f} ms")
        ax.set_xlabel("Inference Time per Image (ms)", fontsize=12)
        ax.set_ylabel("Frequency (Number of Images)", fontsize=12)
        ax.set_title("Haar Cascade - Inference Latency Distribution", fontsize=14, fontweight="bold")
        ax.legend(fontsize=11)
        save_plot(fig, "haar_latency_distribution")

        # --- 2. Inference Time vs. Number of Faces (Crowd Impact) ---
        fig, ax = plt.subplots(figsize=(8, 6))
        ax.scatter(df_results["n_faces"], df_results["inference_time_ms"], color="royalblue", alpha=0.5, edgecolors="none", s=30)
        ax.set_xlabel("Number of Faces Detected (per image)", fontsize=12)
        ax.set_ylabel("Inference Time (ms)", fontsize=12)
        ax.set_title("Inference Time vs. Number of Faces", fontsize=14, fontweight="bold")
        ax.set_ylim(bottom=0)
        save_plot(fig, "haar_time_vs_n_faces")

        # --- 3. Inference Time vs. Minimum Face Size ---
        fig, ax = plt.subplots(figsize=(8, 6))
        # Filtrar apenas imagens onde foram detetadas faces para fazer sentido analítico
        df_faces_only = df_results[df_results["n_faces"] >  0]
        ax.scatter(df_faces_only["min_face_dim"], df_faces_only["inference_time_ms"], color="forestgreen", alpha=0.5, edgecolors="none", s=30)
        ax.set_xlabel("Minimum Face Dimension (pixels)", fontsize=12)
        ax.set_ylabel("Inference Time (ms)", fontsize=12)
        ax.set_title("Inference Time vs. Face Scale/Size", fontsize=14, fontweight="bold")
        ax.set_ylim(bottom=0)
        save_plot(fig, "haar_time_vs_face_size")

        # --- 4. Inference Time vs. Image Resolution (Megapixels) ---
        fig, ax = plt.subplots(figsize=(8, 6))
        ax.scatter(df_results["image_megapixels"], df_results["inference_time_ms"], color="darkorange", alpha=0.5, edgecolors="none", s=30)
        ax.set_xlabel("Image Resolution (Megapixels)", fontsize=12)
        ax.set_ylabel("Inference Time (ms)", fontsize=12)
        ax.set_title("Inference Time vs. Image Resolution", fontsize=14, fontweight="bold")
        ax.set_ylim(bottom=0)
        ax.set_xlim(left=0)
        save_plot(fig, "haar_time_vs_image_resolution")

# ==========================================
# 4. EXECUÇÃO PRINCIPAL
# ==========================================
if __name__ == "__main__":
    if not IMAGES_DIR.exists():
        print(f"Erro: A pasta de imagens não foi encontrada em {IMAGES_DIR}")
    else:
        # 1. Executa avaliações de tempo e características
        df_metrics = evaluate_haar()
        
        if not df_metrics.empty:
            print("\n--- A estatísticas recolhidas ---")
            print(f"Tempo médio de inferência: {df_metrics['inference_time_ms'].mean():.2f} ms")
            print(f"Tempo mediano de inferência: {df_metrics['inference_time_ms'].median():.2f} ms")
            
            # 2. Gera os gráficos
            print("\n--- A gerar gráficos de desempenho do Haar Cascade ---")
            generate_plots(df_metrics)
            
            # Guardar os dados brutos num CSV para referência futura
            csv_path = OUTPUT_DIR / "haar_inference_stats.csv"
            df_metrics.to_csv(csv_path, index=False)
            print(f"\nResultados exportados para: {csv_path}")
            print(f"Todos os gráficos foram guardados na pasta: {OUTPUT_DIR.absolute()}")
        else:
            print("Nenhuma imagem foi processada com sucesso.")