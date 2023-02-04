import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';
import { Currency } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import type {
    StateCardActionOfferPriceUpdate,
    StateCardActionActualizeOffer,
    StateCardActionOfferAutoRenewActivate,
    StateCardActionOfferAutoRenewDisabled,
} from 'auto-core/react/dataDomain/card/StateCard';
import { PAGE_LOADING_SUCCESS } from 'auto-core/react/actionTypes';
import {
    OFFER_PRICE_UPDATE,
    OFFER_ACTUALIZE_SUCCESS,
    OFFER_AUTORENEW_DISABLED,
    OFFER_AUTORENEW_ACTIVATE,
} from 'auto-core/react/dataDomain/card/actionTypes';
import configStateMock from 'auto-core/react/dataDomain/config/mock';
import getActualizeDate from 'auto-core/react/lib/offer/getActualizeDate';
import getIdHash from 'auto-core/react/lib/offer/getIdHash';
import getPrice from 'auto-core/react/lib/offer/getPrice';
import getServiceInfo from 'auto-core/react/lib/offer/getServiceInfo';
import getServiceScheduleTime from 'auto-core/react/lib/offer/getServiceScheduleTime';

import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';

import type {
    TStateSalesActionToggleServiceAutoProlongation,
    TStateSalesActionSetUnfoldedState,
    TStateSalesActionSetLoadingState,
    TStateSalesActionUpdateAllOffers,
    TStateSalesActionExpandAllOffersStats,
    TStateSalesActionCloseAllOffersStats,
} from './types';
import actionTypes from './actionTypes';
import mock from './mocks';
import reducer from './reducer';
import type { TActionPageLoadingSuccess } from './reducer';

it('PAGE_LOADING_SUCCESS: должен раскрыть первый активный оффер', () => {
    const state = mock.value();

    const action: TActionPageLoadingSuccess = {
        type: PAGE_LOADING_SUCCESS,
        payload: {
            sales: {
                offers: [
                    cloneOfferWithHelpers(offerMock).withSaleId('111-aaa').withStatus(OfferStatus.BANNED).value(),
                    cloneOfferWithHelpers(offerMock).withSaleId('222-bbb').value(),
                    cloneOfferWithHelpers(offerMock).withSaleId('333-ccc').value(),
                ],
                pagination: {
                    page: 1,
                    page_size: 10,
                    total_offers_count: 3,
                    total_page_count: 1,
                },
                filters: {},
            },
            cookies: {},
            config: configStateMock.withPageType('sales').value().data,
        },
    };

    const nextState = reducer(state, action);

    expect(nextState.state['111-aaa']).toBeUndefined();
    expect(nextState.state['222-bbb'].unfolded).toBe(true);
    expect(nextState.state['333-ccc']).toBeUndefined();
});

it('PAGE_LOADING_SUCCESS: раскроет все активные офферы, если страница reseller-sales', () => {
    const state = mock.value();

    const action: TActionPageLoadingSuccess = {
        type: PAGE_LOADING_SUCCESS,
        payload: {
            sales: {
                offers: [
                    cloneOfferWithHelpers(offerMock).withSaleId('111-aaa').withStatus(OfferStatus.BANNED).value(),
                    cloneOfferWithHelpers(offerMock).withSaleId('222-bbb').value(),
                    cloneOfferWithHelpers(offerMock).withSaleId('333-ccc').value(),
                ],
                pagination: {
                    page: 1,
                    page_size: 10,
                    total_offers_count: 3,
                    total_page_count: 1,
                },
                filters: {},
            },
            cookies: {},
            config: configStateMock.withPageType('reseller-sales').value().data,
        },
    };

    const nextState = reducer(state, action);

    expect(nextState.state['111-aaa']).toBeUndefined();
    expect(nextState.state['222-bbb'].unfolded).toBe(true);
    expect(nextState.state['333-ccc'].unfolded).toBe(true);
});

