import { House, ReceiptText, Shield, UserRound } from 'lucide-react';
import styles from './BottomNav.module.css';

const items = [
  { key: 'home', label: 'Home', href: '/', Icon: House },
  { key: 'claims', label: 'Claims', href: '/claims', Icon: ReceiptText },
  { key: 'policies', label: 'Policies', href: '/policies', Icon: Shield },
  { key: 'profile', label: 'Profile', href: '/profile', Icon: UserRound },
] as const;

export type CustomerSection = (typeof items)[number]['key'];

type BottomNavProps = {
  /** Which section the customer is in. */
  current: CustomerSection;
};

// The four tabs at the bottom of the customer screens (C02, C13 in Hi-Fi 2.0).
export function BottomNav({ current }: BottomNavProps) {
  return (
    <nav className={styles.nav} aria-label="Main">
      <ul className={styles.list}>
        {items.map(({ key, label, href, Icon }) => (
          <li key={key}>
            <a className={styles.link} href={href} aria-current={key === current ? 'page' : undefined}>
              <span className={styles.icon}>
                <Icon size={20} aria-hidden="true" />
              </span>
              {label}
            </a>
          </li>
        ))}
      </ul>
    </nav>
  );
}
