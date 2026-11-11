PYTHON ?= python

ifeq ($(OS),Windows_NT)
VENV_PYTHON = .venv/Scripts/python.exe
else
VENV_PYTHON = .venv/bin/python
endif

.PHONY: venv run check

# Requer GNU Make e Python 3.11. O README inclui os comandos diretos para Windows.
venv:
	$(PYTHON) -m venv .venv
	$(VENV_PYTHON) -m pip --isolated install --no-user --requirement requirements.txt

run:
	$(VENV_PYTHON) project1.py

check:
	$(VENV_PYTHON) -m compileall -q project1.py project1_plots.py datasets/export_dataset.py
	$(VENV_PYTHON) -m pip check
