import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect } from 'storybook/test';
import { SectionHeader } from './SectionHeader';

const meta = {
  title: 'Components/SectionHeader',
  component: SectionHeader,
  args: { id: 'policies', title: 'Your policies', badge: '1 active', link: { to: '/policies', label: 'View all' } },
  decorators: [(Story) => <div style={{ maxWidth: 358 }}><Story /></div>],
} satisfies Meta<typeof SectionHeader>;

export default meta;
type Story = StoryObj<typeof meta>;

export const WithLink: Story = {
  play: async ({ canvas }) => {
    await expect(canvas.getByRole('heading', { level: 2, name: 'Your policies' })).toBeVisible();
    await expect(canvas.getByRole('link', { name: 'View all' })).toHaveAttribute('href', '/policies');
  },
};

export const TitleOnly: Story = {
  args: { badge: undefined, link: undefined, title: 'Claim limits' },
};
