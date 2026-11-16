// The new-claim form's values and rules, as plain functions so each rule has its own unit test.
// They repeat the server's rules (Claim.java, ClaimService.java, FileCheck.java) to tell the customer early;
// the server still checks everything, because the browser can't be trusted.

import type { ClaimType } from '../../api/claims';
import type { Policy } from '../../api/policies';
import type { components } from '../../api/schema';
import { formatDate, formatMoney } from '../../lib/format';

/** A file the customer picked, with the reason it can't be sent if the browser can already tell. */
export type PickedFile = { id: string; file: File; problem?: string };

export type NewClaimValues = {
  policyId: string;
  claimType: ClaimType | '';
  /** "2026-11-14", as a date input gives it. */
  incidentDate: string;
  /** As typed, for example "450" or "450.50". */
  amount: string;
  description: string;
  files: PickedFile[];
  confirmed: boolean;
};

export const MAX_DESCRIPTION = 500;
export const MAX_FILES = 5;
export const MAX_FILE_BYTES = 10 * 1024 * 1024;

/** Which fields each step checks before moving on. */
export const stepFields: (keyof NewClaimValues)[][] = [
  ['policyId', 'claimType'],
  ['incidentDate', 'amount', 'description'],
  ['files'],
  ['confirmed'],
];

export const stepNames = ['Policy and claim type', 'What happened', 'Documents', 'Check and submit'];

/** The latest date a claim can be about: today, or the end of cover if that came first. */
export function latestIncidentDate(policy: Pick<Policy, 'coverEnd'>, today: string): string {
  return policy.coverEnd < today ? policy.coverEnd : today;
}

export function checkIncidentDate(
  date: string,
  policy: Pick<Policy, 'coverStart' | 'coverEnd'> | undefined,
  today: string,
): string | undefined {
  if (!date) return 'Enter the date it happened.';
  if (date > today) return "The date can't be in the future.";
  if (policy && (date < policy.coverStart || date > policy.coverEnd)) {
    return `Pick a date between ${formatDate(policy.coverStart)} and ${formatDate(policy.coverEnd)}, when your policy covered you.`;
  }
  return undefined;
}

const amountPattern = /^\d+(\.\d{1,2})?$/;

export function checkAmount(text: string, limit: number | undefined, typeLabel: string): string | undefined {
  const trimmed = text.trim();
  if (!trimmed) return 'Enter how much you are claiming.';
  if (!amountPattern.test(trimmed)) return 'Enter an amount like 450 or 450.50, with no commas.';
  const amount = Number(trimmed);
  if (amount <= 0) return 'The amount must be more than zero.';
  if (limit !== undefined && amount > limit) return `That's more than your ${typeLabel} limit of ${formatMoney(limit)}.`;
  return undefined;
}

export function checkDescription(text: string): string | undefined {
  if (!text.trim()) return 'Tell us what happened.';
  if (text.length > MAX_DESCRIPTION) return `Keep it to ${MAX_DESCRIPTION} characters or fewer.`;
  return undefined;
}

const acceptedTypes = ['application/pdf', 'image/jpeg', 'image/png'];
const acceptedEndings = ['.pdf', '.jpg', '.jpeg', '.png'];

/**
 * What the browser can tell about a file before sending it. The server decides by the file's first bytes;
 * this only catches the obvious cases (a .heic photo, a 20 MB scan) so the customer finds out straight away.
 */
export function checkFile(file: Pick<File, 'name' | 'type' | 'size'>): string | undefined {
  const name = file.name.toLowerCase();
  const typeOk = acceptedTypes.includes(file.type) || acceptedEndings.some((ending) => name.endsWith(ending));
  if (!typeOk) return "This file type isn't accepted. Use PDF, JPG or PNG.";
  if (file.size === 0) return 'This file is empty.';
  if (file.size > MAX_FILE_BYTES) return 'This file is larger than 10 MB.';
  return undefined;
}

export function checkFiles(files: PickedFile[]): string | undefined {
  const rejected = files.filter((picked) => picked.problem);
  if (rejected.length > 0) {
    return rejected.length === 1
      ? `Remove ${rejected[0].file.name}: it can't be uploaded.`
      : `Remove the ${rejected.length} files that can't be uploaded.`;
  }
  if (files.length === 0) return 'Attach at least one document, such as a receipt or a letter from the airline.';
  if (files.length > MAX_FILES) return `Attach up to ${MAX_FILES} files.`;
  return undefined;
}

type SubmitClaimRequest = components['schemas']['SubmitClaimRequest'];

/** The JSON part of the multipart request, from the form's values. */
export function toSubmitRequest(values: NewClaimValues): SubmitClaimRequest {
  return {
    policyId: values.policyId,
    claimType: values.claimType as ClaimType,
    incidentDate: values.incidentDate,
    amountClaimed: Number(values.amount.trim()),
    description: values.description.trim(),
  };
}

/**
 * The multipart body: a "claim" part holding the JSON, and one "files" part per document, the shape
 * ClaimController.submit expects. The JSON goes in a Blob typed application/json so Spring reads it as JSON.
 */
export function toFormData(claim: SubmitClaimRequest, files: PickedFile[]): FormData {
  const body = new FormData();
  body.append('claim', new Blob([JSON.stringify(claim)], { type: 'application/json' }));
  for (const picked of files) {
    body.append('files', picked.file, picked.file.name);
  }
  return body;
}
