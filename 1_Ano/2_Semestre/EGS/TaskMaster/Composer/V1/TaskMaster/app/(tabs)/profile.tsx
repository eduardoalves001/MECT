import React from 'react';
import { View, Text, Image, StyleSheet, TouchableOpacity, Alert } from 'react-native';
import Icon from 'react-native-vector-icons/MaterialIcons';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import NfcManager, { NfcTech } from 'react-native-nfc-manager';
import { router } from 'expo-router';
import { useLocalSearchParams } from 'expo-router';
import AsyncStorage from '@react-native-async-storage/async-storage'; // Import AsyncStorage for logout
import { replace } from 'expo-router/build/global-state/routing';

NfcManager.start();

const ProfileScreen = () => {
  const user = useLocalSearchParams();

  // Function to detect NFC tags
  const handleNFCScan = async () => {
    try {
      console.log('Scanning for NFC...');
      await NfcManager.requestTechnology(NfcTech.Ndef);

      const tag = await NfcManager.getTag();

      if (!tag) {
        Alert.alert('NFC Error', 'No NFC tag detected. Try again.');
        return;
      }

      console.log('NFC Tag Detected:', tag);
      Alert.alert('NFC Detected', JSON.stringify(tag));
    } catch (error) {
      console.warn('NFC Error:', error);
      Alert.alert('NFC Error', 'Failed to read NFC tag.');
    } finally {
      await NfcManager.cancelTechnologyRequest();
    }
  };

  // Function to handle logout
  const handleLogout = async () => {
    try {
      // Clear authentication token and user data
      await AsyncStorage.removeItem('authToken');
      await AsyncStorage.removeItem('userData'); // If you're storing user data

      // Redirect to login screen
      router.replace('/screens/authScreen'); // Adjust the path to your login screen
      console.log('Logout successful');
    } catch (error) {
      console.error('Error during logout:', error);
      Alert.alert('Logout Error', 'There was an error logging out.');
    }
  };

  const handleRanking = () => {
    console.log('Ranking pressed');
  };

  return (
    <View style={styles.container}>
      <Image source={{ uri: Array.isArray(user.avatar_url) ? user.avatar_url[0] : user.avatar_url }} style={styles.avatar} />
      <Text style={styles.name}>{user.name || user.login}</Text>
      <Text style={styles.email}>{user.email || 'No email provided'}</Text>
      
      <TouchableOpacity style={styles.button2} onPress={handleNFCScan}>
        <MaterialCommunityIcons name="cellphone-nfc" size={20} color="white" />
        <Text style={styles.buttonText2}>NFC</Text>
      </TouchableOpacity>

      <TouchableOpacity style={styles.button} onPress={handleLogout}>
        <Icon name="logout" size={20} color="#fff" style={styles.icon} />
        <Text style={styles.buttonText}>Logout</Text>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#fff', // Dark mode background
  },
  avatar: {
    width: 200,
    height: 200,
    borderRadius: 100,
    alignSelf:"flex-start",
    left:110,
    bottom:160
  },
  name: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#000',
    bottom:140
  },
  email: {
    fontSize: 16,
    color: '#bbb',
    bottom: 130,
  },
  button: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#E63946',
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 8,
  },
  buttonText: {
    fontSize: 18,
    color: '#fff',
    marginLeft: 8,
  },
  icon: {
    marginRight: 5,
  },
  button2: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#0087bd',
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 8,
    marginBottom: 15,
  },
  buttonText2: {
    fontSize: 18,
    color: '#fff',
    marginLeft: 8,
  },
});

export default ProfileScreen;
