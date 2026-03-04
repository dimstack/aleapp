import { ArrowLeft, Search, User, Settings, Edit2, X, UserPlus } from 'lucide-react';
import { ContactCard } from './ContactCard';
import { Input } from './ui/input';
import { Button } from './ui/button';
import { useState } from 'react';

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
}

interface ServerScreenProps {
  server: Server;
  members: Contact[];
  isAdmin: boolean;
  pendingRequestsCount?: number;
  onBack: () => void;
  onCall: (id: string, type: 'voice' | 'video') => void;
  onProfileClick: () => void;
  onContactClick: (contactId: string) => void;
  onManageServer?: () => void;
  onRemoveMember?: (userId: string) => void;
  onViewRequests?: () => void;
}

export function ServerScreen({ 
  server, 
  members, 
  isAdmin,
  pendingRequestsCount = 0,
  onBack, 
  onCall, 
  onProfileClick, 
  onContactClick,
  onManageServer,
  onRemoveMember,
  onViewRequests,
}: ServerScreenProps) {
  const [searchValue, setSearchValue] = useState('');
  const [isEditMode, setIsEditMode] = useState(false);

  const filteredMembers = members.filter((member) =>
    member.name.toLowerCase().includes(searchValue.toLowerCase())
  );

  return (
    <div className="size-full bg-background overflow-y-auto">
      {/* Header */}
      <header className="border-b bg-card sticky top-0 z-10 shadow-sm">
        <div className="px-6 py-4">
          <div className="flex items-center justify-between gap-3 mb-4">
            <div className="flex items-center gap-3">
              <button
                onClick={onBack}
                className="p-2 rounded-full hover:bg-secondary transition-colors"
                aria-label="Назад"
              >
                <ArrowLeft className="size-5 text-muted-foreground" />
              </button>
              <h1 className="text-2xl font-semibold text-foreground">{server.name}</h1>
            </div>
            <div className="flex items-center gap-2">
              {isAdmin && (
                <>
                  <button
                    onClick={onViewRequests}
                    className="p-2 rounded-full hover:bg-secondary transition-colors relative"
                    aria-label="Заявки на вступление"
                  >
                    <UserPlus className="size-5 text-muted-foreground" />
                    {pendingRequestsCount > 0 && (
                      <div className="absolute -top-0.5 -right-0.5 size-5 rounded-full bg-accent flex items-center justify-center">
                        <span className="text-xs font-bold text-accent-foreground">
                          {pendingRequestsCount > 9 ? '9+' : pendingRequestsCount}
                        </span>
                      </div>
                    )}
                  </button>
                  <button
                    onClick={onManageServer}
                    className="p-2 rounded-full hover:bg-secondary transition-colors"
                    aria-label="Управление сервером"
                  >
                    <Settings className="size-5 text-muted-foreground" />
                  </button>
                </>
              )}
              <button
                onClick={onProfileClick}
                className="p-2 rounded-full hover:bg-secondary transition-colors"
                aria-label="Мой профиль"
              >
                <User className="size-5 text-muted-foreground" />
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Server Info */}
      <div className="bg-card border-b">
        <div className="max-w-4xl mx-auto px-6 py-6">
          <div className="flex gap-6">
            <div className="size-24 rounded-2xl overflow-hidden flex-shrink-0 bg-secondary">
              <img 
                src={server.image} 
                alt={server.name}
                className="size-full object-cover"
              />
            </div>
            <div className="flex-1">
              <h2 className="text-2xl font-bold text-foreground mb-1">{server.name}</h2>
              <p className="text-muted-foreground mb-3">{server.username}</p>
              <p className="text-muted-foreground">{server.description}</p>
              {isAdmin && (
                <div className="mt-3">
                  <span className="px-3 py-1 bg-primary/20 text-primary rounded-full text-sm font-medium">
                    Вы администратор
                  </span>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Search */}
      <div className="bg-card border-b sticky top-[73px] z-10">
        <div className="max-w-4xl mx-auto px-6 py-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-5 text-muted-foreground" />
            <Input
              type="search"
              placeholder="Поиск участников..."
              className="pl-10"
              value={searchValue}
              onChange={(e) => setSearchValue(e.target.value)}
            />
          </div>
        </div>
      </div>

      {/* Members */}
      <main className="max-w-4xl mx-auto px-6 py-6">
        <div className="flex items-center justify-between mb-2">
          <div className="flex items-center gap-2">
            <h3 className="font-semibold text-foreground">Участники</h3>
            <span className="px-2 py-0.5 text-sm font-medium bg-primary/20 text-primary rounded-full">
              {filteredMembers.length}
            </span>
          </div>
          
          {isAdmin && (
            <button
              onClick={() => setIsEditMode(!isEditMode)}
              className={`p-2 rounded-lg transition-colors flex items-center gap-2 ${
                isEditMode 
                  ? 'bg-destructive/10 text-destructive hover:bg-destructive/20' 
                  : 'hover:bg-secondary text-muted-foreground'
              }`}
              aria-label={isEditMode ? "Отменить редактирование" : "Редактировать участников"}
            >
              {isEditMode ? (
                <>
                  <X className="size-5" />
                  <span className="text-sm font-medium">Отмена</span>
                </>
              ) : (
                <Edit2 className="size-5" />
              )}
            </button>
          )}
        </div>
        
        {filteredMembers.length > 0 ? (
          <div className="bg-card rounded-xl shadow-sm border divide-y">
            {filteredMembers.map((member) => (
              <ContactCard
                key={member.id}
                {...member}
                onCall={onCall}
                onContactClick={onContactClick}
                onRemoveMember={isAdmin && isEditMode ? onRemoveMember : undefined}
                showServerName={false}
                showRemoveButton={isAdmin && isEditMode}
              />
            ))}
          </div>
        ) : (
          <div className="text-center py-12">
            <p className="text-muted-foreground">Участники не найдены</p>
          </div>
        )}
      </main>
    </div>
  );
}