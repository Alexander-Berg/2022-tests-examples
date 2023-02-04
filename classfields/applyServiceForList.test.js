/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('www-cabinet/react/dataDomain/sales/actions/applyService');

global.metrika = {
    reachGoal: jest.fn(),
};
const applyServiceForList = require('./applyServiceForList');
const mockStore = require('autoru-frontend/mocks/mockStore').default;

it('должен вызвать applyService 2 раза ' +
    'и вернуть корректный набор actions, ' +
    'если все услуги успешно применены', () => {
    const store = mockStore({});
    const applyService = require('www-cabinet/react/dataDomain/sales/actions/applyService');
    applyService.mockImplementation(() => () => 'SUCCESS');

    return store.dispatch(applyServiceForList({
        offerIDs: [ '15994654-b03da8e3', '16064346-fdba6bea' ],
        service: 'spec',
        isActivate: true,
        metrikaParams: [],
    }))
        .then(() => {
            expect(applyService).toHaveBeenCalledTimes(2);
            expect(store.getActions()).toEqual([
                {
                    type: 'APPLY_SALES_ACTION_PENDING',
                    payload: {
                        action: 'spec',
                    },
                },
                {
                    type: 'APPLY_SALES_ACTION_RESOLVED',
                    payload: {
                        action: 'spec',
                    },
                },
                {
                    type: 'NOTIFIER_SHOW_MESSAGE',
                    payload: {
                        message: 'Услуга успешно применена',
                        view: 'success',
                    },
                },
            ]);
        });
});

it('должен вызвать applyService 2 раза ' +
    'и вернуть корректный набор actions, ' +
    'если 1 из 2 услуг успешно применена', () => {
    const store = mockStore({});
    const applyService = require('www-cabinet/react/dataDomain/sales/actions/applyService');
    applyService.mockImplementation(() => ({ type: 'APPLY_SERVICE' }));
    applyService
        .mockImplementationOnce(() => () => 'SUCCESS')
        .mockImplementationOnce(() => () => 'FAIL');

    return store.dispatch(applyServiceForList({
        offerIDs: [ '15994654-b03da8e3', '16064346-fdba6bea' ],
        service: 'turbo',
        isActivate: false,
        metrikaParams: [],
    }))
        .then(() => {
            expect(applyService).toHaveBeenCalledTimes(2);
            expect(store.getActions()).toEqual([
                {
                    type: 'APPLY_SALES_ACTION_PENDING',
                    payload: {
                        action: 'turbo',
                    },
                },
                {
                    type: 'APPLY_SALES_ACTION_RESOLVED',
                    payload: {
                        action: 'turbo',
                    },
                },
                {
                    type: 'NOTIFIER_SHOW_MESSAGE',
                    payload: {
                        message: 'Услуга отменена для 1 из 2 объявлений',
                        view: 'success',
                    },
                },
            ]);
        });
});

it('должен вызвать applyService 2 раза ' +
    'вернуть корректный набор actions, ' +
    'если ни одна услуга не применилась', () => {
    const store = mockStore({});
    const applyService = require('www-cabinet/react/dataDomain/sales/actions/applyService');
    applyService.mockImplementation(() => () => 'FAILED');

    return store.dispatch(applyServiceForList({
        offerIDs: [ '15994654-b03da8e3', '16064346-fdba6bea' ],
        service: 'premium',
        isActivate: false,
        metrikaParams: [],
    }))
        .then(() => {
            expect(applyService).toHaveBeenCalledTimes(2);
            expect(store.getActions()).toEqual([
                {
                    type: 'APPLY_SALES_ACTION_PENDING',
                    payload: {
                        action: 'premium',
                    },
                },
                {
                    type: 'APPLY_SALES_ACTION_RESOLVED',
                    payload: {
                        action: 'premium',
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
