import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SitePlansList } from '..';

import { getInitialState, getPlans } from './mocks';

describe('SitePlansList', () => {
    it('рисует загрузку', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansList
                    card={{ id: 1 }}
                    plans={{ items: [] }}
                    arePlansLoading
                />
            </AppProvider>,
            { viewport: { width: 360, height: 120 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует загрузку (повторную)', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansList
                    card={{ id: 1 }}
                    plans={getPlans()}
                    arePlansLoading
                />
            </AppProvider>,
            { viewport: { width: 360, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует ошибку загрузки', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansList
                    card={{ id: 1 }}
                    plans={getPlans()}
                    arePlansFailed
                />
            </AppProvider>,
            { viewport: { width: 360, height: 260 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует список', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansList
                    card={{ id: 1 }}
                    plans={getPlans({ withoutPager: true })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует список с пагинатором', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansList
                    card={{ id: 1 }}
                    plans={getPlans()}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует список с пагинатором (последняя страница)', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansList
                    card={{ id: 1 }}
                    plans={getPlans({ page: 2 })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует пустую выдачу', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansList
                    card={{ id: 1 }}
                    plans={{ items: [] }}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
