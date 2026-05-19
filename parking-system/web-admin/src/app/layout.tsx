import type { Metadata } from "next";
import "./globals.css";
import { Providers } from "@/components/providers";

export const metadata: Metadata = {
  title: "Parking Admin",
  description: "Portal administrativo del sistema de parqueaderos",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="es">
      <body className="min-h-screen bg-muted text-foreground antialiased">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
