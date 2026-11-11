"""Figuras do projeto: funções sem treino, seleção ou escrita de métricas.

As figuras exploratórias recebem apenas o conjunto de treino. Todas as
coordenadas do diagrama são explícitas para manter o PNG legível e compacto.
"""

from pathlib import Path
import logging

import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch
import numpy as np
import seaborn as sns
from sklearn.metrics import confusion_matrix, recall_score, roc_curve


# Mensagens INFO sobre eixos categóricos não são avisos de qualidade do fit.
logging.getLogger("matplotlib.category").setLevel(logging.WARNING)


BLUE = "#28638c"
RED = "#b84450"
GREEN = "#39836d"
LIGHT = "#edf3f8"
SHORT_NAMES = {
    "Symbolist - arvore": "Árvore",
    "Connectionist - MLP": "MLP",
    "Evolutionary - GA": "GA",
    "Bayesian - Gaussian NB": "Gaussian NB",
    "Analogizer - kNN": "kNN",
    "Baseline - maioria": "Baseline: maioria",
}


def _save(fig, figures, filename):
    folder = Path(figures)
    folder.mkdir(parents=True, exist_ok=True)
    fig.savefig(folder / filename, dpi=160, bbox_inches="tight", facecolor="white")
    plt.close(fig)


def _box(axis, center, width, height, text, color=LIGHT, fontsize=10):
    x, y = center
    axis.add_patch(FancyBboxPatch(
        (x - width / 2, y - height / 2), width, height,
        boxstyle="round,pad=0.008,rounding_size=0.012",
        linewidth=1.2, edgecolor=BLUE, facecolor=color, zorder=3,
    ))
    axis.text(x, y, text, ha="center", va="center", fontsize=fontsize,
              color="#173348", zorder=4)


def _arrow(axis, start, end, color=BLUE, **kwargs):
    axis.annotate("", xy=end, xytext=start,
                  arrowprops={"arrowstyle": "->", "color": color,
                              "lw": 1.4, **kwargs}, zorder=2)


def make_pipeline_plot(figures):
    """Mostra o holdout e os dois níveis da CV, incluindo os cinco learners."""
    fig, ax = plt.subplots(figsize=(13.5, 8.5))
    ax.set(xlim=(0, 1), ylim=(0, 1))
    ax.axis("off")
    ax.set_title("Protocolo experimental: seleção antes da avaliação no teste",
                 fontsize=16, pad=18, weight="bold")
    _box(ax, (.12, .91), .20, .075, "CSV: polars\n569 observações, 30 features")
    _box(ax, (.43, .91), .24, .075, "Split estratificado 80/20\nseed = 42")
    _arrow(ax, (.23, .91), (.30, .91))
    _box(ax, (.80, .91), .29, .075, "Teste reservado: 20%\nsem seleção nem tuning", color="#fceeee")
    _arrow(ax, (.56, .91), (.64, .91), color=RED)
    _box(ax, (.20, .79), .33, .075, "Treino: 80%\nEDA e decisões de pré-processamento")
    _arrow(ax, (.39, .87), (.25, .835))

    # A moldura distingue a avaliação externa do tuning interno.
    ax.add_patch(FancyBboxPatch((.025, .265), .95, .455,
                 boxstyle="round,pad=0.008,rounding_size=0.015",
                 linewidth=1.3, edgecolor=BLUE, facecolor="#f8fafc", zorder=0))
    ax.text(.045, .69, "CV externa estratificada: 5 folds × 2 repetições",
            fontsize=12, color=BLUE, weight="bold")
    _arrow(ax, (.20, .745), (.20, .727))
    _box(ax, (.20, .59), .28, .105, "Treino do fold externo\nCV interna: 3 folds\nprocura de hiperparâmetros")
    _box(ax, (.20, .415), .28, .105, "Pipeline em cada fit interno\nscaler ajustado só nesse treino\nárvore sem scaler")
    _arrow(ax, (.20, .53), (.20, .475))
    learners = [
        "Symbolist: árvore", "Connectionist: MLP",
        "Evolutionary: GA", "Bayesian: Gaussian NB", "Analogizer: kNN",
    ]
    for y, label in zip(np.linspace(.63, .34, 5), learners):
        _box(ax, (.53, float(y)), .24, .045, label, fontsize=10)
        _arrow(ax, (.35, .415), (.40, float(y)), color="#7194ae")
        _arrow(ax, (.66, float(y)), (.72, .49), color="#7194ae")
    _box(ax, (.84, .49), .21, .13,
         "Melhor configuração\nrefit no treino externo\navaliação no fold externo")
    _box(ax, (.27, .17), .43, .10,
         "Selecionar a família pela média da CV externa\ncomparar dispersão e erros; congelar a escolha")
    ax.plot([.84, .84, .27], [.415, .245, .245], color=BLUE, lw=1.4, zorder=2)
    _arrow(ax, (.27, .245), (.27, .225))
    _box(ax, (.76, .17), .38, .10,
         "Tuning final no treino completo\nrefit e avaliação final dos cinco no teste")
    _arrow(ax, (.50, .17), (.56, .17))
    # O teste contorna a moldura da CV: não entra na avaliação externa.
    ax.plot([.94, .992, .992], [.865, .865, .17], color=RED, lw=1.4, zorder=2)
    _arrow(ax, (.992, .17), (.958, .17), color=RED)
    ax.text(.5, .045,
            "As métricas do teste não escolhem o vencedor nem os hiperparâmetros. "
            "Os resultados antigos do teste já foram observados (limitação histórica).",
            ha="center", va="center", fontsize=9, color="#4b5563", wrap=True)
    _save(fig, figures, "00_pipeline.png")


