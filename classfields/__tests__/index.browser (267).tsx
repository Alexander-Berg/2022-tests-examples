import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OffersMapSerpModalSite } from '../';

import { siteInfo, initialState } from './mocks';

describe('OffersMapSerpModalSite', () => {
    it('рендерится корректно', async () => {
        await render(
            <AppProvider initialState={initialState}>
                <OffersMapSerpModalSite siteInfo={siteInfo} page="page" />
            </AppProvider>,
            { viewport: { width: 400, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
