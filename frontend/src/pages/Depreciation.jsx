import { useState } from 'react';
import { api } from '../api';

const METHODS = ['STRAIGHT_LINE', 'DECLINING_BALANCE', 'DOUBLE_DECLINING', 'SUM_OF_YEARS', 'MACRS'];
const BOOK_TYPES = ['BOOK', 'TAX'];

export default function Depreciation() {
  const [mode, setMode] = useState('run');
  const [form, setForm] = useState({
    assetId: '',
    bookType: 'TAX',
    method: 'STRAIGHT_LINE',
    assetClass: 'COMPUTER_EQUIPMENT',
    inServiceDate: '2026-01-01',
    costBasis: '10000',
    salvageValue: '0',
    usefulLifeYears: '5',
    section179Enabled: false,
    section179Amount: '0',
    bonusDepreciationRate: '0',
    stateCode: 'CA',
    equipmentType: '',
    immediateDeductionPreferred: false,
    longHorizonAsset: false,
  });
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const setField = (key, val) => setForm((f) => ({ ...f, [key]: val }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setResult(null);
    setLoading(true);
    try {
      let data;
      const numericForm = {
        ...form,
        costBasis: parseFloat(form.costBasis),
        salvageValue: parseFloat(form.salvageValue),
        usefulLifeYears: parseInt(form.usefulLifeYears, 10),
        section179Amount: parseFloat(form.section179Amount),
        bonusDepreciationRate: parseFloat(form.bonusDepreciationRate),
      };
      if (mode === 'run') {
        data = await api.runDepreciation(numericForm);
      } else if (mode === 'recommend') {
        data = await api.recommendDepreciation({
          stateCode: form.stateCode,
          equipmentType: form.equipmentType,
          assetClass: form.assetClass,
          immediateDeductionPreferred: form.immediateDeductionPreferred,
          longHorizonAsset: form.longHorizonAsset,
        });
      } else {
        data = await api.aiRunDepreciation(numericForm);
      }
      setResult(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page">
      <div className="page-header">
        <h2>Depreciation</h2>
      </div>

      <div className="tabs">
        {[
          ['run', 'Manual Run'],
          ['recommend', 'AI Recommend'],
          ['ai-run', 'AI Full Schedule'],
        ].map(([key, label]) => (
          <button
            key={key}
            className={`tab ${mode === key ? 'tab-active' : ''}`}
            onClick={() => { setMode(key); setResult(null); setError(''); }}
          >
            {label}
          </button>
        ))}
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="card">
        <form onSubmit={handleSubmit}>
          {(mode === 'run' || mode === 'ai-run') && (
            <>
              <div className="form-row">
                <div className="form-group">
                  <label>Asset ID</label>
                  <input value={form.assetId} onChange={(e) => setField('assetId', e.target.value)} required />
                </div>
                <div className="form-group">
                  <label>Book Type</label>
                  <select value={form.bookType} onChange={(e) => setField('bookType', e.target.value)}>
                    {BOOK_TYPES.map((b) => <option key={b} value={b}>{b}</option>)}
                  </select>
                </div>
              </div>
              {mode === 'run' && (
                <div className="form-group">
                  <label>Method</label>
                  <select value={form.method} onChange={(e) => setField('method', e.target.value)}>
                    {METHODS.map((m) => <option key={m} value={m}>{m.replace(/_/g, ' ')}</option>)}
                  </select>
                </div>
              )}
              <div className="form-row">
                <div className="form-group">
                  <label>Cost Basis ($)</label>
                  <input type="number" step="0.01" value={form.costBasis} onChange={(e) => setField('costBasis', e.target.value)} required />
                </div>
                <div className="form-group">
                  <label>Salvage Value ($)</label>
                  <input type="number" step="0.01" value={form.salvageValue} onChange={(e) => setField('salvageValue', e.target.value)} />
                </div>
                <div className="form-group">
                  <label>Useful Life (years)</label>
                  <input type="number" min="1" value={form.usefulLifeYears} onChange={(e) => setField('usefulLifeYears', e.target.value)} required />
                </div>
              </div>
              <div className="form-group">
                <label>In-Service Date</label>
                <input type="date" value={form.inServiceDate} onChange={(e) => setField('inServiceDate', e.target.value)} required />
              </div>
            </>
          )}

          {(mode === 'recommend' || mode === 'ai-run') && (
            <>
              <div className="form-row">
                <div className="form-group">
                  <label>State Code</label>
                  <input value={form.stateCode} onChange={(e) => setField('stateCode', e.target.value)} placeholder="CA" maxLength={2} required />
                </div>
                <div className="form-group">
                  <label>Equipment Type</label>
                  <input value={form.equipmentType} onChange={(e) => setField('equipmentType', e.target.value)} placeholder="Dell XPS 15 laptop" required />
                </div>
              </div>
              <div className="form-group">
                <label>Asset Class</label>
                <select value={form.assetClass} onChange={(e) => setField('assetClass', e.target.value)}>
                  {['COMPUTER_EQUIPMENT', 'FURNITURE', 'VEHICLE', 'LEASEHOLD_IMPROVEMENT', 'BUILDING_IMPROVEMENT'].map((c) => (
                    <option key={c} value={c}>{c.replace(/_/g, ' ')}</option>
                  ))}
                </select>
              </div>
              <div className="form-row">
                <label className="checkbox-label">
                  <input type="checkbox" checked={form.immediateDeductionPreferred} onChange={(e) => setField('immediateDeductionPreferred', e.target.checked)} />
                  Prefer immediate deduction
                </label>
                <label className="checkbox-label">
                  <input type="checkbox" checked={form.longHorizonAsset} onChange={(e) => setField('longHorizonAsset', e.target.checked)} />
                  Long-horizon asset
                </label>
              </div>
            </>
          )}

          {(mode === 'run' || mode === 'ai-run') && (
            <div className="form-row">
              <label className="checkbox-label">
                <input type="checkbox" checked={form.section179Enabled} onChange={(e) => setField('section179Enabled', e.target.checked)} />
                Section 179
              </label>
              <div className="form-group">
                <label>§179 Amount</label>
                <input type="number" step="0.01" value={form.section179Amount} onChange={(e) => setField('section179Amount', e.target.value)} />
              </div>
              <div className="form-group">
                <label>Bonus Deprec. Rate</label>
                <input type="number" step="0.01" min="0" max="1" value={form.bonusDepreciationRate} onChange={(e) => setField('bonusDepreciationRate', e.target.value)} />
              </div>
            </div>
          )}

          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Running…' : mode === 'recommend' ? 'Get Recommendation' : 'Calculate'}
          </button>
        </form>
      </div>

      {result && (
        <div className="card result-card">
          <h3>Result</h3>
          <pre className="result-json">{JSON.stringify(result, null, 2)}</pre>
        </div>
      )}
    </div>
  );
}
