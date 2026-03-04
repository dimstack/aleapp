import { useState } from 'react';
import { ArrowLeft, Edit2, Camera } from 'lucide-react';
import { Avatar, AvatarFallback, AvatarImage } from './ui/avatar';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Button } from './ui/button';

interface ProfileScreenProps {
  serverId: string;
  serverName: string;
  isAdmin: boolean;
  initialProfile: {
    name: string;
    username: string;
    avatar?: string;
  };
  onBack: () => void;
  onSaveProfile: (serverId: string, username: string, name: string, avatar?: string) => void;
}

export function ProfileScreen({ serverId, serverName, isAdmin, initialProfile, onBack, onSaveProfile }: ProfileScreenProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [profile, setProfile] = useState(initialProfile);
  const [editedProfile, setEditedProfile] = useState(initialProfile);

  const handleSave = () => {
    setProfile(editedProfile);
    onSaveProfile(serverId, editedProfile.username, editedProfile.name, editedProfile.avatar);
    setIsEditing(false);
  };

  const handleCancel = () => {
    setEditedProfile(profile);
    setIsEditing(false);
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
            <h1 className="text-2xl font-semibold text-foreground">Мой профиль</h1>
          </div>
        </div>
      </header>

      {/* Profile Content */}
      <main className="max-w-2xl mx-auto px-6 py-8">
        {/* Avatar Section */}
        <div className="flex flex-col items-center mb-8">
          <div className="relative group">
            <Avatar className="size-32 ring-4 ring-secondary">
              <AvatarImage src={profile.avatar} alt={profile.name} />
              <AvatarFallback className="text-3xl">
                {profile.name.slice(0, 2).toUpperCase()}
              </AvatarFallback>
            </Avatar>
            {isEditing && (
              <button
                className="absolute bottom-0 right-0 p-2 rounded-full bg-primary text-primary-foreground hover:bg-primary/90 transition-colors shadow-lg"
                aria-label="Изменить фото"
              >
                <Camera className="size-4" />
              </button>
            )}
          </div>
          <h3 className="text-2xl font-semibold mt-4 text-foreground">{profile.name}</h3>
          <p className="text-muted-foreground mt-1">@{profile.username}</p>
        </div>

        {/* Edit Button */}
        {!isEditing && (
          <div className="flex justify-center mb-6">
            <Button
              onClick={() => setIsEditing(true)}
              className="gap-2"
            >
              <Edit2 className="size-4" />
              Редактировать профиль
            </Button>
          </div>
        )}

        {/* Profile Info */}
        <div className="space-y-6">
          <div className="bg-card rounded-xl p-6 space-y-4 shadow-sm border">
            {/* Name */}
            <div>
              <Label htmlFor="name" className="text-muted-foreground mb-2">
                <span className="font-medium">Имя</span>
              </Label>
              {isEditing ? (
                <Input
                  id="name"
                  value={editedProfile.name}
                  onChange={(e) =>
                    setEditedProfile({ ...editedProfile, name: e.target.value })
                  }
                  className="mt-1"
                />
              ) : (
                <p className="text-card-foreground mt-1">{profile.name}</p>
              )}
            </div>

            {/* Username */}
            <div>
              <Label htmlFor="username" className="text-muted-foreground mb-2">
                <span className="font-medium">Username</span>
              </Label>
              {isEditing ? (
                <div className="relative">
                  <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">@</span>
                  <Input
                    id="username"
                    value={editedProfile.username}
                    onChange={(e) =>
                      setEditedProfile({ ...editedProfile, username: e.target.value })
                    }
                    className="mt-1 pl-8"
                  />
                </div>
              ) : (
                <p className="text-card-foreground mt-1">@{profile.username}</p>
              )}
            </div>
          </div>

          {/* Server Info */}
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

        {/* Action Buttons */}
        {isEditing && (
          <div className="flex gap-3 mt-6">
            <Button
              onClick={handleSave}
              className="flex-1"
            >
              Сохранить
            </Button>
            <Button
              onClick={handleCancel}
              variant="outline"
              className="flex-1"
            >
              Отмена
            </Button>
          </div>
        )}
      </main>
    </div>
  );
}