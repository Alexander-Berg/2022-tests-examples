import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { Railway } from '../';

import {
    baseInitialState,
    initialStateWithoutLinks,
    initialStateWithoutNewbuildings,
    initialStateWithoutNewbuildingsAndWithLowLinks,
    initialStateWitOneNewbuilding,
} from './mocks';

describe('Railway', function () {
    it('рисует полностью заполненный компонент', async () => {
        await render(
            <AppProvider initialState={baseInitialState}>
                <Railway />
            </AppProvider>,
            { viewport: { width: 500, height: 4800 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует компонент без карусели новостроек', async () => {
        await render(
            <AppProvider initialState={initialStateWithoutNewbuildings}>
                <Railway />
            </AppProvider>,
            { viewport: { width: 500, height: 4500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует компонент с одной новостройкой', async () => {
        await render(
            <AppProvider initialState={initialStateWitOneNewbuilding}>
                <Railway />
            </AppProvider>,
            { viewport: { width: 500, height: 4600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует мало ссылок', async () => {
        await render(
            <AppProvider initialState={initialStateWithoutNewbuildingsAndWithLowLinks}>
                <Railway />
            </AppProvider>,
            { viewport: { width: 600, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует без ссылок', async () => {
        await render(
            <AppProvider initialState={initialStateWithoutLinks}>
                <Railway />
            </AppProvider>,
            { viewport: { width: 500, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
