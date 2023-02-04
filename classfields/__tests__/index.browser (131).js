import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/react/libs/test-helpers';

import SitesMapSearchFilters from '..';

import { getInitialState } from './mocks';

describe('SitesMapSearchFilters', () => {
    it('Рисует обычное состояние', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitesMapSearchFilters />
            </AppProvider>,
            { viewport: { width: 400, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует расширенное состояние', async() => {
        await render(
            <AppProvider initialState={getInitialState({ extraShown: true })}>
                <SitesMapSearchFilters />
            </AppProvider>,
            { viewport: { width: 400, height: 1420 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует расширенное состояние c возможностью сброса', async() => {
        await render(
            <AppProvider initialState={getInitialState({ extraShown: true, forms: { dealType: true } })}>
                <SitesMapSearchFilters />
            </AppProvider>,
            { viewport: { width: 400, height: 1420 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует расширенное состояние в регионах без метро', async() => {
        await render(
            <AppProvider
                initialState={getInitialState({ extraShown: true, refinements: [
                    'directions',
                    'sub-localities',
                    'map-area'
                ] })}
            >
                <SitesMapSearchFilters />
            </AppProvider>,
            { viewport: { width: 400, height: 1300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует расширенное состояние с ограниченной высотой', async() => {
        await render(
            <AppProvider initialState={getInitialState({ extraShown: true })}>
                <SitesMapSearchFilters />
            </AppProvider>,
            { viewport: { width: 400, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует расширенное состояние с банками', async() => {
        await render(
            <AppProvider
                initialState={getInitialState({ extraShown: true,
                    banks: [ {
                        id: 1,
                        name: 'Альфа-банк'
                    } ] })}
            >
                <SitesMapSearchFilters />
            </AppProvider>,
            { viewport: { width: 400, height: 1800 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
