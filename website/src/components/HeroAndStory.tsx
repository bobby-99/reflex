import React, { useEffect, useRef, useState } from 'react';
import { ActiveTab, ThemeMode } from '../types';
import { PhoneFrame } from './Phone/PhoneFrame';
import {
  Download,
  Github,
  Shield,
  Sparkles,
  ArrowRight,
  Play,
  CheckSquare,
  Target,
  Clock,
  Flame,
  Calendar as CalendarIcon,
  Lock,
} from 'lucide-react';

interface HeroAndStoryProps {
  activeTab: ActiveTab;
  onSelectTab: (tab: ActiveTab) => void;
  themeMode: ThemeMode;
  onThemeChange: (theme: ThemeMode) => void;
  isDarkTheme: boolean;
  onTriggerQuickAddPreset: (text: string) => void;
  onTriggerRoutineStart: (id: string) => void;
  onTriggerFocusStart: () => void;
  presetQuickAddText?: string | null;
  onClearPresetQuickAdd?: () => void;
  autoStartRoutineId?: string | null;
  autoStartFocus?: boolean;
}

interface StorySection {
  id: string;
  tab: ActiveTab;
  eyebrow: string;
  title: string;
  description: string;
  bullets: { title: string; text: string }[];
}

const STORY_SECTIONS: StorySection[] = [
  {
    id: 'feature-routines',
    tab: 'routines',
    eyebrow: 'Sequential Runner',
    title: 'Routines with audio cues & momentum',
    description:
      'Chain timed intervals, check-off steps, and repetition counters into structured flows. Audible chimes and subtle haptics guide you step-by-step so you never have to stare at the screen.',
    bullets: [
      { title: 'Foreground Service', text: 'Preserves countdown execution even when phone is locked.' },
      { title: 'Configurable Rest & Auto-Advance', text: 'Configurable default rest intervals with skip controls.' },
      { title: 'Grace-Period Streaks', text: '1-day grace window ensures consistency without penalty.' },
    ],
  },
  {
    id: 'feature-tasks',
    tab: 'tasks',
    eyebrow: 'Intelligent Capture',
    title: 'Natural language tasks & priority alerts',
    description:
      'Type just like you think: "Submit report tmrw 5pm !!!". Reflex parses relative dates, clock times, recurrences, and priorities in real time with copper badge highlighting.',
    bullets: [
      { title: 'System-Wide Overlays', text: 'High & medium priority alerts float over any active app with snooze chips.' },
      { title: 'Recurring Rules', text: 'Weekly, weekday, or custom recurrence schedules with auto-generation.' },
      { title: 'Tactile Check-off', text: 'Instant swipe actions with 3.5-second undo toast.' },
    ],
  },
  {
    id: 'feature-habits',
    tab: 'habits',
    eyebrow: 'Atomic Consistency',
    title: 'Habits tailored to daily realities',
    description:
      'Track check-off habits, measurable targets with units (like 20 pages or 8 glasses), or strict limit habits (like max 2 espressos). View monthly consistency matrix and streak records.',
    bullets: [
      { title: 'AlarmManager Exact Alarms', text: 'Survives Android Doze mode without missing a nudge.' },
      { title: '30-Day Completion Rate', text: 'Clear percentage metrics and historical check-in counts.' },
      { title: 'Flexible Frequencies', text: 'Daily, specific weekdays, or custom times per week.' },
    ],
  },
  {
    id: 'feature-focus',
    tab: 'focus',
    eyebrow: 'Liquid Companion',
    title: 'Liquid focus timer & distraction barrier',
    description:
      'Choose between classic Pomodoro cycles, Timed Flow, or Open Zen Flow. The dynamic dual-wave liquid timer gently drains as you work, while non-invasive app blocking keeps your focus locked.',
    bullets: [
      { title: 'Dual-Wave Liquid Physics', text: 'Smooth sinusoidal waves with sloshing tilt and tabular numbers.' },
      { title: 'Distraction Barrier', text: 'Detects restricted apps and renders a gentle shield to return to focus.' },
      { title: 'Task Linking & Early Stop Insights', text: 'Preserves actual focused duration even if stopped early.' },
    ],
  },
  {
    id: 'feature-calendar',
    tab: 'calendar',
    eyebrow: 'Unified Chronology',
    title: 'Morphing calendar & timeline agenda',
    description:
      'A compact week strip that expands into a full month grid at a touch. Merges scheduled routines, pending tasks, and local device calendar events into one continuous chronological feed.',
    bullets: [
      { title: 'Chronological "Now" Divider', text: 'Displays exact current position between past and future items.' },
      { title: 'Device Calendar Sync', text: 'Live ContentObserver detects edits across all calendars on your phone.' },
      { title: 'Categorized Type Dots', text: 'Distinct indicators for device events, tasks, and routines.' },
    ],
  },
  {
    id: 'feature-settings',
    tab: 'settings',
    eyebrow: 'Zero Network Vault',
    title: 'Encrypted on-device Room SQLite database',
    description:
      'Reflex requests zero internet permission. Your personal schedule, habits, reflections, and tasks are stored strictly on-device in Room SQLite v14 with atomic JSON backup and instant wipe.',
    bullets: [
      { title: 'No Telemetry or Tracking', text: 'Zero network sockets, zero crash reporting, zero tracking.' },
      { title: 'Full JSON Export & Import', text: 'Export plain JSON or restore anytime with schema pre-validation.' },
      { title: 'Live Diagnostic Hub', text: 'Real-time visibility into overlay, calendar, and alarm permissions.' },
    ],
  },
];

