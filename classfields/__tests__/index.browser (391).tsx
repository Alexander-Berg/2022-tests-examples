import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/react/libs/test-helpers';

import { OfferCardSitePreview } from '../index';

import { offer, site, siteWithLongTitle, siteWithManyBadges, siteWithOneFinishDate, siteWithoutBadges } from './mocks';

describe('OfferCardSitePreview', () => {
    it('Базовая отрисовка', async () => {
        await render(
            <AppProvider>
                <OfferCardSitePreview offer={offer} site={site} />
            </AppProvider>,
            {
                viewport: { width: 900, height: 250 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка информации о ЖК с длинным названием', async () => {
        await render(
            <AppProvider>
                <OfferCardSitePreview offer={offer} site={siteWithLongTitle} />
            </AppProvider>,
            {
                viewport: { width: 900, height: 250 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка информации о ЖК без бейджей', async () => {
        await render(
            <AppProvider>
                <OfferCardSitePreview offer={offer} site={siteWithoutBadges} />
            </AppProvider>,
            {
                viewport: { width: 900, height: 250 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка информации о ЖК с периодом дат сдачи', async () => {
        await render(
            <AppProvider>
                <OfferCardSitePreview offer={offer} site={siteWithManyBadges} />
            </AppProvider>,
            {
                viewport: { width: 900, height: 250 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка информации о ЖК с одной датой сдачи', async () => {
        await render(
            <AppProvider>
                <OfferCardSitePreview offer={offer} site={siteWithOneFinishDate} />
            </AppProvider>,
            {
                viewport: { width: 900, height: 250 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
