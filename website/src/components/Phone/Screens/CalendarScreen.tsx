import React, { useState } from 'react';
import { ChevronLeft, ChevronRight, ChevronDown, ChevronUp, Check, Settings as SettingsIcon, Calendar as CalendarIcon, Clock, CheckSquare, Plus, X } from 'lucide-react';

interface CalendarScreenProps {
  isDarkTheme: boolean;
  onOpenSettings: () => void;
  onAddTaskForDate?: () => void;
}

interface CalendarAgendaItem {
  id: string;
  timeLabel: string;
  title: string;
  subtitle: string;
  type: 'task' | 'routine' | 'event';
  priority?: 'High' | 'Medium' | 'Low';
  isCompleted?: boolean;
}

const INITIAL_AGENDA_ITEMS: CalendarAgendaItem[] = [
  {
    id: 'item_journal',
    timeLabel: 'All day',
    title: 'Daily evening journal entry',
    subtitle: 'Task · Low priority',
    type: 'task',
    priority: 'Low',
    isCompleted: false,
  },
  {
    id: 'item_launch',
    timeLabel: 'All day',
    title: 'Draft product launch announcement post',
    subtitle: 'Task · High priority',
    type: 'task',
    priority: 'High',
    isCompleted: true,
  },
  {
    id: 'item_hydration',
    timeLabel: 'All day',
    title: 'Hydration & evening mobility',
    subtitle: 'Routine · 3 steps',
    type: 'routine',
    isCompleted: true,
  },
  {
    id: 'item_sync',
    timeLabel: '11:00 am - 11:30 am',
    title: 'Review and merge mobile redesign PRs',
    subtitle: 'Task · High priority',
    type: 'task',
    priority: 'High',
    isCompleted: false,
  },
];

