import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SitePlans } from '..';

import { getInitialState, getSiteCard } from './mocks';

describe('SitePlans', () => {
    it('рисует сгруппированный блок', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlans
                    card={getSiteCard({
                        resaleTotalOffers: 1
                    })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует плоский блок', async() => {
        await render(
            <AppProvider
                initialState={getInitialState({
                    roomsTotal: [ '1', '2' ]
                })}
            >
                <SitePlans
                    card={getSiteCard({
                        resaleTotalOffers: 1
                    })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует экран "Предложений в продаже нет" (Наличие уточняйте у застройщика)', async() => {
        await render(
            <AppProvider
                initialState={getInitialState()}
            >
                <SitePlans
                    card={getSiteCard({
                        withOffers: false,
                        flatStatus: 'SOLD'
                    })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 240 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует экран "Предложений по ЖК нет" (Посмотрите с открытыми продажами)', async() => {
        await render(
            <AppProvider
                initialState={getInitialState()}
            >
                <SitePlans
                    card={getSiteCard({
                        withOffers: false,
                        flatStatus: 'NOT_ON_SALE'
                    })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 150 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует экран "Продажи от застройщика закрыты" (Посмотрите у частных лиц)', async() => {
        await render(
            <AppProvider
                initialState={getInitialState()}
            >
                <SitePlans
                    card={getSiteCard({
                        resaleTotalOffers: 1,
                        withOffers: false,
                        withBilling: false,
                        flatStatus: 'SOLD'
                    })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 240 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует экран "Предложений в продаже нет" (Никаких)', async() => {
        await render(
            <AppProvider
                initialState={getInitialState()}
            >
                <SitePlans
                    card={getSiteCard({
                        withOffers: false,
                        withBilling: false,
                        flatStatus: 'SOLD'
                    })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 340 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует экран "Предложений в продаже нет" (Дату старта продаж уточняйте у застройщика)', async() => {
        await render(
            <AppProvider
                initialState={getInitialState()}
            >
                <SitePlans
                    card={getSiteCard({
                        withOffers: false,
                        withBilling: false,
                        flatStatus: 'SOON_AVAILABLE'
                    })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 180 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует экран "Наличие предложений и актуальные цены уточняйте у застройщика"', async() => {
        await render(
            <AppProvider
                initialState={getInitialState()}
            >
                <SitePlans
                    card={getSiteCard({
                        withOffers: false,
                        flatStatus: 'ON_SALE'
                    })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 180 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует экран "Предложений от застройщика нет" (Посмотрите у частных лиц)', async() => {
        await render(
            <AppProvider
                initialState={getInitialState()}
            >
                <SitePlans
                    card={getSiteCard({
                        withOffers: false,
                        resaleTotalOffers: 1
                    })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 360 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует экран "Нет предложений в продаже" (default)', async() => {
        await render(
            <AppProvider
                initialState={getInitialState()}
            >
                <SitePlans
                    card={getSiteCard({
                        withOffers: false
                    })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 360 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
