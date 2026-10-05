import React from 'react';
import { Github, FileCode, Bug, Heart, ArrowUp } from 'lucide-react';

interface FooterProps {
  isDarkTheme: boolean;
}

export const Footer: React.FC<FooterProps> = ({ isDarkTheme }) => {
  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <footer
      className={`border-t py-16 transition-colors ${
        isDarkTheme ? 'bg-[#0A0908] border-[#2E2A27] text-[#F5F2EF]' : 'bg-[#F7F3EE] border-[rgba(26,22,20,0.13)] text-[#1A1614]'
      }`}
    >
      <div className="max-w-[1120px] w-full mx-auto px-4 sm:px-6">
        {/* Calm Closing / Support Section */}
        <div className="text-center pb-14 mb-12 border-b border-neutral-700/15">
          <p className={`text-[16px] sm:text-[18px] font-medium mb-6 ${isDarkTheme ? 'text-[#F5F2EF]' : 'text-[#1A1614]'}`}>
            Reflex is free, open source and built by one person.
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3 sm:gap-4 max-w-md mx-auto">
            {/* Star on GitHub (Primary) */}
            <a
              href="https://github.com/bobby-99/reflex"
              target="_blank"
              rel="noopener noreferrer"
              className={`w-full sm:w-auto inline-flex items-center justify-center gap-2.5 px-6 py-3 rounded-full text-[14px] font-semibold transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-[#D9A184]/50 ${
                isDarkTheme
                  ? 'bg-[#D9A184] text-[#0A0908] hover:bg-[#E3B298] shadow-sm'
                  : 'bg-[#8F4C2B] text-white hover:bg-[#783F23] shadow-sm'
              }`}
            >
              <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
              </svg>
              <span>Star on GitHub</span>
            </a>

            {/* Support on Ko-fi (Secondary) */}
            <a
              href="https://ko-fi.com/P5X2288QK6"
              target="_blank"
              rel="noopener noreferrer"
              className={`w-full sm:w-auto inline-flex items-center justify-center gap-2.5 px-6 py-3 rounded-full text-[14px] font-medium transition-all duration-200 border focus:outline-none focus:ring-2 focus:ring-[#D9A184]/50 ${
                isDarkTheme
                  ? 'bg-[#141211] border-[#2E2A27] text-[#F5F2EF] hover:bg-[#1C1A17] hover:border-[#D9A184]/40'
                  : 'bg-white border-[rgba(26,22,20,0.13)] text-[#1A1614] hover:bg-[#F2ECE4] hover:border-[#8F4C2B]/40'
              }`}
            >
              <svg className="w-4 h-4 text-[#D9A184]" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M18 8h1a4 4 0 0 1 0 8h-1M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8zM6 1v3M10 1v3M14 1v3" />
              </svg>
              <span>Support on Ko-fi</span>
            </a>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-12 gap-10 pb-12 border-b border-neutral-700/15">
          {/* Column 1: Brand & Philosophy */}
          <div className="md:col-span-5">
            <div className="flex items-center gap-2.5 mb-3">
              <img
                src="./assets/img/reflex-icon.png"
                alt="Reflex logo"
                className="w-7 h-7 rounded-lg"
              />
              <span className="text-[20px] font-bold tracking-tight">Reflex</span>
            </div>
            <p className={`text-[14px] leading-relaxed max-w-[360px] mb-4 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
              Free software routine runner, atomic habit tracker, natural-language task manager, and deep focus companion for Android. 100% offline.
            </p>
            <p className={`text-[12px] ${isDarkTheme ? 'text-[#6E6963]' : 'text-[#948B83]'}`}>
              Released under GNU General Public License v3.0 or later (GPL-3.0-or-later).
            </p>
          </div>

          {/* Column 2: Contributing & Rules */}
          <div className="md:col-span-4">
            <h4 className="text-[14px] font-bold uppercase tracking-wider mb-3">
              Contributing Principles
            </h4>
            <ul className={`space-y-2 text-[13px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
              <li>• Strictly 100% offline: zero network permissions permitted.</li>
              <li>• Follow the visual guidelines defined in DESIGN.md.</li>
              <li>• Clean Room migrations and zero regression tests.</li>
            </ul>
          </div>

          {/* Column 3: Links */}
          <div className="md:col-span-3 flex flex-col items-start md:items-end">
            <h4 className="text-[14px] font-bold uppercase tracking-wider mb-3">
              Repository & Code
            </h4>
            <div className={`space-y-2.5 text-[14px] flex flex-col items-start md:items-end ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
              <a
                href="https://github.com/bobby-99/reflex"
                target="_blank"
                rel="noopener noreferrer"
                className="hover:text-[#D9A184] transition-colors flex items-center gap-1.5"
              >
                <Github className="w-4 h-4" />
                GitHub Repository
              </a>
              <a
                href="https://github.com/bobby-99/reflex/blob/main/DESIGN.md"
                target="_blank"
                rel="noopener noreferrer"
                className="hover:text-[#D9A184] transition-colors flex items-center gap-1.5"
              >
                <FileCode className="w-4 h-4" />
                DESIGN.md Specification
              </a>
              <a
                href="https://github.com/bobby-99/reflex/issues"
                target="_blank"
                rel="noopener noreferrer"
                className="hover:text-[#D9A184] transition-colors flex items-center gap-1.5"
              >
                <Bug className="w-4 h-4" />
                Report an Issue
              </a>
              <a
                href="https://github.com/bobby-99/reflex/blob/main/LICENSE"
                target="_blank"
                rel="noopener noreferrer"
                className="hover:text-[#D9A184] transition-colors"
              >
                GPL-3.0 License
              </a>
            </div>
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="pt-8 flex flex-col sm:flex-row items-center justify-between gap-4 text-[13px]">
          <p className={isDarkTheme ? 'text-[#6E6963]' : 'text-[#948B83]'}>
            Reflex — Built to help you start, focus, and finish, without handing over your data.
          </p>

          <button
            onClick={scrollToTop}
            className={`flex items-center gap-1.5 py-1.5 px-3 rounded-full text-[12px] font-medium transition-colors ${
              isDarkTheme ? 'bg-[#141211] hover:bg-[#1C1A17] text-[#A39E98]' : 'bg-[#EFEAE4] hover:bg-[#E2DDD5] text-[#5D5750]'
            }`}
          >
            <span>Back to top</span>
            <ArrowUp className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>
    </footer>
  );
};
