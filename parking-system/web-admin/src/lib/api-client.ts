/**
 * Cliente HTTP minimalista del portal admin.
 *
 * En desarrollo usa `/api/proxy/*` (rewrite a `next.config.mjs`).
 * En producción, NEXT_PUBLIC_API_URL apunta directamente al backend.
 */
const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "/api/proxy";

class ApiClient {
  private token: string | null = null;

  setToken(token: string | null) {
    this.token = token;
  }

  async get<T = any>(path: string): Promise<T> {
    return this.request<T>("GET", path);
  }
  async post<T = any>(path: string, body?: unknown): Promise<T> {
    return this.request<T>("POST", path, body);
  }

  private async request<T>(method: string, path: string, body?: unknown): Promise<T> {
    const headers: Record<string, string> = { "Content-Type": "application/json" };
    if (this.token) headers.Authorization = `Bearer ${this.token}`;
    const res = await fetch(`${BASE_URL}${path}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });
    if (!res.ok) {
      throw new Error(`HTTP ${res.status} ${res.statusText} on ${path}`);
    }
    return res.json() as Promise<T>;
  }
}

export const apiClient = new ApiClient();
