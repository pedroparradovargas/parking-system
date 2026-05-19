"use client";

import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

export interface RevenuePoint {
  day: string;
  revenueCents: number;
}

export function RevenueChart({ series }: { series: RevenuePoint[] }) {
  const data = series.map((p) => ({ day: p.day, revenue: Math.round(p.revenueCents / 100) }));
  return (
    <div className="h-80">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data} margin={{ top: 24, right: 24, left: 0, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
          <XAxis dataKey="day" />
          <YAxis tickFormatter={(v) => `$${v.toLocaleString("es-CO")}`} />
          <Tooltip formatter={(v: number) => [`$${v.toLocaleString("es-CO")}`, "Ingreso"]} />
          <Line type="monotone" dataKey="revenue" stroke="#0F3D5C" strokeWidth={2} dot={false} />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
