import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/react/libs/test-helpers';

import { SitesSerpSpecialPinnedBlock } from '..';

import { siteSnippetMock } from './mocks';

const samoletSpecialDeveloperData = {
    developerId: 102320,
    developerName: 'Самолёт',
    geoIds: [1, 10174],
};

describe('SitesSerpSpecialPinnedBlock', () => {
    it('рендерится корректно для 3х сниппетов', async () => {
        await render(
            <AppProvider
                initialState={{ user: { favoritesMap: [] } }}
                context={{ observeIntersection: () => undefined, unObserveIntersection: () => undefined }}
            >
                <SitesSerpSpecialPinnedBlock
                    items={Array(3).fill(siteSnippetMock)}
                    developer={samoletSpecialDeveloperData}
                    rgid={741965}
                />
            </AppProvider>,
            { viewport: { width: 1000, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится корректно для 2х сниппетов', async () => {
        await render(
            <AppProvider
                initialState={{ user: { favoritesMap: [] } }}
                context={{ observeIntersection: () => undefined, unObserveIntersection: () => undefined }}
            >
                <SitesSerpSpecialPinnedBlock
                    items={Array(2).fill(siteSnippetMock)}
                    developer={samoletSpecialDeveloperData}
                    rgid={741965}
                />
            </AppProvider>,
            { viewport: { width: 1000, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('отрабатывает клик по кнопке "добавить в избранное"', async () => {
        await render(
            <AppProvider
                initialState={{ user: { favoritesMap: [] } }}
                context={{ observeIntersection: () => undefined, unObserveIntersection: () => undefined }}
            >
                <SitesSerpSpecialPinnedBlock
                    items={Array(3).fill(siteSnippetMock)}
                    developer={samoletSpecialDeveloperData}
                    rgid={741965}
                />
            </AppProvider>,
            { viewport: { width: 1000, height: 700 } }
        );

        await page.click('.SerpFavoriteAction__icon');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('отрабатывает клик по кнопке обратного звонка', async () => {
        await render(
            <AppProvider
                initialState={{ user: { favoritesMap: [] } }}
                context={{ observeIntersection: () => undefined, unObserveIntersection: () => undefined }}
            >
                <SitesSerpSpecialPinnedBlock
                    items={Array(3).fill(siteSnippetMock)}
                    developer={samoletSpecialDeveloperData}
                    rgid={741965}
                />
            </AppProvider>,
            { viewport: { width: 1000, height: 700 } }
        );

        await page.click('.Button.BackCall');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
