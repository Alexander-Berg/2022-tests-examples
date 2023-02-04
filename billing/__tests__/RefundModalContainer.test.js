import React from 'react';
import Enzyme, { shallow, mount } from 'enzyme';

import Adapter from 'enzyme-adapter-react-16';
import { createStore, combineReducers, applyMiddleware, compose } from 'redux';
import { Provider } from 'react-redux';
import { all, take } from 'redux-saga/effects';
import createSagaMiddleware from 'redux-saga';
import SagaTester from 'redux-saga-tester';

import RefundModalContainer from '../RefundModalContainer';

import commonReducers from 'common/reducers/common';
import reducers from '../../reducers';
import { watchRequestCreateOebsRefund } from '../../sagas/invoice-info';
import withIntlProvider from 'common/utils/test-utils/with-intl-provider';
import { fetchGet, fetchPost } from 'common/utils/old-fetch';
import { INVOICE_INFO as II } from '../../actions';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');

Enzyme.configure({ adapter: new Adapter() });

const HOST = 'http://snout-test';

describe('refund modal', () => {
    beforeAll(initializeDesktopRegistry);

    describe('test refund modal', () => {
        afterEach(() => {
            jest.resetAllMocks();
        });

        test('simple close', async () => {
            expect.assertions(1);

            function* rootSaga() {
                yield all([watchRequestCreateOebsRefund()]);
            }

            const initialState = {
                invoicePage$invoice: {
                    currency: 'RUB',
                    oebs: {
                        payments1c: {
                            items: [
                                {
                                    cpfId: 1,
                                    refundRequisites: {
                                        trasactionNum: {
                                            isValid: false,
                                            val: ''
                                        },
                                        walletNum: {
                                            isValid: false,
                                            val: ''
                                        }
                                    },
                                    editableRefundRequisites: ['walletNum', 'transactionNum'],
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
                                    },
                                    __orig: {
                                        refundableAmount: '300.00',
                                        refundRequisites: {
                                            trasactionNum: {
                                                isValid: false,
                                                val: ''
                                            },
                                            walletNum: {
                                                isValid: false,
                                                val: ''
                                            }
                                        }
                                    }
                                }
                            ]
                        }
                    }
                },
                invoicePage$invoiceInfo: {
                    shownCpfId: 1,
                    isFetchingRefund: false
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
                    <RefundModalContainer />
                </Provider>
            );

            Container = withIntlProvider(Container);

            const wrapper = mount(<Container />);

            wrapper.find('button').at(0).simulate('click');

            await sagaTester.waitFor(II.HIDE_REFUND_MODAL);

            expect(sagaTester.getState().invoicePage$invoiceInfo.shownCpfId).toBe(null);
        });

        test('transaction number and wallet', async () => {
            expect.assertions(7);

            function* rootSaga() {
                yield all([watchRequestCreateOebsRefund()]);
            }

            const initialState = {
                invoicePage$invoice: {
                    currency: 'RUB',
                    oebs: {
                        payments1c: {
                            items: [
                                {
                                    cpfId: 1,
                                    refundableAmount: '300.00',
                                    refundRequisites: {
                                        transactionNum: {
                                            isValid: false,
                                            val: ''
                                        },
                                        walletNum: {
                                            isValid: false,
                                            val: ''
                                        }
                                    },
                                    editableRefundRequisites: ['walletNum', 'transactionNum'],
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
                                    },
                                    __orig: {
                                        refundableAmount: '300.00',
                                        refundRequisites: {
                                            transactionNum: {
                                                isValid: false,
                                                val: ''
                                            },
                                            walletNum: {
                                                isValid: false,
                                                val: ''
                                            }
                                        }
                                    }
                                }
                            ]
                        }
                    }
                },
                invoicePage$invoiceInfo: {
                    shownCpfId: 1,
                    isFetchingRefund: false
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
                    <RefundModalContainer />
                </Provider>
            );

            Container = withIntlProvider(Container);

            const wrapper = mount(<Container />);

            let event = {
                target: { name: '', value: '12' }
            };

            wrapper.find('input').at(1).simulate('change', event);

            expect(
                sagaTester.getState().invoicePage$invoice.oebs.payments1c.items[0].refundRequisites
                    .transactionNum
            ).toEqual({
                isValid: true,
                val: '12',
                message: null
            });

            event = {
                target: { name: '', value: '123456789012' }
            };

            wrapper.find('input').at(2).simulate('change', event);

            expect(
                sagaTester.getState().invoicePage$invoice.oebs.payments1c.items[0].refundRequisites
                    .walletNum
            ).toEqual({
                isValid: true,
                val: '123456789012',
                message: null
            });

            fetchPost.mockResolvedValueOnce({
                data: {
                    refundable_amount: '0.00',
                    refunds_num: 8
                }
            });

            wrapper.find('form').simulate('submit');

            await sagaTester.waitFor(II.RECEIVE_CREATE_OEBS_REFUND);

            expect(fetchPost).toBeCalledWith(
                `${HOST}/invoice/create-oebs-refund`,
                {
                    _csrf: 'csrf',
                    amount: '300.00',
                    cpf_id: 1,
                    refundable_amount: '300.00',
                    transaction_num: '12',
                    wallet_num: '123456789012'
                },
                false
            );

            const payment = sagaTester.getState().invoicePage$invoice.oebs.payments1c.items[0];

            expect(payment.refundableAmount).toEqual('0.00');
            expect(payment.__orig.refundableAmount).toEqual('0.00');
            expect(payment.__orig.refundsNum).toEqual(8);

            await sagaTester.waitFor(II.HIDE_REFUND_MODAL);

            expect(sagaTester.getState().invoicePage$invoiceInfo.shownCpfId).toBe(null);
        });

        test('transaction number only', async () => {
            expect.assertions(6);

            function* rootSaga() {
                yield all([watchRequestCreateOebsRefund()]);
            }

            const initialState = {
                invoicePage$invoice: {
                    currency: 'RUB',
                    oebs: {
                        payments1c: {
                            items: [
                                {
                                    cpfId: 1,
                                    refundableAmount: '300.00',
                                    refundRequisites: {
                                        transactionNum: {
                                            isValid: false,
                                            val: ''
                                        }
                                    },
                                    editableRefundRequisites: ['transactionNum'],
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
                                    },
                                    __orig: {
                                        refundableAmount: '300.00',
                                        refundRequisites: {
                                            transactionNum: {
                                                isValid: false,
                                                val: ''
                                            },
                                            walletNum: {
                                                isValid: false,
                                                val: ''
                                            }
                                        }
                                    }
                                }
                            ]
                        }
                    }
                },
                invoicePage$invoiceInfo: {
                    shownCpfId: 1,
                    isFetchingRefund: false
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
                    <RefundModalContainer />
                </Provider>
            );

            Container = withIntlProvider(Container);

            const wrapper = mount(<Container />);

            const event = {
                target: { name: '', value: '12' }
            };

            wrapper.find('input').at(1).simulate('change', event);

            expect(
                sagaTester.getState().invoicePage$invoice.oebs.payments1c.items[0].refundRequisites
                    .transactionNum
            ).toEqual({
                isValid: true,
                val: '12',
                message: null
            });

            fetchPost.mockResolvedValueOnce({
                data: {
                    refundable_amount: '0.00',
                    refunds_num: 8
                }
            });

            wrapper.find('form').simulate('submit');

            await sagaTester.waitFor(II.RECEIVE_CREATE_OEBS_REFUND);

            expect(fetchPost).toBeCalledWith(
                `${HOST}/invoice/create-oebs-refund`,
                {
                    _csrf: 'csrf',
                    amount: '300.00',
                    cpf_id: 1,
                    refundable_amount: '300.00',
                    transaction_num: '12'
                },
                false
            );

            const payment = sagaTester.getState().invoicePage$invoice.oebs.payments1c.items[0];

            expect(payment.refundableAmount).toEqual('0.00');
            expect(payment.__orig.refundableAmount).toEqual('0.00');
            expect(payment.__orig.refundsNum).toEqual(8);

            await sagaTester.waitFor(II.HIDE_REFUND_MODAL);

            expect(sagaTester.getState().invoicePage$invoiceInfo.shownCpfId).toBe(null);
        });

        test('not editable', async () => {
            expect.assertions(5);

            function* rootSaga() {
                yield all([watchRequestCreateOebsRefund()]);
            }

            const initialState = {
                invoicePage$invoice: {
                    currency: 'RUB',
                    oebs: {
                        payments1c: {
                            items: [
                                {
                                    cpfId: 1,
                                    refundableAmount: '300.00',
                                    refundRequisites: {
                                        transactionNum: {
                                            isValid: true,
                                            val: '12'
                                        },
                                        walletNum: {
                                            isValid: true,
                                            val: '123456789012'
                                        }
                                    },
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
                                    },
                                    __orig: {
                                        refundableAmount: '300.00',
                                        refundRequisites: {
                                            transactionNum: {
                                                isValid: false,
                                                val: ''
                                            },
                                            walletNum: {
                                                isValid: false,
                                                val: ''
                                            }
                                        }
                                    }
                                }
                            ]
                        }
                    }
                },
                invoicePage$invoiceInfo: {
                    shownCpfId: 1,
                    isFetchingRefund: false
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
                    <RefundModalContainer />
                </Provider>
            );

            Container = withIntlProvider(Container);

            const wrapper = mount(<Container />);

            fetchPost.mockResolvedValueOnce({
                data: {
                    refundable_amount: '0.00',
                    refunds_num: 8
                }
            });

            wrapper.find('form').simulate('submit');

            await sagaTester.waitFor(II.RECEIVE_CREATE_OEBS_REFUND);

            expect(fetchPost).toBeCalledWith(
                `${HOST}/invoice/create-oebs-refund`,
                {
                    _csrf: 'csrf',
                    amount: '300.00',
                    cpf_id: 1,
                    refundable_amount: '300.00',
                    transaction_num: '12',
                    wallet_num: '123456789012'
                },
                false
            );

            const payment = sagaTester.getState().invoicePage$invoice.oebs.payments1c.items[0];

            expect(payment.refundableAmount).toEqual('0.00');
            expect(payment.__orig.refundableAmount).toEqual('0.00');
            expect(payment.__orig.refundsNum).toEqual(8);

            await sagaTester.waitFor(II.HIDE_REFUND_MODAL);

            expect(sagaTester.getState().invoicePage$invoiceInfo.shownCpfId).toBe(null);
        });

        test('with bank', async () => {
            expect.assertions(5);

            function* rootSaga() {
                yield all([watchRequestCreateOebsRefund()]);
            }

            const initialState = {
                invoicePage$invoice: {
                    currency: 'RUB',
                    oebs: {
                        payments1c: {
                            items: [
                                {
                                    cpfId: 1,
                                    refundableAmount: '300.00',
                                    refundRequisites: {
                                        account: {
                                            isValid: true,
                                            val: '40702810135463172116'
                                        },
                                        bik: {
                                            isValid: true,
                                            val: '044525440'
                                        },
                                        customerName: {
                                            isValid: true,
                                            val: 'Медведева Анисим Георгиевна'
                                        },
                                        inn: {
                                            isValid: true,
                                            val: '7801875896'
                                        }
                                    },
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
                                    },
                                    __orig: {
                                        refundableAmount: '300.00',
                                        refundRequisites: {
                                            transactionNum: {
                                                isValid: false,
                                                val: ''
                                            },
                                            walletNum: {
                                                isValid: false,
                                                val: ''
                                            }
                                        }
                                    }
                                }
                            ]
                        }
                    }
                },
                invoicePage$invoiceInfo: {
                    shownCpfId: 1,
                    isFetchingRefund: false
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
                    <RefundModalContainer />
                </Provider>
            );

            Container = withIntlProvider(Container);

            const wrapper = mount(<Container />);

            fetchPost.mockResolvedValueOnce({
                data: {
                    refundable_amount: '0.00',
                    refunds_num: 8
                }
            });

            wrapper.find('form').simulate('submit');

            await sagaTester.waitFor(II.RECEIVE_CREATE_OEBS_REFUND);

            expect(fetchPost).toBeCalledWith(
                `${HOST}/invoice/create-oebs-refund`,
                {
                    _csrf: 'csrf',
                    amount: '300.00',
                    cpf_id: 1,
                    refundable_amount: '300.00'
                },
                false
            );

            const payment = sagaTester.getState().invoicePage$invoice.oebs.payments1c.items[0];

            expect(payment.refundableAmount).toEqual('0.00');
            expect(payment.__orig.refundableAmount).toEqual('0.00');
            expect(payment.__orig.refundsNum).toEqual(8);

            await sagaTester.waitFor(II.HIDE_REFUND_MODAL);

            expect(sagaTester.getState().invoicePage$invoiceInfo.shownCpfId).toBe(null);
        });

        test('error 422', async () => {
            expect.assertions(2);

            function* rootSaga() {
                yield all([watchRequestCreateOebsRefund()]);
            }

            const initialState = {
                invoicePage$invoice: {
                    currency: 'RUB',
                    oebs: {
                        payments1c: {
                            items: [
                                {
                                    cpfId: 1,
                                    refundableAmount: '300.00',
                                    refundRequisites: {
                                        transactionNum: {
                                            isValid: true,
                                            val: '12'
                                        },
                                        walletNum: {
                                            isValid: true,
                                            val: '123456789012'
                                        }
                                    },
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
                                    },
                                    __orig: {
                                        refundableAmount: '300.00',
                                        refundRequisites: {
                                            transactionNum: {
                                                isValid: false,
                                                val: ''
                                            },
                                            walletNum: {
                                                isValid: false,
                                                val: ''
                                            }
                                        }
                                    }
                                }
                            ]
                        }
                    }
                },
                invoicePage$invoiceInfo: {
                    shownCpfId: 1,
                    isFetchingRefund: false
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
                    <RefundModalContainer />
                </Provider>
            );

            Container = withIntlProvider(Container);

            const wrapper = mount(<Container />);

            class Error422 extends Error {
                constructor() {
                    super();
                    this.status = 422;
                    this.data = {
                        description: '300.00',
                        error: new Error()
                    };
                }
            }

            fetchPost.mockRejectedValueOnce(new Error422());

            wrapper.find('form').simulate('submit');

            await sagaTester.waitFor(II.RECEIVE_CREATE_OEBS_REFUND);

            expect(fetchPost).toBeCalledWith(
                `${HOST}/invoice/create-oebs-refund`,
                {
                    _csrf: 'csrf',
                    amount: '300.00',
                    cpf_id: 1,
                    refundable_amount: '300.00',
                    transaction_num: '12',
                    wallet_num: '123456789012'
                },
                false
            );

            const payment = sagaTester.getState().invoicePage$invoice.oebs.payments1c.items[0];

            expect(payment.refundErrors).toEqual({
                refundableAmount: '300.00'
            });
        });
    });
});
