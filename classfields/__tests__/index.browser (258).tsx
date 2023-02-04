import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OfferCardInactiveInfo } from '../';

it('Блок "Объявление устарело"', async () => {
    await render(
        <AppProvider>
            <OfferCardInactiveInfo />
        </AppProvider>,
        { viewport: { width: 400, height: 100 } }
    );

    expect(await takeScreenshot()).toMatchImageSnapshot();
});
