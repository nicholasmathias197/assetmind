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

function ProtectedRoute({ children, authed }) {
  return authed ? children : <Navigate to="/login" replace />;
}

export default function App() {
  const [authed, setAuthed] = useState(api.isAuthenticated());

  return (
    <div className="app">
      {authed && <Navbar onLogout={() => setAuthed(false)} />}
      <main className={authed ? 'main-content' : ''}>
        <Routes>
          <Route path="/login" element={
            authed ? <Navigate to="/" replace /> : <Login onLogin={() => setAuthed(true)} />
          } />
          <Route path="/register" element={
            authed ? <Navigate to="/" replace /> : <Register onLogin={() => setAuthed(true)} />
          } />
          <Route path="/" element={
            <ProtectedRoute authed={authed}><Assets /></ProtectedRoute>
          } />
          <Route path="/depreciation" element={
            <ProtectedRoute authed={authed}><Depreciation /></ProtectedRoute>
          } />
          <Route path="/tax-strategy" element={
            <ProtectedRoute authed={authed}><TaxStrategy /></ProtectedRoute>
          } />
          <Route path="/classification" element={
            <ProtectedRoute authed={authed}><Classification /></ProtectedRoute>
          } />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
