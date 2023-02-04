import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/react/libs/test-helpers';

import { FavoritesMap } from '../';

import {
    getStateWithDifferentPins,
    getStateWithDistantPins,
    getStateWithSidebarLoading,
    getStateWithOpenedOffers,
    getStateWithOpenedOffer,
    getStateWithSiteSnippet,
    getStateWithVillageSnippet
} from './mocks';

import styles from './styles.module.css';

// eslint-disable-next-line no-undef
global.BUNDLE_LANG = 'ru';

const Component = ({ initialState }) => (
    <AppProvider initialState={initialState}>
        <FavoritesMap
            className={styles.map}
        />
    </AppProvider>
);

// eslint-disable-next-line jest/no-disabled-tests
describe.skip('FavoritesMap', () => {
    it('Рисует карту без сайдбара с различными пинами', async() => {
        await render(
            <Component initialState={getStateWithDifferentPins()} />,
            { viewport: { width: 1000, height: 1040 } }
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует карту с масштабированием (далеко расположены пины)', async() => {
        await render(
            <Component initialState={getStateWithDistantPins()} />,
            { viewport: { width: 1000, height: 1040 } }
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует карту с сайдбаром в состоянии загрузки', async() => {
        await render(
            <Component initialState={getStateWithSidebarLoading()} />,
            { viewport: { width: 800, height: 1040 } }
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует карту с открытым пином оффера (несколько офферов)', async() => {
        await render(
            <Component initialState={getStateWithOpenedOffers()} />,
            { viewport: { width: 1000, height: 1040 } }
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует карту с открытым сниппетом оффера', async() => {
        await render(
            <Component initialState={getStateWithOpenedOffer()} />,
            { viewport: { width: 1000, height: 1040 } }
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.skip('Рисует карту с открытым сниппетом ЖК', async() => {
        await render(
            <Component initialState={getStateWithSiteSnippet()} />,
            { viewport: { width: 1000, height: 1040 } }
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.skip('Рисует карту с открытым сниппетом КП', async() => {
        await render(
            <Component initialState={getStateWithVillageSnippet()} />,
            { viewport: { width: 1000, height: 1040 } }
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
