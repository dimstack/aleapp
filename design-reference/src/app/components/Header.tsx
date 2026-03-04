import { Video, Settings, Search, Bell } from 'lucide-react';
import { Input } from './ui/input';

interface HeaderProps {
  onSearchChange?: (value: string) => void;
  searchValue?: string;
  showSearch?: boolean;
  onNotificationsClick?: () => void;
  onSettingsClick?: () => void;
  notificationCount?: number;
}

export function Header({ 
  onSearchChange, 
  searchValue = '', 
  showSearch = false,
  onNotificationsClick,
  onSettingsClick,
  notificationCount = 0,
}: HeaderProps) {
  return (
    <header className="border-b bg-card sticky top-0 z-10 shadow-sm">
      <div className="px-6 py-4">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <div className="size-10 rounded-full bg-primary flex items-center justify-center">
              <Video className="size-6 text-primary-foreground" />
            </div>
            <h1 className="text-2xl font-semibold text-foreground">CallApp</h1>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={onNotificationsClick}
              className="p-2 rounded-full hover:bg-secondary transition-colors relative"
              aria-label="Уведомления"
            >
              <Bell className="size-5 text-muted-foreground" />
              {notificationCount > 0 && (
                <div className="absolute -top-0.5 -right-0.5 size-5 rounded-full bg-destructive flex items-center justify-center">
                  <span className="text-xs font-bold text-destructive-foreground">
                    {notificationCount > 9 ? '9+' : notificationCount}
                  </span>
                </div>
              )}
            </button>
            <button
              onClick={onSettingsClick}
              className="p-2 rounded-full hover:bg-secondary transition-colors"
              aria-label="Настройки"
            >
              <Settings className="size-5 text-muted-foreground" />
            </button>
          </div>
        </div>

        {showSearch && (
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-5 text-muted-foreground" />
            <Input
              type="text"
              placeholder="Поиск контактов..."
              value={searchValue}
              onChange={(e) => onSearchChange?.(e.target.value)}
              className="pl-10"
            />
          </div>
        )}
      </div>
    </header>
  );
}