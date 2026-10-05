import React, { useState } from 'react';
import { ThemeMode } from '../../../types';
import { ChevronLeft, ChevronRight, Search, ShieldCheck, Zap, Camera, Moon, Sun, Smartphone, Clock, Calendar as CalendarIcon, Flame, Target, HardDrive, Download, Trash2, Edit3, Check } from 'lucide-react';

interface SettingsScreenProps {
  themeMode: ThemeMode;
  onThemeChange: (theme: ThemeMode) => void;
  isDarkTheme: boolean;
  onBack: () => void;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({
  themeMode,
  onThemeChange,
  isDarkTheme,
  onBack,
}) => {
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [backupDownloaded, setBackupDownloaded] = useState<boolean>(false);
  const [showWipeNotice, setShowWipeNotice] = useState<boolean>(false);

  const triggerExport = () => {
    setBackupDownloaded(true);
    setTimeout(() => setBackupDownloaded(false), 3000);
  };

  return (
    <div className={`h-full flex flex-col ${isDarkTheme ? 'text-[#F5F2EF]' : 'text-[#1A1614]'}`}>
      <div className="flex-1 overflow-y-auto reflex-scrollbar px-4 pt-6 pb-28">
        {/* Top Header with Back */}
        <div className="flex items-center gap-3 mb-4">
          <button
            onClick={onBack}
            className={`w-[36px] h-[36px] rounded-full flex items-center justify-center transition-colors ${
              isDarkTheme ? 'bg-[#1C1A17] text-[#A39E98]' : 'bg-[#EFEAE4] text-[#5D5750]'
            }`}
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
          <h1 className="text-[26px] font-bold leading-tight font-serif">Settings</h1>
        </div>

        {/* Search Bar */}
        <div
          className={`flex items-center gap-2.5 px-4 py-3 rounded-full mb-4 border ${
            isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-xs'
          }`}
        >
          <Search className={`w-4 h-4 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`} />
          <input
            type="text"
            placeholder="Search settings"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="bg-transparent border-none outline-none text-[13px] w-full placeholder:opacity-60"
          />
        </div>

        {/* Permissions Attention Card */}
        <div
          className={`p-4 rounded-[26px] mb-5 border flex items-center justify-between ${
            isDarkTheme ? 'bg-[#2A1C16] border-[#D9A184]/40' : 'bg-[#F7EBE3] border-[#8F4C2B]/30'
          }`}
        >
          <div className="flex items-center gap-3">
            <div className={`w-10 h-10 rounded-full flex items-center justify-center ${
              isDarkTheme ? 'bg-[#1C1A17] text-[#D9A184]' : 'bg-white text-[#8F4C2B]'
            }`}>
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div>
              <span className={`text-[13px] font-bold block leading-snug ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`}>
                4 permissions need attention
              </span>
              <span className={`text-[11px] block ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                Voice input and app blocking are off
              </span>
            </div>
          </div>
          <ChevronRight className="w-4 h-4 opacity-60" />
        </div>

        {/* Profile Section */}
        <div className="mb-5">
          <span className="text-[14px] font-bold block mb-0.5 font-serif">Profile</span>
          <span className={`text-[11px] block mb-2.5 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
            Personalize your reflex workspace
          </span>

          <div
            className={`p-4 rounded-[26px] border flex items-center justify-between ${
              isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-xs'
            }`}
          >
            <div className="flex items-center gap-3.5">
              <div className="relative">
                <div
                  className={`w-12 h-12 rounded-full flex items-center justify-center ${
                    isDarkTheme ? 'bg-[#2A1C16] text-[#D9A184]' : 'bg-[#F7EBE3] text-[#8F4C2B]'
                  }`}
                >
                  <Zap className="w-6 h-6 fill-current" />
                </div>
                <div className={`absolute -bottom-1 -right-1 w-5 h-5 rounded-full flex items-center justify-center border ${
                  isDarkTheme ? 'bg-[#141211] border-[#2E2A27] text-[#D9A184]' : 'bg-white border-[#211A1614] text-[#8F4C2B]'
                }`}>
                  <Camera className="w-3 h-3" />
                </div>
              </div>

              <div>
                <h3 className="text-[16px] font-bold font-serif leading-tight">Bobby</h3>
                <span className={`text-[12px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                  Local-first productivity
                </span>
              </div>
            </div>

            <button className={`p-2 rounded-full ${isDarkTheme ? 'bg-[#1C1A17] text-[#A39E98]' : 'bg-[#EFEAE4] text-[#5D5750]'}`}>
              <Edit3 className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Appearance Section */}
        <div className="mb-5">
          <span className="text-[14px] font-bold block mb-0.5 font-serif">Appearance</span>
          <span className={`text-[11px] block mb-2.5 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
            Follows your phone by default
          </span>

          {/* 3-Way Mode Pill matching screenshot */}
          <div className={`p-1 rounded-full flex border ${
            isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-xs'
          }`}>
            <button
              onClick={() => onThemeChange('system')}
              className={`flex-1 py-2 rounded-full text-[12px] font-medium flex items-center justify-center gap-1.5 transition-colors ${
                themeMode === 'system'
                  ? isDarkTheme ? 'bg-[#D9A184] text-[#0A0908] font-bold' : 'bg-[#8F4C2B] text-white font-bold'
                  : isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'
              }`}
            >
              <Smartphone className="w-3.5 h-3.5" />
              <span>System</span>
            </button>
            <button
              onClick={() => onThemeChange('dark')}
              className={`flex-1 py-2 rounded-full text-[12px] font-medium flex items-center justify-center gap-1.5 transition-colors ${
                themeMode === 'dark'
                  ? isDarkTheme ? 'bg-[#D9A184] text-[#0A0908] font-bold' : 'bg-[#8F4C2B] text-white font-bold'
                  : isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'
              }`}
            >
              <Moon className="w-3.5 h-3.5" />
              <span>Dark</span>
            </button>
            <button
              onClick={() => onThemeChange('light')}
              className={`flex-1 py-2 rounded-full text-[12px] font-medium flex items-center justify-center gap-1.5 transition-colors ${
                themeMode === 'light'
                  ? isDarkTheme ? 'bg-[#D9A184] text-[#0A0908] font-bold' : 'bg-[#8F4C2B] text-white font-bold'
                  : isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'
              }`}
            >
              <Sun className="w-3.5 h-3.5" />
              <span>Light</span>
            </button>
          </div>
        </div>

        {/* Modules Section */}
        <div className="mb-5">
          <span className="text-[14px] font-bold block mb-0.5 font-serif">Modules</span>
          <span className={`text-[11px] block mb-2.5 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
            Tune each part of Reflex
          </span>

          <div
            className={`rounded-[26px] border divide-y overflow-hidden ${
              isDarkTheme ? 'bg-[#141211] border-[#2E2A27] divide-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] divide-[#F3EEE8] shadow-xs'
            }`}
          >
            {/* Routines */}
            <div className="p-3.5 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className={`w-8 h-8 rounded-full flex items-center justify-center ${isDarkTheme ? 'bg-[#1C1A17] text-[#A39E98]' : 'bg-[#EFEAE4] text-[#5D5750]'}`}>
                  <Clock className="w-4 h-4" />
                </div>
                <div>
                  <span className="text-[13px] font-semibold block leading-tight">Routines</span>
                  <span className={`text-[11px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                    Lead time at time · snooze 10 min
                  </span>
                </div>
              </div>
              <ChevronRight className="w-4 h-4 opacity-50" />
            </div>

            {/* Calendar */}
            <div className="p-3.5 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className={`w-8 h-8 rounded-full flex items-center justify-center ${isDarkTheme ? 'bg-[#1C1A17] text-[#A39E98]' : 'bg-[#EFEAE4] text-[#5D5750]'}`}>
                  <CalendarIcon className="w-4 h-4" />
                </div>
                <div>
                  <span className="text-[13px] font-semibold block leading-tight">Calendar</span>
                  <span className={`text-[11px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                    Week strip & agenda · 30d range
                  </span>
                </div>
              </div>
              <ChevronRight className="w-4 h-4 opacity-50" />
            </div>

            {/* Tasks */}
            <div className="p-3.5 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className={`w-8 h-8 rounded-full flex items-center justify-center ${isDarkTheme ? 'bg-[#1C1A17] text-[#A39E98]' : 'bg-[#EFEAE4] text-[#5D5750]'}`}>
                  <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                    <polyline points="4 7 7 10 12 5" />
                    <line x1="15" y1="7" x2="21" y2="7" />
                    <polyline points="4 17 7 20 12 15" />
                    <line x1="15" y1="17" x2="21" y2="17" />
                  </svg>
                </div>
                <div>
                  <span className="text-[13px] font-semibold block leading-tight">Tasks</span>
                  <span className={`text-[11px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                    Parsing on · default priority none
                  </span>
                </div>
              </div>
              <ChevronRight className="w-4 h-4 opacity-50" />
            </div>

            {/* Habits */}
            <div className="p-3.5 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className={`w-8 h-8 rounded-full flex items-center justify-center ${isDarkTheme ? 'bg-[#1C1A17] text-[#A39E98]' : 'bg-[#EFEAE4] text-[#5D5750]'}`}>
                  <Flame className="w-4 h-4" />
                </div>
                <div>
                  <span className="text-[13px] font-semibold block leading-tight">Habits</span>
                  <span className={`text-[11px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                    New habits are check-off · day ends 3 am
                  </span>
                </div>
              </div>
              <ChevronRight className="w-4 h-4 opacity-50" />
            </div>

            {/* Focus */}
            <div className="p-3.5 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className={`w-8 h-8 rounded-full flex items-center justify-center ${isDarkTheme ? 'bg-[#1C1A17] text-[#A39E98]' : 'bg-[#EFEAE4] text-[#5D5750]'}`}>
                  <Target className="w-4 h-4" />
                </div>
                <div>
                  <span className="text-[13px] font-semibold block leading-tight">Focus</span>
                  <span className={`text-[11px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                    App blocking off · auto-start off
                  </span>
                </div>
              </div>
              <ChevronRight className="w-4 h-4 opacity-50" />
            </div>
          </div>
        </div>

        {/* Local Vault Section */}
        <div className="mb-4">
          <span className="text-[14px] font-bold block mb-0.5 font-serif">Local Vault</span>
          <span className={`text-[11px] block mb-2.5 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
            100% On-device Room SQLite database
          </span>

          <div
            className={`p-4 rounded-[26px] border ${
              isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-xs'
            }`}
          >
            <div className="flex items-center justify-between mb-3 text-[12px]">
              <span className={isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}>Storage format</span>
              <span className="font-semibold">Room SQLite v14 (Encrypted)</span>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={triggerExport}
                className={`flex-1 py-2.5 rounded-full text-[12px] font-semibold flex items-center justify-center gap-1.5 transition-colors ${
                  backupDownloaded
                    ? 'bg-[#4ADE80] text-[#0A0908]'
                    : isDarkTheme ? 'bg-[#24211E] text-[#D9A184] hover:bg-[#2A1C16]' : 'bg-[#EFEAE4] text-[#8F4C2B] hover:bg-[#E2DDD5]'
                }`}
              >
                {backupDownloaded ? <Check className="w-3.5 h-3.5" /> : <Download className="w-3.5 h-3.5" />}
                <span>{backupDownloaded ? 'Exported JSON' : 'Export Backup'}</span>
              </button>

              <button
                onClick={() => setShowWipeNotice(true)}
                className="px-4 py-2.5 rounded-full text-[12px] font-semibold bg-[#2C1414] text-[#E05D5D] hover:bg-[#3D1A1A] transition-colors"
              >
                Wipe Vault
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Wipe Confirmation Dialog */}
      {showWipeNotice && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in">
          <div className={`w-full max-w-[320px] p-6 rounded-[32px] border text-center relative ${
            isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-2xl'
          }`}>
            <div className="w-12 h-12 rounded-full flex items-center justify-center mx-auto mb-3 bg-[#2C1414] text-[#E05D5D]">
              <Trash2 className="w-6 h-6" />
            </div>
            <h3 className="text-[17px] font-bold font-serif mb-1">Wipe All Local Data?</h3>
            <p className={`text-[12px] leading-relaxed mb-4 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
              This action immediately clears all tasks, routines, habits, and focus logs from your phone's Room SQLite database.
            </p>
            <div className="flex gap-2">
              <button
                onClick={() => setShowWipeNotice(false)}
                className={`flex-1 py-2.5 rounded-full text-[13px] font-medium ${
                  isDarkTheme ? 'bg-[#24211E] text-[#F5F2EF]' : 'bg-[#EFEAE4] text-[#1A1614]'
                }`}
              >
                Cancel
              </button>
              <button
                onClick={() => setShowWipeNotice(false)}
                className="flex-1 py-2.5 rounded-full text-[13px] font-semibold bg-[#E05D5D] text-white"
              >
                Wipe Now
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
