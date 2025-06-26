import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  FlatList,
  ActivityIndicator,
  StyleSheet,
  TouchableOpacity,
  Alert,
  Modal,
  TextInput,
  Pressable,
} from 'react-native';

interface Quest {
  id: number;
  subject: string;
  title: string;
  description: string;
  user_id: number;
  status: string;
}

const QuestsScreen = () => {
  const [quests, setQuests] = useState<Quest[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);

  // New quest fields
  const [subject, setSubject] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');

  const fetchQuests = async () => {
    try {
      const response = await fetch('http://192.168.1.190:8003/quests/V1/getQuests');
      const data = await response.json();
      const filtered = data.filter((quest: Quest) => quest.user_id === 0);
      setQuests(filtered);
    } catch (error) {
      console.error('Failed to fetch quests:', error);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchQuests();
  }, []);

  const handleCreateQuest = async () => {
    if (!subject || !title || !description) {
      Alert.alert('Missing fields', 'Please fill in all fields.');
      return;
    }

    try {
      const response = await fetch('http://192.168.1.190:8003/quests/V1/createQuest', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          subject,
          title,
          description,
          user_id: 0,
          status: 'pending',
        }),
      });

      if (!response.ok) throw new Error('Failed to create quest');

      Alert.alert('Success', 'Quest created!');
      setModalVisible(false);
      setSubject('');
      setTitle('');
      setDescription('');
      fetchQuests();
    } catch (error) {
      console.error('Error creating quest:', error);
      Alert.alert('Error', 'Could not create quest');
    }
  };

  const renderQuest = ({ item }: { item: Quest }) => (
    <View style={styles.card}>
      <Text style={styles.title}>{item.title}</Text>
      <Text style={styles.subtitle}>{item.subject}</Text>
      <Text>{item.description}</Text>
    </View>
  );

  return (
    <View style={styles.container}>
      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color="#007AFF" />
          <Text>Loading quests...</Text>
        </View>
      ) : (
        <>
          <FlatList
            data={quests}
            keyExtractor={(item) => item.id.toString()}
            contentContainerStyle={styles.listContainer}
            renderItem={renderQuest}
            refreshing={refreshing}
            onRefresh={() => {
              setRefreshing(true);
              fetchQuests();
            }}
          />

          {/* Create Quest Button */}
          <TouchableOpacity style={styles.button} onPress={() => setModalVisible(true)}>
            <Text style={styles.buttonText}>+ Create Quest</Text>
          </TouchableOpacity>

          {/* Modal */}
          <Modal
            animationType="slide"
            transparent
            visible={modalVisible}
            onRequestClose={() => setModalVisible(false)}>
            <View style={styles.modalOverlay}>
              <View style={styles.modalView}>
                <Text style={styles.modalTitle}>New Quest</Text>
                <TextInput
                  placeholder="Subject"
                  value={subject}
                  onChangeText={setSubject}
                  style={styles.input}
                />
                <TextInput
                  placeholder="Title"
                  value={title}
                  onChangeText={setTitle}
                  style={styles.input}
                />
                <TextInput
                  placeholder="Description"
                  value={description}
                  onChangeText={setDescription}
                  style={[styles.input, { height: 80 }]}
                  multiline
                />
                <View style={styles.modalButtons}>
                  <Pressable onPress={() => setModalVisible(false)} style={styles.cancelButton}>
                    <Text style={{ color: '#007AFF' }}>Cancel</Text>
                  </Pressable>
                  <Pressable onPress={handleCreateQuest} style={styles.confirmButton}>
                    <Text style={{ color: 'white' }}>Create</Text>
                  </Pressable>
                </View>
              </View>
            </View>
          </Modal>
        </>
      )}
    </View>
  );
};

export default QuestsScreen;

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  center: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  listContainer: {
    paddingHorizontal: 16,
    paddingTop: 40,
    paddingBottom: 100,
  },
  card: {
    backgroundColor: '#f2f2f2',
    padding: 16,
    marginBottom: 12,
    borderRadius: 12,
    elevation: 2,
  },
  title: {
    fontWeight: 'bold',
    marginBottom: 4,
    fontSize: 16,
  },
  subtitle: {
    fontStyle: 'italic',
    color: '#666',
    marginBottom: 4,
  },
  button: {
    backgroundColor: '#007AFF',
    padding: 16,
    borderRadius: 20,
    position: 'absolute',
    bottom: 30,
    alignSelf: 'center',
    paddingHorizontal: 24,
    shadowColor: '#000',
    shadowOpacity: 0.2,
    shadowRadius: 8,
    elevation: 4,
  },
  buttonText: {
    color: '#fff',
    fontWeight: '600',
    fontSize: 16,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.3)',
    justifyContent: 'center',
    padding: 20,
  },
  modalView: {
    backgroundColor: 'white',
    borderRadius: 12,
    padding: 20,
    elevation: 5,
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 12,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ccc',
    padding: 10,
    borderRadius: 8,
    marginBottom: 12,
  },
  modalButtons: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  cancelButton: {
    padding: 10,
  },
  confirmButton: {
    backgroundColor: '#007AFF',
    padding: 10,
    borderRadius: 8,
  },
});
