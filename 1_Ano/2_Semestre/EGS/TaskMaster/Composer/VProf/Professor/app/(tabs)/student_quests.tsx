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
const quest_url = "http://192.168.1.190:8003";

const QuestsScreen = () => {
  const [quests, setQuests] = useState<Quest[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);

  const [subject, setSubject] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');

  const [searchUserId, setSearchUserId] = useState('');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc');

  const [expandedQuest, setExpandedQuest] = useState<number | null>(null);

  const fetchQuests = async () => {
    try {
      const response = await fetch(`${quest_url}/quests/V1/getQuests`);
      const data = await response.json();
      const filtered = data.filter((quest: Quest) => quest.user_id !== 0);
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
      const response = await fetch(`${quest_url}/quests/V1/createQuest`, {
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

  const handleToggleExpand = (id: number) => {
    setExpandedQuest(expandedQuest === id ? null : id);
  };

  const handleCompleteQuest = async (id: number) => {
    try {
      // Step 1: Fetch the current quest data
      const response = await fetch(`${quest_url}/quests/${id}`);
      if (!response.ok) throw new Error('Quest not found');
      console.log(response);
  
      const quest = await response.json();
  
      // Step 2: Prepare the updated quest data
      const updatedQuest = {
        subject: quest.subject,
        title: quest.title,
        description: quest.description,
        user_id: quest.user_id,
        status: 'completed', // ✅ Only change status
      };
  
      // Step 3: Send the updated quest to the backend
      const updateResponse = await fetch(`${quest_url}/quests/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(updatedQuest),
      });
  
      if (!updateResponse.ok) throw new Error('Failed to complete quest');
  
      Alert.alert('Success', 'Quest completed!');
      fetchQuests(); // Refresh the list
    } catch (error) {
      console.error('Error completing quest:', error);
      Alert.alert('Error', 'Could not complete quest');
    }
  };
  
  
  const filteredAndSortedQuests = quests
    .filter((quest) =>
      searchUserId ? quest.user_id === parseInt(searchUserId) : true
    )
    .sort((a, b) =>
      sortOrder === 'asc' ? a.user_id - b.user_id : b.user_id - a.user_id
    );

  const renderQuest = ({ item }: { item: Quest }) => (
    <View style={styles.card}>
      <TouchableOpacity onPress={() => handleToggleExpand(item.id)}>
        <Text style={styles.title}>{item.title}</Text>
        <Text style={styles.subtitle}>{item.subject}</Text>
        <Text>{item.description}</Text>
        <Text style={styles.userId}>Aluno : {item.user_id}</Text>
      </TouchableOpacity>

      {expandedQuest === item.id && (
        <View style={styles.expandableContent}>
          <Text>Status: {item.status}</Text>
          {item.status !== 'completed' && (
            <TouchableOpacity
              style={styles.completedButton}
              onPress={() => handleCompleteQuest(item.id)}
            >
              <Text style={styles.completedButtonText}>Completed</Text>
            </TouchableOpacity>
          )}
        </View>
      )}
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
          {/* Filter + Sort UI */}
          <View style={styles.filterContainer}>
            <TextInput
              placeholder="Filter by user ID"
              value={searchUserId}
              onChangeText={setSearchUserId}
              keyboardType="numeric"
              style={styles.filterInput}
            />
            <View style={styles.sortButtons}>
              <TouchableOpacity
                style={[
                  styles.sortButton,
                  sortOrder === 'asc' && styles.sortButtonActive,
                ]}
                onPress={() => setSortOrder('asc')}
              >
                <Text style={styles.sortButtonText}>Asc</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[
                  styles.sortButton,
                  sortOrder === 'desc' && styles.sortButtonActive,
                ]}
                onPress={() => setSortOrder('desc')}
              >
                <Text style={styles.sortButtonText}>Desc</Text>
              </TouchableOpacity>
            </View>
          </View>

          {/* Quest list */}
          <FlatList
            data={filteredAndSortedQuests}
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
            onRequestClose={() => setModalVisible(false)}
          >
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
    marginTop:50,
    flex: 1,
  },
  center: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  listContainer: {
    paddingHorizontal: 16,
    paddingTop: 10,
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
  userId: {
    marginTop: 8,
    fontStyle: 'italic',
    color: '#444',
  },
  filterContainer: {
    paddingHorizontal: 16,
    paddingTop: 16,
    flexDirection: 'column',
    gap: 10,
  },
  filterInput: {
    borderWidth: 1,
    borderColor: '#ccc',
    padding: 10,
    borderRadius: 8,
  },
  sortButtons: {
    flexDirection: 'row',
    gap: 10,
    marginTop: 10,
  },
  sortButton: {
    padding: 10,
    borderRadius: 8,
    backgroundColor: '#B0B0B0',
    flex: 1,
    alignItems: 'center',
  },
  sortButtonActive: {
    backgroundColor: '#007AFF',
  },
  sortButtonText: {
    color: 'white',
    fontWeight: 'bold',
  },
  expandableContent: {
    marginTop: 10,
    padding: 10,
    backgroundColor: '#f9f9f9',
    borderRadius: 8,
  },
  completedButton: {
    marginTop: 10,
    backgroundColor: '#28a745',
    padding: 10,
    borderRadius: 8,
    alignItems: 'center',
  },
  completedButtonText: {
    color: 'white',
    fontWeight: 'bold',
    fontSize: 16,
  },
});
