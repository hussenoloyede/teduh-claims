import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect, fn } from 'storybook/test';
import { PlanCard } from './PlanCard';

const plusLimits = [
  { label: 'Flight delay', limit: 500 },
  { label: 'Baggage delayed', limit: 1000 },
  { label: 'Lost or damaged baggage', limit: 3000 },
  { label: 'Trip cancellation', limit: 10000 },
];

const meta = {
  title: 'Components/PlanCard',
  component: PlanCard,
  args: {
    title: 'Teduh Travel Plus',
    name: 'plan',
    blurb: 'Good cover for most trips',
    price: 49,
    limits: plusLimits,
    value: 'PLUS',
    onChange: fn(),
  },
  decorators: [(Story) => <div style={{ maxWidth: 400 }}>{Story()}</div>],
} satisfies Meta<typeof PlanCard>;

export default meta;
type Story = StoryObj<typeof meta>;

export const NotChosen: Story = {
  args: { checked: false },
  play: async ({ args, canvas, userEvent }) => {
    const radio = canvas.getByRole('radio', { name: 'Teduh Travel Plus' });
    await expect(radio).toHaveAccessibleDescription(/Good cover for most trips.*RM 49 per trip, demo price/);
    await userEvent.click(canvas.getByText('Teduh Travel Plus'));
    await expect(args.onChange).toHaveBeenCalled();
  },
};

export const Chosen: Story = {
  args: { checked: true },
  play: async ({ canvas }) => {
    await expect(canvas.getByRole('radio', { name: 'Teduh Travel Plus' })).toBeChecked();
    await expect(canvas.getByText('RM 10,000')).toBeVisible();
  },
};
