import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { District } from '../index';

import {
    baseInitialState,
    initialStateWithoutLinks,
    initialStateWithoutNewbuildings,
    initialStateWithoutNewbuildingsAndWithLowLinks,
    initialStateWitOneNewbuilding,
} from './mocks';

describe('District', function () {
    it('рисует полностью заполненный компонент на узком экране', async () => {
        await render(
            <AppProvider initialState={baseInitialState}>
                <District />
            </AppProvider>,
            { viewport: { width: 1000, height: 800 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует полностью заполненный компонент на широком экране', async () => {
        await render(
            <AppProvider initialState={baseInitialState}>
                <District />
            </AppProvider>,
            { viewport: { width: 1280, height: 800 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует компонент без карусели новостроек', async () => {
        await render(
            <AppProvider initialState={initialStateWithoutNewbuildings}>
                <District />
            </AppProvider>,
            { viewport: { width: 1000, height: 800 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует компонент с одной новостройкой', async () => {
        await render(
            <AppProvider initialState={initialStateWitOneNewbuilding}>
                <District />
            </AppProvider>,
            { viewport: { width: 1000, height: 800 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует мало ссылок', async () => {
        await render(
            <AppProvider initialState={initialStateWithoutNewbuildingsAndWithLowLinks}>
                <District />
            </AppProvider>,
            { viewport: { width: 1000, height: 800 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует без ссылок', async () => {
        await render(
            <AppProvider initialState={initialStateWithoutLinks}>
                <District />
            </AppProvider>,
            { viewport: { width: 1000, height: 800 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
