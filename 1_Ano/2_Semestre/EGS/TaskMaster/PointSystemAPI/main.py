from fastapi import FastAPI, Depends, HTTPException, Query, File, UploadFile
from fastapi.responses import RedirectResponse, JSONResponse
import uuid
from sqlalchemy import exc, desc, func
from sqlalchemy.future import select
from sqlalchemy.orm import Session
from database import engine, SessionLocal
from pydantic import BaseModel, EmailStr
from typing import List, Optional
import models
from models import User, Point, Badge, Quest
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine
import secrets

app = FastAPI()

# Dependência para obter sessão da Base de Dados.
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# Modelos da API
class UserCreate(BaseModel):
    name: str
    email: EmailStr

class PointCreate(BaseModel):
    user_id: int
    points_change: int
    message: str

class PointHistoryResponse(BaseModel):
    points_change: int
    change_date: str

class BadgeCreate(BaseModel):
    name: str
    description: Optional[str] = None
    threshold: int
    image_filename: str

class BadgeOut(BaseModel):
    id: int
    name: str
    description: Optional[str]
    threshold: int
    image_filename: str

    class Config:
        orm_mode = True

class QuestCreate(BaseModel):
    title: str
    description: str
    points: int

class UserPointsResponse(BaseModel):
    user_id: int
    name: str
    email: str
    total_points: int
    history: List[PointHistoryResponse]

class BadgeDeleteRequest(BaseModel):
    badge_id: int

class BadgeResponse(BaseModel):
    id: int
    name: str
    description: str
    threshold: int
    image_filename: str

    class Config:
        orm_mode = True

models.Base.metadata.create_all(bind=engine)

# Redireciona a root da API para os docs para ser mais fácil aceder aos endpoints
@app.get("/")
def read_root():
    return RedirectResponse(url="/docs")

# Cria um utilizador
@app.post("/v1/users/")
def create_user(user: UserCreate, db: Session = Depends(get_db)):
    db_user = User(name=user.name, email=user.email, total_points=0)
    db.add(db_user)
    db.commit()
    return {"message": "Utilizador criado com sucesso!"}

# Atualiza um utilizador
@app.patch("/v1/users/{user_id}/")
def update_user(user_id: int, user: UserCreate, db: Session = Depends(get_db)):
    db_user = db.query(User).filter(User.id == user_id).first()

    if not db_user:
        raise HTTPException(status_code=404, detail="Utilizador não encontrado!")

    db_user.name = user.name
    db_user.email = user.email

    db.commit()

    return {"message": "Utilizador actualizado com sucesso!"}


# Remove um utilizador à escolha com base no ID de utilizador
@app.delete("/v1/users/{user_id}")
def remove_user(user_id: int, db: Session = Depends(get_db)):
    db_user = db.query(User).filter(User.id == user_id).first()
    if db_user is None:
        raise HTTPException(status_code=404, detail="Utilizador não encontrado!")
    try:
        db.delete(db_user)
        db.commit()
        return {"message": "Utilizador removido com sucesso!"}
    except exc.SQLAlchemyError:
        db.rollback()
        raise HTTPException(status_code=500, detail="Um erro ocorreu enquanto o utilizador era removido!")

# Retorna todos os utilizadores presentes na base de dados em formato de rank, atualizado após recomendação do professor de não usar um get á parte para o ranking e juntar tudo no get_users
@app.get("/v1/users/")
def get_users(db: Session = Depends(get_db)):
    result = db.execute(
        select(
            User.id,
            User.name,
            User.total_points,
            User.email,
            Badge.name.label("badge_name")
        )
        .join(Point, User.id == Point.user_id, isouter=True)
        .join(Badge, User.current_badge_id == Badge.id, isouter=True)
        .group_by(User.id, User.name, User.total_points, User.email, Badge.name)
        .order_by(desc(User.total_points))
    )

    users = result.fetchall()

    if not users:
        return {"ranking": []}

    ranking = [
        {
            "rank": i + 1,
            "user_id": user[0],
            "name": user[1],
            "email": user[3],
            "total_points": user[2],
            "badge": user[4] if user[4] else None
        }
        for i, user in enumerate(users)
    ]

    return {"ranking": ranking}




