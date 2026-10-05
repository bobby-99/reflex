import React, { useState, useEffect } from 'react';
import { Play, Pause, SkipForward, CheckCircle2, RotateCcw, Flame, Check, Hourglass, Settings as SettingsIcon, Plus, X, Sparkles, Clock } from 'lucide-react';

interface RoutinesScreenProps {
  isDarkTheme: boolean;
  onOpenSettings: () => void;
  autoStartRoutineId?: string | null;
}

interface RoutineStep {
  title: string;
  type: 'timed' | 'reps' | 'checkoff';
  durationSec?: number;
  targetReps?: number;
}

interface RoutineTemplateItem {
  id: string;
  name: string;
  description: string;
  icon: string;
  stepsCount: number;
  durationLabel: string;
  steps: RoutineStep[];
}

const ALL_ROUTINE_PRESETS: RoutineTemplateItem[] = [
  {
    id: 'morning_routine',
    name: 'Morning Routine',
    description: 'Start your day with hydration, sunlight, movement, and focus.',
    icon: '☀️',
    stepsCount: 6,
    durationLabel: '~16 mins',
    steps: [
      { title: 'Drink a glass of water', type: 'checkoff' },
      { title: 'Step outside / get sunlight', type: 'timed', durationSec: 300 },
      { title: 'Stretch', type: 'timed', durationSec: 300 },
      { title: 'Breathe / meditate', type: 'timed', durationSec: 300 },
      { title: "Plan today's top 3 priorities", type: 'checkoff' },
      { title: 'Make your bed', type: 'checkoff' },
    ],
  },
  {
    id: 'deep_work_launch',
    name: 'Deep Work Launch',
    description: 'Get clear, remove distractions, and enter deep focus in 5 minutes.',
    icon: '⚡',
    stepsCount: 7,
    durationLabel: '~5 mins',
    steps: [
      { title: 'Phone out of reach', type: 'checkoff' },
      { title: 'Water filled, desk clear', type: 'checkoff' },
      { title: 'Brain dump (write down thoughts)', type: 'timed', durationSec: 120 },
      { title: 'Pick the one task, in one sentence', type: 'checkoff' },
      { title: 'Define what "done" looks like', type: 'checkoff' },
      { title: 'Box breathing (4-4-4-4)', type: 'timed', durationSec: 60 },
      { title: "Close everything you don't need", type: 'checkoff' },
    ],
  },
  {
    id: 'workout_circuit',
    name: '7-Minute Workout',
    description: 'Interval circuit with automatic rest breaks.',
    icon: '🏃',
    stepsCount: 6,
    durationLabel: '~6 mins',
    steps: [
      { title: 'Jumping jacks', type: 'timed', durationSec: 30 },
      { title: 'Wall sit', type: 'timed', durationSec: 30 },
      { title: 'Push-ups', type: 'reps', targetReps: 15 },
      { title: 'Ab crunches', type: 'reps', targetReps: 20 },
      { title: 'Squats', type: 'reps', targetReps: 20 },
      { title: 'Step-ups / high knees', type: 'timed', durationSec: 30 },
    ],
  },
  {
    id: 'desk_break',
    name: 'Desk Break',
    description: 'Quick 3-minute physical and visual reset for desk workers.',
    icon: '🧘',
    stepsCount: 7,
    durationLabel: '~4 mins',
    steps: [
      { title: 'Look 20 ft away (20-20-20 rule)', type: 'timed', durationSec: 20 },
      { title: 'Neck rolls', type: 'timed', durationSec: 30 },
      { title: 'Shoulder rolls', type: 'reps', targetReps: 10 },
      { title: 'Chest opener (hands clasped behind)', type: 'timed', durationSec: 30 },
      { title: 'Wrist flexor & extensor stretch', type: 'timed', durationSec: 30 },
      { title: 'Standing forward fold', type: 'timed', durationSec: 30 },
      { title: 'Drink water', type: 'checkoff' },
    ],
  },
  {
    id: 'night_wind_down',
    name: 'Night Wind-Down',
    description: 'ADHD-friendly calm wind-down structure before bed.',
    icon: '🌙',
    stepsCount: 6,
    durationLabel: '~20 mins',
    steps: [
      { title: "Set out tomorrow's clothes/bag", type: 'checkoff' },
      { title: 'Screens off, phone away', type: 'checkoff' },
      { title: 'Light stretch or deep breathing', type: 'timed', durationSec: 300 },
      { title: 'One-line journal / brain dump', type: 'timed', durationSec: 180 },
      { title: 'Read or low-stimulation activity', type: 'timed', durationSec: 600 },
      { title: 'Lights down, into bed', type: 'checkoff' },
    ],
  },
  {
    id: 'daily_reset',
    name: 'Daily Reset',
    description: 'Midday or evening reset to clear clutter and refocus.',
    icon: '🧹',
    stepsCount: 5,
    durationLabel: '~12 mins',
    steps: [
      { title: 'Tidy your space', type: 'timed', durationSec: 300 },
      { title: 'Drink water', type: 'checkoff' },
      { title: "Review today's tasks", type: 'checkoff' },
      { title: 'Quick walk or stretch', type: 'timed', durationSec: 300 },
      { title: "Set tomorrow's top priority", type: 'checkoff' },
    ],
  },
];

