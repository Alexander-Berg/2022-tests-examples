const MockDate = require('mockdate');

const reducer = require('./reducer');

const { PAGE_LOADING_SUCCESS } = require('auto-core/react/actionTypes');
const {
    LISTING_MORE_RESOLVED,
    LISTING_RESOLVED,
} = require('./actionTypes');
const {
    FAVORITES_ADD_ITEM_RESOLVED,
    FAVORITES_DELETE_ITEM_RESOLVED,
} = require('../favorites/types');

let state;
beforeEach(() => {
    state = reducer(undefined, { type: 'INITIAL_ACTION' });
});

afterEach(() => {
    MockDate.reset();
});

it('должен сбросить временный filteredOffersCount при приходе нового листинга', () => {
    const state = {
        filteredOffersCount: 10,
        data: {
            search_parameters: {},
            offers: [],
            pagination: {},
            output_type: 'list',
        },
    };

    const action = {
        type: LISTING_RESOLVED,
        payload: {
            search_parameters: { has_image: true },
            offers: [ { id: 1 } ],
            pagination: {},
            output_type: 'list',
        },
        meta: { ts: 123 },
    };

    expect(reducer(state, action)).toEqual({
        filteredOffersCount: null,
        data: {
            search_parameters: { has_image: true },
            offers: [ { id: 1 } ],
            pagination: {},
            output_type: 'list',
        },
        hasError: false,
        meta: { ts: 123 },
        pending: false,
    });
});

it('должен сбросить временный filteredOffersCount при загрузке новой страницы', () => {
    const state = {
        filteredOffersCount: 10,
        data: {
            offers: [],
            output_type: 'list',
            pagination: {},
            search_parameters: {},
        },
        meta: {},
    };

    const action = {
        type: PAGE_LOADING_SUCCESS,
        payload: {
            listing: {
                offers: [ { id: 1 }, { id: 2 } ],
                output_type: 'list',
                pagination: {},
                request_id: '123',
                search_parameters: {},
                status: 'SUCCESS',
            },
        },
    };

    MockDate.set('2021-07-26');

    expect(reducer(state, action)).toEqual({
        filteredOffersCount: null,
        data: {
            offers: [ { id: 1 }, { id: 2 } ],
            output_type: 'list',
            pagination: {},
            request_id: '123',
            search_parameters: {},
            status: 'SUCCESS',
        },
        meta: { ts: 1627257600000 },
        pending: false,
        searchID: '123',
    });
});

it('должен сбросить временный filteredOffersCount при загрузке новой страницы не листинга', () => {
    const state = {
        filteredOffersCount: 10,
        data: {
            offers: [],
            output_type: 'list',
            pagination: {},
            search_parameters: {},
        },
        meta: {},
    };

    const action = {
        type: PAGE_LOADING_SUCCESS,
        payload: {
            config: {
                pageParams: {},
                pageRequestId: '1234',
            },
        },
    };

    expect(reducer(state, action)).toEqual({
        filteredOffersCount: null,
        data: {
            offers: [],
            output_type: 'list',
            pagination: {},
            search_parameters: {},
        },
        meta: {},
        searchID: '1234',
    });
});

describe('избранное', () => {
    const offer = {
        id: 'foo',
        hash: 'bar',
    };

    const offer2 = {
        id: 'foo2',
        hash: 'bar2',
    };

    it('должен обновить is_favorite на добавление в избранное', () => {
        const state = {
            data: { offers: [ {
                ...offer,
                is_favorite: false,
            } ] },
        };

        const action = {
            type: FAVORITES_ADD_ITEM_RESOLVED,
            payload: { offer },
        };

        expect(reducer(state, action)).toEqual({
            data: { offers: [ {
                ...offer,
                is_favorite: true,
            } ] },
        });
    });

    it('должен обновить is_favorite на удаление из избранного', () => {
        const state = {
            data: { offers: [ {
                ...offer,
                is_favorite: true,
            } ] },
        };

        const action = {
            type: FAVORITES_DELETE_ITEM_RESOLVED,
            payload: { offer },
        };

        expect(reducer(state, action)).toEqual({
            data: { offers: [ {
                ...offer,
                is_favorite: false,
            } ] },
        });
    });

    it('не должен обновить is_favorite на добавление другого оффера в избранное', () => {
        const state = {
            data: { offers: [ {
                ...offer2,
                is_favorite: false,
            } ] },
        };

        const action = {
            type: FAVORITES_ADD_ITEM_RESOLVED,
            payload: { offer },
        };

        expect(reducer(state, action)).toEqual({
            data: { offers: [ {
                ...offer2,
                is_favorite: false,
            } ] },
        });
    });

    it('не должен обновить is_favorite на удаление другого оффера из избранного', () => {
        const state = {
            data: { offers: [ {
                ...offer2,
                is_favorite: true,
            } ] },
        };

        const action = {
            type: FAVORITES_DELETE_ITEM_RESOLVED,
            payload: { offer },
        };

        expect(reducer(state, action)).toEqual({
            data: { offers: [ {
                ...offer2,
                is_favorite: true,
            } ] },
        });
    });
});

