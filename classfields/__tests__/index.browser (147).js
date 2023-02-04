import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import SiteSnippetPin from '../';

import { finishedSite, unfinishedSite, suspendedSite } from './mocks';

const emptyInitialState = {
    user: {
        favoritesMap: {}
    }
};

describe('SiteSnippetPin', () => {
    it('рисует тултип сданного ЖК с картинкой без класса недвижимости', async() => {
        await render(
            <AppProvider initialState={emptyInitialState}>
                <SiteSnippetPin item={finishedSite} />
            </AppProvider>,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует тултип строящегося ЖК без картинки и без минимальной цены', async() => {
        await render(
            <AppProvider initialState={emptyInitialState}>
                <SiteSnippetPin item={unfinishedSite} />
            </AppProvider>,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует тултип замороженого ЖК с картинкой, с классом и минимальной ценой', async() => {
        await render(
            <AppProvider initialState={emptyInitialState}>
                <SiteSnippetPin item={suspendedSite} />
            </AppProvider>,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует тултип ЖК c не активной кнопокой избранного', async() => {
        await render(
            <AppProvider initialState={emptyInitialState}>
                <SiteSnippetPin item={finishedSite} />
            </AppProvider>,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует тултип ЖК c активной кнопокой избранного', async() => {
        const state = {
            user: {
                favoritesMap: {
                    site_1: true
                }
            }
        };

        await render(
            <AppProvider initialState={state}>
                <SiteSnippetPin item={finishedSite} />
            </AppProvider>,
            { viewport: { width: 300, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
