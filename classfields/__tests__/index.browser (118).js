import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import DeveloperCardTopSites from '../';

import { developerWithOneSite, developerWithTwoSite, developerWithThreeSite, developerWithFourSite } from './mocks';

// eslint-disable-next-line no-undef
global.BUNDLE_LANG = 'ru';

const initialState = {
    user: {
        favoritesMap: {}
    }
};

const geo = {
    rgid: 123
};

describe('DeveloperCardTopSites', () => {
    it('рисует премиум снипет, если ЖК один', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <DeveloperCardTopSites developer={developerWithOneSite} geo={geo} />
            </AppProvider>,
            { viewport: { width: 1000, height: 550 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует два обычных снипета, если два ЖК', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <DeveloperCardTopSites developer={developerWithTwoSite} geo={geo} />
            </AppProvider>,
            { viewport: { width: 1000, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует три обычных снипета, если три ЖК', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <DeveloperCardTopSites developer={developerWithThreeSite} geo={geo} />
            </AppProvider>,
            { viewport: { width: 1000, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует три обычных снипета и кнопку, если ЖК больше трех', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <DeveloperCardTopSites developer={developerWithFourSite} geo={geo} />
            </AppProvider>,
            { viewport: { width: 1000, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует кнопку другого цвета при наведении курсора на кнопку, ведущую на выдачу', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <DeveloperCardTopSites developer={developerWithFourSite} geo={geo} />
            </AppProvider>,
            { viewport: { width: 1000, height: 700 } }
        );

        await page.hover('.DeveloperCardTopSites__allObjects');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });
});
