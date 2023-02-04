import React from 'react';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { WithScrollContextProvider } from 'realty-core/view/react/common/enhancers/withScrollContext';

import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/public/reducer';

import { IUniversalStore } from 'view/modules/types';
import { LandingContextProvider } from 'view/enhancers/withLandingContext';

import { LandingOwnerReviewsBlockContainer } from '../container';
import mobileStyles from '../LandingOwnerReviewsBlockMobile/styles.module.css';
import reviewStyles from '../LandingOwnerReview/styles.module.css';

import * as stubs from './stubs/';

const Component: React.FC<{ store: DeepPartial<IUniversalStore> }> = ({ store }) => (
    <AppProvider rootReducer={rootReducer} initialState={store}>
        <WithScrollContextProvider>
            <LandingContextProvider>
                <LandingOwnerReviewsBlockContainer isAnimationDisabled={true} />
            </LandingContextProvider>
        </WithScrollContextProvider>
    </AppProvider>
);

const selectors = {
    modalOpenButton: `.${mobileStyles.button}`,
    showMoreText: `.${reviewStyles.review} .${reviewStyles.showMore}`,
    showMoreComment: `.${reviewStyles.commentWrapper} .${reviewStyles.showMore}`,
};

const viewportOptions = {
    desktop: {
        viewport: {
            width: 1200,
            height: 900,
        },
    },
    iphoneX: {
        viewport: {
            width: 375,
            height: 900,
        },
    },
    iphone5: {
        viewport: {
            width: 320,
            height: 900,
        },
    },
};

describe('LandingOwnerReviewblock', () => {
    describe('Базовый ренедеринг', () => {
        Object.values(viewportOptions).forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}`, async () => {
                await render(<Component store={stubs.baseStore} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Есь несколько отзывов', () => {
        Object.values(viewportOptions).forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}`, async () => {
                await render(<Component store={stubs.manyReviewStore} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Отзыв с большим содержанием текста', () => {
        Object.values(viewportOptions).forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}`, async () => {
                await render(<Component store={stubs.largeTextStore} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Раскрытие текста с большим содержанием', () => {
        Object.values(viewportOptions).forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}`, async () => {
                await render(<Component store={stubs.largeTextStore} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.showMoreText);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Раскрытие комментария с большим содержанием', () => {
        Object.values(viewportOptions).forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}`, async () => {
                await render(<Component store={stubs.largeTextStore} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.showMoreComment);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('открытие модалки в тачах', () => {
        [viewportOptions.iphoneX, viewportOptions.iphone5].forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}px`, async () => {
                await render(<Component store={stubs.largeTextStore} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.modalOpenButton);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('У отзыва нет звезд', () => {
        Object.values(viewportOptions).forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}`, async () => {
                const store = stubs.withRating(0);
                await render(<Component store={store} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('В отзыве одна звезда', () => {
        Object.values(viewportOptions).forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}`, async () => {
                const store = stubs.withRating(1);
                await render(<Component store={store} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });

    describe('Нет отзывов', () => {
        Object.values(viewportOptions).forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}`, async () => {
                await render(<Component store={stubs.storeWithEmptyReviews} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });
});
