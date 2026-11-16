import { describe, expect, it } from 'vitest';
import {
  checkAmount,
  checkDescription,
  checkFile,
  checkFiles,
  checkIncidentDate,
  latestIncidentDate,
  toFormData,
  toSubmitRequest,
  type PickedFile,
} from './newClaimForm';

const cover = { coverStart: '2026-10-01', coverEnd: '2026-12-30' };
const today = '2026-11-16';

describe('checkIncidentDate', () => {
  it('accepts a date inside the cover, up to today', () => {
    expect(checkIncidentDate('2026-10-01', cover, today)).toBeUndefined();
    expect(checkIncidentDate(today, cover, today)).toBeUndefined();
  });

  it('refuses a missing, future or uncovered date', () => {
    expect(checkIncidentDate('', cover, today)).toBe('Enter the date it happened.');
    expect(checkIncidentDate('2026-11-17', cover, today)).toBe("The date can't be in the future.");
    expect(checkIncidentDate('2026-09-30', cover, today)).toContain('between 1 Oct 2026 and 30 Dec 2026');
  });

  it('stops at the end of cover when that came before today', () => {
    expect(latestIncidentDate(cover, today)).toBe(today);
    expect(latestIncidentDate({ coverEnd: '2026-11-01' }, today)).toBe('2026-11-01');
  });
});

describe('checkAmount', () => {
  it('accepts whole ringgit and two decimals within the limit', () => {
    expect(checkAmount('450', 1000, 'baggage delayed')).toBeUndefined();
    expect(checkAmount(' 1000.00 ', 1000, 'baggage delayed')).toBeUndefined();
  });

  it('refuses text, commas, three decimals, zero and amounts over the limit', () => {
    expect(checkAmount('', 1000, 'x')).toBe('Enter how much you are claiming.');
    expect(checkAmount('1,000', 1000, 'x')).toContain('like 450 or 450.50');
    expect(checkAmount('4.505', 1000, 'x')).toContain('like 450 or 450.50');
    expect(checkAmount('0', 1000, 'x')).toBe('The amount must be more than zero.');
    expect(checkAmount('1000.01', 1000, 'baggage delayed')).toBe(
      "That's more than your baggage delayed limit of RM 1,000.00.",
    );
  });
});

describe('checkDescription', () => {
  it('needs some text and at most 500 characters', () => {
    expect(checkDescription('   ')).toBe('Tell us what happened.');
    expect(checkDescription('x'.repeat(500))).toBeUndefined();
    expect(checkDescription('x'.repeat(501))).toContain('500 characters');
  });
});

describe('checkFile', () => {
  it('accepts PDF, JPG and PNG by type or by name', () => {
    expect(checkFile({ name: 'receipt.pdf', type: 'application/pdf', size: 1000 })).toBeUndefined();
    expect(checkFile({ name: 'photo.JPG', type: '', size: 1000 })).toBeUndefined();
  });

  it('refuses other types, empty files and files over 10 MB', () => {
    expect(checkFile({ name: 'boarding-pass.heic', type: 'image/heic', size: 1000 })).toContain("isn't accepted");
    expect(checkFile({ name: 'a.pdf', type: 'application/pdf', size: 0 })).toBe('This file is empty.');
    expect(checkFile({ name: 'a.pdf', type: 'application/pdf', size: 10 * 1024 * 1024 + 1 })).toContain('10 MB');
  });
});

describe('checkFiles', () => {
  const ok = (id: string): PickedFile => ({ id, file: new File(['x'], `${id}.pdf`) });

  it('needs one to five files and no rejected ones', () => {
    expect(checkFiles([])).toContain('at least one');
    expect(checkFiles([ok('a')])).toBeUndefined();
    expect(checkFiles(['a', 'b', 'c', 'd', 'e', 'f'].map(ok))).toBe('Attach up to 5 files.');
    const bad = { id: 'h', file: new File(['x'], 'pass.heic'), problem: 'no' };
    expect(checkFiles([ok('a'), bad])).toBe("Remove pass.heic: it can't be uploaded.");
  });
});

describe('toSubmitRequest and toFormData', () => {
  it('builds the JSON part and one files part per document', async () => {
    const claim = toSubmitRequest({
      policyId: 'p1',
      claimType: 'BAGGAGE_DELAYED',
      incidentDate: '2026-11-14',
      amount: ' 450.50 ',
      description: '  Bag arrived two days late.  ',
      files: [],
      confirmed: true,
    });
    expect(claim).toEqual({
      policyId: 'p1',
      claimType: 'BAGGAGE_DELAYED',
      incidentDate: '2026-11-14',
      amountClaimed: 450.5,
      description: 'Bag arrived two days late.',
    });

    const files = [{ id: '1', file: new File(['%PDF-'], 'receipt.pdf', { type: 'application/pdf' }) }];
    const body = toFormData(claim, files);
    const part = body.get('claim') as Blob;
    expect(part.type).toBe('application/json');
    expect(JSON.parse(await part.text())).toEqual(claim);
    expect((body.getAll('files') as File[]).map((file) => file.name)).toEqual(['receipt.pdf']);
  });
});
