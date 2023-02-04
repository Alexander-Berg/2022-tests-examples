/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi');
jest.mock('www-cabinet/react/lib/getMetrika');

const updatePrice = require('./updatePrice');
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const state = {
    sales: {
        items: [
            {
                id: '1091257820',
                hash: '0e660c35',
                category: 'moto',
                price_info: {
                    RUR: 100,
                },
            },
        ],
    },
};

it('должен вызвать корректный набор actions, если изменилась цена и скидки', () => {
    const gateApi = require('auto-core/react/lib/gateApi');
    const store = mockStore(state);
    gateApi.getResource = jest.fn(() => Promise.resolve({
        price: { status: 'SUCCESS' },
        discount_options: { status: 'SUCCESS' },
    }));

    return store.dispatch(updatePrice({
        offerID: '1091257820-0e660c35',
        priceParams: {
            price: 100,
            currency: 'RUR',
            discount_options: {
                credit: 10,
                insurance: 0,
                max_discount: 30,
                tradein: 0,
            },
        },
    })).then(() => {
        expect(store.getActions()).toEqual([
            {
                type: 'CMEXPERT_WIDGET_UPDATE',
                payload: {
                    offerID: '1091257820-0e660c35',
                    isPriceChanging: true,
                    hasPriceChangingError: false,
                    priceChangingParams: {
                        price: 100,
                        discountOptions: {
                            credit: 10,
                            insurance: 0,
                            max_discount: 30,
                            tradein: 0,
                        },
                    },
                },
            },
            {
                type: 'UPDATE_PRICE',
                payload: {
                    offerID: '1091257820-0e660c35',
                    saleId: '1091257820',
                    price: 100,
                    currency: 'RUR',
                    discountOptions: {
                        credit: 10,
                        insurance: 0,
                        max_discount: 30,
                        tradein: 0,
                    },
                },
            },
            {
                type: 'NOTIFIER_SHOW_MESSAGE',
                payload: {
                    message: 'Цена и скидки успешно обновлены',
                    view: 'success',
                },
            },
            {
                type: 'CMEXPERT_WIDGET_UPDATE',
                payload: {
                    offerID: '1091257820-0e660c35',
                    isPriceChanging: false,

                },
            },
        ]);
    });
});

it('должен вызвать корректный набор actions, если изменилась только цена', () => {
    const gateApi = require('auto-core/react/lib/gateApi');
    const store = mockStore(state);
    gateApi.getResource = jest.fn(() => Promise.resolve({
        price: { status: 'SUCCESS' },
    }));

    return store.dispatch(updatePrice({
        offerID: '1091257820-0e660c35',
        priceParams: {
            price: 100,
            currency: 'RUR',
        },
    })).then(() => {
        expect(store.getActions()).toEqual([
            {
                type: 'CMEXPERT_WIDGET_UPDATE',
                payload: {
                    offerID: '1091257820-0e660c35',
                    isPriceChanging: true,
                    hasPriceChangingError: false,
                    priceChangingParams: {
                        price: 100,
                    },
                },
            },
            {
                type: 'UPDATE_PRICE',
                payload: {
                    offerID: '1091257820-0e660c35',
                    saleId: '1091257820',
                    price: 100,
                    currency: 'RUR',
                },
            },
            {
                type: 'NOTIFIER_SHOW_MESSAGE',
                payload: {
                    message: 'Цена успешно обновлена',
                    view: 'success',
                },
            },
            {
                type: 'CMEXPERT_WIDGET_UPDATE',
                payload: {
                    offerID: '1091257820-0e660c35',
                    isPriceChanging: false,
                },
            },
        ]);
    });
});

it('должен вызвать корректный набор actions, если изменились только скидки', () => {
    const gateApi = require('auto-core/react/lib/gateApi');
    const store = mockStore(state);
    gateApi.getResource = jest.fn(() => Promise.resolve({
        discount_options: { status: 'SUCCESS' },
    }));

    return store.dispatch(updatePrice({
        offerID: '1091257820-0e660c35',
        priceParams: {
            discount_options: {
                credit: 10,
                insurance: 0,
                max_discount: 30,
                tradein: 0,
            },
        },
    })).then(() => {
        expect(store.getActions()).toEqual([
            {
                type: 'CMEXPERT_WIDGET_UPDATE',
                payload: {
                    offerID: '1091257820-0e660c35',
                    isPriceChanging: true,
                    hasPriceChangingError: false,
                    priceChangingParams: {
                        price: 100,
                        discountOptions: {
                            credit: 10,
                            insurance: 0,
                            max_discount: 30,
                            tradein: 0,
                        },
                    },
                },
            },
            {
                type: 'UPDATE_PRICE',
                payload: {
                    offerID: '1091257820-0e660c35',
                    saleId: '1091257820',
                    discountOptions: {
                        credit: 10,
                        insurance: 0,
                        max_discount: 30,
                        tradein: 0,
                    },
                },
            },
            {
                type: 'NOTIFIER_SHOW_MESSAGE',
                payload: {
                    message: 'Скидки успешно обновлены',
                    view: 'success',
                },
            },
            {
                type: 'CMEXPERT_WIDGET_UPDATE',
                payload: {
                    offerID: '1091257820-0e660c35',
                    isPriceChanging: false,
                },
            },
        ]);
    });
});

it('должен вызвать корректный набор actions, если что-то пошло не так', () => {
    const gateApi = require('auto-core/react/lib/gateApi');
    const store = mockStore(state);
    gateApi.getResource = jest.fn(() => Promise.reject({
    }));

    return store.dispatch(updatePrice({
        offerID: '1091257820-0e660c35',
        priceParams: {
            discount_options: {
                credit: 10,
                insurance: 0,
                max_discount: 30,
                tradein: 0,
            },
        },
    })).then(() => {
        expect(store.getActions()).toEqual([
            {
                type: 'CMEXPERT_WIDGET_UPDATE',
                payload: {
                    offerID: '1091257820-0e660c35',
                    isPriceChanging: true,
                    hasPriceChangingError: false,
                    priceChangingParams: {
                        price: 100,
                        discountOptions: {
                            credit: 10,
                            insurance: 0,
                            max_discount: 30,
                            tradein: 0,
                        },
                    },
                },
            },
            {
                type: 'CMEXPERT_WIDGET_UPDATE',
                payload: {
                    offerID: '1091257820-0e660c35',
                    isPriceChanging: false,
                    hasPriceChangingError: true,
                },
            },
            {
                type: 'NOTIFIER_SHOW_MESSAGE',
                payload: {
                    message: 'Произошла ошибка, попробуйте ещё раз',
                    view: 'error',
                },
            },
        ]);
    });
});
