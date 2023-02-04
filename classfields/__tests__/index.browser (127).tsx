import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { SeoSiteSerpLinks } from '../';

import { mocks } from './mocks';

describe('SeoSiteSerpLinks', () => {
    it('все квартиры', async () => {
        await render(<SeoSiteSerpLinks {...mocks.allFlats} />, {
            viewport: { width: 1400, height: 400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Пришли только новостройки', async () => {
        await render(<SeoSiteSerpLinks {...mocks.onlyNewRooms} />, {
            viewport: { width: 1400, height: 400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Пришли только вторичные', async () => {
        await render(<SeoSiteSerpLinks {...mocks.onlyResaleRooms} />, {
            viewport: { width: 1400, height: 400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Не рендерится блок ссылок, если в списке нет ссылок с офферами больше 3', async () => {
        await render(<SeoSiteSerpLinks {...mocks.smallAmountOfOffers} />, {
            viewport: { width: 1400, height: 400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});

describe('SeoSiteSerpLinks Mobile', () => {
    it('все квартиры', async () => {
        await render(<SeoSiteSerpLinks {...mocks.allFlats} isMobile={true} />, {
            viewport: { width: 600, height: 1200 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Пришли только новостройки Mobile', async () => {
        await render(<SeoSiteSerpLinks {...mocks.onlyNewRooms} isMobile={true} />, {
            viewport: { width: 600, height: 1200 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Пришли только вторичные Mobile', async () => {
        await render(<SeoSiteSerpLinks {...mocks.onlyResaleRooms} isMobile={true} />, {
            viewport: { width: 600, height: 1200 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Не рендерится блок ссылок, если в списке нет ссылок с офферами больше 3 (mobile)', async () => {
        await render(<SeoSiteSerpLinks {...mocks.smallAmountOfOffers} isMobile={true} />, {
            viewport: { width: 600, height: 1200 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
