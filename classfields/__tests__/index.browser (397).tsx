import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import OfferCardTags from '../';

import fastlinks from './mock';

describe('OfferSerpTags', () => {
    it('Базовая отрисовка с котроллерами', async () => {
        await render(
            <AppProvider>
                <OfferCardTags fastlinks={fastlinks} />
            </AppProvider>,
            { viewport: { width: 1200, height: 140 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Базовая отрисовка без контроллеров', async () => {
        await render(
            <AppProvider>
                <OfferCardTags fastlinks={fastlinks.slice(0, 4)} />
            </AppProvider>,
            { viewport: { width: 1200, height: 140 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Скролл впарво по клику на контроллер', async () => {
        await render(
            <AppProvider>
                <OfferCardTags fastlinks={fastlinks} />
            </AppProvider>,
            { viewport: { width: 1200, height: 140 } }
        );

        await page.click('[class*="nextButton"]');
        await page.waitFor(1000);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
