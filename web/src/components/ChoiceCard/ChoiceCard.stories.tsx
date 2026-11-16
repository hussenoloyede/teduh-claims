import type { Meta, StoryObj } from '@storybook/react-vite';
import { Clock, Luggage } from 'lucide-react';
import { expect } from 'storybook/test';
import { ChoiceCard } from './ChoiceCard';

const meta = {
  title: 'Components/ChoiceCard',
  component: ChoiceCard,
  args: { name: 'claimType', value: 'FLIGHT_DELAY', title: 'Flight delay' },
  decorators: [(Story) => <div style={{ maxWidth: 358 }}><Story /></div>],
} satisfies Meta<typeof ChoiceCard>;

export default meta;
type Story = StoryObj<typeof meta>;

export const ClaimTypes: Story = {
  render: () => (
    <fieldset style={{ display: 'grid', gap: 12, border: 0, margin: 0, padding: 0 }}>
      <legend style={{ marginBottom: 12, fontWeight: 700 }}>What happened?</legend>
      <ChoiceCard
        name="claimType"
        value="FLIGHT_DELAY"
        title="Flight delay"
        aside="Up to RM 500.00"
        description="Your flight left much later than planned."
        icon={<Clock size={20} />}
      />
      <ChoiceCard
        name="claimType"
        value="BAGGAGE_DELAYED"
        title="Baggage delayed"
        aside="Up to RM 1,000.00"
        description="Your checked bag arrived late and you had to buy things you needed."
        icon={<Luggage size={20} />}
        defaultChecked
      />
    </fieldset>
  ),
  play: async ({ canvas, userEvent }) => {
    const flight = canvas.getByRole('radio', { name: /Flight delay/ });
    const baggage = canvas.getByRole('radio', { name: /Baggage delayed/ });
    await expect(baggage).toBeChecked();
    await userEvent.click(canvas.getByText('Flight delay'));
    await expect(flight).toBeChecked();
    await expect(baggage).not.toBeChecked();
  },
};
