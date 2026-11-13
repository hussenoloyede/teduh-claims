import type { Meta, StoryObj } from '@storybook/react-vite';
import { Download } from 'lucide-react';
import { expect, fn } from 'storybook/test';
import { Button } from './Button';

const meta = {
  title: 'Components/Button',
  component: Button,
  args: { children: 'Start a claim', onClick: fn() },
} satisfies Meta<typeof Button>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Primary: Story = {
  // Checks a click reaches the onClick handler.
  play: async ({ args, canvas, userEvent }) => {
    await userEvent.click(canvas.getByRole('button', { name: 'Start a claim' }));
    await expect(args.onClick).toHaveBeenCalledOnce();
  },
};

export const Secondary: Story = {
  args: { variant: 'secondary', children: 'Back to claims' },
};

export const WithIcon: Story = {
  args: {
    variant: 'secondary',
    children: 'Download',
    icon: <Download size={16} aria-hidden="true" />,
  },
};

export const FullWidth: Story = {
  args: { fullWidth: true, children: 'Submit claim' },
  decorators: [(Story) => <div style={{ maxWidth: 358 }}><Story /></div>],
};

export const Loading: Story = {
  args: { loading: true, children: 'Submitting' },
  play: async ({ canvas }) => {
    await expect(canvas.getByRole('button')).toBeDisabled();
  },
};

export const Disabled: Story = {
  args: { disabled: true },
};

export const AgentButtons: Story = {
  globals: { theme: 'agent' },
  render: (args) => (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--space-3)', alignItems: 'center' }}>
      <Button {...args} size="sm">Open claim</Button>
      <Button {...args} size="sm" variant="secondary">Open claim</Button>
      <Button {...args}>Start review</Button>
      <Button {...args} variant="danger">Reject claim</Button>
    </div>
  ),
};
