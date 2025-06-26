import { View, Text, Image, StyleSheet, Pressable, TextInput, FlatList, TouchableOpacity, Linking, Modal } from 'react-native';
import { useState } from 'react';
import { Stack } from 'expo-router';
import React from 'react';

export default function GitHubScreen() {
  const [username, setUsername] = useState('');
  const [profile, setProfile] = useState<GitHubProfile | null>(null);
  const [repos, setRepos] = useState<GitHubRepo[]>([]);
  const [commitCounts, setCommitCounts] = useState<any>({});
  const [modalVisible, setModalVisible] = useState(false);
  const [modalMessage, setModalMessage] = useState('');

  interface GitHubProfile {
    avatar_url: string;
    name: string;
    login: string;
    bio: string;
    repos_url: string;
  }

  interface GitHubRepo {
    id: number;
    name: string;
    description: string;
    html_url: string;
  }

  const fetchGitHubProfile = async () => {
    try {
      console.log(`Fetching data for: ${username}`);
      const userResponse = await fetch(`https://api.github.com/users/${username}`);
      if (!userResponse.ok) {
        setModalMessage('User Not Found! Please check the username and try again.');
        setModalVisible(true);
        return;
      }
      const userData = await userResponse.json();
      setProfile(userData);

      const reposResponse = await fetch(userData.repos_url);
      if (!reposResponse.ok) {
        setModalMessage('Failed to fetch repositories.');
        setModalVisible(true);
        return;
      }
      const reposData = await reposResponse.json();
      setRepos(reposData);

      // Fetch commits for each repository
      reposData.forEach((repo: GitHubRepo) => {
        fetchCommitDetails(userData.login, repo.name, repo.id);
      });

    } catch (error) {
      console.error('Error fetching data:', error);
      setModalMessage('An error occurred while fetching data.');
      setModalVisible(true);
    }
  };

  // Fetch commit details (message, date, author)
  const fetchCommitDetails = async (owner: string, repoName: string, repoId: number) => {
    try {
      let commitDetails: {
        message: any; date: any; // Commit date
        author: any; // Author's name
        url: any;
      }[] = [];
      let page = 1;
      let commitsResponse;

      // Fetch commits page by page
      do {
        commitsResponse = await fetch(`https://api.github.com/repos/${owner}/${repoName}/commits?per_page=100&page=${page}`);
        if (!commitsResponse.ok) {
          throw new Error('Failed to fetch commits');
        }

        const commitsData = await commitsResponse.json();
        commitsData.forEach((commit: any) => {
          commitDetails.push({
            message: commit.commit.message,
            date: commit.commit.author.date, // Commit date
            author: commit.commit.author.name, // Author's name
            url: commit.html_url, // URL to commit
          });
        });

        page++;
      } while (commitsResponse.headers.get('Link')?.includes('rel="next"')); // Check if there's a next page

      // Set commit details (optional: for displaying in your app)
      setCommitCounts((prev: any) => ({ ...prev, [repoId]: commitDetails }));
    } catch (error) {
      console.error('Error fetching commit details:', error);
    }
  };

  return (
    <>
      <Stack.Screen options={{ title: 'GitHub' }} />

      <View style={styles.container}>
        {/* Search Input */}
        <TextInput
          style={styles.input}
          placeholder="Enter GitHub Username"
          value={username}
          onChangeText={setUsername}
        />

        {/* Search Button */}
        <TouchableOpacity style={styles.button} onPress={() => fetchGitHubProfile()}>
          <Text style={styles.buttonText}>Search</Text>
        </TouchableOpacity>

        {/* Display User Profile */}
        {profile && (
          <View style={styles.profileContainer}>
            <Image source={{ uri: profile.avatar_url }} style={styles.avatar} />
            <Text style={styles.name}>{profile.name || profile.login}</Text>
            <Text style={styles.bio}>{profile.bio}</Text>
          </View>
        )}

        {/* List of Repositories */}
        <FlatList
          data={repos}
          keyExtractor={(item) => item.id.toString()}
          renderItem={({ item }) => (
            <View style={styles.repoContainer}>
              <Text style={styles.repoName}>{item.name}</Text>
              <Text style={styles.repoDesc}>{item.description || 'No description'}</Text>
              <TouchableOpacity style={styles.githubButton} onPress={() => Linking.openURL(item.html_url)}>
                <Text style={styles.githubButtonText}>View on GitHub</Text>
              </TouchableOpacity>

              {/* Display Commit Details */}
              <FlatList
                data={commitCounts[item.id]}
                keyExtractor={(commit, index) => index.toString()}
                renderItem={({ item: commit }) => (
                  <View style={styles.commitContainer}>
                    <Text style={styles.commitMessage}>Message: {commit.message}</Text>
                    <Text style={styles.commitDate}>Date: {new Date(commit.date).toLocaleString()}</Text>
                    <Text style={styles.commitAuthor}>Author: {commit.author}</Text>
                    <TouchableOpacity onPress={() => Linking.openURL(commit.url)}>
                      <Text style={styles.commitLink}>View Commit</Text>
                    </TouchableOpacity>
                  </View>
                )}
              />
            </View>
          )}
          contentContainerStyle={styles.flatListContent} // Add extra padding at the bottom
          keyboardShouldPersistTaps="handled" // Allow tapping items when keyboard is open
        />

        {/* Custom Modal for Error Messages */}
        <Modal visible={modalVisible} transparent animationType="slide">
          <View style={styles.modalContainer}>
            <View style={styles.modalContent}>
              <Text style={styles.modalText}>{modalMessage}</Text>
              <Pressable style={styles.modalButton} onPress={() => setModalVisible(false)}>
                <Text style={styles.modalButtonText}>OK</Text>
              </Pressable>
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
    marginTop:30,
    padding: 16,
    backgroundColor: '#121212',
    justifyContent: 'flex-start',
    alignItems: 'center',
  },
  input: {
    backgroundColor: '#fff',
    padding: 10,
    width: '80%', // Ensure input takes up 80% of the container width
    borderRadius: 8,
    marginBottom: 20,
  },
  button: {
    backgroundColor: '#808080',
    width: '80%', // Make the button the same width as the input
    padding: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  buttonText: {
    fontSize: 18,
    color: '#fff',
  },
  profileContainer: {
    marginTop: 15,
    backgroundColor: '#121212',
    alignItems: 'center',
    marginBottom: 20,
  },
  avatar: {
    width: 100,
    height: 100,
    borderRadius: 50,
  },
  name: {
    fontSize: 22,
    fontWeight: 'bold',
    color: '#fff',
    marginTop: 10,
  },
  bio: {
    color: '#bbb',
    textAlign: 'center',
    paddingHorizontal: 20,
  },
  repoContainer: {
    backgroundColor: '#CCCCCB',
    padding: 12,
    marginVertical: 6,
    borderRadius: 8,
    marginBottom: 10,
    width: 350,
  },
  repoName: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#03A9F4',
  },
  repoDesc: {
    color: '#fff',
    marginBottom: 8,
  },
  githubButton: {
    backgroundColor: '#03A9F4',
    padding: 8,
    borderRadius: 6,
    alignItems: 'center',
  },
  githubButtonText: {
    color: '#fff',
    fontWeight: 'bold',
  },

  // Commit Details Styles
  commitContainer: {
    marginTop: 10,
    backgroundColor: '#121212',
    padding: 8,
    borderRadius: 6,
  },
  commitMessage: {
    color: '#fff',
    fontWeight: 'bold',
  },
  commitDate: {
    color: '#bbb',
  },
  commitAuthor: {
    color: '#bbb',
  },
  commitLink: {
    color: '#03A9F4',
    fontWeight: 'bold',
    marginTop: 5,
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
  flatListContent: {
    paddingBottom: 90, // Adds space at the bottom of the list to ensure the last item is visible
  },
});
