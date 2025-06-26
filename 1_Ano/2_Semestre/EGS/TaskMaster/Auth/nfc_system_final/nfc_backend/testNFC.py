import os
import json
from flask import Flask, request, jsonify
from kafka import KafkaProducer

app = Flask(__name__)

producer = KafkaProducer(
    bootstrap_servers=os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

@app.route('/nfc', methods=['POST'])
def receive_nfc_tag():
    print("📥 Headers recebidos:", dict(request.headers))
    print("📥 Content-Type:", request.content_type)
    print("📥 Raw body:", request.data)

    if request.content_type and request.content_type.startswith("application/json"):
        data = request.get_json(force=True, silent=True)
        if data is None:
            return jsonify({'error': 'Corpo JSON inválido'}), 400
        print(f"📡 Tag recebida via HTTP: {data}")
        producer.send('nfc_topic', value=data)
        return jsonify({'message': 'Tag recebida e enviada para Kafka'}), 200
    else:
        return jsonify({'error': 'Unsupported Content-Type'}), 415

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
