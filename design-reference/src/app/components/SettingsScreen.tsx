import { ArrowLeft, Sun, Moon, Circle, Github, MessageCircle, Info } from 'lucide-react';
import { Button } from './ui/button';

interface SettingsScreenProps {
  theme: 'light' | 'dark';
  userStatus: 'online' | 'offline' | 'busy';
  onBack: () => void;
  onThemeChange: (theme: 'light' | 'dark') => void;
  onStatusChange: (status: 'online' | 'offline' | 'busy') => void;
}

export function SettingsScreen({
  theme,
  userStatus,
  onBack,
  onThemeChange,
  onStatusChange,
}: SettingsScreenProps) {
  const statusOptions = [
    { value: 'online' as const, label: 'В сети', description: 'Вы доступны для звонков' },
    { value: 'busy' as const, label: 'Не беспокоить', description: 'Не получать звонки' },
    { value: 'offline' as const, label: 'Невидимый', description: 'Показываться офлайн' },
  ];

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
            <h1 className="text-2xl font-semibold text-foreground">Настройки</h1>
          </div>
        </div>
      </header>

      {/* Content */}
      <main className="max-w-4xl mx-auto px-6 py-6 space-y-6">
        {/* Theme Section */}
        <section className="bg-card rounded-xl shadow-sm border p-6">
          <h2 className="font-semibold text-card-foreground mb-4 flex items-center gap-2">
            <Sun className="size-5 text-accent" />
            Тема оформления
          </h2>
          <div className="grid grid-cols-2 gap-3">
            <button
              onClick={() => onThemeChange('light')}
              className={`p-4 rounded-lg border-2 transition-all ${
                theme === 'light'
                  ? 'border-primary bg-primary/10'
                  : 'border-border hover:border-muted-foreground/30'
              }`}
            >
              <div className="flex items-center gap-3">
                <div className="size-10 rounded-full bg-secondary flex items-center justify-center">
                  <Sun className="size-5 text-primary" />
                </div>
                <div className="text-left">
                  <div className="font-medium text-card-foreground">Светлая</div>
                  <div className="text-xs text-muted-foreground">Old Money</div>
                </div>
              </div>
            </button>
            <button
              onClick={() => onThemeChange('dark')}
              className={`p-4 rounded-lg border-2 transition-all ${
                theme === 'dark'
                  ? 'border-primary bg-primary/10'
                  : 'border-border hover:border-muted-foreground/30'
              }`}
            >
              <div className="flex items-center gap-3">
                <div className="size-10 rounded-full bg-primary flex items-center justify-center">
                  <Moon className="size-5 text-primary-foreground" />
                </div>
                <div className="text-left">
                  <div className="font-medium text-card-foreground">Темная</div>
                  <div className="text-xs text-muted-foreground">Evening</div>
                </div>
              </div>
            </button>
          </div>
        </section>

        {/* Status Section */}
        <section className="bg-card rounded-xl shadow-sm border p-6">
          <h2 className="font-semibold text-card-foreground mb-4 flex items-center gap-2">
            <Circle 
              className="size-5 fill-current" 
              style={{ color: 'var(--status-online)' }}
            />
            Статус
          </h2>
          <div className="space-y-2">
            {statusOptions.map((option) => (
              <button
                key={option.value}
                onClick={() => onStatusChange(option.value)}
                className={`w-full p-4 rounded-lg border-2 transition-all text-left ${
                  userStatus === option.value
                    ? 'border-primary bg-primary/10'
                    : 'border-border hover:border-muted-foreground/30'
                }`}
              >
                <div className="flex items-center gap-3">
                  <div 
                    className="size-4 rounded-full" 
                    style={{
                      backgroundColor: 
                        option.value === 'online' ? 'var(--status-online)' :
                        option.value === 'busy' ? 'var(--status-busy)' :
                        'var(--status-offline)'
                    }}
                  />
                  <div className="flex-1">
                    <div className="font-medium text-card-foreground">{option.label}</div>
                    <div className="text-sm text-muted-foreground">{option.description}</div>
                  </div>
                  {userStatus === option.value && (
                    <div className="size-5 rounded-full bg-primary flex items-center justify-center">
                      <div className="size-2 rounded-full bg-primary-foreground" />
                    </div>
                  )}
                </div>
              </button>
            ))}
          </div>
        </section>

        {/* About Section */}
        <section className="bg-card rounded-xl shadow-sm border p-6">
          <h2 className="font-semibold text-card-foreground mb-4 flex items-center gap-2">
            <Info className="size-5 text-primary" />
            О приложении
          </h2>
          <div className="space-y-4">
            <div>
              <div className="flex items-center gap-3 mb-3">
                <div className="size-12 rounded-2xl bg-primary flex items-center justify-center">
                  <svg className="size-7 text-primary-foreground" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z"/>
                  </svg>
                </div>
                <div>
                  <h3 className="font-bold text-card-foreground text-lg">CallApp</h3>
                  <p className="text-sm text-muted-foreground">Версия 1.0.0</p>
                </div>
              </div>
              <p className="text-muted-foreground mb-4">
                Приложение для онлайн звонков с организацией вокруг серверов. 
                Общайтесь с коллегами и друзьями в удобном формате.
              </p>
            </div>

            <div className="pt-4 border-t space-y-3">
              <a
                href="https://github.com/callapp/callapp"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-3 p-3 rounded-lg hover:bg-secondary/50 transition-colors group"
              >
                <div className="size-10 rounded-full bg-primary flex items-center justify-center group-hover:scale-110 transition-transform">
                  <Github className="size-5 text-primary-foreground" />
                </div>
                <div className="flex-1">
                  <div className="font-medium text-card-foreground">GitHub</div>
                  <div className="text-sm text-muted-foreground">Исходный код проекта</div>
                </div>
              </a>

              <a
                href="https://t.me/callapp_official"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-3 p-3 rounded-lg hover:bg-secondary/50 transition-colors group"
              >
                <div 
                  className="size-10 rounded-full flex items-center justify-center group-hover:scale-110 transition-transform"
                  style={{ backgroundColor: 'var(--accent)' }}
                >
                  <MessageCircle className="size-5 text-accent-foreground" />
                </div>
                <div className="flex-1">
                  <div className="font-medium text-card-foreground">Telegram</div>
                  <div className="text-sm text-muted-foreground">Новости и поддержка</div>
                </div>
              </a>
            </div>

            <div className="pt-4 border-t">
              <p className="text-xs text-muted-foreground text-center">
                © 2026 CallApp. Все права защищены.
              </p>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}