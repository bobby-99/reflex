import React, { useState, useEffect } from 'react';
import { TaskItem, ParsedTokens } from '../../../types';
import { parseNaturalLanguageTask } from '../../../utils/nlpParser';
import { Plus, Check, RotateCcw, Mic, Settings as SettingsIcon, ChevronDown, ChevronUp, Repeat, X, SlidersHorizontal, Sparkles } from 'lucide-react';

interface TasksScreenProps {
  isDarkTheme: boolean;
  onOpenSettings: () => void;
  presetQuickAddText?: string | null;
  onClearPresetQuickAdd?: () => void;
}

const INITIAL_TASKS: TaskItem[] = [
  {
    id: 'task_prs',
    title: 'Review and merge mobile redesign PRs',
    dueDateLabel: 'Today',
    dueTimeLabel: '11:00 am',
    priority: 'HIGH',
    isCompleted: false,
    category: 'today',
  },
  {
    id: 'task_roadmap',
    title: 'Finalize Q4 roadmap proposal & key deliverables',
    dueDateLabel: 'Today',
    dueTimeLabel: '2:30 pm',
    priority: 'HIGH',
    isCompleted: false,
    category: 'today',
  },
  {
    id: 'task_beans',
    title: 'Pick up cold brew beans & almond milk',
    dueDateLabel: 'Today',
    dueTimeLabel: '5:30 pm',
    priority: 'MEDIUM',
    isCompleted: false,
    category: 'today',
  },
  {
    id: 'task_walk',
    title: '30-min evening recovery walk',
    dueDateLabel: 'Today',
    dueTimeLabel: '7:00 pm',
    priority: 'NONE',
    isCompleted: false,
    category: 'today',
  },
  {
    id: 'task_journal',
    title: 'Daily evening journal entry',
    dueDateLabel: 'Today',
    dueTimeLabel: '',
    priority: 'NONE',
    isCompleted: false,
    category: 'today',
    repeatLabel: 'Every day',
  },
  {
    id: 'task_done_1',
    title: 'Hydrate & morning routine 100%',
    dueDateLabel: 'Today',
    dueTimeLabel: '8:00 am',
    priority: 'LOW',
    isCompleted: true,
    category: 'today',
  },
  {
    id: 'task_done_2',
    title: 'Submit quarterly budget proposal',
    dueDateLabel: 'Today',
    dueTimeLabel: '9:30 am',
    priority: 'MEDIUM',
    isCompleted: true,
    category: 'today',
  },
  {
    id: 'task_up_1',
    title: 'Sync with core design team on dark mode PRs',
    dueDateLabel: 'Tomorrow',
    dueTimeLabel: '10:00 am',
    priority: 'HIGH',
    isCompleted: false,
    category: 'upcoming',
  },
  {
    id: 'task_up_2',
    title: 'Inspect Room SQLite v14 migration logs',
    dueDateLabel: 'Tomorrow',
    dueTimeLabel: '2:00 pm',
    priority: 'MEDIUM',
    isCompleted: false,
    category: 'upcoming',
  },
  {
    id: 'task_up_3',
    title: 'Offline database backup review',
    dueDateLabel: 'Tomorrow',
    dueTimeLabel: '6:00 pm',
    priority: 'LOW',
    isCompleted: false,
    category: 'upcoming',
  },
];

