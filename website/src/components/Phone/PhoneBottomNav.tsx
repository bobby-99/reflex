import React from 'react';
import { ActiveTab } from '../../types';
import { Clock, Calendar as CalendarIcon, CheckSquare, Flame, Target } from 'lucide-react';

interface PhoneBottomNavProps {
  activeTab: ActiveTab;
  onSelectTab: (tab: ActiveTab) => void;
  isDarkTheme: boolean;
}

export const PhoneBottomNav: React.FC<PhoneBottomNavProps> = ({
  activeTab,
  onSelectTab,
  isDarkTheme,
}) => {
  return (
    <div className="absolute bottom-[24px] left-[16px] right-[16px] z-30 pointer-events-auto">
      <nav
        aria-label="Phone navigation"
        className={`h-[72px] rounded-[36px] px-2 flex items-center justify-between transition-colors duration-300 shadow-xl backdrop-blur-xl ${
          isDarkTheme ? 'reflex-glass-dark' : 'reflex-glass-light'
        }`}
        style={{
          backdropFilter: 'blur(20px)',
          WebkitBackdropFilter: 'blur(20px)',
        }}
      >
        {/* Tab 1: Routines */}
        <button
          onClick={() => onSelectTab('routines')}
          className={`flex-1 flex flex-col items-center justify-center py-1 transition-all rounded-full ${
            activeTab === 'routines'
              ? isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'
              : isDarkTheme ? 'text-[#A39E98] hover:text-[#F5F2EF]' : 'text-[#5D5750] hover:text-[#1A1614]'
          }`}
          aria-label="Routines tab"
        >
          <Clock className={`w-[22px] h-[22px] ${activeTab === 'routines' ? 'stroke-[2.3]' : 'stroke-[1.8]'}`} />
          <span className={`text-[12px] leading-tight mt-1 ${activeTab === 'routines' ? 'font-semibold' : 'font-normal'}`}>
            Routines
          </span>
        </button>

        {/* Tab 2: Calendar */}
        <button
          onClick={() => onSelectTab('calendar')}
          className={`flex-1 flex flex-col items-center justify-center py-1 transition-all rounded-full ${
            activeTab === 'calendar'
              ? isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'
              : isDarkTheme ? 'text-[#A39E98] hover:text-[#F5F2EF]' : 'text-[#5D5750] hover:text-[#1A1614]'
          }`}
          aria-label="Calendar tab"
        >
          <CalendarIcon className={`w-[22px] h-[22px] ${activeTab === 'calendar' ? 'stroke-[2.3]' : 'stroke-[1.8]'}`} />
          <span className={`text-[12px] leading-tight mt-1 ${activeTab === 'calendar' ? 'font-semibold' : 'font-normal'}`}>
            Calendar
          </span>
        </button>

        {/* Tab 3: Center Copper Action Button (Tasks) */}
        <div className="flex-shrink-0 px-1">
          <button
            onClick={() => onSelectTab('tasks')}
            className={`w-[50px] h-[50px] rounded-full flex items-center justify-center shadow-lg transition-transform active:scale-95 ${
              isDarkTheme
                ? 'bg-[#D9A184] text-[#0A0908] hover:bg-[#B57E63]'
                : 'bg-[#8F4C2B] text-[#FFFFFF] hover:bg-[#6F3A20]'
            }`}
            aria-label="Switch to tasks"
            title="Tasks"
          >
            {/* Reflex Checklist Tasks Icon from App */}
            <svg
              className="w-[22px] h-[22px]"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <polyline points="4 7 7 10 12 5" />
              <line x1="15" y1="7" x2="21" y2="7" />
              <polyline points="4 17 7 20 12 15" />
              <line x1="15" y1="17" x2="21" y2="17" />
            </svg>
          </button>
        </div>

        {/* Tab 4: Habits */}
        <button
          onClick={() => onSelectTab('habits')}
          className={`flex-1 flex flex-col items-center justify-center py-1 transition-all rounded-full ${
            activeTab === 'habits'
              ? isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'
              : isDarkTheme ? 'text-[#A39E98] hover:text-[#F5F2EF]' : 'text-[#5D5750] hover:text-[#1A1614]'
          }`}
          aria-label="Habits tab"
        >
          <Flame className={`w-[22px] h-[22px] ${activeTab === 'habits' ? 'stroke-[2.3]' : 'stroke-[1.8]'}`} />
          <span className={`text-[12px] leading-tight mt-1 ${activeTab === 'habits' ? 'font-semibold' : 'font-normal'}`}>
            Habits
          </span>
        </button>

        {/* Tab 5: Focus */}
        <button
          onClick={() => onSelectTab('focus')}
          className={`flex-1 flex flex-col items-center justify-center py-1 transition-all rounded-full ${
            activeTab === 'focus'
              ? isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'
              : isDarkTheme ? 'text-[#A39E98] hover:text-[#F5F2EF]' : 'text-[#5D5750] hover:text-[#1A1614]'
          }`}
          aria-label="Focus tab"
        >
          <Target className={`w-[22px] h-[22px] ${activeTab === 'focus' ? 'stroke-[2.3]' : 'stroke-[1.8]'}`} />
          <span className={`text-[12px] leading-tight mt-1 ${activeTab === 'focus' ? 'font-semibold' : 'font-normal'}`}>
            Focus
          </span>
        </button>
      </nav>
    </div>
  );
};
