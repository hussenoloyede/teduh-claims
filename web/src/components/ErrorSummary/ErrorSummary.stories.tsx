import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect } from 'storybook/test';
import { ErrorSummary } from './ErrorSummary';

const meta = {
  title: 'Components/ErrorSummary',
  component: ErrorSummary,
  args: {
    errors: [
      { fieldId: 'amount', message: "That's more than your baggage delayed limit of RM 1,000.00." },
      { fieldId: 'description', message: 'Tell us what happened.' },
    ],
  },
  decorators: [
    (Story) => (
      <div style={{ maxWidth: 358, display: 'grid', gap: 12 }}>
        <Story />
        <label htmlFor="amount">Amount</label>
        <input id="amount" defaultValue="1200" />
        <label htmlFor="description">What happened?</label>
        <textarea id="description" />
      </div>
    ),
  ],
} satisfies Meta<typeof ErrorSummary>;

export default meta;
type Story = StoryObj<typeof meta>;

export const TwoErrors: Story = {
  play: async ({ canvas, userEvent }) => {
    await expect(canvas.getByText('There are 2 things to fix')).toBeVisible();
    await userEvent.click(canvas.getByRole('link', { name: 'Tell us what happened.' }));
    await expect(canvas.getByLabelText('What happened?')).toHaveFocus();
  },
};

export const OneError: Story = {
  args: { errors: [{ fieldId: 'amount', message: 'Enter how much you are claiming.' }] },
};
