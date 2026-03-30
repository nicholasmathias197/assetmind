const API_BASE = '/api/v1';

function getToken() {
  return localStorage.getItem('accessToken');
}

function setTokens(accessToken, refreshToken) {
  localStorage.setItem('accessToken', accessToken);
  if (refreshToken) localStorage.setItem('refreshToken', refreshToken);
}

function clearTokens() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
}

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...options.headers };
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${API_BASE}${path}`, { ...options, headers });

  if (res.status === 401) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      headers['Authorization'] = `Bearer ${getToken()}`;
      const retry = await fetch(`${API_BASE}${path}`, { ...options, headers });
      if (!retry.ok) throw new Error(await retry.text());
      return retry.status === 204 ? null : retry.json();
    }
    clearTokens();
    window.location.href = '/login';
    throw new Error('Session expired');
  }

  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `HTTP ${res.status}`);
  }
  return res.status === 204 ? null : res.json();
}

async function tryRefresh() {
  const refreshToken = localStorage.getItem('refreshToken');
  if (!refreshToken) return false;
  try {
    const res = await fetch(`${API_BASE}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });
    if (!res.ok) return false;
    const data = await res.json();
    if (data.accessToken) {
      setTokens(data.accessToken, data.refreshToken || refreshToken);
      return true;
    }
    return false;
  } catch {
    return false;
  }
}

export const api = {
  // Auth
  login: (username, password) =>
    request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }).then((data) => {
      setTokens(data.accessToken, data.refreshToken);
      return data;
    }),

  register: (username, password, email) =>
    request('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ username, password, email }),
    }),

  logout: () => {
    clearTokens();
  },

  isAuthenticated: () => !!getToken(),

  // Assets
  getAssets: () => request('/assets'),
  getAssetsPaged: (page = 0, size = 20, sortBy = 'id', sortDirection = 'asc', assetClass) => {
    const params = new URLSearchParams({ page, size, sortBy, sortDirection });
    if (assetClass) params.set('assetClass', assetClass);
    return request(`/assets/page?${params}`);
  },
  getAsset: (id) => request(`/assets/${encodeURIComponent(id)}`),
  createAsset: (asset) =>
    request('/assets', { method: 'POST', body: JSON.stringify(asset) }),
  updateAsset: (id, asset) =>
    request(`/assets/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(asset) }),
  deleteAsset: (id) =>
    request(`/assets/${encodeURIComponent(id)}`, { method: 'DELETE' }),

  // Depreciation
  runDepreciation: (data) =>
    request('/depreciation/run', { method: 'POST', body: JSON.stringify(data) }),
  recommendDepreciation: (data) =>
    request('/depreciation/recommend', { method: 'POST', body: JSON.stringify(data) }),
  aiRunDepreciation: (data) =>
    request('/depreciation/ai-run', { method: 'POST', body: JSON.stringify(data) }),

  // Tax Strategy
  recommendTaxStrategy: (data) =>
    request('/tax-strategy/recommend', { method: 'POST', body: JSON.stringify(data) }),

  // Classification
  classifyDocument: (documentText) =>
    request('/classification/suggest', { method: 'POST', body: JSON.stringify({ documentText }) }),

  // Breakout
  suggestBreakout: (documentText) =>
    request('/breakout/suggest', { method: 'POST', body: JSON.stringify({ documentText }) }),

  // Health
  health: () => fetch('/actuator/health').then((r) => r.json()),
};
