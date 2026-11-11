import os
os.environ.setdefault('KERAS_BACKEND', 'jax')

import hashlib
import importlib.metadata
import json
from pathlib import Path
import platform
import random
import sys
from time import perf_counter

import jax
import keras
import matplotlib
matplotlib.use('Agg')
import numpy as np
import polars as pl
from pyBlindOpt import GeneticAlgorithm
from sklearn.base import BaseEstimator, ClassifierMixin, clone
from sklearn.dummy import DummyClassifier
from sklearn.metrics import (balanced_accuracy_score, classification_report,
    confusion_matrix, f1_score, precision_score, recall_score, roc_auc_score)
from sklearn.model_selection import (GridSearchCV, ParameterGrid,
    RepeatedStratifiedKFold, StratifiedKFold, learning_curve, train_test_split)
from sklearn.naive_bayes import GaussianNB
from sklearn.neighbors import KNeighborsClassifier
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.tree import DecisionTreeClassifier
from sklearn.utils.validation import check_array, check_is_fitted, check_X_y
from project1_plots import (make_comparison_plot, make_diagnostics_plot,
    make_eda_plots, make_learning_plot, make_mlp_diagnostic_plot, make_pipeline_plot)

SEED = 42
MALIGNANT_LABEL = 0
INNER_FOLDS, OUTER_FOLDS, OUTER_REPEATS = 3, 5, 2
ROOT = Path(__file__).resolve().parent
DATA_PATH = ROOT / 'datasets' / 'breast_cancer.csv'
FIGURES, RESULTS = ROOT / 'figures', ROOT / 'results'
BASELINE, MLP_NAME, GA_NAME = 'Baseline - maioria', 'Connectionist - MLP', 'Evolutionary - GA'
METRICS = ('balanced_accuracy', 'malignant_recall', 'precision', 'f1', 'f1_macro', 'roc_auc')


def _json_default(value):
    if isinstance(value, np.generic): return value.item()
    if isinstance(value, np.ndarray): return value.tolist()
    if isinstance(value, Path): return str(value)
    raise TypeError(f'Tipo JSON não suportado: {type(value)}')


def write_json(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2, default=_json_default) + '\n', encoding='utf-8')


def json_parameters(parameters):
    return json.dumps(parameters, sort_keys=True, default=_json_default)


def load_data():
    """Auditoria estrutural; nenhuma transformação é ajustada ao dataset inteiro."""
    data = pl.read_csv(DATA_PATH)
    features = [column for column in data.columns if column != 'target']
    x = data.select(features).to_numpy().astype(np.float64)
    y = data['target'].to_numpy().astype(int)
    if len(features) != 30 or data.height != 569:
        raise ValueError('Esperadas 569 observações e 30 features.')
    if not np.isfinite(x).all() or data.null_count().sum_horizontal().item():
        raise ValueError('O protocolo pressupõe features numéricas sem ausentes.')
    if set(np.unique(y)) != {0, 1}:
        raise ValueError('Codificação esperada: 0 maligno; 1 benigno.')
    return data, features, x, y


