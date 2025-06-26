#!/bin/bash

echo "🚀 A iniciar todos os serviços TaskMaster..."

# Define o REDIRECT_URI fixo se necessário (apenas para AUTH)
export REDIRECT_URI="https://wso2-gw.ua.pt/callback"

# Iniciar os serviços com supervisão simples
# Podes alterar esta lógica conforme preferires:
# Cada processo é lançado em background, e o script espera que terminem

python3 auth.py &
echo "🔐 AUTH iniciado"

python3 testNFC.py &
echo "📲 TEST NFC iniciado"

python3 consumer_nfc.py &
echo "📡 CONSUMER NFC iniciado"

# Aguarda que os processos terminem
wait
