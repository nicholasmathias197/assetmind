import { useState } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { api } from './api';
import Navbar from './components/Navbar';
import Login from './pages/Login';
import Register from './pages/Register';
import Assets from './pages/Assets';
import Depreciation from './pages/Depreciation';
import TaxStrategy from './pages/TaxStrategy';
import Classification from './pages/Classification';
import Breakout from './pages/Breakout';
import AccessDenied from './pages/AccessDenied';
import AdminDashboard from './pages/AdminDashboard';

function ProtectedRoute({ children, authed }) {
  return authed ? children : <Navigate to="/login" replace />;
}

function FeatureRoute({ children, authed, feature }) {
  if (!authed) return <Navigate to="/login" replace />;
  if (!api.hasFeatureAccess(feature)) return <Navigate to="/access-denied" replace />;
  return children;
}

function AdminRoute({ children, authed }) {
  if (!authed) return <Navigate to="/login" replace />;
  if (!api.getAccessContext().isAdmin) return <Navigate to="/access-denied" replace />;
  return children;
}

export default function App() {
  const [authed, setAuthed] = useState(api.isAuthenticated());
  const accessContext = api.getAccessContext();

  return (
    <div className="app">
      {authed && <Navbar onLogout={() => setAuthed(false)} accessContext={accessContext} />}
      <main className={authed ? 'main-content' : ''}>
        <Routes>
          <Route path="/login" element={
            authed ? <Navigate to="/" replace /> : <Login onLogin={() => setAuthed(true)} />
          } />
          <Route path="/register" element={
            authed ? <Navigate to="/" replace /> : <Register onLogin={() => setAuthed(true)} />
          } />
          <Route path="/" element={
            <FeatureRoute authed={authed} feature="ASSETS"><Assets /></FeatureRoute>
          } />
          <Route path="/depreciation" element={
            <FeatureRoute authed={authed} feature="DEPRECIATION"><Depreciation /></FeatureRoute>
          } />
          <Route path="/tax-strategy" element={
            <FeatureRoute authed={authed} feature="TAX_STRATEGY"><TaxStrategy /></FeatureRoute>
          } />
          <Route path="/classification" element={
            <FeatureRoute authed={authed} feature="CLASSIFICATION"><Classification /></FeatureRoute>
          } />
          <Route path="/breakout" element={
            <FeatureRoute authed={authed} feature="BREAKOUT"><Breakout /></FeatureRoute>
          } />
          <Route path="/admin" element={
            <AdminRoute authed={authed}><AdminDashboard /></AdminRoute>
          } />
          <Route path="/access-denied" element={
            <ProtectedRoute authed={authed}><AccessDenied /></ProtectedRoute>
          } />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