it('PAGE_LOADING_SUCCESS: раскроет первый оффер в экспе AUTORUFRONT-19219_new_lk_and_vas_block_design, если страница sales и офферов меньше 5', () => {
    const state = mock.value();

    const action: TActionPageLoadingSuccess = {
        type: PAGE_LOADING_SUCCESS,
        payload: {
            sales: {
                offers: [
                    cloneOfferWithHelpers(offerMock).withSaleId('111-aaa').withStatus(OfferStatus.BANNED).value(),
                    cloneOfferWithHelpers(offerMock).withSaleId('222-bbb').value(),
                    cloneOfferWithHelpers(offerMock).withSaleId('333-ccc').value(),
                ],
                pagination: {
                    page: 1,
                    page_size: 10,
                    total_offers_count: 3,
                    total_page_count: 1,
                },
                filters: {},
            },
            cookies: {},
            config: configStateMock.withPageType('sales').withExperiments({ 'AUTORUFRONT-19219_new_lk_and_vas_block_design': true }).value().data,
        },
    };

    const nextState = reducer(state, action);

    expect(nextState.state['111-aaa']).toBeUndefined();
    expect(nextState.state['222-bbb'].unfolded).toBe(true);
    expect(nextState.state['333-ccc']).toBeUndefined();
});

it('PAGE_LOADING_SUCCESS: раскроет все активные офферы в экспе AUTORUFRONT-19219_new_lk_and_vas_block_design, если страница sales и офферов больше 5', () => {
    const state = mock.value();

    const action: TActionPageLoadingSuccess = {
        type: PAGE_LOADING_SUCCESS,
        payload: {
            sales: {
                offers: [
                    cloneOfferWithHelpers(offerMock).withSaleId('111-aaa').withStatus(OfferStatus.BANNED).value(),
                    cloneOfferWithHelpers(offerMock).withSaleId('222-bbb').value(),
                    cloneOfferWithHelpers(offerMock).withSaleId('333-ccc').value(),
                    cloneOfferWithHelpers(offerMock).withSaleId('444-ddd').value(),
                    cloneOfferWithHelpers(offerMock).withSaleId('555-eee').value(),
                    cloneOfferWithHelpers(offerMock).withSaleId('666-fff').value(),
                ],
                pagination: {
                    page: 1,
                    page_size: 10,
                    total_offers_count: 5,
                    total_page_count: 1,
                },
                filters: {},
            },
            cookies: {},
            config: configStateMock.withPageType('sales').withExperiments({ 'AUTORUFRONT-19219_new_lk_and_vas_block_design': true }).value().data,
        },
    };

    const nextState = reducer(state, action);

    expect(nextState.state['111-aaa']).toBeUndefined();
    expect(nextState.state['222-bbb'].unfolded).toBe(true);
    expect(nextState.state['333-ccc'].unfolded).toBe(true);
    expect(nextState.state['444-ddd'].unfolded).toBe(true);
    expect(nextState.state['555-eee'].unfolded).toBe(true);
    expect(nextState.state['666-fff'].unfolded).toBe(true);
});

it('PAGE_LOADING_SUCCESS: не будет открывать активные офферы если страница reseller-sales и есть кука reseller-sales-stats-hidden', () => {
    const state = mock.value();

    const action: TActionPageLoadingSuccess = {
        type: PAGE_LOADING_SUCCESS,
        payload: {
            sales: {
                offers: [
                    cloneOfferWithHelpers(offerMock).withSaleId('111-aaa').withStatus(OfferStatus.BANNED).value(),
                    cloneOfferWithHelpers(offerMock).withSaleId('222-bbb').value(),
                    cloneOfferWithHelpers(offerMock).withSaleId('333-ccc').value(),
                ],
                pagination: {
                    page: 1,
                    page_size: 10,
                    total_offers_count: 3,
                    total_page_count: 1,
                },
                filters: {},
            },
            cookies: {
                'reseller-sales-stats-hidden': 'true',
            },
            config: configStateMock.withPageType('reseller-sales').value().data,
        },
    };

    const nextState = reducer(state, action);

    expect(nextState.state['111-aaa']).toBeUndefined();
    expect(nextState.state['222-bbb']).toBeUndefined();
    expect(nextState.state['333-ccc']).toBeUndefined();
});

it('SALES_UPDATE_ALL_OFFERS: должен раскрыть первый активный оффер', () => {
    const state = mock.value();

    const action: TStateSalesActionUpdateAllOffers = {
        type: actionTypes.SALES_UPDATE_ALL_OFFERS,
        payload: {
            offers: [
                cloneOfferWithHelpers(offerMock).withSaleId('111-aaa').withStatus(OfferStatus.BANNED).value(),
                cloneOfferWithHelpers(offerMock).withSaleId('222-bbb').value(),
                cloneOfferWithHelpers(offerMock).withSaleId('333-ccc').value(),
            ],
            pagination: {
                page: 1,
                page_size: 10,
                total_offers_count: 3,
                total_page_count: 1,
            },
            filters: {},
        },
    };

    const nextState = reducer(state, action);

    expect(nextState.state['111-aaa']).toBeUndefined();
    expect(nextState.state['222-bbb'].unfolded).toBe(true);
    expect(nextState.state['333-ccc']).toBeUndefined();
});

