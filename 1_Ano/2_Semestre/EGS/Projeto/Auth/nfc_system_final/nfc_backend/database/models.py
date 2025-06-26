
from sqlalchemy import Column, Integer, String
from database.database import Base

class NFCTag(Base):
    __tablename__ = "nfc_tags"

    id = Column(Integer, primary_key=True, index=True)
    tag_id = Column(String, unique=True, index=True, nullable=False)
    user_name = Column(String, nullable=False)
    user_email = Column(String, nullable=False)
