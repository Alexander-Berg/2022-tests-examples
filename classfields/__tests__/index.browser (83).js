import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { FavoritesMapComponent } from '../';

import {
    differentPointsProps,
    farPointsProps,
    siteSnippetProps,
    villageSnippetProps,
    offerSnippetProps,
    offerSeveralSnippetsProps
} from './mocks';

// eslint-disable-next-line no-undef
global.BUNDLE_LANG = 'ru';

const FavoritesMap = props => (
    <AppProvider
        initialState={{
            user: {
                favoritesMap: props.favoritesMap || {},
                favorites: props.favorites || [],
                ...props.user
            },
            page: { name: 'favorites-map' }
        }}
        context={{
            loadPage: () => {},
            navigate: () => {}
        }}
    >
        <FavoritesMapComponent {...props} />
    </AppProvider>
);

describe.skip('FavoritesMap', () => {
    it('рисует карту c пинами разных типов', async() => {
        await render(<FavoritesMap {...differentPointsProps} />,
            { viewport: { width: 400, height: 800 } }
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует карту c пинами разных типов в горизонтальной ориентации', async() => {
        await render(<FavoritesMap {...differentPointsProps} />,
            { viewport: { width: 800, height: 400 } }
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует карту c пинами находящимися на разных концах', async() => {
        await render(<FavoritesMap {...farPointsProps} />,
            { viewport: { width: 400, height: 800 } }
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует снипет ЖК в вертикальной ориентации', async() => {
        await render(<FavoritesMap {...siteSnippetProps} />,
            { viewport: { width: 360, height: 640 } }
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует снипет ЖК в горизонатальной ориентации', async() => {
        await render(<FavoritesMap {...siteSnippetProps} />,
            { viewport: { width: 640, height: 360 } }
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует снипет КП в вертикальной ориентации', async() => {
        await render(<FavoritesMap {...villageSnippetProps} />,
            { viewport: { width: 1350, height: 1024 } }
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует снипет КП в горизонатальной ориентации', async() => {
        await render(<FavoritesMap {...villageSnippetProps} />,
            { viewport: { width: 1024, height: 1350 } }
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует снипет оффера в вертикальной ориентации', async() => {
        await render(<FavoritesMap {...offerSnippetProps} />,
            { viewport: { width: 400, height: 800 } }
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует снипет оффера в горизонатальной ориентации', async() => {
        await render(<FavoritesMap {...offerSnippetProps} />,
            { viewport: { width: 800, height: 400 } }
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует несколько снипетов офферов в вертикальной ориентации', async() => {
        await render(<FavoritesMap {...offerSeveralSnippetsProps} />,
            { viewport: { width: 400, height: 800 } }
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует несколько снипетов офферов в горизонатальной ориентации', async() => {
        await render(<FavoritesMap {...offerSeveralSnippetsProps} />,
            { viewport: { width: 800, height: 400 } }
        );

        await customPage.waitForYmapsPins();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
