import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect } from 'storybook/test';
import { TextArea } from './TextArea';

const meta = {
  title: 'Components/TextArea',
  component: TextArea,
  args: { label: 'What happened?', hint: 'Up to 500 characters.', count: { used: 0, max: 500 } },
  decorators: [(Story) => <div style={{ maxWidth: 358 }}><Story /></div>],
} satisfies Meta<typeof TextArea>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Empty: Story = {
  play: async ({ canvas }) => {
    await expect(canvas.getByLabelText('What happened?')).toHaveAccessibleDescription('Up to 500 characters.');
  },
};

export const Filled: Story = {
  args: {
    defaultValue: 'My checked bag arrived two days late. I bought clothes and toiletries while I waited.',
    count: { used: 85, max: 500 },
  },
};

export const WithError: Story = {
  args: { error: 'Tell us what happened.' },
  play: async ({ canvas }) => {
    await expect(canvas.getByLabelText('What happened?')).toHaveAttribute('aria-invalid', 'true');
  },
};
