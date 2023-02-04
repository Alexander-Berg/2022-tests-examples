jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

import type { ThunkMockStore } from 'autoru-frontend/mocks/mockStore';
import mockStore from 'autoru-frontend/mocks/mockStore';

import type { TStateListingPriceRanges } from 'auto-core/react/dataDomain/listingPriceRanges/types';
import { getResource } from 'auto-core/react/lib/gateApi';

import { ActionType } from '../actionType';

import fetchPriceRanges from './fetchPriceRanges';

const getResourceMock = getResource as jest.MockedFunction<typeof getResource>;

let store: ThunkMockStore<{ listingPriceRanges: TStateListingPriceRanges }>;
beforeEach(() => {
    store = mockStore({ listingPriceRanges: {
        data: [],
        pending: false,
    } });
});

it('должен отправить экшен "LISTING_PRICE_RANGES_PENDING"', () => {
    const searchParams = { price_from: 1000 };
    const expectedActions = [
        {
            type: ActionType.LISTING_PRICE_RANGES_PENDING,
        },
    ];

    getResourceMock.mockImplementation(() => Promise.resolve());

    store.dispatch(fetchPriceRanges(searchParams));
    expect(store.getActions()).toEqual(expectedActions);
});

it('после получения успешного ответа должен отправить экшен "LISTING_PRICE_RANGES_RESOLVED"', () => {
    const expectedAction = {
        type: ActionType.LISTING_PRICE_RANGES_RESOLVED,
        payload: 'awesome response',
    };

    getResourceMock.mockImplementation(() => Promise.resolve(expectedAction.payload));

    const pr = store.dispatch(fetchPriceRanges({})) as Promise<unknown>;
    return pr.then((res) => {
        expect(res).toEqual(expectedAction.payload);
    });
});

it('после получения ошибки должен отправить экшен "LISTING_PRICE_RANGES_REJECTED"', () => {
    const expectedAction = {
        type: ActionType.LISTING_PRICE_RANGES_REJECTED,
        payload: 'error',
    };

    getResourceMock.mockImplementation(() => Promise.reject(expectedAction.payload));

    const pr = store.dispatch(fetchPriceRanges({})) as Promise<unknown>;
    return pr.then((err) => {
        expect(err).toEqual([]);
    });
});
