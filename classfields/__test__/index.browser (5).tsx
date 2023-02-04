import React from 'react';

import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/public/reducer';

import { LandingOwner } from '../index';

import * as stubs from './stubs';

const renderOptions = [
    {
        viewport: {
            width: 1280,
            height: 1000,
        },
    },
    {
        isMobile: true,
        viewport: {
            width: 1024,
            height: 1000,
        },
    },
    {
        isMobile: true,
        viewport: {
            width: 768,
            height: 1000,
        },
    },
    {
        isMobile: true,
        viewport: {
            width: 414,
            height: 1000,
        },
    },
    {
        isMobile: true,
        viewport: {
            width: 356,
            height: 1000,
        },
    },
];

const Component: React.FunctionComponent<{ isMobile?: boolean }> = ({ isMobile }) => (
    <AppProvider
        rootReducer={rootReducer}
        initialState={{ ...stubs.store, config: { ...stubs.store.config, isMobile } }}
    >
        <LandingOwner />
    </AppProvider>
);

describe('LandingOwner', () => {
    renderOptions.forEach(({ isMobile, viewport }) => {
        it(`viewport:${viewport.width}px`, async () => {
            await render(<Component isMobile={isMobile} />, { viewport });

            await customPage.waitForAllImagesLoaded();

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });
});
