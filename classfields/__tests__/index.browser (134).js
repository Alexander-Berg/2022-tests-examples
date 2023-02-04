import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/react/libs/test-helpers';

import { OfferMapSnippets } from '../';

import {
    getOneOffer,
    getOffers,
    getSiteOffers,
    getSiteInfo,
    getVillageOffers,
    getVillageInfo,
    getInitialState
} from './mocks';

// eslint-disable-next-line no-undef
global.BUNDLE_LANG = 'ru';

const getProviderProps = () => ({
    initialState: getInitialState(),
    context: {
        observeIntersection: () => {}
    }
});

describe('OfferMapSnippets', () => {
    it('Рисует список с 1 сниппетом', async() => {
        await render(
            <AppProvider {...getProviderProps()}>
                <OfferMapSnippets
                    items={getOneOffer()}
                />
            </AppProvider>,
            { viewport: { width: 400, height: 520 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует список с несколькими сниппетами', async() => {
        await render(
            <AppProvider {...getProviderProps()}>
                <OfferMapSnippets
                    items={getOffers()}
                />
            </AppProvider>,
            { viewport: { width: 400, height: 900 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует список с несколькими сниппетами (доп. информация о новостройке)', async() => {
        await render(
            <AppProvider {...getProviderProps()}>
                <OfferMapSnippets
                    items={getSiteOffers()}
                    info={getSiteInfo()}
                />
            </AppProvider>,
            { viewport: { width: 400, height: 1040 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует список с несколькими сниппетами (доп. информация о КП)', async() => {
        await render(
            <AppProvider {...getProviderProps()}>
                <OfferMapSnippets
                    items={getVillageOffers()}
                    info={getVillageInfo()}
                />
            </AppProvider>,
            { viewport: { width: 400, height: 1040 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
