import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { ISiteCardBaseType } from 'realty-core/types/siteCard';

import { SiteCardNonPrimarySalesBanner } from '../';

import { storeWithConcierge, storeWithoutConcierge } from './mocks';

const card = {
    location: {},
    flatStatus: 'NOT_ON_SALE',
} as ISiteCardBaseType;

describe('SiteCardNonPrimarySalesBanner', () => {
    it('рендерится корректно', async () => {
        await render(
            <AppProvider initialState={storeWithConcierge}>
                <SiteCardNonPrimarySalesBanner card={card} />
            </AppProvider>,
            { viewport: { width: 320, height: 600 } }
        );
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится корректно в гео без консьержа', async () => {
        await render(
            <AppProvider initialState={storeWithoutConcierge}>
                <SiteCardNonPrimarySalesBanner card={card} />
            </AppProvider>,
            { viewport: { width: 320, height: 600 } }
        );
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
