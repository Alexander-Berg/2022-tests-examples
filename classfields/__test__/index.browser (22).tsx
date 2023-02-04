import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { YandexRentOfferPromo } from '../';

const mobileViewports = [
    { width: 335, height: 200 },
    { width: 375, height: 200 },
];

const desktopViewports = [
    { width: 1000, height: 200 },
    { width: 1200, height: 200 },
];

// eslint-disable-next-line @typescript-eslint/no-empty-function
const render = async (component: React.ReactElement, viewports: Array<{ width: number; height: number }>) => {
    for (const viewport of viewports) {
        await _render(component, { viewport });

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    }
};

describe('YandexRentOfferPromo', () => {
    it('Базовая отрисовка тач', async () => {
        await render(<YandexRentOfferPromo isMobile />, mobileViewports);
    });

    it('Базовая отрисовка десктоп', async () => {
        await render(<YandexRentOfferPromo />, desktopViewports);
    });
});
