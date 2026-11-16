import { X } from 'lucide-react';
import { useId, useLayoutEffect, useRef, type ReactNode } from 'react';
import styles from './Dialog.module.css';

type DialogProps = {
  open: boolean;
  title: string;
  /** A short line under the title, such as the claim and customer it is about. */
  subtitle?: string;
  /** Called by Cancel, the close button and the Escape key. */
  onClose: () => void;
  children: ReactNode;
  /** The buttons at the bottom, usually Cancel and the action. */
  footer: ReactNode;
  /** Red top edge for a final, negative decision (reject). */
  tone?: 'default' | 'danger';
};

// A modal dialog built on the browser's own <dialog> element. showModal() does the hard parts for free:
// the rest of the page can't be clicked or reached with Tab, Escape closes it, focus moves into it, and
// screen readers announce it as a dialog named by its title. Focus goes back where it was when it closes.
export function Dialog({ open, title, subtitle, onClose, children, footer, tone = 'default' }: DialogProps) {
  const ref = useRef<HTMLDialogElement>(null);
  const titleId = useId();

  // A layout effect, so on unmount close() runs while the element is still in the page. Closing a removed
  // dialog wouldn't send focus back.
  useLayoutEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
    // Closing (rather than just removing the element) also puts focus back on the button that opened it.
    return () => {
      if (dialog.open) dialog.close();
    };
  }, [open]);

  return (
    <dialog
      ref={ref}
      className={styles.dialog}
      data-tone={tone}
      aria-labelledby={titleId}
      // Escape fires "cancel". Stop the browser closing it by itself, so React's `open` stays the one truth.
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
    >
      {open && (
        <div className={styles.inner}>
          <header className={styles.header}>
            <div>
              <h2 id={titleId} className={styles.title}>
                {title}
              </h2>
              {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
            </div>
            <button type="button" className={styles.close} onClick={onClose}>
              <X size={20} aria-hidden="true" />
              <span className="visually-hidden">Close</span>
            </button>
          </header>
          <div className={styles.body}>{children}</div>
          <footer className={styles.footer}>{footer}</footer>
        </div>
      )}
    </dialog>
  );
}
