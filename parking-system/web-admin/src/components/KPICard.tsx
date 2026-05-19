import { cn } from "@/lib/utils";

interface Props {
  title: string;
  value: string;
  hint?: string;
  className?: string;
}

export function KPICard({ title, value, hint, className }: Props) {
  return (
    <div className={cn("rounded-lg bg-white p-5 shadow-sm", className)}>
      <p className="text-sm text-muted-foreground">{title}</p>
      <p className="mt-1 text-3xl font-semibold text-primary">{value}</p>
      {hint && <p className="mt-1 text-xs text-muted-foreground">{hint}</p>}
    </div>
  );
}
