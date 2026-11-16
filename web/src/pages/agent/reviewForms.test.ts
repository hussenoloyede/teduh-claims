import { describe as group, expect, it } from 'vitest';
import { amountText, checkApproval, checkRejection, checkRequest, isPartial, MAX_MESSAGE } from './reviewForms';

group('checkRequest', () => {
  it('needs a message', () => {
    expect(checkRequest('   ')).toBe('A message to the customer is required.');
  });

  it('accepts up to the limit and no more', () => {
    expect(checkRequest('a'.repeat(MAX_MESSAGE))).toBeUndefined();
    expect(checkRequest('a'.repeat(MAX_MESSAGE + 1))).toBe('The message can be at most 1000 characters.');
  });
});

group('checkRejection', () => {
  it('always needs a reason', () => {
    expect(checkRejection('')).toBe('A reason is required to reject a claim.');
    expect(checkRejection('The bag arrived 3 hours late.')).toBeUndefined();
  });
});

group('checkApproval', () => {
  it('accepts the full amount without a reason', () => {
    expect(checkApproval('450.00', '', 450)).toEqual({});
  });

  it('needs a reason for less than claimed', () => {
    expect(checkApproval('400', ' ', 450)).toEqual({
      reason: 'A reason is required when approving less than the amount claimed.',
    });
    expect(checkApproval('400', 'Receipts total RM 400.', 450)).toEqual({});
  });

  it('refuses zero, more than claimed, and things that are not amounts', () => {
    expect(checkApproval('0', '', 450).amount).toBe('The approved amount must be more than zero.');
    expect(checkApproval('450.01', '', 450).amount).toBe("You can't approve more than the RM 450.00 claimed.");
    expect(checkApproval('1,000', '', 450).amount).toBe('Enter an amount like 400 or 400.50, with no commas.');
    expect(checkApproval('400.505', 'x', 450).amount).toBe('Enter an amount like 400 or 400.50, with no commas.');
  });

  it('still limits an optional reason', () => {
    expect(checkApproval('450', 'a'.repeat(MAX_MESSAGE + 1), 450).reason).toBe(
      'The reason can be at most 1000 characters.',
    );
  });
});

group('isPartial', () => {
  it('is true only for a valid amount below the claim', () => {
    expect(isPartial('400', 450)).toBe(true);
    expect(isPartial('450.00', 450)).toBe(false);
    expect(isPartial('', 450)).toBe(false);
    expect(isPartial('0', 450)).toBe(false);
  });
});

group('amountText', () => {
  it('writes two decimals with no grouping', () => {
    expect(amountText(450)).toBe('450.00');
    expect(amountText(1850.5)).toBe('1850.50');
  });
});
