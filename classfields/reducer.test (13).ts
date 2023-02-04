import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';
import { offerStatsDayMock } from 'auto-core/models/offerStats/mocks';

import reducer from './reducer';
import type { StateOfferStatsApplyPredictionAction, StateOfferStatsDiscardPredictionAction, StateOfferStatsUpdateItemDaysAction } from './types';
import actionTypes from './actionTypes';

const offerId = 'offer-id';

describe('actions OFFER_STATS_UPDATE_ITEM_DAYS:', () => {
    it('корректно сморжит дни в стейте', () => {
        const initialState = {
            [offerId]: [
                offerStatsDayMock.withViews(42).withDateFromToday(-2).value(),
                offerStatsDayMock.withViews(43).withDateFromToday(-1).value(),
                offerStatsDayMock.withViews(44).withDateFromToday(0).value(),
            ],
        };

        const action: StateOfferStatsUpdateItemDaysAction = {
            type: actionTypes.OFFER_STATS_UPDATE_ITEM_DAYS,
            payload: {
                offer_id: offerId,
                counters: [
                    offerStatsDayMock.withViews(72).withDateFromToday(-1).value(),
                    offerStatsDayMock.withViews(73).withDateFromToday(0).value(),
                    offerStatsDayMock.withViews(74).withDateFromToday(1).value(),
                ],
            },
        };

        const nextState = reducer(initialState, action);
        const views = nextState[offerId].map(({ views }) => views);
        expect(views).toEqual([ 42, 43, 44, 74 ]);
    });

    it('если такого оффера нет, вернет текущий стейт', () => {
        const initialState = {
            [offerId]: [
                offerStatsDayMock.withViews(42).withDateFromToday(-2).value(),
                offerStatsDayMock.withViews(43).withDateFromToday(-1).value(),
                offerStatsDayMock.withViews(44).withDateFromToday(0).value(),
            ],
        };

        const action: StateOfferStatsUpdateItemDaysAction = {
            type: actionTypes.OFFER_STATS_UPDATE_ITEM_DAYS,
            payload: {
                offer_id: 'aaa-111',
                counters: [
                    offerStatsDayMock.withViews(72).withDateFromToday(-1).value(),
                    offerStatsDayMock.withViews(73).withDateFromToday(0).value(),
                    offerStatsDayMock.withViews(74).withDateFromToday(1).value(),
                ],
            },
        };

        const nextState = reducer(initialState, action);
        expect(nextState).toBe(initialState);
    });
});

describe('action OFFER_STATS_APPLY_PREDICTION:', () => {
    it('проапдейтит просмотры у переданного оффера', () => {
        const initialState = {
            [offerId]: [
                offerStatsDayMock.withViews(42).value(),
                offerStatsDayMock.withViews(0).withPredict({ [TOfferVas.FRESH]: 121, [TOfferVas.TURBO]: 142 }).value(),
                offerStatsDayMock.withViews(0).withPredict({ [TOfferVas.FRESH]: 111, [TOfferVas.TURBO]: 122 }).value(),
            ],
        };

        const action: StateOfferStatsApplyPredictionAction = {
            type: actionTypes.OFFER_STATS_APPLY_PREDICTION,
            payload: {
                service: TOfferVas.TURBO,
                offerId,
            },
        };

        const nextState = reducer(initialState, action);

        expect(nextState[offerId][0].views).toBe(42);
        expect(nextState[offerId][1].views).toBe(142);
        expect(nextState[offerId][2].views).toBe(122);
    });

    it('если такого оффера нет, вернет текущий стейт', () => {
        const initialState = {
            [offerId]: [
                offerStatsDayMock.withViews(42).value(),
                offerStatsDayMock.withViews(0).withPredict({ [TOfferVas.FRESH]: 121, [TOfferVas.TURBO]: 142 }).value(),
                offerStatsDayMock.withViews(0).withPredict({ [TOfferVas.FRESH]: 111, [TOfferVas.TURBO]: 122 }).value(),
            ],
        };

        const action: StateOfferStatsApplyPredictionAction = {
            type: actionTypes.OFFER_STATS_APPLY_PREDICTION,
            payload: {
                service: TOfferVas.TURBO,
                offerId: 'aaa-111',
            },
        };

        const nextState = reducer(initialState, action);

        expect(nextState).toBe(initialState);
    });

    it('если у оффера нет предикта, вернет текущий стейт', () => {
        const initialState = {
            [offerId]: [
                offerStatsDayMock.withViews(42).value(),
            ],
        };

        const action: StateOfferStatsApplyPredictionAction = {
            type: actionTypes.OFFER_STATS_APPLY_PREDICTION,
            payload: {
                service: TOfferVas.TURBO,
                offerId,
            },
        };

        const nextState = reducer(initialState, action);

        expect(nextState).toEqual(initialState);
    });
});

describe('action OFFER_STATS_DISCARD_PREDICTION:', () => {
    it('сбросит просмотры у переданного оффера', () => {
        const initialState = {
            [offerId]: [
                offerStatsDayMock.withViews(42).value(),
                offerStatsDayMock.withViews(142).withPredict({ [TOfferVas.FRESH]: 121, [TOfferVas.TURBO]: 142 }).value(),
                offerStatsDayMock.withViews(122).withPredict({ [TOfferVas.FRESH]: 111, [TOfferVas.TURBO]: 122 }).value(),
            ],
        };

        const action: StateOfferStatsDiscardPredictionAction = {
            type: actionTypes.OFFER_STATS_DISCARD_PREDICTION,
            payload: {
                offerId,
            },
        };

        const nextState = reducer(initialState, action);

        expect(nextState[offerId][0].views).toBe(42);
        expect(nextState[offerId][1].views).toBe(0);
        expect(nextState[offerId][2].views).toBe(0);
    });

    it('если такого оффера нет, вернет текущий стейт', () => {
        const initialState = {
            [offerId]: [
                offerStatsDayMock.withViews(42).value(),
                offerStatsDayMock.withViews(142).withPredict({ [TOfferVas.FRESH]: 121, [TOfferVas.TURBO]: 142 }).value(),
                offerStatsDayMock.withViews(122).withPredict({ [TOfferVas.FRESH]: 111, [TOfferVas.TURBO]: 122 }).value(),
            ],
        };

        const action: StateOfferStatsDiscardPredictionAction = {
            type: actionTypes.OFFER_STATS_DISCARD_PREDICTION,
            payload: {
                offerId: 'aaa-111',
            },
        };

        const nextState = reducer(initialState, action);

        expect(nextState).toBe(initialState);
    });

    it('если у оффера нет предикта, вернет текущий стейт', () => {
        const initialState = {
            [offerId]: [
                offerStatsDayMock.withViews(42).value(),
            ],
        };

        const action: StateOfferStatsDiscardPredictionAction = {
            type: actionTypes.OFFER_STATS_DISCARD_PREDICTION,
            payload: {
                offerId,
            },
        };

        const nextState = reducer(initialState, action);

        expect(nextState).toEqual(initialState);
    });
});
