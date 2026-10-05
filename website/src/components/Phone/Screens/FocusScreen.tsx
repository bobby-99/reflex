import React, { useState, useEffect } from 'react';
import { FocusMode } from '../../../types';
import { LiquidTimerCanvas } from '../../LiquidTimerCanvas';
import { Settings as SettingsIcon, Ban, RotateCcw, Pause, Play, X, Hourglass, Waves, ChevronRight, Plus, Minus } from 'lucide-react';

interface FocusScreenProps {
  isDarkTheme: boolean;
  onOpenSettings: () => void;
  autoStartFocus?: boolean;
}

const CALM_QUOTES = [
  'Avoiding still spends the time.',
  'Deep work is the ability to focus without distraction.',
  'Momentum is built in silence.',
  'One breath, one sentence, one task.',
  'Calm is a superpower in a noisy world.',
];

const POMODORO_PRESETS = [
  { name: 'Standard Pomodoro', label: '25/5m', focus: 25, short: 5, long: 15, cycles: 4 },
  { name: 'Deep Focus', label: '50/10m', focus: 50, short: 10, long: 20, cycles: 2 },
  { name: 'Quick Sprint', label: '15/3m', focus: 15, short: 3, long: 10, cycles: 3 },
];

const FLOW_DURATION_CHIPS = [15, 30, 45, 60, 90];

