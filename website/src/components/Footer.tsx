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
        <div className="grid grid-cols-1 md:grid-cols-12 gap-10 pb-12 border-b border-neutral-700/15">
          {/* Column 1: Brand & Philosophy */}
          <div className="md:col-span-5">
            <div className="flex items-center gap-2.5 mb-3">
              <img
                src="./assets/img/reflex-icon.svg"
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
