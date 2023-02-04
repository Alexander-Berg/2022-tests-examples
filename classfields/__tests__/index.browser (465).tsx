import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SamoletPlansSnippet } from '../';

import { plan1, plan2, planApart, planMix } from './mocks';

const commonProps = {
    onClick: () => undefined,
};

const store = {
    user: {
        favorites: [],
        favoritesMap: {},
    },
};

describe('SamoletPlansSnippet', () => {
    it('с картинкой', async () => {
        await render(
            <AppProvider initialState={store}>
                <SamoletPlansSnippet plan={plan1} {...commonProps} />
            </AppProvider>,
            { viewport: { width: 350, height: 420 } }
        );
        await page.addStyleTag({ content: 'body{background-color: #f5f6fb;}' });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('без картинки', async () => {
        await render(
            <AppProvider initialState={store}>
                <SamoletPlansSnippet plan={plan2} {...commonProps} />
            </AppProvider>,
            { viewport: { width: 350, height: 420 } }
        );
        await page.addStyleTag({ content: 'body{background-color: #f5f6fb;}' });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('апартамент', async () => {
        await render(
            <AppProvider initialState={store}>
                <SamoletPlansSnippet plan={planApart} {...commonProps} />
            </AppProvider>,
            { viewport: { width: 350, height: 420 } }
        );
        await page.addStyleTag({ content: 'body{background-color: #f5f6fb;}' });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('смешанный тип', async () => {
        await render(
            <AppProvider initialState={store}>
                <SamoletPlansSnippet plan={planMix} {...commonProps} />
            </AppProvider>,
            { viewport: { width: 350, height: 420 } }
        );
        await page.addStyleTag({ content: 'body{background-color: #f5f6fb;}' });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
