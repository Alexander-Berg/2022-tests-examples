import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { ApartmentType } from 'realty-core/types/siteCard';

import { SitePlansV2Serp } from '..';

import { getProps } from './mocks';

describe('SitePlansV2Serp', () => {
    it('Рисует выдачу планировок квартир и открытой сортировкой', async () => {
        await render(
            <AppProvider>
                <SitePlansV2Serp {...getProps(ApartmentType.FLATS)} />
            </AppProvider>,
            { viewport: { width: 320, height: 1300 } }
        );

        await page.click('.Select');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует выдачу планировок апартаментов', async () => {
        await render(
            <AppProvider>
                <SitePlansV2Serp {...getProps(ApartmentType.APARTMENTS)} />
            </AppProvider>,
            { viewport: { width: 320, height: 1300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует выдачу планировок квартир и апартаментов', async () => {
        await render(
            <AppProvider>
                <SitePlansV2Serp {...getProps(ApartmentType.APARTMENTS_AND_FLATS)} />
            </AppProvider>,
            { viewport: { width: 320, height: 1300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
