import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect } from 'storybook/test';
import { BottomNav } from './BottomNav';

const meta = {
  title: 'Components/BottomNav',
  component: BottomNav,
  args: { current: 'home' },
  decorators: [(Story) => <div style={{ maxWidth: 390 }}><Story /></div>],
} satisfies Meta<typeof BottomNav>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Home: Story = {};

export const Claims: Story = {
  args: { current: 'claims' },
  play: async ({ canvas }) => {
    await expect(canvas.getByRole('link', { name: 'Claims' })).toHaveAttribute('aria-current', 'page');
    await expect(canvas.getByRole('link', { name: 'Home' })).not.toHaveAttribute('aria-current');
  },
};

export const Profile: Story = {
  args: { current: 'profile' },
};
