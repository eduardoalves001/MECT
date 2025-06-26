import sys
import os

# Adiciona o path da pasta PointSystemAPI para importar os módulos de lá
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '../PointSystemAPI')))

from database import Base, engine
from models import NFCTag  # importa aqui o User para garantir que é registado

# Cria as tabelas
Base.metadata.create_all(bind=engine)

print("✅ Tabelas criadas com sucesso!")