import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import DeveloperCardTopSites from '../';

import { developerWithOneSite, developerWithTwoSite, developerWithThreeSite, developerWithFourSite } from './mocks';

const initialState = {
    user: {
        favoritesMap: {}
    },
    page: {
        name: ''
    }
};

const geo = {
    rgid: 123
};

describe('DeveloperCardTopSites', () => {
    it('рисует один снипет, если ЖК один', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <DeveloperCardTopSites developer={developerWithOneSite} geo={geo} />
            </AppProvider>,
            { viewport: { width: 320, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует два снипета, если два ЖК', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <DeveloperCardTopSites developer={developerWithTwoSite} geo={geo} />
            </AppProvider>,
            { viewport: { width: 400, height: 1250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует три снипета и кнопку, если три ЖК', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <DeveloperCardTopSites developer={developerWithThreeSite} geo={geo} />
            </AppProvider>,
            { viewport: { width: 600, height: 2300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует три снипета и кнопку, если ЖК больше трех', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <DeveloperCardTopSites developer={developerWithFourSite} geo={geo} />
            </AppProvider>,
            { viewport: { width: 450, height: 2000 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует только два снипета на широком экране', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <DeveloperCardTopSites developer={developerWithFourSite} geo={geo} />
            </AppProvider>,
            { viewport: { width: 700, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
