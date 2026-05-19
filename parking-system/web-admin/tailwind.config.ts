import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        // Misma paleta corporativa que la app Compose.
        primary: { DEFAULT: "#0F3D5C", foreground: "#FFFFFF" },
        accent: { DEFAULT: "#FFA000", foreground: "#000000" },
        muted: { DEFAULT: "#F5F7FA", foreground: "#6B7280" },
      },
      fontFamily: {
        sans: ['"Inter"', "system-ui", "sans-serif"],
      },
    },
  },
  plugins: [],
};
export default config;
