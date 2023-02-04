const reducer = require('./reducer');
const { PAGE_LOADING_SUCCESS } = require('../../actionTypes');
const { COMPARE_ADD_ITEM_RESOLVED, COMPARE_GET_OFFERS_IDS_RESOLVED } = require('./actionTypes');

describe('PAGE_LOADING_SUCCESS', () => {
    it('должен вернуть пустой state, если в payload нет счетчиков шапки', () => {
        expect(reducer(undefined, {
            type: PAGE_LOADING_SUCCESS,
        })).toEqual({
            data: {
                catalog_card_ids: [],
                offers_ids_count: 0,
                models_ids_count: 0,
            },
            hasError: false,
            pending: false,
        });
    });

    it('должен обработать сравнение из payload, если счетчики есть', () => {
        expect(reducer(undefined, {
            type: PAGE_LOADING_SUCCESS,
            payload: {
                headerCounters: {
                    compare: {
                        catalog_card_ids: [ 'favorite-123-abc', '123-456' ],
                    },
                },
            },
        })).toEqual({
            data: {
                catalog_card_ids: [ 'favorite-123-abc', '123-456' ],
                offers_ids_count: 1,
                models_ids_count: 1,
            },
            hasError: false,
            pending: false,
        });
    });
});

describe(COMPARE_ADD_ITEM_RESOLVED, () => {
    it('должен обработать добавление объявления в сравнение', () => {
        expect(reducer(undefined, {
            type: COMPARE_ADD_ITEM_RESOLVED,
            payload: {
                cardId: 'favorite-123-abc',
            },
        })).toEqual({
            data: {
                catalog_card_ids: [ 'favorite-123-abc' ],
                offers_ids_count: 1,
                models_ids_count: 0,
            },
            hasError: false,
            pending: false,
        });
    });

    it('должен обработать добавление модели в сравнение', () => {
        expect(reducer(undefined, {
            type: COMPARE_ADD_ITEM_RESOLVED,
            payload: {
                cardId: '123-456',
            },
        })).toEqual({
            data: {
                catalog_card_ids: [ '123-456' ],
                offers_ids_count: 0,
                models_ids_count: 1,
            },
            hasError: false,
            pending: false,
        });
    });
});

describe(COMPARE_GET_OFFERS_IDS_RESOLVED, () => {
    it('должен обработать пустой ответ от бека', () => {
        expect(reducer(undefined, {
            type: COMPARE_GET_OFFERS_IDS_RESOLVED,
            payload: undefined,
        })).toEqual({
            data: {
                catalog_card_ids: [],
                offers_ids_count: 0,
                models_ids_count: 0,
            },
            hasError: false,
            pending: false,
        });
    });
});