export const FocusScreen: React.FC<FocusScreenProps> = ({
  isDarkTheme,
  onOpenSettings,
  autoStartFocus,
}) => {
  const [mode, setMode] = useState<FocusMode>('pomodoro');
  const [flowSubMode, setFlowSubMode] = useState<'timed' | 'open'>('timed');
  const [flowDurationMin, setFlowDurationMin] = useState<number>(30);

  // Pomodoro config state
  const [selectedPresetIdx, setSelectedPresetIdx] = useState<number>(0);
  const [focusLengthMin, setFocusLengthMin] = useState<number>(25);
  const [shortBreakMin, setShortBreakMin] = useState<number>(5);
  const [longBreakMin, setLongBreakMin] = useState<number>(15);
  const [cycles, setCycles] = useState<number>(4);

  // Running Session State
  const [isRunningSession, setIsRunningSession] = useState<boolean>(false);
  const [currentSessionSec, setCurrentSessionSec] = useState<number>(25 * 60);
  const [totalSessionSec, setTotalSessionSec] = useState<number>(25 * 60);
  const [isPaused, setIsPaused] = useState<boolean>(false);
  const [currentCycle, setCurrentCycle] = useState<number>(1);
  const [currentPhase, setCurrentPhase] = useState<'work' | 'break'>('work');
  const [speedMultiplier, setSpeedMultiplier] = useState<number>(1);
  const [quoteIdx, setQuoteIdx] = useState<number>(0);

  // App blocker preview modal
  const [showBlockerPreview, setShowBlockerPreview] = useState<boolean>(false);

  useEffect(() => {
    if (autoStartFocus && !isRunningSession) {
      startSession();
    }
  }, [autoStartFocus]);

  // Session countdown / countup timer effect
  useEffect(() => {
    let interval: NodeJS.Timeout | null = null;
    if (isRunningSession && !isPaused) {
      interval = setInterval(() => {
        if (mode === 'timed_flow' && flowSubMode === 'open') {
          // Open flow counts upward
          setCurrentSessionSec((prev) => prev + speedMultiplier);
        } else {
          // Timed sessions count downward
          setCurrentSessionSec((prev) => {
            const next = prev - speedMultiplier;
            if (next <= 0) {
              handlePhaseTransition();
              return 0;
            }
            return next;
          });
        }
      }, 1000);
    }
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [isRunningSession, isPaused, currentSessionSec, speedMultiplier, mode, flowSubMode]);

  // Rotate quotes every 12 seconds during running session
  useEffect(() => {
    let quoteInterval: NodeJS.Timeout | null = null;
    if (isRunningSession && !isPaused) {
      quoteInterval = setInterval(() => {
        setQuoteIdx((prev) => (prev + 1) % CALM_QUOTES.length);
      }, 12000);
    }
    return () => {
      if (quoteInterval) clearInterval(quoteInterval);
    };
  }, [isRunningSession, isPaused]);

  const selectPomodoroPreset = (idx: number) => {
    setSelectedPresetIdx(idx);
    const p = POMODORO_PRESETS[idx];
    setFocusLengthMin(p.focus);
    setShortBreakMin(p.short);
    setLongBreakMin(p.long);
    setCycles(p.cycles);
  };

  const startSession = () => {
    const isFlow = mode === 'timed_flow';
    const targetMin = isFlow ? (flowSubMode === 'timed' ? flowDurationMin : 0) : focusLengthMin;
    const sec = targetMin * 60;
    setCurrentSessionSec(isFlow && flowSubMode === 'open' ? 0 : sec);
    setTotalSessionSec(isFlow && flowSubMode === 'open' ? 3600 : sec);
    setCurrentPhase('work');
    setCurrentCycle(1);
    setIsPaused(false);
    setIsRunningSession(true);
  };

  const handlePhaseTransition = () => {
    if (mode === 'timed_flow') {
      stopSession();
      return;
    }

    if (currentPhase === 'work') {
      const isLong = currentCycle % cycles === 0;
      const breakSec = (isLong ? longBreakMin : shortBreakMin) * 60;
      setCurrentPhase('break');
      setCurrentSessionSec(breakSec);
      setTotalSessionSec(breakSec);
    } else {
      if (currentCycle < cycles) {
        setCurrentCycle((prev) => prev + 1);
        const workSec = focusLengthMin * 60;
        setCurrentPhase('work');
        setCurrentSessionSec(workSec);
        setTotalSessionSec(workSec);
      } else {
        stopSession();
      }
    }
  };

  const stopSession = () => {
    setIsRunningSession(false);
    setIsPaused(false);
  };

  const formatTimer = (sec: number) => {
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m < 10 ? '0' : ''}${m}:${s < 10 ? '0' : ''}${s}`;
  };

  const fillRatio =
    mode === 'timed_flow' && flowSubMode === 'open'
      ? Math.min(0.9, 0.15 + (currentSessionSec / 3600) * 0.75)
      : totalSessionSec > 0
      ? currentSessionSec / totalSessionSec
      : 0.8;

  // 1. RUNNING FOCUS IMMERSION VIEW (Matching 4_timer.png)
  if (isRunningSession) {
    const isFlow = mode === 'timed_flow';
    return (
      <div className={`relative h-full flex flex-col justify-between p-6 overflow-hidden select-none ${
        isDarkTheme ? 'bg-[#0A0908] text-[#F5F2EF]' : 'bg-[#F7F3EE] text-[#1A1614]'
      }`}>
        {/* Top Tag & Speed Multiplier */}
        <div className="flex items-center justify-between pt-6">
          <div className={`px-3.5 py-1.5 rounded-full text-[12px] font-medium border ${
            isDarkTheme ? 'bg-[#2A1C16] border-[#D9A184]/40 text-[#D9A184]' : 'bg-[#F7EBE3] border-[#8F4C2B]/30 text-[#8F4C2B]'
          }`}>
            {isFlow ? (flowSubMode === 'timed' ? 'Timed flow' : 'Open flow') : 'Pomodoro'}
          </div>

          <button
            onClick={() => setSpeedMultiplier((prev) => (prev === 1 ? 10 : prev === 10 ? 60 : 1))}
            className={`text-[11px] px-2.5 py-1 rounded-full font-bold tnum border ${
              isDarkTheme ? 'bg-[#1C1A17] border-[#2E2A27] text-[#A39E98]' : 'bg-[#EFEAE4] border-[rgba(26,22,20,0.13)] text-[#5D5750]'
            }`}
            title="Fast-forward demonstration speed"
          >
            {speedMultiplier}x speed
          </button>
        </div>

        {/* Center Liquid Wave Timer Canvas with Flow Harmonic Currents */}
        <div className="flex-1 flex flex-col items-center justify-center text-center my-4">
          <LiquidTimerCanvas
            fillRatio={fillRatio}
            phaseLabel={isFlow ? (flowSubMode === 'timed' ? 'Timed flow' : 'Open flow') : 'Focus'}
            timeReadout={formatTimer(currentSessionSec)}
            statusCaption={isPaused ? 'Paused' : 'Running'}
            isDarkTheme={isDarkTheme}
            isRunning={!isPaused}
            size={230}
            variant={isFlow ? 'flow' : 'pomodoro'}
          />

          {/* Calm Quote */}
          <p className={`text-[13px] italic max-w-[260px] mt-8 transition-opacity duration-500 font-serif ${
            isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'
          }`}>
            "{CALM_QUOTES[quoteIdx]}"
          </p>
        </div>

        {/* Bottom Controls (End & Pause matching 4_timer.png) */}
        <div className="flex items-center gap-3 pb-6">
          <button
            onClick={stopSession}
            className={`flex-1 py-3.5 rounded-full font-medium text-[15px] transition-transform active:scale-95 ${
              isDarkTheme ? 'bg-[#1C1A17] text-[#F5F2EF] hover:bg-[#24211E]' : 'bg-[#EFEAE4] text-[#1A1614] hover:bg-[#E2DDD5]'
            }`}
          >
            End
          </button>

          <button
            onClick={() => setIsPaused(!isPaused)}
            className={`flex-1 py-3.5 rounded-full font-semibold text-[15px] flex items-center justify-center gap-2 transition-transform active:scale-95 shadow-md ${
              isDarkTheme ? 'bg-[#FFFFFF] text-[#0A0908]' : 'bg-[#1A1614] text-[#FFFFFF]'
            }`}
          >
            {isPaused ? 'Resume' : 'Pause'}
          </button>
        </div>
      </div>
    );
  }

  // 2. FOCUS SETUP SCREEN
  return (
    <div className={`h-full overflow-y-auto reflex-scrollbar px-4 pt-6 pb-28 ${isDarkTheme ? 'text-[#F5F2EF]' : 'text-[#1A1614]'}`}>
      {/* Top Header */}
      <div className="flex items-center justify-between mb-3">
        <h1 className="text-[28px] font-bold leading-tight font-serif">Focus</h1>
        <div className="flex items-center gap-2">
          {/* App Blocker Trigger */}
          <button
            onClick={() => setShowBlockerPreview(true)}
            className={`w-[36px] h-[36px] rounded-full flex items-center justify-center transition-colors ${
              isDarkTheme ? 'bg-[#1C1A17] text-[#A39E98]' : 'bg-[#EFEAE4] text-[#5D5750]'
            }`}
            title="App Blocker Barrier"
          >
            <Ban className="w-4 h-4" />
          </button>
          <button
            onClick={() => {}}
            className={`w-[36px] h-[36px] rounded-full flex items-center justify-center transition-colors ${
              isDarkTheme ? 'bg-[#1C1A17] text-[#A39E98]' : 'bg-[#EFEAE4] text-[#5D5750]'
            }`}
            title="History"
          >
            <RotateCcw className="w-4 h-4" />
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

      {/* Main Segmented Mode Selector: Pomodoro | Flow */}
      <div className={`p-1 rounded-full flex mb-3 ${isDarkTheme ? 'bg-[#141211] border border-[#2E2A27]' : 'bg-[#EFEAE4]'}`}>
        <button
          onClick={() => setMode('pomodoro')}
          className={`flex-1 py-2 rounded-full text-[13px] font-semibold flex items-center justify-center gap-1.5 transition-colors ${
            mode === 'pomodoro'
              ? isDarkTheme ? 'bg-[#D9A184] text-[#0A0908] shadow-sm' : 'bg-[#8F4C2B] text-white shadow-sm'
              : isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'
          }`}
        >
          <Hourglass className="w-3.5 h-3.5" />
          <span>Pomodoro</span>
        </button>
        <button
          onClick={() => setMode('timed_flow')}
          className={`flex-1 py-2 rounded-full text-[13px] font-semibold flex items-center justify-center gap-1.5 transition-colors ${
            mode === 'timed_flow'
              ? isDarkTheme ? 'bg-[#D9A184] text-[#0A0908] shadow-sm' : 'bg-[#8F4C2B] text-white shadow-sm'
              : isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'
          }`}
        >
          <Waves className="w-3.5 h-3.5" />
          <span>Flow</span>
        </button>
      </div>

      {/* ========================================================
          FLOW MODE VIEW (Matching media_1791207200529.png)
         ======================================================== */}
      {mode === 'timed_flow' ? (
        <div>
          {/* Sub-selector: Timed flow vs Open flow */}
          <div className={`p-1 rounded-full flex mb-4 ${isDarkTheme ? 'bg-[#141211] border border-[#2E2A27]' : 'bg-[#EFEAE4]'}`}>
            <button
              onClick={() => setFlowSubMode('timed')}
              className={`flex-1 py-1.5 rounded-full text-[12px] font-semibold transition-colors ${
                flowSubMode === 'timed'
                  ? isDarkTheme ? 'bg-[#D9A184] text-[#0A0908]' : 'bg-[#8F4C2B] text-white'
                  : isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'
              }`}
            >
              Timed flow
            </button>
            <button
              onClick={() => setFlowSubMode('open')}
              className={`flex-1 py-1.5 rounded-full text-[12px] font-semibold transition-colors ${
                flowSubMode === 'open'
                  ? isDarkTheme ? 'bg-[#D9A184] text-[#0A0908]' : 'bg-[#8F4C2B] text-white'
                  : isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'
              }`}
            >
              Open flow
            </button>
          </div>

          {/* Hero Fluid Flow Animation Canvas */}
          <div className="flex flex-col items-center justify-center my-3">
            <LiquidTimerCanvas
              fillRatio={0.70}
              phaseLabel={flowSubMode === 'timed' ? 'Timed flow' : 'Open flow'}
              timeReadout={flowSubMode === 'timed' ? `${flowDurationMin}:00` : '00:00'}
              statusCaption="Ready"
              isDarkTheme={isDarkTheme}
              isRunning={true}
              size={188}
              variant="flow"
            />
            <p className={`text-[12px] mt-3 mb-1 text-center font-serif ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
              {flowSubMode === 'timed' ? 'One uninterrupted block, no breaks' : 'Open stopwatch flow with no timer cutoff'}
            </p>
          </div>

          {/* Stepper Card for Flow Duration (matching screenshot) */}
          {flowSubMode === 'timed' && (
            <div
              className={`p-4 rounded-[26px] mb-3 border ${
                isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-xs'
              }`}
            >
              <div className="flex items-center justify-between mb-3">
                <span className="text-[14px] font-semibold">Flow duration</span>
                <div className="flex items-center gap-2.5">
                  <button
                    onClick={() => setFlowDurationMin(Math.max(5, flowDurationMin - 5))}
                    className={`w-8 h-8 rounded-full flex items-center justify-center font-bold ${
                      isDarkTheme ? 'bg-[#24211E] text-[#F5F2EF]' : 'bg-[#EFEAE4] text-[#1A1614]'
                    }`}
                  >
                    <Minus className="w-3.5 h-3.5" />
                  </button>
                  <span className="text-[14px] font-bold min-w-[54px] text-center tnum">{flowDurationMin} min</span>
                  <button
                    onClick={() => setFlowDurationMin(Math.min(180, flowDurationMin + 5))}
                    className={`w-8 h-8 rounded-full flex items-center justify-center font-bold ${
                      isDarkTheme ? 'bg-[#24211E] text-[#F5F2EF]' : 'bg-[#EFEAE4] text-[#1A1614]'
                    }`}
                  >
                    <Plus className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>

              {/* Quick duration chips row [15m, 30m, 45m, 60m, 90m] */}
              <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar">
                {FLOW_DURATION_CHIPS.map((mins) => (
                  <button
                    key={mins}
                    onClick={() => setFlowDurationMin(mins)}
                    className={`flex-1 py-1 rounded-xl text-[12px] font-medium transition-colors ${
                      flowDurationMin === mins
                        ? isDarkTheme ? 'bg-[#2A1C16] text-[#D9A184] font-bold border border-[#D9A184]/40' : 'bg-[#F7EBE3] text-[#8F4C2B] font-bold border border-[#8F4C2B]/30'
                        : isDarkTheme ? 'bg-[#1C1A17] text-[#A39E98]' : 'bg-[#EFEAE4] text-[#5D5750]'
                    }`}
                  >
                    {mins}m
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Link a task or tag row */}
          <div
            className={`p-3.5 rounded-[22px] mb-4 border flex items-center justify-between ${
              isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-xs'
            }`}
          >
            <div className="flex items-center gap-3">
              <div className={`w-8 h-8 rounded-full flex items-center justify-center ${isDarkTheme ? 'bg-[#24211E] text-[#D9A184]' : 'bg-[#F7EBE3] text-[#8F4C2B]'}`}>
                ≡
              </div>
              <div>
                <span className="text-[13px] font-semibold block">Link a task or tag</span>
                <span className={`text-[11px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                  Optional, tap to choose
                </span>
              </div>
            </div>
            <ChevronRight className="w-4 h-4 opacity-50" />
          </div>

          {/* CTA Pill button: Start timed flow */}
          <button
            onClick={startSession}
            className={`w-full py-4 rounded-full font-bold text-[15px] shadow-lg transition-transform active:scale-95 mb-4 ${
              isDarkTheme
                ? 'bg-[#FFFFFF] text-[#0A0908] hover:bg-neutral-100'
                : 'bg-[#1A1614] text-[#FFFFFF] hover:bg-neutral-800'
            }`}
          >
            {flowSubMode === 'timed' ? `Start timed flow (${flowDurationMin}m)` : 'Start open flow'}
          </button>
        </div>
      ) : (
        /* ========================================================
            POMODORO MODE VIEW (Matching 3_focus.png)
           ======================================================== */
        <div>
          {/* Presets Row */}
          <div className="flex items-center justify-between text-[11px] mb-2 px-1">
            <span className={`font-medium ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>Presets</span>
            <span className={`font-semibold cursor-pointer ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`}>
              ✎ Edit / Manage
            </span>
          </div>

          <div className="flex items-center gap-2 overflow-x-auto no-scrollbar pb-3 mb-2">
            {POMODORO_PRESETS.map((p, i) => (
              <button
                key={p.name}
                onClick={() => selectPomodoroPreset(i)}
                className={`px-3 py-1.5 rounded-full text-[12px] whitespace-nowrap font-medium border transition-colors ${
                  selectedPresetIdx === i
                    ? isDarkTheme
                      ? 'bg-[#2A1C16] border-[#D9A184] text-[#D9A184]'
                      : 'bg-[#F7EBE3] border-[#8F4C2B] text-[#8F4C2B]'
                    : isDarkTheme
                    ? 'bg-[#141211] border-[#2E2A27] text-[#A39E98]'
                    : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] text-[#5D5750]'
                }`}
              >
                {p.name} <span className="opacity-70 text-[10px]">{p.label}</span>
              </button>
            ))}
          </div>

          {/* Hero Circular Ready Display */}
          <div className="flex flex-col items-center justify-center my-3">
            <div
              className={`w-[188px] h-[188px] rounded-full flex flex-col items-center justify-center shadow-lg transition-transform ${
                isDarkTheme ? 'bg-[#D9A184] text-[#0A0908]' : 'bg-[#8F4C2B] text-white'
              }`}
            >
              <span className="text-[13px] font-medium opacity-85">Focus</span>
              <span className="text-[44px] font-bold leading-none my-1 font-serif tnum">
                {focusLengthMin}:00
              </span>
              <span className="text-[12px] font-medium opacity-85">Ready</span>
            </div>

            {/* Cycle Dash Segments (4 copper, 3 sage green) */}
            <div className="w-full max-w-[280px] my-3">
              <div className="flex items-center gap-1.5 h-[6px]">
                {Array.from({ length: cycles }).map((_, i) => (
                  <React.Fragment key={i}>
                    <div
                      className={`flex-1 h-full rounded-full ${
                        isDarkTheme ? 'bg-[#D9A184]' : 'bg-[#8F4C2B]'
                      }`}
                    />
                    {i < cycles - 1 && (
                      <div
                        className={`w-3 h-full rounded-full ${
                          isDarkTheme ? 'bg-[#86C9A4]' : 'bg-[#327A54]'
                        }`}
                      />
                    )}
                  </React.Fragment>
                ))}
                <div
                  className={`w-6 h-full rounded-full ${
                    isDarkTheme ? 'bg-[#86C9A4]' : 'bg-[#327A54]'
                  }`}
                />
              </div>
              <p className={`text-center text-[10.5px] mt-1.5 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                {cycles} × {focusLengthMin} min focus ({shortBreakMin}m short · {longBreakMin}m long) · 2h 10m per cycle
              </p>
            </div>
          </div>

          {/* Steppers Card for Pomodoro */}
          <div
            className={`p-4 rounded-[28px] mb-4 border divide-y ${
              isDarkTheme ? 'bg-[#141211] border-[#2E2A27] divide-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] divide-[#F3EEE8] shadow-xs'
            }`}
          >
            {/* Focus length */}
            <div className="py-2 flex items-center justify-between">
              <div>
                <span className="text-[14px] font-semibold block">Focus length</span>
              </div>
              <div className="flex items-center gap-2.5">
                <button
                  onClick={() => setFocusLengthMin(Math.max(5, focusLengthMin - 5))}
                  className={`w-8 h-8 rounded-full flex items-center justify-center font-bold ${
                    isDarkTheme ? 'bg-[#24211E] text-[#F5F2EF]' : 'bg-[#EFEAE4] text-[#1A1614]'
                  }`}
                >
                  -
                </button>
                <span className="text-[14px] font-bold min-w-[50px] text-center tnum">{focusLengthMin} min</span>
                <button
                  onClick={() => setFocusLengthMin(Math.min(120, focusLengthMin + 5))}
                  className={`w-8 h-8 rounded-full flex items-center justify-center font-bold ${
                    isDarkTheme ? 'bg-[#24211E] text-[#F5F2EF]' : 'bg-[#EFEAE4] text-[#1A1614]'
                  }`}
                >
                  +
                </button>
              </div>
            </div>

            {/* Short break */}
            <div className="py-2 flex items-center justify-between">
              <div>
                <span className="text-[14px] font-semibold block">Short break</span>
                <span className={`text-[10px] block ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                  Tap value or decrement to 0 to remove
                </span>
              </div>
              <div className="flex items-center gap-2.5">
                <button
                  onClick={() => setShortBreakMin(Math.max(0, shortBreakMin - 1))}
                  className={`w-8 h-8 rounded-full flex items-center justify-center font-bold ${
                    isDarkTheme ? 'bg-[#24211E] text-[#F5F2EF]' : 'bg-[#EFEAE4] text-[#1A1614]'
                  }`}
                >
                  -
                </button>
                <span className="text-[14px] font-bold min-w-[50px] text-center tnum">{shortBreakMin} min</span>
                <button
                  onClick={() => setShortBreakMin(Math.min(30, shortBreakMin + 1))}
                  className={`w-8 h-8 rounded-full flex items-center justify-center font-bold ${
                    isDarkTheme ? 'bg-[#24211E] text-[#F5F2EF]' : 'bg-[#EFEAE4] text-[#1A1614]'
                  }`}
                >
                  +
                </button>
              </div>
            </div>

            {/* Long break */}
            <div className="py-2 flex items-center justify-between">
              <div>
                <span className="text-[14px] font-semibold block">Long break</span>
                <span className={`text-[10px] block ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                  Tap value or decrement to 0 to remove
                </span>
              </div>
              <div className="flex items-center gap-2.5">
                <button
                  onClick={() => setLongBreakMin(Math.max(5, longBreakMin - 5))}
                  className={`w-8 h-8 rounded-full flex items-center justify-center font-bold ${
                    isDarkTheme ? 'bg-[#24211E] text-[#F5F2EF]' : 'bg-[#EFEAE4] text-[#1A1614]'
                  }`}
                >
                  -
                </button>
                <span className="text-[14px] font-bold min-w-[50px] text-center tnum">{longBreakMin} min</span>
                <button
                  onClick={() => setLongBreakMin(Math.min(60, longBreakMin + 5))}
                  className={`w-8 h-8 rounded-full flex items-center justify-center font-bold ${
                    isDarkTheme ? 'bg-[#24211E] text-[#F5F2EF]' : 'bg-[#EFEAE4] text-[#1A1614]'
                  }`}
                >
                  +
                </button>
              </div>
            </div>

            {/* Sessions per cycle */}
            <div className="py-2 flex items-center justify-between">
              <div>
                <span className="text-[14px] font-semibold block">Sessions per cycle</span>
              </div>
              <div className="flex items-center gap-2.5">
                <button
                  onClick={() => setCycles(Math.max(1, cycles - 1))}
                  className={`w-8 h-8 rounded-full flex items-center justify-center font-bold ${
                    isDarkTheme ? 'bg-[#24211E] text-[#F5F2EF]' : 'bg-[#EFEAE4] text-[#1A1614]'
                  }`}
                >
                  -
                </button>
                <span className="text-[14px] font-bold min-w-[50px] text-center tnum">{cycles}</span>
                <button
                  onClick={() => setCycles(Math.min(8, cycles + 1))}
                  className={`w-8 h-8 rounded-full flex items-center justify-center font-bold ${
                    isDarkTheme ? 'bg-[#24211E] text-[#F5F2EF]' : 'bg-[#EFEAE4] text-[#1A1614]'
                  }`}
                >
                  +
                </button>
              </div>
            </div>
          </div>

          {/* Primary White Action Pill Button */}
          <button
            onClick={startSession}
            className={`w-full py-4 rounded-full font-bold text-[15px] shadow-lg transition-transform active:scale-95 mb-4 ${
              isDarkTheme
                ? 'bg-[#FFFFFF] text-[#0A0908] hover:bg-neutral-100'
                : 'bg-[#1A1614] text-[#FFFFFF] hover:bg-neutral-800'
            }`}
          >
            Start pomodoro session
          </button>
        </div>
      )}

      {/* Distraction Shield Barrier Modal */}
      {showBlockerPreview && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in">
          <div className={`w-full max-w-[320px] p-6 rounded-[32px] border text-center relative ${
            isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-2xl'
          }`}>
            <button
              onClick={() => setShowBlockerPreview(false)}
              className="absolute top-4 right-4 p-1 rounded-full opacity-60 hover:opacity-100"
            >
              <X className="w-4 h-4" />
            </button>
            <div className={`w-12 h-12 rounded-full flex items-center justify-center mx-auto mb-3 ${
              isDarkTheme ? 'bg-[#2A1C16] text-[#D9A184]' : 'bg-[#F7EBE3] text-[#8F4C2B]'
            }`}>
              <Ban className="w-6 h-6" />
            </div>
            <h3 className="text-[17px] font-bold font-serif mb-1">Focus Shield Active</h3>
            <p className={`text-[12px] leading-relaxed mb-4 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
              Reflex blocks social media & games on your device during focus sessions via local <code>UsageStatsManager</code>.
            </p>
            <button
              onClick={() => setShowBlockerPreview(false)}
              className={`w-full py-2.5 rounded-full text-[13px] font-semibold ${
                isDarkTheme ? 'bg-[#D9A184] text-[#0A0908]' : 'bg-[#8F4C2B] text-white'
              }`}
            >
              Return to Calm
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
