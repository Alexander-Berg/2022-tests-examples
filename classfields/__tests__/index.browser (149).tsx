import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import MapLayersPanel from '../';
import styles from '../styles.module.css';

import { heatmaps, heatmapsAtPoint, initialState } from './moks';

const renderComponent = (viewport = { width: 940, height: 60 }) =>
    render(
        <AppProvider initialState={initialState}>
            <MapLayersPanel heatmapsAtPoint={heatmapsAtPoint} layersFromShortcuts={heatmaps} />
        </AppProvider>,
        { viewport }
    );

describe('withCarousel(MapLayersPanel)', (): void => {
    it('рендерится без контролов карусели', async () => {
        await renderComponent();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится c контролами карусели', async () => {
        await renderComponent({ width: 500, height: 60 });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('скролится по клику на контрол', async () => {
        await renderComponent({ width: 500, height: 60 });

        await page.click(`.${styles.carouselControl}:nth-child(2) .${styles.controlButton}`);
        await page.waitFor(250);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('контролы появляются и исчезают при ресайзе', async () => {
        await renderComponent({ width: 500, height: 60 });

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.setViewport({ width: 900, height: 60 });
        await page.waitFor(120);
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.setViewport({ width: 500, height: 60 });
        await page.waitFor(120);
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
