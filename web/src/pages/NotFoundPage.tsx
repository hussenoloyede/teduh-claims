import { Link } from 'react-router';
import { Footer } from '../components/Footer/Footer';
import styles from './NotFoundPage.module.css';

// Any address the router doesn't know. The server sends index.html for every such address (WebPages.java),
// so this page is what someone sees after a mistyped or outdated link.
export function NotFoundPage() {
  return (
    <div className={styles.page}>
      <main className={styles.main}>
        <h1>Page not found</h1>
        <p>There's nothing at this address. The link may be mistyped or out of date.</p>
        <Link to="/">Go to the Teduh home page</Link>
      </main>
      <Footer />
    </div>
  );
}
