import type { Meta, StoryObj } from '@storybook/react-vite';
import { Avatar } from './Avatar';

const meta = {
  title: 'Components/Avatar',
  component: Avatar,
  args: { name: 'Aisyah Rahman' },
} satisfies Meta<typeof Avatar>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Customer: Story = {};

export const Agent: Story = {
  args: { name: 'Farid Hassan' },
  globals: { theme: 'agent' },
};
