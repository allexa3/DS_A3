import { useEffect, useState } from 'react';
import { api, describeError } from '../api.js';
import { useAuth } from '../auth.jsx';
import Shell from '../components/Shell.jsx';
import ConsumptionChart from '../components/ConsumptionChart.jsx';
import { Alert, Field, LoadBar } from '../components/ui.jsx';

/** Today in the browser's time zone as yyyy-MM-dd (the value format of <input type="date">). */
const today = () => new Date().toLocaleDateString('en-CA');

/** Historical energy consumption of one device for one day, chosen from a calendar. */
function ConsumptionPanel({ devices }) {
  const [deviceId, setDeviceId] = useState(devices[0].id);
  const [date, setDate] = useState(today());
  const [type, setType] = useState('bar');
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!deviceId || !date) return undefined;
    let cancelled = false;
    setLoading(true);
    setError('');
    api.monitoring.hourly(deviceId, date)
      .then((d) => { if (!cancelled) setData(d); })
      .catch((e) => { if (!cancelled) { setData(null); setError(describeError(e)); } })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [deviceId, date]);

  const total = data ? data.hours.reduce((sum, h) => sum + h.kwh, 0) : 0;

  return (
    <section className="editor">
      <h2>Energy consumption</h2>
      <div className="grid">
        <Field label="Device">
          <select value={deviceId} onChange={(e) => setDeviceId(e.target.value)}>
            {devices.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
          </select>
        </Field>
        <Field label="Day">
          <input type="date" value={date} max={today()} onChange={(e) => setDate(e.target.value)} />
        </Field>
        <Field label="Chart">
          <select value={type} onChange={(e) => setType(e.target.value)}>
            <option value="bar">Bar chart</option>
            <option value="line">Line chart</option>
          </select>
        </Field>
      </div>
      <Alert>{error}</Alert>
      {loading && <p className="muted">Loading…</p>}
      {data && !error && (
        <>
          <ConsumptionChart hours={data.hours} type={type} />
          <p className="muted small">
            Total for {data.date}: <strong>{total.toFixed(2)} kWh</strong> (hours in {data.timeZone})
            {total === 0 && ' – no measurements were received for this day.'}
          </p>
        </>
      )}
    </section>
  );
}

export default function ClientPage() {
  const { session } = useAuth();
  const [profile, setProfile] = useState(null);
  const [devices, setDevices] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.users.get(session.userId).then(setProfile).catch(() => {});
    api.devices.ofUser(session.userId).then(setDevices).catch((e) => setError(describeError(e)));
  }, [session.userId]);

  const max = Math.max(0, ...(devices || []).map((d) => d.maxConsumption));

  return (
    <Shell title="My devices">
      {profile && <p className="muted">{profile.name} · {profile.email}</p>}
      <Alert>{error}</Alert>
      {devices === null && !error && <p className="muted">Loading devices…</p>}
      {devices && devices.length === 0 && (
        <p className="empty">No devices are assigned to your account yet. Ask an administrator to assign one.</p>
      )}
      {devices && devices.length > 0 && (
        <>
          <ul className="device-list">
            {devices.map((d) => (
              <li key={d.id}>
                <div>
                  <strong>{d.name}</strong>
                  {d.description && <p className="muted">{d.description}</p>}
                </div>
                <div>
                  <span className="muted small">Maximum consumption</span>
                  <LoadBar value={d.maxConsumption} max={max} />
                </div>
              </li>
            ))}
          </ul>
          <ConsumptionPanel devices={devices} />
        </>
      )}
    </Shell>
  );
}