export const RoutinesScreen: React.FC<RoutinesScreenProps> = ({
  isDarkTheme,
  onOpenSettings,
  autoStartRoutineId,
}) => {
  const [activeRoutine, setActiveRoutine] = useState<RoutineTemplateItem>(ALL_ROUTINE_PRESETS[2]); // Default 7-minute workout
  const [isRunningRunner, setIsRunningRunner] = useState<boolean>(false);
  const [currentStepIdx, setCurrentStepIdx] = useState<number>(0);
  const [stepTimerSec, setStepTimerSec] = useState<number>(60);
  const [isTimerRunning, setIsTimerRunning] = useState<boolean>(false);
  const [isCompletedRoutine, setIsCompletedRoutine] = useState<boolean>(false);
  const [autoAdvance, setAutoAdvance] = useState<boolean>(true);
  const [confettiActive, setConfettiActive] = useState<boolean>(false);
  const [showTemplateModal, setShowTemplateModal] = useState<boolean>(false);

  // Tasks today check-off states
  const [taskStates, setTaskStates] = useState<Record<string, boolean>>({
    task_journal: false,
    task_prs: false,
    task_roadmap: false,
    task_walk: false,
  });

  useEffect(() => {
    if (autoStartRoutineId) {
      const found = ALL_ROUTINE_PRESETS.find((p) => p.id === autoStartRoutineId || p.id === 'morning_routine') || ALL_ROUTINE_PRESETS[0];
      startPreset(found);
    }
  }, [autoStartRoutineId]);

  // Step countdown timer effect
  useEffect(() => {
    let interval: NodeJS.Timeout | null = null;
    if (isTimerRunning && stepTimerSec > 0) {
      interval = setInterval(() => {
        setStepTimerSec((prev) => {
          if (prev <= 1) {
            handleStepFinished();
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    }
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [isTimerRunning, stepTimerSec, currentStepIdx, activeRoutine]);

  const startPreset = (preset: RoutineTemplateItem) => {
    setActiveRoutine(preset);
    setIsRunningRunner(true);
    setCurrentStepIdx(0);
    setIsCompletedRoutine(false);
    setStepTimerSec(preset.steps[0]?.durationSec || 60);
    setIsTimerRunning(true);
    setShowTemplateModal(false);
  };

  const startRunner = () => {
    startPreset(activeRoutine);
  };

  const handleStepFinished = () => {
    if (currentStepIdx < activeRoutine.steps.length - 1) {
      const nextIdx = currentStepIdx + 1;
      setCurrentStepIdx(nextIdx);
      const nextStep = activeRoutine.steps[nextIdx];
      setStepTimerSec(nextStep?.durationSec || 60);
      setIsTimerRunning(autoAdvance);
    } else {
      setIsCompletedRoutine(true);
      setIsTimerRunning(false);
      setConfettiActive(true);
      setTimeout(() => setConfettiActive(false), 4000);
    }
  };

  const exitRunner = () => {
    setIsRunningRunner(false);
    setIsTimerRunning(false);
    setIsCompletedRoutine(false);
  };

  const toggleTask = (key: string) => {
    setTaskStates((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  // 1. ACTIVE RUNNING IMMERSION RUNNER
  if (isRunningRunner) {
    const currentStep = activeRoutine.steps[currentStepIdx] || activeRoutine.steps[0];
    const totalSteps = activeRoutine.steps.length;
    const progressPct = ((currentStepIdx + (isCompletedRoutine ? 1 : 0)) / totalSteps) * 100;

    return (
      <div className={`relative h-full flex flex-col justify-between p-6 ${isDarkTheme ? 'bg-[#0A0908] text-[#F5F2EF]' : 'bg-[#F7F3EE] text-[#1A1614]'}`}>
        {/* Top Header */}
        <div className="flex items-center justify-between pt-6">
          <button
            onClick={exitRunner}
            className={`text-[12px] px-3 py-1.5 rounded-full font-medium transition-colors ${
              isDarkTheme ? 'bg-[#1C1A17] text-[#A39E98] hover:text-[#F5F2EF]' : 'bg-[#EFEAE4] text-[#5D5750] hover:text-[#1A1614]'
            }`}
          >
            ← Exit
          </button>
          <div className="text-center px-1 truncate">
            <span className={`text-[12px] font-medium tracking-wide block truncate max-w-[170px] ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`}>
              {activeRoutine.icon} {activeRoutine.name}
            </span>
            <div className={`text-[11px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
              Step {currentStepIdx + 1} of {totalSteps}
            </div>
          </div>
          <button
            onClick={() => setAutoAdvance(!autoAdvance)}
            className={`text-[11px] px-2.5 py-1 rounded-full transition-colors ${
              autoAdvance
                ? isDarkTheme ? 'bg-[#2A1C16] text-[#D9A184]' : 'bg-[#F7EBE3] text-[#8F4C2B]'
                : isDarkTheme ? 'bg-[#1C1A17] text-[#A39E98]' : 'bg-[#EFEAE4] text-[#5D5750]'
            }`}
          >
            Auto {autoAdvance ? 'on' : 'off'}
          </button>
        </div>

        {/* Progress Bar */}
        <div className={`w-full h-1.5 rounded-full overflow-hidden my-3 ${isDarkTheme ? 'bg-[#24211E]' : 'bg-[#EFEAE4]'}`}>
          <div
            className={`h-full transition-all duration-300 ${isDarkTheme ? 'bg-[#D9A184]' : 'bg-[#8F4C2B]'}`}
            style={{ width: `${progressPct}%` }}
          />
        </div>

        {/* Step Body */}
        {isCompletedRoutine ? (
          <div className="flex-1 flex flex-col items-center justify-center text-center px-4">
            <div className={`w-16 h-16 rounded-full flex items-center justify-center mb-4 ${isDarkTheme ? 'bg-[#2A1C16] text-[#4ADE80]' : 'bg-[#327A54]/15 text-[#327A54]'}`}>
              <CheckCircle2 className="w-10 h-10" />
            </div>
            <h3 className="text-[24px] font-bold mb-1">{activeRoutine.name} done</h3>
            <p className={`text-[13px] max-w-[240px] mb-6 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
              All {totalSteps} steps completed. Consistency streak maintained!
            </p>
            {confettiActive && (
              <div className="absolute inset-0 pointer-events-none flex items-center justify-center">
                <div className="text-3xl animate-bounce">✨ 🌿 🏆 ✨</div>
              </div>
            )}
            <button
              onClick={exitRunner}
              className={`w-full py-3.5 rounded-full font-medium text-[15px] shadow-lg transition-transform active:scale-95 ${
                isDarkTheme ? 'bg-[#FFFFFF] text-[#0A0908]' : 'bg-[#1A1614] text-[#FFFFFF]'
              }`}
            >
              Done & Return
            </button>
          </div>
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center text-center px-2">
            <span className={`text-[12px] uppercase tracking-wider mb-2 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
              {currentStep?.type === 'timed' ? 'Timed Interval' : currentStep?.type === 'reps' ? 'Target Repetitions' : 'Check-off Step'}
            </span>
            <h2 className="text-[22px] font-bold max-w-[260px] leading-snug mb-4">
              {currentStep?.title}
            </h2>

            {currentStep?.type === 'timed' && (
              <div className="my-4">
                <div className="text-[64px] font-bold leading-none tnum tracking-tight">
                  {formatTime(stepTimerSec)}
                </div>
                <div className={`text-[12px] mt-2 ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`}>
                  {isTimerRunning ? 'Countdown in progress' : 'Paused'}
                </div>
              </div>
            )}

            {currentStep?.type === 'reps' && (
              <div className="my-4">
                <div className="text-[54px] font-bold leading-none tnum">
                  {currentStep.targetReps}
                </div>
                <div className={`text-[13px] mt-1 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                  repetitions to complete
                </div>
              </div>
            )}

            {currentStep?.type === 'checkoff' && (
              <div className="my-4 flex flex-col items-center">
                <div className={`w-16 h-16 rounded-full flex items-center justify-center border-2 mb-2 ${
                  isDarkTheme ? 'border-[#D9A184] text-[#D9A184]' : 'border-[#8F4C2B] text-[#8F4C2B]'
                }`}>
                  <Check className="w-8 h-8 stroke-[2.5]" />
                </div>
                <div className={`text-[13px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                  Tap Done when finished
                </div>
              </div>
            )}

            {currentStepIdx < totalSteps - 1 && (
              <div className={`text-[11px] mt-4 px-3 py-1 rounded-full ${isDarkTheme ? 'bg-[#1C1A17] text-[#A39E98]' : 'bg-[#EFEAE4] text-[#5D5750]'}`}>
                Next: {activeRoutine.steps[currentStepIdx + 1]?.title}
              </div>
            )}
          </div>
        )}

        {/* Controls */}
        {!isCompletedRoutine && (
          <div className="flex items-center gap-3 pb-6">
            {currentStep?.type === 'timed' && (
              <button
                onClick={() => setIsTimerRunning(!isTimerRunning)}
                className={`flex-1 py-3.5 rounded-full font-medium text-[15px] flex items-center justify-center gap-2 transition-transform active:scale-95 ${
                  isDarkTheme ? 'bg-[#D9A184] text-[#0A0908]' : 'bg-[#8F4C2B] text-[#FFFFFF]'
                }`}
              >
                {isTimerRunning ? (
                  <>
                    <Pause className="w-4 h-4 fill-current" />
                    Pause
                  </>
                ) : (
                  <>
                    <Play className="w-4 h-4 fill-current" />
                    Resume
                  </>
                )}
              </button>
            )}

            <button
              onClick={handleStepFinished}
              className={`py-3.5 px-6 rounded-full font-medium text-[15px] flex items-center justify-center gap-2 transition-transform active:scale-95 ${
                currentStep?.type !== 'timed'
                  ? 'flex-1 ' + (isDarkTheme ? 'bg-[#D9A184] text-[#0A0908]' : 'bg-[#8F4C2B] text-[#FFFFFF]')
                  : (isDarkTheme ? 'bg-[#1C1A17] text-[#F5F2EF] hover:bg-[#24211E]' : 'bg-[#FFFFFF] text-[#1A1614] border border-[#211A1614]')
              }`}
            >
              {currentStepIdx < totalSteps - 1 ? (
                <>
                  Next
                  <SkipForward className="w-4 h-4" />
                </>
              ) : (
                'Finish'
              )}
            </button>
          </div>
        )}
      </div>
    );
  }

  // 2. MAIN DASHBOARD VIEW
  return (
    <div className={`relative h-full overflow-y-auto reflex-scrollbar px-4 pt-6 pb-28 ${isDarkTheme ? 'text-[#F5F2EF]' : 'text-[#1A1614]'}`}>
      {/* Top Header: Good afternoon Bobby + Presets + Settings */}
      <div className="flex items-start justify-between mb-4">
        <div>
          <span className={`text-[13px] block leading-snug ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
            Good afternoon
          </span>
          <h1 className="text-[30px] font-bold leading-tight tracking-tight font-serif">
            Bobby
          </h1>
          <span className={`text-[12px] block mt-0.5 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
            Monday, October 5
          </span>
        </div>

        <div className="flex items-center gap-1.5 pt-2">
          <button
            onClick={() => setShowTemplateModal(true)}
            className={`px-3 py-1.5 rounded-full text-[12px] font-semibold flex items-center gap-1 transition-transform active:scale-95 ${
              isDarkTheme ? 'bg-[#2A1C16] text-[#D9A184] border border-[#D9A184]/40' : 'bg-[#F7EBE3] text-[#8F4C2B] border border-[#8F4C2B]/30'
            }`}
            title="Browse Routine Presets"
          >
            <Sparkles className="w-3.5 h-3.5" />
            <span>Presets</span>
          </button>
          <button
            onClick={onOpenSettings}
            className={`w-[34px] h-[34px] rounded-full flex items-center justify-center transition-colors ${
              isDarkTheme ? 'bg-[#1C1A17] text-[#A39E98]' : 'bg-[#EFEAE4] text-[#5D5750]'
            }`}
            title="Settings"
          >
            <SettingsIcon className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Overview Card: Concentric 3-Ring Chart */}
      <div
        className={`p-4 rounded-[28px] mb-3 border flex items-center justify-between ${
          isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-xs'
        }`}
      >
        {/* SVG Concentric Rings */}
        <div className="relative w-[112px] h-[112px] flex items-center justify-center flex-shrink-0">
          <svg className="w-full h-full -rotate-90" viewBox="0 0 100 100">
            <circle cx="50" cy="50" r="42" fill="none" stroke={isDarkTheme ? '#24211E' : '#EFEAE4'} strokeWidth="5" />
            <circle cx="50" cy="50" r="34" fill="none" stroke={isDarkTheme ? '#24211E' : '#EFEAE4'} strokeWidth="5" />
            <circle cx="50" cy="50" r="26" fill="none" stroke={isDarkTheme ? '#24211E' : '#EFEAE4'} strokeWidth="5" />

            <circle
              cx="50" cy="50" r="42"
              fill="none"
              stroke={isDarkTheme ? '#4ADE80' : '#16A34A'}
              strokeWidth="5"
              strokeDasharray={2 * Math.PI * 42}
              strokeDashoffset={2 * Math.PI * 42 * (1 - 0.25)}
              strokeLinecap="round"
            />
            <circle
              cx="50" cy="50" r="34"
              fill="none"
              stroke={isDarkTheme ? '#D9A184' : '#8F4C2B'}
              strokeWidth="5"
              strokeDasharray={2 * Math.PI * 34}
              strokeDashoffset={2 * Math.PI * 34 * (1 - 0.285)}
              strokeLinecap="round"
            />
            <circle
              cx="50" cy="50" r="26"
              fill="none"
              stroke={isDarkTheme ? '#E3C27A' : '#D97706'}
              strokeWidth="5"
              strokeDasharray={2 * Math.PI * 26}
              strokeDashoffset={2 * Math.PI * 26 * (1 - 0.8)}
              strokeLinecap="round"
            />
          </svg>

          <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
            <span className="text-[19px] font-bold leading-none tnum">44%</span>
            <span className={`text-[10px] mt-0.5 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
              of today
            </span>
          </div>
        </div>

        <div className="space-y-2.5 pr-2">
          <div className="flex items-center gap-2">
            <span className={`w-2.5 h-2.5 rounded-full ${isDarkTheme ? 'bg-[#4ADE80]' : 'bg-[#16A34A]'}`} />
            <div>
              <span className="text-[14px] font-bold leading-none block">1 of 4</span>
              <span className={`text-[11px] leading-tight block ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                Routines
              </span>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <span className={`w-2.5 h-2.5 rounded-full ${isDarkTheme ? 'bg-[#D9A184]' : 'bg-[#8F4C2B]'}`} />
            <div>
              <span className="text-[14px] font-bold leading-none block">2 of 7</span>
              <span className={`text-[11px] leading-tight block ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                Tasks
              </span>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <span className={`w-2.5 h-2.5 rounded-full ${isDarkTheme ? 'bg-[#E3C27A]' : 'bg-[#D97706]'}`} />
            <div>
              <span className="text-[14px] font-bold leading-none block">4 of 5</span>
              <span className={`text-[11px] leading-tight block ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                Habits
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Routine Presets Quick Launcher Bar */}
      <div className="mb-3">
        <div className="flex items-center justify-between mb-2 px-1">
          <span className="text-[14px] font-bold">Routine Presets</span>
          <button
            onClick={() => setShowTemplateModal(true)}
            className={`text-[12px] font-semibold ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`}
          >
            See all 10 →
          </button>
        </div>

        <div className="grid grid-cols-3 gap-2">
          {ALL_ROUTINE_PRESETS.slice(0, 3).map((preset) => (
            <div
              key={preset.id}
              onClick={() => startPreset(preset)}
              className={`p-2.5 rounded-[18px] border cursor-pointer transition-transform active:scale-95 flex flex-col justify-between ${
                isDarkTheme
                  ? 'bg-[#141211] border-[#2E2A27] hover:border-[#D9A184]'
                  : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] hover:border-[#8F4C2B] shadow-2xs'
              }`}
            >
              <div>
                <span className="text-xl block mb-1">{preset.icon}</span>
                <span className="text-[12px] font-bold block leading-tight truncate">
                  {preset.name}
                </span>
              </div>
              <span className={`text-[10px] mt-1 block ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                {preset.durationLabel}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Up next Card (Active routine card) */}
      <div
        className={`p-4 rounded-[26px] mb-3 border flex items-center justify-between transition-transform ${
          isDarkTheme
            ? 'bg-[#E5B59D]/90 border-[#D9A184]/50 text-[#141211]'
            : 'bg-[#E5B59D]/90 border-[#8F4C2B]/30 text-[#141211]'
        }`}
      >
        <div>
          <span className="text-[11px] font-medium tracking-tight block opacity-85">
            Up next · in 147 min · 17:30
          </span>
          <h3 className="text-[17px] font-bold leading-snug font-serif mt-0.5">
            {activeRoutine.icon} {activeRoutine.name}
          </h3>
          <span className="text-[12px] opacity-80 block">
            {activeRoutine.durationLabel} · {activeRoutine.stepsCount} steps
          </span>
        </div>

        <button
          onClick={startRunner}
          className="px-4 py-2 rounded-full text-[13px] font-bold bg-[#0A0908] text-white flex items-center gap-1.5 shadow-md active:scale-95 transition-transform"
        >
          <Play className="w-3.5 h-3.5 fill-current" />
          <span>Start</span>
        </button>
      </div>

      {/* Two Stat Cards (Focus today & Routine streak) */}
      <div className="grid grid-cols-2 gap-2.5 mb-4">
        <div
          className={`p-3.5 rounded-[24px] border ${
            isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-xs'
          }`}
        >
          <div className="flex items-center gap-1.5 mb-1">
            <Hourglass className={`w-3.5 h-3.5 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`} />
            <span className={`text-[11px] font-medium ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
              Focus today
            </span>
          </div>
          <div className="text-[20px] font-bold tnum font-serif">1h 40m</div>
          <div className={`text-[10px] mt-0.5 mb-2 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
            3 sessions today
          </div>
          <button
            onClick={startRunner}
            className={`w-full py-1.5 rounded-full text-[11px] font-semibold transition-colors ${
              isDarkTheme ? 'bg-[#24211E] text-[#D9A184] hover:bg-[#2A1C16]' : 'bg-[#EFEAE4] text-[#8F4C2B] hover:bg-[#E2DDD5]'
            }`}
          >
            Start 25:00
          </button>
        </div>

        <div
          className={`p-3.5 rounded-[24px] border ${
            isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-xs'
          }`}
        >
          <div className="flex items-center gap-1.5 mb-1">
            <Flame className={`w-3.5 h-3.5 ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`} />
            <span className={`text-[11px] font-medium ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
              Routine streak
            </span>
          </div>
          <div className="text-[20px] font-bold tnum font-serif">14 days</div>
          <div className={`text-[10px] mt-0.5 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
            Best streak: 14 days
          </div>
        </div>
      </div>

      {/* Tasks today Section */}
      <div className="space-y-2 mb-4">
        <div className="flex items-center justify-between px-1 mb-1">
          <div className="flex items-center gap-1.5">
            <span className="text-[15px] font-bold">Tasks today</span>
            <span className={`text-[12px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
              5 left
            </span>
          </div>
          <span className={`text-[12px] font-medium cursor-pointer ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`}>
            See all
          </span>
        </div>

        {/* Task 1 */}
        <div
          onClick={() => toggleTask('task_journal')}
          className={`p-3.5 rounded-[22px] border flex items-center justify-between cursor-pointer transition-all ${
            isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-xs'
          }`}
        >
          <div className="flex items-center gap-3 min-w-0">
            <div className={`w-[22px] h-[22px] rounded-full border flex items-center justify-center flex-shrink-0 ${
              taskStates.task_journal
                ? (isDarkTheme ? 'bg-[#D9A184] border-[#D9A184] text-[#0A0908]' : 'bg-[#8F4C2B] border-[#8F4C2B] text-white')
                : (isDarkTheme ? 'border-[#3E3A36]' : 'border-[rgba(26,22,20,0.2)]')
            }`}>
              {taskStates.task_journal && <Check className="w-3.5 h-3.5 stroke-[3]" />}
            </div>
            <div className="min-w-0">
              <span className={`text-[13px] font-semibold block leading-tight truncate ${taskStates.task_journal ? 'line-through opacity-50' : ''}`}>
                Daily evening journal entry
              </span>
              <span className={`text-[11px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                Due today
              </span>
            </div>
          </div>
          <span className={`text-[11px] font-semibold px-2.5 py-0.5 rounded-full flex-shrink-0 ${
            isDarkTheme ? 'bg-[#2A1C16] text-[#D9A184]' : 'bg-[#F7EBE3] text-[#8F4C2B]'
          }`}>
            Medium
          </span>
        </div>

        {/* Task 2 */}
        <div
          onClick={() => toggleTask('task_prs')}
          className={`p-3.5 rounded-[22px] border flex items-center justify-between cursor-pointer transition-all ${
            isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-xs'
          }`}
        >
          <div className="flex items-center gap-3 min-w-0">
            <div className={`w-[22px] h-[22px] rounded-full border-2 flex items-center justify-center flex-shrink-0 ${
              taskStates.task_prs
                ? 'bg-[#E05D5D] border-[#E05D5D] text-white'
                : 'border-[#E05D5D]'
            }`}>
              {taskStates.task_prs && <Check className="w-3.5 h-3.5 stroke-[3]" />}
            </div>
            <div className="min-w-0">
              <span className={`text-[13px] font-semibold block leading-tight truncate ${taskStates.task_prs ? 'line-through opacity-50' : ''}`}>
                Review and merge mobile redesign PRs
              </span>
              <span className={`text-[11px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                11:00 am
              </span>
            </div>
          </div>
          <span className={`text-[11px] font-semibold px-2.5 py-0.5 rounded-full flex-shrink-0 ${
            isDarkTheme ? 'bg-[#2C1414] text-[#E05D5D]' : 'bg-[#FEE2E2] text-[#C0504D]'
          }`}>
            High
          </span>
        </div>
      </div>

      {/* Preset Picker Modal Overlay */}
      {showTemplateModal && (
        <div className="absolute inset-0 bg-black/65 z-50 flex flex-col justify-end backdrop-blur-xs animate-in fade-in duration-200">
          <div
            className={`p-4 rounded-t-[28px] max-h-[82%] overflow-y-auto reflex-scrollbar border-t ${
              isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-2xl'
            }`}
          >
            <div className="flex items-center justify-between pb-3 border-b border-neutral-700/15 mb-3">
              <div>
                <h3 className="text-[17px] font-bold">Routine Presets</h3>
                <span className={`text-[11px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                  Choose a template to start immediately
                </span>
              </div>
              <button
                onClick={() => setShowTemplateModal(false)}
                className={`p-1.5 rounded-full ${isDarkTheme ? 'bg-[#1C1A17] text-[#A39E98]' : 'bg-[#EFEAE4] text-[#5D5750]'}`}
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-2.5">
              {ALL_ROUTINE_PRESETS.map((preset) => (
                <div
                  key={preset.id}
                  onClick={() => startPreset(preset)}
                  className={`p-3 rounded-[20px] border cursor-pointer transition-all flex items-center justify-between ${
                    isDarkTheme
                      ? 'bg-[#1C1A17] border-[#2E2A27] hover:border-[#D9A184]'
                      : 'bg-[#F9F7F5] border-[rgba(26,22,20,0.1)] hover:border-[#8F4C2B]'
                  }`}
                >
                  <div className="flex items-center gap-3 min-w-0 pr-2">
                    <span className="text-2xl">{preset.icon}</span>
                    <div className="min-w-0">
                      <span className="text-[13px] font-bold block truncate">{preset.name}</span>
                      <span className={`text-[11px] block line-clamp-1 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                        {preset.description}
                      </span>
                    </div>
                  </div>
                  <div className="text-right flex-shrink-0">
                    <span className={`text-[11px] font-medium block ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`}>
                      {preset.durationLabel}
                    </span>
                    <span className={`text-[10px] opacity-75 block`}>
                      {preset.stepsCount} steps
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
