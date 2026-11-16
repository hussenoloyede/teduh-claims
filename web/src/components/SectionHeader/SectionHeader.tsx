import { ChevronRight } from 'lucide-react';
import { Link } from 'react-router';
import styles from './SectionHeader.module.css';

type SectionHeaderProps = {
  /** The heading's id, so the section can be labelled by it (aria-labelledby). */
  id: string;
  title: string;
  /** A short count next to the title, for example "1 active". */
  badge?: string;
  link?: { to: string; label: string };
};

// "Your policies · 1 active · View all" on C02.
export function SectionHeader({ id, title, badge, link }: SectionHeaderProps) {
  return (
    <div className={styles.header}>
      <div className={styles.title}>
        <h2 id={id}>{title}</h2>
        {badge && <span className={styles.badge}>{badge}</span>}
      </div>
      {link && (
        <Link className={styles.link} to={link.to}>
          {link.label}
          <ChevronRight size={14} aria-hidden="true" />
        </Link>
      )}
    </div>
  );
}