class KerasMLPClassifier(ClassifierMixin, BaseEstimator):
    """MLP Keras/JAX com orçamento fixo e estado independente por clone/fold."""
    def __init__(self, hidden_units=32, l2=1e-4, epochs=80, learning_rate=0.01, seed=SEED):
        self.hidden_units, self.l2, self.epochs = hidden_units, l2, epochs
        self.learning_rate, self.seed = learning_rate, seed

    def fit(self, x, y):
        x, y = check_X_y(x, y, dtype=np.float32)
        if set(np.unique(y)) != {0, 1}:
            raise ValueError('O MLP espera ambas as classes 0 e 1.')
        if keras.backend.backend() != 'jax':
            raise RuntimeError('Executar com KERAS_BACKEND=jax.')
        keras.backend.clear_session(free_memory=False)
        keras.utils.set_random_seed(self.seed)
        self.classes_, self.n_features_in_ = np.array([0, 1]), x.shape[1]
        self.model_ = keras.Sequential([
            keras.layers.Input(shape=(self.n_features_in_,)),
            keras.layers.Dense(self.hidden_units, activation='relu',
                               kernel_regularizer=keras.regularizers.L2(self.l2)),
            keras.layers.Dense(1, activation='sigmoid'),
        ])
        self.model_.compile(optimizer=keras.optimizers.Adam(self.learning_rate),
                            loss='binary_crossentropy', jit_compile=True)
        history = self.model_.fit(x, y.astype(np.float32), epochs=self.epochs,
                                  batch_size=len(y), shuffle=False, verbose=0)
        self.loss_curve_ = np.asarray(history.history['loss'], dtype=float)
        self.n_iter_ = len(self.loss_curve_)
        # Inferência NumPy equivalente à rede treinada; snapshot de cada clone.
        self.weights_ = [np.asarray(weight).copy() for weight in self.model_.get_weights()]
        return self

    def predict_proba(self, x):
        check_is_fitted(self, 'weights_')
        x = check_array(x, dtype=np.float32)
        if x.shape[1] != self.n_features_in_: raise ValueError('Número de features diferente do treino.')
        kernel, bias, output_kernel, output_bias = self.weights_
        hidden = np.maximum(x @ kernel + bias, 0)
        logits = (hidden @ output_kernel + output_bias).ravel()
        p_one = 1 / (1 + np.exp(-np.clip(logits, -40, 40)))
        return np.column_stack([1 - p_one, p_one])

    def predict(self, x):
        probabilities = self.predict_proba(x)
        return self.classes_[np.argmax(probabilities, axis=1)]


class EvolutionaryClassifier(ClassifierMixin, BaseEstimator):
    """Evolui diretamente 30 pesos e um bias, através de pyBlindOpt."""
    def __init__(self, n_iter=18, n_pop=14, l2=0.002, seed=SEED):
        self.n_iter, self.n_pop, self.l2, self.seed = n_iter, n_pop, l2, seed

    def fit(self, x, y):
        x, y = check_X_y(x, y, dtype=np.float64)
        if set(np.unique(y)) != {0, 1}: raise ValueError('O GA espera ambas as classes 0 e 1.')
        self.classes_, self.n_features_in_, self.n_evaluations_ = np.array([0, 1]), x.shape[1], 0
        counts = np.bincount(y.astype(int), minlength=2)
        sample_weights = 0.5 / counts[y.astype(int)]

        def objective(parameters):
            self.n_evaluations_ += 1
            logits = x @ parameters[:-1] + parameters[-1]
            predicted = (logits > 0).astype(int)
            balanced_accuracy = np.sum(sample_weights * (predicted == y))
            return 1 - balanced_accuracy + self.l2 * np.mean(parameters[:-1] ** 2)

        optimizer = GeneticAlgorithm(objective=objective,
            bounds=np.tile([[-3.0, 3.0]], (x.shape[1] + 1, 1)),
            n_iter=self.n_iter, n_pop=self.n_pop, n_jobs=1, seed=self.seed,
            verbose=False, debug=True)
        self.parameters_, self.best_objective_, history = optimizer.optimize()
        self.history_ = np.column_stack(history)
        return self

    def predict_proba(self, x):
        check_is_fitted(self, 'parameters_')
        x = check_array(x, dtype=np.float64)
        if x.shape[1] != self.n_features_in_: raise ValueError('Número de features diferente do treino.')
        logits = x @ self.parameters_[:-1] + self.parameters_[-1]
        p_one = 1 / (1 + np.exp(-np.clip(logits, -40, 40)))
        return np.column_stack([1 - p_one, p_one])

    def predict(self, x):
        probabilities = self.predict_proba(x)
        return self.classes_[np.argmax(probabilities, axis=1)]


