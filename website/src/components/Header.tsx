import React from 'react';
import { ThemeMode } from '../types';
import { Moon, Sun, Smartphone, Download, Github } from 'lucide-react';

interface HeaderProps {
  themeMode: ThemeMode;
  onThemeChange: (theme: ThemeMode) => void;
  isDarkTheme: boolean;
}

export const Header: React.FC<HeaderProps> = ({
  themeMode,
  onThemeChange,
  isDarkTheme,
}) => {
  return (
    <header className="fixed top-6 sm:top-8 md:top-9 left-0 right-0 z-50 flex justify-center px-4 pointer-events-none">
      <div
        className={`pointer-events-auto h-[60px] max-w-[1120px] w-full rounded-full px-4 md:px-6 flex items-center justify-between transition-colors duration-300 shadow-lg ${
          isDarkTheme ? 'reflex-glass-dark' : 'reflex-glass-light'
        }`}
      >
        {/* Brand Zone (Wordmark & Actual App Icon) */}
        <a
          href="#"
          className="flex items-center gap-2.5 text-inherit no-underline group"
          aria-label="Reflex home"
        >
          <img
            src="./assets/img/reflex-icon.png"
            alt="Reflex logo"
            className="w-8 h-8 rounded-xl shadow-xs transition-transform group-hover:scale-105 object-contain"
          />
          <span className="text-[19px] font-bold tracking-tight font-serif">Reflex</span>
        </a>

        {/* Clean Nav Links (Desktop) */}
        <nav
          aria-label="Main Navigation"
          className="hidden md:flex items-center gap-7 text-[14px] font-medium"
        >
          <a
            href="#features"
            className={`transition-colors hover:text-[#D9A184] ${
              isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'
            }`}
          >
            Features
          </a>
          <a
            href="#quick-add"
            className={`transition-colors hover:text-[#D9A184] ${
              isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'
            }`}
          >
            Quick Add
          </a>
          <a
            href="#privacy"
            className={`transition-colors hover:text-[#D9A184] ${
              isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'
            }`}
          >
            Privacy
          </a>
          <a
            href="#download"
            className={`transition-colors hover:text-[#D9A184] ${
              isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'
            }`}
          >
            Download
          </a>
        </nav>

        {/* Actions Zone: Theme Toggle, GitHub & Download Button */}
        <div className="flex items-center gap-2.5">
          {/* 3-way Theme Pill */}
          <div
            className={`p-1 rounded-full flex items-center border transition-colors ${
              isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#EFEAE4] border-[rgba(26,22,20,0.13)]'
            }`}
            title="Theme: System / Dark / Light"
          >
            <button
              onClick={() => onThemeChange('system')}
              className={`p-1.5 rounded-full transition-colors ${
                themeMode === 'system'
                  ? isDarkTheme ? 'bg-[#D9A184] text-[#0A0908]' : 'bg-[#8F4C2B] text-white'
                  : isDarkTheme ? 'text-[#A39E98] hover:text-[#F5F2EF]' : 'text-[#5D5750] hover:text-[#1A1614]'
              }`}
              aria-label="System theme"
            >
              <Smartphone className="w-3.5 h-3.5" />
            </button>
            <button
              onClick={() => onThemeChange('dark')}
              className={`p-1.5 rounded-full transition-colors ${
                themeMode === 'dark'
                  ? isDarkTheme ? 'bg-[#D9A184] text-[#0A0908]' : 'bg-[#8F4C2B] text-white'
                  : isDarkTheme ? 'text-[#A39E98] hover:text-[#F5F2EF]' : 'text-[#5D5750] hover:text-[#1A1614]'
              }`}
              aria-label="Dark mode"
            >
              <Moon className="w-3.5 h-3.5" />
            </button>
            <button
              onClick={() => onThemeChange('light')}
              className={`p-1.5 rounded-full transition-colors ${
                themeMode === 'light'
                  ? isDarkTheme ? 'bg-[#D9A184] text-[#0A0908]' : 'bg-[#8F4C2B] text-white'
                  : isDarkTheme ? 'text-[#A39E98] hover:text-[#F5F2EF]' : 'text-[#5D5750] hover:text-[#1A1614]'
              }`}
              aria-label="Light mode"
            >
              <Sun className="w-3.5 h-3.5" />
            </button>
          </div>

          {/* GitHub Repository Link */}
          <a
            href="https://github.com/bobby-99/reflex"
            target="_blank"
            rel="noopener noreferrer"
            className={`w-[36px] h-[36px] rounded-full flex items-center justify-center border transition-colors ${
              isDarkTheme
                ? 'bg-[#141211] border-[#2E2A27] text-[#F5F2EF] hover:bg-[#24211E]'
                : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] text-[#1A1614] hover:bg-[#F3EEE8]'
            }`}
            aria-label="View source code on GitHub"
            title="GitHub"
          >
            <Github className="w-4 h-4" />
          </a>

          {/* Primary Download APK Pill */}
          <a
            href="https://github.com/bobby-99/reflex/releases/latest"
            target="_blank"
            rel="noopener noreferrer"
            className={`px-4 py-2 rounded-full text-[13px] font-semibold flex items-center gap-1.5 transition-transform active:scale-95 shadow-sm whitespace-nowrap ${
              isDarkTheme
                ? 'bg-[#D9A184] text-[#0A0908] hover:bg-[#B57E63]'
                : 'bg-[#8F4C2B] text-[#FFFFFF] hover:bg-[#6F3A20]'
            }`}
          >
            <Download className="w-3.5 h-3.5 stroke-[2.5]" />
            <span className="hidden sm:inline">Download</span> APK
          </a>
        </div>
      </div>
    </header>
  );
};
