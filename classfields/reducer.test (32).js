const ROUTES = require('auto-core/router/cabinet.auto.ru/route-names');
const { PAGE_LOADING_SUCCESS } = require('auto-core/react/actionTypes');

const PRODUCTS = require('auto-core/data/trade-in/products');

const tradeInReducer = require('./reducer');

it(`должен сохранить данные по трейд-ину при загрузке и апдейте страницы`, () => {
    const state = {
        isLoading: false,
    };

    const action = {
        type: PAGE_LOADING_SUCCESS,
        payload: {
            [ROUTES.tradeIn]: {
                dealerTradeIn: { foo: 123 },
                dealerCampaignProducts: {
                    items: [
                        { id: PRODUCTS.cars_used },
                        { id: PRODUCTS.cars_new },
                    ],
                },
            },
        },
    };

    const newState = tradeInReducer(state, action);
    expect(newState).toMatchSnapshot();
});

it('should return an initial state, if state = undefined and action = {}', () => {
    expect(tradeInReducer(undefined, {})).toMatchSnapshot();
});

it('должен обновить section.paging, если action.type = TRADE_IN_UPDATE_PAGING', () => {
    expect(tradeInReducer(
        {
            filter: 'somefilters',
            items: 'someitems',
            paging: { min: 0, max: 20, current: 15 },
        },
        {
            type: 'TRADE_IN_UPDATE_PAGING',
            payload: { min: 0, max: 20, current: 21 },
        },
    ))
        .toEqual({
            filter: 'somefilters',
            items: 'someitems',
            paging: { min: 0, max: 20, current: 21 },
        });
});

it('должен обновить filters, items and paging, если action.type = TRADE_IN_RESET_TABLE', () => {
    expect(tradeInReducer(
        {
            isLoading: false,
            filters: 'somefilters',
            items: 'someitems',
            paging: 'paging',
        },
        {
            type: 'TRADE_IN_RESET_TABLE',
            payload: {
                filters: 'newFilters',
                items: 'newItems',
                paging: 'newPaging',
            },
        },
    ))
        .toEqual({
            isLoading: false,
            filters: 'newFilters',
            items: 'newItems',
            paging: 'newPaging',
        });
});

it('должен добавить items to в конец списка, если action.type = TRADE_IN_ADD_ITEMS', () => {
    expect(tradeInReducer(
        {
            isLoading: false,
            filters: 'newFilters',
            items: [ 1, 2, 3 ],
            paging: { current: 0, min: 0, max: 20 },
        },
        {
            type: 'TRADE_IN_ADD_ITEMS',
            payload: {
                items: [ 4, 5, 6 ],
                paging: { current: 1, min: 0, max: 20 },
                filters: 'newFilters',
            },
        },
    ))
        .toEqual({
            isLoading: false,
            filters: 'newFilters',
            paging: { current: 1, min: 0, max: 20 },
            items: [ 1, 2, 3, 4, 5, 6 ],
        });
});

it('должен обновить switcher по id, если action.payload = TRADE_IN_UPDATE_SWITCHER_BY_ID', () => {
    expect(tradeInReducer(
        {
            switcherId: {
                isDisabled: false,
                isActive: true,
            },
            switcherId1: {
                isDisabled: false,
                isActive: false,
            },
        },
        {
            type: 'TRADE_IN_UPDATE_SWITCHER_BY_ID',
            payload: {
                id: 'switcherId',
                params: {
                    isDisabled: true,
                    isActive: true,
                },
            },
        },
    ))
        .toEqual({
            switcherId: {
                isDisabled: true,
                isActive: true,
            },
            switcherId1: {
                isDisabled: false,
                isActive: false,
            },
        });
});

it('должен обновить isLoading, если action.payload = TRADE_IN_UPDATE_IS_LOADING', () => {
    expect(tradeInReducer(
        {
            isLoading: false,
        },
        {
            type: 'TRADE_IN_UPDATE_IS_LOADING',
            payload: true,
        },
    ))
        .toEqual({
            isLoading: true,
        });
});