def make_eda_plots(data, feature_names, figures):
    """Guarda quatro figuras exploratórias, sempre identificadas como treino."""
    counts = data.group_by("target").len().sort("target")
    sizes = counts["len"].to_numpy()
    numeric = data.select(feature_names).to_pandas()
    ranges = numeric.max() - numeric.min()

    fig, axes = plt.subplots(1, 2, figsize=(13.5, 7.5),
                             gridspec_kw={"width_ratios": [1, 1.8]})
    axes[0].bar(["Maligno (0)", "Benigno (1)"], sizes, color=[RED, BLUE])
    for i, count in enumerate(sizes):
        axes[0].text(i, count + max(sizes) * .025,
                     f"{count}\n({count / sum(sizes):.1%})", ha="center", fontsize=11)
    axes[0].set(title="Distribuição do alvo no treino", ylabel="Observações")
    axes[0].set_ylim(0, max(sizes) * 1.20)
    axes[1].barh(feature_names, ranges, color=BLUE, alpha=.85)
    axes[1].invert_yaxis()
    axes[1].set_xscale("log")
    axes[1].set(title="Amplitude das features no treino",
                xlabel="Máximo − mínimo (escala logarítmica)")
    axes[1].tick_params(axis="y", labelsize=8)
    fig.tight_layout(w_pad=3)
    _save(fig, figures, "01_alvo_e_escalas.png")

    selected = ["mean_radius", "mean_texture", "mean_perimeter",
                "mean_area", "mean_smoothness", "mean_concavity"]
    frame = data.select(["target"] + selected).to_pandas()
    fig, axes = plt.subplots(2, 3, figsize=(13.5, 7.5))
    for axis, feature in zip(axes.ravel(), selected):
        sns.boxplot(data=frame, x="target", y=feature, ax=axis,
                    color="#8dbda9", fliersize=3)
        axis.set(title=feature, xlabel="", ylabel="")
        axis.set_xticks([0, 1], ["Maligno", "Benigno"])
    fig.suptitle("Features por classe: separação e sobreposição no treino", fontsize=15)
    fig.tight_layout(rect=(0, 0, 1, .96))
    _save(fig, figures, "02_features_por_classe.png")

    fig, axes = plt.subplots(6, 5, figsize=(16, 17))
    for axis, feature in zip(axes.ravel(), feature_names):
        sns.histplot(numeric[feature], bins=20, kde=True, ax=axis, color=BLUE)
        axis.set_title(feature, fontsize=9)
        axis.set(xlabel="", ylabel="")
        axis.tick_params(labelsize=8)
    fig.suptitle("Distribuições das 30 features no treino", fontsize=16)
    fig.tight_layout(rect=(0, 0, 1, .97))
    _save(fig, figures, "03_distribuicoes_features.png")

    corr = numeric.corr()
    fig, axis = plt.subplots(figsize=(14.5, 12))
    sns.heatmap(corr, cmap="vlag", vmin=-1, vmax=1, center=0, ax=axis,
                square=True, cbar_kws={"label": "Correlação de Pearson", "shrink": .75})
    axis.set_title("Correlações entre features no treino", fontsize=15, pad=15)
    axis.set_xticklabels(axis.get_xticklabels(), rotation=60, ha="right", fontsize=8)
    axis.set_yticklabels(axis.get_yticklabels(), rotation=0, fontsize=8)
    fig.tight_layout()
    _save(fig, figures, "04_correlacoes.png")


