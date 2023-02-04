import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { ISiteCard, ISiteVirtualTour } from 'realty-core/types/siteCard';
import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SiteCardVirtualTours } from '../';
import styles from '../SiteCardVirtualToursRow/styles.module.css';

const getToursMocks = (opts: { type?: 'TOUR_3D' | 'OVERVIEW_360'; count?: number; isFallbackCheck?: boolean }) => {
    const { type = 'TOUR_3D', count = 0, isFallbackCheck = false } = opts;
    const tours: Array<ISiteVirtualTour> = [];

    for (let i = 1; i <= count; i++) {
        tours.push(
            isFallbackCheck
                ? {
                      description: 'Тур без изображения',
                      link: 'https://someTour.url/model',
                      type,
                  }
                : {
                      description: `Тур №${i} с изображением`,
                      link: 'https://someTour.url/model',
                      preview: {
                          viewType: 'GENERAL',
                          full: generateImageUrl({ width: 800, height: 600 }),
                          large1242: generateImageUrl({ width: 800, height: 600 }),
                          cosmic: generateImageUrl({ width: 800, height: 600 }),
                          mini: generateImageUrl({ width: 800, height: 600 }),
                          appMiddle: generateImageUrl({ width: 800, height: 600 }),
                          appLarge: generateImageUrl({ width: 800, height: 600 }),
                          appMiniSnippet: generateImageUrl({ width: 800, height: 600 }),
                          appSmallSnippet: generateImageUrl({ width: 800, height: 600 }),
                          appMiddleSnippet: generateImageUrl({ width: 800, height: 600 }),
                          appLargeSnippet: generateImageUrl({ width: 800, height: 600 }),
                      },
                      type,
                  }
        );
    }

    return tours;
};

const getSiteCardMock = (opts: { toursCount?: number; overviewsCount?: number; isFallbackCheck?: boolean }) => {
    const { toursCount = 0, overviewsCount = 0, isFallbackCheck = false } = opts;

    return {
        images: {
            main: generateImageUrl({ width: 800, height: 600, size: 10 }),
        },
        virtualToursData: {
            tour3d: getToursMocks({ type: 'TOUR_3D', count: toursCount, isFallbackCheck }),
            overview360: getToursMocks({ type: 'OVERVIEW_360', count: overviewsCount, isFallbackCheck }),
        },
    };
};

describe('SiteCardVirtualTours', () => {
    it(`Корректно рендерит большое количество туров`, async () => {
        await render(
            <AppProvider>
                <SiteCardVirtualTours card={getSiteCardMock({ toursCount: 5, overviewsCount: 5 }) as ISiteCard} />
            </AppProvider>,
            { viewport: { width: 900, height: 1000 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it(`Не рендерит один из блоков при отсутствии данных`, async () => {
        await render(
            <AppProvider>
                <SiteCardVirtualTours card={getSiteCardMock({ toursCount: 0, overviewsCount: 5 }) as ISiteCard} />
            </AppProvider>,
            { viewport: { width: 900, height: 1000 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it(`Корректно отрабатывает изображения фоллбеки`, async () => {
        await render(
            <AppProvider>
                <SiteCardVirtualTours
                    card={getSiteCardMock({ toursCount: 3, overviewsCount: 5, isFallbackCheck: true }) as ISiteCard}
                />
            </AppProvider>,
            { viewport: { width: 900, height: 1000 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it(`Корректно рендерит сетки для 1 и 2 туров`, async () => {
        await render(
            <AppProvider>
                <SiteCardVirtualTours card={getSiteCardMock({ toursCount: 1, overviewsCount: 2 }) as ISiteCard} />
            </AppProvider>,
            { viewport: { width: 900, height: 1000 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it(`Корректно работают контролы слайдера`, async () => {
        await render(
            <AppProvider>
                <SiteCardVirtualTours card={getSiteCardMock({ toursCount: 0, overviewsCount: 5 }) as ISiteCard} />
            </AppProvider>,
            { viewport: { width: 900, height: 1000 } }
        );

        await page.click(`.${styles.nextBtn}`);
        await page.click(`.${styles.nextBtn}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${styles.backBtn}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
