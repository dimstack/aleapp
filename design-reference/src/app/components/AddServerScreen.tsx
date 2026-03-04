import { useState } from 'react';
import { ArrowLeft } from 'lucide-react';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Button } from './ui/button';

interface AddServerScreenProps {
  onBack: () => void;
  onConnect: (ip: string, apiKey?: string) => void;
}

export function AddServerScreen({ onBack, onConnect }: AddServerScreenProps) {
  const [ip, setIp] = useState('');
  const [apiKey, setApiKey] = useState('');
  const [error, setError] = useState('');

  const handleConnect = () => {
    if (!ip.trim()) {
      setError('IP адрес обязателен');
      return;
    }

    // Basic IP validation
    const ipPattern = /^(\d{1,3}\.){3}\d{1,3}(:\d+)?$|^[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}(:\d+)?$/;
    if (!ipPattern.test(ip.trim())) {
      setError('Неверный формат IP адреса');
      return;
    }

    setError('');
    onConnect(ip.trim(), apiKey.trim() || undefined);
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
              Введите IP адрес сервера для подключения. API ключ нужен только если вы владелец сервера.
            </p>
          </div>

          {/* IP Address */}
          <div>
            <Label htmlFor="ip" className="mb-2">
              <span className="font-medium">IP адрес сервера</span>
              <span className="text-destructive ml-1">*</span>
            </Label>
            <Input
              id="ip"
              type="text"
              placeholder="192.168.1.1:3000 или server.example.com"
              value={ip}
              onChange={(e) => {
                setIp(e.target.value);
                setError('');
              }}
              className="mt-1"
            />
          </div>

          {/* API Key */}
          <div>
            <Label htmlFor="apiKey" className="mb-2">
              <span className="font-medium">API ключ</span>
              <span className="text-muted-foreground text-sm ml-2">(опционально)</span>
            </Label>
            <Input
              id="apiKey"
              type="password"
              placeholder="Введите API ключ если вы владелец"
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
              className="mt-1"
            />
            <p className="text-xs text-muted-foreground mt-1">
              API ключ даёт права администратора сервера
            </p>
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
          <h3 className="font-medium text-card-foreground mb-2">Как получить IP адрес?</h3>
          <ul className="text-sm text-muted-foreground space-y-1">
            <li>• Попросите владельца сервера предоставить IP адрес</li>
            <li>• IP адрес должен быть в формате: xxx.xxx.xxx.xxx:port</li>
            <li>• Или доменное имя: server.example.com:port</li>
          </ul>
        </div>
      </main>
    </div>
  );
}