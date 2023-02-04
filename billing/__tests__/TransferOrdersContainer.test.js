import React from 'react';
import Enzyme, { shallow, mount } from 'enzyme';

import Adapter from 'enzyme-adapter-react-16';
import { createStore, combineReducers, applyMiddleware, compose } from 'redux';
import { Provider } from 'react-redux';
import { all, take } from 'redux-saga/effects';
import createSagaMiddleware from 'redux-saga';
import SagaTester from 'redux-saga-tester';

import 'common/utils/numeral';
import { Permissions } from 'common/constants';
import TransferOrdersContainer from '../TransferOrdersContainer';

import commonReducers from 'common/reducers/common';
import reducers from '../../reducers';
import {
    watchCheckArbitraryOrder,
    watchFetchOrders,
    watchUnusedFundsLock,
    watchTransfer
} from '../../sagas/transfer-orders';
import withIntlProvider from 'common/utils/test-utils/with-intl-provider';
import { fetchGet, fetchPost } from 'common/utils/old-fetch';
import { TRANSFER_ORDERS as TO } from '../../actions';
import { MessagesActions, acceptModalMessage } from 'common/actions/messages';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');

Enzyme.configure({ adapter: new Adapter() });

const loggerMiddleware = store => next => action => {
    const { type } = action;
    let direction = '';
    if (type.indexOf('REQUEST') > -1) {
        direction = '-> ';
    } else if (type.indexOf('RECEIVE') > -1) {
        direction = '<- ';
    }
    console.debug(`${direction}${action.type}`);
    const result = next(action);

    return result;
};

const HOST = 'http://snout-test';

describe('transfer orders container', () => {
    beforeAll(initializeDesktopRegistry);

    describe('transfer to selected order', () => {
        afterEach(() => {
            jest.resetAllMocks();
        });

        test('test initial orders load', async () => {
            expect.assertions(1);

            function* rootSaga() {
                yield all([watchFetchOrders()]);
            }

            const initialState = {
                perms: [Permissions.TRANSFER_FROM_INVOICE, Permissions.TRANSFER_BETWEEN_CLIENTS],
                invoicePage$invoice: {
                    credit: '0',
                    receiptSum: 1000,
                    consumeSum: 10,
                    invoicePerms: [Permissions.TRANSFER_FROM_INVOICE],

                    client: {
                        id: '1000'
                    },
                    paysys: {
                        currency: 'RUR'
                    }
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
                    <TransferOrdersContainer />
                </Provider>
            );

            Container = withIntlProvider(Container);

            fetchGet.mockResolvedValueOnce({ data: [] });

            const wrapper = mount(<Container />);

            await sagaTester.waitFor(TO.RECEIVE_ORDERS);

            expect(fetchGet).toBeCalledWith(
                `${HOST}/order/withdraw/from-orders`,
                {
                    client_id: '1000',
                    order_id: undefined,
                    service_id: undefined,
                    service_order_id_prefix: undefined
                },
                false,
                false
            );
        });

        test('test transfer to arbitrary order', async () => {
            expect.assertions(2);

            // sagas
            function* rootSaga() {
                yield all([watchCheckArbitraryOrder(), watchTransfer()]);
            }

            const orders = [
                {
                    value: '1-12345',
                    content: '1-12345'
                },
                {
                    value: '2-34566',
                    content: '2-34566'
                }
            ];

            const initialState = {
                perms: [Permissions.TRANSFER_FROM_INVOICE, Permissions.TRANSFER_BETWEEN_CLIENTS],
                invoicePage$invoice: {
                    id: '100',
                    credit: '0',
                    receiptSum: 1000,
                    consumeSum: 10,
                    client: {
                        id: '1000'
                    },
                    paysys: {
                        currency: 'RUR'
                    },
                    unusedFundsInInvoiceCurrency: 990,
                    invoicePerms: [Permissions.TRANSFER_FROM_INVOICE]
                },
                invoicePage$transferOrders: {
                    isFetching: false,
                    unusedFundsLockFetching: false,
                    order: orders[0],
                    ordersOptions: orders,
                    strArbitraryOrder: '3-4567',
                    canSetUnusedFundsLock: false,
                    unusedFunds: {
                        val: 'TRANSFER',
                        value: 'TRANSFER',
                        code: '1',
                        text: 'Взаимозачет',
                        content: 'Взаимозачет'
                    },
                    strAmount: '',
                    strDiscount: '',
                    invalidAmount: false,
                    invalidDiscount: false,
                    mode: 'ALL'
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
                    <TransferOrdersContainer />
                </Provider>
            );

            Container = withIntlProvider(Container);

            fetchGet.mockResolvedValueOnce({ data: { result: 'ok' } });
            fetchPost.mockResolvedValueOnce({ data: {} });

            const wrapper = mount(<Container />);

            wrapper.find('form').simulate('submit');

            await sagaTester.waitFor(MessagesActions.SHOW_CONFIRMATION_MESSAGE);
            await sagaTester.dispatch(acceptModalMessage());

            await sagaTester.waitFor(TO.RECEIVE);

            expect(fetchGet).toBeCalledWith(
                `${HOST}/invoice/transfer/check`,
                {
                    invoice_id: '100',
                    dst_service_id: '3',
                    dst_service_order_id: '4567'
                },
                false,
                false
            );

            expect(fetchPost).toBeCalledWith(
                `${HOST}/invoice/transfer`,
                {
                    _csrf: 'csrf',
                    dst_order_id: '3-4567',
                    invoice_id: '100',
                    mode: 'ALL'
                },
                false
            );
        });
    });
});
