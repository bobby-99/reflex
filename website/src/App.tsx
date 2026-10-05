import React, { useState, useEffect } from 'react';
import { ThemeMode, ActiveTab } from './types';
import { Header } from './components/Header';
import { HeroAndStory } from './components/HeroAndStory';
import { QuickAddPlayground } from './components/QuickAddPlayground';
import { PrivacyProof } from './components/PrivacyProof';
import { DownloadSection } from './components/DownloadSection';
import { Footer } from './components/Footer';

export default function App() {
  // Theme Management (System / Dark / Light) with safe localStorage persistence
  const [themeMode, setThemeMode] = useState<ThemeMode>(() => {
    try {
      const stored = localStorage.getItem('reflex_theme_mode');
      if (stored === 'dark' || stored === 'light' || stored === 'system') {
        return stored;
      }
    } catch {
      // safe fallback for restricted sandbox
    }
    return 'system';
  });

  const [systemIsDark, setSystemIsDark] = useState<boolean>(() => {
    if (typeof window !== 'undefined' && window.matchMedia) {
      return window.matchMedia('(prefers-color-scheme: dark)').matches;
    }
    return true; // default to dark
  });

  // Active phone screen (default to 'routines' as confirmed in Phase 1)
  const [activePhoneTab, setActivePhoneTab] = useState<ActiveTab>('routines');

  // Trigger state for interactive suggested taps from hero or quick action buttons
  const [presetQuickAddText, setPresetQuickAddText] = useState<string | null>(null);
  const [autoStartRoutineId, setAutoStartRoutineId] = useState<string | null>(null);
  const [autoStartFocus, setAutoStartFocus] = useState<boolean>(false);

  // Listen to OS theme changes
  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return;
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    const handleChange = (e: MediaQueryListEvent) => {
      setSystemIsDark(e.matches);
    };
    mediaQuery.addEventListener('change', handleChange);
    return () => mediaQuery.removeEventListener('change', handleChange);
  }, []);

  const handleThemeChange = (newTheme: ThemeMode) => {
    setThemeMode(newTheme);
    try {
      localStorage.setItem('reflex_theme_mode', newTheme);
    } catch {
      // safe fallback
    }
  };

  // Determine effective theme boolean
  const isDark = themeMode === 'system' ? systemIsDark : themeMode === 'dark';

  // Apply theme to document root
  useEffect(() => {
    const root = document.documentElement;
    if (isDark) {
      root.classList.add('dark');
      root.classList.remove('light');
      root.style.backgroundColor = '#0A0908';
      root.style.color = '#F5F2EF';
    } else {
      root.classList.add('light');
      root.classList.remove('dark');
      root.style.backgroundColor = '#F7F3EE';
      root.style.color = '#1A1614';
    }
  }, [isDark]);

  // Handler for quick actions
  const handleTriggerQuickAdd = (text: string) => {
    setActivePhoneTab('tasks');
    setPresetQuickAddText(text);
  };

  const handleTriggerRoutine = (id: string) => {
    setActivePhoneTab('routines');
    setAutoStartRoutineId(id);
  };

  const handleTriggerFocus = () => {
    setActivePhoneTab('focus');
    setAutoStartFocus(true);
  };

  return (
    <div
      id="main-content"
      className={`min-h-screen relative transition-colors duration-300 ${
        isDark ? 'bg-[#0A0908] text-[#F5F2EF]' : 'bg-[#F7F3EE] text-[#1A1614]'
      }`}
    >
      {/* Ambient CSS-Only Copper Blob Backdrops (Contained within fixed inset-0) */}
      <div className="fixed inset-0 pointer-events-none -z-10 overflow-hidden">
        {/* Blob A */}
        <div
          className="absolute -top-[10%] -left-[10%] w-[600px] h-[600px] rounded-full blur-[140px] ambient-blob-a"
          style={{
            background: isDark
              ? 'radial-gradient(circle, rgba(217, 161, 132, 0.16) 0%, rgba(0, 0, 0, 0) 70%)'
              : 'radial-gradient(circle, rgba(143, 76, 43, 0.14) 0%, rgba(255, 255, 255, 0) 70%)',
          }}
        />
        {/* Blob B */}
        <div
          className="absolute top-[40%] -right-[15%] w-[700px] h-[700px] rounded-full blur-[160px] ambient-blob-b"
          style={{
            background: isDark
              ? 'radial-gradient(circle, rgba(181, 126, 99, 0.13) 0%, rgba(0, 0, 0, 0) 70%)'
              : 'radial-gradient(circle, rgba(143, 76, 43, 0.12) 0%, rgba(255, 255, 255, 0) 70%)',
          }}
        />
        {/* Blob C */}
        <div
          className="absolute bottom-[5%] left-[20%] w-[500px] h-[500px] rounded-full blur-[130px] ambient-blob-a"
          style={{
            background: isDark
              ? 'radial-gradient(circle, rgba(217, 161, 132, 0.10) 0%, rgba(0, 0, 0, 0) 70%)'
              : 'radial-gradient(circle, rgba(143, 76, 43, 0.08) 0%, rgba(255, 255, 255, 0) 70%)',
          }}
        />
      </div>

      {/* Floating Glass Pill Navigation Bar */}
      <Header
        themeMode={themeMode}
        onThemeChange={handleThemeChange}
        isDarkTheme={isDark}
      />

      <main>
        {/* Unified Hero Narrative & Feature Story with Single Pinned Sticky Phone */}
        <HeroAndStory
          activeTab={activePhoneTab}
          onSelectTab={setActivePhoneTab}
          themeMode={themeMode}
          onThemeChange={handleThemeChange}
          isDarkTheme={isDark}
          onTriggerQuickAddPreset={handleTriggerQuickAdd}
          onTriggerRoutineStart={handleTriggerRoutine}
          onTriggerFocusStart={handleTriggerFocus}
          presetQuickAddText={presetQuickAddText}
          onClearPresetQuickAdd={() => setPresetQuickAddText(null)}
          autoStartRoutineId={autoStartRoutineId}
          autoStartFocus={autoStartFocus}
        />

        {/* Quick Add NLP Playground */}
        <QuickAddPlayground isDarkTheme={isDark} />

        {/* Privacy Proof & Zero Network Architecture */}
        <PrivacyProof isDarkTheme={isDark} />

        {/* Direct APK Download Section */}
        <DownloadSection isDarkTheme={isDark} />
      </main>

      {/* Quiet Footer */}
      <Footer isDarkTheme={isDark} />
    </div>
  );
}
