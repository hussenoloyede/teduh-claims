import styles from './ComingSoonPage.module.css';

type ComingSoonPageProps = { title: string };

// Stands in for a screen that isn't built yet, and says so plainly rather than pretending.
export function ComingSoonPage({ title }: ComingSoonPageProps) {
  return (
    <div className={styles.page}>
      <title>{`${title} · Teduh Claims`}</title>
      <h1>{title}</h1>
      <p>This screen isn't built yet. It's next on the list.</p>
    </div>
  );
}