export const HeroAndStory: React.FC<HeroAndStoryProps> = ({
  activeTab,
  onSelectTab,
  themeMode,
  onThemeChange,
  isDarkTheme,
  onTriggerQuickAddPreset,
  onTriggerRoutineStart,
  onTriggerFocusStart,
  presetQuickAddText,
  onClearPresetQuickAdd,
  autoStartRoutineId,
  autoStartFocus,
}) => {
  const userInteractedRef = useRef<boolean>(false);
  const userInteractTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const [mobilePhoneScale, setMobilePhoneScale] = useState<number>(0.52);
  const [desktopPhoneScale, setDesktopPhoneScale] = useState<number>(0.68);

  // Responsive scale for phone based on browser viewport height and width
  useEffect(() => {
    const updateScales = () => {
      if (typeof window === 'undefined') return;
      const vh = window.innerHeight;
      const vw = window.innerWidth;

      // Mobile phone scaling (max height ~60vh, max width ~330px)
      const maxMobileH = vh * 0.58;
      const maxMobileW = Math.min(vw - 32, 330);
      const mScaleH = maxMobileH / 820;
      const mScaleW = maxMobileW / 380;
      const responsiveMobile = Math.max(0.44, Math.min(0.56, Math.min(mScaleH, mScaleW)));
      setMobilePhoneScale(responsiveMobile);

      // Desktop phone scaling (comfortably fits on laptops 720p - 1080p without overflowing)
      const maxDesktopH = vh * 0.72;
      const dScaleH = maxDesktopH / 820;
      const responsiveDesktop = Math.max(0.56, Math.min(0.70, dScaleH));
      setDesktopPhoneScale(responsiveDesktop);
    };

    updateScales();
    window.addEventListener('resize', updateScales);
    return () => window.removeEventListener('resize', updateScales);
  }, []);

  // Allow visitor direct interaction with the phone without scroll fighting
  const handleUserSelectTab = (tab: ActiveTab) => {
    userInteractedRef.current = true;
    onSelectTab(tab);

    if (userInteractTimeoutRef.current) clearTimeout(userInteractTimeoutRef.current);
    userInteractTimeoutRef.current = setTimeout(() => {
      userInteractedRef.current = false;
    }, 9000);
  };

  // Scroll detection to gently synchronize phone screen as sections pass the viewport
  useEffect(() => {
    const handleScroll = () => {
      if (userInteractedRef.current) return;

      const heroEl = document.getElementById('hero-intro');
      const sectionElements = STORY_SECTIONS.map((s) => ({
        tab: s.tab,
        el: document.getElementById(s.id),
      }));

      const scrollY = window.scrollY;
      const viewportMid = scrollY + window.innerHeight * 0.45;

      // Check if we are still in hero
      if (heroEl) {
        const heroRect = heroEl.getBoundingClientRect();
        const heroBottom = heroRect.bottom + scrollY;
        if (viewportMid < heroBottom) {
          if (activeTab !== 'routines') {
            onSelectTab('routines');
          }
          return;
        }
      }

      // Check feature sections
      for (let i = sectionElements.length - 1; i >= 0; i--) {
        const item = sectionElements[i];
        if (item.el) {
          const rect = item.el.getBoundingClientRect();
          const top = rect.top + scrollY;
          if (viewportMid >= top) {
            if (activeTab !== item.tab) {
              onSelectTab(item.tab);
            }
            break;
          }
        }
      }
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, [activeTab, onSelectTab]);

  return (
    <div id="features" className="relative pt-24 pb-20">
      <div className="max-w-[1140px] w-full mx-auto px-4 sm:px-6">
        {/* Two-Column Grid: Left Column Scrolls, Right Column Stays Pinned */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-10 items-start">
          {/* =========================================================
              LEFT COLUMN: HERO NARRATIVE FOLLOWED BY 6 STORY CHAPTERS
              ========================================================= */}
          <div className="lg:col-span-7 flex flex-col">
            {/* 1. Hero Introduction */}
            <div id="hero-intro" className="min-h-[82vh] flex flex-col justify-center pt-8 pb-16">
              {/* Quiet Top Pill Badge */}
              <div
                className={`inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full text-[13px] font-medium mb-6 border transition-colors self-start ${
                  isDarkTheme
                    ? 'bg-[#141211] border-[#2E2A27] text-[#D9A184]'
                    : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] text-[#8F4C2B] shadow-2xs'
                }`}
              >
                <Shield className="w-4 h-4" />
                <span>Free & Open Source Android App</span>
              </div>

              {/* Headline in Lora */}
              <h1
                className="text-[38px] sm:text-[50px] lg:text-[54px] font-bold leading-[1.08] tracking-tight mb-5"
                style={{ textWrap: 'balance' }}
              >
                Start. Focus. Finish.{' '}
                <span className={isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}>
                  Completely offline.
                </span>
              </h1>

              {/* Supporting Sentence */}
              <p
                className={`text-[17px] sm:text-[19px] leading-relaxed max-w-[520px] mb-8 font-normal ${
                  isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'
                }`}
              >
                Routines, habits, tasks and deep focus, in one calm app that never leaves your phone.
              </p>

              {/* Action Buttons */}
              <div className="flex flex-wrap items-center gap-3.5 mb-10 w-full sm:w-auto">
                <a
                  href="https://github.com/bobby-99/reflex/releases/latest"
                  target="_blank"
                  rel="noopener noreferrer"
                  className={`w-full sm:w-auto px-7 py-3.5 rounded-full text-[15px] font-semibold flex items-center justify-center gap-2 transition-transform active:scale-95 shadow-md ${
                    isDarkTheme
                      ? 'bg-[#D9A184] text-[#0A0908] hover:bg-[#B57E63]'
                      : 'bg-[#8F4C2B] text-[#FFFFFF] hover:bg-[#6F3A20]'
                  }`}
                >
                  <Download className="w-4 h-4 stroke-[2.5]" />
                  Download APK
                </a>

                <a
                  href="https://github.com/bobby-99/reflex"
                  target="_blank"
                  rel="noopener noreferrer"
                  className={`w-full sm:w-auto px-6 py-3.5 rounded-full text-[15px] font-semibold flex items-center justify-center gap-2 border transition-colors ${
                    isDarkTheme
                      ? 'bg-[#141211] border-[#2E2A27] text-[#F5F2EF] hover:bg-[#1C1A17]'
                      : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] text-[#1A1614] hover:bg-[#F3EEE8] shadow-2xs'
                  }`}
                >
                  <Github className="w-4 h-4" />
                  View on GitHub
                </a>
              </div>

              {/* 3 Quiet Facts */}
              <div className="grid grid-cols-3 gap-4 pt-6 border-t border-neutral-700/15 w-full max-w-[480px] mb-8">
                <div>
                  <span className="text-[14px] font-bold block">100% Offline</span>
                  <span className={`text-[12px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                    No internet permission
                  </span>
                </div>
                <div>
                  <span className="text-[14px] font-bold block">No account</span>
                  <span className={`text-[12px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                    Zero cloud servers
                  </span>
                </div>
                <div>
                  <span className="text-[14px] font-bold block">Open source</span>
                  <span className={`text-[12px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                    GPL-3.0-or-later
                  </span>
                </div>
              </div>

              {/* Suggested Taps Card */}
              <div
                className={`p-3.5 rounded-2xl border w-full max-w-[480px] ${
                  isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-2xs'
                }`}
              >
                <div className="flex items-center gap-1.5 mb-2">
                  <Sparkles className={`w-4 h-4 ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`} />
                  <span className="text-[12px] font-bold uppercase tracking-wider">
                    Interactive phone hints
                  </span>
                </div>
                <div className="flex flex-wrap gap-2">
                  <button
                    onClick={() => onTriggerQuickAddPreset('Call mom tmrw 5pm !!!')}
                    className={`text-[12px] px-2.5 py-1 rounded-full font-medium transition-colors text-left flex items-center gap-1 ${
                      isDarkTheme ? 'bg-[#24211E] text-[#D9A184] hover:bg-[#2A1C16]' : 'bg-[#EFEAE4] text-[#8F4C2B] hover:bg-[#E2DDD5]'
                    }`}
                  >
                    <CheckSquare className="w-3 h-3" />
                    Type "Call mom tmrw 5pm !!!"
                  </button>
                  <button
                    onClick={() => onTriggerRoutineStart('morning_momentum')}
                    className={`text-[12px] px-2.5 py-1 rounded-full font-medium transition-colors text-left flex items-center gap-1 ${
                      isDarkTheme ? 'bg-[#24211E] text-[#D9A184] hover:bg-[#2A1C16]' : 'bg-[#EFEAE4] text-[#8F4C2B] hover:bg-[#E2DDD5]'
                    }`}
                  >
                    <Play className="w-3 h-3" />
                    Start Morning Routine
                  </button>
                  <button
                    onClick={onTriggerFocusStart}
                    className={`text-[12px] px-2.5 py-1 rounded-full font-medium transition-colors text-left flex items-center gap-1 ${
                      isDarkTheme ? 'bg-[#24211E] text-[#D9A184] hover:bg-[#2A1C16]' : 'bg-[#EFEAE4] text-[#8F4C2B] hover:bg-[#E2DDD5]'
                    }`}
                  >
                    <Target className="w-3 h-3" />
                    Launch Liquid Timer
                  </button>
                </div>
              </div>
            </div>

            {/* Mobile Fallback Phone Section (< lg screens) */}
            <div className="lg:hidden flex flex-col items-center my-10 w-full">
              <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar py-2 mb-4 w-full justify-center px-2">
                {STORY_SECTIONS.map((sec) => (
                  <button
                    key={sec.id}
                    onClick={() => handleUserSelectTab(sec.tab)}
                    className={`px-3 py-1 rounded-full text-[12px] font-medium whitespace-nowrap transition-colors ${
                      activeTab === sec.tab
                        ? isDarkTheme ? 'bg-[#D9A184] text-[#0A0908] font-bold' : 'bg-[#8F4C2B] text-white font-bold'
                        : isDarkTheme ? 'bg-[#1C1A17] text-[#A39E98]' : 'bg-[#EFEAE4] text-[#5D5750]'
                    }`}
                  >
                    {sec.eyebrow}
                  </button>
                ))}
              </div>

              <div className="w-full flex justify-center py-2 items-center">
                <PhoneFrame
                  activeTab={activeTab}
                  onSelectTab={handleUserSelectTab}
                  themeMode={themeMode}
                  onThemeChange={onThemeChange}
                  isDarkTheme={isDarkTheme}
                  scale={mobilePhoneScale}
                  presetQuickAddText={presetQuickAddText}
                  onClearPresetQuickAdd={onClearPresetQuickAdd}
                  autoStartRoutineId={autoStartRoutineId}
                  autoStartFocus={autoStartFocus}
                />
              </div>
            </div>

            {/* 2. All 6 Feature Story Chapters Scrolling Smoothly on Desktop */}
            <div id="features" className="space-y-28 pt-8 scroll-mt-28">
              {STORY_SECTIONS.map((sec, idx) => {
                const isCurrent = activeTab === sec.tab;
                return (
                  <div
                    key={sec.id}
                    id={sec.id}
                    className={`scroll-mt-32 p-6 sm:p-8 rounded-[36px] border transition-all duration-300 ${
                      isCurrent
                        ? isDarkTheme
                          ? 'bg-[#141211] border-[#D9A184]/45 shadow-xl'
                          : 'bg-[#FFFFFF] border-[#8F4C2B]/40 shadow-lg'
                        : isDarkTheme
                        ? 'bg-[#141211]/50 border-[#2E2A27]'
                        : 'bg-[#FFFFFF]/70 border-[rgba(26,22,20,0.13)]'
                    }`}
                  >
                    <div className="flex items-center gap-2 mb-3">
                      <span
                        className={`text-[12px] font-bold uppercase tracking-wider ${
                          isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'
                        }`}
                      >
                        0{idx + 1} · {sec.eyebrow}
                      </span>
                    </div>

                    <h3 className="text-[26px] sm:text-[30px] font-bold leading-snug tracking-tight mb-4">
                      {sec.title}
                    </h3>

                    <p
                      className={`text-[16px] leading-relaxed mb-6 ${
                        isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'
                      }`}
                    >
                      {sec.description}
                    </p>

                    {/* Bullet Highlights */}
                    <div className="space-y-3.5 mb-6">
                      {sec.bullets.map((b, bIdx) => (
                        <div key={bIdx} className="flex items-start gap-3">
                          <div
                            className={`w-5 h-5 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5 ${
                              isDarkTheme ? 'bg-[#2A1C16] text-[#D9A184]' : 'bg-[#F7EBE3] text-[#8F4C2B]'
                            }`}
                          >
                            <span className="text-[11px] font-bold">✓</span>
                          </div>
                          <div>
                            <span className="text-[14px] font-semibold block">{b.title}</span>
                            <span
                              className={`text-[13px] ${
                                isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'
                              }`}
                            >
                              {b.text}
                            </span>
                          </div>
                        </div>
                      ))}
                    </div>

                    {/* Presets Showcase within feature-routines */}
                    {sec.id === 'feature-routines' && (
                      <div className="mb-6 pt-2 border-t border-neutral-700/15">
                        <div className="flex items-center justify-between mb-3">
                          <span className="text-[13px] font-bold tracking-tight">
                            Built-in Presets
                          </span>
                          <span className={`text-[11px] ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`}>
                            10 structured routines
                          </span>
                        </div>
                        <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
                          <div
                            onClick={() => {
                              onTriggerRoutineStart('morning_routine');
                              handleUserSelectTab('routines');
                            }}
                            className={`p-3 rounded-[20px] border cursor-pointer transition-all hover:scale-[1.02] ${
                              isDarkTheme
                                ? 'bg-[#1C1A17] border-[#2E2A27] hover:border-[#D9A184]'
                                : 'bg-[#F9F7F5] border-[rgba(26,22,20,0.1)] hover:border-[#8F4C2B] shadow-2xs'
                            }`}
                          >
                            <div className="text-xl mb-1">☀️</div>
                            <div className="text-[13px] font-bold leading-tight">Morning Routine</div>
                            <div className={`text-[11px] mt-1 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                              6 steps · ~16 min
                            </div>
                            <div className={`text-[11px] font-semibold mt-2 ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`}>
                              Run on phone →
                            </div>
                          </div>

                          <div
                            onClick={() => {
                              onTriggerRoutineStart('deep_work_launch');
                              handleUserSelectTab('routines');
                            }}
                            className={`p-3 rounded-[20px] border cursor-pointer transition-all hover:scale-[1.02] ${
                              isDarkTheme
                                ? 'bg-[#1C1A17] border-[#2E2A27] hover:border-[#D9A184]'
                                : 'bg-[#F9F7F5] border-[rgba(26,22,20,0.1)] hover:border-[#8F4C2B] shadow-2xs'
                            }`}
                          >
                            <div className="text-xl mb-1">⚡</div>
                            <div className="text-[13px] font-bold leading-tight">Deep Work Launch</div>
                            <div className={`text-[11px] mt-1 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                              7 steps · ~5 min
                            </div>
                            <div className={`text-[11px] font-semibold mt-2 ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`}>
                              Run on phone →
                            </div>
                          </div>

                          <div
                            onClick={() => {
                              onTriggerRoutineStart('workout_circuit');
                              handleUserSelectTab('routines');
                            }}
                            className={`p-3 rounded-[20px] border cursor-pointer transition-all hover:scale-[1.02] ${
                              isDarkTheme
                                ? 'bg-[#1C1A17] border-[#2E2A27] hover:border-[#D9A184]'
                                : 'bg-[#F9F7F5] border-[rgba(26,22,20,0.1)] hover:border-[#8F4C2B] shadow-2xs'
                            }`}
                          >
                            <div className="text-xl mb-1">🏃</div>
                            <div className="text-[13px] font-bold leading-tight">7-Min Workout</div>
                            <div className={`text-[11px] mt-1 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                              6 steps · ~6 min
                            </div>
                            <div className={`text-[11px] font-semibold mt-2 ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`}>
                              Run on phone →
                            </div>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Interactive Button */}
                    <button
                      onClick={() => handleUserSelectTab(sec.tab)}
                      className={`text-[13px] font-semibold inline-flex items-center gap-1.5 px-4 py-2 rounded-full border transition-all ${
                        isCurrent
                          ? isDarkTheme
                            ? 'bg-[#D9A184] text-[#0A0908] border-[#D9A184]'
                            : 'bg-[#8F4C2B] text-white border-[#8F4C2B]'
                          : isDarkTheme
                          ? 'bg-[#1C1A17] text-[#F5F2EF] border-[#2E2A27] hover:bg-[#24211E]'
                          : 'bg-[#EFEAE4] text-[#1A1614] border-[rgba(26,22,20,0.13)] hover:bg-[#E2DDD5]'
                      }`}
                    >
                      <span>Interact on phone</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </button>
                  </div>
                );
              })}
            </div>
          </div>

          {/* =========================================================
              RIGHT COLUMN: ONE UNIFIED CENTERPIECE PHONE (STAYS PINNED!)
              ========================================================= */}
          <div className="hidden lg:flex lg:col-span-5 sticky top-20 self-start flex-col items-center justify-start z-30 pt-2">
            <div className="relative">
              {/* Subtle Ambient Radial Behind-Phone Glow */}
              <div
                className="absolute inset-0 rounded-full blur-3xl opacity-20 pointer-events-none -z-10"
                style={{
                  background: isDarkTheme
                    ? 'radial-gradient(circle, #D9A184 0%, rgba(0,0,0,0) 70%)'
                    : 'radial-gradient(circle, #8F4C2B 0%, rgba(255,255,255,0) 70%)',
                }}
              />

              {/* Status Header Pill */}
              <div
                className={`text-center text-[12px] font-medium py-1 px-3.5 rounded-full border shadow-xs mb-3 flex items-center justify-between transition-colors ${
                  isDarkTheme
                    ? 'bg-[#1C1A17] border-[#2E2A27] text-[#D9A184]'
                    : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] text-[#8F4C2B]'
                }`}
              >
                <span>Interactive Reflex Replica</span>
                <span className="text-[11px] opacity-75">100% Offline</span>
              </div>

              {/* The Pinned Interactive Phone */}
              <PhoneFrame
                activeTab={activeTab}
                onSelectTab={handleUserSelectTab}
                themeMode={themeMode}
                onThemeChange={onThemeChange}
                isDarkTheme={isDarkTheme}
                scale={desktopPhoneScale}
                presetQuickAddText={presetQuickAddText}
                onClearPresetQuickAdd={onClearPresetQuickAdd}
                autoStartRoutineId={autoStartRoutineId}
                autoStartFocus={autoStartFocus}
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
