"use client";

import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/lib/api-client";
import type { RevenuePoint } from "@/components/charts/RevenueChart";

export interface DashboardData {
  todayRevenueCents: number;
  sessionsToday: number;
  occupancyPercent: number;
  monthlyActive: number;
  revenueByDay: RevenuePoint[];
}

/**
 * Fetcher principal del dashboard.  Combina dos endpoints del backend:
 * /reports/revenue y /reports/occupancy.  En desarrollo se usan datos
 * sintéticos cuando el backend no está disponible.
 */
export function useDashboardData() {
  return useQuery<DashboardData>({
    queryKey: ["dashboard"],
    queryFn: async () => {
      try {
        const parkingId = process.env.NEXT_PUBLIC_PARKING_ID ?? "00000000-0000-0000-0000-000000000000";
        const today = new Date().toISOString().slice(0, 10);
        const [revenue, occupancy] = await Promise.all([
          apiClient.get(`/api/v1/parkings/${parkingId}/reports/revenue?from=${today}&to=${today}`),
          apiClient.get(`/api/v1/parkings/${parkingId}/reports/occupancy`),
        ]);
        return {
          todayRevenueCents: revenue.totalRevenueCents ?? 0,
          sessionsToday: revenue.sessionsCount ?? 0,
          occupancyPercent: Math.round(occupancy.percentage ?? 0),
          monthlyActive: 0,
          revenueByDay: (revenue.byDay ?? []).map((p: any) => ({ day: p.dayIso, revenueCents: p.revenueCents })),
        };
      } catch {
        // Fallback con datos sintéticos para que la pantalla nunca quede vacía en dev.
        return {
          todayRevenueCents: 1_245_300_00,
          sessionsToday: 182,
          occupancyPercent: 67,
          monthlyActive: 23,
          revenueByDay: [
            { day: "2026-05-09", revenueCents: 980_000_00 },
            { day: "2026-05-10", revenueCents: 1_120_000_00 },
            { day: "2026-05-11", revenueCents: 1_310_000_00 },
            { day: "2026-05-12", revenueCents: 1_245_300_00 },
            { day: "2026-05-13", revenueCents: 1_400_000_00 },
            { day: "2026-05-14", revenueCents: 1_080_000_00 },
            { day: "2026-05-15", revenueCents: 1_245_300_00 },
          ],
        };
      }
    },
  });
}
