import { useState } from 'react';
import { ArrowLeft, Camera, Trash2 } from 'lucide-react';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Button } from './ui/button';

interface ServerManagementScreenProps {
  server: {
    id: string;
    name: string;
    username: string;
    description: string;
    image: string;
  };
  onBack: () => void;
  onSave: (serverId: string, name: string, description: string, image: string, username: string) => void;
}

export function ServerManagementScreen({ server, onBack, onSave }: ServerManagementScreenProps) {
  const [name, setName] = useState(server.name);
  const [username, setUsername] = useState(server.username);
  const [description, setDescription] = useState(server.description);
  const [image, setImage] = useState(server.image);
  const [error, setError] = useState('');

  const handleSave = () => {
    if (!name.trim()) {
      setError('Название обязательно');
      return;
    }

    if (!username.trim()) {
      setError('Username обязателен');
      return;
    }

    if (!description.trim()) {
      setError('Описание обязательно');
      return;
    }

    setError('');
    onSave(server.id, name.trim(), description.trim(), image.trim(), username.trim());
    onBack();
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
            <h1 className="text-2xl font-semibold text-foreground">Управление сервером</h1>
          </div>
        </div>
      </header>

      {/* Content */}
      <main className="max-w-2xl mx-auto px-6 py-8">
        <div className="bg-card rounded-xl p-6 shadow-sm border space-y-6">
          {/* Server Image Preview */}
          <div className="flex flex-col items-center mb-6">
            <div className="relative group">
              <div className="size-32 rounded-2xl overflow-hidden bg-secondary">
                {image ? (
                  <img 
                    src={image} 
                    alt="Server preview"
                    className="size-full object-cover"
                  />
                ) : (
                  <div className="size-full bg-primary" />
                )}
              </div>
              <button
                className="absolute bottom-0 right-0 p-2 rounded-full bg-primary text-primary-foreground hover:bg-primary/90 transition-colors shadow-lg"
                aria-label="Изменить фото"
              >
                <Camera className="size-4" />
              </button>
            </div>
            <p className="text-sm text-muted-foreground mt-3">Изображение сервера</p>
          </div>

          {/* Form */}
          <div className="space-y-5">
            {/* Server Name */}
            <div>
              <Label htmlFor="name" className="mb-2">
                <span className="font-medium">Название сервера</span>
                <span className="text-destructive ml-1">*</span>
              </Label>
              <Input
                id="name"
                type="text"
                placeholder="Введите название сервера"
                value={name}
                onChange={(e) => {
                  setName(e.target.value);
                  setError('');
                }}
                className="mt-1"
              />
            </div>

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
                  placeholder="server_username"
                  value={username.replace('@', '')}
                  onChange={(e) => {
                    setUsername('@' + e.target.value.replace('@', ''));
                    setError('');
                  }}
                  className="pl-8 mt-1"
                />
              </div>
            </div>

            {/* Description */}
            <div>
              <Label htmlFor="description" className="mb-2">
                <span className="font-medium">Описание</span>
                <span className="text-destructive ml-1">*</span>
              </Label>
              <textarea
                id="description"
                placeholder="Введите описание сервера"
                value={description}
                onChange={(e) => {
                  setDescription(e.target.value);
                  setError('');
                }}
                className="mt-1 w-full min-h-24 px-3 py-2 rounded-lg border bg-input-background text-foreground border-border focus:outline-none focus:ring-2 focus:ring-ring resize-none"
              />
            </div>

            {/* Image URL */}
            <div>
              <Label htmlFor="image" className="mb-2">
                <span className="font-medium">URL изображения</span>
              </Label>
              <Input
                id="image"
                type="url"
                placeholder="https://example.com/image.jpg"
                value={image}
                onChange={(e) => setImage(e.target.value)}
                className="mt-1"
              />
            </div>

            {/* Error Message */}
            {error && (
              <div className="bg-destructive/10 border border-destructive/20 rounded-lg p-3">
                <p className="text-sm text-destructive">{error}</p>
              </div>
            )}

            {/* Action Buttons */}
            <div className="flex gap-3 pt-4">
              <Button onClick={handleSave} className="flex-1" size="lg">
                Сохранить изменения
              </Button>
              <Button onClick={onBack} variant="outline" className="flex-1" size="lg">
                Отмена
              </Button>
            </div>
          </div>
        </div>

        {/* Invite Tokens Link */}
        <div
          className="mt-6 bg-card rounded-xl p-5 shadow-sm border cursor-pointer hover:bg-secondary/50 transition-colors"
          onClick={() => {/* navigate to invite tokens */}}
        >
          <div className="flex items-center gap-3">
            <div className="flex-1">
              <h3 className="font-medium text-foreground">Токены приглашений</h3>
              <p className="text-sm text-muted-foreground mt-1">
                Управление токенами для приглашения новых участников
              </p>
            </div>
            <ArrowLeft className="size-5 text-muted-foreground rotate-180" />
          </div>
        </div>

        {/* Danger Zone */}
        <div className="mt-6 bg-destructive/10 border border-destructive/20 rounded-xl p-6">
          <div className="flex items-start gap-3">
            <Trash2 className="size-5 text-destructive mt-0.5 flex-shrink-0" />
            <div className="flex-1">
              <h3 className="font-semibold text-destructive mb-1">Опасная зона</h3>
              <p className="text-sm text-muted-foreground mb-3">
                Удаление сервера приведет к потере всех данных. Это действие нельзя отменить.
              </p>
              <Button variant="outline" className="border-destructive/30 text-destructive hover:bg-destructive/10">
                Удалить сервер
              </Button>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}