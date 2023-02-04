import type { ThunkMockStore } from 'autoru-frontend/mocks/mockStore';
import mockStore from 'autoru-frontend/mocks/mockStore';

import type { TStateListing } from 'auto-core/react/dataDomain/listing/TStateListing';

import type { State } from './resetParams';
import resetParams from './resetParams';
import ActionTypes from './../actionTypes';

let store: ThunkMockStore<State>;

it('должен сбросить параметры и сохранить section, category, sort', () => {
    store = mockStore({
        listing: {
            data: {
                search_parameters: {
                    section: 'used',
                    category: 'cars',
                    sort: 'price-asc',
                    catalog_filter: [ { mark: 'AUDI', model: '100' } ],
                },
            },
        } as unknown as TStateListing,
    });

    const expectedActions = [
        {
            type: ActionTypes.LISTING_CHANGE_PARAMS,
            payload: {
                section: 'used',
                category: 'cars',
                sort: 'price-asc',
            },
        },
    ];

    store.dispatch(resetParams());
    expect(store.getActions()).toEqual(expectedActions);
});

it('должен сбросить параметры и сохранить section, category, sort, trucks_category', () => {
    store = mockStore({
        listing: {
            data: {
                search_parameters: {
                    section: 'used',
                    category: 'trucks',
                    trucks_category: 'TRUCK',
                    sort: 'price-asc',
                    catalog_filter: [ { mark: 'AUDI', model: '100' } ],
                },
            },
        } as unknown as TStateListing,
    });

    const expectedActions = [
        {
            type: ActionTypes.LISTING_CHANGE_PARAMS,
            payload: {
                section: 'used',
                category: 'trucks',
                trucks_category: 'TRUCK',
                sort: 'price-asc',
            },
        },
    ];

    store.dispatch(resetParams());
    expect(store.getActions()).toEqual(expectedActions);
});

it('должен сбросить параметры и сохранить section, category, sort, moto_category', () => {
    store = mockStore({
        listing: {
            data: {
                search_parameters: {
                    section: 'used',
                    category: 'moto',
                    moto_category: 'ATV',
                    sort: 'price-asc',
                    catalog_filter: [ { mark: 'AUDI', model: '100' } ],
                },
            },
        } as unknown as TStateListing,
    });

    const expectedActions = [
        {
            type: ActionTypes.LISTING_CHANGE_PARAMS,
            payload: {
                section: 'used',
                category: 'moto',
                moto_category: 'ATV',
                sort: 'price-asc',
            },
        },
    ];

    store.dispatch(resetParams());
    expect(store.getActions()).toEqual(expectedActions);
});

it('должен сбросить параметры и сохранить параметры дилера', () => {
    store = mockStore({
        listing: {
            data: {
                search_parameters: {
                    section: 'used',
                    category: 'cars',
                    dealer_code: 'borishof_balashiha_bmw',
                    dealer_id: '20136851',
                    sort: 'price-asc',
                    catalog_filter: [ { mark: 'AUDI', model: '100' } ],
                },
            },
        } as unknown as TStateListing,
    });

    const expectedActions = [
        {
            type: ActionTypes.LISTING_CHANGE_PARAMS,
            payload: {
                section: 'used',
                category: 'cars',
                dealer_code: 'borishof_balashiha_bmw',
                dealer_id: '20136851',
                sort: 'price-asc',
            },
        },
    ];

    store.dispatch(resetParams());
    expect(store.getActions()).toEqual(expectedActions);
});
