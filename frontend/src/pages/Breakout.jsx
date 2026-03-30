import { useState, useRef } from 'react';
import { api } from '../api';

const ASSET_CLASSES = [
  'COMPUTER_EQUIPMENT',
  'FURNITURE',
  'VEHICLE',
  'LEASEHOLD_IMPROVEMENT',
  'BUILDING_IMPROVEMENT',
  'LAND',
  'BUILDING',
  'MACHINERY',
];

const emptyLine = () => ({
  key: Date.now() + Math.random(),
  id: '',
  description: '',
  assetClass: 'BUILDING',
  costBasis: '',
  usefulLifeYears: '',
});

const TEMPLATES = {
  property: [
    { description: 'Land', assetClass: 'LAND', pct: 20, usefulLifeYears: 0 },
    { description: 'Building structure', assetClass: 'BUILDING', pct: 50, usefulLifeYears: 39 },
    { description: 'Roof', assetClass: 'BUILDING_IMPROVEMENT', pct: 8, usefulLifeYears: 20 },
    { description: 'HVAC system', assetClass: 'BUILDING_IMPROVEMENT', pct: 10, usefulLifeYears: 15 },
    { description: 'Electrical system', assetClass: 'BUILDING_IMPROVEMENT', pct: 5, usefulLifeYears: 15 },
    { description: 'Plumbing', assetClass: 'BUILDING_IMPROVEMENT', pct: 4, usefulLifeYears: 15 },
    { description: 'Parking lot / paving', assetClass: 'LEASEHOLD_IMPROVEMENT', pct: 3, usefulLifeYears: 15 },
  ],
  office_buildout: [
    { description: 'Interior walls & framing', assetClass: 'LEASEHOLD_IMPROVEMENT', pct: 30, usefulLifeYears: 15 },
    { description: 'Flooring', assetClass: 'LEASEHOLD_IMPROVEMENT', pct: 15, usefulLifeYears: 10 },
    { description: 'Electrical & lighting', assetClass: 'BUILDING_IMPROVEMENT', pct: 20, usefulLifeYears: 15 },
    { description: 'HVAC modifications', assetClass: 'BUILDING_IMPROVEMENT', pct: 15, usefulLifeYears: 15 },
    { description: 'Furniture & fixtures', assetClass: 'FURNITURE', pct: 12, usefulLifeYears: 7 },
    { description: 'IT / network cabling', assetClass: 'COMPUTER_EQUIPMENT', pct: 8, usefulLifeYears: 5 },
  ],
};

