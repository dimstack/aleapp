import { ArrowLeft, Star } from 'lucide-react';
import { Avatar, AvatarFallback, AvatarImage } from './ui/avatar';
import { Button } from './ui/button';

interface UserProfileScreenProps {
  userId: string;
  name: string;
  username: string;
  avatar: string;
  serverName: string;
  isAdmin: boolean;
  isFavorite: boolean;
  onBack: () => void;
  onToggleFavorite: (userId: string) => void;
}

export function UserProfileScreen({
  userId,
  name,
  username,
  avatar,
  serverName,
  isAdmin,
  isFavorite,
  onBack,
  onToggleFavorite,
}: UserProfileScreenProps) {
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
            <h1 className="text-2xl font-semibold text-foreground">Профиль пользователя</h1>
          </div>
        </div>
      </header>

      {/* Profile Content */}
      <main className="max-w-2xl mx-auto px-6 py-8">
        {/* Avatar Section */}
        <div className="flex flex-col items-center mb-8">
          <Avatar className="size-32 ring-4 ring-secondary">
            <AvatarImage src={avatar} alt={name} />
            <AvatarFallback className="text-3xl">
              {name.slice(0, 2).toUpperCase()}
            </AvatarFallback>
          </Avatar>
          <h3 className="text-2xl font-semibold mt-4 text-foreground">{name}</h3>
          <p className="text-muted-foreground mt-1">@{username}</p>
        </div>

        {/* Add to Favorites Button */}
        <div className="flex justify-center mb-6">
          <Button
            onClick={() => onToggleFavorite(userId)}
            className="gap-2"
            variant={isFavorite ? "outline" : "default"}
          >
            <Star className={`size-4 ${isFavorite ? 'fill-accent text-accent' : ''}`} />
            {isFavorite ? 'Удалить из избранного' : 'Добавить в избранное'}
          </Button>
        </div>

        {/* Profile Info */}
        <div className="space-y-4">
          <div className="bg-card rounded-xl p-6 space-y-4 shadow-sm border">
            {/* Server */}
            <div>
              <p className="text-sm font-medium text-muted-foreground mb-1">Сервер</p>
              <p className="text-card-foreground">{serverName}</p>
            </div>

            {/* Role */}
            <div>
              <p className="text-sm font-medium text-muted-foreground mb-1">Роль</p>
              <div className="flex items-center gap-2">
                {isAdmin ? (
                  <span className="px-3 py-1 bg-primary/20 text-primary rounded-full text-sm font-medium">
                    Администратор
                  </span>
                ) : (
                  <span className="px-3 py-1 bg-secondary text-muted-foreground rounded-full text-sm font-medium">
                    Участник
                  </span>
                )}
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}