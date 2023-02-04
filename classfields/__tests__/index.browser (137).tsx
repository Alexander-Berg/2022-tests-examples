import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { ConciergeServiceBanner } from '..';

describe('ConciergeServiceBanner', () => {
    it('дефолтное состояние size s', async () => {
        await render(
            <AppProvider>
                <ConciergeServiceBanner size="s" onClick={() => void 0} pageName="somePage" />
            </AppProvider>,
            {
                viewport: { width: 320, height: 380 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('дефолтное состояние size xl 320', async () => {
        await render(
            <AppProvider>
                <ConciergeServiceBanner size="xl" onClick={() => void 0} pageName="somePage" />
            </AppProvider>,
            {
                viewport: { width: 320, height: 400 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('дефолтное состояние size xl 375', async () => {
        await render(
            <AppProvider>
                <ConciergeServiceBanner size="xl" onClick={() => void 0} pageName="somePage" />
            </AppProvider>,
            {
                viewport: { width: 375, height: 400 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('дефолтное состояние size xl 414', async () => {
        await render(
            <AppProvider>
                <ConciergeServiceBanner size="xl" onClick={() => void 0} pageName="somePage" />
            </AppProvider>,
            {
                viewport: { width: 414, height: 400 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('дефолтное состояние size xl 600', async () => {
        await render(
            <AppProvider>
                <ConciergeServiceBanner size="xl" onClick={() => void 0} pageName="somePage" />
            </AppProvider>,
            {
                viewport: { width: 600, height: 500 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
