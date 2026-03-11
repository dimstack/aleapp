import { useState } from 'react';
import { ArrowLeft, Plus, Copy, X } from 'lucide-react';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Switch } from './ui/switch';
import { Badge } from './ui/badge';

interface InviteToken {
  id: string;
  code: string;
  label: string;
  maxUses: number;
  useCount: number;
  grantedRole: 'ADMIN' | 'MEMBER';
  requireApproval: boolean;
  revoked: boolean;
}

interface InviteTokensScreenProps {
  onBack: () => void;
  serverAddress?: string;
}

const sampleTokens: InviteToken[] = [
  { id: 't1', code: 'ABC12345', label: 'Для команды дизайна', maxUses: 10, useCount: 3, grantedRole: 'MEMBER', requireApproval: false, revoked: false },
  { id: 't2', code: 'XYZ98765', label: 'Админ-доступ', maxUses: 1, useCount: 1, grantedRole: 'ADMIN', requireApproval: true, revoked: false },
  { id: 't3', code: 'OLD54321', label: 'Старый токен', maxUses: 5, useCount: 5, grantedRole: 'MEMBER', requireApproval: false, revoked: true },
];

export function InviteTokensScreen({ onBack, serverAddress = '192.168.1.100:3000' }: InviteTokensScreenProps) {
  const [tokens] = useState<InviteToken[]>(sampleTokens);
  const [showDialog, setShowDialog] = useState(false);

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
            <h1 className="text-2xl font-semibold text-foreground">Токены приглашений</h1>
          </div>
        </div>
      </header>

      {/* Content */}
      <main className="max-w-xl mx-auto px-6 py-6 space-y-4">
        {tokens.length === 0 ? (
          <div className="text-center py-16">
            <p className="text-muted-foreground">Нет токенов</p>
            <p className="text-sm text-muted-foreground mt-1">
              Создайте токен приглашения для новых участников
            </p>
          </div>
        ) : (
          tokens.map((token) => (
            <div
              key={token.id}
              className={`bg-card rounded-xl p-4 shadow-sm border ${token.revoked ? 'opacity-50' : ''}`}
            >
              {/* Header */}
              <div className="flex items-center justify-between mb-2">
                <span className="font-medium text-foreground">{token.label}</span>
                {token.revoked && (
                  <span className="text-xs bg-destructive/15 text-destructive px-2 py-0.5 rounded">
                    Отозван
                  </span>
                )}
              </div>

              {/* Code */}
              <div className="flex items-center gap-2 mb-3">
                <code className="text-sm font-mono text-primary flex-1">
                  {serverAddress}/{token.code}
                </code>
                {!token.revoked && (
                  <button className="p-1 hover:bg-secondary rounded transition-colors">
                    <Copy className="size-4 text-muted-foreground" />
                  </button>
                )}
              </div>

              {/* Badges */}
              <div className="flex gap-2 flex-wrap">
                <Badge variant="secondary" className="text-xs">
                  {token.maxUses === 0 ? `${token.useCount} / ∞` : `${token.useCount} / ${token.maxUses}`}
                </Badge>
                <Badge variant="secondary" className="text-xs">
                  {token.grantedRole === 'ADMIN' ? 'Админ' : 'Участник'}
                </Badge>
                <Badge variant="secondary" className="text-xs">
                  {token.requireApproval ? 'С одобрением' : 'Без одобрения'}
                </Badge>
              </div>

              {/* Revoke button */}
              {!token.revoked && (
                <div className="mt-3">
                  <Button variant="outline" size="sm" className="text-destructive border-destructive/30">
                    Отозвать
                  </Button>
                </div>
              )}
            </div>
          ))
        )}
      </main>

      {/* FAB */}
      <button
        onClick={() => setShowDialog(true)}
        className="fixed bottom-6 right-6 size-14 rounded-full bg-primary text-primary-foreground shadow-lg flex items-center justify-center hover:opacity-90 transition-opacity"
      >
        <Plus className="size-6" />
      </button>
    </div>
  );
}
