/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const resetSaleServiceSchedule = require('./cancelSchedule');

const state = {
    config: {
        customerRole: 'manager',
        client: {
            id: 'dealer_id',
        },
    },
    sales: {
        items: [
            {
                id: '1091189848',
                hash: '21914653',
                category: 'cars',
            },
        ],
    },
};

it('должен вызвать gateApi.getResource с корретными параметрами и вернуть корректный набор actions, ' +
    'если .response.status === SUCCESS', () => {
    const gateApi = require('auto-core/react/lib/gateApi');
    gateApi.getResource = jest.fn(() => Promise.resolve({
        status: 'SUCCESS',
    }));

    const store = mockStore(state);

    return store.dispatch(resetSaleServiceSchedule({
        offerID: '1091189848-21914653',
    }))
        .then(() => {
            expect(gateApi.getResource).toHaveBeenCalledWith('deleteBillingSchedules', {
                offerId: '1091189848-21914653',
                product: 'all_sale_fresh',
                dealer_id: 'dealer_id',
                category: 'cars',
            });

            expect(store.getActions()).toEqual([
                {
                    type: 'RESETTED_SERVICE_SCHEDULE',
                    payload: {
                        saleId: '1091189848',
                        service: 'all_sale_fresh',
                    },
                },
                {
                    type: 'NOTIFIER_SHOW_MESSAGE',
                    payload: {
                        message: 'Автоприменение услуги «Поднятие в поиске» отключено',
                        view: 'success',
                    },
                },
            ]);
        });
});

it('должен вызвать gateApi.getResource и вернуть корректный набор actions, ' +
    'если .response.status === FAILED', () => {
    const gateApi = require('auto-core/react/lib/gateApi');
    gateApi.getResource = jest.fn(() => Promise.resolve({
        response: {
            status: 'FAILED',
        },
    }));

    const store = mockStore(state);

    return store.dispatch(resetSaleServiceSchedule({
        offerID: '1091189848-21914653',
    }))
        .then(() => {
            expect(gateApi.getResource).toHaveBeenCalled();
            expect(store.getActions()).toEqual([
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

it('должен вызвать gateApi.getResource и вернуть корректный набор actions, ' +
    'если gateApi.getResource вернул Promise.reject', () => {
    const gateApi = require('auto-core/react/lib/gateApi');
    gateApi.getResource = jest.fn(() => Promise.reject());

    const store = mockStore(state);

    return store.dispatch(resetSaleServiceSchedule({
        offerID: '1091189848-21914653',
    }))
        .then(() => {
            expect(gateApi.getResource).toHaveBeenCalled();

            expect(store.getActions()).toEqual([
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
