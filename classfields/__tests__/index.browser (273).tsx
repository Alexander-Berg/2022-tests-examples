import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OffersBuildingsFastlinks } from '../';

import { props2, props3, props12 } from './mocks';

it('2 ссылки', async () => {
    await render(
        <AppProvider>
            <OffersBuildingsFastlinks {...props2} />
        </AppProvider>,
        { viewport: { width: 390, height: 844 } }
    );

    customPage.waitForYmaps();

    expect(await takeScreenshot()).toMatchImageSnapshot();
});

it('3 ссылки', async () => {
    await render(
        <AppProvider>
            <OffersBuildingsFastlinks {...props3} />
        </AppProvider>,
        { viewport: { width: 390, height: 844 } }
    );

    customPage.waitForYmaps();

    expect(await takeScreenshot()).toMatchImageSnapshot();
});

it('12 ссылок', async () => {
    await render(
        <AppProvider>
            <OffersBuildingsFastlinks {...props12} />
        </AppProvider>,
        { viewport: { width: 390, height: 844 } }
    );

    customPage.waitForYmaps();

    expect(await takeScreenshot()).toMatchImageSnapshot();
});
