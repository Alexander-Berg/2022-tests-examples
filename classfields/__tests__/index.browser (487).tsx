/* eslint @typescript-eslint/ban-ts-comment: 0 */

import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SiteSnippetSimilar } from '../';

import { siteWithOneMetro, siteWithSeveralMetro, siteWithSeveralDevelopers } from './mocks';

// @ts-ignore
global.BUNDLE_LANG = 'ru';

const context = {
    observeIntersection: (): void => undefined,
    unObserveIntersection: (): void => undefined,
};

function hoverGallary() {
    // @ts-ignore
    return page.hover('.SnippetGallery');
}

describe('SiteSnippetSimilar', () => {
    it('рисует с одним метро', async () => {
        await render(
            <AppProvider initialState={{}} context={context}>
                <SiteSnippetSimilar item={siteWithOneMetro} url="#" />
            </AppProvider>,
            { viewport: { width: 300, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует с несколькими метро', async () => {
        await render(
            <AppProvider initialState={{}} context={context}>
                <SiteSnippetSimilar item={siteWithSeveralMetro} url="#" />
            </AppProvider>,
            { viewport: { width: 300, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует с несколькими застройщиками', async () => {
        await render(
            <AppProvider initialState={{}} context={context}>
                <SiteSnippetSimilar item={siteWithSeveralDevelopers} url="#" />
            </AppProvider>,
            { viewport: { width: 300, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует при наведение на галерею с одной картинкой', async () => {
        await render(
            <AppProvider initialState={{}} context={context}>
                <SiteSnippetSimilar item={siteWithOneMetro} url="#" />
            </AppProvider>,
            { viewport: { width: 300, height: 400 } }
        );

        await hoverGallary();

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует при наведение на галерею с тремя картинками', async () => {
        await render(
            <AppProvider initialState={{}} context={context}>
                <SiteSnippetSimilar item={siteWithSeveralMetro} url="#" />
            </AppProvider>,
            { viewport: { width: 300, height: 400 } }
        );

        await hoverGallary();

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует при наведение на галерею восемью картинками', async () => {
        await render(
            <AppProvider initialState={{}} context={context}>
                <SiteSnippetSimilar item={siteWithSeveralDevelopers} url="#" />
            </AppProvider>,
            { viewport: { width: 300, height: 400 } }
        );

        await hoverGallary();

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });
});
