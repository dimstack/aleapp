import { Phone, PhoneOff, Video } from 'lucide-react';
import { Avatar, AvatarFallback, AvatarImage } from './ui/avatar';
import { Button } from './ui/button';
import { useState, useEffect } from 'react';

interface IncomingCallScreenProps {
  contact: {
    name: string;
    avatar: string;
  };
  serverName: string;
  callType: 'voice' | 'video';
  onAccept: () => void;
  onDecline: () => void;
}

export function IncomingCallScreen({
  contact,
  serverName,
  callType,
  onAccept,
  onDecline,
}: IncomingCallScreenProps) {
  const [pulse, setPulse] = useState(false);

  useEffect(() => {
    const interval = setInterval(() => {
      setPulse(prev => !prev);
    }, 1000);

    return () => clearInterval(interval);
  }, []);

  return (
    <div className="fixed inset-0 z-50 bg-secondary flex flex-col items-center justify-between p-8 animate-in fade-in duration-300">
      {/* Top Section */}
      <div className="flex-1 flex flex-col items-center justify-center">
        {/* Avatar with pulse animation */}
        <div className="relative mb-8">
          <div
            className={`absolute inset-0 rounded-full transition-transform duration-1000`}
            style={{
              backgroundColor: 'var(--accent)',
              opacity: pulse ? 0 : 0.3,
              transform: pulse ? 'scale(1.5)' : 'scale(1)',
            }}
          />
          <div
            className={`absolute inset-0 rounded-full transition-transform duration-1000 delay-500`}
            style={{
              backgroundColor: 'var(--accent)',
              opacity: pulse ? 0 : 0.2,
              transform: pulse ? 'scale(1.5)' : 'scale(1)',
            }}
          />
          <Avatar className="size-32 border-4 relative z-10" style={{ borderColor: 'var(--accent)' }}>
            <AvatarImage src={contact.avatar} alt={contact.name} />
            <AvatarFallback className="text-4xl">
              {contact.name.slice(0, 2).toUpperCase()}
            </AvatarFallback>
          </Avatar>
        </div>

        {/* Contact Info */}
        <h1 className="text-4xl font-bold mb-3 text-center text-foreground">{contact.name}</h1>
        <p className="text-xl text-muted-foreground mb-2">{serverName}</p>
        
        {/* Call Type Badge */}
        <div className="flex items-center gap-2 px-4 py-2 bg-card rounded-full border">
          {callType === 'video' ? (
            <>
              <Video className="size-5 text-primary" />
              <span className="font-medium text-card-foreground">Видеозвонок</span>
            </>
          ) : (
            <>
              <Phone className="size-5 text-primary" />
              <span className="font-medium text-card-foreground">Голосовой звонок</span>
            </>
          )}
        </div>

        {/* Pulsing text */}
        <p className="text-lg text-muted-foreground mt-8 animate-pulse">
          Входящий вызов...
        </p>
      </div>

      {/* Action Buttons */}
      <div className="flex items-center gap-20 mb-8">
        {/* Decline Button */}
        <button
          onClick={onDecline}
          className="flex flex-col items-center gap-3 group"
        >
          <div 
            className="size-20 rounded-full flex items-center justify-center shadow-2xl transition-all group-hover:scale-110 group-active:scale-95"
            style={{ backgroundColor: 'var(--call-decline)' }}
          >
            <PhoneOff className="size-10" style={{ color: 'var(--primary-foreground)' }} />
          </div>
          <span className="text-lg font-medium text-foreground">Отклонить</span>
        </button>

        {/* Accept Button */}
        <button
          onClick={onAccept}
          className="flex flex-col items-center gap-3 group"
        >
          <div 
            className="size-20 rounded-full flex items-center justify-center shadow-2xl transition-all group-hover:scale-110 group-active:scale-95 animate-pulse"
            style={{ backgroundColor: 'var(--call-accept)' }}
          >
            <Phone className="size-10" style={{ color: 'var(--primary-foreground)' }} />
          </div>
          <span className="text-lg font-medium text-foreground">Принять</span>
        </button>
      </div>

      {/* Swipe hint for mobile */}
      <div className="text-center text-muted-foreground text-sm">
        <p>Нажмите кнопку для ответа на вызов</p>
      </div>
    </div>
  );
}