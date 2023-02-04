import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SiteCardNonPrimarySalesOffers } from '../index';

import { card } from './mocks';

describe('SiteCardNonPrimarySalesOffers', () => {
    it('рендерится корректно', async () => {
        await render(
            <AppProvider>
                <SiteCardNonPrimarySalesOffers card={card} />
            </AppProvider>,
            { viewport: { width: 1200, height: 500 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