def make_comparison_plot(spread, figures):
    """Compara média e desvio amostral nos mesmos folds externos."""
    frame = spread.to_pandas()
    order = frame.sort_values("balanced_accuracy_mean", ascending=False)
    labels = [SHORT_NAMES.get(name, name) for name in order["model"]]
    positions = np.arange(len(order))
    fig, axes = plt.subplots(1, 2, figsize=(12.5, 5.2), sharey=True)
    for axis, metric, title, baseline in [
        (axes[0], "balanced_accuracy", "Balanced accuracy", .5),
        (axes[1], "malignant_recall", "Recall maligno", 0),
    ]:
        axis.errorbar(order[f"{metric}_mean"], positions,
                      xerr=order[f"{metric}_std"], fmt="o", capsize=4,
                      color=BLUE, markersize=7)
        axis.axvline(baseline, color=RED, linestyle="--", linewidth=1,
                     label="Baseline de maioria")
        axis.set(title=title, xlabel="Média ± desvio padrão dos folds", xlim=(-.035, 1.035))
        axis.set_yticks(positions, labels)
        axis.grid(axis="y", visible=False)
        axis.legend(loc="lower right", fontsize=9)
    axes[0].invert_yaxis()
    fig.suptitle("Comparação em nested CV: 5 folds × 2 repetições", fontsize=15)
    fig.tight_layout(rect=(0, 0, 1, .95))
    _save(fig, figures, "05_comparacao_com_spread.png")


