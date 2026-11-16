// The rules for answering an agent's request, matching Claim.respond() and DocumentService on the server.

import type { components } from '../../api/schema';
import type { PickedFile } from '../newClaim/newClaimForm';

export const MAX_NOTE = 500;
export const MAX_FILES_PER_UPLOAD = 5;
export const MAX_FILES_PER_CLAIM = 15;

/** How many more files this response may carry: five at a time, fifteen on a claim in all. */
export function filesAllowed(documentsOnClaim: number): number {
  return Math.max(0, Math.min(MAX_FILES_PER_UPLOAD, MAX_FILES_PER_CLAIM - documentsOnClaim));
}

export function checkResponse(note: string, files: PickedFile[], allowed: number): string | undefined {
  if (note.length > MAX_NOTE) return `Keep the note to ${MAX_NOTE} characters or fewer.`;
  const rejected = files.filter((picked) => picked.problem);
  if (rejected.length > 0) return `Remove ${rejected.map((picked) => picked.file.name).join(', ')}: it can't be uploaded.`;
  if (files.length > allowed) return `You can attach ${allowed} more ${allowed === 1 ? 'file' : 'files'} to this claim.`;
  if (!note.trim() && files.length === 0) return 'Add a note, a file, or both.';
  return undefined;
}

type RespondRequest = components['schemas']['RespondRequest'];

/** The multipart body ClaimController.respond expects: a "response" JSON part and one "files" part each. */
export function toResponseFormData(response: RespondRequest, files: PickedFile[]): FormData {
  const body = new FormData();
  body.append('response', new Blob([JSON.stringify(response)], { type: 'application/json' }));
  for (const picked of files) {
    body.append('files', picked.file, picked.file.name);
  }
  return body;
}
