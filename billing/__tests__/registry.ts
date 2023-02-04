import { initialize as initializeDesktop } from '../../../.storybook/registry.desktop';
import { initialize as initializeMobile } from '../../../.storybook/registry.mobile';

export const initializeDesktopRegistry = () => {
    initializeDesktop();
};

export const initializeMobileRegistry = () => {
    initializeMobile();
};
