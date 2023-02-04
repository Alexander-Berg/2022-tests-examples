import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/react/libs/test-helpers';

import { SitesSearchFilters } from '..';
import { SitesSearchFiltersType } from '../types';

import { getInitialState, getFilledInitialState } from './mocks';

describe('SitesSearchFilters', () => {
    it('Рисует обычное состояние', async () => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitesSearchFilters type={SitesSearchFiltersType.SERP} />
            </AppProvider>,
            { viewport: { width: 1280, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует обычное состояние (узкий экран)', async () => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitesSearchFilters type={SitesSearchFiltersType.SERP} />
            </AppProvider>,
            { viewport: { width: 1000, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует заполненный фильтр', async () => {
        await render(
            <AppProvider initialState={getFilledInitialState()}>
                <SitesSearchFilters type={SitesSearchFiltersType.SERP} />
            </AppProvider>,
            { viewport: { width: 1280, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует заполненный фильтр (узкий экран)', async () => {
        await render(
            <AppProvider initialState={getFilledInitialState()}>
                <SitesSearchFilters type={SitesSearchFiltersType.SERP} />
            </AppProvider>,
            { viewport: { width: 1000, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Открытый контрол цены', async () => {
        await render(
            <AppProvider initialState={getFilledInitialState()}>
                <SitesSearchFilters type={SitesSearchFiltersType.SERP} />
            </AppProvider>,
            { viewport: { width: 1000, height: 300 } }
        );

        await page.click('.FiltersFormField_name_price-with-type-button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Открытый контрол гео', async () => {
        await render(
            <AppProvider initialState={getFilledInitialState()}>
                <SitesSearchFilters type={SitesSearchFiltersType.SERP} />
            </AppProvider>,
            { viewport: { width: 1000, height: 300 } }
        );

        await page.click('.FiltersFormField__refinements');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Открытая модалка', async () => {
        await render(
            <AppProvider initialState={getFilledInitialState()}>
                <SitesSearchFilters type={SitesSearchFiltersType.SERP} />
            </AppProvider>,
            { viewport: { width: 1340, height: 2000 } }
        );

        await page.click('[data-test=SitesSearchFiltersModalButton]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Открытая модалка (ограничение по высоте)', async () => {
        await render(
            <AppProvider initialState={getFilledInitialState()}>
                <SitesSearchFilters type={SitesSearchFiltersType.SERP} />
            </AppProvider>,
            { viewport: { width: 1340, height: 1000 } }
        );

        await page.click('[data-test=SitesSearchFiltersModalButton]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
