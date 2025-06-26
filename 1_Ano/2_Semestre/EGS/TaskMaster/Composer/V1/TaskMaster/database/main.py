from fastapi import FastAPI, Depends, HTTPException, status
from sqlalchemy import create_engine, Column, Integer, String, Boolean, DateTime
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, Session
from pydantic import BaseModel
import secrets
from datetime import datetime, timedelta
from typing import Optional
import psycopg2
from psycopg2 import sql
import uvicorn

# ==============================================
# DATABASE SETUP
# ==============================================

# Database configuration
DB_CONFIG = {
    "user": "postgres",
    "password": "rafa",  # Change to your PostgreSQL password
    "host": "localhost",
    "port": "5432",
    "name": "api_key_db"
}

def create_database():
    """Create the database if it doesn't exist"""
    try:
        # Connect to default postgres database
        conn = psycopg2.connect(
            dbname="postgres",
            user=DB_CONFIG["user"],
            password=DB_CONFIG["password"],
            host=DB_CONFIG["host"],
            port=DB_CONFIG["port"]
        )
        conn.autocommit = True
        cursor = conn.cursor()
        
        # Check if database exists
        cursor.execute("SELECT 1 FROM pg_database WHERE datname = %s", (DB_CONFIG["name"],))
        exists = cursor.fetchone()
        
        if not exists:
            cursor.execute(sql.SQL("CREATE DATABASE {}").format(sql.Identifier(DB_CONFIG["name"])))
            print(f"Database {DB_CONFIG['name']} created successfully")
        
        cursor.close()
        conn.close()
    except Exception as e:
        print(f"Error creating database: {e}")
        raise

# Create the database first
create_database()

# SQLAlchemy setup
SQLALCHEMY_DATABASE_URL = f"postgresql://{DB_CONFIG['user']}:{DB_CONFIG['password']}@{DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['name']}"
engine = create_engine(SQLALCHEMY_DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()

# ==============================================
# MODELS
# ==============================================

class APIKey(Base):
    __tablename__ = "api_keys"

    id = Column(Integer, primary_key=True, index=True)
    key = Column(String(64), unique=True, index=True)
    name = Column(String(100), nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    expires_at = Column(DateTime, nullable=True)
    is_active = Column(Boolean, default=True)
    user_id = Column(Integer, nullable=True)

# Create tables
Base.metadata.create_all(bind=engine)

# ==============================================
# PYDANTIC SCHEMAS
# ==============================================

class APIKeyCreate(BaseModel):
    name: Optional[str] = None
    expires_in_days: Optional[int] = None
    user_id: Optional[int] = None

class APIKeyResponse(BaseModel):
    id: int
    key: str
    name: Optional[str]
    created_at: datetime
    expires_at: Optional[datetime]
    is_active: bool
    user_id: Optional[int]

class APIKeyValidationResponse(BaseModel):
    valid: bool
    name: Optional[str]
    user_id: Optional[int]
    expires_at: Optional[datetime]
    is_active: bool

# ==============================================
# FASTAPI APP
# ==============================================

app = FastAPI()

# Dependency
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# ==============================================
# ENDPOINTS
# ==============================================

@app.post("/api-keys/", response_model=APIKeyResponse, status_code=status.HTTP_201_CREATED)
def create_api_key(key_data: APIKeyCreate, db: Session = Depends(get_db)):
    """Generate and store a new API key"""
    # Generate secure API key
    api_key = secrets.token_urlsafe(32)
    
    # Calculate expiration if needed
    expires_at = None
    if key_data.expires_in_days:
        expires_at = datetime.utcnow() + timedelta(days=key_data.expires_in_days)
    
    # Create database record
    db_key = APIKey(
        key=api_key,
        name=key_data.name,
        expires_at=expires_at,
        user_id=key_data.user_id
    )
    
    db.add(db_key)
    db.commit()
    db.refresh(db_key)
    
    return db_key

@app.get("/api-keys/validate/", response_model=APIKeyValidationResponse)
def validate_api_key(api_key: str, db: Session = Depends(get_db)):
    """Validate an existing API key"""
    db_key = db.query(APIKey).filter(APIKey.key == api_key).first()
    
    if not db_key:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="API key not found"
        )
    
    is_valid = True
    if not db_key.is_active:
        is_valid = False
    elif db_key.expires_at and db_key.expires_at < datetime.utcnow():
        is_valid = False
    
    return {
        "valid": is_valid,
        "name": db_key.name,
        "user_id": db_key.user_id,
        "expires_at": db_key.expires_at,
        "is_active": db_key.is_active
    }

@app.post("/api-keys/{key_id}/revoke/", status_code=status.HTTP_200_OK)
def revoke_api_key(key_id: int, db: Session = Depends(get_db)):
    """Revoke an API key by marking it inactive"""
    db_key = db.query(APIKey).filter(APIKey.id == key_id).first()
    
    if not db_key:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="API key not found"
        )
    
    db_key.is_active = False
    db.commit()
    
    return {"message": "API key revoked successfully"}

@app.get("/api-keys/", response_model=list[APIKeyResponse])
def list_api_keys(db: Session = Depends(get_db)):
    """List all API keys"""
    return db.query(APIKey).order_by(APIKey.created_at.desc()).all()

# ==============================================
# MAIN ENTRY POINT
# ==============================================

if __name__ == "__main__":
    uvicorn.run("main:app", host=" 10.236.227.2", port=8001, reload=True)