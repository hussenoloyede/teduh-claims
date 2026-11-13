import type { Preview } from '@storybook/react-vite';
import '../src/index.css';

const preview: Preview = {
  // A toolbar switch between the customer and agent colours.
  globalTypes: {
    theme: {
      description: 'Customer or agent colours',
      toolbar: {
        title: 'Theme',
        icon: 'paintbrush',
        items: [
          { value: 'customer', title: 'Customer' },
          { value: 'agent', title: 'Agent' },
        ],
        dynamicTitle: true,
      },
    },
  },
  initialGlobals: { theme: 'customer' },

  // Wraps every story. data-theme="agent" switches the tokens to blue.
  decorators: [
    (Story, { globals }) => (
      <div
        data-theme={globals.theme === 'agent' ? 'agent' : undefined}
        style={{ padding: 'var(--space-4)', background: 'var(--color-bg-page)' }}
      >
        <Story />
      </div>
    ),
  ],

  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
    // 'error' makes an accessibility problem fail the test run (and the
    // pipeline later), not just show a warning in the panel.
    a11y: { test: 'error' },
  },
};

export default preview;
