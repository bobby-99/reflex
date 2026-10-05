# Reflex — Official Website & Interactive Demo Technical Overview

This document provides a comprehensive technical overview of the official website for **Reflex**, an open-source, 100% offline Android productivity application for routines, habits, tasks, deep focus, and a unified calendar.

---

## 1. Executive Summary & Brand Architecture

| Parameter | Specification |
| :--- | :--- |
| **Product Name** | Reflex |
| **Target Platform** | Android 16 (API 36), Min SDK 26 |
| **License** | GNU General Public License v3.0 or later ([GPL-3.0-or-later](https://github.com/bobby-99/reflex/blob/main/LICENSE)) |
| **Repository** | [github.com/bobby-99/reflex](https://github.com/bobby-99/reflex) |
| **Production Site URL** | [https://bobby-99.github.io/reflex/](https://bobby-99.github.io/reflex/) |
| **Hosting Strategy** | GitHub Pages served purely from `/docs` on the `main` branch |
| **Privacy Model** | 100% On-Device, zero telemetry, zero accounts, no `android.permission.INTERNET` |
| **Design Standard** | Reflex Design System v1.0 (`DESIGN.md`) |

---

## 2. Design System Tokens & Aesthetics (DESIGN.md)

The website faithfully reproduces the organic materials, tactile curvature, and typography of Reflex:

### 2.1 Color Tokens & Theming

```
Dark Theme (Obsidian Copper)               Light Theme (Warm Paper & Rich Copper)
├── Canvas:     #0A0908 (OLED deep black)   ├── Canvas:     #F7F3EE (Warm off-white linen)
├── Surface:    #141211 (Level 1 cards)     ├── Surface:    #FFFFFF (Pure white cards)
├── Elevated:   #1C1A17 (Floating sheets)   ├── Input:      #EFEAE4 (Pill chips & inputs)
├── Input:      #24211E (Inputs & chips)    ├── Ink:        #1A1614 (Deep warm ink)
├── Accent:     #D9A184 (Metallic copper)   ├── Secondary:  #5D5750 (WCAG AA 6.4:1 contrast)
├── Pressed:    #B57E63 (Active rims)       ├── Accent:     #8F4C2B (Rich copper, 7.3:1 contrast)
├── Border:     #2E2A27 (1px hairline)      ├── Border:     rgba(26, 22, 20, 0.13)
├── Green:      #4ADE80 (Streaks & checks)  ├── Green:      #327A54 (Accessible green)
└── Blue:       #6F9BFF (Calendar events)   └── Blue:       #3565D6 (Accessible blue)
```

### 2.2 Typography Hierarchy
- **Primary Typeface**: Locally bundled serif **Lora** (Regular 400, Medium 500, SemiBold 600, Bold 700) with a universal serif system fallback (`Georgia`, `Times New Roman`, `serif`).
- **Tabular Figures (`tnum`)**: All countdown clocks, stopwatches, percentages, matrix grids, and stat numbers enforce:
  ```css
  font-feature-settings: 'tnum' 1;
  font-variant-numeric: tabular-nums;
  ```
  This eliminates horizontal layout jittering as numbers change.
- **Universal Sentence Case**: Strictly zero all-caps strings, zero `.toUpperCase()` styling, and zero mechanical tags (`//`, `>_`).
- **Font Size Floor**: 13px floor across all metadata, chips, and labels.

### 2.3 Glassmorphism System
- **Backdrop Blur**: `backdrop-filter: blur(24px); -webkit-backdrop-filter: blur(24px);`.
- **Glass Tinting**:
  - Dark Mode: `rgba(28, 26, 23, 0.76)` with `1px solid rgba(255, 255, 255, 0.08)`.
  - Light Mode: `rgba(255, 255, 255, 0.78)` with `1px solid rgba(26, 22, 20, 0.12)`.
- **Solid Fallbacks**: Protected via `@supports not (backdrop-filter: blur(24px))` ensuring solid opaque surfaces on browsers without hardware compositing.

### 2.4 Ambient Motion & Accessibility
- Very slow, CSS-only copper radial gradient light blobs (`.ambient-blob-a` and `.ambient-blob-b`) drifting on 24s/30s periods.
- Full compliance with `@media (prefers-reduced-motion: reduce)` which zeroes all animations instantly.

---

## 3. Architecture of the Interactive Android Phone Replica

The centerpiece of the website is a custom-engineered, generic Android device component (CSS/SVG, logical resolution **390 × 844 px**) rendered alongside the narrative.

### 3.1 Device Bezel & Hardware Details
- **Outer Shell**: Sleek 52px corner radius with 10px bezel, metallic 1px rim, and a 30px blur ambient drop shadow.
- **Punch-Hole Camera**: Centered 12px circular aperture at top center.
- **Live Status Bar**:
  - Dynamic clock running on a 30-second interval, formatted in local 12-hour time.
  - "OFFLINE" copper status badge + `WifiOff` icon demonstrating zero network communication.
  - Battery charge gauge (94% charging).
- **Gesture Bar**: 116 × 4 px rounded stadium pill centered 8px above the bottom screen edge.
- **Responsive Viewport Scaling**: Container scales cleanly (`scale(0.84)` to `scale(1.0)`) without using viewport units inside the screen, maintaining pixel-perfect coordinate systems.

### 3.2 Floating Frosted Glass Tab Bar (`PhoneBottomNav.tsx`)
- Geometry: 72px tall stadium pill with 36px corner radius, floating 24px above the bottom gesture pill with 16px horizontal margins.
- Backdrop blur: 24px blur with 76% glass tint and 1px hairline border rim.
- 5 Top-Level Destinations:
  1. **Routines**: Clock icon (22px), label "Routines".
  2. **Calendar**: Calendar icon (22px), label "Calendar".
  3. **Tasks Checklist Button**: 52px circular solid copper action button (`#D9A184` dark / `#8F4C2B` light) with checklist icon. Tapping switches directly to Tasks. Does **not** morph or open Quick Add directly.
  4. **Habits**: Flame icon (22px), label "Habits".
  5. **Focus**: Target circle icon (22px), label "Focus".

---

## 4. Replicated Application Screens

### 4.1 Routines Subsystem (`RoutinesScreen.tsx`)
- **Week Activity Strip**: 7 circular day indicators with today's highlight and active completion dots.
- **2×2 Tactile Stat Tiles**: Active routines (3), Pending tasks (2), Focus time (45 mins), Momentum streak (14 days).
- **Active Routines List**: Cards for *Morning Momentum* (18m, 4 steps), *Daily Reset* (12m, 3 steps), and *Deep Work Priming* (10m, 2 steps).
- **Sequential Step Runner (Immersion View)**:
  - Full-screen countdown / rep display with progress tracking bar.
  - Step types: Timed intervals, Check-off actions, and Target repetition counters.
  - Runtime controls: Pause/Resume, Next Step, Skip Rest, and auto-advance toggle.
  - Completion Celebration: Multi-burst celebratory icon animation when all steps finish.

### 4.2 Tasks & NLP Subsystem (`TasksScreen.tsx`)
- **Daily Progress Card**: "3 of 5 done today", percentage readout, and animated copper progress bar.
- **Sticky Filter Chips**: Stadium pills for `All` (4), `Today` (3), `Upcoming` (1), `No date` (0), and `Done` (2).
- **Categorized Sections**:
  - **Overdue**: Crimson header with "needs attention" subtitle.
  - **Today**: Wed, Sep 30 subtitle.
  - **Upcoming**: Scheduled count subtitle.
  - **Completed Accordion**: Collapsible 48px toggle row.
- **Task Rows & Interactive Checkbox**:
  - Circular priority-bordered checkboxes (Red for High, Copper for Medium, Neutral for Low).
  - 380ms visual strikethrough transition before moving to completed state.
  - Floating 3.5-second Undo toast with interactive restore button.
- **Dedicated Round "+" FAB**: 58px circular copper pill floating at bottom-right (44px clearance above the raised tab bar), opening the Quick Add sheet.
- **Quick Add Card Sheet**:
  - Elevated 32px sheet with natural-language text input.
  - Voice dictation microphone button with pulsing ring.
  - Live copper NLP token chips (`📅 Date`, `⏰ Time`, `⚡ Priority`, `🔁 Repeat`).
  - Tune panel for manual override of priorities and recurrences.

### 4.3 Habits Subsystem (`HabitsScreen.tsx`)
- **Streak Hero Card**: 44sp Bold copper number (14 days), "Best: 28 days", and 7-day indicator dots.
- **Daily Circular Progress Ring**: 110px SVG circular progress ring displaying overall completion percentage and fraction.
- **3 Distinct Habit Types**:
  1. `CheckOff`: Binary checkmark toggle (e.g. *Morning sunlight & walk*).
  2. `Measurable`: Numeric progress with units and circular `-` / `+` steppers (e.g. *Deep book reading: 15 / 20 pages*).
  3. `Limit`: Cap tracker with red alert over-limit state (e.g. *Espresso limit: max 2 cups*).
- **Monthly Matrix View**: 30-day interactive matrix grid showing past daily completion intensities.
- **Analytics Sheet**: 30-day rate (87%), Best streak (28d), Total check-ins (142).

### 4.4 Deep Focus & Liquid Timer (`FocusScreen.tsx` & `LiquidTimerCanvas.tsx`)
- **Segmented Mode Selector**: Pomodoro cycles vs. Timed / Open Flow.
- **Hero LiquidTimer Sphere**:
  - Canvas rendering with dual sinusoidal waves: `y = fillY + amp * sin(freq * x + phase)`.
  - Realistic sloshing tilt and liquid draining ratio matching elapsed time.
  - Center readout: Phase label, giant countdown in Lora Bold tabular numerals, and status caption.
  - Performance: Single `requestAnimationFrame` loop that automatically pauses when off-screen via `IntersectionObserver` or when the document is hidden via `visibilitychange`.
- **Pomodoro Cycle Strip**: Proportional copper segments for work intervals and sage green segments for short/long breaks.
- **Duration Steppers**: Focus length (5–120 min), Short break (0–30 min), Long break (5–60 min), and Cycles (1–8).
- **Running Session Immersion View**:
  - Giant 79sp countdown display.
  - Linked task chip badge.
  - Rotating calm quotes during focus.
  - Demo speed control multiplier (1x, 10x, 60x) for rapid demonstration of wave draining.
- **Distraction Shield Preview**: Interactive modal simulating the Android system overlay barrier when opening restricted apps during focus.

### 4.5 Calendar Subsystem (`CalendarScreen.tsx`)
- **Morphing Calendar Card**: Compact 1-row week strip that smoothly animates into a full 30-day month grid with directional grab handle.
- **Category Indicator Dots**: Blue for native device events (`#6F9BFF` / `#3565D6`), Copper for tasks (`#D9A184` / `#8F4C2B`), and Green for routines (`#86C9A4` / `#327A54`).
- **Agenda Timeline List**:
  - Vertical spine line under the date indicator center.
  - Chronological "Now" divider with pulsing dot and live time marker.
  - Interactive task completion directly within the agenda feed.

### 4.6 Settings & Diagnostics (`SettingsScreen.tsx`)
- **User Profile Header**: 68px avatar ring in metallic copper with camera badge, Name ("Alex Rivera"), and Bio.
- **Live 3-Way Theme Switcher**: System, Dark, Light controls that instantly re-theme both the phone and the parent website, stored in `localStorage`.
- **Permissions Diagnostic Hub**: Live audit state for Overlay (`p_ovr`), Device Calendar (`p_cal`), Notification Channels (`p_not`), and Exact Alarms (`p_alm`).
- **Local Vault**: 100% offline Room database stats, JSON backup export simulation, and instant wipe dialog.

---

## 5. Natural Language Processing Engine (`nlpParser.ts`)

A pure TypeScript implementation of Reflex's Kotlin `TaskParser.kt` and `TaskHighlightVisualTransformation.kt`:

| Category | Input Syntax Patterns | Parsed Result |
| :--- | :--- | :--- |
| **Relative Dates** | `today`, `2day` | `dueDate = 'Today'` |
| | `tomorrow`, `tmrw`, `tmr` | `dueDate = 'Tomorrow'` |
| | `tonight` | `dueDate = 'Today'`, `dueTime = '8:00 pm'` |
| | `next week`, `this friday`, `in 5 days` | Relative calendar offsets |
| **Times** | `5pm`, `17:30`, `11:30 am`, `3:15 pm` | 12-hour formatted time string |
| | `morning` / `mrng` | `8:00 am` |
| | `noon` / `midday` | `12:30 pm` |
| | `afternoon` | `3:00 pm` |
| | `evening` / `evng` | `4:00 pm` |
| | `eod` / `end of day` | `6:00 pm` |
| | `night` / `nite` | `8:00 pm` |
| | `midnight` | `12:00 am` |
| **Relative Durations**| `in 45 mins`, `in 10 min`, `in 1hr 4min` | Relative offset duration |
| **Priority Tokens** | `!!!`, `p1`, `urgent`, `asap`, `high priority` | `Priority.HIGH` (Red badge) |
| | `!!`, `p2`, `med priority`, `medium priority` | `Priority.MEDIUM` (Copper badge) |
| | `!`, `p3`, `low priority` | `Priority.LOW` (Unbadged) |
| **Recurrences** | `repeat every tuesday`, `every tuesday` | `repeat = 'Every Tuesday'` |
| | `every day repeat`, `repeat daily`, `every day` | `repeat = 'Every day'` |
| | `repeat every weekday`, `weekdays repeat` | `repeat = 'Every weekday'` |
| | `repeat every 2 weeks`, `every 3 days` | `repeat = 'Every N intervals'` |

---

## 6. Pinned Sticky Phone Architecture (`HeroAndStory.tsx`)

### The Challenge of Sticky Pinning
In web browsers (Blink, WebKit, Gecko), any ancestor element with `overflow: hidden`, `overflow-x: hidden`, or `overflow-y: hidden` disrupts the viewport scroll boundary, causing CSS `position: sticky` to fall back to `position: relative`.

### The Solution Implemented
1. **Root Overflow Containment**: Removed `overflow-x-hidden` from `#main-content`. Ambient background glow blobs are strictly contained in their own `fixed inset-0 pointer-events-none -z-10 overflow-hidden` canvas, preventing horizontal page scroll without affecting sticky positioning.
2. **Unified Narrative Layout**: Unified the Hero section and all 6 Feature Story chapters into a single two-column grid (`HeroAndStory.tsx`):
   - **Left Column (`lg:col-span-7`)**: The hero introduction and all 6 feature cards scroll smoothly through ~3,500px of content.
   - **Right Column (`lg:col-span-5`)**: Houses **one single, persistent phone** anchored at `sticky top-20 self-start z-30`.
3. **Scroll Story Synchronization**: An `IntersectionObserver` and window scroll listener track which section is currently centered in the viewport, smoothly navigating the phone to that screen.
4. **Zero-Fighting Interaction Priority**: When a visitor clicks, touches, or interacts with any control inside the phone, a 9-second interaction lock prevents window scrolling from overriding the user's active screen.

---

## 7. Zero-Network Security, SEO & Hosting Specifications

### 7.1 Content-Security-Policy (CSP)
Enforced in `index.html` via `<meta http-equiv="Content-Security-Policy">`:
```http
default-src 'self';
script-src 'self' 'unsafe-inline';
style-src 'self' 'unsafe-inline';
img-src 'self' data:;
font-src 'self';
connect-src 'self';
```
This physically guarantees that the website never initiates any connection to third-party endpoints, external CDNs, analytics collectors, or cloud backends.

### 7.2 OpenGraph, Twitter & Schema.org Metadata
- Branded `<title>`: `Reflex — Routines, Habits, Tasks & Focus (100% Offline Android App)`
- Canonical URL: `<link rel="canonical" href="https://bobby-99.github.io/reflex/" />`
- Schema.org Structured Data (`application/ld+json`):
  ```json
  {
    "@context": "https://schema.org",
    "@type": "SoftwareApplication",
    "name": "Reflex",
    "operatingSystem": "Android",
    "applicationCategory": "ProductivityApplication",
    "offers": {
      "@type": "Offer",
      "price": "0",
      "priceCurrency": "USD"
    },
    "license": "https://www.gnu.org/licenses/gpl-3.0.en.html"
  }
  ```
- Accessible Skip Link: `<a href="#main-content">Skip to main content</a>`
- `<noscript>` Fallback: Full semantic HTML block presenting the core architectural guarantees, features, and direct GitHub links.

---

## 8. Directory & File Manifest

### Source Code (`/src`)
```
src/
├── App.tsx                        # Root layout, theme persistence, and ambient backdrop
├── index.css                      # Tailwind v4, Lora font-face declarations, glass tokens
├── main.tsx                       # React DOM entry point
├── types.ts                       # Shared interfaces for tasks, routines, habits, and focus
├── utils/
│   └── nlpParser.ts               # TaskParser natural language tokenizer
└── components/
    ├── Header.tsx                 # Floating frosted glass navigation pill
    ├── HeroAndStory.tsx           # Unified hero narrative, feature story, and sticky phone
    ├── LiquidTimerCanvas.tsx      # Dual-wave Canvas fluid physics engine
    ├── QuickAddPlayground.tsx     # Live interactive NLP input and syntax table
    ├── PrivacyProof.tsx           # INTERNET permission audit & AndroidManifest link
    ├── DesignCraft.tsx            # Lora typography scale, swatches, and liquid demo
    ├── DownloadSection.tsx        # Direct APK GitHub Releases button & 3 install steps
    ├── Footer.tsx                 # License notices, repository links, and back-to-top
    └── Phone/
        ├── PhoneFrame.tsx         # 390x844px Android device bezel, status bar & gesture bar
        ├── PhoneBottomNav.tsx     # 72px floating frosted glass stadium tab bar (5 tabs)
        └── Screens/
            ├── RoutinesScreen.tsx # Step runner, timers, skip rest, streak heatmap
            ├── TasksScreen.tsx    # Progress card, filter chips, swipe check-off, round FAB
            ├── HabitsScreen.tsx   # Streak, daily progress ring, steppers, monthly matrix
            ├── FocusScreen.tsx    # Pomodoro/Flow, steppers, quotes, app blocking preview
            ├── CalendarScreen.tsx # Morphing week/month grid, agenda feed, "Now" divider
            └── SettingsScreen.tsx # User profile, live 3-way theme switcher, permissions hub
```

### Static Distribution for GitHub Pages (`/docs` at repository root)
```
docs/
├── .nojekyll                      # Disables Jekyll processing on GitHub Pages
├── 404.html                       # Fallback routing for SPA subpaths
├── favicon.svg / favicon.png      # Vector & raster favicons
├── index.html                     # Pre-rendered production HTML entry point
├── robots.txt                     # Crawler policy
├── screenshots/                   # Original Android 16 app UI screenshots
└── assets/
    ├── lora_*.ttf                 # Official bundled Reflex Lora fonts
    ├── index-*.css                # Minified production styles
    └── index-*.js                 # Minified production bundle
```

---

## 9. GitHub Pages Deployment Steps

1. **Commit and Push to GitHub**:
   Ensure both the source and `/docs` folder are committed to your repository's `main` branch:
   ```bash
   git add docs/ website-overview.md
   git commit -m "Deploy official Reflex website and documentation"
   git push origin main
   ```

2. **Configure GitHub Pages in Settings**:
   - In your browser, navigate to: `https://github.com/bobby-99/reflex/settings/pages`
   - Under **Build and deployment**:
     - **Source**: Select **Deploy from a branch**
     - **Branch**: Select `main`
     - **Folder**: Select `/docs`
     - Click **Save**.

3. **Verify Deployment**:
   Within approximately 60 seconds, your site will be live at:
   **https://bobby-99.github.io/reflex/**
