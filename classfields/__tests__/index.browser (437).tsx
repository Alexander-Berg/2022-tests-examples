import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { MetroStationsContainer } from '../container';

import {
    getBaseInitialStateWithOneBlock,
    getBaseInitialStateWithoutBlocks,
    getBaseInitialStateWithDifferentStationsBlock,
    getStateWithManyStations,
} from './mocks';

describe('MetroStationsBlock', () => {
    it('Отрисовка - один блок', async () => {
        await render(
            <AppProvider initialState={getBaseInitialStateWithOneBlock()}>
                <MetroStationsContainer />
            </AppProvider>,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Станции должны попадать в один блок, если их названия начинаются с одной буквы в разных кейсах', async () => {
        await render(
            <AppProvider initialState={getBaseInitialStateWithDifferentStationsBlock()}>
                <MetroStationsContainer />
            </AppProvider>,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка - нет блоков', async () => {
        await render(
            <AppProvider initialState={getBaseInitialStateWithoutBlocks()}>
                <MetroStationsContainer />
            </AppProvider>,
            { viewport: { width: 1000, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка - много станций метро', async () => {
        await render(
            <AppProvider initialState={getStateWithManyStations()}>
                <MetroStationsContainer />
            </AppProvider>,
            { viewport: { width: 1000, height: 1700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
