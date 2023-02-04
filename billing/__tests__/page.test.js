import React from 'react';
import Enzyme, { mount } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { combineReducers } from 'redux';
import { Provider } from 'react-redux';
import SagaTester from 'redux-saga-tester';
import { fromJS, List } from 'immutable';
import { fetchGet, fetchPost } from 'common/utils/old-fetch';
import { all } from 'redux-saga/effects';

import { fromQSToParams } from '../history';
import { camelCasePropNames } from 'common/utils';
import { loggerMiddleware, HOST } from 'common/utils/test-utils/common';
import commonReducers from 'common/reducers/common';
import { Permissions } from 'common/constants';
import withIntlProvider from 'common/utils/test-utils/with-intl-provider';
import { MessagesActions, acceptModalMessage } from 'common/actions/messages';
import { messages } from 'common/sagas/messages';
import { numerise } from 'admin/api/data-processors/common';
import { preprocessOrder } from 'admin/api/data-processors/order';
import { reducers } from '../reducers';
import { createInitialState as createOrder } from '../reducers/order';
import { createInitialState as createOrderOperations } from '../reducers/order-operations';
import { createOrderRequests } from '../reducers/order-requests';
import { createInitialState as createPayOrder } from '../reducers/pay-order';
import { createRoot } from '../reducers/root';
import { createInitialState as createTransferOrder } from '../reducers/transfer-order';
import { createTransferTargets } from '../reducers/transfer-targets';
import { rootSaga } from '../sagas';
import {
    ORDER,
    ORDER_OPERATIONS,
    ORDER_REQUESTS,
    ORDER_TRANSFER_TARGETS,
    ORDER_SELECT,
    MODE_CHANGE,
    AMOUNT_CHANGE,
    DISCOUNT_CHANGE,
    DISCOUNT_SWITCH,
    TRANSFER_ORDERS,
    PAY_ORDER
} from '../actions';
import { RootContainer } from '../containers/RootContainer';
import { defaultDestinationOrder } from '../constants';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');
jest.mock('../history');
Enzyme.configure({ adapter: new Adapter() });

const client = { id: 10226, name: 'Фельман Дмитрий Павлович' };
const consumes = {
    act_qty: '600.000000',
    act_sum: '483.90',
    bonus_qty: '0.000000',
    completion_qty: '0.000000',
    completion_sum: '483.90',
    consumes_list: [],
    current_qty: '600.000000',
    current_sum: '483.90',
    pageNumber: 1,
    pageSize: 10
};
const product = {
    id: 286,
    name: 'Отказ от размещения баннера на сайте пользователя'
};
const service = {
    cc: 'narod_1',
    id: 2,
    name: 'Народ: Отключение баннеров',
    url_orders: 'http://narod.yandex.ru/balance/nb_form.xhtml'
};
const order = {
    agency: null,
    client,
    completion_qty: consumes.completion_qty,
    consume_qty: consumes.current_qty,
    consumes,
    dt: '2003-05-27T01:40:08',
    has_consumes: true,
    id: 1015,
    invoice_orders: [
        {
            invoice: {
                credit: 0,
                currency: 'RUR',
                dt: '2019-09-22T00:45:04',
                effective_sum: '10000.00',
                external_id: 'ЛСТ-1920472890-1',
                has_receipts: false,
                id: 101052625,
                paysys: {
                    certificate: '0',
                    id: 1301003,
                    name: 'Банк для юридических лиц'
                },
                total_sum: '10000.00'
            }
        }
    ],
    creditOrderPaginator: { pageNumber: 1, pageSize: 10 },
    withReceiptsOrderPaginator: { pageNumber: 1, pageSize: 10 },
    withoutReceiptsOrderPaginator: { pageNumber: 1, pageSize: 10 },
    isFound: true,
    manager: null,
    memo: null,
    passport_id: 16571028,
    precision: 6,
    product,
    service,
    service_order_id: 420,
    text: 'Отключение баннеров',
    unit_name: 'дни',
    consumesPaginator: {
        isUpdating: false,
        paginator: {
            pageNumber: 1,
            pageSize: 10
        }
    },
    invoiceOrdersSort: {
        credit: {
            sortKey: 'DT',
            sortOrder: 'DESC'
        },
        withReceipts: {
            sortKey: 'DT',
            sortOrder: 'DESC'
        },
        withoutReceipts: {
            sortKey: 'DT',
            sortOrder: 'DESC'
        }
    }
};

