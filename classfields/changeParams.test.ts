import type { ThunkMockStore } from 'autoru-frontend/mocks/mockStore';
import mockStore from 'autoru-frontend/mocks/mockStore';

import type { TStateListing } from 'auto-core/react/dataDomain/listing/TStateListing';

import changeParams from './changeParams';
import ActionTypes from './../actionTypes';

interface State {
    listing: TStateListing;
}

let store: ThunkMockStore<State>;
beforeEach(() => {
    store = mockStore({
        listing: {
            data: {
                search_parameters: {
                    catalog_filter: [ { mark: 'AUDI', model: '100' } ],
                },
            },
        } as TStateListing,
    });
});

it('должен отправить экшен "LISTING_CHANGE_PARAMS"', () => {
    const expectedActions = [
        { type: ActionTypes.LISTING_CHANGE_PARAMS, payload: {
            catalog_filter: [ { mark: 'AUDI', model: '100' } ],
            price_from: 1000,
        } },
    ];

    store.dispatch(changeParams({ price_from: 1000 }));
    expect(store.getActions()).toEqual(expectedActions);
});

it('должен вернуть новые вычисленные параметры поиска', () => {
    const result = store.dispatch(changeParams({ price_from: 100 }));
    expect(result).toEqual({
        catalog_filter: [ { mark: 'AUDI', model: '100' } ],
        price_from: 100,
    });
});

it('должен убрать search_tag=certificate при переходе в new', () => {
    const store = mockStore({
        listing: {
            data: {
                search_parameters: {
                    search_tag: [ 'certificate' ],
                },
            },
        },
    });

    const expectedActions = [
        {
            type: ActionTypes.LISTING_CHANGE_PARAMS,
            payload: {
                section: 'new',
            } },
    ];

    store.dispatch(changeParams({ section: 'new' }));
    expect(store.getActions()).toEqual(expectedActions);
});

describe('configuration_id', () => {
    it('должен убрать configuration_id при изменеии catalog_filter', () => {
        const store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        catalog_filter: [ { mark: 'AUDI', model: 'A3', generation: '20785010' } ],
                        configuration_id: '20785459',
                    },
                },
            },
        });

        const expectedActions = [
            {
                type: ActionTypes.LISTING_CHANGE_PARAMS,
                payload: {
                    catalog_filter: [ { mark: 'AUDI', model: 'A4' } ],
                } },
        ];

        store.dispatch(changeParams({ catalog_filter: [ { mark: 'AUDI', model: 'A4' } ] }));
        expect(store.getActions()).toEqual(expectedActions);
    });

    it('должен убрать configuration_id при изменеии body_type_group', () => {
        const store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        catalog_filter: [ { mark: 'AUDI', model: 'A3' } ],
                        configuration_id: '20785459',
                        body_type_group: 'SEDAN',
                    },
                },
            },
        });

        const expectedActions = [
            {
                type: ActionTypes.LISTING_CHANGE_PARAMS,
                payload: {
                    catalog_filter: [ { mark: 'AUDI', model: 'A3' } ],
                    body_type_group: [ 'CABRIO' ],
                } },
        ];

        store.dispatch(changeParams({ body_type_group: [ 'CABRIO' ] }));
        expect(store.getActions()).toEqual(expectedActions);
    });
});

