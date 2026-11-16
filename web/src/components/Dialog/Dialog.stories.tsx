import type { Meta, StoryObj } from '@storybook/react-vite';
import { useState } from 'react';
import { expect, fn, screen, waitFor } from 'storybook/test';
import { Banner } from '../Banner/Banner';
import { Button } from '../Button/Button';
import { TextArea } from '../TextArea/TextArea';
import { Dialog } from './Dialog';

// Each story opens the dialog from a button, as the claim review page does.
function Example({ tone, onConfirm }: { tone?: 'default' | 'danger'; onConfirm: () => void }) {
  const [open, setOpen] = useState(false);
  const danger = tone === 'danger';
  return (
    <>
      <Button variant={danger ? 'danger' : 'primary'} onClick={() => setOpen(true)}>
        {danger ? 'Reject claim' : 'Request more information'}
      </Button>
      <Dialog
        open={open}
        tone={tone}
        title={danger ? 'Reject claim CLM-2026-000017' : 'Ask Aisyah Rahman for more information'}
        subtitle="CLM-2026-000017 · Baggage delayed · RM 450.00"
        onClose={() => setOpen(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button
              variant={danger ? 'danger' : 'primary'}
              onClick={() => {
                onConfirm();
                setOpen(false);
              }}
            >
              {danger ? 'Reject claim' : 'Send request'}
            </Button>
          </>
        }
      >
        {danger && (
          <Banner variant="warning" title="This decision is final">
            The claim can't be reopened. Aisyah Rahman will see your reason.
          </Banner>
        )}
        <TextArea label={danger ? 'Reason (shown to the customer)' : 'What do you need?'} rows={4} />
      </Dialog>
    </>
  );
}

const meta = {
  title: 'Components/Dialog',
  component: Example,
  args: { onConfirm: fn() },
  globals: { theme: 'agent' },
} satisfies Meta<typeof Example>;

export default meta;
type Story = StoryObj<typeof meta>;

export const RequestInformation: Story = {
  play: async ({ args, canvas, userEvent }) => {
    await userEvent.click(canvas.getByRole('button', { name: 'Request more information' }));
    const dialog = await screen.findByRole('dialog', { name: 'Ask Aisyah Rahman for more information' });
    await expect(dialog).toBeVisible();
    await userEvent.click(screen.getByRole('button', { name: 'Send request' }));
    await expect(args.onConfirm).toHaveBeenCalledOnce();
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
  },
};

export const Reject: Story = {
  args: { tone: 'danger' },
  play: async ({ args, canvas, userEvent }) => {
    await userEvent.click(canvas.getByRole('button', { name: 'Reject claim' }));
    await screen.findByRole('dialog', { name: 'Reject claim CLM-2026-000017' });
    // Close leaves without deciding anything. (Escape does the same in a real browser; the test's simulated
    // key press doesn't reach the browser's own dialog handling, so the page tests check Escape instead.)
    await userEvent.click(screen.getByRole('button', { name: 'Close' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    await expect(args.onConfirm).not.toHaveBeenCalled();
  },
};
