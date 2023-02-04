import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/react/libs/test-helpers';

import { OfferCardSummaryInfo } from '../index';
import styles from '../styles.module.css';

import {
    newbuildingOffer,
    rentOffer,
    secondaryOffer,
    commercialOffer,
    houseOffer,
    garageOffer,
    rentCommercialOffer,
    newbuildingOfferWithPriceChange,
    rentOfferWithPriceChange,
    secondaryOfferWithPriceChange,
    commercialOfferWithPriceChange,
    houseOfferWithPriceChange,
    garageOfferWithPriceChange,
    rentCommercialOfferWithPriceChange,
    houseOfferWithAreCost,
} from './mocks';

describe('OfferCardSummaryInfo', function () {
    describe('Новостройка', function () {
        it('Базовая отрисовка', async () => {
            await render(
                <AppProvider>
                    <OfferCardSummaryInfo offer={newbuildingOffer} />
                </AppProvider>,
                {
                    viewport: { width: 400, height: 150 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Отрисовка с изменением цены', async () => {
            await render(
                <AppProvider>
                    <OfferCardSummaryInfo offer={newbuildingOfferWithPriceChange} />
                </AppProvider>,
                {
                    viewport: { width: 400, height: 150 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('Вторичная недвижимость', function () {
        it('Базовая отрисовка', async () => {
            await render(
                <AppProvider>
                    <OfferCardSummaryInfo offer={secondaryOffer} />
                </AppProvider>,
                {
                    viewport: { width: 400, height: 150 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Отрисовка с изменением цены', async () => {
            await render(
                <AppProvider>
                    <OfferCardSummaryInfo offer={secondaryOfferWithPriceChange} />
                </AppProvider>,
                {
                    viewport: { width: 400, height: 150 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Базовая отрисовка аренды', async () => {
            await render(
                <AppProvider>
                    <OfferCardSummaryInfo offer={rentOffer} />
                </AppProvider>,
                {
                    viewport: { width: 400, height: 150 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Отрисовка аренды с изменением цены', async () => {
            await render(
                <AppProvider>
                    <OfferCardSummaryInfo offer={rentOfferWithPriceChange} />
                </AppProvider>,
                {
                    viewport: { width: 400, height: 150 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('Коммерческая недвижимость', function () {
        it('Базовая отрисовка', async () => {
            await render(
                <AppProvider>
                    <OfferCardSummaryInfo offer={commercialOffer} />
                </AppProvider>,
                {
                    viewport: { width: 400, height: 150 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Отрисовка с изменением цены', async () => {
            await render(
                <AppProvider>
                    <OfferCardSummaryInfo offer={commercialOfferWithPriceChange} />
                </AppProvider>,
                {
                    viewport: { width: 400, height: 150 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Базовая отрисовка аренда', async () => {
            await render(
                <AppProvider>
                    <OfferCardSummaryInfo offer={rentCommercialOffer} />
                </AppProvider>,
                {
                    viewport: { width: 400, height: 150 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Отрисовка аренды с изменением цены', async () => {
            await render(
                <AppProvider>
                    <OfferCardSummaryInfo offer={rentCommercialOfferWithPriceChange} />
                </AppProvider>,
                {
                    viewport: { width: 400, height: 150 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('Дом', function () {
        it('Базовая отрисовка', async () => {
            await render(
                <AppProvider>
                    <OfferCardSummaryInfo offer={houseOffer} />
                </AppProvider>,
                {
                    viewport: { width: 400, height: 150 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Отрисовка с изменением цены', async () => {
            await render(
                <AppProvider>
                    <OfferCardSummaryInfo offer={houseOfferWithPriceChange} />
                </AppProvider>,
                {
                    viewport: { width: 400, height: 150 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Отрисовка с ценой за сотку', async () => {
            await render(
                <AppProvider>
                    <OfferCardSummaryInfo offer={houseOfferWithAreCost} />
                </AppProvider>,
                {
                    viewport: { width: 400, height: 150 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('Гараж', function () {
        it('Базовая отрисовка', async () => {
            await render(
                <AppProvider>
                    <OfferCardSummaryInfo offer={garageOffer} />
                </AppProvider>,
                {
                    viewport: { width: 400, height: 150 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Базовая отрисовка с измением цены', async () => {
            await render(
                <AppProvider>
                    <OfferCardSummaryInfo offer={garageOfferWithPriceChange} />
                </AppProvider>,
                {
                    viewport: { width: 400, height: 150 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('Владелец', () => {
        it('Базовая отрисовка с редактированием', async () => {
            await render(
                <AppProvider>
                    <OfferCardSummaryInfo offer={{ ...secondaryOffer, isEditable: true }} isOwner />
                </AppProvider>,
                {
                    viewport: { width: 400, height: 150 },
                }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('При клике на "Изменить цену" открывается поле ввода цены', async () => {
            await render(
                <AppProvider>
                    <OfferCardSummaryInfo offer={{ ...secondaryOffer, isEditable: true }} isOwner />
                </AppProvider>,
                {
                    viewport: { width: 400, height: 150 },
                }
            );

            await page.click(`.${styles.smallButton}`);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
