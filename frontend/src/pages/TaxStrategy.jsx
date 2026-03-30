import { useState } from 'react';
import { api } from '../api';

export default function TaxStrategy() {
  const [form, setForm] = useState({
    stateCode: 'CA',
    equipmentType: '',
    immediateDeductionPreferred: true,
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
      const data = await api.recommendTaxStrategy(form);
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
        <h2>Tax Strategy</h2>
        <p className="text-muted">Get AI-powered tax strategy recommendations</p>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="card">
        <form onSubmit={handleSubmit}>
          <div className="form-row">
            <div className="form-group">
              <label>State Code</label>
              <input
                value={form.stateCode}
                onChange={(e) => setField('stateCode', e.target.value)}
                placeholder="CA"
                maxLength={2}
                required
              />
            </div>
            <div className="form-group">
              <label>Equipment Type</label>
              <input
                value={form.equipmentType}
                onChange={(e) => setField('equipmentType', e.target.value)}
                placeholder="e.g. server rack, cargo van"
                required
              />
            </div>
          </div>
          <div className="form-row">
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={form.immediateDeductionPreferred}
                onChange={(e) => setField('immediateDeductionPreferred', e.target.checked)}
              />
              Prefer immediate deduction
            </label>
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={form.longHorizonAsset}
                onChange={(e) => setField('longHorizonAsset', e.target.checked)}
              />
              Long-horizon asset
            </label>
          </div>
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Analyzing…' : 'Get Recommendation'}
          </button>
        </form>
      </div>

      {result && (
        <div className="card result-card">
          <h3>Recommendation</h3>
          <pre className="result-json">{JSON.stringify(result, null, 2)}</pre>
        </div>
      )}
    </div>
  );
}
