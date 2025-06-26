import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, Modal } from 'react-native';
import { Stack } from 'expo-router';

interface UserRank {
  id: number;
  name: string;
  points: number;
}

export default function RankingScreen() {
  const [ranking, setRanking] = useState<UserRank[]>([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [modalMessage, setModalMessage] = useState('');

  // Simulate fetching ranking data (you can replace this with an API call)
  useEffect(() => {
    const fetchRanking = async () => {
      try {
        // Simulating an API call with sample data
        const fetchedRanking: UserRank[] = [
          { id: 1, name: 'User 1', points: 350 },
          { id: 2, name: 'User 2', points: 280 },
          { id: 3, name: 'User 3', points: 450 },
          { id: 4, name: 'User 4', points: 220 },
          { id: 5, name: 'User 5', points: 310 },
        ];

        // Sort users based on points (highest first)
        fetchedRanking.sort((a, b) => b.points - a.points);
        setRanking(fetchedRanking);
      } catch (error) {
        console.error('Error fetching ranking:', error);
        setModalMessage('An error occurred while fetching the ranking.');
        setModalVisible(true);
      }
    };

    fetchRanking();
  }, []);

  return (
    <>
      <Stack.Screen options={{ title: 'Ranking' }} />

      <View style={styles.container}>
        <Text style={styles.header}>Ranking</Text>

        {/* List of Users */}
        <FlatList
          data={ranking}
          keyExtractor={(item) => item.id.toString()}
          renderItem={({ item, index }) => (
            <View style={styles.rankContainer}>
              <Text style={styles.rankText}>#{index + 1}</Text>
              <Text style={styles.userName}>{item.name}</Text>
              <Text style={styles.userPoints}>{item.points} points</Text>
            </View>
          )}
          contentContainerStyle={styles.flatListContent} // Add extra padding at the bottom
        />

        {/* Custom Modal for Error Messages */}
        <Modal visible={modalVisible} transparent animationType="slide">
          <View style={styles.modalContainer}>
            <View style={styles.modalContent}>
              <Text style={styles.modalText}>{modalMessage}</Text>
              <TouchableOpacity style={styles.modalButton} onPress={() => setModalVisible(false)}>
                <Text style={styles.modalButtonText}>OK</Text>
              </TouchableOpacity>
            </View>
          </View>
        </Modal>
      </View>
    </>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
    backgroundColor: '#121212',
    alignItems: 'center',
  },
  header: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#fff',
    marginBottom: 20,
  },
  rankContainer: {
    backgroundColor: '#2c3e50',
    padding: 15,
    marginVertical: 8,
    borderRadius: 8,
    width: '90%',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignSelf:'center'
  },
  rankText: {
    fontSize: 22,
    fontWeight: 'bold',
    color: '#fff',
  },
  userName: {
    fontSize: 18,
    color: '#fff',
  },
  userPoints: {
    fontSize: 18,
    color: '#fff',
    fontWeight: 'bold',
  },
  flatListContent: {
    paddingBottom: 90, // Adds space at the bottom of the list to ensure the last item is visible
  },

  // Modal Styles
  modalContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.6)', // Dark overlay
  },
  modalContent: {
    backgroundColor: '#fff',
    padding: 20,
    borderRadius: 10,
    alignItems: 'center',
    width: '80%',
  },
  modalText: {
    fontSize: 18,
    color: '#333',
    marginBottom: 15,
    textAlign: 'center',
  },
  modalButton: {
    backgroundColor: '#E63946',
    padding: 10,
    borderRadius: 6,
    width: '50%',
    alignItems: 'center',
  },
  modalButtonText: {
    color: '#fff',
    fontWeight: 'bold',
  },
});
