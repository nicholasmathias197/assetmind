const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api/v1';

const FEATURE_FLAGS = ['ASSETS', 'DEPRECIATION', 'TAX_STRATEGY', 'CLASSIFICATION', 'BREAKOUT'];

function getToken() {
  return localStorage.getItem('accessToken');
}

function decodeJwtPayload(token) {
  if (!token || token.split('.').length < 2) return null;
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = `${base64}${'='.repeat((4 - (base64.length % 4)) % 4)}`;
    const json = atob(padded);
    return JSON.parse(json);
  } catch {
    return null;
  }
}

function getAccessContext() {
  const token = getToken();
  const claims = decodeJwtPayload(token);
  const role = (claims?.role || 'USER').toUpperCase();
  const rawFeatures = Array.isArray(claims?.featureAccess) ? claims.featureAccess : [];
  const features = rawFeatures
    .filter((item) => typeof item === 'string' && item.trim() !== '')
    .map((item) => item.trim().toUpperCase());

  return {
    userId: claims?.sub || null,
    username: claims?.username || null,
    role,
    isAdmin: role === 'ADMIN',
    features,
  };
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
  const isFormData = options.body instanceof FormData;
  const defaultHeaders = isFormData ? {} : { 'Content-Type': 'application/json' };
  const headers = { ...defaultHeaders, ...options.headers };
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

async function requestRaw(path, options = {}) {
  const headers = { ...options.headers };
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${API_BASE}${path}`, { ...options, headers });

  if (res.status === 401) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      headers['Authorization'] = `Bearer ${getToken()}`;
      const retry = await fetch(`${API_BASE}${path}`, { ...options, headers });
      if (!retry.ok) throw new Error(await retry.text());
      return retry;
    }
    clearTokens();
    window.location.href = '/login';
    throw new Error('Session expired');
  }

  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `HTTP ${res.status}`);
  }

  return res;
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
  getAccessContext,
  hasFeatureAccess: (feature) => {
    const context = getAccessContext();
    if (context.isAdmin) return true;
    return context.features.includes(String(feature || '').toUpperCase());
  },
  listFeatureFlags: () => FEATURE_FLAGS,

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
  exportAssetsExcel: async (assetClass) => {
    const params = new URLSearchParams();
    if (assetClass) params.set('assetClass', assetClass);
    const query = params.toString();
    const response = await requestRaw(`/assets/export${query ? `?${query}` : ''}`, {
      method: 'GET',
      headers: {
        Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      },
    });
    return response.blob();
  },
  exportAssetsTemplateExcel: async () => {
    const response = await requestRaw('/assets/export/template', {
      method: 'GET',
      headers: {
        Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      },
    });
    return response.blob();
  },
  importAssetsExcel: async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return request('/assets/import', {
      method: 'POST',
      headers: {},
      body: formData,
    });
  },

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

  // Admin
  listUsers: () => request('/admin/users'),
  updateUserAccess: (userId, payload) =>
    request(`/admin/users/${encodeURIComponent(userId)}/access`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),
};