const invoice = {
    client,
    dt: '2005-09-01T09:19:42',
    'external-id': 'Б-628141-1',
    hidden: false,
    id: 529386,
    paysys: { id: 1003 },
    'receipt-sum': '200.00',
    'receipt-sum-1c': '5711.32',
    request: { id: 628141 },
    'total-sum': '5711.32'
};

const transaction = {
    'dynamic-discount-pct': '0.00',
    invoice,
    'static-discount-pct': '0.00',
    'transact-qty': '-10.00000',
    type: 'reverse'
};

const operation = {
    'dst-order': null,
    dt: '2005-09-06T13:58:56',
    'external-type-id': 6,
    id: null,
    invoice,
    memo: null,
    'oper-qty': transaction['transact-qty'],
    'passport-id': order.passport_id,
    'src-order': {
        'agency-id': null,
        'direct-payment-number': 5007335,
        id: order.id,
        service,
        text: order.text,
        'type-rate': 1,
        unit: null
    },
    transaction
};

const orderPerms = [
    {
        code: 'TransferFromOrder',
        id: 11141,
        name: 'Перенос средств c заказа'
    },
    { code: 'ViewOrders', id: 11201, name: 'Просмотр заказов' },
    {
        code: 'TransferBetweenClients',
        id: 11143,
        name: 'Перенос средств между разными клиентами'
    },
    { code: 'BillingSupport', id: 1100, name: 'Саппорт Биллинга' },
    { code: 'ViewFishes', id: 22, name: 'Фишковидец' },
    {
        code: 'AdminAccess',
        id: 0,
        name: 'Доступ к административному режиму'
    },
    {
        code: 'CreateCerificatePayments',
        id: 4,
        name: 'Внесение платежей сертификатом, бартером'
    }
];

const initials = [
    {
        url: '/order',
        data: order,
        params: {
            order_id: order.id,
            consumes_pn: 1,
            consumes_ps: 10,
            service_cc: undefined,
            service_id: undefined,
            service_order_id: undefined
        }
    },
    {
        url: '/user/object-permissions',
        data: orderPerms,
        params: { classname: 'Order', object_id: order.id }
    },
    {
        url: '/order/withdraw/from-orders',
        data: [
            {
                client_id: client.id,
                client_name: client.name,
                last_touch_dt: '2019-09-20',
                order_client_id: client.id,
                order_dt: '2003-05-27',
                order_eid: '2-420',
                order_id: 23344,
                price: '5.00000',
                price_wo_nds: '4.237333',
                product_name: product.name,
                service_cc: service.cc,
                service_id: service.id,
                service_order_id: order.service_order_id,
                text: order.text,
                type_rate: 30,
                unit: 'мес. '
            }
        ],
        params: { client_id: client.id }
    },
    {
        url: '/order/untouched-requests',
        data: {
            entry: [
                {
                    client_id: client.id,
                    order_id: order.id,
                    request_dt: order.dt,
                    request_id: 1018
                }
            ],
            request: { order_id: order.id, ps: 20, pn: 1 }
        },
        params: {
            order_id: order.id,
            service_cc: 'narod_1',
            service_id: 2,
            service_order_id: 420,
            pagination_pn: 1,
            pagination_ps: 10,
            sort_key: 'DT',
            sort_order: 'DESC'
        }
    },
    {
        url: '/order/operations',
        data: {
            'has-next': true,
            operations: [operation]
        },
        params: {
            limit: 10,
            offset: 0,
            order_id: 1015,
            service_cc: 'narod_1',
            service_id: 2,
            service_order_id: 420,
            sort_key: 'DT',
            sort_order: 'DESC'
        }
    }
];

const perms = Object.values(Permissions);

