import os
import sys
import json
import time
from kafka import KafkaConsumer
from kafka.errors import NoBrokersAvailable
from sqlalchemy.orm import Session

# Adiciona o path correto para importar os modelos
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '../')))

from database.database import SessionLocal
from database.models import NFCTag

KAFKA_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "kafka_nfc:9092")

# Espera ativa para o broker Kafka estar disponível
for attempt in range(30):
    try:
        consumer = KafkaConsumer(
            'nfc_topic',
            bootstrap_servers=KAFKA_SERVERS,
            auto_offset_reset='earliest',
            value_deserializer=lambda m: json.loads(m.decode('utf-8')),
            group_id='nfc_group'
        )
        print("🛰️ A ouvir o tópico 'nfc_topic'...")
        break
    except NoBrokersAvailable:
        print(f"⚠️ Kafka ainda não disponível em {KAFKA_SERVERS}... tentativa {attempt + 1}/10")
        time.sleep(5)
else:
    print("❌ Kafka não respondeu após 30 tentativas. A sair.")
    sys.exit(1)

def save_tag_to_db(tag_data):
    db: Session = SessionLocal()
    tag_id = str(tag_data["tag_id"])

    tag_exists = db.query(NFCTag).filter_by(tag_id=tag_id).first()
    if not tag_exists:
        tag = NFCTag(
            tag_id=tag_id,
            user_name=tag_data.get("user_name", "Desconhecido"),
            user_email=tag_data.get("user_email", "Desconhecido")
        )
        db.add(tag)
        db.commit()
        print(f"✅ Tag guardada: {tag.tag_id}")
    else:
        print(f"⚠️ Tag já existente: {tag_id}")
    db.close()

for message in consumer:
    tag_data = message.value
    print(f"📥 Tag recebida: {tag_data}")
    save_tag_to_db(tag_data)
