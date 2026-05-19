import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * Helper estándar shadcn — combina clases Tailwind condicionalmente
 * y resuelve conflictos (ej.: `p-4 p-2` → `p-2`).
 */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}
