import { useState } from 'react';
import { ArrowLeft } from 'lucide-react';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Button } from './ui/button';

interface AddServerScreenProps {
  onBack: () => void;
  onConnect: (token: string) => void;
}

export function AddServerScreen({ onBack, onConnect }: AddServerScreenProps) {
  const [token, setToken] = useState('');
  const [error, setError] = useState('');

  const handleConnect = () => {
    if (!token.trim()) {
      setError('Токен приглашения обязателен');
      return;
    }

    // Basic token format validation: address/code
    const tokenPattern = /^[a-zA-Z0-9._:-]+\/[a-zA-Z0-9]+$/;
    if (!tokenPattern.test(token.trim())) {
      setError('Неверный формат токена. Ожидается: server:port/CODE');
      return;
    }

    setError('');
    onConnect(token.trim());
  };

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
            <h1 className="text-2xl font-semibold text-foreground">Подключение к серверу</h1>
          </div>
        </div>
      </header>

      {/* Content */}
      <main className="max-w-xl mx-auto px-6 py-8">
        <div className="bg-card rounded-xl p-6 shadow-sm border space-y-6">
          <div className="text-center mb-6">
            <div className="size-16 rounded-full bg-primary/10 flex items-center justify-center mx-auto mb-3">
              <svg
                className="size-8 text-primary"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M5 12h14M5 12a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v4a2 2 0 01-2 2M5 12a2 2 0 00-2 2v4a2 2 0 002 2h14a2 2 0 002-2v-4a2 2 0 00-2-2m-2-4h.01M17 16h.01"
                />
              </svg>
            </div>
            <p className="text-muted-foreground">
              Вставьте токен приглашения, полученный от администратора сервера.
            </p>
          </div>

          {/* Invite Token */}
          <div>
            <Label htmlFor="token" className="mb-2">
              <span className="font-medium">Токен приглашения</span>
              <span className="text-destructive ml-1">*</span>
            </Label>
            <Input
              id="token"
              type="text"
              placeholder="server.example.com:3000/ABCD1234"
              value={token}
              onChange={(e) => {
                setToken(e.target.value);
                setError('');
              }}
              className="mt-1"
            />
          </div>

          {/* Error Message */}
          {error && (
            <div className="bg-destructive/10 border border-destructive/20 rounded-lg p-3">
              <p className="text-sm text-destructive">{error}</p>
            </div>
          )}

          {/* Connect Button */}
          <Button
            onClick={handleConnect}
            className="w-full"
            size="lg"
          >
            Подключиться
          </Button>
        </div>

        {/* Info */}
        <div className="mt-6 bg-primary/10 border border-primary/20 rounded-lg p-4">
          <h3 className="font-medium text-card-foreground mb-2">Как получить токен?</h3>
          <ul className="text-sm text-muted-foreground space-y-1">
            <li>• Попросите администратора сервера создать для вас токен приглашения</li>
            <li>• Токен имеет формат: server.example.com:3000/ABCD1234</li>
            <li>• Токен может быть одноразовым или многоразовым</li>
          </ul>
        </div>
      </main>
    </div>
  );
}
