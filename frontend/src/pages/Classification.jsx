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
          <h3>Classification Result</h3>
          <pre className="result-json">{JSON.stringify(result, null, 2)}</pre>
        </div>
      )}
    </div>
  );
}
