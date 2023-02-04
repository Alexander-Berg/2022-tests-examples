import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SamoletSerpSnippet } from '../';

import { snippet, store } from './mocks';

describe('SamoletSerpSnippet', () => {
    it('рендерится корректно', async () => {
        await render(
            <AppProvider initialState={store}>
                <SamoletSerpSnippet searchParams={{}} item={snippet} />
            </AppProvider>,
            { viewport: { width: 320, height: 500 } }
        );

        await page.addStyleTag({ content: 'body{background-color: #F4F6FB; padding: 0}' });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рендерится корректно для карты', async () => {
        await render(
            <AppProvider initialState={store}>
                <SamoletSerpSnippet isMap searchParams={{}} item={snippet} />
            </AppProvider>,
            { viewport: { width: 400, height: 800 } }
        );

        await page.addStyleTag({ content: 'body{background-color: #F4F6FB; padding: 0}' });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рендерится корректно для карты с маленьким экраном', async () => {
        await render(
            <AppProvider initialState={store}>
                <SamoletSerpSnippet isMap searchParams={{}} item={snippet} />
            </AppProvider>,
            { viewport: { width: 320, height: 400 } }
        );

        await page.addStyleTag({ content: 'body{background-color: #F4F6FB; padding: 0}' });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
