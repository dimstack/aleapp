import { Phone, Trash2 } from 'lucide-react';
import { Avatar, AvatarFallback, AvatarImage } from './ui/avatar';

interface ContactCardProps {
  id: string;
  name: string;
  avatar: string;
  status: 'online' | 'offline' | 'busy';
  isFavorite?: boolean;
  serverName?: string;
  showServerName?: boolean;
  showRemoveButton?: boolean;
  onCall?: (id: string, type: 'voice' | 'video') => void;
  onContactClick?: (id: string) => void;
  onRemoveMember?: (id: string) => void;
}

export function ContactCard({
  id,
  name,
  avatar,
  status,
  isFavorite = false,
  serverName,
  showServerName = true,
  showRemoveButton = false,
  onCall,
  onContactClick,
  onRemoveMember,
}: ContactCardProps) {
  const statusColors = {
    online: '[--status-online]',
    offline: '[--status-offline]',
    busy: '[--status-busy]',
  };

  const statusLabels = {
    online: 'В сети',
    offline: 'Не в сети',
    busy: 'Занят',
  };

  const handleRemove = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (window.confirm(`Удалить ${name} с сервера?`)) {
      onRemoveMember?.(id);
    }
  };

  return (
    <div className="flex items-center gap-4 p-4 hover:bg-secondary/50 rounded-lg transition-colors">
      <button 
        onClick={() => onContactClick?.(id)}
        className="relative flex-shrink-0"
      >
        <Avatar className="size-14">
          <AvatarImage src={avatar} alt={name} />
          <AvatarFallback>{name.slice(0, 2).toUpperCase()}</AvatarFallback>
        </Avatar>
        <div
          className={`absolute bottom-0 right-0 size-4 rounded-full border-2 border-card`}
          style={{
            backgroundColor: status === 'online' 
              ? 'var(--status-online)' 
              : status === 'busy' 
              ? 'var(--status-busy)' 
              : 'var(--status-offline)'
          }}
        />
      </button>

      <button 
        onClick={() => onContactClick?.(id)}
        className="flex-1 min-w-0 text-left"
      >
        <h3 className="font-medium text-card-foreground truncate">{name}</h3>
        <p className="text-sm text-muted-foreground">
          {showServerName && serverName ? serverName : statusLabels[status]}
        </p>
      </button>

      <div className="flex items-center gap-2">
        {showRemoveButton ? (
          <button
            onClick={handleRemove}
            className="p-2 rounded-full hover:bg-destructive/10 text-destructive transition-colors"
            aria-label="Удалить участника"
          >
            <Trash2 className="size-5" />
          </button>
        ) : (
          <button
            onClick={() => onCall?.(id, 'voice')}
            className="p-2 rounded-full hover:bg-primary/10 text-primary transition-colors"
            aria-label="Голосовой звонок"
          >
            <Phone className="size-5" />
          </button>
        )}
      </div>
    </div>
  );
}