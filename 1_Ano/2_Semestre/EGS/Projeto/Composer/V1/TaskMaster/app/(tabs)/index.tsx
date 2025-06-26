import { Image, StyleSheet, TouchableOpacity, View, Text, Alert, Platform } from 'react-native';
import { useRouter } from 'expo-router'; // Import useRouter
import ParallaxScrollView from '@/components/ParallaxScrollView';
import React, { useEffect, useState } from 'react';
import * as Notifications from 'expo-notifications';
import Constants from 'expo-constants';
import * as Device from 'expo-device';
import * as SecureStore from 'expo-secure-store';

//  Ter atenção:
//  - Backend_URL
//  - ID do projeto -> app.json/expo-> extra->eas->projectID
const BACKEND_URL = 'http://192.168.1.99:8001';


export default function HomeScreen() {
  const router = useRouter(); // Get the navigation router
  const [expoToken, setExpoToken] = useState('');
  const [status, setStatus] = useState('A iniciar...');
  const [apiKey, setApiKey] = useState('');
  const [pressCount, setPressCount] = useState(0); //só para testar -> o teste seria carregar 5 vezes num botão e ver se recebe notificação após isso



  useEffect(() => {
    console.log("--------------------------------------ATUALIZOU----------------------------------------------------------")
    async function inicializar() {
      await setup();
      //gerarApiKey se não existir
      await gerarApiKey();
  
      if (expoToken && apiKey) {
        //await enviarNotificacao(apiKey, expoToken, 'Olá!', 'Esta é uma notificação de teste'); // SÓ PARA TESTAR
      }
    }
  
    inicializar();


    // 🔔 Listener para notificação recebida com a app aberta
    const subscription = Notifications.addNotificationReceivedListener(notification => {
      Alert.alert(
        '📬 Notificação Recebida',
        `Título: ${notification.request.content.title}\nMensagem: ${notification.request.content.body}`
      );
    });

    return () => {
      subscription.remove(); // Limpa o listener ao sair
    };
  }, []);

  // Function to handle the API request using fetch
  const handleApiRequest = async () => {

  };

  async function enviarNotificacao(apiKey: string, token: string, titulo: string, mensagem: string) {
    try {
      const response = await fetch(`${BACKEND_URL}/send_notification?api_key=${apiKey}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          user_token: token,
          title: titulo,
          message: mensagem,
        }),
      });
  
      const data = await response.json();
  
      if (response.ok) {
        console.log('✅ Notificação enviada com sucesso:', data.message);
      } else {
        console.error('❌ Erro ao enviar notificação:', data.detail);
  
        // ⚠️ VERIFICAÇÃO EXTRA: API Key inválida → apaga e gera nova
        if (data.detail === "API Key inválida!") {
          await SecureStore.deleteItemAsync('api_key');
          console.log('🔑 API Key inválida removida. A gerar nova...');
          const novaKey = await gerarApiKey();
          if (novaKey && token) {
            await enviarNotificacao(novaKey, token, titulo, mensagem); // tenta de novo
          }
        }
      }
    } catch (err) {
      console.error('❌ Erro inesperado ao enviar notificação:', err);
    }
  }
  

  //----------------------SÓ-PARA-TESTE------------------
  const handleSecretButtonPress = () => {
    const newCount = pressCount + 1;
    setPressCount(newCount);
  
    if (newCount === 5) {
      // Envia notificação após 5 cliques
      if (apiKey && expoToken) {
        enviarNotificacao(apiKey, expoToken, 'Notificação', 'Carregaste 5 vezes!');
        setPressCount(0); // Reinicia o contador
      } else {
        Alert.alert('Erro', 'API Key ou Token ainda não disponível');
      }
    }
  };
  

  //-------------------AUXILIARES--NOTIFICAÇÕES---------------------------------------------------
  async function setup(): Promise<string> {
    try {
      if (!Device.isDevice) {
        Alert.alert('Erro', 'Notificações só funcionam em dispositivos reais');
        setStatus('Dispositivo não suportado');
        return '';
      }
  
      const { status: existingStatus } = await Notifications.getPermissionsAsync();
      let finalStatus = existingStatus;
  
      if (existingStatus !== 'granted') {
        const { status } = await Notifications.requestPermissionsAsync();
        finalStatus = status;
      }
  
      if (finalStatus !== 'granted') {
        Alert.alert('Permissão recusada');
        setStatus('Permissão recusada');
        return '';
      }
  
      const tokenData = await Notifications.getExpoPushTokenAsync({
        projectId: Constants.expoConfig?.extra?.eas?.projectId,
      });
  
      const token = tokenData?.data || '';
      if (token) {
        setExpoToken(token);
        setStatus('Token obtido com sucesso!');
        console.log('Expo Token:', token);
        return token;
      } else {
        setStatus('Falha ao obter token');
        return '';
      }
  
    } catch (err) {
      console.error('Erro ao configurar notificações:', err);
      setStatus('Erro inesperado');
      return '';
    }
  }
  

  //Nesta função devemos guardar as keys para não serem sempre geradas novas
  async function gerarApiKey(): Promise<string> {
    try {
      // Verifica se já existe uma key guardada
      const storedKey = await SecureStore.getItemAsync('api_key');
      if (storedKey) {
        console.log('API Key armazenada:', storedKey);
        setApiKey(storedKey);
        return storedKey;
      }
  
      // Se não existir, gera uma nova
      const response = await fetch(`${BACKEND_URL}/generate_api_key`, {
        method: 'POST',
      });
  
      if (!response.ok) {
        throw new Error('Erro ao gerar nova API Key');
      }
  
      const data = await response.json();
      await SecureStore.setItemAsync('api_key', data.api_key); // Guarda a nova key
      setApiKey(data.api_key);
      console.log('🔐 Nova API Key gerada e guardada:', data.api_key);
      return data.api_key;
    } catch (err) {
      console.error('❌ Erro ao obter API Key:', err);
      return '';
    }
  }
  
  
  //--------------------------------------------------------------------------------------------------------

  return (
    <ParallaxScrollView
      headerBackgroundColor={{ light: 'transparent', dark: 'transparent' }}
      headerImage={<Image source={require('@/assets/images/header.png')} style={styles.reactLogo} />}>
      <View style={styles.container}>
        <View style={styles.buttonGrid}>
          <TouchableOpacity style={styles.button} onPress={handleApiRequest}>
            <Image source={require('@/assets/images/completed_tasks.png')} style={styles.buttonImage} />
            <Text style={styles.buttonText}>API Request</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.button} onPress={() => router.push('/screens/selectTasksScreen')}>
            <Image source={require('@/assets/images/list_of_tasks.png')} style={styles.buttonImage} />
            <Text style={styles.buttonText}>All Tasks</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.button} onPress={() => router.push('/screens/selectTasksScreen')}>
            <Image source={require('@/assets/images/select_tasks.png')} style={styles.buttonImage} />
            <Text style={styles.buttonText}>Select Tasks</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.button} onPress={() => router.push('/screens/githubScreen')}>
            <Image source={require('@/assets/images/github.png')} style={styles.buttonImage} />
            <Text style={styles.buttonText}>GitHub</Text>
          </TouchableOpacity>
          <Text style={styles.label}>Token do Dispositivo:</Text>
            <Text style={styles.code}>
              {expoToken || 'A obter token...'}
            </Text>
            <Text style={styles.label}>API Key:</Text>
            <Text style={styles.code}>
              {apiKey || 'A obter api key...'}
            </Text>
            <TouchableOpacity style={styles.button} onPress={handleSecretButtonPress}>
              <Image source={require('@/assets/images/icon.png')} style={styles.buttonImage} />
              <Text style={styles.buttonText}>Segredo ({pressCount}/5)</Text>
            </TouchableOpacity>
        </View>
      </View>
    </ParallaxScrollView>
    
  );
}

const styles = StyleSheet.create({
  reactLogo: {
    height: 150,
    width: 420,
    bottom: 70,
    top: 50,
    left: 0,
    position: 'absolute',
    backgroundColor: '#121212',
  },
  container: {
    flex: 0.2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  label: {
    fontSize: 16,
    fontWeight: 'bold',
    marginTop: 16,
  },
  buttonGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    width: '90%',
  },
  code: {
    fontSize: 14,
    color: '#333',
    backgroundColor: '#eaeaea',
    padding: 8,
    borderRadius: 5,
    marginTop: 4,
  },
  button: {
    backgroundColor: '#03A9F4',
    paddingVertical: 10,
    paddingHorizontal: 30,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
    height: 150,
    width: '48%',
    marginBottom: 10,
    marginTop: 30,
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: 'bold',
    marginTop: 10,
    textAlign: 'center',
  },
  buttonImage: {
    width: 70,
    height: 70,
    resizeMode: 'contain',
  },
});
