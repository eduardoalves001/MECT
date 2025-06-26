import { Tabs } from 'expo-router';
import React from 'react';
import { Platform } from 'react-native';
import { HapticTab } from '@/components/HapticTab';
import { IconSymbol } from '@/components/ui/IconSymbol';
import TabBarBackground from '@/components/ui/TabBarBackground';
import { Colors } from '@/constants/Colors';
import { useColorScheme } from '@/hooks/useColorScheme';
import Icon from 'react-native-vector-icons/MaterialIcons';


export default function TabLayout() {
  const colorScheme = useColorScheme();

  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: Colors[colorScheme ?? 'dark'].tint,
        headerShown: false,
        tabBarButton: HapticTab,
        tabBarBackground: TabBarBackground,
        tabBarStyle: Platform.select({
          ios: {
            // Use a transparent background on iOS to show the blur effect
            position: 'absolute',
          },
          default: {
            borderRadius:10
          },
        }),
      }}>
      
      <Tabs.Screen
        name="index"
        options={{
          title: 'index',
          tabBarIcon: ({ color }) => <IconSymbol size={28} name="house.fill" color={color} />,
        }}
      />
      <Tabs.Screen
        name="subjects"
        options={{
          title: 'subjects',
          tabBarIcon: ({ color }) => <Icon size={28} name="assignment" color={color} />,
        }}
      />
      <Tabs.Screen
        name="ranking"
        options={{
          title: 'ranking',
          tabBarIcon: ({ color }) => <Icon size={28} name="leaderboard" color={color} />,
        }}
      />
      <Tabs.Screen
        name="rewards"
        options={{
          title: 'rewards',
          tabBarIcon: ({ color }) => <Icon size={28} name="emoji-events" color={color} />,
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          title: 'Profile',
          tabBarIcon: ({ color }) => <Icon size={28} name="person" color={color} />,
        }}
      />
    </Tabs>
  );
}
