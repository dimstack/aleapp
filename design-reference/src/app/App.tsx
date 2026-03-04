import { useState, useEffect } from 'react';
import { Header } from './components/Header';
import { ContactCard } from './components/ContactCard';
import { ServerCard } from './components/ServerCard';
import { CallScreen } from './components/CallScreen';
import { IncomingCallScreen } from './components/IncomingCallScreen';
import { ProfileScreen } from './components/ProfileScreen';
import { ServerScreen } from './components/ServerScreen';
import { AddServerScreen } from './components/AddServerScreen';
import { CreateProfileScreen } from './components/CreateProfileScreen';
import { UserProfileScreen } from './components/UserProfileScreen';
import { ServerManagementScreen } from './components/ServerManagementScreen';
import { JoinRequestsScreen } from './components/JoinRequestsScreen';
import { PendingRequestScreen } from './components/PendingRequestScreen';
import { NotificationsScreen } from './components/NotificationsScreen';
import { SettingsScreen } from './components/SettingsScreen';
import { Plus } from 'lucide-react';

interface Contact {
  id: string;
  name: string;
  avatar: string;
  status: 'online' | 'offline' | 'busy';
  isFavorite: boolean;
  serverId: string;
}

interface Server {
  id: string;
  name: string;
  username: string;
  description: string;
  image: string;
  adminApiKey?: string;
}

interface ServerProfile {
  serverId: string;
  username: string;
  name: string;
  avatar?: string;
  isAdmin: boolean;
}

interface JoinRequest {
  id: string;
  userId: string;
  userName: string;
  userAvatar?: string;
  username: string;
  serverId: string;
  status: 'pending' | 'approved' | 'rejected';
  createdAt: Date;
}

interface Notification {
  id: string;
  type: 'request_approved' | 'request_rejected' | 'request_pending';
  serverName: string;
  serverId: string;
  message: string;
  createdAt: Date;
  read: boolean;
}

const servers: Server[] = [
  {
    id: 's1',
    name: 'Tech Community',
    username: '@tech_community',
    description: 'Сообщество разработчиков и технических специалистов. Обсуждаем последние технологии и делимся опытом.',
    image: 'https://images.unsplash.com/photo-1764885517706-614101aa5811?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHx0ZWNobm9sb2d5JTIwY29tbXVuaXR5JTIwYmx1ZXxlbnwxfHx8fDE3NzI0ODAxNDl8MA&ixlib=rb-4.1.0&q=80&w=1080',
  },
  {
    id: 's2',
    name: 'Creative Studio',
    username: '@creative_studio',
    description: 'Творческая студия для дизайнеров, художников и креативных профессионалов.',
    image: 'https://images.unsplash.com/photo-1763191213523-1489179a1088?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxjcmVhdGl2ZSUyMHdvcmtzcGFjZSUyMGRlc2lnbnxlbnwxfHx8fDE3NzI0MTM4NTd8MA&ixlib=rb-4.1.0&q=80&w=1080',
  },
  {
    id: 's3',
    name: 'Music Production',
    username: '@music_prod',
    description: 'Сообщество музыкальных продюсеров, звукорежиссеров и музыкантов.',
    image: 'https://images.unsplash.com/photo-1741746239350-9021ddfa3d59?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxtdXNpYyUyMHN0dWRpbyUyMHB1cnBsZXxlbnwxfHx8fDE3NzI0ODAxNDl8MA&ixlib=rb-4.1.0&q=80&w=1080',
  },
  {
    id: 's4',
    name: 'Game Dev Hub',
    username: '@gamedev_hub',
    description: 'Хаб для разработчиков игр - от инди до AAA проектов.',
    image: 'https://images.unsplash.com/photo-1762503203730-ca33982518af?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxhYnN0cmFjdCUyMHBhdHRlcm4lMjBncmFkaWVudCUyMGNvbG9yZnVsfGVufDF8fHx8MTc3MjQ4MDE1Mnww&ixlib=rb-4.1.0&q=80&w=1080',
  },
];