export const TasksScreen: React.FC<TasksScreenProps> = ({
  isDarkTheme,
  onOpenSettings,
  presetQuickAddText,
  onClearPresetQuickAdd,
}) => {
  const [tasks, setTasks] = useState<TaskItem[]>(INITIAL_TASKS);
  const [activeFilter, setActiveFilter] = useState<'all' | 'today' | 'upcoming' | 'nodate' | 'completed'>('all');
  const [completedExpanded, setCompletedExpanded] = useState<boolean>(false);
  const [animatingTaskIds, setAnimatingTaskIds] = useState<Set<string>>(new Set());
  const [undoTask, setUndoTask] = useState<{ task: TaskItem; prevCompleted: boolean } | null>(null);

  // Quick Add State
  const [isQuickAddOpen, setIsQuickAddOpen] = useState<boolean>(false);
  const [quickAddInput, setQuickAddInput] = useState<string>('');
  const [parsedTokens, setParsedTokens] = useState<ParsedTokens>({ cleanTitle: '', rawInput: '' });
  const [isTuneOpen, setIsTuneOpen] = useState<boolean>(false);
  const [isMicListening, setIsMicListening] = useState<boolean>(false);

  // When parent passes a preset quick add text (e.g. from hero suggestion)
  useEffect(() => {
    if (presetQuickAddText) {
      setIsQuickAddOpen(true);
      setQuickAddInput(presetQuickAddText);
      setParsedTokens(parseNaturalLanguageTask(presetQuickAddText));
      if (onClearPresetQuickAdd) onClearPresetQuickAdd();
    }
  }, [presetQuickAddText]);

  // Handle live input NLP parsing
  const handleInputChange = (text: string) => {
    setQuickAddInput(text);
    setParsedTokens(parseNaturalLanguageTask(text));
  };

  // Toggle checkbox with 380ms visual delay before moving
  const toggleTaskCompletion = (task: TaskItem) => {
    const isNowCompleted = !task.isCompleted;

    // Start 380ms strikethrough animation
    setAnimatingTaskIds((prev) => new Set(prev).add(task.id));

    setTimeout(() => {
      setTasks((prev) =>
        prev.map((t) => (t.id === task.id ? { ...t, isCompleted: isNowCompleted } : t))
      );
      setAnimatingTaskIds((prev) => {
        const next = new Set(prev);
        next.delete(task.id);
        return next;
      });

      // Show Undo toast
      setUndoTask({ task, prevCompleted: task.isCompleted });
    }, 380);
  };

  // Undo last toggle
  const handleUndo = () => {
    if (!undoTask) return;
    setTasks((prev) =>
      prev.map((t) => (t.id === undoTask.task.id ? { ...t, isCompleted: undoTask.prevCompleted } : t))
    );
    setUndoTask(null);
  };

  // Auto-dismiss undo toast after 3.5s
  useEffect(() => {
    if (undoTask) {
      const timer = setTimeout(() => {
        setUndoTask(null);
      }, 3500);
      return () => clearTimeout(timer);
    }
  }, [undoTask]);

  // Submit quick add
  const handleQuickAddSubmit = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!quickAddInput.trim()) return;

    const parsed = parsedTokens.cleanTitle ? parsedTokens : parseNaturalLanguageTask(quickAddInput);
    const newTask: TaskItem = {
      id: `task_${Date.now()}`,
      title: parsed.cleanTitle || quickAddInput,
      dueDateLabel: parsed.dueDate || 'Today',
      dueTimeLabel: parsed.dueTime || '6:00 pm',
      priority: parsed.priority || 'NONE',
      isCompleted: false,
      category: parsed.dueDate === 'Tomorrow' ? 'upcoming' : 'today',
      repeatLabel: parsed.repeat,
    };

    setTasks([newTask, ...tasks]);
    setQuickAddInput('');
    setParsedTokens({ cleanTitle: '', rawInput: '' });
    setIsQuickAddOpen(false);
  };

  // Fake voice dictation simulation
  const handleMicToggle = () => {
    if (isMicListening) {
      setIsMicListening(false);
      return;
    }
    setIsMicListening(true);
    setQuickAddInput('Listening...');
    setTimeout(() => {
      const sample = 'Water houseplants tomorrow 9am !!';
      setQuickAddInput(sample);
      setParsedTokens(parseNaturalLanguageTask(sample));
      setIsMicListening(false);
    }, 1500);
  };

  // Filter tasks
  const todayTasks = tasks.filter((t) => t.category === 'today' || t.dueDateLabel === 'Today');
  const doneToday = todayTasks.filter((t) => t.isCompleted).length;
  const totalToday = todayTasks.length;
  const progressPct = totalToday > 0 ? Math.round((doneToday / totalToday) * 100) : 100;

  const filteredTasks = tasks.filter((t) => {
    if (activeFilter === 'today') return (t.category === 'today' || t.dueDateLabel === 'Today') && !t.isCompleted;
    if (activeFilter === 'upcoming') return (t.category === 'upcoming' || t.dueDateLabel === 'Tomorrow') && !t.isCompleted;
    if (activeFilter === 'nodate') return t.category === 'nodate' && !t.isCompleted;
    if (activeFilter === 'completed') return t.isCompleted;
    return true; // 'all'
  });

  const overdueList = filteredTasks.filter((t) => t.category === 'overdue' && !t.isCompleted);
  const todayList = filteredTasks.filter((t) => (t.category === 'today' || t.dueDateLabel === 'Today') && !t.isCompleted);
  const upcomingList = filteredTasks.filter((t) => (t.category === 'upcoming' || t.dueDateLabel === 'Tomorrow') && !t.isCompleted);
  const completedList = tasks.filter((t) => t.isCompleted);

  return (
    <div className={`relative h-full flex flex-col ${isDarkTheme ? 'text-[#F5F2EF]' : 'text-[#1A1614]'}`}>
      {/* Scrollable Tasks Container */}
      <div className="flex-1 overflow-y-auto reflex-scrollbar px-4 pt-8 pb-36">
        {/* Top Header */}
        <div className="flex items-center justify-between mb-2">
          <div>
            <h1 className="text-[28px] font-bold leading-tight font-serif">Tasks</h1>
            <p className={`text-[12px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
              Monday, October 5
            </p>
          </div>
          <button
            onClick={onOpenSettings}
            className={`w-[40px] h-[40px] rounded-full flex items-center justify-center transition-colors ${
              isDarkTheme ? 'bg-[#24211E] text-[#A39E98] hover:text-[#F5F2EF]' : 'bg-[#EFEAE4] text-[#5D5750] hover:text-[#1A1614]'
            }`}
            aria-label="Settings"
          >
            <SettingsIcon className="w-5 h-5" />
          </button>
        </div>

        {/* Progress Card (v3.0 design) */}
        <div
          className={`p-4 rounded-[26px] mb-4 border transition-colors ${
            isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-xs'
          }`}
        >
          <div className="flex items-center justify-between mb-2.5">
            <span className="text-[15px] font-semibold">
              {doneToday} of {totalToday} done today
            </span>
            <span className={`text-[13px] font-medium tnum ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`}>
              {progressPct}%
            </span>
          </div>
          <div className={`w-full h-[8px] rounded-full overflow-hidden ${isDarkTheme ? 'bg-[#24211E]' : 'bg-[#EFEAE4]'}`}>
            <div
              className={`h-full transition-all duration-400 ease-out rounded-full ${
                isDarkTheme ? 'bg-[#D9A184]' : 'bg-[#8F4C2B]'
              }`}
              style={{ width: `${progressPct}%` }}
            />
          </div>
        </div>

        {/* Sticky Filter Chips Row */}
        <div className="flex items-center gap-2 overflow-x-auto no-scrollbar py-2 mb-4 -mx-1 px-1">
          {(
            [
              { id: 'all', label: 'All', count: 10 },
              { id: 'today', label: 'Today', count: 5 },
              { id: 'upcoming', label: 'Upcoming', count: 5 },
              { id: 'nodate', label: 'No date', count: 0 },
              { id: 'completed', label: 'Completed', count: completedList.length },
            ] as const
          ).map((chip) => {
            const isActive = activeFilter === chip.id;
            return (
              <button
                key={chip.id}
                onClick={() => setActiveFilter(chip.id)}
                className={`h-[36px] px-3.5 rounded-full text-[13px] whitespace-nowrap font-medium flex items-center gap-1.5 transition-colors ${
                  isActive
                    ? isDarkTheme ? 'bg-[#D9A184] text-[#0A0908] font-semibold' : 'bg-[#8F4C2B] text-[#FFFFFF] font-semibold'
                    : isDarkTheme ? 'bg-[#24211E] text-[#A39E98] hover:text-[#F5F2EF]' : 'bg-[#EFEAE4] text-[#5D5750] hover:text-[#1A1614]'
                }`}
              >
                <span>{chip.label}</span>
                {chip.count > 0 && (
                  <span className={`text-[11px] tnum ${isActive ? 'opacity-85' : 'opacity-70'}`}>
                    {chip.count}
                  </span>
                )}
              </button>
            );
          })}
        </div>

        {/* Grouped Task Sections */}
        <div className="space-y-5">
          {/* Overdue Section */}
          {overdueList.length > 0 && activeFilter === 'all' && (
            <div>
              <div className="flex items-center justify-between mb-2">
                <span className={`text-[15px] font-semibold ${isDarkTheme ? 'text-[#E05D5D]' : 'text-[#C0504D]'}`}>
                  Overdue
                </span>
                <span className={`text-[12px] ${isDarkTheme ? 'text-[#E05D5D]/80' : 'text-[#C0504D]/80'}`}>
                  needs attention
                </span>
              </div>
              <div className="space-y-2">
                {overdueList.map((task) => (
                  <TaskRowItem
                    key={task.id}
                    task={task}
                    isDarkTheme={isDarkTheme}
                    isAnimating={animatingTaskIds.has(task.id)}
                    onToggle={() => toggleTaskCompletion(task)}
                  />
                ))}
              </div>
            </div>
          )}

          {/* Today Section */}
          {todayList.length > 0 && (activeFilter === 'all' || activeFilter === 'today') && (
            <div>
              <div className="flex items-center justify-between mb-2">
                <span className="text-[15px] font-semibold">Today</span>
                <span className={`text-[12px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                  Mon, Oct 5
                </span>
              </div>
              <div className="space-y-2">
                {todayList.map((task) => (
                  <TaskRowItem
                    key={task.id}
                    task={task}
                    isDarkTheme={isDarkTheme}
                    isAnimating={animatingTaskIds.has(task.id)}
                    onToggle={() => toggleTaskCompletion(task)}
                  />
                ))}
              </div>
            </div>
          )}

          {/* Upcoming Section */}
          {upcomingList.length > 0 && (activeFilter === 'all' || activeFilter === 'upcoming') && (
            <div>
              <div className="flex items-center justify-between mb-2">
                <span className="text-[15px] font-semibold">Upcoming</span>
                <span className={`text-[12px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                  {upcomingList.length} scheduled
                </span>
              </div>
              <div className="space-y-2">
                {upcomingList.map((task) => (
                  <TaskRowItem
                    key={task.id}
                    task={task}
                    isDarkTheme={isDarkTheme}
                    isAnimating={animatingTaskIds.has(task.id)}
                    onToggle={() => toggleTaskCompletion(task)}
                  />
                ))}
              </div>
            </div>
          )}

          {/* Completed Accordion (in 'all' or 'completed' filter) */}
          {completedList.length > 0 && (
            <div className="pt-2">
              <button
                onClick={() => setCompletedExpanded(!completedExpanded)}
                className={`w-full py-2.5 px-3 rounded-2xl flex items-center justify-between transition-colors ${
                  isDarkTheme ? 'hover:bg-[#141211] text-[#A39E98]' : 'hover:bg-[#F3EEE8] text-[#5D5750]'
                }`}
              >
                <span className="text-[14px] font-medium">
                  Completed ({completedList.length})
                </span>
                {completedExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
              </button>

              {completedExpanded && (
                <div className="space-y-2 mt-2">
                  {completedList.map((task) => (
                    <TaskRowItem
                      key={task.id}
                      task={task}
                      isDarkTheme={isDarkTheme}
                      isAnimating={animatingTaskIds.has(task.id)}
                      onToggle={() => toggleTaskCompletion(task)}
                    />
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Floating Undo Toast */}
      {undoTask && (
        <div className="absolute bottom-[114px] left-6 right-6 z-40 animate-in fade-in slide-in-from-bottom-2 duration-200">
          <div
            className={`px-4 py-2.5 rounded-full flex items-center justify-between shadow-xl border ${
              isDarkTheme ? 'bg-[#1C1A17] border-[#2E2A27] text-[#F5F2EF]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] text-[#1A1614]'
            }`}
          >
            <span className="text-[13px] truncate mr-2">Task updated</span>
            <button
              onClick={handleUndo}
              className={`text-[13px] font-semibold flex items-center gap-1.5 px-3 py-1 rounded-full ${
                isDarkTheme ? 'text-[#D9A184] bg-[#2A1C16]' : 'text-[#8F4C2B] bg-[#F7EBE3]'
              }`}
            >
              <RotateCcw className="w-3.5 h-3.5" />
              Undo
            </button>
          </div>
        </div>
      )}

      {/* Dedicated Fixed Round "+" FAB (Floating 44dp above raised nav bar) */}
      {!isQuickAddOpen && (
        <div className="absolute bottom-[108px] right-[24px] z-30 pointer-events-auto">
          <button
            onClick={() => setIsQuickAddOpen(true)}
            className={`w-[58px] h-[58px] rounded-full flex items-center justify-center shadow-xl transition-transform active:scale-90 ${
              isDarkTheme
                ? 'bg-[#D9A184] text-[#0A0908] hover:bg-[#B57E63]'
                : 'bg-[#8F4C2B] text-[#FFFFFF] hover:bg-[#6F3A20]'
            }`}
            style={{
              boxShadow: isDarkTheme ? '0 12px 28px -4px rgba(217, 161, 132, 0.4)' : '0 12px 28px -4px rgba(143, 76, 43, 0.35)',
            }}
            aria-label="Quick add task"
            title="Add task"
          >
            <Plus className="w-[30px] h-[30px] stroke-[2.4]" />
          </button>
        </div>
      )}

      {/* Quick Add Card Modal (Elevated 32dp sheet) */}
      {isQuickAddOpen && (
        <div className="absolute inset-0 z-40 bg-black/60 flex flex-col justify-end p-3 animate-in fade-in">
          <div
            className={`w-full rounded-[32px] p-5 shadow-2xl border transition-colors ${
              isDarkTheme ? 'bg-[#1C1A17] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)]'
            }`}
          >
            {/* Sheet Header */}
            <div className="flex items-center justify-between mb-3">
              <span className="text-[17px] font-semibold flex items-center gap-1.5">
                <Sparkles className={`w-4 h-4 ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`} />
                Quick add task
              </span>
              <button
                onClick={() => setIsQuickAddOpen(false)}
                className={`p-1.5 rounded-full ${isDarkTheme ? 'text-[#A39E98] hover:text-[#F5F2EF]' : 'text-[#5D5750] hover:text-[#1A1614]'}`}
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Natural Language Input */}
            <form onSubmit={handleQuickAddSubmit}>
              <div
                className={`relative rounded-2xl p-2.5 flex items-center border ${
                  isDarkTheme ? 'bg-[#24211E] border-[#2E2A27]' : 'bg-[#EFEAE4] border-[rgba(26,22,20,0.13)]'
                }`}
              >
                <input
                  type="text"
                  autoFocus
                  placeholder="e.g. Call mom tmrw 5pm !!!"
                  value={quickAddInput}
                  onChange={(e) => handleInputChange(e.target.value)}
                  className={`w-full bg-transparent outline-none text-[15px] px-2 ${
                    isDarkTheme ? 'text-[#F5F2EF] placeholder:text-[#6E6963]' : 'text-[#1A1614] placeholder:text-[#948B83]'
                  }`}
                />
                <button
                  type="button"
                  onClick={handleMicToggle}
                  className={`p-2 rounded-full transition-transform active:scale-90 ${
                    isMicListening
                      ? 'bg-red-500 text-white animate-pulse'
                      : isDarkTheme ? 'text-[#D9A184] hover:bg-[#2A1C16]' : 'text-[#8F4C2B] hover:bg-[#F7EBE3]'
                  }`}
                  title="Dictate with voice"
                >
                  <Mic className="w-4 h-4" />
                </button>
              </div>

              {/* Dynamic Live NLP Parsed Tokens */}
              <div className="flex flex-wrap gap-1.5 my-3 min-h-[28px]">
                {parsedTokens.dueDate && (
                  <span className={`text-[12px] px-2.5 py-0.5 rounded-full font-medium flex items-center gap-1 ${
                    isDarkTheme ? 'bg-[#2A1C16] text-[#D9A184]' : 'bg-[#F7EBE3] text-[#8F4C2B]'
                  }`}>
                    📅 {parsedTokens.dueDate}
                  </span>
                )}
                {parsedTokens.dueTime && (
                  <span className={`text-[12px] px-2.5 py-0.5 rounded-full font-medium flex items-center gap-1 ${
                    isDarkTheme ? 'bg-[#2A1C16] text-[#D9A184]' : 'bg-[#F7EBE3] text-[#8F4C2B]'
                  }`}>
                    ⏰ {parsedTokens.dueTime}
                  </span>
                )}
                {parsedTokens.priority && (
                  <span className={`text-[12px] px-2.5 py-0.5 rounded-full font-medium flex items-center gap-1 ${
                    parsedTokens.priority === 'HIGH'
                      ? isDarkTheme ? 'bg-[#2C1414] text-[#E05D5D]' : 'bg-red-100 text-[#C0504D]'
                      : isDarkTheme ? 'bg-[#2A1C16] text-[#D9A184]' : 'bg-[#F7EBE3] text-[#8F4C2B]'
                  }`}>
                    ⚡ {parsedTokens.priority}
                  </span>
                )}
                {parsedTokens.repeat && (
                  <span className={`text-[12px] px-2.5 py-0.5 rounded-full font-medium flex items-center gap-1 ${
                    isDarkTheme ? 'bg-[#2A1C16] text-[#D9A184]' : 'bg-[#F7EBE3] text-[#8F4C2B]'
                  }`}>
                    🔁 {parsedTokens.repeat}
                  </span>
                )}
              </div>

              {/* Tune Panel Toggle */}
              {isTuneOpen && (
                <div className={`p-3 rounded-2xl mb-3 text-[12px] space-y-2 border ${
                  isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#F3EEE8] border-[rgba(26,22,20,0.13)]'
                }`}>
                  <div className="flex items-center justify-between">
                    <span>Priority preset</span>
                    <div className="flex gap-1">
                      {(['HIGH', 'MEDIUM', 'LOW'] as const).map((p) => (
                        <button
                          key={p}
                          type="button"
                          onClick={() => setParsedTokens({ ...parsedTokens, priority: p })}
                          className={`px-2 py-0.5 rounded-full ${
                            parsedTokens.priority === p
                              ? isDarkTheme ? 'bg-[#D9A184] text-[#0A0908] font-bold' : 'bg-[#8F4C2B] text-white font-bold'
                              : isDarkTheme ? 'bg-[#24211E]' : 'bg-white'
                          }`}
                        >
                          {p}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>
              )}

              {/* Bottom Actions */}
              <div className="flex items-center justify-between pt-1">
                <button
                  type="button"
                  onClick={() => setIsTuneOpen(!isTuneOpen)}
                  className={`text-[13px] flex items-center gap-1 py-1.5 px-3 rounded-full transition-colors ${
                    isDarkTheme ? 'text-[#A39E98] hover:text-[#F5F2EF]' : 'text-[#5D5750] hover:text-[#1A1614]'
                  }`}
                >
                  <SlidersHorizontal className="w-3.5 h-3.5" />
                  {isTuneOpen ? 'Hide options' : 'Tune'}
                </button>

                <button
                  type="submit"
                  disabled={!quickAddInput.trim()}
                  className={`px-5 py-2 rounded-full font-medium text-[14px] transition-transform active:scale-95 disabled:opacity-50 ${
                    isDarkTheme ? 'bg-[#D9A184] text-[#0A0908]' : 'bg-[#8F4C2B] text-[#FFFFFF]'
                  }`}
                >
                  Save task
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

// -------------------------------------------------------------
// Individual Task Row Component (26dp radius surface)
// -------------------------------------------------------------
interface TaskRowItemProps {
  task: TaskItem;
  isDarkTheme: boolean;
  isAnimating: boolean;
  onToggle: () => void;
}

const TaskRowItem: React.FC<TaskRowItemProps> = ({
  task,
  isDarkTheme,
  isAnimating,
  onToggle,
}) => {
  const isStruck = task.isCompleted || isAnimating;

  // Checkbox border color based on priority
  let checkboxBorder = isDarkTheme ? 'border-[#6E6963]' : 'border-[#948B83]';
  if (task.priority === 'HIGH') checkboxBorder = isDarkTheme ? 'border-[#E05D5D]' : 'border-[#C0504D]';
  else if (task.priority === 'MEDIUM') checkboxBorder = isDarkTheme ? 'border-[#D9A184]' : 'border-[#8F4C2B]';

  return (
    <div
      className={`p-3.5 rounded-[26px] border flex items-center justify-between gap-3 transition-all ${
        isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)] shadow-2xs'
      } ${task.isCompleted ? 'opacity-65' : ''}`}
    >
      {/* Checkbox (28dp circle) */}
      <button
        onClick={onToggle}
        className={`w-[28px] h-[28px] rounded-full border-2 flex items-center justify-center flex-shrink-0 transition-all ${checkboxBorder} ${
          task.isCompleted
            ? isDarkTheme ? 'bg-[#D9A184] border-[#D9A184] text-[#0A0908]' : 'bg-[#8F4C2B] border-[#8F4C2B] text-[#FFFFFF]'
            : 'bg-transparent'
        }`}
        aria-label={`Mark ${task.title} as ${task.isCompleted ? 'incomplete' : 'complete'}`}
      >
        {task.isCompleted && <Check className="w-4 h-4 stroke-[3]" />}
      </button>

      {/* Task Content */}
      <div className="flex-1 min-w-0">
        <h4
          className={`text-[15px] font-semibold leading-snug truncate transition-all ${
            isStruck
              ? isDarkTheme ? 'line-through text-[#6E6963]' : 'line-through text-[#948B83]'
              : isDarkTheme ? 'text-[#F5F2EF]' : 'text-[#1A1614]'
          }`}
        >
          {task.title}
        </h4>
        <div className="flex items-center gap-2 mt-0.5 text-[12px]">
          <span className={isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}>
            {task.dueDateLabel} · {task.dueTimeLabel}
          </span>
          {task.repeatLabel && (
            <span className={`flex items-center gap-0.5 ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`}>
              <Repeat className="w-3 h-3" />
              {task.repeatLabel}
            </span>
          )}
        </div>
      </div>

      {/* Far-Right Priority Badge (High & Medium only) */}
      {task.priority === 'HIGH' && (
        <span
          className={`px-2.5 py-0.5 rounded-full text-[11px] font-semibold flex-shrink-0 ${
            isDarkTheme ? 'bg-[#2C1414] text-[#E05D5D]' : 'bg-red-100 text-[#C0504D]'
          }`}
        >
          High
        </span>
      )}
      {task.priority === 'MEDIUM' && (
        <span
          className={`px-2.5 py-0.5 rounded-full text-[11px] font-semibold flex-shrink-0 ${
            isDarkTheme ? 'bg-[#2A1C16] text-[#D9A184]' : 'bg-[#F7EBE3] text-[#8F4C2B]'
          }`}
        >
          Medium
        </span>
      )}
    </div>
  );
};
