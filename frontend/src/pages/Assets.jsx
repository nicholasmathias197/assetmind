import { useState, useEffect, useCallback } from 'react';
import { api } from '../api';

const ASSET_CLASSES = [
  'COMPUTER_EQUIPMENT',
  'FURNITURE',
  'VEHICLE',
  'LEASEHOLD_IMPROVEMENT',
  'BUILDING_IMPROVEMENT',
];

const emptyAsset = {
  id: '',
  description: '',
  assetClass: 'COMPUTER_EQUIPMENT',
  costBasis: '',
  inServiceDate: '',
  usefulLifeYears: '',
};

export default function Assets() {
  const [assets, setAssets] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [filterClass, setFilterClass] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({ ...emptyAsset });
  const [busyAction, setBusyAction] = useState('');

  const loadAssets = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.getAssetsPaged(page, 20, 'id', 'asc', filterClass || undefined);
      setAssets(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [page, filterClass]);

  useEffect(() => {
    loadAssets();
  }, [loadAssets]);

  const handleFilterChange = (cls) => {
    setFilterClass(cls);
    setPage(0);
  };

  const openCreate = () => {
    setForm({ ...emptyAsset });
    setEditing(false);
    setShowForm(true);
  };

  const openEdit = (asset) => {
    setForm({ ...asset, costBasis: String(asset.costBasis), usefulLifeYears: String(asset.usefulLifeYears) });
    setEditing(true);
    setShowForm(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    const payload = {
      ...form,
      costBasis: parseFloat(form.costBasis),
      usefulLifeYears: parseInt(form.usefulLifeYears, 10),
    };
    try {
      if (editing) {
        await api.updateAsset(form.id, payload);
      } else {
        await api.createAsset(payload);
      }
      setShowForm(false);
      loadAssets();
    } catch (err) {
      setError(err.message);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm(`Delete asset "${id}"?`)) return;
    try {
      await api.deleteAsset(id);
      loadAssets();
    } catch (err) {
      setError(err.message);
    }
  };

  const handleExportExcel = async () => {
    try {
      setBusyAction('export');
      const blob = await api.exportAssetsExcel(filterClass || undefined);
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = `assets-${new Date().toISOString().slice(0, 10)}.xlsx`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyAction('');
    }
  };

  const handleImportExcel = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    try {
      setBusyAction('import');
      setError('');
      const result = await api.importAssetsExcel(file);
      await loadAssets();
      if (result.failed > 0) {
        setError(`Imported ${result.imported}/${result.totalRows} rows. ${result.errors[0] || 'Some rows failed.'}`);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyAction('');
      event.target.value = '';
    }
  };

  const handleDownloadTemplate = async () => {
    try {
      setBusyAction('template');
      const blob = await api.exportAssetsTemplateExcel();
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = 'assets-template.xlsx';
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyAction('');
    }
  };

  const setField = (key, val) => setForm((f) => ({ ...f, [key]: val }));

  const formatCurrency = (n) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(n);

  const classLabel = (cls) =>
    cls.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase()).toLowerCase().replace(/^\w/, (c) => c.toUpperCase());

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h2>Assets</h2>
          <p className="text-muted">{totalElements} total asset{totalElements !== 1 ? 's' : ''}</p>
        </div>
        <div className="asset-toolbar">
          <button className="btn btn-secondary" onClick={handleDownloadTemplate} disabled={busyAction === 'template'}>
            {busyAction === 'template' ? 'Downloading...' : 'Download Template'}
          </button>
          <button className="btn btn-secondary" onClick={handleExportExcel} disabled={busyAction === 'export'}>
            {busyAction === 'export' ? 'Exporting...' : 'Export Excel'}
          </button>
          <label className={`btn btn-secondary ${busyAction === 'import' ? 'btn-disabled' : ''}`}>
            {busyAction === 'import' ? 'Importing...' : 'Import Excel'}
            <input
              type="file"
              accept=".xlsx"
              onChange={handleImportExcel}
              disabled={busyAction === 'import'}
              hidden
            />
          </label>
          <button className="btn btn-primary" onClick={openCreate}>+ New Asset</button>
        </div>
      </div>

      <div className="filters">
        <select value={filterClass} onChange={(e) => handleFilterChange(e.target.value)}>
          <option value="">All Classes</option>
          {ASSET_CLASSES.map((c) => (
            <option key={c} value={c}>{classLabel(c)}</option>
          ))}
        </select>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {showForm && (
        <div className="modal-backdrop" onClick={() => setShowForm(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>{editing ? 'Edit Asset' : 'Create Asset'}</h3>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Asset ID</label>
                <input
                  value={form.id}
                  onChange={(e) => setField('id', e.target.value)}
                  placeholder="e.g. laptop-001"
                  required
                  disabled={editing}
                />
              </div>
              <div className="form-group">
                <label>Description</label>
                <input
                  value={form.description}
                  onChange={(e) => setField('description', e.target.value)}
                  placeholder="e.g. Dell XPS 15"
                  required
                />
              </div>
              <div className="form-group">
                <label>Asset Class</label>
                <select value={form.assetClass} onChange={(e) => setField('assetClass', e.target.value)}>
                  {ASSET_CLASSES.map((c) => (
                    <option key={c} value={c}>{classLabel(c)}</option>
                  ))}
                </select>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>Cost Basis ($)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={form.costBasis}
                    onChange={(e) => setField('costBasis', e.target.value)}
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Useful Life (years)</label>
                  <input
                    type="number"
                    min="1"
                    value={form.usefulLifeYears}
                    onChange={(e) => setField('usefulLifeYears', e.target.value)}
                    required
                  />
                </div>
              </div>
              <div className="form-group">
                <label>In-Service Date</label>
                <input
                  type="date"
                  value={form.inServiceDate}
                  onChange={(e) => setField('inServiceDate', e.target.value)}
                  required
                />
              </div>
              <div className="form-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowForm(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">{editing ? 'Save Changes' : 'Create Asset'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {loading ? (
        <div className="loading">Loading assets…</div>
      ) : assets.length === 0 ? (
        <div className="empty-state">
          <p>No assets found.</p>
          <button className="btn btn-primary" onClick={openCreate}>Create your first asset</button>
        </div>
      ) : (
        <>
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Description</th>
                  <th>Class</th>
                  <th>Cost Basis</th>
                  <th>In-Service</th>
                  <th>Useful Life</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {assets.map((a) => (
                  <tr key={a.id}>
                    <td className="font-mono">{a.id}</td>
                    <td>{a.description}</td>
                    <td><span className="badge">{classLabel(a.assetClass)}</span></td>
                    <td className="text-right">{formatCurrency(a.costBasis)}</td>
                    <td>{a.inServiceDate}</td>
                    <td>{a.usefulLifeYears}y</td>
                    <td className="actions">
                      <button className="btn btn-sm btn-secondary" onClick={() => openEdit(a)}>Edit</button>
                      <button className="btn btn-sm btn-danger" onClick={() => handleDelete(a.id)}>Delete</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="pagination">
            <button className="btn btn-sm btn-secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>
              ← Prev
            </button>
            <span>Page {page + 1} of {totalPages}</span>
            <button className="btn btn-sm btn-secondary" disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}>
              Next →
            </button>
          </div>
        </>
      )}
    </div>
  );
}
