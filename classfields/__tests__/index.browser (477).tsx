import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OffersBuildingsFastlinks } from '../';

import { props3, props6, props7, props8, props18 } from './mocks';

it('3 ссылки', async () => {
    await render(
        <AppProvider>
            <OffersBuildingsFastlinks {...props3} />
        </AppProvider>,
        { viewport: { width: 900, height: 300 } }
    );

    customPage.waitForYmaps();

    expect(await takeScreenshot()).toMatchImageSnapshot();
});

it('6 ссылок', async () => {
    await render(
        <AppProvider>
            <OffersBuildingsFastlinks {...props6} />
        </AppProvider>,
        { viewport: { width: 900, height: 300 } }
    );

    customPage.waitForYmaps();

    expect(await takeScreenshot()).toMatchImageSnapshot();
});

it('7 ссылок', async () => {
    await render(
        <AppProvider>
            <OffersBuildingsFastlinks {...props7} />
        </AppProvider>,
        { viewport: { width: 900, height: 300 } }
    );

    customPage.waitForYmaps();

    expect(await takeScreenshot()).toMatchImageSnapshot();
});

it('8 ссылок', async () => {
    await render(
        <AppProvider>
            <OffersBuildingsFastlinks {...props8} />
        </AppProvider>,
        { viewport: { width: 900, height: 300 } }
    );

    customPage.waitForYmaps();

    expect(await takeScreenshot()).toMatchImageSnapshot();
});

it('18 ссылок', async () => {
    await render(
        <AppProvider>
            <OffersBuildingsFastlinks {...props18} />
        </AppProvider>,
        { viewport: { width: 900, height: 300 } }
    );

    customPage.waitForYmaps();

    expect(await takeScreenshot()).toMatchImageSnapshot();
});
