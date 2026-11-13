import type { StorybookConfig } from '@storybook/react-vite';

const config: StorybookConfig = {
  stories: ['../src/**/*.stories.@(ts|tsx)'],
  addons: [
    '@storybook/addon-vitest', // runs every story as a test
    '@storybook/addon-a11y', // accessibility checks on every story
    '@storybook/addon-docs', // a docs page per component
  ],
  framework: '@storybook/react-vite',
  core: { disableTelemetry: true }, // don't send usage data to Storybook
};

export default config;
