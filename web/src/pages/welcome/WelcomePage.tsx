import { ArrowRight, BellRing, FileText, Umbrella } from 'lucide-react';
import { useEffect, useRef, useState, type ReactNode } from 'react';
import { Link } from 'react-router';
import { Button, ButtonLink } from '../../components/Button/Button';
import styles from './WelcomePage.module.css';

type Slide = { icon: ReactNode; title: string; text: string };

const slides: Slide[] = [
  {
    icon: <Umbrella size={48} />,
    title: 'Travel cover for when trips go wrong',
    text: 'Teduh covers delayed flights, late or lost bags and cancelled trips. Pick a plan for your trip in a minute.',
  },
  {
    icon: <FileText size={48} />,
    title: 'Claim from your phone',
    text: 'Tell us what happened, add photos or PDFs of your receipts, and send. Nothing to print or post.',
  },
  {
    icon: <BellRing size={48} />,
    title: 'Always know what happens next',
    text: 'Follow every step of your claim. If your agent needs something, you see exactly what to send.',
  },
];

// /welcome: S02 to S04, shown once after "Try as a new customer". Three short screens, then a choice:
// choose a plan now, or look around first. Skip goes straight to the home page, which offers the plans too.
export function WelcomePage() {
  const [index, setIndex] = useState(0);
  const headingRef = useRef<HTMLHeadingElement>(null);
  const firstRender = useRef(true);
  const slide = slides[index];
  const last = index === slides.length - 1;

  // Each new screen moves focus to its heading, so a screen reader reads the new screen.
  useEffect(() => {
    if (firstRender.current) {
      firstRender.current = false;
      return;
    }
    headingRef.current?.focus();
  }, [index]);

  return (
    <div className={styles.page}>
      <title>{`Welcome, ${index + 1} of ${slides.length} · Teduh Claims`}</title>
      <header className={styles.bar}>
        <span className={styles.brand}>Teduh</span>
        {!last && (
          <Link to="/" className={styles.skip}>
            Skip
          </Link>
        )}
      </header>

      <main className={styles.main}>
        <span className={styles.picture} aria-hidden="true">
          {slide.icon}
        </span>
        <h1 ref={headingRef} tabIndex={-1}>
          {slide.title}
        </h1>
        <p>{slide.text}</p>
        <p className="visually-hidden">
          Screen {index + 1} of {slides.length}
        </p>
        <div className={styles.dots} aria-hidden="true">
          {slides.map((each, dot) => (
            <span key={each.title} className={dot === index ? styles.current : undefined} />
          ))}
        </div>
      </main>

      <div className={styles.actions}>
        {last ? (
          <>
            <ButtonLink to="/plans" fullWidth>
              Choose a plan
            </ButtonLink>
            <ButtonLink to="/" variant="secondary" fullWidth>
              Not now, look around first
            </ButtonLink>
          </>
        ) : (
          <Button fullWidth onClick={() => setIndex(index + 1)}>
            Next
            <ArrowRight size={16} aria-hidden="true" />
          </Button>
        )}
        <p className={styles.note}>Teduh is a fictional insurer made for a portfolio project. Nothing here is real.</p>
      </div>
    </div>
  );
}
