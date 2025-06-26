import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, FlatList, Image, TouchableOpacity, ActivityIndicator } from 'react-native';
import { Stack } from 'expo-router';

interface UserRank {
  rank: number;
  user_id: number;
  name: string;
  total_points: number;
}
const BASE_URL = 'http://192.168.1.99:8002';

export default function RankingScreen() {
  const [ranking, setRanking] = useState<UserRank[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchRanking = async () => {
      try {
        const response = await fetch(`${BASE_URL}/v1/users/`, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
          },
        });

        if (!response.ok) throw new Error('Failed to fetch ranking');

        const jsonData = await response.json();
        setRanking(jsonData.ranking);
      } catch (err) {
        if (err instanceof Error) {
          setError(err.message);
        } else {
          setError('An unknown error occurred');
        }
      } finally {
        setLoading(false);
      }
    };

    fetchRanking();

    const intervalId = setInterval(() => {
      fetchRanking();
    }, 30000); // Refresh every 30 seconds

    return () => clearInterval(intervalId);
  }, []);

  if (loading) {
    return <ActivityIndicator size="large" color="#4CAF50" style={styles.loader} />;
  }

  if (error) {
    return <Text style={styles.error}>Error: {error}</Text>;
  }

  return (
    <>
      <Stack.Screen options={{ title: 'Ranking' }} />
      <View style={styles.container}>
        <Text style={styles.header}>Ranking</Text>
        <View style={styles.top3Container}>
          <View style={[styles.topUser, styles.thirdPlace]}>
            <Image source={require('../../assets/images/avatar.jpg')} style={styles.profileCircle} />
            <Text style={styles.userName}>{ranking[2]?.name}</Text>
            <Text style={styles.userPoints}>{ranking[2]?.total_points} XP</Text>
            <View style={styles.rankBadge}><Text style={styles.rankBadgeText}>3</Text></View>
          </View>
          <View style={[styles.topUser, styles.firstPlace]}>
            <Image source={require('../../assets/images/avatar.jpg')} style={styles.profileCircle} />
            <Text style={styles.userName}>{ranking[0]?.name}</Text>
            <Text style={styles.userPoints}>{ranking[0]?.total_points} XP</Text>
            <View style={styles.rankBadge}><Text style={styles.rankBadgeText}>1</Text></View>
          </View>
          <View style={[styles.topUser, styles.secondPlace]}>
            <Image source={require('../../assets/images/avatar.jpg')} style={styles.profileCircle} />
            <Text style={styles.userName}>{ranking[1]?.name}</Text>
            <Text style={styles.userPoints}>{ranking[1]?.total_points} XP</Text>
            <View style={styles.rankBadge}><Text style={styles.rankBadgeText}>2</Text></View>
          </View>
        </View>
        <FlatList
          data={ranking.slice(3)}
          keyExtractor={(item) => item.user_id.toString()}
          renderItem={({ item, index }) => (
            <View style={styles.rankContainer}>
              <Text style={styles.rankText}>{index + 4}</Text>
              <View style={styles.profileSmall} />
              <Text style={styles.userName}>{item.name}</Text>
              <Text style={styles.userPoints}>{item.total_points} XP</Text>
              <TouchableOpacity>
                <Text style={styles.arrow}>{'>'}</Text>
              </TouchableOpacity>
            </View>
          )}
        />
      </View>
    </>
  );
}

const styles = StyleSheet.create({
  container: {
    marginTop: 40,
    flex: 1,
    backgroundColor: '#fff',
    padding: 16,
  },
  loader: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  error: {
    fontSize: 16,
    color: 'red',
    textAlign: 'center',
    marginTop: 20,
  },
  header: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#000',
    textAlign: 'center',
    marginBottom: 20,
  },
  top3Container: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 20,
    alignSelf: 'center',
  },
  topUser: {
    alignItems: 'center',
    marginHorizontal: 10,
  },
  firstPlace: {
    transform: [{ scale: 1.2 }],
    zIndex: 3,
  },
  secondPlace: {
    zIndex: 2,
  },
  thirdPlace: {
    zIndex: 1,
  },
  profileCircle: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: '#ddd',
    marginBottom: 5,
  },
  profileSmall: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#ddd',
  },
  userName: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#000',
    margin: 20,
  },
  userPoints: {
    fontSize: 12,
    color: '#777',
    marginRight: 20,
  },
  rankBadge: {
    backgroundColor: '#4CAF50',
    padding: 5,
    borderRadius: 10,
  },
  rankBadgeText: {
    color: '#fff',
    fontWeight: 'bold',
  },
  rankContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 10,
    borderBottomWidth: 1,
    borderColor: '#eee',
  },
  rankText: {
    fontSize: 18,
    fontWeight: 'bold',
    width: 30,
  },
  arrow: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#4CAF50',
    marginLeft: 'auto',
  },
});
