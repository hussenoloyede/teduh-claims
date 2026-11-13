import { Check } from 'lucide-react';
import { formatDateTime } from '../../lib/format';
import styles from './Timeline.module.css';

export type TimelineItem = {
  id: string;
  title: string;
  /** When it happened, as the API sends it (UTC). Upcoming steps have none. */
  time?: string;
  detail?: string;
  /** A step that hasn't happened yet: shown faded with a dot. */
  upcoming?: boolean;
};

type TimelineProps = {
  items: TimelineItem[];
};

// A claim's history, oldest first, as an ordered list.
export function Timeline({ items }: TimelineProps) {
  return (
    <ol className={styles.timeline}>
      {items.map((item) => (
        <li key={item.id} className={styles.item} data-upcoming={item.upcoming || undefined}>
          <span className={styles.marker} aria-hidden="true">
            {item.upcoming ? <span className={styles.dot} /> : <Check size={12} strokeWidth={3} />}
          </span>
          <div className={styles.body}>
            <p className={styles.title}>
              {item.title}
              {item.upcoming && <span className="visually-hidden"> (not yet)</span>}
            </p>
            {item.time && (
              <time className={styles.time} dateTime={item.time}>
                {formatDateTime(item.time)}
              </time>
            )}
            {item.detail && <p className={styles.detail}>{item.detail}</p>}
          </div>
        </li>
      ))}
    </ol>
  );
}
