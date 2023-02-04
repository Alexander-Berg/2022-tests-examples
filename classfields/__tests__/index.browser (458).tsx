import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { RailwaysList } from '../';

import {
    getBaseInitialStateFullList,
    getBaseInitialStateFiveStations,
    getBaseInitialStateWithoutStations,
} from './mocks';

describe('Railways', () => {
    it('Отрисовка - корректно выводит список (5 станций)', async () => {
        await render(
            <AppProvider initialState={getBaseInitialStateFiveStations()}>
                <RailwaysList />
            </AppProvider>,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка - корректно выводит список (~Все станции МСК)', async () => {
        await render(
            <AppProvider initialState={getBaseInitialStateFullList()}>
                <RailwaysList />
            </AppProvider>,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('Отрисовка - нет станицй', async () => {
        await render(
            <AppProvider initialState={getBaseInitialStateWithoutStations()}>
                <RailwaysList />
            </AppProvider>,
            { viewport: { width: 1000, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
