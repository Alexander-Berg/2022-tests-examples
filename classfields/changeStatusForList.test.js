/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi');
jest.mock('www-cabinet/react/dataDomain/sales/actions/changeStatus');
jest.mock('www-cabinet/react/lib/listing/sendMetrika');

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const changeStatusForList = require('./changeStatusForList');

it('должен вызвать changeStatus 2 раза ' +
    'вернуть корректный набор actions, ' +
    'если все статусы успешно обновлены', () => {
    const store = mockStore({});
    const changeStatus = require('www-cabinet/react/dataDomain/sales/actions/changeStatus');
    changeStatus.mockImplementation(() => () => {
        return 'SUCCESS';
    });

    return store.dispatch(changeStatusForList({
        offerIDs: [ '3137946-45138483', '3138046-229fd63a' ],
        status: 'active',
        metrikaParams: [],
    }))
        .then(() => {
            expect(changeStatus).toHaveBeenCalledTimes(2);
            expect(store.getActions()).toEqual([
                {
                    type: 'APPLY_SALES_ACTION_PENDING',
                    payload: {
                        action: 'active',
                    },
                },
                {
                    type: 'APPLY_SALES_ACTION_RESOLVED',
                    payload: {
                        action: 'active',
                    },
                },
                {
                    type: 'NOTIFIER_SHOW_MESSAGE',
                    payload: {
                        message: 'Объявления активированы',
                        view: 'success',
                    },
                },
            ]);
        });
});

it('должен вызвать changeStatus 2 раза ' +
    'вернуть корректный набор actions, ' +
    'если не все статусы вернули SUCCESS', () => {
    const store = mockStore({});
    const changeStatus = require('www-cabinet/react/dataDomain/sales/actions/changeStatus');
    changeStatus.mockImplementation(() => ({ type: 'CHANGE_STATUS' }));
    changeStatus
        .mockImplementationOnce(() => () => 'SUCCESS')
        .mockImplementationOnce(() => () => 'FAILED');

    return store.dispatch(changeStatusForList({
        offerIDs: [ '3137946-45138483', '3138046-229fd63a' ],
        status: 'inactive',
        metrikaParams: [],
    }))
        .then(() => {
            expect(changeStatus).toHaveBeenCalledTimes(2);
            expect(store.getActions()).toEqual([
                {
                    type: 'APPLY_SALES_ACTION_PENDING',
                    payload: {
                        action: 'inactive',
                    },
                },
                {
                    type: 'APPLY_SALES_ACTION_RESOLVED',
                    payload: {
                        action: 'inactive',
                    },
                },
                {
                    type: 'NOTIFIER_SHOW_MESSAGE',
                    payload: {
                        message: 'Сняты с продажи 1 из 2 объявлений',
                        view: 'success',
                    },
                },
            ]);
        });
});

it('должен вызвать changeStatus 2 раза ' +
    'вернуть корректный набор actions, ' +
    'если все статусы вернули FAIL', () => {
    const store = mockStore({});
    const changeStatus = require('www-cabinet/react/dataDomain/sales/actions/changeStatus');
    changeStatus.mockImplementation(() => () => {
        return 'FAILED';
    });

    return store.dispatch(
        changeStatusForList({
            offerIDs: [ '3137946-45138483', '3138046-229fd63a' ],
            status: 'removed',
            metrikaParams: [],
        }))
        .then(() => {
            expect(changeStatus).toHaveBeenCalledTimes(2);
            expect(store.getActions()).toEqual([
                {
                    type: 'APPLY_SALES_ACTION_PENDING',
                    payload: {
                        action: 'removed',
                    },
                },
                {
                    type: 'APPLY_SALES_ACTION_RESOLVED',
                    payload: {
                        action: 'removed',
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
