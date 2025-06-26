
# 📲 Sistema de Leitura de NFC com Kafka e PostgreSQL

Este projeto permite a leitura de **tags NFC** (por exemplo, de cartões universitários), envio desses dados para o **Kafka**, e armazenamento na base de dados **PostgreSQL** com mapeamento para utilizadores.

---

## 🔧 Tecnologias Utilizadas

- Python (Flask)
- Kafka (`kafka-python`)
- PostgreSQL
- SQLAlchemy
- React Native (Flutter app pode ser adicionado)
- Docker (para Kafka/Zookeeper)

---

## 📁 Estrutura do Projeto

```
nfc_system_complete/
├── nfc_reader_flutter/             # App móvel para leitura de NFC (placeholder)
├── nfc_backend/
│   ├── kafka/
│   │   ├── producer_nfc.py         # Simula envio de tags NFC
│   │   └── consumer_nfc.py         # Consome mensagens do Kafka e grava na DB
│   └── database/
│       ├── database.py             # Configuração SQLAlchemy
│       └── models.py               # Tabela NFCTag
```

---

## ⚙️ Setup da Base de Dados

1. Criar base de dados PostgreSQL com nome:

```sql
CREATE DATABASE PointSystemEGS;
```

2. Ativar virtualenv e instalar dependências:

```bash
python3 -m venv venv
source venv/bin/activate
pip install kafka-python sqlalchemy psycopg2-binary
```

3. Criar tabelas (usando `models.py`):

```python
from database.database import Base, engine
from database.models import NFCTag

Base.metadata.create_all(bind=engine)
```

---

## 🛰️ Kafka com Docker

Cria um `docker-compose.yml` com Kafka + Zookeeper:

```yaml
version: '3'
services:
  zookeeper:
    image: wurstmeister/zookeeper
    ports:
      - "2181:2181"
  kafka:
    image: wurstmeister/kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092
    depends_on:
      - zookeeper
```

Depois corre:

```bash
docker compose up -d
```

---

## ▶️ Executar o Sistema

### 1. Producer (simula leitura NFC):

```bash
python3 kafka/producer_nfc.py
```

### 2. Consumer (ouve Kafka e grava na DB):

```bash
python3 kafka/consumer_nfc.py
```

---

## 🧠 Como Funciona

1. Um leitor NFC (telemóvel ou outro) lê a tag de um cartão.
2. A tag é enviada para o Kafka (via `producer_nfc.py`)
3. O `consumer_nfc.py` lê a tag, associa-a ao utilizador (nome/email) e guarda-a na base de dados `PointSystemEGS`.

---

## 🗂️ Exemplo de Dados Guardados

| tag_id | user_name      | user_email         |
|--------|----------------|--------------------|
| ABC123 | Gabriel Couto  | gabriel@ua.pt      |
| XYZ456 | Joana Silva    | joana@ua.pt        |

---

## 📬 Dúvidas ou sugestões?

Contacta o autor do projeto para mais detalhes ou melhorias.