describe('tech_param_id', () => {
    it('должен убрать tech_param_id при изменении catalog_filter', () => {
        const store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        catalog_filter: [ { mark: 'AUDI', model: 'A3', generation: '20785010' } ],
                        configuration_id: '20785459',
                        tech_param_id: '20785628',
                    },
                },
            },
        });

        const expectedActions = [
            {
                type: ActionTypes.LISTING_CHANGE_PARAMS,
                payload: {
                    catalog_filter: [ { mark: 'AUDI', model: 'A4' } ],
                } },
        ];

        store.dispatch(changeParams({ catalog_filter: [ { mark: 'AUDI', model: 'A4' } ] }));
        expect(store.getActions()).toEqual(expectedActions);
    });

    it('должен убрать tech_param_id при изменении displacement_from', () => {
        const store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        catalog_filter: [ { mark: 'AUDI', model: 'A3', generation: '20785010' } ],
                        configuration_id: '20785459',
                        tech_param_id: '20785628',
                        displacement_from: 1000,
                        displacement_to: 1000,
                    },
                },
            },
        });

        const expectedActions = [
            {
                type: ActionTypes.LISTING_CHANGE_PARAMS,
                payload: {
                    catalog_filter: [ { mark: 'AUDI', model: 'A3', generation: '20785010' } ],
                    configuration_id: '20785459',
                    displacement_from: 800,
                    displacement_to: 1000,
                } },
        ];

        store.dispatch(changeParams({ displacement_from: 800 }));
        expect(store.getActions()).toEqual(expectedActions);
    });

    it('должен убрать tech_param_id при изменении displacement_to', () => {
        const store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        catalog_filter: [ { mark: 'AUDI', model: 'A3', generation: '20785010' } ],
                        configuration_id: '20785459',
                        tech_param_id: '20785628',
                        displacement_from: 1000,
                        displacement_to: 1000,
                    },
                },
            },
        });

        const expectedActions = [
            {
                type: ActionTypes.LISTING_CHANGE_PARAMS,
                payload: {
                    catalog_filter: [ { mark: 'AUDI', model: 'A3', generation: '20785010' } ],
                    configuration_id: '20785459',
                    displacement_from: 1000,
                    displacement_to: 1200,
                } },
        ];

        store.dispatch(changeParams({ displacement_to: 1200 }));
        expect(store.getActions()).toEqual(expectedActions);
    });

    it('должен убрать tech_param_id при изменении gear_type', () => {
        const store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        catalog_filter: [ { mark: 'AUDI', model: 'A3', generation: '20785010' } ],
                        configuration_id: '20785459',
                        tech_param_id: '20785628',
                        gear_type: 'FORWARD_CONTROL',
                    },
                },
            },
        });

        const expectedActions = [
            {
                type: ActionTypes.LISTING_CHANGE_PARAMS,
                payload: {
                    catalog_filter: [ { mark: 'AUDI', model: 'A3', generation: '20785010' } ],
                    configuration_id: '20785459',
                } },
        ];

        store.dispatch(changeParams({ gear_type: [] }));
        expect(store.getActions()).toEqual(expectedActions);
    });

    it('должен убрать tech_param_id при изменении engine_group', () => {
        const store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        catalog_filter: [ { mark: 'AUDI', model: 'A3', generation: '20785010' } ],
                        configuration_id: '20785459',
                        tech_param_id: '20785628',
                        engine_group: 'DIESEL',
                    },
                },
            },
        });

        const expectedActions = [
            {
                type: ActionTypes.LISTING_CHANGE_PARAMS,
                payload: {
                    catalog_filter: [ { mark: 'AUDI', model: 'A3', generation: '20785010' } ],
                    configuration_id: '20785459',
                    engine_group: [ 'GASOLINE' ],
                } },
        ];

        store.dispatch(changeParams({ engine_group: [ 'GASOLINE' ] }));
        expect(store.getActions()).toEqual(expectedActions);
    });

    it('должен убрать tech_param_id при изменении transmission', () => {
        const store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        catalog_filter: [ { mark: 'AUDI', model: 'A3', generation: '20785010' } ],
                        configuration_id: '20785459',
                        tech_param_id: '20785628',
                        transmission: 'ROBOT',
                    },
                },
            },
        });

        const expectedActions = [
            {
                type: ActionTypes.LISTING_CHANGE_PARAMS,
                payload: {
                    catalog_filter: [ { mark: 'AUDI', model: 'A3', generation: '20785010' } ],
                    configuration_id: '20785459',
                    transmission: [ 'VARIATOR' ],
                } },
        ];

        store.dispatch(changeParams({ transmission: [ 'VARIATOR' ] }));
        expect(store.getActions()).toEqual(expectedActions);
    });

    it('должен убрать configuration_id при изменеии body_type_group', () => {
        const store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        catalog_filter: [ { mark: 'AUDI', model: 'A4' } ],
                        configuration_id: '20785459',
                        tech_param_id: '20785628',
                        body_type_group: 'SEDAN',
                    },
                },
            },
        });

        const expectedActions = [
            {
                type: ActionTypes.LISTING_CHANGE_PARAMS,
                payload: {
                    catalog_filter: [ { mark: 'AUDI', model: 'A4' } ],
                    body_type_group: [ 'CABRIO' ],
                } },
        ];

        store.dispatch(changeParams({ body_type_group: [ 'CABRIO' ] }));
        expect(store.getActions()).toEqual(expectedActions);
    });
});

