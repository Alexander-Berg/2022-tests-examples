import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import CacheFooterLinksSections from '../index';

import { initialState } from './mocks';

describe('CacheFooterLinksSections', () => {
    it('should render links sections component', async () => {
        await render(
            <AppProvider initialState={initialState}>
                <CacheFooterLinksSections pageType="index" />
            </AppProvider>
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('should render links sections component and one expanded section', async () => {
        await render(
            <AppProvider initialState={initialState}>
                <CacheFooterLinksSections pageType="index" />
            </AppProvider>
        );

        await page.click('[class*="CacheFooterLinksSections__header"]');

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('should render links sections component and one full expanded section', async () => {
        await render(
            <AppProvider initialState={initialState}>
                <CacheFooterLinksSections pageType="index" />
            </AppProvider>
        );

        await page.click('[class*="CacheFooterLinksSections__header"]');
        await page.click('[class*="CacheFooterLinksSections__show-more"]');

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
