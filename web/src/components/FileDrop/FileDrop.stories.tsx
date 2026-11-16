import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect, fn } from 'storybook/test';
import { FileDrop } from './FileDrop';

const meta = {
  title: 'Components/FileDrop',
  component: FileDrop,
  args: { files: [], max: 5, onAdd: fn(), onRemove: fn() },
  decorators: [(Story) => <div style={{ maxWidth: 358 }}><Story /></div>],
} satisfies Meta<typeof FileDrop>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Empty: Story = {
  play: async ({ canvas }) => {
    await expect(canvas.getByRole('button', { name: 'Choose files' })).toBeVisible();
  },
};

export const WithFiles: Story = {
  args: {
    files: [
      { id: '1', name: 'receipt-clothes.pdf', size: 317440 },
      { id: '2', name: 'receipt-toiletries.jpg', size: 1258291 },
      { id: '3', name: 'boarding-pass.heic', size: 4299161, problem: "This file type isn't accepted. Use PDF, JPG or PNG." },
    ],
    error: "Remove boarding-pass.heic: it can't be uploaded.",
  },
  play: async ({ args, canvas, userEvent }) => {
    await expect(canvas.getByText('2 of 5')).toBeVisible();
    await userEvent.click(canvas.getByRole('button', { name: 'Remove boarding-pass.heic' }));
    await expect(args.onRemove).toHaveBeenCalledWith('3');
  },
};

export const NothingAttached: Story = {
  args: { error: 'Attach at least one document, such as a receipt or a letter from the airline.' },
};