export const CalendarScreen: React.FC<CalendarScreenProps> = ({
  isDarkTheme,
  onOpenSettings,
  onAddTaskForDate,
}) => {
  const [isMonthExpanded, setIsMonthExpanded] = useState<boolean>(true);
  const [selectedDay, setSelectedDay] = useState<number>(5);
  const [activeFilter, setActiveFilter] = useState<'all' | 'events' | 'tasks' | 'routines'>('all');
  const [showSyncBanner, setShowSyncBanner] = useState<boolean>(true);
  const [agendaItems, setAgendaItems] = useState<CalendarAgendaItem[]>(INITIAL_AGENDA_ITEMS);

  const toggleItem = (id: string) => {
    setAgendaItems((prev) =>
      prev.map((it) => (it.id === id ? { ...it, isCompleted: !it.isCompleted } : it))
    );
  };

  const weekHeaders = ['Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa', 'Su'];

  // Days for October 2026: Starts on Thursday Oct 1 (so Mon 28, Tue 29, Wed 30 of Sep precede it)
  const monthCells = [
    { day: 28, isPrev: true, green: true, copper: false, blue: false },
    { day: 29, isPrev: true, green: false, copper: true, blue: false },
    { day: 30, isPrev: true, green: true, copper: false, blue: false },
    { day: 1, isPrev: false, green: true, copper: false, blue: false },
    { day: 2, isPrev: false, green: true, copper: false, blue: false },
    { day: 3, isPrev: false, green: true, copper: false, blue: false },
    { day: 4, isPrev: false, green: true, copper: true, blue: false },
    { day: 5, isPrev: false, green: true, copper: true, blue: false, isToday: true },
    { day: 6, isPrev: false, green: true, copper: true, blue: false },
    { day: 7, isPrev: false, green: true, copper: false, blue: false },
    { day: 8, isPrev: false, green: true, copper: true, blue: false },
    { day: 9, isPrev: false, green: true, copper: false, blue: false },
    { day: 10, isPrev: false, green: true, copper: true, blue: false },
    { day: 11, isPrev: false, green: true, copper: false, blue: false },
    { day: 12, isPrev: false, green: true, copper: false, blue: false },
    { day: 13, isPrev: false, green: true, copper: true, blue: false },
    { day: 14, isPrev: false, green: true, copper: false, blue: false },
    { day: 15, isPrev: false, green: true, copper: false, blue: false },
    { day: 16, isPrev: false, green: true, copper: false, blue: false },
    { day: 17, isPrev: false, green: false, copper: false, blue: false },
    { day: 18, isPrev: false, green: true, copper: false, blue: false },
    { day: 19, isPrev: false, green: true, copper: false, blue: false },
    { day: 20, isPrev: false, green: false, copper: true, blue: false },
    { day: 21, isPrev: false, green: true, copper: false, blue: false },
    { day: 22, isPrev: false, green: false, copper: false, blue: false },
    { day: 23, isPrev: false, green: true, copper: false, blue: false },
    { day: 24, isPrev: false, green: false, copper: false, blue: false },
    { day: 25, isPrev: false, green: true, copper: false, blue: false },
    { day: 26, isPrev: false, green: true, copper: false, blue: false },
    { day: 27, isPrev: false, green: false, copper: false, blue: false },
    { day: 28, isPrev: false, green: true, copper: false, blue: false },
    { day: 29, isPrev: false, green: false, copper: false, blue: false },
    { day: 30, isPrev: false, green: true, copper: false, blue: false },
    { day: 31, isPrev: false, green: false, copper: false, blue: false },
    { day: 1, isNext: true, green: true, copper: false, blue: false },
  ];

  const filteredItems = agendaItems.filter((item) => {
    if (activeFilter === 'events') return item.type === 'event';
    if (activeFilter === 'tasks') return item.type === 'task';
    if (activeFilter === 'routines') return item.type === 'routine';
    return true;
  });

  return (
    <div className={`h-full flex flex-col ${isDarkTheme ? 'text-[#F5F2EF]' : 'text-[#1A1614]'}`}>
      <div className="flex-1 overflow-y-auto reflex-scrollbar px-4 pt-6 pb-28">
        {/* Top Header matching 6_calendar.png */}
        <div className="flex items-center justify-between mb-3">
          <h1 className="text-[28px] font-bold leading-tight font-serif">Calendar</h1>
          <div className="flex items-center gap-2">
            <button
              onClick={() => onAddTaskForDate && onAddTaskForDate()}
              className={`px-3 py-1.5 rounded-full text-[12px] font-semibold flex items-center gap-1 transition-colors ${
                isDarkTheme ? 'bg-[#1C1A17] text-[#D9A184]' : 'bg-[#F7EBE3] text-[#8F4C2B]'
              }`}
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Event</span>
            </button>
            <button
              onClick={() => setSelectedDay(5)}
              className={`px-3 py-1.5 rounded-full text-[12px] font-semibold transition-colors ${
                isDarkTheme ? 'bg-[#D9A184] text-[#0A0908]' : 'bg-[#8F4C2B] text-white'
              }`}
            >
              Today
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

        {/* Morphing Calendar Card matching 6_calendar.png */}
        <div
          className={`p-4 rounded-[28px] mb-3 border ${
            isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-xs'
          }`}
        >
          {/* Calendar Month Header */}
          <div className="flex items-center justify-between mb-2">
            <button
              className={`w-7 h-7 rounded-full flex items-center justify-center ${
                isDarkTheme ? 'bg-[#1C1A17] text-[#A39E98]' : 'bg-[#EFEAE4] text-[#5D5750]'
              }`}
            >
              <ChevronLeft className="w-4 h-4" />
            </button>

            <button
              onClick={() => setIsMonthExpanded(!isMonthExpanded)}
              className="flex items-center gap-1 text-[15px] font-bold font-serif"
            >
              <span>October 2026</span>
              {isMonthExpanded ? (
                <ChevronUp className="w-4 h-4 text-[#D9A184]" />
              ) : (
                <ChevronDown className="w-4 h-4 text-[#D9A184]" />
              )}
            </button>

            <button
              className={`w-7 h-7 rounded-full flex items-center justify-center ${
                isDarkTheme ? 'bg-[#1C1A17] text-[#A39E98]' : 'bg-[#EFEAE4] text-[#5D5750]'
              }`}
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>

          {/* Weekday Names */}
          <div className="grid grid-cols-7 gap-1 text-center text-[11px] font-medium opacity-65 mb-2">
            {weekHeaders.map((h, i) => (
              <span key={i}>{h}</span>
            ))}
          </div>

          {/* Grid (Month or Week strip) */}
          <div className="grid grid-cols-7 gap-1">
            {(isMonthExpanded ? monthCells : monthCells.slice(7, 14)).map((c, i) => {
              const isSelected = selectedDay === c.day && !c.isPrev && !c.isNext;
              return (
                <button
                  key={i}
                  onClick={() => {
                    if (!c.isPrev && !c.isNext) setSelectedDay(c.day);
                  }}
                  className="flex flex-col items-center py-0.5 rounded-xl transition-all"
                >
                  <div
                    className={`w-[32px] h-[32px] rounded-full flex items-center justify-center text-[12px] font-medium transition-all ${
                      isSelected
                        ? isDarkTheme ? 'bg-[#D9A184] text-[#0A0908] font-bold shadow-sm' : 'bg-[#8F4C2B] text-white font-bold shadow-sm'
                        : c.isPrev || c.isNext
                        ? isDarkTheme ? 'text-[#4A4540]' : 'text-[#B0A8A0]'
                        : isDarkTheme ? 'text-[#F5F2EF]' : 'text-[#1A1614]'
                    }`}
                  >
                    {c.day}
                  </div>
                  {/* Category Indicator Dots */}
                  <div className="flex gap-0.5 mt-0.5 h-1">
                    {c.green && (
                      <span className={`w-1 h-1 rounded-full ${isDarkTheme ? 'bg-[#4ADE80]' : 'bg-[#16A34A]'}`} />
                    )}
                    {c.copper && (
                      <span className={`w-1 h-1 rounded-full ${isDarkTheme ? 'bg-[#D9A184]' : 'bg-[#8F4C2B]'}`} />
                    )}
                    {c.blue && (
                      <span className={`w-1 h-1 rounded-full ${isDarkTheme ? 'bg-[#6F9BFF]' : 'bg-[#3565D6]'}`} />
                    )}
                  </div>
                </button>
              );
            })}
          </div>

          {/* Morph handle */}
          <div className="flex justify-center pt-2">
            <button
              onClick={() => setIsMonthExpanded(!isMonthExpanded)}
              className={`w-10 h-1 rounded-full ${isDarkTheme ? 'bg-[#2E2A27]' : 'bg-[#EFEAE4]'}`}
            />
          </div>
        </div>

        {/* Filter Pills matching 6_calendar.png */}
        <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar pb-3 mb-1">
          {(['all', 'events', 'tasks', 'routines'] as const).map((flt) => (
            <button
              key={flt}
              onClick={() => setActiveFilter(flt)}
              className={`px-3.5 py-1.5 rounded-full text-[12px] font-semibold capitalize transition-colors ${
                activeFilter === flt
                  ? isDarkTheme ? 'bg-[#D9A184] text-[#0A0908]' : 'bg-[#8F4C2B] text-white'
                  : isDarkTheme ? 'bg-[#141211] border border-[#2E2A27] text-[#A39E98]' : 'bg-[#FFFFFF] border border-[rgba(26,22,20,0.13)] text-[#5D5750]'
              }`}
            >
              {flt === 'all' ? 'All' : flt}
            </button>
          ))}
        </div>

        {/* Device Calendar Sync Banner */}
        {showSyncBanner && (
          <div
            className={`p-3 rounded-2xl mb-3 border flex items-center justify-between text-[12px] ${
              isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)]'
            }`}
          >
            <div className="flex items-center gap-2">
              <CalendarIcon className={`w-4 h-4 ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`} />
              <span className="font-medium">Connect device calendar</span>
            </div>
            <button onClick={() => setShowSyncBanner(false)} className="opacity-60 hover:opacity-100">
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        )}

        {/* Agenda Section Header */}
        <div className="flex items-center justify-between mb-2 px-1">
          <div className="flex items-center gap-2">
            <span
              className={`w-6 h-6 rounded-full flex items-center justify-center text-[12px] font-bold ${
                isDarkTheme ? 'bg-[#D9A184] text-[#0A0908]' : 'bg-[#8F4C2B] text-white'
              }`}
            >
              5
            </span>
            <span className="text-[14px] font-bold">Monday, Oct 5</span>
            <span
              className={`text-[10px] px-2 py-0.5 rounded-full font-bold ${
                isDarkTheme ? 'bg-[#2A1C16] text-[#D9A184]' : 'bg-[#F7EBE3] text-[#8F4C2B]'
              }`}
            >
              Today
            </span>
            <span className={`text-[11px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
              11 items
            </span>
          </div>
          <div className="flex items-center gap-1.5">
            <button
              onClick={() => onAddTaskForDate && onAddTaskForDate()}
              className={`text-[11px] px-2 py-0.5 rounded-full font-medium border ${
                isDarkTheme ? 'border-[#2E2A27] text-[#A39E98]' : 'border-[rgba(26,22,20,0.13)] text-[#5D5750]'
              }`}
            >
              + Event
            </button>
            <button
              onClick={() => onAddTaskForDate && onAddTaskForDate()}
              className={`text-[11px] px-2 py-0.5 rounded-full font-medium border ${
                isDarkTheme ? 'border-[#2E2A27] text-[#A39E98]' : 'border-[rgba(26,22,20,0.13)] text-[#5D5750]'
              }`}
            >
              + Task
            </button>
          </div>
        </div>

        {/* Agenda Feed Cards */}
        <div className="space-y-2">
          {filteredItems.map((item) => (
            <div
              key={item.id}
              onClick={() => toggleItem(item.id)}
              className={`p-3.5 rounded-[22px] border flex items-center justify-between cursor-pointer transition-all ${
                isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-xs'
              }`}
            >
              <div className="flex items-center gap-3 min-w-0">
                <div
                  className={`w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 ${
                    item.type === 'routine'
                      ? isDarkTheme ? 'bg-[#1C1A17] text-[#86C9A4]' : 'bg-[#327A54]/15 text-[#327A54]'
                      : isDarkTheme ? 'bg-[#2A1C16] text-[#D9A184]' : 'bg-[#F7EBE3] text-[#8F4C2B]'
                  }`}
                >
                  {item.type === 'routine' ? (
                    <Clock className="w-4 h-4" />
                  ) : (
                    <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                      <polyline points="4 7 7 10 12 5" />
                      <line x1="15" y1="7" x2="21" y2="7" />
                      <polyline points="4 17 7 20 12 15" />
                      <line x1="15" y1="17" x2="21" y2="17" />
                    </svg>
                  )}
                </div>

                <div className="min-w-0">
                  <span className={`text-[11px] block leading-none mb-1 opacity-75`}>
                    {item.timeLabel}
                  </span>
                  <span className={`text-[13px] font-semibold block leading-tight truncate ${item.isCompleted ? 'line-through opacity-55' : ''}`}>
                    {item.title}
                  </span>
                  <span className={`text-[11px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                    {item.subtitle}
                  </span>
                </div>
              </div>

              {/* Status checkbox ring */}
              <div
                className={`w-[22px] h-[22px] rounded-full border flex items-center justify-center flex-shrink-0 ${
                  item.isCompleted
                    ? isDarkTheme ? 'bg-[#D9A184] border-[#D9A184] text-[#0A0908]' : 'bg-[#8F4C2B] border-[#8F4C2B] text-white'
                    : isDarkTheme ? 'border-[#3E3A36]' : 'border-[rgba(26,22,20,0.2)]'
                }`}
              >
                {item.isCompleted && <Check className="w-3.5 h-3.5 stroke-[3]" />}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
