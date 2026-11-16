import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect } from 'storybook/test';
import { Stepper } from './Stepper';

const steps = ['Policy and claim type', 'What happened', 'Documents', 'Check and submit'];

const meta = {
  title: 'Components/Stepper',
  component: Stepper,
  args: { current: 1, steps },
  decorators: [(Story) => <div style={{ maxWidth: 358 }}><Story /></div>],
} satisfies Meta<typeof Stepper>;

export default meta;
type Story = StoryObj<typeof meta>;

export const FirstStep: Story = {
  play: async ({ canvas }) => {
    await expect(canvas.getByRole('progressbar')).toHaveAttribute('aria-valuetext', 'Step 1 of 4: Policy and claim type');
  },
};

export const SecondStep: Story = { args: { current: 2 } };

export const LastStep: Story = { args: { current: 4 } };