def make_models():
    return {
        'Symbolist - arvore': (
            Pipeline([('model', DecisionTreeClassifier(random_state=SEED, class_weight='balanced'))]),
            {'model__max_depth': [2, 4, None], 'model__min_samples_leaf': [1, 4]}),
        MLP_NAME: (
            Pipeline([('scale', StandardScaler()), ('model', KerasMLPClassifier())]),
            {'model__hidden_units': [16, 32], 'model__l2': [1e-4, 1e-2]}),
        GA_NAME: (
            Pipeline([('scale', StandardScaler()), ('model', EvolutionaryClassifier())]),
            {'model__n_iter': [18, 36], 'model__n_pop': [14, 24], 'model__l2': [0.002, 0.02]}),
        'Bayesian - Gaussian NB': (
            Pipeline([('scale', StandardScaler()), ('model', GaussianNB())]),
            {'model__var_smoothing': [1e-9, 1e-7, 1e-5]}),
        'Analogizer - kNN': (
            Pipeline([('scale', StandardScaler()), ('model', KNeighborsClassifier())]),
            {'model__n_neighbors': [3, 7, 15], 'model__weights': ['uniform', 'distance']}),
    }


def make_search(pipeline, grid):
    return GridSearchCV(pipeline, grid, scoring='balanced_accuracy',
        cv=StratifiedKFold(n_splits=INNER_FOLDS, shuffle=True, random_state=SEED),
        n_jobs=1, refit=True, return_train_score=True, error_score='raise')


def score_predictions(y_true, predictions, probabilities):
    """Binárias: maligno=0 positivo; F1 macro identificado separadamente."""
    matrix = confusion_matrix(y_true, predictions, labels=[0, 1])
    return {
        'balanced_accuracy': float(balanced_accuracy_score(y_true, predictions)),
        'malignant_recall': float(recall_score(y_true, predictions, pos_label=0, zero_division=0)),
        'precision': float(precision_score(y_true, predictions, pos_label=0, zero_division=0)),
        'f1': float(f1_score(y_true, predictions, pos_label=0, zero_division=0)),
        'f1_macro': float(f1_score(y_true, predictions, average='macro', zero_division=0)),
        'roc_auc': float(roc_auc_score((y_true == 0).astype(int), probabilities[:, 0])),
        'false_negatives': int(matrix[0, 1]), 'false_positives': int(matrix[1, 0]),
    }


def candidate_rows(search, model, stage):
    result = search.cv_results_
    return [{'stage': stage, 'model': model, 'params': json_parameters(parameters),
        'mean_validation_balanced_accuracy': float(result['mean_test_score'][i]),
        'std_validation_balanced_accuracy': float(result['std_test_score'][i]),
        'mean_train_balanced_accuracy': float(result['mean_train_score'][i]),
        'rank': int(result['rank_test_score'][i]),
        'mean_fit_seconds': float(result['mean_fit_time'][i])}
        for i, parameters in enumerate(result['params'])]


def summarize_folds(rows):
    summary = []
    for model in dict.fromkeys(row['model'] for row in rows):
        selected = [row for row in rows if row['model'] == model]
        item = {'model': model, 'n_folds': len(selected)}
        for metric in METRICS:
            values = [row[metric] for row in selected]
            item[f'{metric}_mean'] = float(np.mean(values))
            item[f'{metric}_std'] = float(np.std(values, ddof=1))
        summary.append(item)
    return summary


def select_best_model(cv_rows):
    """Só recebe validação: nenhum valor do teste entra nesta decisão."""
    candidates = [row for row in cv_rows if row['model'] != BASELINE]
    return min(candidates, key=lambda row: (-row['balanced_accuracy_mean'],
        row['balanced_accuracy_std'], row['model']))['model']


