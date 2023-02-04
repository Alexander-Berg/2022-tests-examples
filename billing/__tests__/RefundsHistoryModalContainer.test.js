import React from 'react';
import Enzyme, { shallow, mount } from 'enzyme';

import Adapter from 'enzyme-adapter-react-16';
import { createStore, combineReducers, applyMiddleware, compose } from 'redux';
import { Provider } from 'react-redux';
import { all, take } from 'redux-saga/effects';
import createSagaMiddleware from 'redux-saga';
import SagaTester from 'redux-saga-tester';

import RefundHistoryModalContainer from '../RefundsHistoryModalContainer';

import commonReducers from 'common/reducers/common';
import reducers from '../../reducers';
import { watchFetchUnlockRefund } from '../../sagas/invoice-info';
import withIntlProvider from 'common/utils/test-utils/with-intl-provider';
import { fetchGet, fetchPost } from 'common/utils/old-fetch';
import { INVOICE_INFO as II } from '../../actions';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');

Enzyme.configure({ adapter: new Adapter() });

const HOST = 'http://snout-test';

describe('refund history modal', () => {
    beforeAll(initializeDesktopRegistry);

    describe('test refund history modal', () => {
        afterEach(() => {
            jest.resetAllMocks();
        });

        test('no unlock', async () => {
            expect.assertions(2);

            function* rootSaga() {
                yield all([watchFetchUnlockRefund()]);
            }

            const initialState = {
                invoicePage$invoice: {
                    currency: 'RUB',
                    oebs: {
                        payments1c: {
                            items: [
                                {
                                    cpfId: 1,
                                    oebsRefunds: {
                                        items: [
                                            {
                                                amount: '300.00',
                                                dt: '2019-02-10',
                                                id: 2,
                                                oebsPaymentNum: '36781',
                                                statusCode: 'successful',
                                                statusDescr: null,
                                                unlockAllowed: false
                                            }
                                        ]
                                    }
                                }
                            ]
                        }
                    }
                },
                invoicePage$invoiceInfo: {
                    shownCpfIdHistory: 1,
                    isFetchingOebsRefunds: false
                }
            };

            const sagaTester = new SagaTester({
                initialState,
                reducers: combineReducers({ ...commonReducers, ...reducers }),
                middlewares: []
            });

            sagaTester.start(rootSaga);

            let Container = () => (
                <Provider store={sagaTester.store}>
                    <RefundHistoryModalContainer />
                </Provider>
            );

            Container = withIntlProvider(Container);

            const wrapper = mount(<Container />);

            wrapper.find('button').simulate('click');

            await sagaTester.waitFor(II.HIDE_OEBS_REFUNDS_MODAL);

            expect(sagaTester.getState().invoicePage$invoiceInfo.shownCpfIdHistory).toBe(null);
            expect(
                sagaTester.getState().invoicePage$invoice.oebs.payments1c.items[0].oebsRefunds
            ).toEqual({});
        });

        test('with unlock', async () => {
            expect.assertions(12);

            function* rootSaga() {
                yield all([watchFetchUnlockRefund()]);
            }

            const initialState = {
                invoicePage$invoice: {
                    currency: 'RUB',
                    oebs: {
                        payments1c: {
                            items: [
                                {
                                    cpfId: 1,
                                    refundableAmount: '10.00',
                                    oebsRefunds: {
                                        items: [
                                            {
                                                amount: '300.00',
                                                dt: '2019-02-10',
                                                id: 2,
                                                oebsPaymentNum: '36781',
                                                statusCode: 'successful',
                                                statusDescr: null,
                                                unlockAllowed: true
                                            }
                                        ]
                                    },
                                    __orig: { refundableAmount: '10.00' }
                                }
                            ]
                        }
                    },
                    unusedFundsInInvoiceCurrency: '10.00'
                },
                invoicePage$invoiceInfo: {
                    shownCpfIdHistory: 1,
                    isFetchingOebsRefunds: false
                }
            };

            const sagaTester = new SagaTester({
                initialState,
                reducers: combineReducers({ ...commonReducers, ...reducers }),
                middlewares: []
            });

            sagaTester.start(rootSaga);

            let Container = () => (
                <Provider store={sagaTester.store}>
                    <RefundHistoryModalContainer />
                </Provider>
            );

            Container = withIntlProvider(Container);

            const wrapper = mount(<Container />);

            fetchPost.mockResolvedValueOnce({
                data: {
                    refundable_amount: '300.00',
                    status_code: 'failed',
                    status_descr: 'some text'
                }
            });

            wrapper.find('button').at(1).simulate('click');

            await sagaTester.waitFor(II.RECEIVE_UNLOCK_REFUND);

            expect(fetchPost).toBeCalledWith(
                `${HOST}/invoice/unlock-oebs-refund`,
                {
                    _csrf: 'csrf',
                    refund_id: 2
                },
                false
            );

            expect(sagaTester.getState().invoicePage$invoiceInfo.shownCpfIdHistory).toEqual(1);

            let invoice = sagaTester.getState().invoicePage$invoice;
            expect(invoice.unusedFundsInInvoiceCurrency).toEqual('300.00');

            let payment = invoice.oebs.payments1c.items[0];
            expect(payment.refundableAmount).toEqual('300.00');
            expect(payment.__orig.refundableAmount).toEqual('300.00');

            const refund = payment.oebsRefunds.items[0];
            expect(refund.statusCode).toEqual('failed');
            expect(refund.statusDescr).toEqual('some text');

            wrapper.find('button').at(0).simulate('click');

            await sagaTester.waitFor(II.HIDE_OEBS_REFUNDS_MODAL);

            expect(sagaTester.getState().invoicePage$invoiceInfo.shownCpfIdHistory).toBe(null);

            invoice = sagaTester.getState().invoicePage$invoice;
            expect(invoice.unusedFundsInInvoiceCurrency).toEqual('300.00');

            payment = invoice.oebs.payments1c.items[0];
            expect(payment.refundableAmount).toEqual('300.00');
            expect(payment.__orig.refundableAmount).toEqual('300.00');
            expect(invoice.oebs.payments1c.items[0].oebsRefunds).toEqual({});
        });

        test('with unlock, close before complete', async () => {
            expect.assertions(11);

            function* rootSaga() {
                yield all([watchFetchUnlockRefund()]);
            }

            const initialState = {
                invoicePage$invoice: {
                    currency: 'RUB',
                    oebs: {
                        payments1c: {
                            items: [
                                {
                                    cpfId: 1,
                                    refundableAmount: '10.00',
                                    oebsRefunds: {
                                        items: [
                                            {
                                                amount: '300.00',
                                                dt: '2019-02-10',
                                                id: 2,
                                                oebsPaymentNum: '36781',
                                                statusCode: 'successful',
                                                statusDescr: null,
                                                unlockAllowed: true
                                            }
                                        ]
                                    },
                                    __orig: { refundableAmount: '10.00' }
                                }
                            ]
                        }
                    },
                    unusedFundsInInvoiceCurrency: '10.00'
                },
                invoicePage$invoiceInfo: {
                    shownCpfIdHistory: 1,
                    isFetchingOebsRefunds: false
                }
            };

            const sagaTester = new SagaTester({
                initialState,
                reducers: combineReducers({ ...commonReducers, ...reducers }),
                middlewares: []
            });

            sagaTester.start(rootSaga);

            let Container = () => (
                <Provider store={sagaTester.store}>
                    <RefundHistoryModalContainer />
                </Provider>
            );

            Container = withIntlProvider(Container);

            const wrapper = mount(<Container />);

            fetchPost.mockResolvedValueOnce({
                data: {
                    refundable_amount: '300.00',
                    status_code: 'failed',
                    status_descr: 'some text'
                }
            });

            wrapper.find('button').at(1).simulate('click');

            wrapper.find('button').at(0).simulate('click');

            expect(sagaTester.getState().invoicePage$invoiceInfo.shownCpfIdHistory).toBe(null);

            let invoice = sagaTester.getState().invoicePage$invoice;
            expect(invoice.unusedFundsInInvoiceCurrency).toEqual('10.00');

            let payment = invoice.oebs.payments1c.items[0];
            expect(payment.refundableAmount).toEqual('10.00');
            expect(payment.__orig.refundableAmount).toEqual('10.00');

            expect(invoice.oebs.payments1c.items[0].oebsRefunds).toEqual({});

            await sagaTester.waitFor(II.RECEIVE_UNLOCK_REFUND);

            expect(fetchPost).toBeCalledWith(
                `${HOST}/invoice/unlock-oebs-refund`,
                {
                    _csrf: 'csrf',
                    refund_id: 2
                },
                false
            );

            expect(sagaTester.getState().invoicePage$invoiceInfo.shownCpfIdHistory).toEqual(null);

            invoice = sagaTester.getState().invoicePage$invoice;
            expect(invoice.unusedFundsInInvoiceCurrency).toEqual('300.00');

            payment = invoice.oebs.payments1c.items[0];
            expect(payment.refundableAmount).toEqual('300.00');
            expect(payment.__orig.refundableAmount).toEqual('300.00');
            expect(invoice.oebs.payments1c.items[0].oebsRefunds).toEqual({});
        });
    });
});
