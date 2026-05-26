"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/lib/api-client";
import { useState } from "react";

interface CashSession {
  id: string;
  parkingId: string;
  operatorUserId: string;
  operatorName?: string;
  openedAtMillis: number;
  closedAtMillis?: number;
  totalCashCents: number;
  totalCardCents: number;
  totalOtherCents: number;
  sessionsCount: number;
  notes?: string;
}

const PARKING_ID = process.env.NEXT_PUBLIC_PARKING_ID ?? "default";

export default function TurnosPage() {
  const qc = useQueryClient();
  const [notes, setNotes] = useState("");

  const { data: current, isLoading: loadingCurrent } = useQuery({
    queryKey: ["cash-session-current"],
    queryFn: () =>
      apiClient.get<CashSession | null>(
        `/v1/parkings/${PARKING_ID}/cash-sessions/current`
      ).catch(() => null),
  });

  const { data: history = [], isLoading: loadingHistory } = useQuery({
    queryKey: ["cash-sessions"],
    queryFn: () =>
      apiClient.get<CashSession[]>(
        `/v1/parkings/${PARKING_ID}/cash-sessions?limit=20`
      ),
  });

  const openMut = useMutation({
    mutationFn: () =>
      apiClient.post<CashSession>(
        `/v1/parkings/${PARKING_ID}/cash-sessions/open`,
        { notes: notes || undefined }
      ),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["cash-session-current"] });
      qc.invalidateQueries({ queryKey: ["cash-sessions"] });
      setNotes("");
    },
  });

  const closeMut = useMutation({
    mutationFn: (id: string) =>
      apiClient.post<CashSession>(
        `/v1/parkings/${PARKING_ID}/cash-sessions/${id}/close`,
        { notes: notes || undefined }
      ),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["cash-session-current"] });
      qc.invalidateQueries({ queryKey: ["cash-sessions"] });
      setNotes("");
    },
  });

  if (loadingCurrent || loadingHistory) {
    return <main className="p-10">Cargando turnos...</main>;
  }

  return (
    <main className="mx-auto max-w-5xl px-6 py-10">
      <h1 className="text-3xl font-semibold text-primary">Turnos de Caja</h1>
      <p className="text-muted-foreground">Gestiona la apertura y cierre de turnos</p>

      <section className="mt-8 rounded-lg bg-white p-6 shadow-sm">
        <h2 className="text-xl font-medium mb-4">Turno actual</h2>

        {current ? (
          <div className="space-y-3">
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <span className="text-muted-foreground">Operador: </span>
                <strong>{current.operatorName || current.operatorUserId}</strong>
              </div>
              <div>
                <span className="text-muted-foreground">Abierto: </span>
                <strong>{new Date(current.openedAtMillis).toLocaleString("es-CO")}</strong>
              </div>
            </div>
            <div>
              <label className="block text-sm text-muted-foreground mb-1">Notas de cierre</label>
              <input
                type="text"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Observaciones del turno..."
                className="w-full rounded border px-3 py-2 text-sm"
              />
            </div>
            <button
              onClick={() => closeMut.mutate(current.id)}
              disabled={closeMut.isPending}
              className="rounded bg-red-600 px-4 py-2 text-sm text-white hover:bg-red-700 disabled:opacity-50"
            >
              {closeMut.isPending ? "Cerrando..." : "Cerrar turno"}
            </button>
            {closeMut.isError && (
              <p className="text-sm text-red-600">{String(closeMut.error)}</p>
            )}
          </div>
        ) : (
          <div className="space-y-3">
            <p className="text-sm text-muted-foreground">No hay turno abierto.</p>
            <div>
              <label className="block text-sm text-muted-foreground mb-1">Notas de apertura</label>
              <input
                type="text"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Observaciones..."
                className="w-full rounded border px-3 py-2 text-sm"
              />
            </div>
            <button
              onClick={() => openMut.mutate()}
              disabled={openMut.isPending}
              className="rounded bg-primary px-4 py-2 text-sm text-white hover:opacity-90 disabled:opacity-50"
            >
              {openMut.isPending ? "Abriendo..." : "Abrir turno"}
            </button>
            {openMut.isError && (
              <p className="text-sm text-red-600">{String(openMut.error)}</p>
            )}
          </div>
        )}
      </section>

      <section className="mt-8 rounded-lg bg-white p-6 shadow-sm">
        <h2 className="text-xl font-medium mb-4">Historial de turnos</h2>
        {history.length === 0 ? (
          <p className="text-sm text-muted-foreground">Sin turnos registrados.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left text-muted-foreground">
                  <th className="pb-2">Operador</th>
                  <th className="pb-2">Apertura</th>
                  <th className="pb-2">Cierre</th>
                  <th className="pb-2 text-right">Sesiones</th>
                  <th className="pb-2 text-right">Total</th>
                  <th className="pb-2">Estado</th>
                </tr>
              </thead>
              <tbody>
                {history.map((s) => (
                  <tr key={s.id} className="border-b last:border-0">
                    <td className="py-2">{s.operatorName || s.operatorUserId.slice(0, 8)}</td>
                    <td className="py-2">{new Date(s.openedAtMillis).toLocaleString("es-CO")}</td>
                    <td className="py-2">
                      {s.closedAtMillis
                        ? new Date(s.closedAtMillis).toLocaleString("es-CO")
                        : "-"}
                    </td>
                    <td className="py-2 text-right">{s.sessionsCount}</td>
                    <td className="py-2 text-right">{fmtCop(s.totalCashCents + s.totalCardCents + s.totalOtherCents)}</td>
                    <td className="py-2">
                      <span
                        className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${
                          s.closedAtMillis
                            ? "bg-gray-100 text-gray-600"
                            : "bg-green-100 text-green-700"
                        }`}
                      >
                        {s.closedAtMillis ? "Cerrado" : "Abierto"}
                      </span>
                    </td>
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
