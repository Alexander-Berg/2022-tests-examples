import React from 'react';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { WithScrollContextProvider } from 'realty-core/view/react/common/enhancers/withScrollContext';

import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/user/reducer';

import { IUniversalStore } from 'view/modules/types';
import { LandingContextProvider } from 'view/enhancers/withLandingContext';

import { FormHeaderContainer } from '../container';

import * as stubs from './stubs/';

const Component: React.FC<{ store: DeepPartial<IUniversalStore>; title: string; subtitle?: string }> = ({
    store,
    title,
    subtitle,
}) => (
    <AppProvider rootReducer={rootReducer} initialState={store}>
        <WithScrollContextProvider>
            <LandingContextProvider>
                <FormHeaderContainer title={title} subtitle={subtitle} withSkeleton />
            </LandingContextProvider>
        </WithScrollContextProvider>
    </AppProvider>
);

const titles = {
    payment: 'Арендная плата 26 сен. – 25 окт.',
    flat: 'г Санкт‑Петербург, пр‑кт Энергетиков, д 30 к 1, кв 88',
    paymentsHistory: 'Финансы',
};

const subtitle = 'Список платежей';

const renderOptions = [{ viewport: { width: 1024, height: 800 } }, { viewport: { width: 375, height: 812 } }];

describe('FormHeader', () => {
    describe('Базовый ренедеринг', () => {
        it(`${renderOptions[0].viewport.width}px`, async () => {
            await render(<Component store={stubs.store} title={titles.payment} />, renderOptions[0]);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`${renderOptions[1].viewport.width}px`, async () => {
            await render(<Component store={stubs.mobileStore} title={titles.payment} />, renderOptions[1]);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('Есть подзаголовок', () => {
        it(`${renderOptions[0].viewport.width}px`, async () => {
            await render(
                <Component store={stubs.store} title={titles.payment} subtitle={subtitle} />,
                renderOptions[0]
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`${renderOptions[1].viewport.width}px`, async () => {
            await render(
                <Component store={stubs.mobileStore} title={titles.payment} subtitle={subtitle} />,
                renderOptions[1]
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('OnlyContent', () => {
        it(`${renderOptions[0].viewport.width}px`, async () => {
            await render(<Component store={stubs.onlyContentStore} title={titles.payment} />, renderOptions[0]);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`${renderOptions[1].viewport.width}px`, async () => {
            await render(<Component store={stubs.onlyContentMobileStore} title={titles.payment} />, renderOptions[1]);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('Одна крошка', () => {
        it(`${renderOptions[0].viewport.width}px`, async () => {
            await render(<Component store={stubs.oneCrumbStore} title={titles.flat} />, renderOptions[0]);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`${renderOptions[1].viewport.width}px`, async () => {
            await render(<Component store={stubs.oneCrumbMobileStore} title={titles.flat} />, renderOptions[1]);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('Много крошек', () => {
        it(`${renderOptions[0].viewport.width}px`, async () => {
            await render(<Component store={stubs.manyCrumbsStore} title={titles.flat} />, renderOptions[0]);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`${renderOptions[1].viewport.width}px`, async () => {
            await render(<Component store={stubs.manyCrumbsMobileStore} title={titles.flat} />, renderOptions[1]);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('Заголовок совпадает с последней крошкой', () => {
        it(`${renderOptions[0].viewport.width}px`, async () => {
            await render(<Component store={stubs.sameTitleStore} title={titles.paymentsHistory} />, renderOptions[0]);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`${renderOptions[1].viewport.width}px`, async () => {
            await render(
                <Component store={stubs.sameTitleMobileStore} title={titles.paymentsHistory} />,
                renderOptions[1]
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('Нет крошек', () => {
        it(`${renderOptions[0].viewport.width}px`, async () => {
            await render(<Component store={stubs.noBcStore} title={titles.payment} />, renderOptions[0]);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`${renderOptions[1].viewport.width}px`, async () => {
            await render(<Component store={stubs.noBcMobileStore} title={titles.payment} />, renderOptions[1]);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('Скелетон', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={stubs.skeletonStore} title={titles.payment} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
