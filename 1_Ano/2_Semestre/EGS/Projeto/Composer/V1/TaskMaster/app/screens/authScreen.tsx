import React, { useState, useEffect } from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import * as WebBrowser from 'expo-web-browser';
import * as AuthSession from 'expo-auth-session';
import { router } from 'expo-router';


WebBrowser.maybeCompleteAuthSession();

type AuthScreenProps = {
  onLogin: () => void;
};

interface GitHubUser {
  login: string;
  avatar_url: string;
  name?: string;
  email?: string;
}

const BASE_URL = 'http://10.163.235.3:8002';

const AuthScreen: React.FC<AuthScreenProps> = ({ onLogin }) => {
  const [userInfo, setUserInfo] = useState<GitHubUser | null>(null);

  const [request, response, promptAsync] = AuthSession.useAuthRequest(
    {
      clientId: 'Ov23liMZA9d8J6V9gKaO',
      scopes: ['read:user', 'user:email'],
      redirectUri: AuthSession.makeRedirectUri(),
    },
    { authorizationEndpoint: 'https://github.com/login/oauth/authorize' }
  );

  useEffect(() => {
    if (response?.type === 'success') {
      const { code } = response.params;
      exchangeCodeForToken(code);
    }
  }, [response]);

  async function exchangeCodeForToken(code: string) {
    try {
      const response = await fetch('https://github.com/login/oauth/access_token', {
        method: 'POST',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          client_id: 'Ov23liMZA9d8J6V9gKaO',
          client_secret: '7af134740ee040b21436f2b89dffc5c5c8f58e60',
          code,
        }),
      });

      const data = await response.json();
      fetchGitHubUser(data.access_token);
    } catch (error) {
      console.error('Error exchanging code:', error);
    }
  }


// Inside fetchGitHubUser function
async function fetchGitHubUser(token: string) {
  try {
    const response = await fetch('https://api.github.com/user', {
      headers: { Authorization: `Bearer ${token}` },
    });

    const data = await response.json();
    setUserInfo(data);

    console.log("GitHub user data:", data);

    // First, get all users
    const usersResponse = await fetch(`${BASE_URL}/v1/users/`, { method: 'GET' });
    const users = await usersResponse.json();
    console.log("users:", JSON.stringify(users, null, 2));
    const userList = users.ranking.map((user: { name: string; Email: string }) => ({
      name: user.name,
      email: user.Email 
    }));
    
    console.log("User List:", userList);
    let userExists = false;
    // Check if user already exists
    for (const user of userList) {
      if(user.name == data.name && user.email == data.email){
        userExists =true;
      }
    }
    
    if (userExists) {
      console.log("User already exists, skipping creation.");
    } else {
      // Only add user if not found
      const addUserResponse = await fetch(`${BASE_URL}/v1/users/`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: data.name, email: data.email }),
      });

      const responseJson = await addUserResponse.json();
      console.log("Server response:", responseJson);
    }

    // Navigate to profile page
    setTimeout(() => {
      router.push({
        pathname: '/(tabs)/profile',
        params: {
          login: data.login,
          avatar_url: data.avatar_url,
          email: data.email,
          name: data.name,
        },
      });
    }, 500);

    onLogin();
  } catch (error) {
    console.error('Error fetching user data:', error);
  }
}


  return (
    <View style={styles.container}>
      <Text style={styles.title}>TaskMaster</Text>

      {userInfo && <Text style={styles.userText}>Welcome, {userInfo.login}!</Text>}

        <TouchableOpacity style={styles.button2} onPress={() => promptAsync()}>
          <Text style={styles.buttonText2}>Login with GitHub</Text>
        </TouchableOpacity>
  
      <TouchableOpacity style={styles.button2} onPress={onLogin}>
        <Text style={styles.buttonText2}>Login with UA</Text>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#121212',
    padding: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#fff',
    marginBottom: 20,
  },
  userText: {
    color: '#fff',
    fontSize: 18,
    marginBottom: 20,
  },
  button2: {
    backgroundColor: '#FFFFFF',
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 8,
    marginBottom: 15,
  },
  buttonText2: {
    color: '#000000',
    fontSize: 18,
    fontWeight: 'bold',
  },
});

export default AuthScreen;
