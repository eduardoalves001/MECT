
from kafka import KafkaProducer
import json
import time

producer = KafkaProducer(
    bootstrap_servers='localhost:9092',
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

# Simular leitura de tags NFC no telemóvel
tags = [
    {"tag_id": "ABC123", "user_name": "Gabriel Couto", "user_email": "gabrielcouto@ua.pt"},
    {"tag_id": "XYZ456", "user_name": "Joana Silva", "user_email": "joana@ua.pt"},
]

for tag in tags:
    print(f"📡 Enviando tag: {tag}")
    producer.send('nfc_topic', tag)
    time.sleep(2)
