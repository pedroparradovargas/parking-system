import Link from "next/link";

export default function HomePage() {
  return (
    <main className="mx-auto flex min-h-screen max-w-3xl flex-col items-center justify-center px-6 py-16">
      <h1 className="text-4xl font-semibold text-primary">Parking Admin</h1>
      <p className="mt-3 text-muted-foreground">
        Portal administrativo. Configura tarifas, gestiona usuarios, revisa
        reportes y monitorea ocupación en tiempo real.
      </p>
      <Link
        href="/dashboard"
        className="mt-8 rounded-md bg-primary px-6 py-3 text-primary-foreground hover:bg-primary/90"
      >
        Entrar al dashboard
      </Link>
    </main>
  );
}