it('SALES_UPDATE_ALL_OFFERS: должен заменить пагинацию', () => {
    const state = mock.withPagination({
        page: 1,
        page_size: 10,
        total_offers_count: 3,
        total_page_count: 1,
    }).value();

    const newPagination = {
        page: 1,
        page_size: 10,
        total_offers_count: 0,
        total_page_count: 1,
    };

    const action: TStateSalesActionUpdateAllOffers = {
        type: actionTypes.SALES_UPDATE_ALL_OFFERS,
        payload: {
            offers: [],
            pagination: newPagination,
            filters: {},
        },
    };

    const nextState = reducer(state, action);

    expect(nextState.pagination).toEqual(newPagination);
});

it('SALES_EXPAND_ALL_OFFERS_STATS: должен раскрыть все активные офферы', () => {
    const state = mock.withOffers([
        cloneOfferWithHelpers(offerMock).withSaleId('111-aaa').withStatus(OfferStatus.BANNED).value(),
        cloneOfferWithHelpers(offerMock).withSaleId('222-bbb').value(),
        cloneOfferWithHelpers(offerMock).withSaleId('333-ccc').value(),
    ]).value();

    const action: TStateSalesActionExpandAllOffersStats = {
        type: actionTypes.SALES_EXPAND_ALL_OFFERS_STATS,
    };

    const nextState = reducer(state, action);

    expect(nextState.state['111-aaa']).toBeUndefined();
    expect(nextState.state['222-bbb'].unfolded).toBe(true);
    expect(nextState.state['333-ccc'].unfolded).toBe(true);
});

it('SALES_CLOSE_ALL_OFFERS_STATS: должен скрыть стату всех раскрытых офферов', () => {
    const state = mock
        .withOffers([
            cloneOfferWithHelpers(offerMock).withSaleId('111-aaa').withStatus(OfferStatus.BANNED).value(),
            cloneOfferWithHelpers(offerMock).withSaleId('222-bbb').value(),
            cloneOfferWithHelpers(offerMock).withSaleId('333-ccc').value(),
        ])
        .withOfferState('222-bbb', { unfolded: true })
        .withOfferState('333-ccc', { unfolded: true })
        .value();

    const action: TStateSalesActionCloseAllOffersStats = {
        type: actionTypes.SALES_CLOSE_ALL_OFFERS_STATS,
    };

    const nextState = reducer(state, action);

    expect(nextState.state['111-aaa']).toBeUndefined();
    expect(nextState.state['222-bbb']).toEqual({ unfolded: false });
    expect(nextState.state['333-ccc']).toEqual({ unfolded: false });
});

it('OFFER_ACTUALIZE_SUCCESS: должен обновить дату актулизации', () => {
    const state = mock.value();

    const action: StateCardActionActualizeOffer = {
        type: OFFER_ACTUALIZE_SUCCESS,
        payload: {
            params: { offer: state.items[0] },
            timestamp: '12345',
        },
    };

    const nextState = reducer(state, action);

    expect(getActualizeDate(nextState.items[0])).toBe('12345');
});

it('OFFER_AUTORENEW_DISABLED: удаляет расписание у оффера', () => {
    const state = mock
        .withOffers([
            cloneOfferWithHelpers(offerMock).withServiceSchedule(TOfferVas.FRESH).value(),
        ])
        .value();

    const action: StateCardActionOfferAutoRenewDisabled = {
        type: OFFER_AUTORENEW_DISABLED,
        payload: {
            offerId: getIdHash(state.items[0]),
        },
    };

    const nextState = reducer(state, action);

    expect(getServiceScheduleTime(nextState.items[0], TOfferVas.FRESH)).toBeUndefined();
});

describe('OFFER_AUTORENEW_ACTIVATE', () => {
    it('обновляет оффер без установленного расписания', () => {
        const state = mock.value();

        const action: StateCardActionOfferAutoRenewActivate = {
            type: OFFER_AUTORENEW_ACTIVATE,
            payload: {
                time: '01:00',
                offerId: getIdHash(state.items[0]),
            },
        };

        const nextState = reducer(state, action);

        expect(getServiceScheduleTime(nextState.items[0], TOfferVas.FRESH)).toBe('01:00');
    });

    it('обновляет оффер с установленным расписанием', () => {
        const state = mock
            .withOffers([
                cloneOfferWithHelpers(offerMock).withServiceSchedule(TOfferVas.FRESH).value(),
            ])
            .value();

        const action: StateCardActionOfferAutoRenewActivate = {
            type: OFFER_AUTORENEW_ACTIVATE,
            payload: {
                time: '01:00',
                offerId: getIdHash(state.items[0]),
            },
        };

        const nextState = reducer(state, action);

        expect(getServiceScheduleTime(nextState.items[0], TOfferVas.FRESH)).toBe('01:00');
    });
});