def nested_cross_validation(x_train, y_train, train_ids):
    outer = RepeatedStratifiedKFold(n_splits=OUTER_FOLDS, n_repeats=OUTER_REPEATS, random_state=SEED)
    rows, predictions, candidates, manifest = [], [], [], []
    first_fold = None
    for fold, (fit, validation) in enumerate(outer.split(x_train, y_train), start=1):
        print(f'Nested CV: fold {fold}/{OUTER_FOLDS * OUTER_REPEATS}', flush=True)
        for role, indices in (('fit', fit), ('validation', validation)):
            manifest.extend({'fold': fold, 'row_id': int(train_ids[i]), 'role': role} for i in indices)
        ga_parameters = None
        for name, (pipeline, grid) in make_models().items():
            started = perf_counter()
            search = make_search(pipeline, grid).fit(x_train[fit], y_train[fit])
            seconds = perf_counter() - started
            estimator = search.best_estimator_
            predicted = estimator.predict(x_train[validation])
            probability = estimator.predict_proba(x_train[validation])
            model = estimator.named_steps['model']
            rows.append({'model': name, 'fold': fold, 'repeat': (fold - 1) // OUTER_FOLDS + 1,
                'n_fit': len(fit), 'n_validation': len(validation),
                **score_predictions(y_train[validation], predicted, probability),
                'best_params': json_parameters(search.best_params_), 'search_seconds': seconds,
                'n_iter': int(getattr(model, 'n_iter_', getattr(model, 'n_iter', 0)))})
            predictions.extend({'model': name, 'fold': fold, 'row_id': int(train_ids[row]),
                'true_label': int(y_train[row]), 'predicted_label': int(predicted[j]),
                'p_malignant': float(probability[j, 0])} for j, row in enumerate(validation))
            candidates.extend(candidate_rows(search, name, f'outer_{fold:02d}'))
            if name == GA_NAME: ga_parameters = search.best_params_
        baseline = DummyClassifier(strategy='most_frequent').fit(x_train[fit], y_train[fit])
        rows.append({'model': BASELINE, 'fold': fold, 'repeat': (fold - 1) // OUTER_FOLDS + 1,
            'n_fit': len(fit), 'n_validation': len(validation),
            **score_predictions(y_train[validation], baseline.predict(x_train[validation]),
                                baseline.predict_proba(x_train[validation])),
            'best_params': '{}', 'search_seconds': 0.0, 'n_iter': 0})
        if first_fold is None: first_fold = (fit, validation, ga_parameters)
    return rows, predictions, candidates, manifest, first_fold


def tune_full_training(x_train, y_train):
    tuned, rows, candidates = {}, [], []
    for name, (pipeline, grid) in make_models().items():
        started = perf_counter()
        search = make_search(pipeline, grid).fit(x_train, y_train)
        seconds = perf_counter() - started
        tuned[name] = search.best_estimator_
        model = tuned[name].named_steps['model']
        rows.append({'model': name, 'best_params': search.best_params_,
            'best_cv_balanced_accuracy': float(search.best_score_),
            'candidates': len(ParameterGrid(grid)), 'cv_fits': len(ParameterGrid(grid)) * INNER_FOLDS,
            'seconds': seconds, 'n_iter': int(getattr(model, 'n_iter_', getattr(model, 'n_iter', 0))),
            'objective_evaluations': int(getattr(model, 'n_evaluations_', 0))})
        candidates.extend(candidate_rows(search, name, 'full_training'))
        print(f'Tuning treino completo: {name}', flush=True)
    return tuned, rows, candidates


def paired_comparisons(fold_rows, best_name):
    reference = {row['fold']: row['balanced_accuracy'] for row in fold_rows if row['model'] == best_name}
    rows = []
    for name in dict.fromkeys(row['model'] for row in fold_rows):
        if name == best_name: continue
        difference = np.array([row['balanced_accuracy'] - reference[row['fold']]
                              for row in fold_rows if row['model'] == name])
        rows.append({'model': name, 'reference': best_name, 'n_folds': len(difference),
            'mean_difference': float(difference.mean()), 'std_difference': float(difference.std(ddof=1)),
            'wins': int(np.sum(difference > 1e-12)), 'ties': int(np.sum(np.abs(difference) <= 1e-12)),
            'losses': int(np.sum(difference < -1e-12))})
    return rows


