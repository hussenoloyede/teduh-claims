import { ArrowLeft, ChevronDown } from 'lucide-react';
import { Link } from 'react-router';
import styles from './HelpPage.module.css';
import page from './Page.module.css';

type Question = { question: string; answer: string };

const questions: Question[] = [
  {
    question: 'What does a Teduh plan cover?',
    answer:
      'Four things: a delayed flight, a checked bag that arrives late, a bag that is lost or damaged, and a trip you had to cancel. Each plan has its own limit for each one.',
  },
  {
    question: 'Which documents should I send?',
    answer:
      'The receipts for what you are claiming, and any letter or report from the airline, such as a delay letter. Send photos or PDFs, up to 5 files of 10 MB each.',
  },
  {
    question: 'What happens after I claim?',
    answer:
      'A claims agent picks up your claim and reviews it. They approve it, reject it with a reason, or ask you for more. Each time the status changes, you get a notification and the claim page shows the new step.',
  },
  {
    question: 'What if my agent asks for more?',
    answer:
      'The claim shows "More info needed" with what the agent asked for. Open the claim, write a reply, attach any documents they asked for and send it. The claim goes back to the agent.',
  },
  {
    question: 'Can a decision be changed?',
    answer:
      'Not in this demo. Once a claim is approved or rejected it is closed. If something else went wrong on your trip, start a new claim for it.',
  },
  {
    question: 'Is Teduh real?',
    answer:
      'No. Teduh is a fictional insurer made for a portfolio project. Nothing is charged, no money is paid, and the policies are not real cover.',
  },
];

// C21: short answers to what a new customer asks first. Each answer opens in place (a <details> element,
// so it works with the keyboard and screen readers without any script). Linked from Profile and Home.
export function HelpPage() {
  return (
    <div className={page.page}>
      <title>Help and questions · Teduh Claims</title>
      <Link to="/profile" className={styles.back}>
        <ArrowLeft size={16} aria-hidden="true" />
        Profile
      </Link>
      <div className={page.intro}>
        <h1>Help and questions</h1>
        <p>How Teduh plans and claims work.</p>
      </div>
      <div className={styles.list}>
        {questions.map((item, index) => (
          <details key={item.question} className={styles.item} open={index === 0}>
            <summary>
              <span>{item.question}</span>
              <ChevronDown size={18} aria-hidden="true" className={styles.chevron} />
            </summary>
            <p>{item.answer}</p>
          </details>
        ))}
      </div>
      <p className={styles.note}>Teduh is a fictional insurer made for a portfolio project. Don't upload real documents.</p>
    </div>
  );
}
