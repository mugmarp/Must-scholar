import React, { createContext, useContext, useEffect, useState } from "react";

export const ACCENTS = {
  indigo: { name: "Indigo", hsl: "243 75% 59%", swatch: "#4F46E5" },
  purple: { name: "Purple", hsl: "263 70% 50%", swatch: "#6D28D9" },
  green: { name: "Green", hsl: "160 84% 39%", swatch: "#059669" },
  orange: { name: "Orange", hsl: "24 95% 53%", swatch: "#F97316" },
};

const ThemeContext = createContext(null);

export function ThemeProvider({ children }) {
  const [dark, setDark] = useState(() => localStorage.getItem("must_dark") === "1");
  const [accent, setAccent] = useState(() => localStorage.getItem("must_accent") || "indigo");

  useEffect(() => {
    document.documentElement.classList.toggle("dark", dark);
    localStorage.setItem("must_dark", dark ? "1" : "0");
  }, [dark]);

  useEffect(() => {
    const a = ACCENTS[accent] || ACCENTS.indigo;
    const root = document.documentElement;
    root.style.setProperty("--primary", a.hsl);
    root.style.setProperty("--primary-foreground", "0 0% 100%");
    root.style.setProperty("--ring", a.hsl);
    localStorage.setItem("must_accent", accent);
  }, [accent]);

  return (
    <ThemeContext.Provider value={{ dark, setDark, accent, setAccent }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error("useTheme must be used within ThemeProvider");
  return ctx;
}