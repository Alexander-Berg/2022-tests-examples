import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { NewbuildingNonPrimarySalesOffers } from '../index';

import { card } from './mocks';

describe('NewbuildingNonPrimarySalesOffers', () => {
    it('рендерится корректно', async () => {
        await render(
            <AppProvider>
                <NewbuildingNonPrimarySalesOffers card={card} />
            </AppProvider>,
            { viewport: { width: 320, height: 500 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
