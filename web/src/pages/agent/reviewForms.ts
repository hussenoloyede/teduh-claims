// The rules for the agent's three dialogs: ask for more information, approve, reject. The same rules and
// the same messages as Claim.java, so the agent hears about a problem before sending anything. The server
// still checks everything again.

import { formatMoney } from '../../lib/format';

/** Claim.MAX_MESSAGE_LENGTH: the longest request, or reason, the server accepts. */
export const MAX_MESSAGE = 1000;

const amountPattern = /^\d+(\.\d{1,2})?$/;

function checkText(text: string, whenMissing: string, what: string): string | undefined {
  const trimmed = text.trim();
  if (!trimmed) return whenMissing;
  if (trimmed.length > MAX_MESSAGE) return `${what} can be at most ${MAX_MESSAGE} characters.`;
  return undefined;
}

/** What the agent asks the customer for. */
export function checkRequest(message: string): string | undefined {
  return checkText(message, 'A message to the customer is required.', 'The message');
}

/** A rejection always needs a reason, because the customer sees it. */
export function checkRejection(reason: string): string | undefined {
  return checkText(reason, 'A reason is required to reject a claim.', 'The reason');
}

/** The amount typed, or undefined while it isn't a valid amount yet. */
export function parseAmount(text: string): number | undefined {
  const trimmed = text.trim();
  return amountPattern.test(trimmed) ? Number(trimmed) : undefined;
}

/** True when the amount typed is a valid amount below what was claimed, so a reason becomes required. */
export function isPartial(amountText: string, claimed: number): boolean {
  const amount = parseAmount(amountText);
  return amount !== undefined && amount > 0 && amount < claimed;
}

export type ApprovalProblems = { amount?: string; reason?: string };

/** An approval: more than zero, no more than claimed, and a reason when it is less than claimed. */
export function checkApproval(amountText: string, reason: string, claimed: number): ApprovalProblems {
  const problems: ApprovalProblems = {};
  const amount = parseAmount(amountText);
  if (amount === undefined) {
    problems.amount = 'Enter an amount like 400 or 400.50, with no commas.';
  } else if (amount <= 0) {
    problems.amount = 'The approved amount must be more than zero.';
  } else if (amount > claimed) {
    problems.amount = `You can't approve more than the ${formatMoney(claimed)} claimed.`;
  }

  if (isPartial(amountText, claimed)) {
    problems.reason = checkText(reason, 'A reason is required when approving less than the amount claimed.', 'The reason');
  } else if (reason.trim().length > MAX_MESSAGE) {
    problems.reason = `The reason can be at most ${MAX_MESSAGE} characters.`;
  }
  return problems;
}

/** Amounts go into the field as the API sends them: 450 becomes "450.00". */
export function amountText(amount: number): string {
  return amount.toFixed(2);
}
