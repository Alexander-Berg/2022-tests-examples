import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { MetroStationContainer } from '../container';

import {
    baseInitialState,
    initialStateWithoutLinks,
    initialStateWithoutNewbuildings,
    initialStateWithoutNewbuildingsAndWithLowLinks,
    initialStateWitOneNewbuilding,
} from './mocks';

describe('MetroStation', function () {
    it('рисует полностью заполненный компонент', async () => {
        await render(
            <AppProvider initialState={baseInitialState}>
                <MetroStationContainer />
            </AppProvider>,
            { viewport: { width: 500, height: 4900 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует компонент без карусели новостроек', async () => {
        await render(
            <AppProvider initialState={initialStateWithoutNewbuildings}>
                <MetroStationContainer />
            </AppProvider>,
            { viewport: { width: 500, height: 4900 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует компонент с одной новостройкой', async () => {
        await render(
            <AppProvider initialState={initialStateWitOneNewbuilding}>
                <MetroStationContainer />
            </AppProvider>,
            { viewport: { width: 500, height: 4900 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует мало ссылок', async () => {
        await render(
            <AppProvider initialState={initialStateWithoutNewbuildingsAndWithLowLinks}>
                <MetroStationContainer />
            </AppProvider>,
            { viewport: { width: 600, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует нет ссылок', async () => {
        await render(
            <AppProvider initialState={initialStateWithoutLinks}>
                <MetroStationContainer />
            </AppProvider>,
            { viewport: { width: 500, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
