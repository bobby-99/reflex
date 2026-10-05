import React, { useEffect, useRef } from 'react';

interface LiquidTimerCanvasProps {
  fillRatio: number; // 0.0 to 1.0
  phaseLabel: string;
  timeReadout: string;
  statusCaption: string;
  isDarkTheme: boolean;
  isRunning: boolean;
  size?: number;
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
  variant = 'pomodoro',
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const isVisibleRef = useRef<boolean>(true);
  const animFrameRef = useRef<number | null>(null);
  const phaseRef = useRef<number>(0);

  useEffect(() => {
    // Visibility observer to pause RAF when off-screen
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

    let lastTime = performance.now();

    const render = (now: number) => {
      const dt = (now - lastTime) / 1000;
      lastTime = now;

      if (isVisibleRef.current) {
        // Advance wave phase: faster when running, gentle drift when idle
        const speed = isRunning ? 2.4 : 1.1;
        phaseRef.current += speed * dt;

        const dpr = window.devicePixelRatio || 1;
        const w = size;
        const h = size;

        // Ensure canvas internal resolution matches device pixel ratio
        if (canvas.width !== w * dpr || canvas.height !== h * dpr) {
          canvas.width = w * dpr;
          canvas.height = h * dpr;
        }

        ctx.save();
        ctx.scale(dpr, dpr);
        ctx.clearRect(0, 0, w, h);

        const cx = w / 2;
        const cy = h / 2;
        const radius = (w / 2) - 8;

        // 1. Outer Glass Sphere Background
        ctx.save();
        ctx.beginPath();
        ctx.arc(cx, cy, radius, 0, Math.PI * 2);
        ctx.fillStyle = isDarkTheme ? 'rgba(28, 26, 23, 0.72)' : 'rgba(255, 255, 255, 0.85)';
        ctx.fill();

        // 2. Liquid Waves clipped to the inner circle
        ctx.clip();

        // Clamp fill height: 0.0 means empty bottom, 1.0 means full top
        const clampedFill = Math.max(0.04, Math.min(0.96, fillRatio));
        const fillY = cy + radius - (clampedFill * 2 * radius);
        const amp = isRunning ? (variant === 'flow' ? 8.5 : 7) : (variant === 'flow' ? 5.5 : 4.5);
        const freq = variant === 'flow' ? 0.032 : 0.038;

        const getWaveY = (x: number, isBack: boolean) => {
          if (variant === 'flow') {
            const p = isBack ? phaseRef.current + Math.PI * 0.75 : phaseRef.current;
            const w1 = amp * Math.sin(freq * 1.15 * x + p);
            const w2 = (amp * 0.4) * Math.sin(freq * 2.2 * x - p * 0.7);
            const w3 = (amp * 0.2) * Math.cos(freq * 0.6 * x + p * 1.3);
            return fillY + w1 + w2 + w3;
          }
          return fillY + amp * Math.sin(freq * x + phaseRef.current + (isBack ? Math.PI * 0.7 : 0));
        };

        // Layer 1: Back Wave (slightly darker copper tone)
        ctx.beginPath();
        ctx.moveTo(0, h);
        for (let x = 0; x <= w; x += 3) {
          ctx.lineTo(x, getWaveY(x, true));
        }
        ctx.lineTo(w, h);
        ctx.closePath();
        ctx.fillStyle = isDarkTheme ? 'rgba(181, 126, 99, 0.45)' : 'rgba(143, 76, 43, 0.35)';
        ctx.fill();

        // Layer 2: Front Wave (bright metallic copper)
        ctx.beginPath();
        ctx.moveTo(0, h);
        for (let x = 0; x <= w; x += 3) {
          ctx.lineTo(x, getWaveY(x, false));
        }
        ctx.lineTo(w, h);
        ctx.closePath();

        const grad = ctx.createLinearGradient(0, fillY - 10, 0, h);
        if (isDarkTheme) {
          grad.addColorStop(0, '#D9A184');
          grad.addColorStop(1, '#945A3C');
        } else {
          grad.addColorStop(0, '#8F4C2B');
          grad.addColorStop(1, '#5E311B');
        }
        ctx.fillStyle = grad;
        ctx.fill();

        // Wave crest highlight rim
        ctx.beginPath();
        for (let x = 0; x <= w; x += 3) {
          const y = getWaveY(x, false);
          if (x === 0) ctx.moveTo(x, y);
          else ctx.lineTo(x, y);
        }
        ctx.strokeStyle = isDarkTheme ? 'rgba(255, 240, 230, 0.45)' : 'rgba(255, 255, 255, 0.6)';
        ctx.lineWidth = 1.5;
        ctx.stroke();

        ctx.restore(); // Exit clip

        // 3. Glass Sphere Hairline Rim & Subtle Specular Highlight
        ctx.beginPath();
        ctx.arc(cx, cy, radius, 0, Math.PI * 2);
        ctx.strokeStyle = isDarkTheme ? 'rgba(217, 161, 132, 0.32)' : 'rgba(143, 76, 43, 0.28)';
        ctx.lineWidth = 1.5;
        ctx.stroke();

        // Inner shadow / vignette
        const innerGrad = ctx.createRadialGradient(cx, cy, radius * 0.7, cx, cy, radius);
        innerGrad.addColorStop(0, 'rgba(0,0,0,0)');
        innerGrad.addColorStop(1, isDarkTheme ? 'rgba(0,0,0,0.35)' : 'rgba(0,0,0,0.08)');
        ctx.beginPath();
        ctx.arc(cx, cy, radius, 0, Math.PI * 2);
        ctx.fillStyle = innerGrad;
        ctx.fill();

        ctx.restore();
      }

      animFrameRef.current = requestAnimationFrame(render);
    };

    animFrameRef.current = requestAnimationFrame(render);

    return () => {
      if (animFrameRef.current) {
        cancelAnimationFrame(animFrameRef.current);
      }
    };
  }, [fillRatio, isDarkTheme, isRunning, size, variant]);

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

      {/* Central Readable Readout Overlay (Positioned with CSS for absolute accessibility) */}
      <div className="absolute inset-0 flex flex-col items-center justify-center text-center pointer-events-none px-4">
        <span
          className={`text-[12px] font-medium tracking-wide transition-colors ${
            isDarkTheme ? 'text-[#F5F2EF]/90' : 'text-[#1A1614]/90'
          }`}
          style={{ textShadow: isDarkTheme ? '0 1px 3px rgba(0,0,0,0.85)' : '0 1px 2px rgba(255,255,255,0.85)' }}
        >
          {phaseLabel}
        </span>
        <span
          className={`text-[36px] font-bold leading-none my-1 tracking-tight tnum transition-colors ${
            isDarkTheme ? 'text-[#FFFFFF]' : 'text-[#1A1614]'
          }`}
          style={{ textShadow: isDarkTheme ? '0 2px 6px rgba(0,0,0,0.9)' : '0 1px 3px rgba(255,255,255,0.9)' }}
        >
          {timeReadout}
        </span>
        <span
          className={`text-[11px] font-medium transition-colors ${
            isDarkTheme ? 'text-[#D9A184]' : 'text-[#8F4C2B]'
          }`}
          style={{ textShadow: isDarkTheme ? '0 1px 2px rgba(0,0,0,0.8)' : '0 1px 1px rgba(255,255,255,0.8)' }}
        >
          {statusCaption}
        </span>
      </div>
    </div>
  );
};
