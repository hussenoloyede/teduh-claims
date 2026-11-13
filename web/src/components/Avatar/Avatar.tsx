import styles from './Avatar.module.css';

type AvatarProps = {
  /** The person's name. Only the initials are drawn. */
  name: string;
};

// Initials in a circle. The app shows no photos of people.
// Hidden from screen readers, because the name is always given next to it
// or in the label of the link around it.
export function Avatar({ name }: AvatarProps) {
  const initials = name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');

  return (
    <span className={styles.avatar} aria-hidden="true">
      {initials}
    </span>
  );
}
