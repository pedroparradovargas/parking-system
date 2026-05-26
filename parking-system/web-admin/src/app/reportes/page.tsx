"use client";

import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/lib/api-client";
import { useState } from "react";

interface CashClosingRow {
  operatorUserId: string;
  operatorName: string;
  dayIso: string;
  sessionsCount: number;
  totalCents: number;
  ivaCents: number;
}

interface CashClosingReport {
  parkingId: string;
  fromIso: string;
  toIso: string;
  rows: CashClosingRow[];
  grandTotalCents: number;
  grandIvaCents: number;
}

interface TopPlateRow {
  plate: string;
  sessionsCount: number;
  totalCents: number;
  totalMinutes: number;
}

interface TopPlatesReport {
  parkingId: string;
  fromIso: string;
  toIso: string;
  rows: TopPlateRow[];
}

const PARKING_ID = process.env.NEXT_PUBLIC_PARKING_ID ?? "default";

export default function ReportesPage() {
  const today = new Date().toISOString().slice(0, 10);
  const thirtyDaysAgo = new Date(Date.now() - 30 * 86400000).toISOString().slice(0, 10);

  const [from, setFrom] = useState(thirtyDaysAgo);
  const [to, setTo] = useState(today);

  const { data: cashClosing, isLoading: loadingCash } = useQuery({
    queryKey: ["cash-closing", from, to],
    queryFn: () =>
      apiClient.get<CashClosingReport>(
        `/v1/parkings/${PARKING_ID}/reports/cash-closing?from=${from}&to=${to}`
      ),
  });

  const { data: topPlates, isLoading: loadingPlates } = useQuery({
    queryKey: ["top-plates", from, to],
    queryFn: () =>
      apiClient.get<TopPlatesReport>(
        `/v1/parkings/${PARKING_ID}/reports/top-plates?from=${from}&to=${to}&limit=10`
      ),
  });

  async function downloadExcel(report: "cash-closing" | "top-plates") {
    await apiClient.downloadBlob(
      `/v1/parkings/${PARKING_ID}/reports/${report}.xlsx?from=${from}&to=${to}`,
      `${report}-${from}-${to}.xlsx`
    );
  }

  return (
    <main className="mx-auto max-w-6xl px-6 py-10">
      <h1 className="text-3xl font-semibold text-primary">Reportes</h1>
      <p className="text-muted-foreground">Informes operativos con exportacion Excel</p>

      <section className="mt-6 flex flex-wrap items-end gap-4">
        <div>
          <label className="block text-sm text-muted-foreground">Desde</label>
          <input
            type="date"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
            className="rounded border px-3 py-2 text-sm"
          />
        </div>
        <div>
          <label className="block text-sm text-muted-foreground">Hasta</label>
          <input
            type="date"
            value={to}
            onChange={(e) => setTo(e.target.value)}
            className="rounded border px-3 py-2 text-sm"
          />
        </div>
      </section>

      {/* Cierre de caja */}
      <section className="mt-8 rounded-lg bg-white p-6 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-medium">Cierre de caja</h2>
          <button
            onClick={() => downloadExcel("cash-closing")}
            className="rounded bg-green-600 px-3 py-1.5 text-sm text-white hover:bg-green-700"
          >
            Descargar Excel
          </button>
        </div>

        {loadingCash ? (
          <p className="text-sm text-muted-foreground">Cargando...</p>
        ) : !cashClosing || cashClosing.rows.length === 0 ? (
          <p className="text-sm text-muted-foreground">Sin datos en el rango seleccionado.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left text-muted-foreground">
                  <th className="pb-2">Fecha</th>
                  <th className="pb-2">Operador</th>
                  <th className="pb-2 text-right">Sesiones</th>
                  <th className="pb-2 text-right">Total</th>
                  <th className="pb-2 text-right">IVA</th>
                </tr>
              </thead>
              <tbody>
                {cashClosing.rows.map((r, i) => (
                  <tr key={i} className="border-b last:border-0">
                    <td className="py-2">{r.dayIso}</td>
                    <td className="py-2">{r.operatorName}</td>
                    <td className="py-2 text-right">{r.sessionsCount}</td>
                    <td className="py-2 text-right">{fmtCop(r.totalCents)}</td>
                    <td className="py-2 text-right">{fmtCop(r.ivaCents)}</td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr className="font-semibold">
                  <td className="pt-2">TOTAL</td>
                  <td></td>
                  <td className="pt-2 text-right">
                    {cashClosing.rows.reduce((a, r) => a + r.sessionsCount, 0)}
                  </td>
                  <td className="pt-2 text-right">{fmtCop(cashClosing.grandTotalCents)}</td>
                  <td className="pt-2 text-right">{fmtCop(cashClosing.grandIvaCents)}</td>
                </tr>
              </tfoot>
            </table>
          </div>
        )}
      </section>

      {/* Top placas */}
      <section className="mt-8 rounded-lg bg-white p-6 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-medium">Top placas por recaudo</h2>
          <button
            onClick={() => downloadExcel("top-plates")}
            className="rounded bg-green-600 px-3 py-1.5 text-sm text-white hover:bg-green-700"
          >
            Descargar Excel
          </button>
        </div>

        {loadingPlates ? (
          <p className="text-sm text-muted-foreground">Cargando...</p>
        ) : !topPlates || topPlates.rows.length === 0 ? (
          <p className="text-sm text-muted-foreground">Sin datos en el rango seleccionado.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left text-muted-foreground">
                  <th className="pb-2">#</th>
                  <th className="pb-2">Placa</th>
                  <th className="pb-2 text-right">Sesiones</th>
                  <th className="pb-2 text-right">Total</th>
                  <th className="pb-2 text-right">Horas</th>
                </tr>
              </thead>
              <tbody>
                {topPlates.rows.map((r, i) => (
                  <tr key={r.plate} className="border-b last:border-0">
                    <td className="py-2 text-muted-foreground">{i + 1}</td>
                    <td className="py-2 font-mono">{r.plate}</td>
                    <td className="py-2 text-right">{r.sessionsCount}</td>
                    <td className="py-2 text-right">{fmtCop(r.totalCents)}</td>
                    <td className="py-2 text-right">{(r.totalMinutes / 60).toFixed(1)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </main>
  );
}

function fmtCop(cents: number): string {
  const pesos = Math.round(cents / 100);
  return new Intl.NumberFormat("es-CO", {
    style: "currency",
    currency: "COP",
    maximumFractionDigits: 0,
  }).format(pesos);
}
