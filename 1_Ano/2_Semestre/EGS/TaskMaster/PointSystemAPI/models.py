from sqlalchemy import Column, Integer, String, ForeignKey, TIMESTAMP, func, Text, Boolean
from sqlalchemy.orm import relationship
from database import Base
from datetime import datetime

class User(Base):
    __tablename__ = 'users'

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    email = Column(String, unique=True, nullable=False)
    total_points = Column(Integer, default=0, nullable=False)
    api_key = Column(String, unique=True, nullable=True)
    current_badge_id = Column(Integer, ForeignKey("badges.id"), nullable=True)


    # Relação que relaciona com o historial de pontos
    points_history = relationship("Point", back_populates="user", cascade="all, delete-orphan")
    current_badge = relationship("Badge", lazy="joined")
    quests = relationship("Quest", back_populates="user")

class Point(Base):
    __tablename__ = 'points'

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    points_change = Column(Integer, nullable=False)
    change_date = Column(TIMESTAMP, default=datetime.utcnow, server_default=func.now())
    message = Column(String, nullable=True)

    # Relação que relaciona com o utilizador
    user = relationship("User", back_populates="points_history")

class Badge(Base):
    __tablename__ = 'badges'

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, unique=True, nullable=False)
    description = Column(String, nullable=True)
    image_filename = Column(String, nullable=False)
    threshold = Column(Integer, nullable=True)  # Número de pontos necessários para ganhar um badge

class Quest(Base):
    __tablename__ = "quests"

    id = Column(Integer, primary_key=True, index=True)
    title = Column(String, index=True)
    description = Column(Text)
    points = Column(Integer)  # Pontos a serem atribuídos
    completed = Column(Boolean, default=False)  # Se a quest foi completada

    user_id = Column(Integer, ForeignKey("users.id"))  # Relacionamento com o utilizador
    user = relationship("User", back_populates="quests")



