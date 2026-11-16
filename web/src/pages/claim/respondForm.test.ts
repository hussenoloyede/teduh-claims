import { describe, expect, it } from 'vitest';
import type { PickedFile } from '../newClaim/newClaimForm';
import { checkResponse, filesAllowed, toResponseFormData } from './respondForm';

const pdf = (name: string): PickedFile => ({ id: name, file: new File(['%PDF-'], name, { type: 'application/pdf' }) });

describe('filesAllowed', () => {
  it('allows five at a time and fifteen on a claim', () => {
    expect(filesAllowed(2)).toBe(5);
    expect(filesAllowed(12)).toBe(3);
    expect(filesAllowed(15)).toBe(0);
  });
});

describe('checkResponse', () => {
  it('needs a note, a file, or both', () => {
    expect(checkResponse('  ', [], 5)).toBe('Add a note, a file, or both.');
    expect(checkResponse('Here it is.', [], 5)).toBeUndefined();
    expect(checkResponse('', [pdf('report.pdf')], 5)).toBeUndefined();
  });

  it('refuses a long note, rejected files and too many files', () => {
    expect(checkResponse('x'.repeat(501), [], 5)).toContain('500 characters');
    expect(checkResponse('', [{ ...pdf('pass.heic'), problem: 'no' }], 5)).toBe("Remove pass.heic: it can't be uploaded.");
    expect(checkResponse('', [pdf('a.pdf'), pdf('b.pdf')], 1)).toBe('You can attach 1 more file to this claim.');
  });
});

describe('toResponseFormData', () => {
  it('builds the response part and one files part per document', async () => {
    const body = toResponseFormData({ version: 2, note: 'Here it is.' }, [pdf('report.pdf')]);
    expect(JSON.parse(await (body.get('response') as Blob).text())).toEqual({ version: 2, note: 'Here it is.' });
    expect((body.getAll('files') as File[]).map((file) => file.name)).toEqual(['report.pdf']);
  });
});