describe('чистка пустых значений', () => {
    beforeEach(() => {
        store = mockStore({
            listing: {
                data: {
                    search_parameters: {},
                },
            } as TStateListing,
        });
    });

    it('число', () => {
        const expectedActions = [
            { type: ActionTypes.LISTING_CHANGE_PARAMS, payload: { price_from: 1 } },
            { type: ActionTypes.LISTING_CHANGE_PARAMS, payload: {} },
        ];

        store.dispatch(changeParams({ price_from: 1 }));
        store.dispatch(changeParams({ price_from: 0 }));
        expect(store.getActions()).toEqual(expectedActions);
    });

    it('строка', () => {
        const expectedActions = [
            { type: ActionTypes.LISTING_CHANGE_PARAMS, payload: { year_from: 2016 } },
            { type: ActionTypes.LISTING_CHANGE_PARAMS, payload: {} },
        ];

        store.dispatch(changeParams({ year_from: 2016 }));
        store.dispatch(changeParams({ year_from: undefined }));
        expect(store.getActions()).toEqual(expectedActions);
    });

    it('массив', () => {
        const expectedActions = [
            { type: ActionTypes.LISTING_CHANGE_PARAMS, payload: { body_type_group: [ 'SEDAN' ] } },
            { type: ActionTypes.LISTING_CHANGE_PARAMS, payload: {} },
        ];

        store.dispatch(changeParams({ body_type_group: [ 'SEDAN' ] }));
        store.dispatch(changeParams({ body_type_group: [] }));
        expect(store.getActions()).toEqual(expectedActions);
    });

    it('не должен удалить has_image=false', () => {
        const expectedActions = [
            { type: ActionTypes.LISTING_CHANGE_PARAMS, payload: { has_image: false } },
        ];

        store.dispatch(changeParams({ has_image: false }));
        expect(store.getActions()).toEqual(expectedActions);
    });

    it('не должен удалить in_stock=IN_STOCK для section=used|all', () => {
        const expectedActions = [
            { type: ActionTypes.LISTING_CHANGE_PARAMS, payload: { in_stock: 'IN_STOCK' } },
        ];

        store.dispatch(changeParams({ in_stock: 'IN_STOCK' }));
        expect(store.getActions()).toEqual(expectedActions);
    });

    it('не должен удалить in_stock=ANY_STOCK для section=used|all', () => {
        const expectedActions = [
            { type: ActionTypes.LISTING_CHANGE_PARAMS, payload: { } },
        ];

        store.dispatch(changeParams({ in_stock: 'ANY_STOCK' }));
        expect(store.getActions()).toEqual(expectedActions);
    });
});

describe('чистка полей при смене секции на new', () => {
    beforeEach(() => {
        store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        body_type_group: [ 'SEDAN' ],
                        in_stock: 'ANY_STOCK',
                        km_age_from: 1000,
                        catalog_filter: [ { mark: 'AUDI', model: 'A4', nameplate: '1', generation: '2', configuration: '3', tech_param: '4' } ],
                        pts_status: 1,
                        seller_group: 'PRIVATE',
                        with_warranty: true,
                    },
                },
            } as unknown as TStateListing,
        });
    });

    it('должен убрать ненужные поля при переключении секции', () => {
        const expectedActions = [
            {
                type: ActionTypes.LISTING_CHANGE_PARAMS,
                payload: { body_type_group: [ 'SEDAN' ], catalog_filter: [ { mark: 'AUDI', model: 'A4', nameplate: '1' } ], section: 'new' },
            },
        ];

        store.dispatch(changeParams({ section: 'new' }));
        expect(store.getActions()).toEqual(expectedActions);
    });

    it('не должен сломаться при переключении секции, если в параметрах нет ммм', () => {
        store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        body_type_group: [ 'SEDAN' ],
                        has_image: true,
                        has_video: true,
                        in_stock: 'ANY_STOCK',
                        pts_status: 1,
                        seller_group: 'PRIVATE',
                        with_warranty: true,
                    },
                },
            } as unknown as TStateListing,
        });

        const expectedActions = [
            { type: ActionTypes.LISTING_CHANGE_PARAMS, payload: { body_type_group: [ 'SEDAN' ], has_video: true, section: 'new' } },
        ];

        store.dispatch(changeParams({ section: 'new' }));
        expect(store.getActions()).toEqual(expectedActions);
    });
});

it('должен убрать pinned_offer_id при изменении параметров', () => {
    store = mockStore({
        listing: {
            data: {
                search_parameters: {
                    pinned_offer_id: '123',
                    catalog_filter: [ { mark: 'AUDI' } ],
                },
            },
        } as unknown as TStateListing,
    });

    const expectedActions = [
        { type: ActionTypes.LISTING_CHANGE_PARAMS, payload: { has_image: false, catalog_filter: [ { mark: 'AUDI' } ] } },
    ];

    store.dispatch(changeParams({ has_image: false }));
    expect(store.getActions()).toEqual(expectedActions);
});
