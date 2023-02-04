/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const _ = require('lodash');
const changeProductState = require('./changeProductState');
const gateApi = require('auto-core/react/lib/gateApi');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const matchApplicationsMock = require('www-cabinet/react/dataDomain/matchApplications/mocks/withApplications.mock');

beforeEach(() => {
    gateApi.getResource.mockImplementation(() => Promise.resolve());
});

it('должен обновлять в сторе состояние продукта на активное', () => {
    const store = mockStore({
        matchApplications: matchApplicationsMock,
    });

    store.dispatch(
        changeProductState('alias'),
    );

    expect(store.getActions()[0].payload.isActive).toBe(true);
});

it('должен вызвать getResource с методом активации продукта, если он был выключен', () => {
    const store = mockStore({
        matchApplications: matchApplicationsMock,
    });

    store.dispatch(
        changeProductState('alias'),
    );

    expect(gateApi.getResource).toHaveBeenCalledWith('putDealerCampaignProduct', { dealer_id: undefined, product: 'alias' });
});

it('должен вызвать getResource с методом отключения продукта, если он был включен', () => {
    const mockClone = _.cloneDeep(matchApplicationsMock);

    mockClone.dealerCampaignProducts.items[0].isActive = true;

    const store = mockStore({
        matchApplications: mockClone,
    });

    store.dispatch(
        changeProductState('alias'),
    );

    expect(gateApi.getResource).toHaveBeenCalledWith('deleteDealerCampaignProduct', { dealer_id: undefined, product: 'alias' });
});

it('должен сделать роллбэк, если ручка ответила с ошибкой', () => {
    expect.assertions(1);
    const store = mockStore({
        matchApplications: matchApplicationsMock,
    });

    gateApi.getResource.mockImplementation(() => Promise.reject());

    return store.dispatch(
        changeProductState('alias'),
    ).then(() => {
        const lastAction = store.getActions()[2];

        expect(lastAction.payload.isActive).toBe(false);
    });
});

it('должен вызвать экшен нотификации об ошибке, если ручка ответила с ошибкой', () => {
    expect.assertions(1);
    const store = mockStore({
        matchApplications: matchApplicationsMock,
    });

    gateApi.getResource.mockImplementation(() => Promise.reject());

    return store.dispatch(
        changeProductState('alias'),
    ).then(() => {
        expect(store.getActions()).toContainEqual({
            type: 'NOTIFIER_SHOW_MESSAGE',
            payload: {
                message: 'Произошла ошибка, попробуйте ещё раз',
                view: 'error',
            },
        });
    });
});
