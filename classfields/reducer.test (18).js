const ROUTES = require('auto-core/router/cabinet.auto.ru/route-names');

const reducer = require('./reducer');

const { PAGE_LOADING_SUCCESS } = require('auto-core/react/actionTypes');

it('должен сохранить данные для клиента с мультипостингом', () => {
    const state = {
        listing: [],
        hasError: false,
        pagination: {},
    };

    const action = {
        type: PAGE_LOADING_SUCCESS,
        payload: {
            unconfirmedListing: [ 3, 4, 5 ],
            [ROUTES.callsNumbers]: {
                callsNumbers: {
                    listing: [ 1, 2, 3 ],
                    pagination: {
                        page: 1,
                    },
                },
                settings: 'settings',
            },
        },
    };

    const newState = reducer(state, action);
    expect(newState).toEqual({
        settings: 'settings',
        hasError: false,
        listing: [ 1, 2, 3 ],
        unconfirmedListing: [ 3, 4, 5 ],
        pagination: {
            page: 1,
        },
    });
});

it('должен сохранить данные для клиента без мультипостинга', () => {
    const state = {
        listing: [],
        hasError: false,
        pagination: {},
    };

    const action = {
        type: PAGE_LOADING_SUCCESS,
        payload: {
            [ROUTES.callsNumbers]: {
                callsNumbers: {
                    listing: [ 1, 2, 3 ],
                    pagination: {
                        page: 1,
                    },
                },
                settings: 'settings',
            },
        },
    };

    const newState = reducer(state, action);
    expect(newState).toEqual({
        settings: 'settings',
        hasError: false,
        listing: [ 1, 2, 3 ],
        pagination: {
            page: 1,
        },
    });
});
