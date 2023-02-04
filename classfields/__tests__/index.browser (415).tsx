import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { Districts } from '../index';

import {
    getBaseInitialStateFullList,
    getBaseInitialStateFiveDistricts,
    getBaseInitialStateWithoutDistricts,
} from './mocks';

describe('Districts', () => {
    it('Отрисовка - корректно выводит список (5 районов)', async () => {
        await render(
            <AppProvider initialState={getBaseInitialStateFiveDistricts()}>
                <Districts />
            </AppProvider>,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка - корректно выводит список (Все районы МСК)', async () => {
        await render(
            <AppProvider initialState={getBaseInitialStateFullList()}>
                <Districts />
            </AppProvider>,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('Отрисовка - нет районов', async () => {
        await render(
            <AppProvider initialState={getBaseInitialStateWithoutDistricts()}>
                <Districts />
            </AppProvider>,
            { viewport: { width: 1000, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