it('OFFER_PRICE_UPDATE: должен обновить цену оффера', () => {
    const state = mock.value();

    const action: StateCardActionOfferPriceUpdate = {
        type: OFFER_PRICE_UPDATE,
        payload: {
            offerID: getIdHash(state.items[0]),
            price: 77777,
            currency: Currency.USD,
        },
    };

    const nextState = reducer(state, action);

    expect(getPrice(nextState.items[0])).toEqual(expect.objectContaining({ price: 77777, currency: 'USD' }));
});

it('SALES_TOGGLE_SERVICE_AUTO_PROLONGATION: должен обновить цену оффера', () => {
    const state = mock
        .withOffers([
            cloneOfferWithHelpers(offerMock).withActiveVas([ TOfferVas.COLOR ], { prolongable: false }).value(),
        ])
        .value();

    const action: TStateSalesActionToggleServiceAutoProlongation = {
        type: actionTypes.SALES_TOGGLE_SERVICE_AUTO_PROLONGATION,
        payload: {
            offerId: getIdHash(state.items[0]),
            isOn: true,
            product: TOfferVas.COLOR,
        },
    };

    const nextState = reducer(state, action);

    expect(getServiceInfo(nextState.items[0], TOfferVas.COLOR).prolongable).toBe(true);
});

describe('SALES_SET_UNFOLDED_STATE', () => {
    it('без флага keepOthersUnfolded, расхлопнет целевое объявление и схлопнет остальные', () => {
        const state = mock
            .withOfferState('111-aaa', { unfolded: true })
            .withOfferState('222-bbb', { unfolded: false, loading: true })
            .value();

        const action: TStateSalesActionSetUnfoldedState = {
            type: actionTypes.SALES_SET_UNFOLDED_STATE,
            payload: {
                offerId: '333-ccc',
                isUnfolded: true,
            },
        };

        const nextState = reducer(state, action);

        expect(nextState.state['111-aaa'].unfolded).toBe(false);
        expect(nextState.state['222-bbb']).toEqual({ unfolded: false, loading: true });
        expect(nextState.state['333-ccc'].unfolded).toBe(true);
    });

    it('с флагом keepOthersUnfolded, расхлопнет целевое объявление, не трогая остальные', () => {
        const state = mock
            .withOfferState('111-aaa', { unfolded: true })
            .withOfferState('222-bbb', { unfolded: false, loading: true })
            .value();

        const action: TStateSalesActionSetUnfoldedState = {
            type: actionTypes.SALES_SET_UNFOLDED_STATE,
            payload: {
                offerId: '333-ccc',
                isUnfolded: true,
                keepOthersUnfolded: true,
            },
        };

        const nextState = reducer(state, action);

        expect(nextState.state['111-aaa'].unfolded).toBe(true);
        expect(nextState.state['222-bbb']).toEqual({ unfolded: false, loading: true });
        expect(nextState.state['333-ccc'].unfolded).toBe(true);
    });
});

it('SALES_SET_LOADING_STATE: меняет статус объявления, не трогая остальные', () => {
    const state = mock
        .withOfferState('111-aaa', { unfolded: true, loading: false })
        .withOfferState('222-bbb', { unfolded: false, loading: true })
        .value();

    const action: TStateSalesActionSetLoadingState = {
        type: actionTypes.SALES_SET_LOADING_STATE,
        payload: {
            offerID: '333-ccc',
            isLoading: true,
        },
    };

    const action2: TStateSalesActionSetLoadingState = {
        type: actionTypes.SALES_SET_LOADING_STATE,
        payload: {
            offerID: '222-bbb',
            isLoading: false,
        },
    };

    const nextState = reducer(reducer(state, action), action2);

    expect(nextState.state['111-aaa']).toEqual({ unfolded: true, loading: false });
    expect(nextState.state['222-bbb']).toEqual({ unfolded: false, loading: false });
    expect(nextState.state['333-ccc']).toEqual({ loading: true });
});
