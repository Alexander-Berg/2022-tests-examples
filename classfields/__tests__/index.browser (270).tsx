import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OffersSearchCategories } from '../';

import {
    initialState,
    initialStateWithoutLinks,
    initialStateWithoutNewbuildings,
    initialStateWithoutNewbuildingsAndWithFewLinks,
    initialStateWitOneNewbuilding,
} from './mocks';

describe('OffersSearchCategories', function () {
    it('рисует полностью заполненный компонент', async () => {
        await render(
            <AppProvider initialState={initialState}>
                <OffersSearchCategories />
            </AppProvider>,
            { viewport: { width: 500, height: 4800 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует компонент без карусели новостроек', async () => {
        await render(
            <AppProvider initialState={initialStateWithoutNewbuildings}>
                <OffersSearchCategories />
            </AppProvider>,
            { viewport: { width: 500, height: 4500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует компонент с одной новостройкой', async () => {
        await render(
            <AppProvider initialState={initialStateWitOneNewbuilding}>
                <OffersSearchCategories />
            </AppProvider>,
            { viewport: { width: 500, height: 4600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует мало ссылок', async () => {
        await render(
            <AppProvider initialState={initialStateWithoutNewbuildingsAndWithFewLinks}>
                <OffersSearchCategories />
            </AppProvider>,
            { viewport: { width: 600, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует без ссылок', async () => {
        await render(
            <AppProvider initialState={initialStateWithoutLinks}>
                <OffersSearchCategories />
            </AppProvider>,
            { viewport: { width: 500, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
