import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import galleryStyles from 'realty-core/view/react/deskpad/components/GalleryV2/styles.module.css';

import SiteCardProgress from '../index';

import { cardWithLotPhotos, cardWithFewPhotos } from './mocks';

describe('SiteCardProgress', () => {
    it('корректно рендерит компонент', async () => {
        await render(
            <AppProvider>
                <SiteCardProgress card={cardWithLotPhotos} />
            </AppProvider>,
            { viewport: { width: 1000, height: 600 } }
        );

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    });

    it('корректно рендерит компонент при малом количестве фото', async () => {
        await render(
            <AppProvider>
                <SiteCardProgress card={cardWithFewPhotos} />
            </AppProvider>,
            { viewport: { width: 1000, height: 600 } }
        );

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    });

    it('отрабатывает клики по контролам галереи', async () => {
        await render(
            <AppProvider>
                <SiteCardProgress card={cardWithLotPhotos} />
            </AppProvider>,
            { viewport: { width: 1000, height: 600 } }
        );

        await page.click(`.${galleryStyles.controlNextImage}`);
        await page.waitFor(700); // дожидаемся окончания анимации скролла

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();

        await page.click(`.${galleryStyles.controlPrevImage}`);
        await page.waitFor(700);

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    });
});
