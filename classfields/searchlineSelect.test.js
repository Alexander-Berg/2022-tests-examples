const searchlineSelect = require('./searchlineSelect');
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const { location } = global;
let store;
beforeEach(() => {
    delete global.location;
    global.location = {
        assign: jest.fn(),
    };
});

afterEach(() => {
    global.location = location;
});

it('должен перезагрузить страницу при смене секции', () => {
    store = mockStore({
        listing: {
            data: {
                search_parameters: {
                    section: 'all',
                },
            },
        },
    });

    store.dispatch(
        searchlineSelect({
            category: 'CARS',
            params: {
                section: 'new',
            },
            url: 'item-url',
        }),
    );

    expect(global.location.assign).toHaveBeenCalledWith('item-url');
});

it('должен перезагрузить страницу при смене категории', () => {
    store = mockStore({
        listing: {
            data: {
                search_parameters: {
                    category: 'cars',
                    section: 'all',
                },
            },
        },
    });

    store.dispatch(
        searchlineSelect({
            category: 'TRUCKS',
            params: {
                section: 'all',
            },
            url: 'item-url',
        }),
    );

    expect(global.location.assign).toHaveBeenCalledWith('item-url');
});

it('должен перезагрузить страницу при смене гео', () => {
    store = mockStore({
        listing: {
            data: {
                search_parameters: {
                    category: 'cars',
                    section: 'all',
                },
            },
        },
    });

    store.dispatch(
        searchlineSelect({
            category: 'CARS',
            params: {
                geo_id: 2,
                section: 'all',
            },
            url: 'item-url',
        }),
    );

    expect(global.location.assign).toHaveBeenCalledWith('item-url');
});

it('не должен перезагрузить страницу, если секция и категория совпадают', () => {
    store = mockStore({
        listing: {
            data: {
                search_parameters: {
                    category: 'cars',
                    section: 'all',
                },
            },
        },
    });

    store.dispatch(
        searchlineSelect({
            category: 'CARS',
            params: {
                body_type_group: [ 'SEDAN' ],
                section: 'all',
            },
            url: 'item-url',
        }),
    );

    expect(store.getActions()).toEqual([
        {
            type: 'LISTING_CHANGE_PARAMS',
            payload: {
                body_type_group: [ 'SEDAN' ],
                section: 'all',
            },
        },
    ]);
});
