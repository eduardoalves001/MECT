import { Stack } from 'expo-router';
import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, FlatList, Switch, TouchableOpacity } from 'react-native';

// Define a type for the task object
interface Subject {
  name: string;
}

const QuestsTab = () => {
  const [tasks, setTasks] = useState<Subject[]>([]);

  useEffect(() => {
    const fetchTasks = async () => {
      const taskData: Subject[] = [
        {name: 'EGS' },
        {name: 'RSA' },
        {name: 'SD' },
        {name: 'ASE' },
        {name: 'MDRS'},
      ];
      setTasks(taskData);
    };

    fetchTasks();
  }, []);


  const renderItem = ({ item }: { item: Subject }) => (
    <View style={styles.taskContainer}>
      <TouchableOpacity>
        <Text style={styles.taskName}>{item.name}</Text>
      </TouchableOpacity>
      
    </View>
  );

  return (
    <>
    <Stack.Screen options={{ title: 'Subjects' }} />
    


    <View style={styles.container}>
      <Text style={styles.title}>Subjects</Text>
      <FlatList
        data={tasks}
        renderItem={renderItem}
        contentContainerStyle={styles.flatListContent}
      />

      
    </View>
    </>
  );
};

const styles = StyleSheet.create({
  container: {
    marginTop:40,
    flex: 1,
    paddingTop: 20,
    paddingHorizontal: 16,
    backgroundColor: '#ffffff',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 10,
    color: '#000',
  },
  taskContainer: {
    marginTop:15,
    backgroundColor: '#C6C2BF',
    marginBottom: 10,
    borderRadius: 35,
    padding: 15,
    width:300,
    alignSelf:'center'
  },
  taskName: {
    color:'#000',
    fontSize: 18,
    fontWeight: 'bold',
  },
  taskDescription: {
    fontSize: 14,
    color: '#000',
  },
  toggleContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: 10,
  },
  toggleLabel: {
    fontSize: 16,
    color: '#888',
  },
  submitButtonContainer: {
    marginTop: 20,
    paddingBottom: 20, // Ensure there's space below the button
  },
  submitButton: {
    backgroundColor: '#4CAF50',
    paddingVertical: 15,
    paddingHorizontal: 30,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 50,
  },
  submitButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: 'bold',
  },
  flatListContent: {
    paddingBottom: 80, // Ensure there's space at the bottom for the button
  },
});

export default QuestsTab;
