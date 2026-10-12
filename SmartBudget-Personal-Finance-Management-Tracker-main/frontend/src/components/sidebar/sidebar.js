import { Link, useNavigate } from 'react-router-dom';
import AuthService from '../../services/auth.service';
import '../../assets/styles/sidebar.css';
import SideBarLinks from './sideBarLinks';
import { useState } from 'react';
import AuthVerify from '../../services/auth.verify';
import Logo from '../utils/Logo';

function Sidebar({ activeNavId }) {
  const [isSideBarOpen, setIsSideBarOpen] = useState(false);
  const navigate = useNavigate();

  const logout = () => {
    AuthService.logout_req();
    navigate('/');
    window.location.reload();
  };

  const currentUser = AuthService.getCurrentUser();

  return (
    <div className={isSideBarOpen ? 'side-bar open' : 'side-bar'}>
      <div
        style={{
          padding: '15px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <Logo />
        <span
          onClick={() => setIsSideBarOpen(false)}
          className="mobile"
        >
          <i className="fa fa-times" aria-hidden="true"></i>
        </span>
        <span
          onClick={() => setIsSideBarOpen(true)}
          className="mobile menu"
        >
          <i className="fa fa-bars" aria-hidden="true"></i>
        </span>
      </div>

      <ul>
        {SideBarLinks.filter((link) => {
          if (!currentUser) return false;

          // ✅ Show ONLY admin links if admin
          if (currentUser.roles.includes('ROLE_ADMIN')) {
            return link.role === 'ROLE_ADMIN';
          }

          // ✅ Otherwise show user links
          return currentUser.roles.includes(link.role);
        }).map((link) => {
          return (
            <Link key={link.id} className="nav-link" to={link.to}>
              <li className={activeNavId === link.id ? 'active' : ''}>
                <i className={link.icon} aria-hidden="true"></i>
                {link.name}
              </li>
            </Link>
          );
        })}

        {/* Logout */}
        <span onClick={logout}>
          <Link className="nav-link">
            <li>
              <i className="fa fa-sign-out" aria-hidden="true"></i>
              Log out
            </li>
          </Link>
        </span>
      </ul>
      <AuthVerify logOut={logout} />
    </div>
  );
}

export default Sidebar;
