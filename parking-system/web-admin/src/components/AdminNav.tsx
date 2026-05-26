"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const links = [
  { href: "/dashboard", label: "Dashboard" },
  { href: "/turnos", label: "Turnos" },
  { href: "/reportes", label: "Reportes" },
];

export function AdminNav() {
  const pathname = usePathname();

  if (pathname === "/") return null;

  return (
    <nav className="border-b bg-white">
      <div className="mx-auto flex max-w-7xl items-center gap-8 px-6 py-3">
        <Link href="/" className="text-lg font-semibold text-primary">
          Parking Admin
        </Link>
        <div className="flex gap-1">
          {links.map((l) => (
            <Link
              key={l.href}
              href={l.href}
              className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                pathname.startsWith(l.href)
                  ? "bg-primary/10 text-primary"
                  : "text-muted-foreground hover:text-foreground"
              }`}
            >
              {l.label}
            </Link>
          ))}
        </div>
      </div>
    </nav>
  );
}
