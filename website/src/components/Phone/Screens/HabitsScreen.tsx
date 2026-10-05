import React, { useState } from 'react';
import { HabitItem } from '../../../types';
import { Flame, Check, Plus, Minus, BarChart3, Settings as SettingsIcon, X } from 'lucide-react';

interface HabitsScreenProps {
  isDarkTheme: boolean;
  onOpenSettings: () => void;
}

const INITIAL_HABITS: HabitItem[] = [
  {
    id: 'habit_meditation',
    name: 'Morning Meditation',
    type: 'checkoff',
    currentValue: 1,
    targetValue: 1,
    streakDays: 30,
    history: [true, true, true, true, true, true, true],
  },
  {
    id: 'habit_water',
    name: 'Drink 2.5L Water',
    type: 'measurable',
    currentValue: 2.3,
    targetValue: 2.5,
    unit: 'L',
    streakDays: 24,
    history: [true, true, true, true, true, true, false],
  },
  {
    id: 'habit_reading',
    name: 'Read 20 Pages',
    type: 'measurable',
    currentValue: 25,
    targetValue: 20,
    unit: 'pages',
    streakDays: 30,
    history: [true, true, true, true, true, true, true],
  },
  {
    id: 'habit_deepwork',
    name: 'Deep Work (2+ hrs)',
    type: 'measurable',
    currentValue: 2.5,
    targetValue: 2.0,
    unit: 'hrs',
    streakDays: 18,
    history: [true, true, true, true, true, true, true],
  },
  {
    id: 'habit_coffee',
    name: 'Espresso limit',
    type: 'limit',
    currentValue: 1,
    targetValue: 2,
    unit: 'cups',
    streakDays: 14,
    history: [true, true, true, true, true, true, true],
  },
];