export default function Breakout() {
  const errorRef = useRef(null);
  const [parentDesc, setParentDesc] = useState('');
  const [parentCost, setParentCost] = useState('');
  const [inServiceDate, setInServiceDate] = useState('2026-01-01');
  const [idPrefix, setIdPrefix] = useState('');
  const [lines, setLines] = useState([emptyLine()]);
  const [results, setResults] = useState(null);
  const [error, setError] = useState('');
  const [creating, setCreating] = useState(false);

  // AI suggestion state
  const [aiText, setAiText] = useState('');
  const [aiLoading, setAiLoading] = useState(false);
  const [aiResult, setAiResult] = useState(null);
  const [aiError, setAiError] = useState('');

  const totalCost = parseFloat(parentCost) || 0;
  const allocatedCost = lines.reduce((sum, l) => sum + (parseFloat(l.costBasis) || 0), 0);
  const remaining = totalCost - allocatedCost;
  const allocationPct = totalCost > 0 ? Math.round((allocatedCost / totalCost) * 100) : 0;

  const updateLine = (key, field, value) => {
    setLines((prev) => prev.map((l) => (l.key === key ? { ...l, [field]: value } : l)));
  };

  const removeLine = (key) => {
    setLines((prev) => prev.filter((l) => l.key !== key));
  };

  const addLine = () => {
    setLines((prev) => [...prev, emptyLine()]);
  };

  const applyTemplate = (templateKey) => {
    const template = TEMPLATES[templateKey];
    if (!template) return;
    const cost = totalCost;
    const prefix = idPrefix || 'asset';
    setLines(
      template.map((t, i) => ({
        key: Date.now() + i,
        id: `${prefix}-${t.description.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`,
        description: t.description,
        assetClass: t.assetClass,
        costBasis: cost > 0 ? String(Math.round((cost * t.pct) / 100)) : '',
        usefulLifeYears: String(t.usefulLifeYears),
      }))
    );
  };

  const distributeEvenly = () => {
    if (totalCost <= 0 || lines.length === 0) return;
    const perLine = Math.floor(totalCost / lines.length);
    const remainder = totalCost - perLine * lines.length;
    setLines((prev) =>
      prev.map((l, i) => ({
        ...l,
        costBasis: String(i === 0 ? perLine + remainder : perLine),
      }))
    );
  };

  const distributeRemaining = () => {
    if (remaining <= 0 || lines.length === 0) return;
    const emptyLines = lines.filter((l) => !l.costBasis || parseFloat(l.costBasis) === 0);
    const targets = emptyLines.length > 0 ? emptyLines : lines;
    const perLine = Math.floor(remaining / targets.length);
    const extra = remaining - perLine * targets.length;
    let idx = 0;
    setLines((prev) =>
      prev.map((l) => {
        const isTarget = targets.some((t) => t.key === l.key);
        if (!isTarget) return l;
        const amount = parseFloat(l.costBasis) || 0;
        const add = idx === 0 ? perLine + extra : perLine;
        idx++;
        return { ...l, costBasis: String(amount + add) };
      })
    );
  };

  const handleCreate = async () => {
    setError('');
    setResults(null);

    const invalid = lines.filter((l) => !l.id || !l.description || !l.costBasis || (!l.usefulLifeYears && l.usefulLifeYears !== '0' && l.usefulLifeYears !== 0));
    if (invalid.length > 0) {
      setError('All component rows must have an ID, description, cost, and useful life.');
      setTimeout(() => errorRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' }), 50);
      return;
    }

    if (totalCost > 0 && Math.abs(remaining) > 0.01) {
      if (!window.confirm(`Cost allocation is off by $${Math.abs(remaining).toFixed(2)}. Create anyway?`)) {
        return;
      }
    }

    setCreating(true);
    const outcomes = [];
    for (const line of lines) {
      try {
        const asset = await api.createAsset({
          id: line.id,
          description: line.description,
          assetClass: line.assetClass,
          costBasis: parseFloat(line.costBasis),
          inServiceDate,
          usefulLifeYears: parseInt(line.usefulLifeYears, 10),
        });
        outcomes.push({ id: line.id, status: 'success', asset });
      } catch (err) {
        outcomes.push({ id: line.id, status: 'error', message: err.message });
      }
    }
    setResults(outcomes);
    setCreating(false);
  };

  const formatCurrency = (n) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(n);

  const classLabel = (cls) =>
    cls
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/^\w/, (c) => c.toUpperCase());

  const handleAiSuggest = async () => {
    if (!aiText.trim()) return;
    setAiError('');
    setAiResult(null);
    setAiLoading(true);
    try {
      const result = await api.suggestBreakout(aiText);
      setAiResult(result);
    } catch (err) {
      setAiError(err.message || 'AI suggestion failed');
    } finally {
      setAiLoading(false);
    }
  };

  const applyAiSuggestion = () => {
    if (!aiResult || !aiResult.components) return;
    const cost = totalCost;
    const prefix = idPrefix || 'asset';
    setLines(
      aiResult.components.map((comp, i) => ({
        key: Date.now() + i,
        id: `${prefix}-${comp.description.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/-$/, '')}`,
        description: comp.description,
        assetClass: comp.assetClass,
        costBasis: cost > 0 ? String(Math.round((cost * comp.costPercentage) / 100)) : '',
        usefulLifeYears: String(comp.usefulLifeYears),
      }))
    );
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h2>Asset Breakout</h2>
          <p className="text-muted">Split a purchase into component assets</p>
        </div>
      </div>

      {error && <div className="alert alert-error" ref={errorRef}>{error}</div>}

      {/* Parent Purchase */}
      <div className="card">
        <h3 className="card-title">Purchase Details</h3>
        <div className="form-row">
          <div className="form-group" style={{ flex: 2 }}>
            <label>Description</label>
            <input
              value={parentDesc}
              onChange={(e) => setParentDesc(e.target.value)}
              placeholder="e.g. 123 Main St commercial property"
            />
          </div>
          <div className="form-group">
            <label>Total Cost ($)</label>
            <input
              type="number"
              step="0.01"
              min="0"
              value={parentCost}
              onChange={(e) => setParentCost(e.target.value)}
              placeholder="500000"
            />
          </div>
        </div>
        <div className="form-row">
          <div className="form-group">
            <label>In-Service Date</label>
            <input
              type="date"
              value={inServiceDate}
              onChange={(e) => setInServiceDate(e.target.value)}
            />
          </div>
          <div className="form-group">
            <label>ID Prefix</label>
            <input
              value={idPrefix}
              onChange={(e) => setIdPrefix(e.target.value)}
              placeholder="e.g. property-123"
            />
          </div>
        </div>

        {/* Quick Templates */}
        <div className="template-bar">
          <span className="template-label">Quick templates:</span>
          <button className="chip" type="button" onClick={() => applyTemplate('property')}>
            Commercial Property
          </button>
          <button className="chip" type="button" onClick={() => applyTemplate('office_buildout')}>
            Office Build-out
          </button>
        </div>
      </div>

      {/* AI Breakout Suggestion */}
      <div className="card">
        <h3 className="card-title">AI Breakout Suggestion</h3>
        <p className="text-muted" style={{ marginBottom: '0.75rem' }}>
          Paste an invoice description and let AI suggest a component breakdown.
        </p>
        <div className="form-group">
          <textarea
            rows={4}
            value={aiText}
            onChange={(e) => setAiText(e.target.value)}
            placeholder="e.g. Purchase of 3-story commercial office building at 500 Main St, includes HVAC, electrical, plumbing, parking lot, landscaping..."
            style={{ width: '100%', resize: 'vertical' }}
          />
        </div>
        <button
          className="btn btn-primary"
          onClick={handleAiSuggest}
          disabled={aiLoading || !aiText.trim()}
          style={{ marginTop: '0.5rem' }}
        >
          {aiLoading ? 'Analyzing…' : 'Suggest Breakout'}
        </button>

        {aiError && <div className="alert alert-error" style={{ marginTop: '0.75rem' }}>{aiError}</div>}

        {aiResult && (
          <div className="result-card" style={{ marginTop: '1rem' }}>
            <div className="result-summary">
              <div className="result-grid">
                <div className="result-item">
                  <span className="result-label">Components</span>
                  <span className="result-value">{aiResult.components?.length || 0}</span>
                </div>
                <div className="result-item">
                  <span className="result-label">Confidence</span>
                  <span className={`confidence-badge ${aiResult.confidence >= 0.8 ? 'confidence-high' : aiResult.confidence >= 0.6 ? 'confidence-medium' : 'confidence-low'}`}>
                    {Math.round(aiResult.confidence * 100)}%
                  </span>
                </div>
                <div className="result-item">
                  <span className="result-label">Source</span>
                  <span className={`source-badge ${aiResult.source === 'AI_GROQ' ? 'source-ai' : 'source-rule'}`}>
                    {aiResult.source === 'AI_GROQ' ? 'AI' : 'Rules'}
                  </span>
                </div>
              </div>
              <div className="rationale-block">
                <span className="result-label">Rationale</span>
                <p>{aiResult.rationale}</p>
              </div>

              {aiResult.components && aiResult.components.length > 0 && (
                <div style={{ marginTop: '1rem' }}>
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Component</th>
                        <th>Asset Class</th>
                        <th>Cost %</th>
                        {totalCost > 0 && <th>Est. Cost</th>}
                        <th>Useful Life</th>
                      </tr>
                    </thead>
                    <tbody>
                      {aiResult.components.map((comp, i) => (
                        <tr key={i}>
                          <td>{comp.description}</td>
                          <td><span className="badge">{classLabel(comp.assetClass)}</span></td>
                          <td>{comp.costPercentage}%</td>
                          {totalCost > 0 && <td>{formatCurrency(Math.round((totalCost * comp.costPercentage) / 100))}</td>}
                          <td>{comp.usefulLifeYears > 0 ? `${comp.usefulLifeYears} yrs` : 'N/A (land)'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  <button
                    className="btn btn-primary"
                    onClick={applyAiSuggestion}
                    style={{ marginTop: '0.75rem' }}
                  >
                    Apply Suggestion to Components
                  </button>
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Allocation Bar */}
      {totalCost > 0 && (
        <div className="allocation-bar">
          <div className="allocation-track">
            <div
              className={`allocation-fill ${allocationPct > 100 ? 'allocation-over' : allocationPct === 100 ? 'allocation-exact' : ''}`}
              style={{ width: `${Math.min(allocationPct, 100)}%` }}
            />
          </div>
          <div className="allocation-info">
            <span>
              {formatCurrency(allocatedCost)} of {formatCurrency(totalCost)} allocated ({allocationPct}%)
            </span>
            {remaining > 0.01 && (
              <span className="allocation-remaining">{formatCurrency(remaining)} unallocated</span>
            )}
            {remaining < -0.01 && (
              <span className="allocation-over-text">{formatCurrency(Math.abs(remaining))} over-allocated</span>
            )}
          </div>
        </div>
      )}

      {/* Component Lines */}
      <div className="card">
        <div className="breakout-header">
          <h3 className="card-title">Components</h3>
          <div className="breakout-actions">
            {totalCost > 0 && (
              <>
                <button className="btn btn-sm btn-secondary" type="button" onClick={distributeEvenly}>
                  Split Evenly
                </button>
                {remaining > 0.01 && (
                  <button className="btn btn-sm btn-secondary" type="button" onClick={distributeRemaining}>
                    Fill Remaining
                  </button>
                )}
              </>
            )}
            <button className="btn btn-sm btn-primary" type="button" onClick={addLine}>
              + Add Component
            </button>
          </div>
        </div>

        <div className="breakout-lines">
          {lines.map((line, idx) => (
            <div key={line.key} className="breakout-line">
              <span className="line-number">{idx + 1}</span>
              <div className="breakout-fields">
                <div className="form-group">
                  <label>Asset ID</label>
                  <input
                    value={line.id}
                    onChange={(e) => updateLine(line.key, 'id', e.target.value)}
                    placeholder="component-id"
                  />
                </div>
                <div className="form-group" style={{ flex: 2 }}>
                  <label>Description</label>
                  <input
                    value={line.description}
                    onChange={(e) => updateLine(line.key, 'description', e.target.value)}
                    placeholder="Component description"
                  />
                </div>
                <div className="form-group">
                  <label>Class</label>
                  <select
                    value={line.assetClass}
                    onChange={(e) => updateLine(line.key, 'assetClass', e.target.value)}
                  >
                    {ASSET_CLASSES.map((c) => (
                      <option key={c} value={c}>
                        {classLabel(c)}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Cost ($)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={line.costBasis}
                    onChange={(e) => updateLine(line.key, 'costBasis', e.target.value)}
                  />
                </div>
                <div className="form-group" style={{ maxWidth: 100 }}>
                  <label>Life (yrs)</label>
                  <input
                    type="number"
                    min="0"
                    value={line.usefulLifeYears}
                    onChange={(e) => updateLine(line.key, 'usefulLifeYears', e.target.value)}
                  />
                </div>
              </div>
              <button
                className="btn btn-sm btn-danger breakout-remove"
                type="button"
                onClick={() => removeLine(line.key)}
                title="Remove component"
              >
                ✕
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* Create Button */}
      <div className="breakout-submit">
        {error && <div className="alert alert-error" style={{ marginBottom: '0.75rem' }}>{error}</div>}
        <button
          className="btn btn-primary"
          onClick={handleCreate}
          disabled={creating || lines.length === 0}
        >
          {creating ? 'Creating assets…' : `Create ${lines.length} Asset${lines.length !== 1 ? 's' : ''}`}
        </button>
      </div>

      {/* Results */}
      {results && (
        <div className="card result-card">
          <div className="result-summary">
            <h3>
              {results.every((r) => r.status === 'success')
                ? 'All assets created successfully'
                : 'Creation completed with errors'}
            </h3>
            <div className="breakout-results">
              {results.map((r) => (
                <div key={r.id} className={`breakout-result-row ${r.status}`}>
                  <span className={`status-dot ${r.status}`} />
                  <span className="font-mono">{r.id}</span>
                  {r.status === 'success' ? (
                    <span className="text-muted">Created</span>
                  ) : (
                    <span className="result-error-msg">{r.message}</span>
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