const initialContacts: Contact[] = [
  {
    id: '1',
    name: 'Анна Смирнова',
    avatar: 'https://images.unsplash.com/photo-1689600944138-da3b150d9cb8?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxwcm9mZXNzaW9uYWwlMjBoZWFkc2hvdCUyMHdvbWFufGVufDF8fHx8MTc3MjQ0MzgwMnww&ixlib=rb-4.1.0&q=80&w=1080',
    status: 'online',
    isFavorite: true,
    serverId: 's1',
  },
  {
    id: '2',
    name: 'Дмитрий Петров',
    avatar: 'https://images.unsplash.com/photo-1723537742563-15c3d351dbf2?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxwcm9mZXNzaW9uYWwlMjBoZWFkc2hvdCUyMG1hbiUyMGJ1c2luZXNzfGVufDF8fHx8MTc3MjQyMDMwMXww&ixlib=rb-4.1.0&q=80&w=1080',
    status: 'online',
    isFavorite: true,
    serverId: 's2',
  },
  {
    id: '3',
    name: 'Елена Иванова',
    avatar: 'https://images.unsplash.com/photo-1623594675959-02360202d4d6?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxwcm9mZXNzaW9uYWwlMjBwb3J0cmFpdCUyMHdvbWFuJTIwc21pbGluZ3xlbnwxfHx8fDE3NzI0NzE2NDR8MA&ixlib=rb-4.1.0&q=80&w=1080',
    status: 'busy',
    isFavorite: true,
    serverId: 's3',
  },
  {
    id: '4',
    name: 'Алексей Козлов',
    avatar: 'https://images.unsplash.com/photo-1762753674498-73ec49feafc4?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxwcm9mZXNzaW9uYWwlMjBoZWFkc2hvdCUyMHlvdW5nJTIwbWFufGVufDF8fHx8MTc3MjQ3MTY0NHww&ixlib=rb-4.1.0&q=80&w=1080',
    status: 'online',
    isFavorite: false,
    serverId: 's1',
  },
  {
    id: '5',
    name: 'Мария Волкова',
    avatar: 'https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxidXNpbmVzcyUyMHdvbWFuJTIwcG9ydHJhaXR8ZW58MXx8fHwxNzcyNDY3ODYzfDA&ixlib=rb-4.1.0&q=80&w=1080',
    status: 'offline',
    isFavorite: false,
    serverId: 's2',
  },
  {
    id: '6',
    name: 'Сергей Новиков',
    avatar: 'https://images.unsplash.com/photo-1750741268857-7e44510f867d?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxwcm9mZXNzaW9uYWwlMjBtYW4lMjBwb3J0cmFpdCUyMGNhc3VhbHxlbnwxfHx8fDE3NzIzOTIwMTB8MA&ixlib=rb-4.1.0&q=80&w=1080',
    status: 'online',
    isFavorite: false,
    serverId: 's1',
  },
  {
    id: '7',
    name: 'Ольга Соколова',
    avatar: 'https://images.unsplash.com/photo-1689600944138-da3b150d9cb8?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxwcm9mZXNzaW9uYWwlMjBoZWFkc2hvdCUyMHdvbWFufGVufDF8fHx8MTc3MjQ0MzgwMnww&ixlib=rb-4.1.0&q=80&w=1080',
    status: 'busy',
    isFavorite: false,
    serverId: 's3',
  },
  {
    id: '8',
    name: 'Игорь Морозов',
    avatar: 'https://images.unsplash.com/photo-1723537742563-15c3d351dbf2?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxwcm9mZXNzaW9uYWwlMjBoZWFkc2hvdCUyMG1hbiUyMGJ1c2luZXNzfGVufDF8fHx8MTc3MjQyMDMwMXww&ixlib=rb-4.1.0&q=80&w=1080',
    status: 'offline',
    isFavorite: false,
    serverId: 's4',
  },
  {
    id: '9',
    name: 'Наталья Попова',
    avatar: 'https://images.unsplash.com/photo-1623594675959-02360202d4d6?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxwcm9mZXNzaW9uYWwlMjBwb3J0cmFpdCUyMHdvbWFuJTIwc21pbGluZ3xlbnwxfHx8fDE3NzI0NzE2NDR8MA&ixlib=rb-4.1.0&q=80&w=1080',
    status: 'online',
    isFavorite: false,
    serverId: 's2',
  },
  {
    id: '10',
    name: 'Владимир Кузнецов',
    avatar: 'https://images.unsplash.com/photo-1762753674498-73ec49feafc4?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxwcm9mZXNzaW9uYWwlMjBoZWFkc2hvdCUyMHlvdW5nJTIwbWFufGVufDF8fHx8MTc3MjQ3MTY0NHww&ixlib=rb-4.1.0&q=80&w=1080',
    status: 'busy',
    isFavorite: false,
    serverId: 's3',
  },
];