describe('searchID', () => {
    it('должен забрать searchID из listing.request_id на PAGE_LOADING_SUCCESS', () => {
        const action = {
            type: PAGE_LOADING_SUCCESS,
            payload: {
                listing: {
                    search_id: '123',
                    request_id: '123',
                },
            },
        };

        expect(reducer(state, action)).toMatchObject({
            data: {
                search_id: '123',
                request_id: '123',
            },
            searchID: '123',
        });
    });

    it('не должен забрать searchID из cardGroupOffers на PAGE_LOADING_SUCCESS', () => {
        const action = {
            type: PAGE_LOADING_SUCCESS,
            payload: {
                cardGroupOffers: {
                    search_id: '123',
                    request_id: '123',
                },
            },
        };

        expect(reducer(state, action)).toMatchObject({
            data: {
                search_id: '123',
                request_id: '123',
            },
            searchID: '',
        });
    });

    it('должен забрать searchID и search_parameters из config.pageRequestId на PAGE_LOADING_SUCCESS, если нет листинга', () => {
        const action = {
            type: PAGE_LOADING_SUCCESS,
            payload: {
                config: {
                    pageParams: { category: 'cars', section: 'all' },
                    pageRequestId: '123',
                },
            },
        };

        expect(reducer(state, action)).toMatchObject({
            data: {
                search_parameters: { category: 'cars', section: 'all' },
            },
            searchID: '123',
        });
    });

    it('должен обновить searchID на LISTING_RESOLVED, если поменялся listing.search_id', () => {
        // первая загрузка
        state = reducer(state, {
            type: PAGE_LOADING_SUCCESS,
            payload: {
                listing: {
                    search_id: '111',
                    request_id: '222',
                },
            },
        });
        // пришел новый листинг
        state = reducer(state, {
            type: LISTING_RESOLVED,
            payload: {
                search_id: '333',
                request_id: '444',
            },
        });

        expect(state).toMatchObject({
            data: {
                search_id: '333',
                request_id: '444',
            },
            searchID: '444',
        });
    });

    it('не должен обновить searchID на LISTING_RESOLVED, если не поменялся listing.search_id', () => {
        // первая загрузка
        state = reducer(state, {
            type: PAGE_LOADING_SUCCESS,
            payload: {
                listing: {
                    search_id: '111',
                    request_id: '222',
                },
            },
        });
        // пришел новый листинг
        state = reducer(state, {
            type: LISTING_RESOLVED,
            payload: {
                search_id: '111',
                request_id: '333',
            },
        });

        expect(state).toMatchObject({
            data: {
                search_id: '111',
                request_id: '333',
            },
            searchID: '222',
        });
    });
});

describe('action LISTING_MORE_RESOLVED', () => {
    it('в пришедших офферах отфильтрует те, которые уже были загружены ранее', () => {
        const state = {
            data: {
                search_parameters: {},
                offers: [
                    { id: '111', hash: 'aaa' },
                    { id: '222', hash: 'aaa' },
                    { id: '333', hash: 'aaa' },
                ],
                pagination: {
                    page: 1,
                    total_page_count: 2,
                },
                output_type: 'list',
            },
        };

        const action = {
            type: LISTING_MORE_RESOLVED,
            payload: {
                offers: [
                    { id: '333', hash: 'aaa' },
                    { id: '444', hash: 'aaa' },
                    { id: '555', hash: 'aaa' },
                ],
                pagination: {
                    page: 2,
                    total_page_count: 2,
                },
                output_type: 'list',
            },
        };

        const result = reducer(state, action);

        expect(result.data.offers).toHaveLength(5);
        expect(result.data.offers.filter(({ id }) => id === '333')).toHaveLength(1);
    });
});
