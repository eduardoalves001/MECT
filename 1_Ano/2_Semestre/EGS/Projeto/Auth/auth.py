

# auth.py
import os
import sys
import base64
import datetime
import requests
import jwt
from flask import Flask, request, jsonify, redirect, session, url_for
from authlib.integrations.flask_client import OAuth
from werkzeug.security import generate_password_hash, check_password_hash
from sqlalchemy.orm import Session

# Adiciona a pasta 'nfc_system_final/nfc_backend' ao sys.path
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), 'nfc_system_final', 'nfc_backend')))

from database.database import SessionLocal
from database.models import NFCTag

# Flask setup
app = Flask(__name__)
app.secret_key = os.urandom(24)

# Configs OAuth
app.config['OAUTH_CREDENTIALS'] = {
    'client_id': os.getenv("OAUTH_CLIENT_ID"),
    'client_secret': os.getenv("OAUTH_CLIENT_SECRET"),
    'authorize_url': os.getenv("OAUTH_AUTHORIZE_URL"),
    'token_url': os.getenv("OAUTH_TOKEN_URL"),
    'userinfo_endpoint': os.getenv("OAUTH_USERINFO_URL")
}

REDIRECT_URI = os.getenv("REDIRECT_URI", "http://localhost:5000")
DATABASE_URL = os.getenv("DATABASE_URL", "postgresql://postgres:password@localhost:5432/PointSystemEGS")

# OAuth
oauth = OAuth(app)
ua_oauth = oauth.register(
    name='ua',
    client_id=app.config['OAUTH_CREDENTIALS']['client_id'],
    client_secret=app.config['OAUTH_CREDENTIALS']['client_secret'],
    authorize_url=app.config['OAUTH_CREDENTIALS']['authorize_url'],
    token_url=app.config['OAUTH_CREDENTIALS']['token_url'],
    client_kwargs={'scope': 'openid email profile'},
)

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

@app.route('/')
def home():
    code = request.args.get('code')
    if code:
        return handle_oauth_callback(code)
    if 'user' in session:
        return jsonify(session['user'])
    return redirect(url_for('login'))

def handle_oauth_callback(code):
    credentials = app.config['OAUTH_CREDENTIALS']
    auth_header = base64.b64encode(f"{credentials['client_id']}:{credentials['client_secret']}".encode()).decode()

    response = requests.post(credentials['token_url'], data={
        'grant_type': 'authorization_code',
        'code': code,
        'redirect_uri': REDIRECT_URI
    }, headers={
        'Authorization': f'Basic {auth_header}',
        'Content-Type': 'application/x-www-form-urlencoded'
    })

    if response.status_code != 200:
        return "Authorization failed", 400

    access_token = response.json().get('access_token')
    user_info = requests.get(credentials['userinfo_endpoint'], headers={
        'Authorization': f'Bearer {access_token}'
    }).json()

    db = SessionLocal()
    user = db.query(User).filter_by(email=user_info.get("email")).first()
    if not user:
        user = User(name=user_info.get("name", "Sem Nome"), email=user_info["email"], total_points=0)
        db.add(user)
        db.commit()
    session['user'] = user_info
    db.close()

    return jsonify({'message': 'Login successful!', 'access_token': access_token})

@app.route('/login')
def login():
    return ua_oauth.authorize_redirect(REDIRECT_URI)

@app.route('/callback')
def callback():
    return handle_oauth_callback(request.args.get('code'))

@app.route('/protected')
def protected():
    token = request.headers.get('Authorization')
    if not token:
        return jsonify({'message': 'Token is missing!'}), 403
    try:
        data = jwt.decode(token, app.secret_key, algorithms=['HS256'])
        return jsonify({'message': f'Welcome {data["username"]}!'}), 200
    except jwt.ExpiredSignatureError:
        return jsonify({'message': 'Token expired!'}), 403
    except jwt.InvalidTokenError:
        return jsonify({'message': 'Invalid token!'}), 403

@app.route('/logout')
def logout():
    session.pop('user', None)
    return redirect(url_for('home'))

if __name__ == '__main__':
    app.run(host="0.0.0.0", port=5000)
