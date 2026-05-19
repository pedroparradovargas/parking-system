/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  experimental: {
    typedRoutes: true,
  },
  // Reescrituras para proxy al backend Ktor en desarrollo (evita CORS).
  async rewrites() {
    return [
      {
        source: "/api/proxy/:path*",
        destination: `${process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"}/:path*`,
      },
    ];
  },
};

export default nextConfig;
