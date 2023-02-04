import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { ConciergeBanner } from '..';
import { Views } from '../ConciergeServiceBanner';

const renderComponent = ({
    height = 250,
    width = 940,
    view = 'xl',
}: {
    height?: number;
    width?: number;
    view?: Views;
}) =>
    render(
        <AppProvider>
            <ConciergeBanner bannerView={view} />
        </AppProvider>,
        { viewport: { width, height } }
    );

describe('ConciergeBanner', () => {
    it('рендерится дефолтное состояние xl', async () => {
        await renderComponent({});

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится дефолтное состояние широкий экран xl', async () => {
        await renderComponent({ width: 1201 });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится дефолтное состояние xs', async () => {
        await renderComponent({ view: 'xs', width: 420 });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится дефолтное состояние m', async () => {
        await renderComponent({ view: 'm' });

        await page.addStyleTag({ content: 'body{padding: 0}' });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится дефолтное состояние blank', async () => {
        await renderComponent({ view: 'blank' });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
