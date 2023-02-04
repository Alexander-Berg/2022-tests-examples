import { combineReducers } from 'redux';
import { all } from 'redux-saga/effects';
import SagaTester from 'redux-saga-tester';

import { fetchGet } from 'common/utils/old-fetch';
import { camelCasePropNames } from 'common/utils';
import { reducers } from '../../reducers';
import { createInitialState as createInitialOrder } from '../../reducers/order';
import { createInitialState as createInitialOrderOperations } from '../../reducers/order-operations';
import { ORDER_OPERATIONS } from '../../actions';
import { watchOrderOperationsRequest } from '../order-operations';

jest.mock('common/utils/old-fetch');

const HOST = 'http://snout-test';
function* rootSaga() {
    yield all([watchOrderOperationsRequest()]);
}

describe('testing order-operations saga', () => {
    afterEach(() => {
        jest.resetAllMocks();
    });

    it('try fetch order operations', async () => {
        const initialState = {
            order: createInitialOrder({ id: 1, service: { id: 2, cc: 'cc' } }),
            orderOperations: createInitialOrderOperations()
        };

        const sagaTester = new SagaTester({
            initialState,
            reducers: combineReducers({ ...reducers }),
            middlewares: []
        });

        sagaTester.start(rootSaga);

        const data = {
            'has-next': false,
            operations: [
                {
                    'dst-order': null,
                    dt: '2019-09-23T15:40:08',
                    'external-type-id': '1',
                    id: 1167988052,
                    invoice: {
                        client: {
                            id: 109497694,
                            name: 'balance_test 2019-09-23 15:40:03.778074'
                        },
                        dt: '2019-09-23T15:40:07',
                        'external-id': 'Z-1920528087-1',
                        hidden: false,
                        id: 101068440,
                        paysys: { id: 2501020 },
                        'receipt-sum': '1176.00',
                        'receipt-sum-1c': '1176.00',
                        request: { id: 1920528087 },
                        'total-sum': '1176.00'
                    },
                    memo: null,
                    'oper-qty': '6.349206',
                    'passport-id': null,
                    'src-order': null,
                    transaction: {
                        'dynamic-discount-pct': '0.00',
                        invoice: {
                            client: {
                                id: 109497694,
                                name: 'balance_test 2019-09-23 15:40:03.778074'
                            },
                            dt: '2019-09-23T15:40:07',
                            'external-id': 'Z-1920528087-1',
                            hidden: false,
                            id: 101068440,
                            paysys: { id: 2501020 },
                            'receipt-sum': '1176.00',
                            'receipt-sum-1c': '1176.00',
                            request: { id: 1920528087 },
                            'total-sum': '1176.00'
                        },
                        'static-discount-pct': '5.50',
                        'transact-qty': '6.349206',
                        type: 'consume'
                    },
                    'type-id': 2
                }
            ],
            'total-row-count': 1
        };

        fetchGet.mockResolvedValueOnce({
            data
        });

        sagaTester.dispatch({ type: ORDER_OPERATIONS.REQUEST });

        await sagaTester.waitFor(ORDER_OPERATIONS.RECEIVE);

        expect(fetchGet).toBeCalledWith(
            `${HOST}/order/operations`,
            {
                limit: 10,
                offset: 0,
                order_id: 1,
                service_cc: 'cc',
                service_id: 2,
                service_order_id: 0,
                sort_key: 'DT',
                sort_order: 'DESC'
            },
            false,
            false
        );

        const orderOperations = sagaTester.getState().orderOperations;

        expect(orderOperations.isFetching).toEqual(false);
        expect(orderOperations.hasNext).toEqual(false);
        expect(orderOperations.items.toJS()).toEqual(camelCasePropNames(data.operations));
    });
});
