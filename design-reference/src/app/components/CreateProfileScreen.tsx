import { useState } from 'react';
import { Camera } from 'lucide-react';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Button } from './ui/button';
import { Avatar, AvatarFallback, AvatarImage } from './ui/avatar';

interface CreateProfileScreenProps {
  serverName: string;
  onCreateProfile: (username: string, name: string, avatar?: string) => void;
}

export function CreateProfileScreen({ serverName, onCreateProfile }: CreateProfileScreenProps) {
  const [username, setUsername] = useState('');
  const [name, setName] = useState('');
  const [avatar, setAvatar] = useState('');
  const [error, setError] = useState('');

  const handleCreate = () => {
    if (!username.trim()) {
      setError('Username обязателен');
      return;
    }

    if (!name.trim()) {
      setError('Имя обязательно');
      return;
    }

    // Validate username format
    if (!/^[a-zA-Z0-9_]+$/.test(username.trim())) {
      setError('Username может содержать только буквы, цифры и подчёркивание');
      return;
    }

    setError('');
    onCreateProfile(username.trim(), name.trim(), avatar.trim() || undefined);
  };

  return (
    <div className="size-full bg-background overflow-y-auto flex items-center justify-center">
      <main className="max-w-xl w-full mx-auto px-6 py-8">
        <div className="bg-card rounded-xl p-8 shadow-sm border">
          {/* Header */}
          <div className="text-center mb-8">
            <h1 className="text-3xl font-bold text-foreground mb-2">Создание профиля</h1>
            <p className="text-muted-foreground">
              Создайте свой профиль для сервера{' '}
              <span className="font-semibold text-primary">{serverName}</span>
            </p>
          </div>

          {/* Avatar */}
          <div className="flex flex-col items-center mb-8">
            <div className="relative group">
              <Avatar className="size-24 ring-4 ring-secondary">
                {avatar ? (
                  <AvatarImage src={avatar} alt="Avatar" />
                ) : (
                  <AvatarFallback className="text-2xl bg-primary text-primary-foreground">
                    {name ? name.slice(0, 2).toUpperCase() : '??'}
                  </AvatarFallback>
                )}
              </Avatar>
              <button
                className="absolute bottom-0 right-0 p-2 rounded-full bg-primary text-primary-foreground hover:bg-primary/90 transition-colors shadow-lg"
                aria-label="Изменить фото"
              >
                <Camera className="size-4" />
              </button>
            </div>
            <p className="text-sm text-muted-foreground mt-3">Загрузите фото профиля</p>
          </div>

          {/* Form */}
          <div className="space-y-5">
            {/* Username */}
            <div>
              <Label htmlFor="username" className="mb-2">
                <span className="font-medium">Username</span>
                <span className="text-destructive ml-1">*</span>
              </Label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">@</span>
                <Input
                  id="username"
                  type="text"
                  placeholder="username"
                  value={username}
                  onChange={(e) => {
                    setUsername(e.target.value);
                    setError('');
                  }}
                  className="pl-8 mt-1"
                />
              </div>
              <p className="text-xs text-muted-foreground mt-1">
                Только буквы, цифры и подчёркивание
              </p>
            </div>

            {/* Name */}
            <div>
              <Label htmlFor="name" className="mb-2">
                <span className="font-medium">Имя</span>
                <span className="text-destructive ml-1">*</span>
              </Label>
              <Input
                id="name"
                type="text"
                placeholder="Введите ваше имя"
                value={name}
                onChange={(e) => {
                  setName(e.target.value);
                  setError('');
                }}
                className="mt-1"
              />
            </div>

            {/* Avatar URL (optional) */}
            <div>
              <Label htmlFor="avatar" className="mb-2">
                <span className="font-medium">URL аватара</span>
                <span className="text-muted-foreground text-sm ml-2">(опционально)</span>
              </Label>
              <Input
                id="avatar"
                type="url"
                placeholder="https://example.com/avatar.jpg"
                value={avatar}
                onChange={(e) => setAvatar(e.target.value)}
                className="mt-1"
              />
            </div>

            {/* Error Message */}
            {error && (
              <div className="bg-destructive/10 border border-destructive/20 rounded-lg p-3">
                <p className="text-sm text-destructive">{error}</p>
              </div>
            )}

            {/* Create Button */}
            <Button
              onClick={handleCreate}
              className="w-full"
              size="lg"
            >
              Создать профиль
            </Button>
          </div>
        </div>

        {/* Info */}
        <div className="mt-6 bg-primary/10 border border-primary/20 rounded-lg p-4">
          <p className="text-sm text-muted-foreground">
            💡 Вы можете создать разные профили для разных серверов
          </p>
        </div>
      </main>
    </div>
  );
}