import React, { useState } from 'react';
import { parseNaturalLanguageTask } from '../utils/nlpParser';
import { Sparkles, Terminal, ArrowRight, CheckCircle2 } from 'lucide-react';

interface QuickAddPlaygroundProps {
  isDarkTheme: boolean;
}

const PRESET_EXAMPLES = [
  'Submit final taxes tmrw 5pm !!!',
  'Engineering standup every tuesday 9am',
  'Water garden every 3 days',
  'Stretch and hydrate in 45 mins',
  'Review security audit report tonight !p1',
];

export const QuickAddPlayground: React.FC<QuickAddPlaygroundProps> = ({ isDarkTheme }) => {
  const [inputText, setInputText] = useState<string>('Submit final taxes tmrw 5pm !!!');
  const parsed = parseNaturalLanguageTask(inputText);

  return (
    <section id="quick-add" className="py-20 relative">
      <div className="max-w-[1120px] w-full mx-auto px-4 sm:px-6">
        {/* Section Header */}
        <div className="text-center max-w-[680px] mx-auto mb-14">
          <span
            className={`text-[13px] font-bold uppercase tracking-wider block mb-2 ${
              isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'
            }`}
          >
            Natural Language Engine
          </span>
          <h2 className="text-[32px] sm:text-[42px] font-bold leading-tight tracking-tight mb-4">
            Type like you talk. Zero form fatigue.
          </h2>
          <p className={`text-[16px] sm:text-[18px] ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
            Reflex parses dates, times, relative durations, priorities, and recurrence patterns on the fly. Try typing below:
          </p>
        </div>

        {/* Interactive Live Playground Card */}
        <div
          className={`p-6 sm:p-10 rounded-[36px] border mb-12 shadow-xl ${
            isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)]'
          }`}
        >
          {/* Preset Buttons */}
          <div className="flex flex-wrap items-center gap-2 mb-4">
            <span className={`text-[12px] font-medium mr-1 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
              Try a preset:
            </span>
            {PRESET_EXAMPLES.map((ex, i) => (
              <button
                key={i}
                onClick={() => setInputText(ex)}
                className={`text-[12px] px-3 py-1 rounded-full border transition-all ${
                  inputText === ex
                    ? isDarkTheme
                      ? 'bg-[#2A1C16] border-[#D9A184] text-[#D9A184] font-semibold'
                      : 'bg-[#F7EBE3] border-[#8F4C2B] text-[#8F4C2B] font-semibold'
                    : isDarkTheme
                    ? 'bg-[#1C1A17] border-[#2E2A27] text-[#A39E98] hover:text-[#F5F2EF]'
                    : 'bg-[#EFEAE4] border-[rgba(26,22,20,0.13)] text-[#5D5750] hover:text-[#1A1614]'
                }`}
              >
                "{ex}"
              </button>
            ))}
          </div>

          {/* Live Input Field */}
          <div
            className={`p-4 rounded-2xl border flex items-center gap-3 transition-colors ${
              isDarkTheme ? 'bg-[#24211E] border-[#2E2A27]' : 'bg-[#EFEAE4] border-[rgba(26,22,20,0.13)]'
            }`}
          >
            <Terminal className={`w-5 h-5 flex-shrink-0 ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`} />
            <input
              type="text"
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              placeholder="Type any task with natural dates, times, and priorities..."
              className={`w-full bg-transparent outline-none text-[16px] sm:text-[18px] font-medium ${
                isDarkTheme ? 'text-[#F5F2EF] placeholder:text-[#6E6963]' : 'text-[#1A1614] placeholder:text-[#948B83]'
              }`}
            />
          </div>

          {/* Live Parsed Output Representation */}
          <div className="mt-8 pt-6 border-t border-neutral-700/15">
            <div className="flex items-center gap-2 mb-3">
              <Sparkles className={`w-4 h-4 ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`} />
              <span className="text-[13px] font-bold uppercase tracking-wider">
                Live Parser Output
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
              {/* Clean Title */}
              <div
                className={`p-3.5 rounded-2xl border ${
                  isDarkTheme ? 'bg-[#1C1A17] border-[#2E2A27]' : 'bg-[#F3EEE8] border-[rgba(26,22,20,0.13)]'
                }`}
              >
                <span className={`text-[11px] block uppercase font-medium mb-1 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                  Clean Title
                </span>
                <span className="text-[14px] font-semibold truncate block">
                  {parsed.cleanTitle || '—'}
                </span>
              </div>

              {/* Due Date */}
              <div
                className={`p-3.5 rounded-2xl border ${
                  isDarkTheme ? 'bg-[#1C1A17] border-[#2E2A27]' : 'bg-[#F3EEE8] border-[rgba(26,22,20,0.13)]'
                }`}
              >
                <span className={`text-[11px] block uppercase font-medium mb-1 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                  Due Date
                </span>
                {parsed.dueDate ? (
                  <span
                    className={`inline-block px-2.5 py-0.5 rounded-full text-[12px] font-semibold ${
                      isDarkTheme ? 'bg-[#2A1C16] text-[#D9A184]' : 'bg-[#F7EBE3] text-[#8F4C2B]'
                    }`}
                  >
                    📅 {parsed.dueDate}
                  </span>
                ) : (
                  <span className="text-[13px] opacity-40">None</span>
                )}
              </div>

              {/* Due Time */}
              <div
                className={`p-3.5 rounded-2xl border ${
                  isDarkTheme ? 'bg-[#1C1A17] border-[#2E2A27]' : 'bg-[#F3EEE8] border-[rgba(26,22,20,0.13)]'
                }`}
              >
                <span className={`text-[11px] block uppercase font-medium mb-1 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                  Due Time
                </span>
                {parsed.dueTime ? (
                  <span
                    className={`inline-block px-2.5 py-0.5 rounded-full text-[12px] font-semibold ${
                      isDarkTheme ? 'bg-[#2A1C16] text-[#D9A184]' : 'bg-[#F7EBE3] text-[#8F4C2B]'
                    }`}
                  >
                    ⏰ {parsed.dueTime}
                  </span>
                ) : (
                  <span className="text-[13px] opacity-40">None</span>
                )}
              </div>

              {/* Priority */}
              <div
                className={`p-3.5 rounded-2xl border ${
                  isDarkTheme ? 'bg-[#1C1A17] border-[#2E2A27]' : 'bg-[#F3EEE8] border-[rgba(26,22,20,0.13)]'
                }`}
              >
                <span className={`text-[11px] block uppercase font-medium mb-1 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                  Priority Level
                </span>
                {parsed.priority ? (
                  <span
                    className={`inline-block px-2.5 py-0.5 rounded-full text-[12px] font-semibold ${
                      parsed.priority === 'HIGH'
                        ? isDarkTheme ? 'bg-[#2C1414] text-[#E05D5D]' : 'bg-red-100 text-[#C0504D]'
                        : isDarkTheme ? 'bg-[#2A1C16] text-[#D9A184]' : 'bg-[#F7EBE3] text-[#8F4C2B]'
                    }`}
                  >
                    ⚡ {parsed.priority}
                  </span>
                ) : (
                  <span className="text-[13px] opacity-40">Default (None)</span>
                )}
              </div>

              {/* Recurrence */}
              <div
                className={`p-3.5 rounded-2xl border ${
                  isDarkTheme ? 'bg-[#1C1A17] border-[#2E2A27]' : 'bg-[#F3EEE8] border-[rgba(26,22,20,0.13)]'
                }`}
              >
                <span className={`text-[11px] block uppercase font-medium mb-1 ${isDarkTheme ? 'text-[#A39E98]' : 'text-[#5D5750]'}`}>
                  Recurrence
                </span>
                {parsed.repeat ? (
                  <span
                    className={`inline-block px-2.5 py-0.5 rounded-full text-[12px] font-semibold ${
                      isDarkTheme ? 'bg-[#2A1C16] text-[#D9A184]' : 'bg-[#F7EBE3] text-[#8F4C2B]'
                    }`}
                  >
                    🔁 {parsed.repeat}
                  </span>
                ) : (
                  <span className="text-[13px] opacity-40">None</span>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Syntax Vocabulary Table from README */}
        <div
          className={`p-6 sm:p-8 rounded-[36px] border ${
            isDarkTheme ? 'bg-[#141211] border-[#2E2A27]' : 'bg-[#FFFFFF] border-[rgba(26,22,20,0.13)]'
          }`}
        >
          <h3 className="text-[20px] font-bold mb-4">Complete NLP Syntax Reference</h3>

          <div className="overflow-x-auto reflex-scrollbar">
            <table className="w-full text-left text-[14px]">
              <thead>
                <tr className={`border-b ${isDarkTheme ? 'border-[#2E2A27] text-[#A39E98]' : 'border-[rgba(26,22,20,0.13)] text-[#5D5750]'}`}>
                  <th className="py-2.5 font-semibold">What you type</th>
                  <th className="py-2.5 font-semibold">Parsed interpretation</th>
                  <th className="py-2.5 font-semibold">Category</th>
                </tr>
              </thead>
              <tbody className={`divide-y ${isDarkTheme ? 'divide-[#2E2A27]/60' : 'divide-[#F3EEE8]'}`}>
                <tr>
                  <td className="py-3 font-mono text-[13px]"><code className="px-2 py-0.5 rounded bg-neutral-500/10">today</code>, <code className="px-2 py-0.5 rounded bg-neutral-500/10">tmrw</code>, <code className="px-2 py-0.5 rounded bg-neutral-500/10">next week</code>, <code className="px-2 py-0.5 rounded bg-neutral-500/10">in 5 days</code></td>
                  <td className="py-3">Relative calendar due dates</td>
                  <td className={`py-3 ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`}>Due Dates</td>
                </tr>
                <tr>
                  <td className="py-3 font-mono text-[13px]"><code className="px-2 py-0.5 rounded bg-neutral-500/10">5pm</code>, <code className="px-2 py-0.5 rounded bg-neutral-500/10">17:30</code>, <code className="px-2 py-0.5 rounded bg-neutral-500/10">morning</code>, <code className="px-2 py-0.5 rounded bg-neutral-500/10">noon</code>, <code className="px-2 py-0.5 rounded bg-neutral-500/10">eod</code></td>
                  <td className="py-3">Clock times and contextual periods</td>
                  <td className={`py-3 ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`}>Times</td>
                </tr>
                <tr>
                  <td className="py-3 font-mono text-[13px]"><code className="px-2 py-0.5 rounded bg-neutral-500/10">in 45 mins</code>, <code className="px-2 py-0.5 rounded bg-neutral-500/10">in 1hr 4min</code></td>
                  <td className="py-3">Current time + relative duration</td>
                  <td className={`py-3 ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`}>Durations</td>
                </tr>
                <tr>
                  <td className="py-3 font-mono text-[13px]"><code className="px-2 py-0.5 rounded bg-neutral-500/10">!!!</code>, <code className="px-2 py-0.5 rounded bg-neutral-500/10">p1</code>, <code className="px-2 py-0.5 rounded bg-neutral-500/10">urgent</code>, <code className="px-2 py-0.5 rounded bg-neutral-500/10">asap</code></td>
                  <td className="py-3 font-semibold text-red-500">High priority (Triggers system floating overlay)</td>
                  <td className="py-3 text-red-500">Priority</td>
                </tr>
                <tr>
                  <td className="py-3 font-mono text-[13px]"><code className="px-2 py-0.5 rounded bg-neutral-500/10">!!</code>, <code className="px-2 py-0.5 rounded bg-neutral-500/10">p2</code>, <code className="px-2 py-0.5 rounded bg-neutral-500/10">med priority</code></td>
                  <td className="py-3 font-semibold">Medium priority (Triggers system floating overlay)</td>
                  <td className={`py-3 ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`}>Priority</td>
                </tr>
                <tr>
                  <td className="py-3 font-mono text-[13px]"><code className="px-2 py-0.5 rounded bg-neutral-500/10">repeat every tuesday</code>, <code className="px-2 py-0.5 rounded bg-neutral-500/10">every 2 weeks</code></td>
                  <td className="py-3">Autonomous recurrence scheduling</td>
                  <td className={`py-3 ${isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'}`}>Recurrence</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </section>
  );
};
