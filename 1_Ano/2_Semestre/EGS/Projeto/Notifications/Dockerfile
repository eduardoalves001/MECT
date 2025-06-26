# Base image com Python
FROM python:3.11

# Define diretório de trabalho dentro do container
WORKDIR /app

# Copia os ficheiros para o container
COPY . .

# Instala dependências
RUN pip install fastapi uvicorn requests firebase-admin

# Porta onde o serviço vai correr
EXPOSE 8000

# Comando para correr a API
CMD ["uvicorn", "api_notifications:app", "--host", "0.0.0.0", "--port", "8000"]
