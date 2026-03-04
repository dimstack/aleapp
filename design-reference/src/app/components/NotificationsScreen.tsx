import { ArrowLeft, CheckCircle, XCircle, Clock, Bell } from 'lucide-react';
import { Button } from './ui/button';

interface Notification {
  id: string;
  type: 'request_approved' | 'request_rejected' | 'request_pending';
  serverName: string;
  serverId: string;
  message: string;
  createdAt: Date;
  read: boolean;
}

interface NotificationsScreenProps {
  notifications: Notification[];
  onBack: () => void;
  onMarkAsRead: (notificationId: string) => void;
  onClearAll: () => void;
}

export function NotificationsScreen({ 
  notifications, 
  onBack, 
  onMarkAsRead,
  onClearAll 
}: NotificationsScreenProps) {
  const unreadCount = notifications.filter(n => !n.read).length;

  const getIcon = (type: Notification['type']) => {
    switch (type) {
      case 'request_approved':
        return <CheckCircle className="size-6" style={{ color: 'var(--status-online)' }} />;
      case 'request_rejected':
        return <XCircle className="size-6 text-destructive" />;
      case 'request_pending':
        return <Clock className="size-6 text-accent" />;
    }
  };

  const getBgColor = (type: Notification['type']) => {
    switch (type) {
      case 'request_approved':
        return 'bg-[var(--status-online)]/10';
      case 'request_rejected':
        return 'bg-destructive/10';
      case 'request_pending':
        return 'bg-accent/10';
    }
  };

  return (
    <div className="size-full bg-background overflow-y-auto">
      {/* Header */}
      <header className="border-b bg-card sticky top-0 z-10 shadow-sm">
        <div className="px-6 py-4">
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <button
                onClick={onBack}
                className="p-2 rounded-full hover:bg-secondary transition-colors"
                aria-label="Назад"
              >
                <ArrowLeft className="size-5 text-muted-foreground" />
              </button>
              <div>
                <h1 className="text-2xl font-semibold text-foreground">Уведомления</h1>
                {unreadCount > 0 && (
                  <p className="text-sm text-muted-foreground mt-0.5">
                    {unreadCount} непрочитанных
                  </p>
                )}
              </div>
            </div>
            {notifications.length > 0 && (
              <Button
                onClick={onClearAll}
                variant="outline"
                size="sm"
              >
                Очистить все
              </Button>
            )}
          </div>
        </div>
      </header>

      {/* Content */}
      <main className="max-w-4xl mx-auto px-6 py-6">
        {notifications.length > 0 ? (
          <div className="space-y-3">
            {notifications.map((notification) => (
              <div
                key={notification.id}
                className={`bg-card rounded-xl p-4 shadow-sm border transition-all ${
                  notification.read ? 'opacity-60' : ''
                }`}
                onClick={() => !notification.read && onMarkAsRead(notification.id)}
              >
                <div className="flex gap-4">
                  <div className={`size-12 rounded-full ${getBgColor(notification.type)} flex items-center justify-center flex-shrink-0`}>
                    {getIcon(notification.type)}
                  </div>
                  
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2 mb-1">
                      <h3 className="font-medium text-card-foreground">
                        {notification.type === 'request_approved' && 'Заявка одобрена'}
                        {notification.type === 'request_rejected' && 'Заявка отклонена'}
                        {notification.type === 'request_pending' && 'Заявка отправлена'}
                      </h3>
                      {!notification.read && (
                        <div className="size-2 rounded-full bg-primary flex-shrink-0 mt-2" />
                      )}
                    </div>
                    <p className="text-sm text-muted-foreground mb-2">
                      {notification.message}
                    </p>
                    <div className="flex items-center gap-3">
                      <span className="text-xs font-medium text-muted-foreground">
                        {notification.serverName}
                      </span>
                      <span className="text-xs text-muted-foreground">
                        {new Date(notification.createdAt).toLocaleDateString('ru-RU', {
                          day: 'numeric',
                          month: 'long',
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="bg-card rounded-xl p-12 text-center shadow-sm border">
            <div className="size-16 rounded-full bg-secondary flex items-center justify-center mx-auto mb-4">
              <Bell className="size-8 text-muted-foreground" />
            </div>
            <p className="text-muted-foreground font-medium">Уведомлений нет</p>
            <p className="text-sm text-muted-foreground mt-1">
              Здесь будут отображаться уведомления о статусе ваших заявок
            </p>
          </div>
        )}
      </main>
    </div>
  );
}