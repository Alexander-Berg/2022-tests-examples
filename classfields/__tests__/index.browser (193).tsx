import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { DistrictsContainer } from '../container';

import {
    getBaseInitialStateFullList,
    getBaseInitialStateFiveDistricts,
    getBaseInitialStateWithoutDistricts,
} from './mocks';

describe('DistrictsComponent', () => {
    it('Отрисовка - полный список', async () => {
        await render(
            <AppProvider initialState={getBaseInitialStateFullList()}>
                <DistrictsContainer />
            </AppProvider>,
            { viewport: { width: 480, height: 300 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('Отрисовка - 5 районов', async () => {
        await render(
            <AppProvider initialState={getBaseInitialStateFiveDistricts()}>
                <DistrictsContainer />
            </AppProvider>,
            { viewport: { width: 480, height: 450 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка - нет районов', async () => {
        await render(
            <AppProvider initialState={getBaseInitialStateWithoutDistricts()}>
                <DistrictsContainer />
            </AppProvider>,
            { viewport: { width: 480, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
