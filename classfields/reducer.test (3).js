const reducer = require('./reducer');
const { OFFER_ACTUALIZE_SUCCESS, OFFER_AUTORENEW_ACTIVATE, OFFER_PRICE_UPDATE } = require('./actionTypes');
const {
    FAVORITES_ADD_ITEM_RESOLVED,
    FAVORITES_DELETE_ITEM_RESOLVED,
} = require('../favorites/types');

it('должен обновить дату актулизации на экшен OFFER_ACTUALIZE_SUCCESS', () => {
    const state = {
        additional_info: {
            actualize_date: 98765,
        },
        id: 'foo',
        saleId: 'foo-bar',
    };

    const action = {
        type: OFFER_ACTUALIZE_SUCCESS,
        payload: {
            timestamp: 12345,
        },
    };

    expect(reducer(state, action)).toEqual({
        additional_info: {
            actualize_date: 12345,
        },
        id: 'foo',
        saleId: 'foo-bar',
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
            ...offer,
            is_favorite: false,
        };

        const action = {
            type: FAVORITES_ADD_ITEM_RESOLVED,
            payload: { offer },
        };

        expect(reducer(state, action)).toEqual({
            ...offer,
            is_favorite: true,
        });
    });

    it('должен обновить is_favorite на удаление из избранного', () => {
        const state = {
            ...offer,
            is_favorite: true,
        };

        const action = {
            type: FAVORITES_DELETE_ITEM_RESOLVED,
            payload: { offer },
        };

        expect(reducer(state, action)).toEqual({
            ...offer,
            is_favorite: false,
        });
    });

    it('не должен обновить is_favorite на добавление другого оффера в избранное', () => {
        const state = {
            ...offer2,
            is_favorite: false,
        };

        const action = {
            type: FAVORITES_ADD_ITEM_RESOLVED,
            payload: { offer },
        };

        expect(reducer(state, action)).toEqual({
            ...offer2,
            is_favorite: false,
        });
    });

    it('не должен обновить is_favorite на удаление другого оффера из избранного', () => {
        const state = {
            ...offer2,
            is_favorite: true,
        };

        const action = {
            type: FAVORITES_DELETE_ITEM_RESOLVED,
            payload: { offer },
        };

        expect(reducer(state, action)).toEqual({
            ...offer2,
            is_favorite: true,
        });
    });
});

describe('экшен OFFER_AUTORENEW_ACTIVATE', () => {
    it('обновляет пустой state', () => {
        const state = {
            saleId: 'offer-id-mock',
        };

        const action = {
            type: OFFER_AUTORENEW_ACTIVATE,
            payload: {
                time: '01:00',
                offerId: 'offer-id-mock',
            },
        };

        expect(reducer(state, action)).toMatchSnapshot();
    });

    it('обновляет не пустой state', () => {
        const state = {
            service_schedules: { products: { foo: {} } },
            saleId: 'offer-id-mock',
        };

        const action = {
            type: OFFER_AUTORENEW_ACTIVATE,
            payload: {
                time: '01:00',
                offerId: 'offer-id-mock',
            },
        };

        expect(reducer(state, action)).toMatchSnapshot();
    });
});

describe('экшен OFFER_PRICE_UPDATE', () => {
    it('обновляет цену оффера', () => {
        const state = {
            price_info: {
                price: 123,
                RUR: 123,
                USD: 321,
                currency: 'RUR',
            },
            saleId: 'offer-id-mock',
        };

        const action = {
            type: OFFER_PRICE_UPDATE,
            payload: {
                price: 42,
                currency: 'USD',
                offerID: 'offer-id-mock',
            },
        };

        expect(reducer(state, action)).toMatchSnapshot();
    });

    it('не обновляет цену оффера если оффер не торт', () => {
        const state = {
            price_info: {
                price: 123,
                RUR: 123,
                USD: 321,
                currency: 'RUR',
            },
            saleId: 'offer-id-mock',
        };

        const action = {
            type: OFFER_PRICE_UPDATE,
            payload: {
                price: 42,
                currency: 'USD',
                offerID: 'offer-id-ne-tort',
            },
        };

        expect(reducer(state, action)).toBe(state);
    });
});
