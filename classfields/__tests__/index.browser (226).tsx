import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { MetroStationsContainer } from '../container';

import {
    getBaseInitialStateWithOneBlock,
    getBaseInitialStateWithoutBlocks,
    getBaseInitialStateWithTwoBlocks,
    getBaseInitialStateWithDifferentStationsBlock,
} from './mocks';

describe('MetroStationsBlock', () => {
    it('Отрисовка - один блок', async () => {
        await render(
            <AppProvider initialState={getBaseInitialStateWithOneBlock()}>
                <MetroStationsContainer />
            </AppProvider>,
            { viewport: { width: 480, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка - несколько блоков', async () => {
        await render(
            <AppProvider initialState={getBaseInitialStateWithTwoBlocks()}>
                <MetroStationsContainer />
            </AppProvider>,
            { viewport: { width: 480, height: 450 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Станции должны попадать в один блок, если их названия начинаются с одной буквы в разных кейсах', async () => {
        await render(
            <AppProvider initialState={getBaseInitialStateWithDifferentStationsBlock()}>
                <MetroStationsContainer />
            </AppProvider>,
            { viewport: { width: 480, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка - нет блоков', async () => {
        await render(
            <AppProvider initialState={getBaseInitialStateWithoutBlocks()}>
                <MetroStationsContainer />
            </AppProvider>,
            { viewport: { width: 480, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
