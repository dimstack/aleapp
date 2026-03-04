import { useState } from 'react';
import { Mic, MicOff, Video, VideoOff, Phone, Info, SwitchCamera } from 'lucide-react';
import { Avatar, AvatarFallback, AvatarImage } from './ui/avatar';

interface CallScreenProps {
  contact: {
    name: string;
    avatar: string;
  };
  onEndCall: () => void;
}

export function CallScreen({ contact, onEndCall }: CallScreenProps) {
  const [isMicOn, setIsMicOn] = useState(true);
  const [isCameraOn, setIsCameraOn] = useState(false);
  const [showInfo, setShowInfo] = useState(false);
  const [callDuration, setCallDuration] = useState('00:23');

  return (
    <div className="fixed inset-0 bg-primary z-50 flex flex-col">
      {/* Header */}
      <div className="flex justify-between items-center p-6">
        <div className="text-primary-foreground">
          <p className="text-sm opacity-80">Звонок...</p>
          <p className="text-lg font-medium">{callDuration}</p>
        </div>
        <button
          onClick={() => setShowInfo(!showInfo)}
          className="p-3 rounded-full hover:bg-primary-foreground/10 text-primary-foreground transition-colors"
          aria-label="Информация о звонке"
        >
          <Info className="size-5" />
        </button>
      </div>

      {/* Info Panel */}
      {showInfo && (
        <div className="absolute top-20 right-6 bg-card rounded-xl shadow-xl p-4 w-72 animate-in fade-in slide-in-from-top-2 border">
          <h3 className="font-semibold mb-3 text-card-foreground">Информация о звонке</h3>
          <div className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Контакт:</span>
              <span className="font-medium text-card-foreground">{contact.name}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Длительность:</span>
              <span className="font-medium text-card-foreground">{callDuration}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Качество:</span>
              <span className="font-medium" style={{ color: 'var(--status-online)' }}>Отличное</span>
            </div>
          </div>
        </div>
      )}

      {/* Contact Avatar */}
      <div className="flex-1 flex items-center justify-center">
        <div className="text-center">
          <Avatar className="size-32 mx-auto mb-6 ring-4" style={{ ringColor: 'var(--accent)' }}>
            <AvatarImage src={contact.avatar} alt={contact.name} />
            <AvatarFallback className="text-3xl">
              {contact.name.slice(0, 2).toUpperCase()}
            </AvatarFallback>
          </Avatar>
          <h2 className="text-3xl font-semibold text-primary-foreground mb-2">{contact.name}</h2>
          <p className="text-primary-foreground/80">Звонок...</p>
        </div>
      </div>

      {/* Floating Camera Preview */}
      {isCameraOn && (
        <div className="absolute bottom-32 right-6 w-32 h-40 bg-muted rounded-xl overflow-hidden shadow-2xl border-2" style={{ borderColor: 'var(--accent)' }}>
          <div className="size-full bg-muted flex items-center justify-center">
            <Video className="size-8 text-muted-foreground" />
          </div>
          <div className="absolute top-2 right-2 px-2 py-1 bg-destructive rounded text-destructive-foreground text-xs font-medium">
            REC
          </div>
        </div>
      )}

      {/* Controls */}
      <div className="pb-12 px-6">
        <div className="flex justify-center items-center gap-6">
          {/* Microphone Toggle */}
          <button
            onClick={() => setIsMicOn(!isMicOn)}
            className={`p-4 rounded-full transition-all ${
              isMicOn
                ? 'bg-primary-foreground/10 hover:bg-primary-foreground/20 text-primary-foreground'
                : 'text-primary-foreground'
            }`}
            style={!isMicOn ? { backgroundColor: 'var(--destructive)' } : {}}
            aria-label={isMicOn ? 'Выключить микрофон' : 'Включить микрофон'}
          >
            {isMicOn ? <Mic className="size-6" /> : <MicOff className="size-6" />}
          </button>

          {/* Camera Toggle */}
          <button
            onClick={() => setIsCameraOn(!isCameraOn)}
            className={`p-4 rounded-full transition-all ${
              isCameraOn
                ? 'bg-primary-foreground/10 hover:bg-primary-foreground/20 text-primary-foreground'
                : 'bg-primary-foreground/5 hover:bg-primary-foreground/10 text-primary-foreground'
            }`}
            aria-label={isCameraOn ? 'Выключить камеру' : 'Включить камеру'}
          >
            {isCameraOn ? <Video className="size-6" /> : <VideoOff className="size-6" />}
          </button>

          {/* Switch Camera */}
          <button
            onClick={() => {}}
            className="p-4 rounded-full bg-primary-foreground/10 hover:bg-primary-foreground/20 text-primary-foreground transition-all"
            aria-label="Переключить камеру"
          >
            <SwitchCamera className="size-6" />
          </button>

          {/* End Call */}
          <button
            onClick={onEndCall}
            className="p-4 rounded-full text-primary-foreground transition-all shadow-lg"
            style={{ backgroundColor: 'var(--destructive)' }}
            aria-label="Завершить звонок"
          >
            <Phone className="size-6 rotate-[135deg]" />
          </button>
        </div>
      </div>
    </div>
  );
}