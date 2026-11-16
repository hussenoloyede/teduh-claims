import type { Meta, StoryObj } from '@storybook/react-vite';
import { useState } from 'react';
import { expect } from 'storybook/test';
import { FilterChips } from './FilterChips';

type Filter = 'all' | 'open' | 'decided';
const options = [
  { value: 'all' as const, label: 'All claims', count: 3 },
  { value: 'open' as const, label: 'In progress', count: 1 },
  { value: 'decided' as const, label: 'Decided', count: 2 },
];

function Example() {
  const [value, setValue] = useState<Filter>('all');
  return <FilterChips label="Show claims" options={options} value={value} onChange={setValue} />;
}

const meta = {
  title: 'Components/FilterChips',
  component: Example,
} satisfies Meta<typeof Example>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Claims: Story = {
  play: async ({ canvas, userEvent }) => {
    const all = canvas.getByRole('button', { name: /All claims/ });
    const decided = canvas.getByRole('button', { name: /Decided/ });
    await expect(all).toHaveAttribute('aria-pressed', 'true');
    await userEvent.click(decided);
    await expect(decided).toHaveAttribute('aria-pressed', 'true');
    await expect(all).toHaveAttribute('aria-pressed', 'false');
  },
};
