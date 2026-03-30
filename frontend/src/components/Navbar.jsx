import { NavLink, useNavigate } from 'react-router-dom';
import { api } from '../api';

export default function Navbar({ onLogout }) {
  const navigate = useNavigate();

  const handleLogout = () => {
    api.logout();
    onLogout();
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <div className="navbar-brand">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <rect x="2" y="3" width="20" height="14" rx="2" />
          <path d="M8 21h8" />
          <path d="M12 17v4" />
        </svg>
        <span>AssetMind</span>
      </div>
      <div className="navbar-links">
        <NavLink to="/" end>Assets</NavLink>
        <NavLink to="/depreciation">Depreciation</NavLink>
        <NavLink to="/tax-strategy">Tax Strategy</NavLink>
        <NavLink to="/classification">Classification</NavLink>
      </div>
      <button className="btn btn-sm btn-secondary" onClick={handleLogout}>
        Sign Out
      </button>
    </nav>
  );
}
