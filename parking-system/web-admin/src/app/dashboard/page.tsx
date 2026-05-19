"use client";

import { KPICard } from "@/components/KPICard";
import { RevenueChart } from "@/components/charts/RevenueChart";
import { useDashboardData } from "@/hooks/useDashboardData";

export default function DashboardPage() {
  const { data, isLoading, error } = useDashboardData();

  if (isLoading) return <Loading />;
  if (error || !data) return <ErrorView message={String(error)} />;

  return (
    <main className="mx-auto max-w-7xl px-6 py-10">
      <h1 className="text-3xl font-semibold text-primary">Dashboard ejecutivo</h1>
      <p className="text-muted-foreground">Vista general de operación</p>

      <section className="mt-8 grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
        <KPICard title="Ingresos hoy" value={fmtCop(data.todayRevenueCents)} hint="vs ayer" />
        <KPICard title="Sesiones" value={String(data.sessionsToday)} hint="cierres del día" />
        <KPICard title="Ocupación" value={`${data.occupancyPercent}%`} hint="promedio actual" />
        <KPICard title="Mensualidades" value={String(data.monthlyActive)} hint="vigentes" />
      </section>

      <section className="mt-10 rounded-lg bg-white p-6 shadow-sm">
        <h2 className="text-xl font-medium">Ingresos por día</h2>
        <RevenueChart series={data.revenueByDay} />
      </section>
    </main>
  );
}

function Loading() {
  return <main className="p-10">Cargando dashboard…</main>;
}

function ErrorView({ message }: { message: string }) {
  return (
    <main className="p-10 text-red-600">
      No se pudo cargar el dashboard: {message}
    </main>
  );
}

function fmtCop(cents: number): string {
  const pesos = Math.round(cents / 100);
  return new Intl.NumberFormat("es-CO", { style: "currency", currency: "COP", maximumFractionDigits: 0 }).format(pesos);
}