def make_diagnostics_plot(best_name, y_test, predictions, probabilities, figures):
    """A curva descreve thresholds; não escolhe um novo threshold no teste."""
    truth = np.asarray(y_test) == 0
    probability = np.asarray(probabilities)[:, 0]
    fpr, tpr, thresholds = roc_curve(truth.astype(int), probability, drop_intermediate=False)
    finite = np.isfinite(thresholds)
    thresholds, recalls, specificity = thresholds[finite], tpr[finite], 1 - fpr[finite]
    # Incluir t=0 e t=1 torna visíveis os extremos mesmo sem scores exatamente 0/1.
    thresholds = np.unique(np.r_[0., thresholds, .5, 1.])
    recalls = np.asarray([np.mean(probability[truth] >= t) for t in thresholds])
    specificity = np.asarray([np.mean(probability[~truth] < t) for t in thresholds])
    fig, axes = plt.subplots(1, 2, figsize=(12.5, 5))
    matrix = confusion_matrix(y_test, predictions, labels=[0, 1])
    sns.heatmap(matrix, annot=True, fmt="d", cmap="Blues", ax=axes[0], cbar=False,
                xticklabels=["Maligno", "Benigno"], yticklabels=["Maligno", "Benigno"],
                annot_kws={"size": 14})
    axes[0].set(title="Matriz de confusão no teste", xlabel="Predito", ylabel="Real")
    axes[0].tick_params(axis="y", rotation=0)
    axes[1].step(thresholds, recalls, where="pre", label="Recall maligno", color=BLUE)
    axes[1].step(thresholds, specificity, where="pre", label="Especificidade", color=GREEN)
    axes[1].axvline(.5, color=RED, linestyle="--", linewidth=1, label="Threshold de referência: 0,5")
    axes[1].scatter([.5], [recall_score(y_test, predictions, pos_label=0)],
                     color=BLUE, s=45, zorder=5)
    axes[1].set(title="Sensibilidade ao threshold (diagnóstico)",
                xlabel="Threshold da probabilidade de malignidade",
                ylabel="Proporção", xlim=(-.02, 1.02), ylim=(-.035, 1.035))
    axes[1].legend(loc="lower left", fontsize=9)
    fig.suptitle(f"Modelo selecionado pela CV: {SHORT_NAMES.get(best_name, best_name)}", fontsize=15)
    fig.tight_layout(rect=(0, 0, 1, .95))
    _save(fig, figures, "06_diagnostico_melhor_learner.png")


def make_learning_plot(sizes, train_scores, validation_scores, figures):
    """Os pontos são diagnósticos no treino, sem recurso ao conjunto de teste."""
    train_scores, validation_scores = np.asarray(train_scores), np.asarray(validation_scores)
    fig, axis = plt.subplots(figsize=(8.5, 5.5))
    for scores, label, color in [(train_scores, "Treino", BLUE),
                                  (validation_scores, "Validação", GREEN)]:
        mean, std = scores.mean(axis=1), scores.std(axis=1, ddof=1)
        axis.plot(sizes, mean, "o-", label=label, color=color)
        axis.fill_between(sizes, np.clip(mean - std, 0, 1), np.clip(mean + std, 0, 1),
                           alpha=.13, color=color)
    axis.set(xlabel="Número de exemplos de treino", ylabel="Balanced accuracy",
              title="Curva de aprendizagem do modelo selecionado pela CV")
    axis.legend(title="Média ± desvio padrão")
    fig.tight_layout()
    _save(fig, figures, "07_curva_aprendizagem.png")


def make_mlp_diagnostic_plot(diagnostics, figures):
    """Mostra a diferença entre discriminação e decisão nos folds externos."""
    if hasattr(diagnostics, "to_dicts"):
        diagnostics = diagnostics.to_dicts()
    if not diagnostics:
        return
    folds = [row["fold"] for row in diagnostics]
    fig, axes = plt.subplots(1, 2, figsize=(12, 4.5))
    for metric, label, color in [
        ("balanced_accuracy", "Balanced accuracy", BLUE),
        ("malignant_recall", "Recall maligno", RED),
        ("roc_auc", "ROC-AUC", GREEN),
    ]:
        axes[0].plot(folds, [row[metric] for row in diagnostics], "o-", label=label, color=color)
    score_min = min(row[metric] for row in diagnostics
                    for metric in ("balanced_accuracy", "malignant_recall", "roc_auc"))
    axes[0].set(xlabel="Fold externo", ylabel="Score", ylim=(max(0, score_min - .05), 1.02),
                title="MLP: discriminação e decisão")
    axes[0].set_xticks(folds)
    axes[0].legend(fontsize=9)
    iterations = [row.get("n_iter", np.nan) for row in diagnostics]
    axes[1].bar(folds, iterations, color=BLUE, alpha=.85)
    axes[1].set(xlabel="Fold externo", ylabel="Iterações / épocas realizadas",
                title="MLP: duração do treino por fold")
    axes[1].set_xticks(folds)
    fig.tight_layout()
    _save(fig, figures, "08_diagnostico_mlp.png")
