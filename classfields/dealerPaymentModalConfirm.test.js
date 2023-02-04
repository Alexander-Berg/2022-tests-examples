/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/proofOfWork');
jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResourcePublicApi: jest.fn(),
    };
});

const getResourcePublicApi = require('auto-core/react/lib/gateApi').getResourcePublicApi;

const dealerPaymentModalConfirm = require('./dealerPaymentModalConfirm');

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const configStateMock = require('auto-core/react/dataDomain/config/mock').default;

let store;

beforeEach(() => {
    store = mockStore({
        config: configStateMock.value(),
    });
});

it('правильно вызывает getRichVinReport', () => {
    const gateApiPromise = Promise.resolve({});
    getResourcePublicApi.mockImplementation(() => gateApiPromise);

    store.dispatch(dealerPaymentModalConfirm('{"foo":"bar"}', '{"foo":"bar"}'));

    return Promise.resolve().then(() => {
        expect(getResourcePublicApi).toHaveBeenCalledWith('getRichVinReport', {
            decrement_quota: true,
            foo: 'bar',
            isCardPage: false,
            pow: {
                client_timestamp: 1,
                hash: 'hash',
                payload: undefined,
                time: 2,
                timestamp: 3,
            },
        });
    });
});

it('показывает успех, если пришел оплаченный отчет', () => {
    return new Promise((done) => {
        const gateApiPromise = Promise.resolve({ report: { report_type: 'PAID_REPORT' } });
        getResourcePublicApi.mockImplementation(() => gateApiPromise);

        store.dispatch(dealerPaymentModalConfirm('{"foo":"bar"}', '{"foo":"bar"}'));
        setTimeout(() => {
            expect(store.getActions()).toEqual([
                {
                    payload: { paymentParams: { foo: 'bar' }, reportParams: { decrement_quota: true, foo: 'bar', isCardPage: false } },
                    type: 'VIN_REPORT_FETCHING',
                },
                { payload: { report: { report_type: 'PAID_REPORT' } }, type: 'VIN_REPORT_RESOLVED' },
                { type: 'CLOSE_DEALER_PAYMENT_MODAL' },
                { payload: { message: 'Отчёт успешно куплен', view: 'success' }, type: 'NOTIFIER_SHOW_MESSAGE' },
            ]);
            done();
        }, 100);
    });
});

it('показывает неуспех оплаты, если пришел неоплаченный отчет', () => {
    return new Promise((done) => {
        const gateApiPromise = Promise.resolve({ report: { report_type: 'FREE_REPORT' } });
        getResourcePublicApi.mockImplementation(() => gateApiPromise);

        store.dispatch(dealerPaymentModalConfirm('{"foo":"bar"}', '{"foo":"bar"}'));
        setTimeout(() => {
            expect(store.getActions()).toEqual([
                {
                    payload: { paymentParams: { foo: 'bar' }, reportParams: { decrement_quota: true, foo: 'bar', isCardPage: false } },
                    type: 'VIN_REPORT_FETCHING',
                },
                { payload: { report: { report_type: 'FREE_REPORT' } }, type: 'VIN_REPORT_RESOLVED' },
                { type: 'CLOSE_DEALER_PAYMENT_MODAL' },
                { payload: { message: 'Отчёт не удалось оплатить', view: 'error' }, type: 'NOTIFIER_SHOW_MESSAGE' },
            ]);
            done();
        }, 100);
    });
});

it('показывает неуспех, если отчет не ответил', () => {
    return new Promise((done) => {
        const gateApiPromise = Promise.reject();
        getResourcePublicApi.mockImplementation(() => gateApiPromise);

        store.dispatch(dealerPaymentModalConfirm('{"foo":"bar"}', '{"foo":"bar"}'));
        setTimeout(() => {
            expect(store.getActions()).toEqual([
                {
                    payload: { paymentParams: { foo: 'bar' }, reportParams: { decrement_quota: true, foo: 'bar', isCardPage: false } },
                    type: 'VIN_REPORT_FETCHING',
                },
                { payload: 'UNKNOWN_ERROR', type: 'VIN_REPORT_REJECTED' },
                { type: 'CLOSE_DEALER_PAYMENT_MODAL' },
                { payload: { message: 'Произошла ошибка', view: 'error' }, type: 'NOTIFIER_SHOW_MESSAGE' },
            ]);
            done();
        }, 100);
    });
});