export default function App() {
  const [contacts, setContacts] = useState<Contact[]>(initialContacts);
  const [serverList, setServerList] = useState<Server[]>(servers);
  const [serverProfiles, setServerProfiles] = useState<ServerProfile[]>([
    { serverId: 's1', username: 'alex_tech', name: 'Александр', avatar: 'https://images.unsplash.com/photo-1605298046196-e205d0d699d7?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxwcm9mZXNzaW9uYWwlMjBtYW4lMjBwb3J0cmFpdCUyMHNtaWxpbmd8ZW58MXx8fHwxNzcyNDM5NDM2fDA&ixlib=rb-4.1.0&q=80&w=1080', isAdmin: true },
    { serverId: 's2', username: 'alex_creative', name: 'Александр Дизайнер', avatar: 'https://images.unsplash.com/photo-1605298046196-e205d0d699d7?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxwcm9mZXNzaW9uYWwlMjBtYW4lMjBwb3J0cmFpdCUyMHNtaWxpbmd8ZW58MXx8fHwxNzcyNDM5NDM2fDA&ixlib=rb-4.1.0&q=80&w=1080', isAdmin: false },
  ]);
  const [joinRequests, setJoinRequests] = useState<JoinRequest[]>([
    {
      id: 'req1',
      userId: 'u1',
      userName: 'Иван Петров',
      userAvatar: 'https://images.unsplash.com/photo-1762753674498-73ec49feafc4?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxwcm9mZXNzaW9uYWwlMjBoZWFkc2hvdCUyMHlvdW5nJTIwbWFufGVufDF8fHx8MTc3MjQ3MTY0NHww&ixlib=rb-4.1.0&q=80&w=1080',
      username: 'ivan_petrov',
      serverId: 's1',
      status: 'pending',
      createdAt: new Date('2026-03-02T10:30:00'),
    },
    {
      id: 'req2',
      userId: 'u2',
      userName: 'Мария Сидорова',
      userAvatar: 'https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxidXNpbmVzcyUyMHdvbWFuJTIwcG9ydHJhaXR8ZW58MXx8fHwxNzcyNDY3ODYzfDA&ixlib=rb-4.1.0&q=80&w=1080',
      username: 'maria_sidorova',
      serverId: 's1',
      status: 'pending',
      createdAt: new Date('2026-03-03T09:15:00'),
    },
  ]);
  const [notifications, setNotifications] = useState<Notification[]>([
    {
      id: 'notif1',
      type: 'request_pending',
      serverName: 'Tech Community',
      serverId: 's1',
      message: 'Ваша заявка на вступление отправлена администратору',
      createdAt: new Date('2026-03-03T10:00:00'),
      read: false,
    },
  ]);
  const [activeCall, setActiveCall] = useState<Contact | null>(null);
  const [incomingCall, setIncomingCall] = useState<{
    contact: Contact;
    type: 'voice' | 'video';
  } | null>(null);
  const [activeView, setActiveView] = useState<'home' | 'profile' | 'server' | 'addServer' | 'createProfile' | 'userProfile' | 'serverManagement' | 'joinRequests' | 'pendingRequest' | 'notifications' | 'settings'>('home');
  const [selectedServerId, setSelectedServerId] = useState<string | null>(null);
  const [selectedContactId, setSelectedContactId] = useState<string | null>(null);
  const [pendingServerConnection, setPendingServerConnection] = useState<{
    ip: string;
    apiKey?: string;
    serverName: string;
    serverId?: string;
  } | null>(null);
  const [theme, setTheme] = useState<'light' | 'dark'>('light');
  const [userStatus, setUserStatus] = useState<'online' | 'offline' | 'busy'>('online');

  // Apply theme to document
  useEffect(() => {
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [theme]);

  const handleCall = (id: string, type: 'voice' | 'video') => {
    const contact = contacts.find((c) => c.id === id);
    if (contact) {
      setActiveCall(contact);
    }
  };

  const handleEndCall = () => {
    setActiveCall(null);
  };

  const handleServerClick = (serverId: string) => {
    setSelectedServerId(serverId);
    setActiveView('server');
  };

  const handleBackToHome = () => {
    setActiveView('home');
    setSelectedServerId(null);
  };

  const handleBackToServer = () => {
    setActiveView('server');
    setSelectedContactId(null);
  };

  const handleAddServerClick = () => {
    setActiveView('addServer');
  };

  const handleProfileClick = () => {
    setActiveView('profile');
  };

  const handleContactClick = (contactId: string) => {
    setSelectedContactId(contactId);
    setActiveView('userProfile');
  };

  const handleManageServer = () => {
    setActiveView('serverManagement');
  };

  const handleSaveServer = (serverId: string, name: string, description: string, image: string, username: string) => {
    setServerList(serverList.map(s =>
      s.id === serverId ? { ...s, name, description, image, username } : s
    ));
  };

  const handleRemoveMember = (userId: string) => {
    setContacts(contacts.filter(c => c.id !== userId));
  };

  const handleToggleFavorite = (userId: string) => {
    setContacts(contacts.map(c => 
      c.id === userId ? { ...c, isFavorite: !c.isFavorite } : c
    ));
  };

  const handleSaveProfile = (serverId: string, username: string, name: string, avatar?: string) => {
    setServerProfiles(serverProfiles.map(p =>
      p.serverId === serverId ? { ...p, username, name, avatar } : p
    ));
  };

  const handleConnectToServer = (ip: string, apiKey?: string) => {
    // Simulate server connection - in real app would make API call
    const mockServerName = 'New Server ' + (serverList.length + 1);
    const mockServerId = 's' + (serverList.length + 1);
    
    // Check if user has profile for this server
    const hasProfile = serverProfiles.some(p => p.serverId === mockServerId);
    
    if (!hasProfile) {
      // Need to create profile first
      setPendingServerConnection({
        ip,
        apiKey,
        serverName: mockServerName,
      });
      setActiveView('createProfile');
    } else {
      // Profile exists, just add server
      setActiveView('home');
    }
  };

  const handleCreateProfile = (username: string, name: string, avatar?: string) => {
    if (pendingServerConnection) {
      // Create profile for the new server
      const mockServerId = 's' + (serverList.length + 1);
      const hasApiKey = !!pendingServerConnection.apiKey;
      const newProfile: ServerProfile = {
        serverId: mockServerId,
        username,
        name,
        avatar,
        isAdmin: hasApiKey,
      };
      setServerProfiles([...serverProfiles, newProfile]);
      
      // Store connection info to show pending request screen
      setPendingServerConnection({
        ...pendingServerConnection,
        serverId: mockServerId,
      });
      
      // If not admin, show pending request screen and create join request
      if (!hasApiKey) {
        const newRequest: JoinRequest = {
          id: 'req' + (joinRequests.length + 1),
          userId: 'currentUser',
          userName: name,
          userAvatar: avatar,
          username,
          serverId: mockServerId,
          status: 'pending',
          createdAt: new Date(),
        };
        setJoinRequests([...joinRequests, newRequest]);
        
        // Create notification
        const newNotification: Notification = {
          id: 'notif' + (notifications.length + 1),
          type: 'request_pending',
          serverName: pendingServerConnection.serverName,
          serverId: mockServerId,
          message: 'Ваша заявка на вступление отправлена администратору',
          createdAt: new Date(),
          read: false,
        };
        setNotifications([newNotification, ...notifications]);
        
        setActiveView('pendingRequest');
      } else {
        // Admin - go straight to home
        setPendingServerConnection(null);
        setActiveView('home');
      }
    }
  };

  const handleViewRequests = () => {
    setActiveView('joinRequests');
  };

  const handleApproveRequest = (requestId: string) => {
    const request = joinRequests.find(r => r.id === requestId);
    if (request) {
      // Update request status
      setJoinRequests(joinRequests.map(r =>
        r.id === requestId ? { ...r, status: 'approved' } : r
      ));
      
      // Add notification for user
      const newNotification: Notification = {
        id: 'notif' + (notifications.length + 1),
        type: 'request_approved',
        serverName: getServerName(request.serverId),
        serverId: request.serverId,
        message: 'Ваша заявка на вступление одобрена администратором',
        createdAt: new Date(),
        read: false,
      };
      setNotifications([newNotification, ...notifications]);
    }
  };

  const handleRejectRequest = (requestId: string) => {
    const request = joinRequests.find(r => r.id === requestId);
    if (request) {
      // Update request status
      setJoinRequests(joinRequests.map(r =>
        r.id === requestId ? { ...r, status: 'rejected' } : r
      ));
      
      // Add notification for user
      const newNotification: Notification = {
        id: 'notif' + (notifications.length + 1),
        type: 'request_rejected',
        serverName: getServerName(request.serverId),
        serverId: request.serverId,
        message: 'Ваша заявка на вступление отклонена администратором',
        createdAt: new Date(),
        read: false,
      };
      setNotifications([newNotification, ...notifications]);
    }
  };

  const handleNotificationsClick = () => {
    setActiveView('notifications');
  };

  const handleMarkAsRead = (notificationId: string) => {
    setNotifications(notifications.map(n =>
      n.id === notificationId ? { ...n, read: true } : n
    ));
  };

  const handleClearAllNotifications = () => {
    setNotifications([]);
  };

  const handleSettingsClick = () => {
    setActiveView('settings');
  };

  const handleThemeChange = (newTheme: 'light' | 'dark') => {
    setTheme(newTheme);
  };

  const handleStatusChange = (newStatus: 'online' | 'offline' | 'busy') => {
    setUserStatus(newStatus);
  };

  const handleAcceptCall = () => {
    if (incomingCall) {
      setActiveCall(incomingCall.contact);
      setIncomingCall(null);
    }
  };

  const handleDeclineCall = () => {
    setIncomingCall(null);
  };

  // Mock function to simulate incoming call (for testing)
  const simulateIncomingCall = () => {
    const randomContact = contacts[Math.floor(Math.random() * contacts.length)];
    const randomType = Math.random() > 0.5 ? 'video' : 'voice';
    setIncomingCall({ contact: randomContact, type: randomType as 'voice' | 'video' });
  };

  const getServerUsername = (serverId: string) => {
    return serverList.find((s) => s.id === serverId)?.username || '';
  };

  const favoriteContacts = contacts.filter((c) => c.isFavorite);
  const selectedServer = serverList.find((s) => s.id === selectedServerId);
  const selectedContact = contacts.find((c) => c.id === selectedContactId);
  const currentServerProfile = selectedServerId 
    ? serverProfiles.find(p => p.serverId === selectedServerId)
    : null;
  const serverMembers = selectedServerId
    ? contacts.filter((c) => c.serverId === selectedServerId)
    : [];
  const pendingRequestsForServer = selectedServerId
    ? joinRequests.filter(r => r.serverId === selectedServerId && r.status === 'pending')
    : [];
  const unreadNotifications = notifications.filter(n => !n.read);

  const getServerName = (serverId: string) => {
    return serverList.find((s) => s.id === serverId)?.name || '';
  };

  return (
    <div className="size-full bg-background">
      {/* Incoming Call Screen - highest priority */}
      {incomingCall && (
        <IncomingCallScreen
          contact={{ name: incomingCall.contact.name, avatar: incomingCall.contact.avatar }}
          serverName={getServerName(incomingCall.contact.serverId)}
          callType={incomingCall.type}
          onAccept={handleAcceptCall}
          onDecline={handleDeclineCall}
        />
      )}

      {/* Active Call Screen */}
      {activeCall && !incomingCall && (
        <CallScreen
          contact={{ name: activeCall.name, avatar: activeCall.avatar }}
          onEndCall={handleEndCall}
        />
      )}

      {activeView === 'addServer' ? (
        <AddServerScreen 
          onBack={handleBackToHome}
          onConnect={handleConnectToServer}
        />
      ) : activeView === 'createProfile' && pendingServerConnection ? (
        <CreateProfileScreen
          serverName={pendingServerConnection.serverName}
          onCreateProfile={handleCreateProfile}
        />
      ) : activeView === 'pendingRequest' && pendingServerConnection ? (
        <PendingRequestScreen
          serverName={pendingServerConnection.serverName}
          userName={serverProfiles.find(p => p.serverId === pendingServerConnection.serverId)?.name || ''}
          status="pending"
          onBackToHome={handleBackToHome}
        />
      ) : activeView === 'joinRequests' && selectedServer ? (
        <JoinRequestsScreen
          requests={pendingRequestsForServer}
          serverName={selectedServer.name}
          onBack={handleBackToServer}
          onApprove={handleApproveRequest}
          onReject={handleRejectRequest}
        />
      ) : activeView === 'notifications' ? (
        <NotificationsScreen
          notifications={notifications}
          onBack={handleBackToHome}
          onMarkAsRead={handleMarkAsRead}
          onClearAll={handleClearAllNotifications}
        />
      ) : activeView === 'settings' ? (
        <SettingsScreen
          theme={theme}
          userStatus={userStatus}
          onBack={handleBackToHome}
          onThemeChange={handleThemeChange}
          onStatusChange={handleStatusChange}
        />
      ) : activeView === 'userProfile' && selectedContact && selectedServer ? (
        <UserProfileScreen
          userId={selectedContact.id}
          name={selectedContact.name}
          username={`user_${selectedContact.id}`}
          avatar={selectedContact.avatar}
          serverName={selectedServer.name}
          isAdmin={false}
          isFavorite={selectedContact.isFavorite}
          onBack={handleBackToServer}
          onToggleFavorite={handleToggleFavorite}
        />
      ) : activeView === 'serverManagement' && selectedServer && currentServerProfile?.isAdmin ? (
        <ServerManagementScreen
          server={selectedServer}
          onBack={handleBackToServer}
          onSave={handleSaveServer}
        />
      ) : activeView === 'profile' && selectedServer && currentServerProfile ? (
        <ProfileScreen 
          serverId={selectedServer.id}
          serverName={selectedServer.name}
          isAdmin={currentServerProfile.isAdmin}
          initialProfile={{
            name: currentServerProfile.name,
            username: currentServerProfile.username,
            avatar: currentServerProfile.avatar,
          }}
          onBack={handleBackToServer}
          onSaveProfile={handleSaveProfile}
        />
      ) : activeView === 'server' && selectedServer ? (
        <ServerScreen
          server={selectedServer}
          members={serverMembers}
          isAdmin={currentServerProfile?.isAdmin || false}
          pendingRequestsCount={pendingRequestsForServer.length}
          onBack={handleBackToHome}
          onCall={handleCall}
          onProfileClick={handleProfileClick}
          onContactClick={handleContactClick}
          onManageServer={handleManageServer}
          onRemoveMember={handleRemoveMember}
          onViewRequests={handleViewRequests}
        />
      ) : (
        <>
          <Header 
            onNotificationsClick={handleNotificationsClick}
            onSettingsClick={handleSettingsClick}
            notificationCount={unreadNotifications.length}
          />

          <main className="max-w-4xl mx-auto px-6 py-6 pb-24">
            {/* Favorite Contacts */}
            {favoriteContacts.length > 0 && (
              <section className="mb-8">
                <div className="flex items-center gap-2 mb-2">
                  <h2 className="font-semibold text-foreground">Избранные</h2>
                  <span className="px-2 py-0.5 text-sm font-medium bg-accent/20 text-accent-foreground rounded-full">
                    {favoriteContacts.length}
                  </span>
                </div>
                <div className="bg-card rounded-xl shadow-sm border divide-y">
                  {favoriteContacts.map((contact) => (
                    <ContactCard
                      key={contact.id}
                      {...contact}
                      serverName={getServerUsername(contact.serverId)}
                      onCall={handleCall}
                      onContactClick={handleContactClick}
                    />
                  ))}
                </div>
              </section>
            )}

            {/* Servers */}
            <section>
              <div className="flex items-center gap-2 mb-2">
                <h2 className="font-semibold text-foreground">Серверы</h2>
                <span className="px-2 py-0.5 text-sm font-medium bg-primary/20 text-primary rounded-full">
                  {serverList.length}
                </span>
              </div>
              <div className="bg-card rounded-xl shadow-sm border divide-y">
                {serverList.map((server) => (
                  <ServerCard
                    key={server.id}
                    {...server}
                    onClick={() => handleServerClick(server.id)}
                  />
                ))}
              </div>
            </section>
          </main>

          {/* FAB Button */}
          <button
            onClick={handleAddServerClick}
            className="fixed bottom-6 right-6 size-14 rounded-full bg-primary text-primary-foreground shadow-lg hover:opacity-90 transition-all hover:shadow-xl flex items-center justify-center group"
            aria-label="Добавить сервер"
          >
            <Plus className="size-6 group-hover:scale-110 transition-transform" />
          </button>

          {/* Test Button for Incoming Call (development only) */}
          <button
            onClick={simulateIncomingCall}
            className="fixed bottom-6 left-6 px-4 py-2 rounded-full text-primary-foreground text-sm shadow-lg transition-all hover:shadow-xl hover:opacity-90"
            style={{ backgroundColor: 'var(--status-online)' }}
            aria-label="Симулировать входящий звонок"
          >
            Тест звонок
          </button>
        </>
      )}
    </div>
  );
}