import { useEffect, useMemo, useState } from 'react';
import { api } from '../api';

const FEATURE_LABELS = {
  ASSETS: 'Assets',
  DEPRECIATION: 'Depreciation',
  TAX_STRATEGY: 'Tax Strategy',
  CLASSIFICATION: 'Classification',
  BREAKOUT: 'Breakout',
};

export default function AdminDashboard() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [savingUserId, setSavingUserId] = useState('');

  const featureFlags = useMemo(() => api.listFeatureFlags(), []);

  const loadUsers = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.listUsers();
      setUsers(data || []);
    } catch (err) {
      setError(err.message || 'Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
  }, []);

  const updateLocalUser = (userId, updater) => {
    setUsers((prev) => prev.map((user) => (user.id === userId ? updater(user) : user)));
  };

  const toggleFeature = (userId, feature) => {
    updateLocalUser(userId, (user) => {
      const current = new Set(user.featureAccess || []);
      if (current.has(feature)) current.delete(feature);
      else current.add(feature);
      return { ...user, featureAccess: Array.from(current) };
    });
  };

  const changeRole = (userId, role) => {
    updateLocalUser(userId, (user) => ({ ...user, role }));
  };

  const toggleEnabled = (userId) => {
    updateLocalUser(userId, (user) => ({ ...user, enabled: !user.enabled }));
  };

  const saveUser = async (user) => {
    setSavingUserId(user.id);
    setError('');
    setSuccess('');
    try {
      const payload = {
        role: user.role,
        enabled: user.enabled,
        featureAccess: user.featureAccess || [],
      };
      const updated = await api.updateUserAccess(user.id, payload);
      updateLocalUser(user.id, () => updated);
      setSuccess(`Updated access for ${updated.username}`);
    } catch (err) {
      setError(err.message || 'Failed to update user');
    } finally {
      setSavingUserId('');
    }
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h2>Admin Dashboard</h2>
          <p className="text-muted">Manage user roles and per-feature access permissions.</p>
        </div>
        <button className="btn btn-secondary" onClick={loadUsers} disabled={loading}>
          {loading ? 'Refreshing...' : 'Refresh'}
        </button>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <div className="admin-grid">
        {(users || []).map((user) => (
          <div key={user.id} className="card admin-user-card">
            <div className="admin-user-header">
              <div>
                <h3>{user.username}</h3>
                <p className="text-muted admin-user-meta">{user.email || 'No email'} • {user.id}</p>
              </div>
              <span className={`badge ${user.role === 'ADMIN' ? 'badge-admin' : ''}`}>{user.role}</span>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label>Role</label>
                <select value={user.role} onChange={(e) => changeRole(user.id, e.target.value)}>
                  <option value="USER">USER</option>
                  <option value="ADMIN">ADMIN</option>
                </select>
              </div>
              <div className="form-group">
                <label>Account</label>
                <label className="checkbox-label">
                  <input type="checkbox" checked={user.enabled} onChange={() => toggleEnabled(user.id)} />
                  Enabled
                </label>
              </div>
            </div>

            <div className="admin-features">
              <p className="admin-section-title">Feature Access</p>
              <div className="admin-feature-list">
                {featureFlags.map((feature) => (
                  <label key={feature} className="checkbox-label">
                    <input
                      type="checkbox"
                      checked={(user.featureAccess || []).includes(feature)}
                      onChange={() => toggleFeature(user.id, feature)}
                    />
                    {FEATURE_LABELS[feature] || feature}
                  </label>
                ))}
              </div>
            </div>

            <div className="form-actions">
              <button
                className="btn btn-primary"
                onClick={() => saveUser(user)}
                disabled={savingUserId === user.id}
              >
                {savingUserId === user.id ? 'Saving...' : 'Save Access'}
              </button>
            </div>
          </div>
        ))}
      </div>

      {!loading && users.length === 0 && (
        <div className="empty-state">
          <p>No users found.</p>
        </div>
      )}
    </div>
  );
}
