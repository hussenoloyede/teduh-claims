import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect, fn } from 'storybook/test';
import { SignInForm } from './SignInForm';

const meta = {
  title: 'Screens/SignInForm',
  component: SignInForm,
  args: { onDemo: fn(), onSubmit: fn() },
  parameters: { layout: 'fullscreen' },
} satisfies Meta<typeof SignInForm>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Empty: Story = {
  play: async ({ args, canvas, userEvent }) => {
    await userEvent.type(canvas.getByLabelText('Email address'), 'aisyah@example.com');
    await userEvent.type(canvas.getByLabelText('Password'), 'teduh-demo');
    await userEvent.click(canvas.getByRole('button', { name: 'Sign in' }));
    await expect(args.onSubmit).toHaveBeenCalledWith('aisyah@example.com', 'teduh-demo');
  },
};

export const DemoButtons: Story = {
  play: async ({ args, canvas, userEvent }) => {
    await userEvent.click(canvas.getByRole('button', { name: /Try as agent/ }));
    await expect(args.onDemo).toHaveBeenCalledWith('agent');
  },
};

export const ShowPassword: Story = {
  play: async ({ canvas, userEvent }) => {
    const password = canvas.getByLabelText('Password');
    await expect(password).toHaveAttribute('type', 'password');
    const toggle = canvas.getByRole('button', { name: 'Show password' });
    await userEvent.click(toggle);
    await expect(password).toHaveAttribute('type', 'text');
    await expect(toggle).toHaveAttribute('aria-pressed', 'true');
  },
};

export const WrongPassword: Story = {
  args: { error: "That email and password don't match." },
  play: async ({ canvas }) => {
    await expect(canvas.getByRole('alert')).toHaveTextContent("That email and password don't match.");
  },
};

export const SigningInAsCustomer: Story = {
  args: { pending: 'customer' },
  play: async ({ canvas }) => {
    await expect(canvas.getByRole('button', { name: /Signing in/ })).toBeDisabled();
    await expect(canvas.getByRole('button', { name: 'Sign in' })).toBeDisabled();
  },
};
