import { ChevronRight } from 'lucide-react';

interface ServerCardProps {
  id: string;
  name: string;
  username: string;
  image: string;
  onClick: () => void;
}

export function ServerCard({ name, username, image, onClick }: ServerCardProps) {
  return (
    <button
      onClick={onClick}
      className="w-full flex items-center gap-4 p-4 hover:bg-secondary/50 transition-colors rounded-xl group"
    >
      <div className="size-16 rounded-xl overflow-hidden flex-shrink-0 bg-muted">
        <img 
          src={image} 
          alt={name}
          className="size-full object-cover"
        />
      </div>
      
      <div className="flex-1 text-left">
        <h3 className="font-semibold text-card-foreground">{name}</h3>
        <p className="text-sm text-muted-foreground">{username}</p>
      </div>

      <ChevronRight className="size-5 text-muted-foreground group-hover:text-foreground transition-colors" />
    </button>
  );
}