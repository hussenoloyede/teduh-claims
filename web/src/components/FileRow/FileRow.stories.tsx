import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect, fn } from 'storybook/test';
import { FileRow } from './FileRow';

const meta = {
  title: 'Components/FileRow',
  component: FileRow,
  args: {
    fileName: 'receipt-clothes.pdf',
    contentType: 'application/pdf',
    sizeBytes: 317440,
    onDownload: fn(),
  },
  decorators: [(Story) => <div style={{ maxWidth: 358 }}><Story /></div>],
} satisfies Meta<typeof FileRow>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Pdf: Story = {
  // The button's name includes the file, so screen reader users can tell
  // several Download buttons apart.
  play: async ({ args, canvas, userEvent }) => {
    await userEvent.click(canvas.getByRole('button', { name: 'Download receipt-clothes.pdf' }));
    await expect(args.onDownload).toHaveBeenCalledOnce();
  },
};

export const Image: Story = {
  args: { fileName: 'receipt-toiletries.jpg', contentType: 'image/jpeg', sizeBytes: 1258291 },
};

export const LongName: Story = {
  args: { fileName: 'airline-delay-confirmation-letter-scanned-copy-page-1-of-2.pdf' },
};

export const Downloading: Story = {
  args: { busy: true },
  play: async ({ canvas }) => {
    await expect(canvas.getByRole('button')).toBeDisabled();
  },
};

export const Agent: Story = {
  globals: { theme: 'agent' },
};
