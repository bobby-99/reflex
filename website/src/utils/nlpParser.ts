import { ParsedTokens } from '../types';

/**
 * Pure TypeScript replica of Reflex's TaskParser.kt
 * Extracts relative dates, 12h/24h times, priority tokens, and recurrence intervals.
 */
export function parseNaturalLanguageTask(input: string): ParsedTokens {
  const raw = input.trim();
  if (!raw) {
    return { cleanTitle: '', rawInput: '' };
  }

  let text = raw;
  let dueDate: string | undefined;
  let dueTime: string | undefined;
  let priority: 'HIGH' | 'MEDIUM' | 'LOW' | undefined;
  let repeat: string | undefined;

  // 1. Priority parsing (High to Low priority markers)
  const highPriorityRegex = /\b(!!!|p1|urgent|asap|high priority)\b/i;
  const medPriorityRegex = /\b(!!|p2|med priority|medium priority)\b/i;
  const lowPriorityRegex = /\b(!|p3|low priority)\b/i;

  if (highPriorityRegex.test(text)) {
    priority = 'HIGH';
    text = text.replace(highPriorityRegex, '').trim();
  } else if (medPriorityRegex.test(text)) {
    priority = 'MEDIUM';
    text = text.replace(medPriorityRegex, '').trim();
  } else if (lowPriorityRegex.test(text)) {
    priority = 'LOW';
    text = text.replace(lowPriorityRegex, '').trim();
  }

  // 2. Recurrence parsing
  const repeatWeekdayRegex = /\b(repeat every weekday|every weekday|weekdays repeat)\b/i;
  const repeatDayRegex = /\b(every (monday|tuesday|wednesday|thursday|friday|saturday|sunday)|repeat every (monday|tuesday|wednesday|thursday|friday|saturday|sunday))\b/i;
  const repeatDailyRegex = /\b(every day repeat|repeat daily|every day|daily repeat)\b/i;
  const repeatIntervalRegex = /\b(repeat every (\d+)\s*(days|weeks|months)|every (\d+)\s*(days|weeks|months))\b/i;
  const repeatMonthlyRegex = /\b(every month \d+(?:st|nd|rd|th)? day repeat|every month|monthly repeat)\b/i;

  if (repeatWeekdayRegex.test(text)) {
    repeat = 'Every weekday';
    text = text.replace(repeatWeekdayRegex, '').trim();
  } else if (repeatDayRegex.test(text)) {
    const match = text.match(repeatDayRegex);
    const day = (match?.[2] || match?.[3] || 'day');
    repeat = `Every ${day.charAt(0).toUpperCase() + day.slice(1).toLowerCase()}`;
    text = text.replace(repeatDayRegex, '').trim();
  } else if (repeatDailyRegex.test(text)) {
    repeat = 'Every day';
    text = text.replace(repeatDailyRegex, '').trim();
  } else if (repeatIntervalRegex.test(text)) {
    const match = text.match(repeatIntervalRegex);
    const num = match?.[2] || match?.[4] || '2';
    const unit = match?.[3] || match?.[5] || 'days';
    repeat = `Every ${num} ${unit}`;
    text = text.replace(repeatIntervalRegex, '').trim();
  } else if (repeatMonthlyRegex.test(text)) {
    repeat = 'Every month';
    text = text.replace(repeatMonthlyRegex, '').trim();
  }

  // 3. Relative Date parsing
  const todayRegex = /\b(today|2day)\b/i;
  const tomorrowRegex = /\b(tomorrow|tmrw|tmr)\b/i;
  const tonightRegex = /\b(tonight)\b/i;
  const nextWeekRegex = /\b(next week|next monday|next friday|next weekend)\b/i;
  const inDaysRegex = /\b(in (\d+)\s*days|in (\d+)\s*weeks)\b/i;
  const dayNameRegex = /\b(this (monday|tuesday|wednesday|thursday|friday|saturday|sunday))\b/i;

  if (tonightRegex.test(text)) {
    dueDate = 'Today';
    dueTime = '8:00 pm';
    text = text.replace(tonightRegex, '').trim();
  } else if (tomorrowRegex.test(text)) {
    dueDate = 'Tomorrow';
    text = text.replace(tomorrowRegex, '').trim();
  } else if (todayRegex.test(text)) {
    dueDate = 'Today';
    text = text.replace(todayRegex, '').trim();
  } else if (nextWeekRegex.test(text)) {
    const match = text.match(nextWeekRegex);
    dueDate = match ? match[0] : 'Next week';
    text = text.replace(nextWeekRegex, '').trim();
  } else if (inDaysRegex.test(text)) {
    const match = text.match(inDaysRegex);
    dueDate = match ? match[0] : 'Upcoming';
    text = text.replace(inDaysRegex, '').trim();
  } else if (dayNameRegex.test(text)) {
    const match = text.match(dayNameRegex);
    dueDate = match ? match[0] : 'This week';
    text = text.replace(dayNameRegex, '').trim();
  }

  // 4. Time parsing (Exact 12h/24h or relative duration / time of day)
  const exactTimeRegex = /\b((?:1[0-2]|0?[1-9]):[0-5][0-9]\s*(?:am|pm)|(?:1[0-2]|0?[1-9])\s*(?:am|pm)|(?:[01]?[0-9]|2[0-3]):[0-5][0-9])\b/i;
  const timeOfDayRegex = /\b(morning|mrng|noon|midday|afternoon|evening|evng|eod|end of day|midnight|nite)\b/i;
  const durationRegex = /\b(in\s+(?:\d+\s*(?:hr|hour|hours))?\s*(?:\d+\s*(?:min|mins|minutes))?)\b/i;

  if (!dueTime && exactTimeRegex.test(text)) {
    const match = text.match(exactTimeRegex);
    if (match) {
      dueTime = match[0].toLowerCase();
      text = text.replace(exactTimeRegex, '').trim();
    }
  } else if (!dueTime && durationRegex.test(text)) {
    const match = text.match(durationRegex);
    if (match) {
      dueTime = match[0].toLowerCase();
      text = text.replace(durationRegex, '').trim();
    }
  } else if (!dueTime && timeOfDayRegex.test(text)) {
    const match = text.match(timeOfDayRegex);
    const token = match?.[0].toLowerCase();
    if (token === 'morning' || token === 'mrng') dueTime = '8:00 am';
    else if (token === 'noon' || token === 'midday') dueTime = '12:30 pm';
    else if (token === 'afternoon') dueTime = '3:00 pm';
    else if (token === 'evening' || token === 'evng') dueTime = '4:00 pm';
    else if (token === 'eod' || token === 'end of day') dueTime = '6:00 pm';
    else if (token === 'midnight') dueTime = '12:00 am';
    else if (token === 'nite') dueTime = '8:00 pm';

    if (match) {
      text = text.replace(timeOfDayRegex, '').trim();
    }
  }

  // Clean trailing punctuation or prepositions (like "at", "on")
  let cleanTitle = text
    .replace(/\s+(at|on|due|for)$/i, '')
    .replace(/^[,.\s-]+|[,.\s-]+$/g, '')
    .trim();

  if (!cleanTitle && raw) {
    cleanTitle = raw;
  }

  return {
    cleanTitle,
    dueDate,
    dueTime,
    priority,
    repeat,
    rawInput: raw,
  };
}
