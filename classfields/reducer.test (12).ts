import type { TStateListingPriceRanges } from 'auto-core/react/dataDomain/listingPriceRanges/types';

import reducer from './reducer';
import { ActionType } from './actionType';
import type { ListingPriceRangeAction } from './types';

let state: TStateListingPriceRanges;
beforeEach(() => {
    state = {
        pending: false,
        data: [],
    };
});

it('правильно меняет статус при пендинге', () => {
    const action: ListingPriceRangeAction = {
        type: ActionType.LISTING_PRICE_RANGES_PENDING,
    };

    const newState = reducer(state, action);
    expect(newState).toMatchObject({
        pending: true,
    });
});

it('правильно меняет статус когда вернулась ошибка', () => {
    const action: ListingPriceRangeAction = {
        type: ActionType.LISTING_PRICE_RANGES_REJECTED,
    };

    const newState = reducer(state, action);
    expect(newState).toMatchObject({
        pending: false,
    });
});

it('правильно меняет статус и сохраняет данные при успехе', () => {
    const priceRangeFiltersMock = [
        { price_to: 500000, offers_count: 1 },
        { price_to: 600000, offers_count: 2 },
        { price_to: 700000, offers_count: 3 },
    ];
    const action: ListingPriceRangeAction = {
        type: ActionType.LISTING_PRICE_RANGES_RESOLVED,
        payload: priceRangeFiltersMock,
    };

    const newState = reducer(state, action);
    expect(newState).toMatchObject({
        pending: false,
        data: priceRangeFiltersMock,
    });
});