# Adiciona pontos ao utilizador
@app.post("/v1/users/{user_id}/points/")
def add_points(user_id: int, points: int, message: str, db: Session = Depends(get_db)):
    if points <= 0:
        raise HTTPException(status_code=400, detail="Pontos a atribuir têm que ter um valor positivo")
    
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="Utilizador não encontrado!")
    
    new_point = Point(user_id=user_id, points_change=points, message=message)
    db.add(new_point)
    user.total_points += points
    db.commit()
    
    return {"message": "Pontos atribuidos com sucesso!", "total_points": user.total_points}


# Remove pontos do utilizador
@app.delete("/v1/users/{user_id}/points/")
def remove_points(user_id: int, points: int, message: str, db: Session = Depends(get_db)):
    if points <= 0:
        raise HTTPException(status_code=400, detail="Pontos a remover têm que ter um valor positivo")
    
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="Utilizador não encontrado!")
    
    new_point = Point(user_id=user_id, points_change=-abs(points), message=message)
    db.add(new_point)
    user.total_points = max(0, user.total_points - abs(points))
    db.commit()
    
    return {"message": "Pontos removidos com sucesso!", "total_points": user.total_points}

# Histórico de pontos de um utilizador, onde se sabe quantos pontos recebou ou lhe foram retirados e em que dia.
@app.get("/v1/points/history/{user_id}")
def get_user_points_history(
    user_id: int,
    db: Session = Depends(get_db),
    skip: int = Query(0, ge=0),  # Quantos valores queremos passar a frente, ou seja se o skip for 10 e tivermos 100 resultados. Aparecem do resultado 10 ao 100 (dá skip ao 1 a 10)
    limit: int = Query(10, ge=1, le=100)  # Quantidade de resultados por página
):
    try:
        user = db.query(User).filter(User.id == user_id).first()
        if not user:
            raise HTTPException(status_code=404, detail="Utilizador não encontrado!")

        # Obter todos os resultados antes da paginação
        total_results = db.query(Point).filter(Point.user_id == user_id).count()

        # Fetch paginated history
        history = (
            db.query(Point)
            .filter(Point.user_id == user_id)
            .order_by(desc(Point.change_date))
            .offset(skip)
            .limit(limit)
            .all()
        )

        history_data = [
            {"points_change": p.points_change, "change_date": p.change_date.isoformat(), "message": p.message} 
            for p in history
        ]

        return {
            "user_id": user.id,
            "name": user.name,
            "email": user.email,
            "total_points": user.total_points,
            "total_results": total_results,
            "history": history_data,
            "pagination": {
                "skip": skip,
                "limit": limit,
                "remaining": max(0, total_results - (skip + limit))
            }
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Internal Server Error: {str(e)}")
    
@app.post("/v1/generate-api-key")
def generate_api_key(user_id: int, db: Session = Depends(get_db)):
    # Gera uma nova chave de API para um utilizador, substituindo a anterior.

    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="Utilizador não encontrado.")

    new_api_key = secrets.token_hex(32)  # Gera uma chave segura
    user.api_key = new_api_key  # Substitui a chave antiga
    db.commit()
    db.refresh(user)

    return {"api_key": new_api_key}

@app.get("/v1/validate-api-key")
async def validate_api_key(api_key: str, db: Session = Depends(get_db)):
    
    # Verifica se a chave de API é válida e retorna o ID do utilizador correspondente.
    
    user = db.query(User).filter(User.api_key == api_key).first()
    if not user:
        raise HTTPException(status_code=401, detail="API key inválida.")

    return {"valid": True, "user_id": user.id}


