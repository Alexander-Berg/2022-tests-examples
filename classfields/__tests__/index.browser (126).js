import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/react/libs/test-helpers';

import NewSitesSearchFilters from '..';

import { getInitialState } from './mocks';

describe('NewSitesSearchFilters', () => {
    it('Рисует обычное состояние', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <NewSitesSearchFilters />
            </AppProvider>,
            { viewport: { width: 1280, height: 370 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует обычное состояние с возможностью сброса', async() => {
        await render(
            <AppProvider initialState={getInitialState({ forms: { roomsTotal: [ '1' ] } })}>
                <NewSitesSearchFilters />
            </AppProvider>,
            { viewport: { width: 1280, height: 370 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует расширенное состояние', async() => {
        await render(
            <AppProvider initialState={getInitialState({ extraShown: true })}>
                <NewSitesSearchFilters />
            </AppProvider>,
            { viewport: { width: 1280, height: 1500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует расширенное состояние c возможностью сброса', async() => {
        await render(
            <AppProvider initialState={getInitialState({ extraShown: true, forms: { dealType: true } })}>
                <NewSitesSearchFilters />
            </AppProvider>,
            { viewport: { width: 1280, height: 1500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует расширенное состояние с ограниченной высотой', async() => {
        await render(
            <AppProvider initialState={getInitialState({ extraShown: true })}>
                <NewSitesSearchFilters />
            </AppProvider>,
            { viewport: { width: 1280, height: 900 } }
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
                <NewSitesSearchFilters />
            </AppProvider>,
            { viewport: { width: 1280, height: 1350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует расширенное состояние с банками', async() => {
        await render(
            <AppProvider
                initialState={getInitialState({ extraShown: true, refinements: [
                    'directions',
                    'sub-localities',
                    'map-area'
                ],
                banks: [ {
                    id: 1,
                    name: 'Альфа-банк'
                } ] })}
            >
                <NewSitesSearchFilters />
            </AppProvider>,
            { viewport: { width: 1280, height: 1300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
