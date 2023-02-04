import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import CacheFooterLinksSections from '..';

import { initialState } from './mocks';

describe('CacheFooterLinksSections', () => {
    it('should render links sections component', async () => {
        await render(
            <div style={{ backgroundColor: 'black', padding: '20px' }}>
                <AppProvider initialState={initialState}>
                    <CacheFooterLinksSections pageType="index" />
                </AppProvider>
            </div>,
            { viewport: { width: 1600, height: 600 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