@app.post("/v1/badge")
async def create_badge(
    name: str,
    threshold: int,
    image_filename: str,
    description: str = "",
    db: Session = Depends(get_db)
):
    existing = db.query(Badge).filter(Badge.name == name).first()
    if existing:
        raise HTTPException(status_code=400, detail="Badge com este nome já existe.")

    badge = Badge(
        name=name,
        description=description,
        threshold=threshold,
        image_filename=image_filename,
    )
    db.add(badge)
    db.commit()
    db.refresh(badge)

    return {
        "message": "Badge criado com sucesso!",
        "badge_id": badge.id
    }

@app.post("/v1/badge/assign")
async def assign_badges(db: Session = Depends(get_db)):
    # Fetch all badges ordered from highest to lowest threshold
    badges = db.query(Badge).order_by(Badge.threshold.desc()).all()
    if not badges:
        raise HTTPException(status_code=404, detail="Nenhum badge encontrado.")

    users = db.query(User).all()
    if not users:
        raise HTTPException(status_code=404, detail="Nenhum utilizador encontrado.")

    updated_users = []

    for user in users:
        assigned = False
        for badge in badges:
            if user.total_points >= badge.threshold:
                if user.current_badge_id != badge.id:
                    user.current_badge_id = badge.id
                    updated_users.append({
                        "user_id": user.id,
                        "name": user.name,
                        "new_badge": badge.name
                    })
                assigned = True
                break  # Badge encontrado, parar de procurar
        if not assigned and user.current_badge_id is not None:
            # Se o utilizador não tem pontos para nenhum badge, remove
            user.current_badge_id = None

    db.commit()

    return {
        "message": "Badges atribuídos com base nos pontos.",
        "updated": updated_users
    }


@app.delete("/v1/badges/{badge_id}")
def remove_badge(badge_id: int, db: Session = Depends(get_db)):
    db_badge = db.query(Badge).filter(Badge.id == badge_id).first()
    if db_badge is None:
        raise HTTPException(status_code=404, detail="Badge não encontrado!")
    try:
        db.delete(db_badge)
        db.commit()
        return {"message": "Badge removido com sucesso!"}
    except exc.SQLAlchemyError:
        db.rollback()
        raise HTTPException(status_code=500, detail="Um erro ocorreu enquanto o badge era removido!")

@app.get("/v1/badges", response_model=List[BadgeResponse])
async def list_badges(db: Session = Depends(get_db)):
    badges = db.query(Badge).all()
    return badges


@app.post("/v1/quests/")
def create_quest(quest: QuestCreate, user_id: int, db: Session = Depends(get_db)):
    db_user = db.query(User).filter(User.id == user_id).first()
    if not db_user:
        raise HTTPException(status_code=404, detail="Utilizador não encontrado!")

    new_quest = Quest(
        title=quest.title,
        description=quest.description,
        points=quest.points,
        user_id=user_id
    )

    db.add(new_quest)
    db.commit()
    db.refresh(new_quest)

    return {"message": "Quest criada com sucesso!", "quest_id": new_quest.id}

@app.post("/v1/quests/{quest_id}/complete")
def complete_quest(quest_id: int, db: Session = Depends(get_db)):
    quest = db.query(Quest).filter(Quest.id == quest_id).first()
    if not quest:
        raise HTTPException(status_code=404, detail="Quest não encontrada!")
    
    if quest.completed:
        raise HTTPException(status_code=400, detail="A quest já foi completada!")

    # Marcar a quest como completada
    quest.completed = True
    db.commit()

    # Atribuir os pontos ao utilizador
    user = db.query(User).filter(User.id == quest.user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="Utilizador não encontrado!")
    
    user.total_points += quest.points
    db.commit()

    return {"message": f"Quest '{quest.title}' concluída com sucesso! {quest.points} pontos atribuídos."}


@app.get("/v1/quests/user/{user_id}")
def get_quests_by_user(user_id: int, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="Utilizador não encontrado!")

    quests = db.query(Quest).filter(Quest.user_id == user_id).all()

    return [
        {
            "id": quest.id,
            "title": quest.title,
            "description": quest.description,
            "points": quest.points,
            "completed": quest.completed,
        }
        for quest in quests
    ]
