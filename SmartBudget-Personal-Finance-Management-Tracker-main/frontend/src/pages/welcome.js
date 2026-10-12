import { Link } from 'react-router-dom';
import '../assets/styles/welcome.css';
import Logo from '../components/utils/Logo';

function Welcome() {
    return (
        <section className="hero-section">
            <Logo/>
            <h2>Welcome to SmartBudget!</h2>
            <h3> Take control of your money with SmartBudget.<br />  
            Track, save, and grow smarter.</h3>

            <div>
                <Link to='/auth/login'><p><button>Log in</button></p></Link>
                <Link to='/auth/register'><button>Create Account</button></Link>
            </div>
        </section>
    )
}

export default Welcome;