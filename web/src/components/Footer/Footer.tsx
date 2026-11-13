import styles from './Footer.module.css';

// Every screen says Teduh is made up, so nobody mistakes it for a real insurer.
export function Footer() {
  return (
    <footer className={styles.footer}>
      <p>Teduh is a fictional insurer made for a portfolio project.</p>
    </footer>
  );
}
