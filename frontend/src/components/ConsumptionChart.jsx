const W = 720;
const H = 320;
const M = { top: 20, right: 20, bottom: 52, left: 62 };

/** Rounds the largest value up to a "nice" axis maximum (1, 2, 5 x 10^n). */
function niceMax(v) {
  if (v <= 0) return 1;
  const p = 10 ** Math.floor(Math.log10(v));
  const f = v / p;
  return (f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10) * p;
}

const fmt = (v) => (v >= 10 ? v.toFixed(1) : v.toFixed(2));

/**
 * Hourly energy chart, drawn as plain SVG (no chart library).
 * hours = [{ hour: 0..23, kwh }], type = 'bar' | 'line'. OX = hour of the day, OY = energy in kWh.
 */
export default function ConsumptionChart({ hours, type = 'bar' }) {
  const plotW = W - M.left - M.right;
  const plotH = H - M.top - M.bottom;
  const max = niceMax(Math.max(0, ...hours.map((h) => h.kwh)));
  const slot = plotW / hours.length;
  const x = (i) => M.left + slot * i + slot / 2;
  const y = (v) => M.top + plotH - (v / max) * plotH;
  const ticks = [0, 1, 2, 3, 4].map((i) => (max / 4) * i);

  return (
    <svg className="chart" viewBox={`0 0 ${W} ${H}`} role="img"
         aria-label="Energy consumption per hour of the selected day">
      {ticks.map((t) => (
        <g key={t}>
          <line className="chart-grid" x1={M.left} x2={W - M.right} y1={y(t)} y2={y(t)} />
          <text className="chart-tick" x={M.left - 8} y={y(t) + 4} textAnchor="end">{fmt(t)}</text>
        </g>
      ))}
      <line className="chart-axis" x1={M.left} x2={W - M.right} y1={y(0)} y2={y(0)} />

      {type === 'bar' && hours.map((h, i) => (
        <rect key={h.hour} className="chart-bar" x={x(i) - slot * 0.35} width={slot * 0.7}
              y={y(h.kwh)} height={Math.max(0, y(0) - y(h.kwh))}>
          <title>{`${String(h.hour).padStart(2, '0')}:00 - ${h.kwh} kWh`}</title>
        </rect>
      ))}

      {type === 'line' && (
        <>
          <polyline className="chart-line" fill="none"
                    points={hours.map((h, i) => `${x(i)},${y(h.kwh)}`).join(' ')} />
          {hours.map((h, i) => (
            <circle key={h.hour} className="chart-dot" cx={x(i)} cy={y(h.kwh)} r="3.5">
              <title>{`${String(h.hour).padStart(2, '0')}:00 - ${h.kwh} kWh`}</title>
            </circle>
          ))}
        </>
      )}

      {hours.map((h, i) => (
        <text key={h.hour} className="chart-tick" x={x(i)} y={y(0) + 16} textAnchor="middle">{h.hour}</text>
      ))}
      <text className="chart-label" x={M.left + plotW / 2} y={H - 8} textAnchor="middle">Hour of the day</text>
      <text className="chart-label" transform={`translate(14 ${M.top + plotH / 2}) rotate(-90)`} textAnchor="middle">
        Energy [kWh]
      </text>
    </svg>
  );
}
