import React from 'react';
import { ShieldCheck, Lock, HardDrive, ExternalLink } from 'lucide-react';

interface PrivacyProofProps {
  isDarkTheme: boolean;
}

export const PrivacyProof: React.FC<PrivacyProofProps> = ({ isDarkTheme }) => {
  return (
    <section id="privacy" className="py-20 relative">
      <div className="max-w-[1120px] w-full mx-auto px-4 sm:px-6">
        {/* Section Header */}
        <div className="text-center max-w-[640px] mx-auto mb-12">
          <span
            className={`text-[13px] font-bold uppercase tracking-wider block mb-2 ${
              isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'
            }`}
          >
            Zero Trust Architecture
          </span>
          <h2 className="text-[32px] sm:text-[42px] font-bold leading-tight tracking-tight mb-3 font-serif">
            Your data never leaves your phone.
          </h2>
          <p className={`text-[16px] sm:text-[18px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
            Most apps ask you to trust a privacy policy. Reflex makes tracking architecturally impossible.
          </p>
        </div>

        {/* 3 Short, Clean, Impactful Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          {/* Card 1: Zero Network Permission */}
          <div
            className={`p-7 rounded-[32px] border transition-transform hover:-translate-y-1 ${
              isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-xs'
            }`}
          >
            <div className="w-12 h-12 rounded-2xl flex items-center justify-center mb-5 bg-emerald-500/10 text-emerald-500">
              <ShieldCheck className="w-6 h-6" />
            </div>
            <h3 className="text-[19px] font-bold font-serif mb-2">No Internet Permission</h3>
            <p className={`text-[14px] leading-relaxed ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
              Reflex does not declare <code className="font-mono text-[12px] px-1 py-0.5 rounded bg-neutral-800/20">INTERNET</code> in AndroidManifest.xml. The Android OS kernel blocks all network socket requests.
            </p>
          </div>

          {/* Card 2: Zero Telemetry & Cloud */}
          <div
            className={`p-7 rounded-[32px] border transition-transform hover:-translate-y-1 ${
              isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-xs'
            }`}
          >
            <div className={`w-12 h-12 rounded-2xl flex items-center justify-center mb-5 ${
              isDarkTheme ? 'bg-[#2A1C16] text-[#D9A184]' : 'bg-[#F7EBE3] text-[#8F4C2B]'
            }`}>
              <Lock className="w-6 h-6" />
            </div>
            <h3 className="text-[19px] font-bold font-serif mb-2">No Accounts or Tracking</h3>
            <p className={`text-[14px] leading-relaxed ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
              Zero sign-ups, zero cloud accounts, and zero third-party analytics SDKs. Your habits, routines, and focus history belong strictly to you.
            </p>
          </div>

          {/* Card 3: 100% Local SQLite */}
          <div
            className={`p-7 rounded-[32px] border transition-transform hover:-translate-y-1 ${
              isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-xs'
            }`}
          >
            <div className={`w-12 h-12 rounded-2xl flex items-center justify-center mb-5 ${
              isDarkTheme ? 'bg-[#2A1C16] text-[#D9A184]' : 'bg-[#F7EBE3] text-[#8F4C2B]'
            }`}>
              <HardDrive className="w-6 h-6" />
            </div>
            <h3 className="text-[19px] font-bold font-serif mb-2">On-Device SQLite Vault</h3>
            <p className={`text-[14px] leading-relaxed ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
              Everything is written to local Room SQLite in your phone’s protected sandbox. One-tap JSON export gives you complete data ownership.
            </p>
          </div>
        </div>

        {/* Clear Manifest Verification Link */}
        <div className="text-center">
          <a
            href="https://github.com/bobby-99/reflex/blob/main/app/src/main/AndroidManifest.xml"
            target="_blank"
            rel="noopener noreferrer"
            className={`inline-flex items-center gap-1.5 text-[14px] font-semibold underline underline-offset-4 transition-colors ${
              isDarkTheme ? 'text-[#D9A184] hover:text-[#F5F2EF]' : 'text-[#8F4C2B] hover:text-[#1A1614]'
            }`}
          >
            <span>Verify in AndroidManifest.xml on GitHub</span>
            <ExternalLink className="w-3.5 h-3.5" />
          </a>
        </div>
      </div>
    </section>
  );
};
