import React, { useEffect, useRef } from 'react';

interface LiquidTimerCanvasProps {
  fillRatio: number; // 0.0 to 1.0
  phaseLabel: string;
  timeReadout: string;
  statusCaption: string;
  isDarkTheme: boolean;
  isRunning: boolean;
  size?: number;
  isBreak?: boolean;
  variant?: 'pomodoro' | 'flow';
}

export const LiquidTimerCanvas: React.FC<LiquidTimerCanvasProps> = ({
  fillRatio,
  phaseLabel,
  timeReadout,
  statusCaption,
  isDarkTheme,
  isRunning,
  size = 190,
  isBreak = false,
  variant = 'pomodoro',
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const isVisibleRef = useRef<boolean>(true);
  const animFrameRef = useRef<number | null>(null);
  const phaseRef = useRef<number>(0);
  const lastTimeRef = useRef<number>(0);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        isVisibleRef.current = entry.isIntersecting;
      },
      { threshold: 0.1 }
    );

    if (containerRef.current) {
      observer.observe(containerRef.current);
    }

    const handleVisibilityChange = () => {
      isVisibleRef.current = !document.hidden;
    };
    document.addEventListener('visibilitychange', handleVisibilityChange);

    return () => {
      observer.disconnect();
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      if (animFrameRef.current) {
        cancelAnimationFrame(animFrameRef.current);
      }
    };
  }, []);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    lastTimeRef.current = performance.now();

    const render = (now: number) => {
      const dt = Math.min((now - (lastTimeRef.current || now)) / 1000, 0.1);
      lastTimeRef.current = now;

      if (isVisibleRef.current) {
        // Phase progression matches reference: faster when running, gentle drift when idle
        const speed = isRunning ? (variant === 'flow' ? 2.6 : 2.4) : (variant === 'flow' ? 1.3 : 1.1);
        phaseRef.current += dt * speed;

        const dpr = window.devicePixelRatio || 1;
        const S = size;

        if (canvas.width !== Math.round(S * dpr) || canvas.height !== Math.round(S * dpr)) {
          canvas.width = Math.round(S * dpr);
          canvas.height = Math.round(S * dpr);
          canvas.style.width = `${S}px`;
          canvas.style.height = `${S}px`;
        }

        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        ctx.clearRect(0, 0, S, S);

        // Exact Design System & Reference Tokens
        const surfaceCol = isDarkTheme ? '#141211' : '#FFFFFF';
        const borderCol = isDarkTheme ? '#2E2A27' : 'rgba(26, 22, 20, 0.08)';
        const inkCol = isDarkTheme ? '#F5F2EF' : '#1A1614';
        const onlCol = isDarkTheme ? '#0A0908' : '#FFFFFF';
        const topCol = isBreak
          ? (isDarkTheme ? '#A6D8BF' : '#7FB39A')
          : (isDarkTheme ? '#E1AD93' : '#C98460');
        const botCol = isBreak
          ? (isDarkTheme ? '#5E9C7E' : '#4F8A6B')
          : (isDarkTheme ? '#B57E63' : '#A5623F');
        const glareCol = isDarkTheme ? 'rgba(255, 255, 255, 0.22)' : 'rgba(255, 255, 255, 0.55)';

        const cx = S / 2;
        const cy = S / 2;
        const r = S / 2 - 4;
        const ratio = Math.max(0, Math.min(1, fillRatio));

        // Exact fluid wave mechanics from reference
        const amp = S * 0.02 * Math.min(1, ratio * 8);
        const level = (cy + r + amp * 3) - ratio * (2 * r + amp * 6);
        const tilt = Math.sin(now / 1100) * amp * 0.55;
        const k = (Math.PI * 2) / (S * 0.85);

        const wave = (ph: number, a: number, off: number) => {
          ctx.beginPath();
          for (let x = cx - r - 2; x <= cx + r + 2; x += 4) {
            const y = level + off + a * Math.sin(x * k + ph) + a * 0.3 * Math.sin(x * k * 2.1 - ph * 1.3) + tilt * (x - cx) / r;
            if (x === cx - r - 2) ctx.moveTo(x, y);
            else ctx.lineTo(x, y);
          }
          ctx.lineTo(cx + r + 2, cy + r + 6);
          ctx.lineTo(cx - r - 2, cy + r + 6);
          ctx.closePath();
        };

        const circ = () => {
          ctx.beginPath();
          ctx.arc(cx, cy, r, 0, Math.PI * 2);
        };

        // 1. Sphere background & border
        circ();
        ctx.fillStyle = surfaceCol;
        ctx.fill();
        ctx.lineWidth = 1.5;
        ctx.strokeStyle = borderCol;
        ctx.stroke();

        // 2. Liquid fill with dual waves
        const g = ctx.createLinearGradient(0, level - amp, 0, cy + r);
        g.addColorStop(0, topCol);
        g.addColorStop(1, botCol);

        ctx.save();
        circ();
        ctx.clip();

        // Back wave (semi-transparent)
        ctx.globalAlpha = 0.5;
        wave(phaseRef.current + 1.8, amp * 1.2, amp * 0.4);
        ctx.fillStyle = g;
        ctx.fill();

        // Front wave (full opacity)
        ctx.globalAlpha = 1.0;
        wave(phaseRef.current, amp, 0);
        ctx.fillStyle = g;
        ctx.fill();

        ctx.restore();

        // 3. Tabular Typography with exact dual-tone clipping
        const fs = Math.round(S * 0.26);
        const lf = Math.round(Math.max(12, S * 0.055));
        const df = `700 ${fs}px Lora, Georgia, serif`;
        ctx.font = df;

        let cell = 0;
        for (const d of '0123456789') {
          cell = Math.max(cell, ctx.measureText(d).width);
        }
        const colon = ctx.measureText(':').width;
        const str = timeReadout;
        const totalTextWidth = [...str].reduce((a, c) => a + (c === ':' ? colon : cell), 0);

        const paintText = (col: string) => {
          ctx.fillStyle = col;
          ctx.textAlign = 'center';
          ctx.textBaseline = 'middle';
          ctx.font = df;
          let x = cx - totalTextWidth / 2;
          for (const ch of str) {
            const w = ch === ':' ? colon : cell;
            ctx.fillText(ch, x + w / 2, cy);
            x += w;
          }
          ctx.font = `500 ${lf}px Lora, Georgia, serif`;
          ctx.globalAlpha = 0.85;
          ctx.fillText(phaseLabel, cx, cy - fs * 0.78);
          ctx.fillText(statusCaption, cx, cy + fs * 0.78);
          ctx.globalAlpha = 1.0;
        };

        // Pass 1: Draw text on top of base surface in ink color
        paintText(inkCol);

        // Pass 2: Clip to the water wave and re-paint text in onl (contrast color)
        ctx.save();
        circ();
        ctx.clip();
        wave(phaseRef.current, amp, 0);
        ctx.clip();
        paintText(onlCol);
        ctx.restore();

        // 4. Glare crescent arc on top-left rim
        ctx.beginPath();
        ctx.arc(cx, cy, r - 9, Math.PI * 1.08, Math.PI * 1.38);
        ctx.strokeStyle = glareCol;
        ctx.lineWidth = 3;
        ctx.lineCap = 'round';
        ctx.stroke();
      }

      animFrameRef.current = requestAnimationFrame(render);
    };

    animFrameRef.current = requestAnimationFrame(render);

    return () => {
      if (animFrameRef.current) {
        cancelAnimationFrame(animFrameRef.current);
      }
    };
  }, [fillRatio, isBreak, isDarkTheme, isRunning, phaseLabel, size, statusCaption, timeReadout, variant]);

  return (
    <div
      ref={containerRef}
      className="relative flex items-center justify-center select-none"
      style={{ width: size, height: size }}
    >
      <canvas
        ref={canvasRef}
        style={{ width: size, height: size }}
        className="block"
      />
    </div>
  );
};
