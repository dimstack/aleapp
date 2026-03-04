import { ArrowLeft, Check, X } from 'lucide-react';
import { Avatar, AvatarFallback, AvatarImage } from './ui/avatar';
import { Button } from './ui/button';

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

interface JoinRequestsScreenProps {
  requests: JoinRequest[];
  serverName: string;
  onBack: () => void;
  onApprove: (requestId: string) => void;
  onReject: (requestId: string) => void;
}

export function JoinRequestsScreen({ 
  requests, 
  serverName, 
  onBack, 
  onApprove, 
  onReject 
}: JoinRequestsScreenProps) {
  const pendingRequests = requests.filter(r => r.status === 'pending');

  return (
    <div className="size-full bg-background overflow-y-auto">
      {/* Header */}
      <header className="border-b bg-card sticky top-0 z-10 shadow-sm">
        <div className="px-6 py-4">
          <div className="flex items-center gap-3">
            <button
              onClick={onBack}
              className="p-2 rounded-full hover:bg-secondary transition-colors"
              aria-label="Назад"
            >
              <ArrowLeft className="size-5 text-muted-foreground" />
            </button>
            <div className="flex-1">
              <h1 className="text-2xl font-semibold text-foreground">Заявки на вступление</h1>
              <p className="text-sm text-muted-foreground mt-0.5">{serverName}</p>
            </div>
          </div>
        </div>
      </header>

      {/* Content */}
      <main className="max-w-4xl mx-auto px-6 py-6">
        <div className="flex items-center gap-2 mb-2">
          <h3 className="font-semibold text-foreground">Новые заявки</h3>
          <span className="px-2 py-0.5 text-sm font-medium bg-accent/20 text-accent-foreground rounded-full">
            {pendingRequests.length}
          </span>
        </div>

        {pendingRequests.length > 0 ? (
          <div className="bg-card rounded-xl shadow-sm border divide-y">
            {pendingRequests.map((request) => (
              <div key={request.id} className="p-4 hover:bg-secondary/50 transition-colors">
                <div className="flex items-center gap-4">
                  <Avatar className="size-14 flex-shrink-0">
                    <AvatarImage src={request.userAvatar} alt={request.userName} />
                    <AvatarFallback>
                      {request.userName.slice(0, 2).toUpperCase()}
                    </AvatarFallback>
                  </Avatar>

                  <div className="flex-1 min-w-0">
                    <h3 className="font-medium text-card-foreground truncate">
                      {request.userName}
                    </h3>
                    <p className="text-sm text-muted-foreground">@{request.username}</p>
                    <p className="text-xs text-muted-foreground mt-1">
                      {new Date(request.createdAt).toLocaleDateString('ru-RU', {
                        day: 'numeric',
                        month: 'long',
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </p>
                  </div>

                  <div className="flex items-center gap-2">
                    <Button
                      onClick={() => onApprove(request.id)}
                      size="sm"
                      className="gap-2"
                      style={{ backgroundColor: 'var(--status-online)' }}
                    >
                      <Check className="size-4" />
                      Принять
                    </Button>
                    <Button
                      onClick={() => onReject(request.id)}
                      size="sm"
                      variant="outline"
                      className="gap-2 text-destructive border-destructive/30 hover:bg-destructive/10"
                    >
                      <X className="size-4" />
                      Отклонить
                    </Button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="bg-card rounded-xl p-12 text-center shadow-sm border">
            <div className="size-16 rounded-full bg-secondary flex items-center justify-center mx-auto mb-4">
              <Check className="size-8 text-muted-foreground" />
            </div>
            <p className="text-muted-foreground font-medium">Новых заявок нет</p>
            <p className="text-sm text-muted-foreground mt-1">
              Все заявки обработаны
            </p>
          </div>
        )}
      </main>
    </div>
  );
}