def compute_learning_curve(best_name, x_train, y_train):
    pipeline, grid = make_models()[best_name]
    sizes, train, validation = learning_curve(make_search(pipeline, grid), x_train, y_train,
        cv=StratifiedKFold(n_splits=3, shuffle=True, random_state=SEED),
        scoring='balanced_accuracy', train_sizes=[0.25, 0.5, 0.75, 1.0],
        shuffle=True, random_state=SEED, n_jobs=1, error_score='raise')
    rows = [{'train_size': int(size), 'train_mean': float(train[i].mean()),
        'train_std': float(train[i].std(ddof=1)), 'validation_mean': float(validation[i].mean()),
        'validation_std': float(validation[i].std(ddof=1))} for i, size in enumerate(sizes)]
    make_learning_plot(sizes, train, validation, FIGURES)
    return rows


def scaling_ablation(x_train, y_train):
    """Configurações fixas antes da comparação; mesmos três folds e seeds."""
    configurations = {MLP_NAME: KerasMLPClassifier(hidden_units=32, l2=1e-4),
        'Bayesian - Gaussian NB': GaussianNB(var_smoothing=1e-9),
        'Analogizer - kNN': KNeighborsClassifier(n_neighbors=7, weights='uniform')}
    splits = list(StratifiedKFold(n_splits=3, shuffle=True, random_state=SEED).split(x_train, y_train))
    rows = []
    for name, model in configurations.items():
        for scaled in (False, True):
            scores = []
            for fit, validation in splits:
                steps = [('scale', StandardScaler())] if scaled else []
                estimator = Pipeline(steps + [('model', clone(model))]).fit(x_train[fit], y_train[fit])
                scores.append(balanced_accuracy_score(y_train[validation], estimator.predict(x_train[validation])))
            rows.append({'model': name, 'scaled': scaled,
                'balanced_accuracy_mean': float(np.mean(scores)),
                'balanced_accuracy_std': float(np.std(scores, ddof=1))})
    return rows


def ga_seed_sensitivity(x_train, y_train, first_fold):
    fit, validation, parameters = first_fold
    rows = []
    for seed in (0, 21, 42, 84, 126):
        pipeline, _ = make_models()[GA_NAME]
        pipeline.set_params(**parameters, model__seed=seed)
        pipeline.fit(x_train[fit], y_train[fit])
        scores = score_predictions(y_train[validation], pipeline.predict(x_train[validation]),
                                   pipeline.predict_proba(x_train[validation]))
        rows.append({'seed': seed, 'fold': 1, **scores, 'best_params': json_parameters(parameters)})
    return rows


def audit_data(data, features, x_train, y_train, y_test):
    frame = pl.DataFrame(x_train, schema=features).to_pandas()
    correlation = frame.corr()
    pairs = [{'feature_a': a, 'feature_b': b, 'correlation': float(correlation.loc[a, b])}
             for i, a in enumerate(features) for b in features[i + 1:]]
    pairs.sort(key=lambda row: abs(row['correlation']), reverse=True)
    low, high = np.quantile(x_train, [0.25, 0.75], axis=0)
    outliers = (x_train < low - 1.5 * (high - low)) | (x_train > high + 1.5 * (high - low))
    counts = lambda y: {str(label): int(np.sum(y == label)) for label in (0, 1)}
    return {'n_rows': data.height, 'n_features': len(features),
        'class_counts': counts(data['target'].to_numpy()),
        'missing': int(data.null_count().sum_horizontal().item()),
        'duplicates': data.height - data.unique().height,
        'train_rows': len(y_train), 'test_rows': len(y_test),
        'train_class_counts': counts(y_train), 'test_class_counts': counts(y_test),
        'top_correlations': pairs[:8],
        'skewed_features': [{'feature': name, 'skewness': float(value)}
            for name, value in frame.skew().sort_values(ascending=False).head(5).items()],
        'outlier_cells': int(outliers.sum())}