export const HabitsScreen: React.FC<HabitsScreenProps> = ({
  isDarkTheme,
  onOpenSettings,
}) => {
  const [habits, setHabits] = useState<HabitItem[]>(INITIAL_HABITS);
  const [showAnalytics, setShowAnalytics] = useState<boolean>(false);

  const toggleCheckoff = (id: string) => {
    setHabits((prev) =>
      prev.map((h) => (h.id === id ? { ...h, currentValue: h.currentValue >= 1 ? 0 : 1 } : h))
    );
  };

  const adjustValue = (id: string, delta: number) => {
    setHabits((prev) =>
      prev.map((h) => {
        if (h.id !== id) return h;
        const step = h.unit === 'L' || h.unit === 'hrs' ? 0.2 : 1;
        const next = Math.max(0, Math.round((h.currentValue + delta * step) * 10) / 10);
        return { ...h, currentValue: next };
      })
    );
  };

  const completedCount = habits.filter((h) => {
    if (h.type === 'checkoff') return h.currentValue >= 1;
    if (h.type === 'measurable') return h.currentValue >= h.targetValue;
    if (h.type === 'limit') return h.currentValue <= h.targetValue;
    return false;
  }).length;
  const totalHabits = habits.length;
  const pct = Math.round((completedCount / totalHabits) * 100);

  const radius = 38;
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset = circumference - (pct / 100) * circumference;

  const weekDayLabels = ['T', 'W', 'T', 'F', 'S', 'S', 'M'];

  return (
    <div className={`h-full overflow-y-auto reflex-scrollbar px-4 pt-6 pb-28 ${isDarkTheme ? 'text-[#F5F2EF]' : 'text-[#1A1614]'}`}>
      {/* Top Header */}
      <div className="flex items-center justify-between mb-3">
        <div>
          <h1 className="text-[28px] font-bold leading-tight font-serif">Habits</h1>
          <p className={`text-[12px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
            Local tracker · offline
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowAnalytics(true)}
            className={`w-[36px] h-[36px] rounded-full flex items-center justify-center transition-colors ${
              isDarkTheme ? 'bg-[#1C1A17] text-[#A39E98]' : 'bg-[#EFEAE4] text-[#5D5750]'
            }`}
            title="Analytics"
          >
            <BarChart3 className="w-4 h-4" />
          </button>
          <button
            onClick={onOpenSettings}
            className={`w-[36px] h-[36px] rounded-full flex items-center justify-center transition-colors ${
              isDarkTheme ? 'bg-[#1C1A17] text-[#A39E98]' : 'bg-[#EFEAE4] text-[#5D5750]'
            }`}
            title="Settings"
          >
            <SettingsIcon className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Streak Hero Card (Matching 5_habits.png) */}
      <div
        className={`p-4 rounded-[28px] mb-3 border ${
          isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-xs'
        }`}
      >
        <span className="text-[13px] font-bold block">Streak</span>
        <span className={`text-[11px] block mb-2 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
          consecutive active days
        </span>

        <div className="flex items-baseline justify-between mb-4">
          <div>
            <span className="text-[42px] font-bold leading-none font-serif tnum">30</span>
            <span className={`text-[12px] block mt-0.5 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
              day streak
            </span>
          </div>

          <div
            className={`p-3 rounded-2xl text-left border ${
              isDarkTheme ? 'bg-[#1C1A17] border-[#2E2A27]' : 'bg-[#F7EBE3] border-[rgba(26,22,20,0.13)]'
            }`}
          >
            <span className="text-[20px] font-bold leading-none block font-serif tnum">30</span>
            <span className={`text-[11px] font-semibold block mt-0.5 ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`}>
              Best
            </span>
            <span className={`text-[10px] block opacity-75`}>longest run</span>
          </div>
        </div>

        {/* 7-Day Dots Strip */}
        <div className="flex items-center justify-between pt-2 border-t border-dashed border-neutral-700/20">
          {weekDayLabels.map((lbl, idx) => {
            const isToday = idx === 6; // Monday
            return (
              <div key={idx} className="flex flex-col items-center">
                <div
                  className={`w-[28px] h-[28px] rounded-full flex items-center justify-center text-[12px] font-semibold transition-colors ${
                    isDarkTheme
                      ? 'bg-[#2A1C16] text-[#D9A184]'
                      : 'bg-[#F7EBE3] text-[#8F4C2B]'
                  }`}
                >
                  {lbl}
                </div>
                {isToday && (
                  <span className={`text-[9px] font-bold mt-1 px-1.5 py-0.5 rounded-full ${
                    isDarkTheme ? 'bg-[#24211E] text-[#D9A184]' : 'bg-[#EFEAE4] text-[#8F4C2B]'
                  }`}>
                    Today
                  </span>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Daily Progress Card (Matching 5_habits.png) */}
      <div
        className={`p-4 rounded-[28px] mb-3 border flex items-center justify-between ${
          isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-xs'
        }`}
      >
        <div className="flex items-center gap-4">
          {/* Circular Progress Ring */}
          <div className="relative w-[88px] h-[88px] flex items-center justify-center flex-shrink-0">
            <svg className="w-full h-full -rotate-90" viewBox="0 0 96 96">
              <circle
                cx="48" cy="48" r={radius}
                fill="none"
                stroke={isDarkTheme ? '#24211E' : '#EFEAE4'}
                strokeWidth="7"
              />
              <circle
                cx="48" cy="48" r={radius}
                fill="none"
                stroke={isDarkTheme ? '#D9A184' : '#8F4C2B'}
                strokeWidth="7"
                strokeDasharray={circumference}
                strokeDashoffset={strokeDashoffset}
                strokeLinecap="round"
                className="transition-all duration-500 ease-out"
              />
            </svg>
            <div className="absolute inset-0 flex items-center justify-center">
              <span className="text-[17px] font-bold font-serif tnum">
                {completedCount}/{totalHabits}
              </span>
            </div>
          </div>

          <div>
            <span className="text-[14px] font-bold block">Daily progress</span>
            <span className={`text-[11px] block ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
              completed · today
            </span>
            <div className="text-[34px] font-bold leading-tight font-serif tnum mt-0.5">
              {pct}%
            </div>
          </div>
        </div>

        <button
          onClick={() => {}}
          className={`px-3 py-1.5 rounded-full text-[12px] font-semibold border transition-colors ${
            isDarkTheme ? 'bg-[#1C1A17] border-[#2E2A27] text-[#A39E98]' : 'bg-[#EFEAE4] border-[rgba(26,22,20,0.13)] text-[#5D5750]'
          }`}
        >
          Manage
        </button>
      </div>

      {/* Habit Cards List (Matching 5_habits.png) */}
      <div className="space-y-2 mb-4">
        {habits.map((habit) => {
          const isDone =
            habit.type === 'checkoff'
              ? habit.currentValue >= 1
              : habit.type === 'measurable'
              ? habit.currentValue >= habit.targetValue
              : habit.currentValue <= habit.targetValue;

          return (
            <div
              key={habit.id}
              className={`p-3.5 rounded-[22px] border flex items-center justify-between transition-all ${
                isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-xs'
              }`}
            >
              <div className="flex items-center gap-3 min-w-0">
                {/* Checkbox button */}
                <button
                  onClick={() => toggleCheckoff(habit.id)}
                  className={`w-[24px] h-[24px] rounded-full border flex items-center justify-center flex-shrink-0 transition-colors ${
                    isDone
                      ? isDarkTheme ? 'bg-[#2A1C16] border-[#D9A184] text-[#D9A184]' : 'bg-[#F7EBE3] border-[#8F4C2B] text-[#8F4C2B]'
                      : isDarkTheme ? 'border-[#3E3A36]' : 'border-[rgba(26,22,20,0.2)]'
                  }`}
                >
                  {isDone && <Check className="w-3.5 h-3.5 stroke-[3]" />}
                </button>

                <div className="min-w-0">
                  <span className={`text-[13px] font-semibold block leading-tight truncate ${isDone ? 'opacity-90' : ''}`}>
                    {habit.name}
                  </span>
                  {habit.type !== 'checkoff' && (
                    <span className={`text-[11px] tnum ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                      {habit.currentValue} / {habit.targetValue} {habit.unit}
                    </span>
                  )}
                </div>
              </div>

              {/* Stepper buttons for measurable and limit */}
              {habit.type !== 'checkoff' && (
                <div className="flex items-center gap-1.5 flex-shrink-0">
                  <button
                    onClick={() => adjustValue(habit.id, -1)}
                    className={`w-[30px] h-[30px] rounded-full flex items-center justify-center transition-transform active:scale-90 ${
                      isDarkTheme ? 'bg-[#24211E] text-[#F5F2EF]' : 'bg-[#EFEAE4] text-[#1A1614]'
                    }`}
                  >
                    <Minus className="w-3.5 h-3.5" />
                  </button>
                  <button
                    onClick={() => adjustValue(habit.id, 1)}
                    className={`w-[30px] h-[30px] rounded-full flex items-center justify-center transition-transform active:scale-90 ${
                      isDarkTheme ? 'bg-[#24211E] text-[#F5F2EF]' : 'bg-[#EFEAE4] text-[#1A1614]'
                    }`}
                  >
                    <Plus className="w-3.5 h-3.5" />
                  </button>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Analytics Modal */}
      {showAnalytics && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in">
          <div className={`w-full max-w-[320px] p-6 rounded-[32px] border relative ${
            isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-2xl'
          }`}>
            <button
              onClick={() => setShowAnalytics(false)}
              className="absolute top-4 right-4 p-1 rounded-full opacity-60 hover:opacity-100"
            >
              <X className="w-4 h-4" />
            </button>
            <h3 className="text-[18px] font-bold font-serif mb-4">Habit Analytics</h3>
            <div className="space-y-3 mb-5">
              <div className="flex justify-between text-[13px]">
                <span className={isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}>30-Day Rate</span>
                <span className="font-bold tnum">87%</span>
              </div>
              <div className="flex justify-between text-[13px]">
                <span className={isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}>Best Streak</span>
                <span className="font-bold tnum">30 days</span>
              </div>
              <div className="flex justify-between text-[13px]">
                <span className={isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}>Total Check-ins</span>
                <span className="font-bold tnum">142</span>
              </div>
            </div>
            <button
              onClick={() => setShowAnalytics(false)}
              className={`w-full py-2.5 rounded-full text-[13px] font-semibold ${
                isDarkTheme ? 'bg-[#D9A184] text-[#0A0908]' : 'bg-[#8F4C2B] text-white'
              }`}
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
