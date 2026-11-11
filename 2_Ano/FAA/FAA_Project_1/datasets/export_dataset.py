"""Exporta, sem rede, a cópia scikit-learn do dataset UCI WDBC."""

import argparse
import hashlib
from pathlib import Path

import polars as pl
from sklearn.datasets import load_breast_cancer


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--overwrite', action='store_true', help='Permite substituir um CSV com conteúdo diferente.')
    args = parser.parse_args()
    bundle = load_breast_cancer()
    columns = [name.replace(' ', '_') for name in bundle.feature_names]
    frame = pl.DataFrame(bundle.data, schema=columns).with_columns(pl.Series('target', bundle.target))
    content = frame.write_csv().encode('utf-8')
    output = Path(__file__).resolve().parent / 'breast_cancer.csv'
    if output.exists() and output.read_bytes() != content and not args.overwrite:
        parser.error('O CSV existente difere da exportação; inspecione-o antes de usar --overwrite.')
    if not output.exists() or output.read_bytes() != content:
        output.write_bytes(content)
    print(f'{output.name}: {frame.height} linhas, {len(columns)} features')
    print(f'SHA256: {hashlib.sha256(content).hexdigest()}')


if __name__ == '__main__':
    main()
