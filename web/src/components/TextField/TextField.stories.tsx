import type { Meta, StoryObj } from '@storybook/react-vite';
import { Mail } from 'lucide-react';
import { expect } from 'storybook/test';
import { TextField } from './TextField';

const meta = {
  title: 'Components/TextField',
  component: TextField,
  args: { label: 'Email address', type: 'email', name: 'email' },
  decorators: [(Story) => <div style={{ maxWidth: 358 }}><Story /></div>],
} satisfies Meta<typeof TextField>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Empty: Story = {
  play: async ({ canvas, userEvent }) => {
    const input = canvas.getByLabelText('Email address');
    await userEvent.type(input, 'aisyah@example.com');
    await expect(input).toHaveValue('aisyah@example.com');
  },
};

export const WithIcon: Story = {
  args: { icon: <Mail size={18} />, defaultValue: 'aisyah@example.com' },
};

export const WithHint: Story = {
  args: { hint: 'The address your Teduh account uses.' },
  play: async ({ canvas }) => {
    await expect(canvas.getByLabelText('Email address')).toHaveAccessibleDescription(
      'The address your Teduh account uses.',
    );
  },
};

export const WithError: Story = {
  args: { defaultValue: 'aisyah@', error: 'Enter an email address like name@example.com.' },
  play: async ({ canvas }) => {
    const input = canvas.getByLabelText('Email address');
    await expect(input).toHaveAttribute('aria-invalid', 'true');
    await expect(input).toHaveAccessibleDescription('Enter an email address like name@example.com.');
  },
};

export const Disabled: Story = {
  args: { disabled: true, defaultValue: 'aisyah@example.com' },
};
