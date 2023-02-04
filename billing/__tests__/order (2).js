import { combineReducers } from 'redux';
import { all } from 'redux-saga/effects';
import SagaTester from 'redux-saga-tester';

import { fetchGet } from 'common/utils/old-fetch';
import { numerise } from 'admin/api/data-processors/common';

import { camelCasePropNames } from 'common/utils';
import { reducers } from '../../reducers';
import { createInitialState as createInitialOrder } from '../../reducers/order';
import { ORDER } from '../../actions';
import { watchOrderRequest } from '../order';

jest.mock('common/utils/old-fetch');

const HOST = 'http://snout-test';

function* rootSaga() {
    yield all([watchOrderRequest()]);
}

describe('testing order saga', () => {
    afterEach(() => {
        jest.resetAllMocks();
    });

    it('try fetch order', async () => {
        const initialState = {
            order: createInitialOrder()
        };

        const sagaTester = new SagaTester({
            initialState,
            reducers: combineReducers({ ...reducers }),
            middlewares: []
        });

        sagaTester.start(rootSaga);

        const data = {
            agency: null,
            client: {
                id: 109475479,
                name: 'balance_test 2019-09-22 00:44:59.505155'
            },
            completion_qty: '600.000000',
            consume_qty: '600.000000',
            consumes: {
                act_qty: '600.000000',
                act_sum: '483.90',
                bonus_qty: '0.000000',
                completion_qty: '600.000000',
                completion_sum: '483.90',
                consumes_list: [],
                current_qty: '600.000000',
                current_sum: '483.90',
                pageNumber: 1,
                pageSize: 10
            },
            dt: '2019-09-22T00:45:02',
            has_consumes: true,
            id: 1526927146,
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
            product: {
                id: 509966,
                name:
                    'Услуги по организации перевозок пассажиров и багажа легковым такси, а также по организации иных услуг, доступных для заказа с использованием Сервиса'
            },
            service: {
                cc: 'taxi_corp_clients',
                id: 650,
                name: 'Яндекс.Корпоративное Такси (Клиенты)',
                url_orders: null
            },
            service_order_id: 20000012733024,
            text:
                'Услуги по организации перевозок пассажиров и багажа легковым такси, а также по организации иных услуг, доступных для заказа с использованием Сервиса',
            unit_name: 'деньги',
            consumesPaginator: {
                isUpdating: false,
                paginator: {
                    pageNumber: 1,
                    pageSize: 10
                }
            },
            invoiceOrdersSort: {
                sortKey: 'DT',
                sortOrder: 'DESC'
            }
        };

        fetchGet
            .mockResolvedValueOnce({
                data
            })
            .mockResolvedValueOnce({
                data: [
                    {
                        code: 'CreateCerificatePayments',
                        id: 4,
                        name: 'Внесение платежей сертификатом, бартером'
                    }
                ]
            });

        sagaTester.dispatch({ type: ORDER.REQUEST });

        expect(sagaTester.getState().order.isFetching).toBe(true);

        await sagaTester.waitFor(ORDER.RECEIVE);

        expect(sagaTester.getState().order.isFetching).toBe(false);

        const expected = numerise(camelCasePropNames(data));
        expected.invoiceOrders.forEach(({ invoice }) => (invoice.credit = invoice.credit === 2));

        expect(sagaTester.getState().order.details.toJS()).toEqual(expected);
    });
});
