import 'package:flutter/material.dart';
import 'package:flutter_nfc_kit/flutter_nfc_kit.dart';
import 'package:http/http.dart' as http;

void main() => runApp(const NFCSenderApp());

class NFCSenderApp extends StatelessWidget {
  const NFCSenderApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(home: NFCReaderPage());
  }
}

// ✅ URL do backend via ngrok
String backendUrl = ' https://dee2-2001-818-eb2c-be00-3346-c7c4-36eb-a59e.ngrok-free.app ';

class NFCReaderPage extends StatefulWidget {
  const NFCReaderPage({super.key});

  @override
  State<NFCReaderPage> createState() => _NFCReaderPageState();
}

class _NFCReaderPageState extends State<NFCReaderPage> {
  String _status = "Aproxime uma tag NFC...";

  Future<void> _readAndSendTag() async {
    try {
      setState(() => _status = "A ler a tag...");
      final tag = await FlutterNfcKit.poll();
      final tagId = tag.id;
      setState(() => _status = "Tag lida: $tagId");

      final response = await http.post(
        Uri.parse(backendUrl),
        headers: {'Content-Type': 'application/json'},
        body: '{"tag_id": "$tagId"}',
      );

      setState(() => _status = "Tag enviada! Resposta: ${response.statusCode}");
      await FlutterNfcKit.finish();
    } catch (e) {
      setState(() => _status = "Erro: $e");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Center(
        child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
          Text(_status, style: const TextStyle(color: Colors.white)),
          const SizedBox(height: 20),
          ElevatedButton(
            onPressed: _readAndSendTag,
            child: const Text("Ler e Enviar Tag"),
          )
        ]),
      ),
    );
  }
}
