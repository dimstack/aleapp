import { Clock, CheckCircle, XCircle, Home } from 'lucide-react';
import { Button } from './ui/button';

interface PendingRequestScreenProps {
  serverName: string;
  userName: string;
  status: 'pending' | 'approved' | 'rejected';
  onBackToHome: () => void;
}

export function PendingRequestScreen({ 
  serverName, 
  userName, 
  status,
  onBackToHome 
}: PendingRequestScreenProps) {
  return (
    <div className="size-full bg-background flex items-center justify-center p-6">
      <div className="max-w-md w-full">
        <div className="bg-card rounded-2xl p-8 shadow-lg border text-center">
          {/* Icon */}
          <div className="mb-6">
            {status === 'pending' && (
              <div className="size-20 rounded-full bg-accent/20 flex items-center justify-center mx-auto">
                <Clock className="size-10 text-accent" />
              </div>
            )}
            {status === 'approved' && (
              <div className="size-20 rounded-full flex items-center justify-center mx-auto" style={{ backgroundColor: 'var(--status-online)', opacity: 0.2 }}>
                <CheckCircle className="size-10" style={{ color: 'var(--status-online)' }} />
              </div>
            )}
            {status === 'rejected' && (
              <div className="size-20 rounded-full bg-destructive/20 flex items-center justify-center mx-auto">
                <XCircle className="size-10 text-destructive" />
              </div>
            )}
          </div>

          {/* Title */}
          <h1 className="text-2xl font-bold text-foreground mb-2">
            {status === 'pending' && 'Заявка отправлена'}
            {status === 'approved' && 'Заявка одобрена!'}
            {status === 'rejected' && 'Заявка отклонена'}
          </h1>

          {/* Description */}
          <p className="text-muted-foreground mb-6">
            {status === 'pending' && (
              <>
                Ваша заявка на вступление в сервер{' '}
                <span className="font-semibold">{serverName}</span> отправлена администратору.
                <br />
                <br />
                Вы получите уведомление, когда администратор рассмотрит вашу заявку.
              </>
            )}
            {status === 'approved' && (
              <>
                Администратор одобрил вашу заявку на вступление в сервер{' '}
                <span className="font-semibold">{serverName}</span>!
                <br />
                <br />
                Теперь вы можете пользоваться всеми возможностями сервера.
              </>
            )}
            {status === 'rejected' && (
              <>
                Администратор отклонил вашу заявку на вступление в сервер{' '}
                <span className="font-semibold">{serverName}</span>.
                <br />
                <br />
                Вы можете попробовать подать заявку снова позже.
              </>
            )}
          </p>

          {/* Profile Info */}
          <div className="bg-secondary rounded-lg p-4 mb-6">
            <p className="text-sm text-muted-foreground mb-1">Ваш профиль</p>
            <p className="font-medium text-card-foreground">{userName}</p>
          </div>

          {/* Action Button */}
          <Button
            onClick={onBackToHome}
            className="w-full gap-2"
            size="lg"
          >
            <Home className="size-5" />
            Вернуться на главную
          </Button>

          {status === 'pending' && (
            <p className="text-xs text-muted-foreground mt-4">
              Проверить статус заявки можно в разделе уведомлений
            </p>
          )}
        </div>
      </div>
    </div>
  );
}