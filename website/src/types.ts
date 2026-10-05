/**
 * Core type definitions for Reflex Landing & Interactive Phone Replica
 */

export type ThemeMode = 'system' | 'dark' | 'light';
export type ActiveTab = 'routines' | 'calendar' | 'tasks' | 'habits' | 'focus' | 'settings';

export type FocusMode = 'pomodoro' | 'timed_flow' | 'open_flow';

export interface TaskItem {
  id: string;
  title: string;
  dueDateLabel: string;
  dueTimeLabel: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW' | 'NONE';
  isCompleted: boolean;
  category: 'overdue' | 'today' | 'upcoming' | 'nodate';
  repeatLabel?: string;
}

export interface RoutineStep {
  title: string;
  type: 'timed' | 'checkoff' | 'reps';
  durationSec?: number;
  targetReps?: number;
}

export interface RoutineItem {
  id: string;
  name: string;
  category: string;
  durationMin: number;
  stepCount: number;
  streakDays: number;
  steps: RoutineStep[];
}

export interface HabitItem {
  id: string;
  name: string;
  type: 'checkoff' | 'measurable' | 'limit';
  currentValue: number;
  targetValue: number;
  unit?: string;
  streakDays: number;
  history: boolean[]; // last 7 days
}

export interface CalendarEventItem {
  id: string;
  title: string;
  time: string;
  type: 'event' | 'task' | 'routine';
  subtitle?: string;
  isCompleted?: boolean;
}

export interface ParsedTokens {
  cleanTitle: string;
  dueDate?: string;
  dueTime?: string;
  priority?: 'HIGH' | 'MEDIUM' | 'LOW';
  repeat?: string;
  rawInput: string;
}
