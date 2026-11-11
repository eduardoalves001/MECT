# Breast Cancer Wisconsin Diagnostic

O ficheiro `breast_cancer.csv` contém as 569 observações e 30 medidas numéricas de `sklearn.datasets.load_breast_cancer()` do scikit-learn 1.6.1. Esta distribuição local é usada para tornar a execução independente da rede. Não foram selecionadas linhas, removidos outliers ou transformados valores. Os nomes das features resultam apenas da substituição dos espaços por underscores. O identificador original de cada caso não está incluído; uma referência à linha do CSV não equivale a um identificador clínico.

| Coluna/classe | Significado                                                                                        |
| ------------- | -------------------------------------------------------------------------------------------------- |
| 30 features   | Dez propriedades dos núcleos celulares, resumidas por média, erro padrão e valor extremo (`worst`) |
| `target = 0`  | Maligno: 212 observações                                                                           |
| `target = 1`  | Benigno: 357 observações                                                                           |

O CSV não tem valores ausentes nem linhas duplicadas. A ordem das linhas e os valores de `X` e `y` foram comparados com a distribuição do scikit-learn. Não existe uma coluna de identificação do paciente que permita verificar dependências entre pacientes ou construir uma validação por grupos; esta é uma limitação dos dados disponíveis.

SHA-256 do CSV entregue:

```text
9b9e3a2fe53a2264f7e756aff00ab883450186c47bfb2027b4d90ca51d23347d
```

O script de exportação explícito permite reconstruir o CSV sem acesso à rede:

```powershell
python datasets/export_dataset.py
```

Com as versões fixadas em `requirements.txt`, a exportação produz o mesmo hash. O script recusa substituir um ficheiro diferente, a menos que seja passado `--overwrite`. A exportação não é necessária para executar o projeto, pois o CSV já está incluído.
