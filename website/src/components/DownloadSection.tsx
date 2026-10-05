import React from 'react';
import { Download, Github } from 'lucide-react';

interface DownloadSectionProps {
  isDarkTheme: boolean;
}

export const DownloadSection: React.FC<DownloadSectionProps> = ({ isDarkTheme }) => {
  return (
    <section id="download" className="py-14 relative">
      <div className="max-w-[820px] w-full mx-auto px-4 sm:px-6">
        <div
          className={`p-8 sm:p-10 rounded-[32px] border text-center relative overflow-hidden shadow-lg ${
            isDarkTheme
              ? 'bg-[#141211] border-[#2E2A27]'
              : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)]'
          }`}
        >
          {/* Subtle Ambient Radial Wash */}
          <div
            className="absolute inset-0 pointer-events-none opacity-10"
            style={{
              background: isDarkTheme
                ? 'radial-gradient(circle at 50% 30%, #D9A184 0%, transparent 70%)'
                : 'radial-gradient(circle at 50% 30%, #8F4C2B 0%, transparent 70%)',
            }}
          />

          <div className="relative z-10 max-w-[560px] mx-auto">
            <span
              className={`text-[12px] font-bold uppercase tracking-wider block mb-2 ${
                isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'
              }`}
            >
              Direct Android Installation
            </span>
            <h2 className="text-[26px] sm:text-[32px] font-bold leading-tight font-serif mb-3">
              Ready to reclaim your quiet focus?
            </h2>
            <p
              className={`text-[14px] sm:text-[15px] leading-relaxed mb-6 ${
                isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'
              }`}
            >
              Official release APK directly from GitHub. Zero account setup, zero subscriptions, 100% offline.
            </p>

            {/* Actions */}
            <div className="flex flex-col sm:flex-row items-center justify-center gap-3 mb-4">
              <a
                href="https://github.com/bobby-99/reflex/releases/latest"
                target="_blank"
                rel="noopener noreferrer"
                className={`w-full sm:w-auto px-7 py-3.5 rounded-full text-[14px] font-semibold flex items-center justify-center gap-2 transition-transform active:scale-95 shadow-md ${
                  isDarkTheme
                    ? 'bg-[#D9A184] text-[#0A0908] hover:bg-[#B57E63]'
                    : 'bg-[#8F4C2B] text-[#FFFFFF] hover:bg-[#6F3A20]'
                }`}
              >
                <Download className="w-4 h-4 stroke-[2.5]" />
                Download Latest APK
              </a>

              <a
                href="https://github.com/bobby-99/reflex"
                target="_blank"
                rel="noopener noreferrer"
                className={`w-full sm:w-auto px-6 py-3.5 rounded-full text-[14px] font-medium flex items-center justify-center gap-2 border transition-colors ${
                  isDarkTheme
                    ? 'bg-[#1C1A17] border-[#2E2A27] text-[#F5F2EF] hover:bg-[#24211E]'
                    : 'bg-[#F7F3EE] border-[rgba(26,22,20,0.13)] text-[#1A1614] hover:bg-[#EFEAE4]'
                }`}
              >
                <Github className="w-4 h-4" />
                View Source Code
              </a>
            </div>

            <p className={`text-[12px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
              Requires Android 8.0+ (API 26+) · Direct APK installation
            </p>
          </div>
        </div>
      </div>
    </section>
  );
};
