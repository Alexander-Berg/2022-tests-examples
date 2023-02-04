import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { ApartmentType } from 'realty-core/types/siteCard';

import { SitePlansOffersContainer } from '../container';

import {
    getProps,
    getInitialState,
    reducer,
    defaultGate,
    gateWithLoadMore,
    gateWithoutOffers,
    gateLoadingOffers,
    gateErrorOffers,
} from './mocks';

describe('SitePlansOffers', () => {
    [ApartmentType.FLATS, ApartmentType.APARTMENTS, ApartmentType.APARTMENTS_AND_FLATS].forEach((apartmentType) => {
        it(`[${apartmentType}] основной блок`, async () => {
            await render(
                <AppProvider initialState={getInitialState({ mainFiltersCount: 2 })}>
                    <SitePlansOffersContainer {...getProps(apartmentType)} />
                </AppProvider>,
                { viewport: { width: 320, height: 1000 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`[${apartmentType}] основной блок (нет офферов)`, async () => {
            await render(
                <AppProvider initialState={getInitialState({ mainFiltersCount: 0 })}>
                    <SitePlansOffersContainer {...getProps(apartmentType)} />
                </AppProvider>,
                { viewport: { width: 320, height: 1000 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`[${apartmentType}] модалка с выдачей`, async () => {
            await render(
                <AppProvider
                    initialState={getInitialState({ mainFiltersCount: 2 })}
                    Gate={defaultGate}
                    rootReducer={reducer}
                >
                    <SitePlansOffersContainer {...getProps(apartmentType)} />
                </AppProvider>,
                { viewport: { width: 320, height: 1000 } }
            );

            await page.click('[class*=mainFiltersButton]');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`[${apartmentType}] открыт фильтр в модалке`, async () => {
            await render(
                <AppProvider
                    initialState={getInitialState({ mainFiltersCount: 2 })}
                    rootReducer={reducer}
                    Gate={defaultGate}
                >
                    <SitePlansOffersContainer {...getProps(apartmentType)} />
                </AppProvider>,
                { viewport: { width: 320, height: 1000 } }
            );

            await page.click('[class*=mainFiltersButton]');
            await page.click('[class*=IconSvg_filters]');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`[${apartmentType}] модалка с пустой выдачей`, async () => {
            await render(
                <AppProvider
                    initialState={getInitialState({ mainFiltersCount: 2 })}
                    rootReducer={reducer}
                    Gate={gateWithoutOffers}
                >
                    <SitePlansOffersContainer {...getProps(apartmentType)} />
                </AppProvider>,
                { viewport: { width: 320, height: 1000 } }
            );

            await page.click('[class*=mainFiltersButton]');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`[${apartmentType}] модалка c кнопкой подгрузки выдачи`, async () => {
            const props = getProps(apartmentType);

            await render(
                <AppProvider
                    initialState={getInitialState({ mainFiltersCount: 2 })}
                    rootReducer={reducer}
                    Gate={gateWithLoadMore}
                >
                    <SitePlansOffersContainer {...props} />
                </AppProvider>,
                { viewport: { width: 320, height: 1000 } }
            );

            await page.click('[class*=mainFiltersButton]');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    it('основной блок (состояние загрузки)', async () => {
        await render(
            <AppProvider initialState={getInitialState({ areMainFiltersStatsLoading: true, mainFiltersCount: 2 })}>
                <SitePlansOffersContainer {...getProps(ApartmentType.FLATS)} />
            </AppProvider>,
            { viewport: { width: 320, height: 1000 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('модалка в состоянии загрузки', async () => {
        await render(
            <AppProvider
                initialState={getInitialState({ mainFiltersCount: 2 })}
                rootReducer={reducer}
                Gate={gateLoadingOffers}
            >
                <SitePlansOffersContainer {...getProps(ApartmentType.FLATS)} />
            </AppProvider>,
            { viewport: { width: 320, height: 1000 } }
        );

        await page.click('[class*=mainFiltersButton]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('модалка с ошибкой', async () => {
        await render(
            <AppProvider
                initialState={getInitialState({ mainFiltersCount: 2 })}
                rootReducer={reducer}
                Gate={gateErrorOffers}
            >
                <SitePlansOffersContainer {...getProps(ApartmentType.FLATS)} />
            </AppProvider>,
            { viewport: { width: 320, height: 1000 } }
        );

        await page.click('[class*=mainFiltersButton]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
