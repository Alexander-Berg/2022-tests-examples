import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { RailwayList } from '../';

import {
    getBaseInitialStateFullList,
    getBaseInitialStateFiveStations,
    getBaseInitialStateWithoutStations,
} from './mocks';

describe('RailwayList', () => {
    it('Отрисовка - полный список', async () => {
        await render(
            <AppProvider initialState={getBaseInitialStateFullList()}>
                <RailwayList />
            </AppProvider>,
            { viewport: { width: 480, height: 300 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('Отрисовка - 5 станций', async () => {
        await render(
            <AppProvider initialState={getBaseInitialStateFiveStations()}>
                <RailwayList />
            </AppProvider>,
            { viewport: { width: 480, height: 450 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка - нет станций', async () => {
        await render(
            <AppProvider initialState={getBaseInitialStateWithoutStations()}>
                <RailwayList />
            </AppProvider>,
            { viewport: { width: 480, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