def main():
    # Windows pode redirecionar stdout com CP1252; tabelas/acentos exigem UTF-8.
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    started = perf_counter()
    random.seed(SEED)
    np.random.seed(SEED)
    FIGURES.mkdir(exist_ok=True)
    RESULTS.mkdir(exist_ok=True)
    print(f'Keras {keras.__version__}; backend {keras.backend.backend()}; JAX {jax.devices()}', flush=True)
    data, features, x, y = load_data()
    train_ids, test_ids = train_test_split(np.arange(len(y)), test_size=0.2, stratify=y, random_state=SEED)
    x_train, x_test, y_train, y_test = x[train_ids], x[test_ids], y[train_ids], y[test_ids]
    pl.DataFrame([{'row_id': int(i), 'role': role, 'target': int(y[i])}
        for role, ids in (('training', train_ids), ('test', test_ids)) for i in ids]).write_csv(RESULTS / 'split_manifest.csv')
    audit = audit_data(data, features, x_train, y_train, y_test)
    make_pipeline_plot(FIGURES)
    make_eda_plots(data[train_ids.tolist()], features, FIGURES)
    folds, oof, candidates, manifest, first_fold = nested_cross_validation(x_train, y_train, train_ids)
    cv = summarize_folds(folds)
    for filename, rows in [('fold_scores.csv', folds), ('outer_predictions.csv', oof),
        ('outer_fold_manifest.csv', manifest), ('cross_validation.csv', cv)]:
        pl.DataFrame(rows).write_csv(RESULTS / filename)
    best_name = select_best_model(cv)
    selection = {'model': best_name, 'criterion': 'balanced_accuracy_mean',
        'selected_before_test': True, 'tie_break': 'menor desvio padrão; depois nome',
        'test_previously_observed': True}
    write_json(RESULTS / 'selection.json', selection)
    print(f'Selecionado pela CV antes da avaliação do teste: {best_name}', flush=True)
    make_comparison_plot(pl.DataFrame(cv), FIGURES)
    mlp = [row for row in folds if row['model'] == MLP_NAME]
    pl.DataFrame(mlp).write_csv(RESULTS / 'mlp_diagnostics.csv')
    make_mlp_diagnostic_plot(mlp, FIGURES)
    paired = paired_comparisons(folds, best_name)
    pl.DataFrame(paired).write_csv(RESULTS / 'paired_comparisons.csv')
    tuned, tuning, full_candidates = tune_full_training(x_train, y_train)
    pl.DataFrame([{**row, 'best_params': json_parameters(row['best_params'])} for row in tuning]).write_csv(RESULTS / 'tuning.csv')
    pl.DataFrame(candidates + full_candidates).write_csv(RESULTS / 'tuning_candidates.csv')
    learning = compute_learning_curve(best_name, x_train, y_train)
    pl.DataFrame(learning).write_csv(RESULTS / 'learning_curve.csv')
    ablation = scaling_ablation(x_train, y_train)
    pl.DataFrame(ablation).write_csv(RESULTS / 'scaling_ablation.csv')
    seeds = ga_seed_sensitivity(x_train, y_train, first_fold)
    pl.DataFrame(seeds).write_csv(RESULTS / 'ga_seed_sensitivity.csv')
    ga = tuned[GA_NAME].named_steps['model']
    pl.DataFrame([{'generation': i + 1, 'best_objective': float(row[0]),
        'mean_objective': float(row[1]), 'worst_objective': float(row[2])}
        for i, row in enumerate(ga.history_)]).write_csv(RESULTS / 'ga_history.csv')

    # Só agora são calculadas métricas no holdout. Seleção e modelos congelados.
    # O holdout já era conhecido da versão anterior: não é nova validação externa.
    final, cached = [], {}
    for name, estimator in tuned.items():
        predicted, probability = estimator.predict(x_test), estimator.predict_proba(x_test)
        final.append({'model': name, **score_predictions(y_test, predicted, probability)})
        cached[name] = (predicted, probability)
    baseline = DummyClassifier(strategy='most_frequent').fit(x_train, y_train)
    final.append({'model': BASELINE, **score_predictions(y_test, baseline.predict(x_test), baseline.predict_proba(x_test))})
    pl.DataFrame(final).write_csv(RESULTS / 'final_test.csv')
    predicted, probability = cached[best_name]
    make_diagnostics_plot(best_name, y_test, predicted, probability, FIGURES)
    report = classification_report(y_test, predicted, labels=[0, 1], target_names=['maligno', 'benigno'], zero_division=0)
    (RESULTS / 'best_classification_report.txt').write_text(report, encoding='utf-8')
    error_cases = [{'row_id': int(test_ids[j]), 'true_label': int(y_test[j]),
        'predicted_label': int(predicted[j]), 'p_malignant': float(probability[j, 0]),
        **{feature: float(x_test[j, features.index(feature)])
           for feature in ('mean_radius', 'mean_texture', 'mean_concavity')}}
        for j in range(len(y_test)) if y_test[j] != predicted[j]]
    schema = {'row_id': pl.Int64, 'true_label': pl.Int64, 'predicted_label': pl.Int64,
        'p_malignant': pl.Float64, 'mean_radius': pl.Float64, 'mean_texture': pl.Float64, 'mean_concavity': pl.Float64}
    pl.DataFrame(error_cases, schema=schema).write_csv(RESULTS / 'holdout_errors.csv')
    costs = [{'model': row['model'], 'fn_cost_ratio': ratio,
        'total_cost': ratio * row['false_negatives'] + row['false_positives'],
        'cost_per_100': 100 * (ratio * row['false_negatives'] + row['false_positives']) / len(y_test)}
        for row in final for ratio in (1, 5, 10)]
    pl.DataFrame(costs).write_csv(RESULTS / 'cost_scenarios.csv')
    versions = {name: importlib.metadata.version(name) for name in
        ('polars', 'numpy', 'jax', 'keras', 'scikit-learn', 'pyBlindOpt', 'matplotlib', 'seaborn', 'pandas', 'pyarrow')}
    metadata = {'student': 'Eduardo Alves', 'student_number': '104179', 'date': '2026-11-03',
        'seed': SEED, 'python': platform.python_version(), 'versions': versions,
        'platform': platform.platform(), 'jax_devices': [str(device) for device in jax.devices()],
        'elapsed_seconds': perf_counter() - started,
        'dataset_sha256': hashlib.sha256(DATA_PATH.read_bytes()).hexdigest(),
        'test_previously_observed': True}
    write_json(RESULTS / 'environment.json', metadata)
    summary = {'metadata': metadata, 'audit': audit, 'cv': cv, 'final': final,
        'tuning': tuning, 'selection': selection, 'paired': paired, 'learning': learning,
        'mlp_diagnostics': mlp, 'costs': costs, 'error_cases': error_cases,
        'ablation': ablation, 'ga_seed_sensitivity': seeds,
        'grid': [{'model': name, 'search_space': grid} for name, (_, grid) in make_models().items()]}
    write_json(RESULTS / 'run_summary.json', summary)
    print(f"\nExperimento completo: {metadata['elapsed_seconds']:.1f} s; selecionado: {best_name}", flush=True)
    print(pl.DataFrame(final))
    print(report)
    return summary


if __name__ == '__main__':
    main()
