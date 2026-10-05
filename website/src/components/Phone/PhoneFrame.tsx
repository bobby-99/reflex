import React, { useState, useEffect } from 'react';
import { ActiveTab, ThemeMode } from '../../types';
import { PhoneBottomNav } from './PhoneBottomNav';
import { RoutinesScreen } from './Screens/RoutinesScreen';
import { TasksScreen } from './Screens/TasksScreen';
import { HabitsScreen } from './Screens/HabitsScreen';
import { FocusScreen } from './Screens/FocusScreen';
import { CalendarScreen } from './Screens/CalendarScreen';
import { SettingsScreen } from './Screens/SettingsScreen';
import { WifiOff, BatteryCharging } from 'lucide-react';

interface PhoneFrameProps {
  activeTab: ActiveTab;
  onSelectTab: (tab: ActiveTab) => void;
  themeMode: ThemeMode;
  onThemeChange: (theme: ThemeMode) => void;
  isDarkTheme: boolean;
  scale?: number;
  presetQuickAddText?: string | null;
  onClearPresetQuickAdd?: () => void;
  autoStartRoutineId?: string | null;
  autoStartFocus?: boolean;
}

export const PhoneFrame: React.FC<PhoneFrameProps> = ({
  activeTab,
  onSelectTab,
  themeMode,
  onThemeChange,
  isDarkTheme,
  scale = 1,
  presetQuickAddText,
  onClearPresetQuickAdd,
  autoStartRoutineId,
  autoStartFocus,
}) => {
  const [previousTab, setPreviousTab] = useState<ActiveTab>('routines');
  const [currentTimeStr, setCurrentTimeStr] = useState<string>('10:46');

  // Update status bar clock
  useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      let hours = now.getHours();
      const minutes = now.getMinutes();
      const ampm = hours >= 12 ? 'pm' : 'am';
      hours = hours % 12 || 12;
      setCurrentTimeStr(`${hours}:${minutes < 10 ? '0' : ''}${minutes}`);
    };
    updateTime();
    const interval = setInterval(updateTime, 30000);
    return () => clearInterval(interval);
  }, []);

  const handleOpenSettings = () => {
    setPreviousTab(activeTab);
    onSelectTab('settings');
  };

  const handleSettingsBack = () => {
    onSelectTab(previousTab);
  };

  return (
    <div
      className="relative select-none mx-auto"
      style={{
        width: Math.round(380 * scale),
        height: Math.round(820 * scale),
      }}
    >
      {/* Scaled Phone Enclosure */}
      <div
        className={`absolute top-0 left-0 w-[380px] h-[820px] rounded-[48px] p-[8px] shadow-2xl transition-colors duration-300 ${
          isDarkTheme
            ? 'bg-[#181614] border border-[#2E2A27]/80'
            : 'bg-[#E5E0D8] border border-[rgba(26,22,20,0.18)]'
        }`}
        style={{
          transform: `scale(${scale})`,
          transformOrigin: 'top left',
          boxShadow: isDarkTheme
            ? '0 25px 70px -12px rgba(0, 0, 0, 0.9), 0 0 0 1px rgba(255, 255, 255, 0.06)'
            : '0 25px 70px -12px rgba(26, 22, 20, 0.22), 0 0 0 1px rgba(26, 22, 20, 0.08)',
        }}
      >
        {/* Inner Screen Display (364 x 804 px) */}
        <div
          className={`relative w-full h-full rounded-[40px] overflow-hidden flex flex-col transition-colors duration-300 ${
            isDarkTheme ? 'bg-[#0A0908]' : 'bg-[#F7F3EE]'
          }`}
        >
          {/* Live Android Status Bar */}
          <div className="h-[38px] px-6 pt-2 flex items-center justify-between text-[12px] font-semibold tracking-tight z-30 select-none">
            {/* Clock */}
            <span className={`tnum ${isDarkTheme ? 'text-[#F5F2EF]' : 'text-[#1A1614]'}`}>
              {currentTimeStr}
            </span>

            {/* Centered Punch-Hole Camera */}
            <div className="w-[12px] h-[12px] rounded-full bg-black border border-neutral-800 shadow-inner flex-shrink-0" />

            {/* Offline & Battery Status Icons */}
            <div className="flex items-center gap-1.5 opacity-85">
              <span className={`text-[10px] font-bold tracking-wider px-1.5 py-0.2 rounded-full ${
                isDarkTheme ? 'bg-[#2A1C16] text-[#D9A184]' : 'bg-[#F7EBE3] text-[#8F4C2B]'
              }`}>
                OFFLINE
              </span>
              <WifiOff className={`w-3.5 h-3.5 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`} />
              <div className="flex items-center gap-0.5">
                <span className={`text-[11px] tnum ${isDarkTheme ? 'text-[#F5F2EF]' : 'text-[#1A1614]'}`}>94%</span>
                <BatteryCharging className={`w-3.5 h-3.5 ${isDarkTheme ? 'text-[#4ADE80]' : 'text-[#327A54]'}`} />
              </div>
            </div>
          </div>

          {/* Screen Content Container */}
          <div className="relative flex-1 w-full overflow-hidden">
            {activeTab === 'routines' && (
              <RoutinesScreen
                isDarkTheme={isDarkTheme}
                onOpenSettings={handleOpenSettings}
                autoStartRoutineId={autoStartRoutineId}
              />
            )}
            {activeTab === 'tasks' && (
              <TasksScreen
                isDarkTheme={isDarkTheme}
                onOpenSettings={handleOpenSettings}
                presetQuickAddText={presetQuickAddText}
                onClearPresetQuickAdd={onClearPresetQuickAdd}
              />
            )}
            {activeTab === 'habits' && (
              <HabitsScreen
                isDarkTheme={isDarkTheme}
                onOpenSettings={handleOpenSettings}
              />
            )}
            {activeTab === 'focus' && (
              <FocusScreen
                isDarkTheme={isDarkTheme}
                onOpenSettings={handleOpenSettings}
                autoStartFocus={autoStartFocus}
              />
            )}
            {activeTab === 'calendar' && (
              <CalendarScreen
                isDarkTheme={isDarkTheme}
                onOpenSettings={handleOpenSettings}
                onAddTaskForDate={() => onSelectTab('tasks')}
              />
            )}
            {activeTab === 'settings' && (
              <SettingsScreen
                themeMode={themeMode}
                onThemeChange={onThemeChange}
                isDarkTheme={isDarkTheme}
                onBack={handleSettingsBack}
              />
            )}
          </div>

          {/* Floating Frosted Glass Tab Bar (Hidden in Settings) */}
          {activeTab !== 'settings' && (
            <PhoneBottomNav
              activeTab={activeTab}
              onSelectTab={onSelectTab}
              isDarkTheme={isDarkTheme}
            />
          )}

          {/* Bottom Gesture Bar */}
          <div className="absolute bottom-[8px] left-0 right-0 flex justify-center pointer-events-none z-40">
            <div
              className={`w-[116px] h-[4px] rounded-full transition-colors ${
                isDarkTheme ? 'bg-[#F5F2EF]/25' : 'bg-[#1A1614]/25'
              }`}
            />
          </div>
        </div>
      </div>
    </div>
  );
};
