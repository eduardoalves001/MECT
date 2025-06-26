import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, Alert } from 'react-native';
import { ProgressBar } from 'react-native-paper';
import * as Notifications from 'expo-notifications';
import Constants from 'expo-constants';

const BACKEND_URL = 'http://192.168.1.190:8003';
const api_key = "e93ab6df869a0d1a3c86eeb47e54e52daa7aad097203a340bdf8d0c25fa6fca0";

type Reward = {
  id: string;
  title: string;
  progress: number;
  notifications: number;
  description: string;
  user_id?: number;
  status: string;
};

const RewardsScreen = () => {
  const [expandedReward, setExpandedReward] = useState<string | null>(null);
  const [expoToken, setExpoToken] = useState('');
  const [apiKey, setApiKey] = useState('');
  const [rewards, setRewards] = useState<Reward[]>([]);

  useEffect(() => {
    async function setup() {
      const { status: existingStatus } = await Notifications.getPermissionsAsync();
      let finalStatus = existingStatus;
      if (existingStatus !== 'granted') {
        const { status } = await Notifications.requestPermissionsAsync();
        finalStatus = status;
      }

      if (finalStatus === 'granted') {
        const tokenData = await Notifications.getExpoPushTokenAsync({
          projectId: Constants.expoConfig?.extra?.eas?.projectId,
        });
        setExpoToken(tokenData.data);
      }

      setApiKey(api_key);
      getQuests();
    }

    setup();
  }, []);

  const toggleExpand = (rewardId: string) => {
    setExpandedReward(expandedReward === rewardId ? null : rewardId);
  };

  const enviarNotificacao = async (apiKey: string, token: string, titulo: string, mensagem: string) => {
    try {
      const response = await fetch(`${BACKEND_URL}/send_notification?api_key=${apiKey}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
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
      }
    } catch (err) {
      console.error('❌ Erro inesperado ao enviar notificação:', err);
    }
  };

  const redeemReward = async (rewardTitle: string) => {
    if (apiKey && expoToken) {
      await enviarNotificacao(apiKey, expoToken, '🎉 Reward Redeemed!', `You redeemed: ${rewardTitle}`);
    } else {
      Alert.alert('Aviso', 'API Key ou Token não estão disponíveis');
    }
  };

  const toggleProgress = (rewardId: string) => {
    setRewards((prevRewards) =>
      prevRewards.map((r) =>
        r.id === rewardId
          ? {
              ...r,
              progress: r.progress === 1 ? 0.5 : 1,
            }
          : r
      )
    );
  };

  const getQuests = async () => {
    try {
      const response = await fetch(`${BACKEND_URL}/quests/V1/getQuests`);
      const data = await response.json();
      console.log('Quests:', data);

      const formatted = data.map((quest: any) => ({
        id: String(quest.id),
        title: quest.subject, // ✅ use subject instead of title
        progress: quest.status === 'completed' ? 1 : 0,
        notifications: 0,
        description: quest.description,
        user_id: quest.user_id,
        status: quest.status === 'created' ? 'in progress' : quest.status, // Set status to 'in progress' if it's 'created'
      }));

      setRewards(formatted);
    } catch (error) {
      console.error('Error fetching quests:', error);
    }
  };

  const renderRewardItem = ({ item }: { item: Reward }) => (
    <View style={styles.rewardCard}>
      <TouchableOpacity onPress={() => toggleExpand(item.id)}>
        <View style={styles.rewardHeader}>
          <Text style={styles.rewardTitle}>{item.title}</Text>
          {item.notifications > 0 && (
            <View style={styles.notificationBadge}>
              <Text style={styles.notificationText}>{item.notifications}</Text>
            </View>
          )}
        </View>
      </TouchableOpacity>
  
      {expandedReward === item.id && (
        <View style={styles.expandedContent}>
          <Text style={styles.assignedText}>🎓 Assigned to student: {item.user_id ?? 'N/A'}</Text>
          <Text style={styles.description}>{item.description}</Text>
          <TouchableOpacity onPress={() => toggleProgress(item.id)} />
          
          {item.status === 'pending' && (
            <TouchableOpacity
              style={styles.startButton}
              onPress={async () => {
                try {
                  const response = await fetch(`${BACKEND_URL}/quests/V1/createQuest`, {
                    method: 'POST',
                    headers: {
                      'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                      subject: item.title,
                      title: item.title,
                      description: item.description,
                      user_id: 1,
                      status: 'in progress',
                    }),
                  });
              
                  if (response.ok) {
                    Alert.alert('✅ Success', `Quest "${item.title}" has been started!`);
                    getQuests(); // Refresh the quest list
                  } else {
                    const errData = await response.json();
                    console.error('❌ Failed to create quest:', errData);
                    Alert.alert('❌ Error', errData.message || 'Failed to create quest.');
                  }
                } catch (error) {
                  console.error('❌ Error creating quest:', error);
                  Alert.alert('❌ Error', 'An unexpected error occurred.');
                }
              }}
              
              
            >
              <Text style={styles.startButtonText}>Start Quest</Text>
            </TouchableOpacity>
          )}
  
          {item.progress === 1 && (
            <TouchableOpacity style={styles.redeemButton} onPress={() => redeemReward(item.title)}>
              <Text style={styles.redeemText}>Redeem</Text>
            </TouchableOpacity>
          )}
        </View>
      )}
    </View>
  );
  
  const toCompleteRewards = rewards.filter((reward) => reward.status === 'pending' && reward.user_id === 0);
    const inProgressRewards = rewards.filter((reward) => reward.status === 'assigned' || reward.status === 'in progress');
  const completedRewards = rewards.filter((reward) => reward.status === 'completed');

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Rewards</Text>

      <Text style={styles.sectionTitle}>To Complete</Text>
      <FlatList data={toCompleteRewards} keyExtractor={(item) => item.id} renderItem={renderRewardItem} />

      <Text style={styles.sectionTitle}>In Progress</Text>
      <FlatList data={inProgressRewards} keyExtractor={(item) => item.id} renderItem={renderRewardItem} />

      <Text style={styles.sectionTitle}>Completed</Text>
      <FlatList data={completedRewards} keyExtractor={(item) => item.id} renderItem={renderRewardItem} />
    </View>
  );
};

export default RewardsScreen;

const styles = StyleSheet.create({
  container: { flex: 1, paddingTop: 50, backgroundColor: '#f2f2f2' },
  title: { fontSize: 26, fontWeight: 'bold', textAlign: 'center', marginBottom: 20 },
  sectionTitle: { fontSize: 20, fontWeight: '600', marginLeft: 16, marginTop: 20 },
  rewardCard: {
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 12,
    marginHorizontal: 16,
    marginVertical: 8,
    elevation: 3,
  },
  rewardHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  rewardTitle: { fontSize: 18, fontWeight: 'bold' },
  notificationBadge: {
    backgroundColor: 'red',
    borderRadius: 12,
    paddingHorizontal: 8,
    paddingVertical: 2,
  },
  notificationText: { color: '#fff', fontWeight: 'bold' },
  progressBar: { marginTop: 10, height: 10, borderRadius: 5 },
  expandedContent: { marginTop: 10 },
  assignedText: {
    fontSize: 13,
    fontStyle: 'italic',
    color: '#555',
    marginBottom: 4,
  },
  description: { fontSize: 14, color: '#333', marginBottom: 10 },
  redeemButton: {
    backgroundColor: '#4CAF50',
    padding: 10,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 10,
  },
  redeemText: { color: '#fff', fontWeight: 'bold' },
  toggleText: {
    color: '#fff',
    fontWeight: 'bold',
  },
  startButton: {
    backgroundColor: '#007BFF',
    padding: 10,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 10,
  },
  startButtonText: { 
    color: '#fff', 
    fontWeight: 'bold' 
  },
});
