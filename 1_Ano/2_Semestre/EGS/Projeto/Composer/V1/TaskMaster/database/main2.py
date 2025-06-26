from fastapi import FastAPI, Depends, HTTPException, status, Query
from pydantic import BaseModel
from sqlalchemy import Boolean, create_engine, Column, Integer, String, Text
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, Session
from typing import Optional
import psycopg2
from psycopg2 import sql
import uvicorn

# ==============================================
# DATABASE SETUP
# ==============================================

DB_CONFIG = {
    "user": "postgres",
    "password": "rafa",
    "host": "localhost",
    "port": "5432",
    "name": "composer"
}

def create_database():
    """Create the database if it doesn't exist"""
    try:
        conn = psycopg2.connect(
            dbname="postgres",
            user=DB_CONFIG["user"],
            password=DB_CONFIG["password"],
            host=DB_CONFIG["host"],
            port=DB_CONFIG["port"]
        )
        conn.autocommit = True
        cursor = conn.cursor()
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

# Create database
create_database()

# SQLAlchemy setup
SQLALCHEMY_DATABASE_URL = f"postgresql://{DB_CONFIG['user']}:{DB_CONFIG['password']}@{DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['name']}"
engine = create_engine(SQLALCHEMY_DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()

# ==============================================
# MODELS
# ==============================================

class User(Base):
    __tablename__ = "users"
    user_id = Column(Integer, primary_key=True, index=True)
    user_email = Column(String, unique=True, index=True)
    isProfessor = Column(Boolean, default=False)

class Quest(Base):
    __tablename__ = "quests"
    id = Column(Integer, primary_key=True, index=True)
    subject = Column(String, nullable=False)
    title = Column(String, nullable=False)
    description = Column(Text, nullable=False)
    user_id = Column(Integer, default=0)
    status = Column(String, default="notSelected")

# Create tables
Base.metadata.create_all(bind=engine)

app = FastAPI()

# Dependency
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# ==============================================
# SCHEMAS
# ==============================================

class UserCreate(BaseModel):
    user_id: int
    user_email: str
    isProfessor: bool = False

class QuestCreate(BaseModel):
    subject: str
    title: str
    description: str
    user_id: int = 0
    status: str = "pending"

class QuestUpdate(BaseModel):
    subject: str
    title: str
    description: str
    user_id: int = 0
    status: str = "pending"

# ==============================================
# USER ENDPOINTS
# ==============================================

@app.post("/users/", status_code=status.HTTP_201_CREATED)
def create_user(user: UserCreate, db: Session = Depends(get_db)):
    if db.query(User).filter(User.user_id == user.user_id).first():
        raise HTTPException(status_code=400, detail="User ID already exists")
    if db.query(User).filter(User.user_email == user.user_email).first():
        raise HTTPException(status_code=400, detail="Email already registered")

    new_user = User(
        user_id=user.user_id,
        user_email=user.user_email,
        isProfessor=user.isProfessor
    )
    db.add(new_user)
    db.commit()
    db.refresh(new_user)
    return {
        "message": "User created",
        "user": {
            "user_id": new_user.user_id,
            "user_email": new_user.user_email,
            "isProfessor": new_user.isProfessor
        }
    }

@app.get("/users/", status_code=status.HTTP_200_OK)
def list_users(db: Session = Depends(get_db)):
    users = db.query(User).all()
    return [{"user_id": user.user_id, "user_email": user.user_email, "isProfessor": user.isProfessor} for user in users]

# ==============================================
# QUEST ENDPOINTS
# ==============================================

@app.get("/quests/V1/getQuests", status_code=status.HTTP_200_OK)
def get_quests(status: Optional[str] = Query(None), db: Session = Depends(get_db)):
    """List all quests or filter by status"""
    if status:
        quests = db.query(Quest).filter(Quest.status == status).all()
    else:
        quests = db.query(Quest).all()

    return [{
        "id": q.id,
        "subject": q.subject,
        "title": q.title,
        "description": q.description,
        "user_id": q.user_id,
        "status": q.status
    } for q in quests]

@app.post("/quests/V1/createQuest", status_code=status.HTTP_201_CREATED)
def create_quest(quest: QuestCreate, db: Session = Depends(get_db)):
    new_quest = Quest(**quest.dict())
    db.add(new_quest)
    db.commit()
    db.refresh(new_quest)
    return {
        "message": "Quest created",
        "quest": {
            "id": new_quest.id,
            "subject": new_quest.subject,
            "title": new_quest.title,
            "description": new_quest.description,
            "user_id": new_quest.user_id,
            "status": new_quest.status
        }
    }

@app.put("/quests/{quest_id}", status_code=status.HTTP_200_OK)
def update_quest(quest_id: int, updated_quest: QuestUpdate, db: Session = Depends(get_db)):
    quest = db.query(Quest).filter(Quest.id == quest_id).first()
    if not quest:
        raise HTTPException(status_code=404, detail="Quest not found")

    for key, value in updated_quest.dict().items():
        setattr(quest, key, value)

    db.commit()
    db.refresh(quest)
    return {
        "message": "Quest updated",
        "quest": {
            "id": quest.id,
            "subject": quest.subject,
            "title": quest.title,
            "description": quest.description,
            "user_id": quest.user_id,
            "status": quest.status
        }
    }
@app.get("/quests/{quest_id}", status_code=status.HTTP_200_OK)
def get_quest_by_id(quest_id: int, db: Session = Depends(get_db)):
    quest = db.query(Quest).filter(Quest.id == quest_id).first()
    if not quest:
        raise HTTPException(status_code=404, detail="Quest not found")
    return {
        "id": quest.id,
        "subject": quest.subject,
        "title": quest.title,
        "description": quest.description,
        "user_id": quest.user_id,
        "status": quest.status
    }


# ==============================================
# MAIN ENTRY POINT
# ==============================================

if __name__ == "__main__":
    uvicorn.run("main2:app", host="192.168.1.99", port=8003, reload=True)