[
    'order-loading-container',
    'order-consumes-list-container',
    'order-general-info-container',
    'order-invoice-credit-container',
    'order-invoice-without-receipts-container',
    'order-invoice-with-receipts-container',
    'order-operations-container',
    'order-memo-container',
    'order-requests-container',
    'order-pay-container',
    'order-transfer-orders-container'
].forEach(id => {
    const div = window.document.createElement('div');
    div.id = id;
    window.document.body.appendChild(div);
});

describe('admin-order', () => {
    beforeAll(initializeDesktopRegistry);

    afterEach(() => {
        jest.resetAllMocks();
    });

    it('open page and load initial data', async () => {
        expect.assertions(initials.length + 14);

        const initialState = {
            perms,
            order: createOrder(),
            orderOperations: createOrderOperations(),
            orderRequests: createOrderRequests(),
            payOrder: createPayOrder(),
            root: createRoot(),
            transferOrder: createTransferOrder(),
            transferTargets: createTransferTargets()
        };

        const rootReducer = combineReducers({
            ...commonReducers,
            ...reducers
        });

        const sagaTester = new SagaTester({
            initialState,
            reducers: rootReducer
            // middlewares: [loggerMiddleware]
        });

        sagaTester.start(rootSaga);

        const store = sagaTester.store;

        const Container = withIntlProvider(() => (
            <Provider store={store}>
                <RootContainer />
            </Provider>
        ));

        fromQSToParams.mockReturnValue({ orderId: order.id });

        let i = 0;
        let j = 0;
        initials.forEach(({ data, url }) => {
            fetchGet.mockResolvedValueOnce({ data });
        });

        mount(<Container />);

        // После монтирования страницы должны быть отправланы запросы на начальные данные.
        await sagaTester.waitFor(ORDER.RECEIVE);
        await sagaTester.waitFor(ORDER_TRANSFER_TARGETS.RECEIVE);
        await sagaTester.waitFor(ORDER_REQUESTS.RECEIVE);
        await sagaTester.waitFor(ORDER_OPERATIONS.RECEIVE);

        initials.forEach(({ url, params }, idx) => {
            expect(fetchGet).nthCalledWith(idx + 1, `${HOST}${url}`, params, false, false);
        });

        const {
            root,
            order: stateOrder,
            orderOperations,
            orderRequests,
            transferTargets
        } = sagaTester.getState();

        expect(root.isFetching).toBe(false);

        expect(stateOrder.isFetching).toBe(false);
        expect(stateOrder.isUpdating).toBe(false);
        expect(stateOrder.details.toJS()).toEqual(
            preprocessOrder(numerise(camelCasePropNames(order)))
        );

        expect(orderOperations.isFetching).toBe(false);
        expect(orderOperations.isUpdating).toBe(false);
        expect(orderOperations.hasNext).toBe(true);
        expect(orderOperations.items).toEqual(fromJS([camelCasePropNames(operation)]));

        expect(orderRequests.isFetching).toBe(false);
        expect(orderRequests.isUpdating).toBe(false);
        expect(orderRequests.items).toEqual([
            {
                clientId: client.id,
                orderId: order.id,
                requestDt: order.dt,
                requestId: 1018
            }
        ]);

        expect(transferTargets.isFetching).toBe(false);
        expect(transferTargets.isUpdating).toBe(false);
        expect(transferTargets.items).toEqual([
            {
                value: defaultDestinationOrder,
                content: 'Беззаказье - свободные средства'
            },
            {
                value: '2-420',
                content:
                    '2-420 (2019-09-20): Отключение баннеров, Клиент: 10226-Фельман Дмитрий Павлович'
            }
        ]);
    });

    it('transfer order', async () => {
        expect.assertions(initials.length + 7);
        const initialState = {
            perms,
            orderPerms: List(orderPerms.map(({ code }) => code)),
            order: createOrder(preprocessOrder(numerise(camelCasePropNames(order)))),
            orderOperations: createOrderOperations({
                hasNext: true,
                items: fromJS([camelCasePropNames(operation)])
            }),
            orderRequests: createOrderRequests({
                items: [
                    {
                        clientId: client.id,
                        orderId: order.id,
                        requestDt: order.dt,
                        requestId: 1018
                    }
                ],
                totalCount: 1
            }),
            payOrder: createPayOrder(),
            root: createRoot({ isFetching: false }),
            transferOrder: createTransferOrder(),
            transferTargets: createTransferTargets({
                items: [
                    {
                        value: defaultDestinationOrder,
                        content: 'Беззаказье - свободные средства'
                    },
                    {
                        value: '2-420',
                        content:
                            '2-420 (2019-09-20): Отключение баннеров, Клиент: 10226-Фельман Дмитрий Павлович'
                    }
                ]
            })
        };

        const rootReducer = combineReducers({
            ...commonReducers,
            ...reducers
        });

        const sagaTester = new SagaTester({
            initialState,
            reducers: rootReducer
        });

        sagaTester.start(rootSaga);

        const store = sagaTester.store;

        const Container = withIntlProvider(() => (
            <Provider store={store}>
                <RootContainer />
            </Provider>
        ));

        const wrapper = mount(<Container />);

        // Имитируем выбор заказа назначения через выпадающий список.
        sagaTester.dispatch({ type: ORDER_SELECT, order: '2-420' });

        expect(sagaTester.getState().transferOrder.order).toBe('2-420');

        // Пробуем ввести кастомный заказ через поле ввода
        wrapper
            .find('input#transfer-orders__arbitrary-order')
            .simulate('change', { target: { value: '1-1' } });

        await sagaTester.waitFor(ORDER_SELECT);

        expect(sagaTester.getState().transferOrder.order).toBe('1-1');

        // Выбираем перевести часть средств
        sagaTester.dispatch({
            type: MODE_CHANGE,
            mode: 'SRC_PTS',
            strAmount: `100`
        });

        expect(sagaTester.getState().transferOrder.mode).toBe('SRC_PTS');

        // Задаем сумму
        wrapper
            .update()
            .find('input#transfer-orders__amount')
            .simulate('change', { target: { value: '50' } });

        await sagaTester.waitFor(AMOUNT_CHANGE);

        expect(sagaTester.getState().transferOrder.strAmount).toBe('50');

        // Выбираем возможность ввести скидку вручную
        sagaTester.dispatch({ type: DISCOUNT_SWITCH });

        // Задаем скидку
        wrapper
            .find('input#transfer-orders__discount')
            .simulate('change', { target: { value: '10' } });

        await sagaTester.waitFor(DISCOUNT_CHANGE);

        expect(sagaTester.getState().transferOrder.strDiscount).toBe('10');

        // При сабмите должен быть отправлен запрос на проверку возможности перевода, в случае успеха - запрос на перевод,
        // после чего мы должны обновить страницу, запросив обновленные данные по заказу
        fetchGet.mockResolvedValueOnce({ data: { msg: null, result: true } });
        fetchPost.mockResolvedValueOnce({ data: null });
        fromQSToParams.mockReturnValue({ orderId: order.id });
        initials.forEach(({ data }, i) => fetchGet.mockResolvedValueOnce({ data }));

        // Пытаемся выполнить перевод
        wrapper.find('form').at(0).simulate('submit');

        await sagaTester.waitFor(TRANSFER_ORDERS.REQUEST);

        // Проверяем, что был запрос на проверку
        expect(fetchGet).toBeCalledWith(
            `${HOST}/order/transfer/check`,
            {
                order_id: order.id,
                dst_service_id: 1,
                dst_service_order_id: 1
            },
            false,
            false
        );

        await sagaTester.waitFor(TRANSFER_ORDERS.RECEIVE);

        // Проверяем, что был запрос на перевод
        expect(fetchPost).toBeCalledWith(
            `${HOST}/order/transfer`,
            {
                order_id: order.id,
                dst_service_id: 1,
                dst_service_order_id: 1,
                qty: '50',
                discount_pct: '10',
                mode: 'SRC_PTS',
                _csrf: 'csrf'
            },
            false
        );

        await sagaTester.waitFor(ORDER.RECEIVE);
        await sagaTester.waitFor(ORDER_TRANSFER_TARGETS.RECEIVE);
        await sagaTester.waitFor(ORDER_REQUESTS.RECEIVE);
        await sagaTester.waitFor(ORDER_OPERATIONS.RECEIVE);

        // Проверяем, чтобы были отправлены запросы для обновления страницы
        initials.forEach(({ url, params }) => {
            expect(fetchGet).toBeCalledWith(`${HOST}${url}`, params, false, false);
        });
    });

    it('pay order', async () => {
        expect.assertions(9);

        const initialState = {
            perms,
            orderPerms: List(orderPerms.map(({ code }) => code)),
            order: createOrder(preprocessOrder(numerise(camelCasePropNames(order)))),
            orderOperations: createOrderOperations({
                hasNext: true,
                items: fromJS([camelCasePropNames(operation)])
            }),
            orderRequests: createOrderRequests({
                items: [
                    {
                        clientId: client.id,
                        orderId: order.id,
                        requestDt: order.dt,
                        requestId: 1018
                    }
                ],
                totalCount: 1
            }),
            payOrder: createPayOrder(),
            root: createRoot({ isFetching: false }),
            transferOrder: createTransferOrder(),
            transferTargets: createTransferTargets({
                items: [
                    {
                        value: defaultDestinationOrder,
                        content: 'Беззаказье - свободные средства'
                    },
                    {
                        value: '2-420',
                        content:
                            '2-420 (2019-09-20): Отключение баннеров, Клиент: 10226-Фельман Дмитрий Павлович'
                    }
                ]
            })
        };

        const rootReducer = combineReducers({
            ...commonReducers,
            ...reducers
        });

        const sagaTester = new SagaTester({
            initialState,
            reducers: rootReducer
        });

        sagaTester.start(function* () {
            yield all([rootSaga(), messages()]);
        });

        const store = sagaTester.store;

        const Container = withIntlProvider(() => (
            <Provider store={store}>
                <RootContainer />
            </Provider>
        ));

        const wrapper = mount(<Container />);

        // Имитируем изменение оплаты
        sagaTester.dispatch({ type: PAY_ORDER.SELECT_PAY_SYS, paySys: 'co' });
        expect(sagaTester.getState().payOrder.paySys).toBe('co');

        // Пробуем изменить сумму оплаты
        wrapper.find('input#order-pay-sum-input').simulate('change', { target: { value: '50' } });

        await sagaTester.waitFor(PAY_ORDER.CHANGE_QTY);

        expect(sagaTester.getState().payOrder.qty).toBe('50');
        expect(sagaTester.getState().payOrder.disabled).toBe(false);

        // Сабмитим форму, после сабмита должна появиться форма подтверждения
        wrapper.find('form').at(1).simulate('submit');
        await sagaTester.waitFor(MessagesActions.SHOW_CONFIRMATION_MESSAGE);

        fetchPost.mockResolvedValueOnce({ data: null });
        // После успешной оплаты мы должны обновить страницу, запросив обновленные данные по заказу
        fromQSToParams.mockReturnValue({ orderId: order.id });
        initials.forEach(({ data }, i) => fetchGet.mockResolvedValueOnce({ data }));

        await sagaTester.dispatch(acceptModalMessage());
        await sagaTester.waitFor(PAY_ORDER.RECEIVE);

        //Проверяем, что был отправлен запрос на оплату
        expect(fetchPost).toBeCalledWith(
            `${HOST}/order/pay`,
            {
                order_id: order.id,
                qty: '50',
                paysys_cc: 'co',
                _csrf: 'csrf'
            },
            false
        );

        await sagaTester.waitFor(ORDER.RECEIVE);
        await sagaTester.waitFor(ORDER_TRANSFER_TARGETS.RECEIVE);
        await sagaTester.waitFor(ORDER_REQUESTS.RECEIVE);
        await sagaTester.waitFor(ORDER_OPERATIONS.RECEIVE);

        // Проверяем, чтобы были отправлены запросы для обновления страницы
        initials.forEach(({ url, params }) => {
            expect(fetchGet).toBeCalledWith(`${HOST}${url}`, params, false, false);
        });
    });
});
