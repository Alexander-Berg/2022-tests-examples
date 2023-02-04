import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/react/libs/test-helpers';

import SitePlansList from '..';

import { getPlans } from './mocks';

describe('SitePlansList', () => {
    it('рисует загрузку', async() => {
        await render(
            <AppProvider>
                <SitePlansList
                    plans={{ items: [] }}
                    arePlansLoading
                    apartmentsType="FLATS"
                />
            </AppProvider>,
            { viewport: { width: 840, height: 120 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует загрузку (повторную)', async() => {
        await render(
            <AppProvider>
                <SitePlansList
                    plans={getPlans()}
                    arePlansLoading
                    apartmentsType="FLATS"
                />
            </AppProvider>,
            { viewport: { width: 840, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует ошибку загрузки', async() => {
        await render(
            <AppProvider>
                <SitePlansList
                    plans={getPlans()}
                    arePlansFailed
                    apartmentsType="FLATS"
                />
            </AppProvider>,
            { viewport: { width: 840, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует список', async() => {
        await render(
            <AppProvider>
                <SitePlansList
                    plans={getPlans({ withoutPager: true })}
                    apartmentsType="FLATS"
                />
            </AppProvider>,
            { viewport: { width: 840, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует список (апартаменты)', async() => {
        await render(
            <AppProvider>
                <SitePlansList
                    plans={getPlans({ withoutPager: true })}
                    apartmentsType="APARTMENTS"
                />
            </AppProvider>,
            { viewport: { width: 840, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует список (смешанная выдача)', async() => {
        await render(
            <AppProvider>
                <SitePlansList
                    plans={getPlans({ withoutPager: true })}
                    apartmentsType="APARTMENTS_AND_FLATS"
                />
            </AppProvider>,
            { viewport: { width: 840, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует список с пагинатором', async() => {
        await render(
            <AppProvider>
                <SitePlansList
                    plans={getPlans()}
                    apartmentsType="FLATS"
                />
            </AppProvider>,
            { viewport: { width: 840, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует список с пагинатором (2 страница)', async() => {
        await render(
            <AppProvider>
                <SitePlansList
                    plans={getPlans({ page: 2 })}
                    apartmentsType="FLATS"
                />
            </AppProvider>,
            { viewport: { width: 840, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует пустую выдачу', async() => {
        await render(
            <AppProvider>
                <SitePlansList
                    plans={{ items: [] }}
                    apartmentsType="FLATS"
                />
            </AppProvider>,
            { viewport: { width: 840, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
