import { useState } from 'react';
import { api } from '../api';

const EXAMPLES = [
  'Invoice: Dell XPS 15 laptop, 32GB RAM, 1TB SSD — purchased for the accounting team',
  'Standing desk and ergonomic chair for new employee onboarding',
  'Ford Transit cargo van for field team deliveries',
  'Tenant improvement — floor 4 interior fit-out including new walls and flooring',
  'Roof replacement and HVAC system upgrade for main building',
];

export default function Classification() {
  const [documentText, setDocumentText] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setResult(null);
    setLoading(true);
    try {
      const data = await api.classifyDocument(documentText);
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
        <h2>Asset Classification</h2>
        <p className="text-muted">Classify document text into asset categories</p>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="card">
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Document Text</label>
            <textarea
              rows={4}
              value={documentText}
              onChange={(e) => setDocumentText(e.target.value)}
              placeholder="Paste invoice or purchase description…"
              required
            />
          </div>
          <div className="example-chips">
            {EXAMPLES.map((ex, i) => (
              <button
                key={i}
                type="button"
                className="chip"
                onClick={() => setDocumentText(ex)}
              >
                {ex.slice(0, 50)}…
              </button>
            ))}
          </div>
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Classifying…' : 'Classify'}
          </button>
        </form>
      </div>

      {result && (
        <div className="card result-card">
          <div className="result-summary">
            <h3>Classification Result</h3>
            <div className="result-grid">
              <div className="result-item">
                <span className="result-label">Asset Class</span>
                <span className="result-value"><span className="badge badge-lg">{(result.assetClass || '').replace(/_/g, ' ')}</span></span>
              </div>
              <div className="result-item">
                <span className="result-label">GL Code</span>
                <span className="result-value font-mono">{result.glCode}</span>
              </div>
              <div className="result-item">
                <span className="result-label">Useful Life</span>
                <span className="result-value">{result.usefulLifeYears} years</span>
              </div>
              <div className="result-item">
                <span className="result-label">Confidence</span>
                <span className="result-value">
                  <span className={`confidence-badge ${result.confidence >= 0.7 ? 'confidence-high' : result.confidence >= 0.4 ? 'confidence-med' : 'confidence-low'}`}>
                    {Math.round((result.confidence ?? 0) * 100)}%
                  </span>
                </span>
              </div>
            </div>
            {result.rationale && (
              <div className="result-rationale">
                <span className="result-label">Rationale</span>
                <p>{result.rationale}</p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
