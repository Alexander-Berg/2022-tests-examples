import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/react/libs/test-helpers';

import SiteCardFilters from '..';

import { getInitialState } from './mocks';

describe('SiteCardFilters', () => {
    it('рисует закрытое состояние фильтра', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SiteCardFilters />
            </AppProvider>,
            { viewport: { width: 840, height: 230 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует открытое состояние фильтра', async() => {
        await render(
            <AppProvider initialState={getInitialState({ extraShown: true })}>
                <SiteCardFilters />
            </AppProvider>,
            { viewport: { width: 840, height: 460 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует открытый селект выбора срока сдачи и корпуса', async() => {
        await render(
            <AppProvider initialState={getInitialState({ extraShown: true })}>
                <SiteCardFilters />
            </AppProvider>,
            { viewport: { width: 840, height: 460 } }
        );

        await page.click('.FiltersFormField__houseId');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует закрытое состояние фильтра без доступных корпусов', async() => {
        await render(
            <AppProvider initialState={getInitialState({ houseIdValues: [] })}>
                <SiteCardFilters />
            </AppProvider>,
            { viewport: { width: 840, height: 230 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
