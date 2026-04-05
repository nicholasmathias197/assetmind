import { Link } from 'react-router-dom';

export default function AccessDenied() {
  return (
    <div className="page">
      <div className="card access-denied-card">
        <h2>Access denied</h2>
        <p className="text-muted">
          Your account is active, but you do not have permission to use this feature yet.
        </p>
        <p className="text-muted">
          Ask an admin to grant access from the Admin dashboard.
        </p>
        <div className="form-actions" style={{ justifyContent: 'flex-start', marginTop: '16px' }}>
          <Link to="/" className="btn btn-secondary">Back to Home</Link>
        </div>
      </div>
    </div>
  );
}
