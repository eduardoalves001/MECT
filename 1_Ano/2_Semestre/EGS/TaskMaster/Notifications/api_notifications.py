from fastapi import FastAPI, HTTPException, Depends
from pydantic import BaseModel
import requests
import smtplib
import secrets
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

EMAIL_SMTP = "smtp.gmail.com"
EMAIL_PORT = 587
EMAIL_USER = "notifications.email2025@gmail.com"
EMAIL_PASSWORD = "uepq nybn zvji cvpb"

app = FastAPI()

# 🔹 Simulação de Base de Dados para armazenar API Keys
api_keys_db = {}

# 🔹 Simulação de Base de Dados de Tokens Expo
tokens_ativos = set()

# 🔹 Geração de nova API Key
def gerar_api_key():
    return secrets.token_hex(32)

# 🔹 Validação da API Key
def validar_api_key(api_key: str):
    if api_key not in api_keys_db:
        raise HTTPException(status_code=401, detail="API Key inválida!")

# 🔹 Modelos de dados
class APIKeyResponse(BaseModel):
    api_key: str

class NotificationRequest(BaseModel):
    user_token: str
    title: str
    message: str

class EmailRequest(BaseModel):
    to_email: str
    subject: str
    body: str

class TokenRequest(BaseModel):
    user_token: str

# 🔹 Endpoint para gerar API Key
@app.post("/generate_api_key", response_model=APIKeyResponse)
def generate_api_key():
    new_api_key = gerar_api_key()
    api_keys_db[new_api_key] = True
    return {"api_key": new_api_key}

# 🔹 Novo Endpoint: Enviar notificação via Expo Push API
@app.post("/send_notification")
def send_expo_notification(notification: NotificationRequest, api_key: str = Depends(validar_api_key)):
    expo_api_url = "https://exp.host/--/api/v2/push/send"
    headers = {
        "Accept": "application/json",
        "Accept-Encoding": "gzip, deflate",
        "Content-Type": "application/json"
    }
    payload = {
        "to": notification.user_token,
        "title": notification.title,
        "body": notification.message
    }

    response = requests.post(expo_api_url, json=payload, headers=headers)

    if response.status_code == 200:
        return {"message": "Notificação Expo enviada com sucesso"}
    else:
        raise HTTPException(status_code=500, detail=f"Erro ao enviar notificação: {response.text}")

# 🔹 Endpoint para envio de emails
@app.post("/send_email")
def send_email(email_request: EmailRequest, api_key: str = Depends(validar_api_key)):
    try:
        msg = MIMEMultipart()
        msg["From"] = EMAIL_USER
        msg["To"] = email_request.to_email
        msg["Subject"] = email_request.subject
        msg.attach(MIMEText(email_request.body, "plain"))

        server = smtplib.SMTP(EMAIL_SMTP, EMAIL_PORT)
        server.starttls()
        server.login(EMAIL_USER, EMAIL_PASSWORD)
        server.sendmail(EMAIL_USER, email_request.to_email, msg.as_string())
        server.quit()

        return {"message": f"Email enviado para {email_request.to_email} com sucesso!"}

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Erro ao enviar email: {str(e)}")

# 🔹 Endpoint para registar tokens de utilizadores
@app.post("/register_token")
def register_token(request: TokenRequest, api_key: str = Depends(validar_api_key)):
    tokens_ativos.add(request.user_token)
    return {"message": "Token registado com sucesso!"}
