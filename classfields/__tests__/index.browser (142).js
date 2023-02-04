import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import * as plansActions from 'realty-core/view/react/modules/site-plans/redux/actions';

import { AppProvider } from 'view/react/libs/test-helpers';

import SitePlans from '..';

import { getInitialState, getSiteCard } from './mocks';

const mockAsyncAction = () => () => Promise.resolve();

describe('SitePlans', () => {
    plansActions.default.loadSitePlansStats = mockAsyncAction;

    it('рисует сгруппированный блок', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlans
                    card={getSiteCard()}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует плоский блок', async() => {
        plansActions.default.loadSitePlans = mockAsyncAction;

        await render(
            <AppProvider
                initialState={getInitialState({
                    roomsTotal: [ '1', '2' ]
                })}
            >
                <SitePlans
                    card={getSiteCard()}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 820 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует экран (no primary sale)', async() => {
        await render(
            <AppProvider
                initialState={getInitialState()}
            >
                <SitePlans
                    card={getSiteCard({
                        withOffers: false,
                        withBilling: false,
                        flatStatus: 'ON_SALE'
                    })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 120 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует экран все продано', async() => {
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
            { viewport: { width: 840, height: 300 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует экран нет в продаже', async() => {
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
            { viewport: { width: 840, height: 120 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует экран нет офферов', async() => {
        await render(
            <AppProvider
                initialState={getInitialState()}
            >
                <SitePlans
                    card={getSiteCard({
                        withOffers: false,
                        resaleTotalOffers: 0
                    })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 120 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует экран продажи приостановлены', async() => {
        await render(
            <AppProvider
                initialState={getInitialState()}
            >
                <SitePlans
                    card={getSiteCard({
                        withOffers: false,
                        flatStatus: 'ON_SALE',
                        location: {
                            subjectFederationId: 1
                        },
                        withBilling: false
                    })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 150 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует сгруппированный блок (апартаменты)', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlans
                    card={getSiteCard({
                        buildingFeatures: { isApartment: true, apartmentType: 'APARTMENTS' }
                    })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует плоский блок (апартаменты)', async() => {
        plansActions.default.loadSitePlans = mockAsyncAction;

        await render(
            <AppProvider
                initialState={getInitialState({
                    roomsTotal: [ '1', '2' ]
                })}
            >
                <SitePlans
                    card={getSiteCard({
                        buildingFeatures: { isApartment: true, apartmentType: 'APARTMENTS' }
                    })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 820 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует экран (no primary sale) (апартаменты)', async() => {
        await render(
            <AppProvider
                initialState={getInitialState()}
            >
                <SitePlans
                    card={getSiteCard({
                        withOffers: false,
                        withBilling: false,
                        flatStatus: 'ON_SALE',
                        buildingFeatures: { isApartment: true, apartmentType: 'APARTMENTS' }
                    })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 120 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует экран все продано (апартаменты)', async() => {
        await render(
            <AppProvider
                initialState={getInitialState()}
            >
                <SitePlans
                    card={getSiteCard({
                        withOffers: false,
                        flatStatus: 'SOLD',
                        buildingFeatures: { isApartment: true, apartmentType: 'APARTMENTS' }
                    })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 300 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует экран нет в продаже (апартаменты)', async() => {
        await render(
            <AppProvider
                initialState={getInitialState()}
            >
                <SitePlans
                    card={getSiteCard({
                        withOffers: false,
                        flatStatus: 'NOT_ON_SALE',
                        buildingFeatures: { isApartment: true, apartmentType: 'APARTMENTS' }
                    })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 120 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует экран нет офферов (апартаменты)', async() => {
        await render(
            <AppProvider
                initialState={getInitialState()}
            >
                <SitePlans
                    card={getSiteCard({
                        withOffers: false,
                        resaleTotalOffers: 0,
                        buildingFeatures: { isApartment: true, apartmentType: 'APARTMENTS' }
                    })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 120 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует экран продажи приостановлены (апартаменты)', async() => {
        await render(
            <AppProvider
                initialState={getInitialState()}
            >
                <SitePlans
                    card={getSiteCard({
                        withOffers: false,
                        flatStatus: 'ON_SALE',
                        location: {
                            subjectFederationId: 1
                        },
                        withBilling: false,
                        buildingFeatures: { isApartment: true, apartmentType: 'APARTMENTS' }
                    })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 150 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует сгруппированный блок (квартиры и апартаменты)', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlans
                    card={getSiteCard({
                        buildingFeatures: { isApartment: true, apartmentType: 'APARTMENTS_AND_FLATS' }
                    })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует плоский блок (квартиры и апартаменты)', async() => {
        plansActions.default.loadSitePlans = mockAsyncAction;

        await render(
            <AppProvider
                initialState={getInitialState({
                    roomsTotal: [ '1', '2' ]
                })}
            >
                <SitePlans
                    card={getSiteCard({
                        buildingFeatures: { isApartment: true, apartmentType: 'APARTMENTS_AND_FLATS' }
                    })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 820 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует экран (no primary sale) (квартиры и апартаменты)', async() => {
        await render(
            <AppProvider
                initialState={getInitialState()}
            >
                <SitePlans
                    card={getSiteCard({
                        withOffers: false,
                        withBilling: false,
                        flatStatus: 'ON_SALE',
                        buildingFeatures: { isApartment: true, apartmentType: 'APARTMENTS_AND_FLATS' }
                    })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 120 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует экран все продано (квартиры и апартаменты)', async() => {
        await render(
            <AppProvider
                initialState={getInitialState()}
            >
                <SitePlans
                    card={getSiteCard({
                        withOffers: false,
                        flatStatus: 'SOLD',
                        buildingFeatures: { isApartment: true, apartmentType: 'APARTMENTS_AND_FLATS' }
                    })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 300 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует экран нет в продаже (квартиры и апартаменты)', async() => {
        await render(
            <AppProvider
                initialState={getInitialState()}
            >
                <SitePlans
                    card={getSiteCard({
                        withOffers: false,
                        flatStatus: 'NOT_ON_SALE',
                        buildingFeatures: { isApartment: true, apartmentType: 'APARTMENTS_AND_FLATS' }
                    })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 120 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует экран нет офферов (квартиры и апартаменты)', async() => {
        await render(
            <AppProvider
                initialState={getInitialState()}
            >
                <SitePlans
                    card={getSiteCard({
                        withOffers: false,
                        resaleTotalOffers: 0,
                        buildingFeatures: { isApartment: true, apartmentType: 'APARTMENTS_AND_FLATS' }
                    })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 120 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует экран продажи приостановлены (квартиры и апартаменты)', async() => {
        await render(
            <AppProvider
                initialState={getInitialState()}
            >
                <SitePlans
                    card={getSiteCard({
                        withOffers: false,
                        flatStatus: 'ON_SALE',
                        location: {
                            subjectFederationId: 1
                        },
                        withBilling: false,
                        buildingFeatures: { isApartment: true, apartmentType: 'APARTMENTS_AND_FLATS' }
                    })}
                />
            </AppProvider>,
            { viewport: { width: 840, height: 200 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
