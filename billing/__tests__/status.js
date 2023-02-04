import { Map } from 'immutable';
import { runSaga } from 'redux-saga';
import { delay } from 'redux-saga/effects';

import { EXPORT_STATE, OBJECT_NAME, reexportObject, getExportState } from 'admin/api/common';
import { fetchReexport, fetchGetState } from '../status';
import { STATUS } from '../../actions';

jest.mock('admin/api/act');
jest.mock('admin/api/common');
jest.mock('redux-saga/effects', () => {
    const effects = jest.requireActual('redux-saga/effects');
    return {
        ...effects,
        delay: jest.fn()
    };
});

describe('admin / act / sagas / status', () => {
    afterEach(() => {
        jest.resetAllMocks();
    });

    describe('fetchReexport', () => {
        test('successful reexport', async () => {
            expect.assertions(2);

            reexportObject.mockReturnValueOnce();

            const dispatched = [];

            const state = { actPage$status: Map({ actStatus: { id: 123 } }) };

            await runSaga(
                {
                    dispatch: action => dispatched.push(action),
                    getState: () => state
                },
                fetchReexport
            );

            expect(reexportObject).toBeCalledWith({
                objectName: OBJECT_NAME.ACT,
                objectId: 123
            });
            expect(dispatched).toEqual([{ type: STATUS.RECEIVE_REEXPORT }]);
        });

        test('failed reexport', async () => {
            expect.assertions(2);

            const error = new Error('Something went wrong');
            reexportObject.mockRejectedValue(error);

            const dispatched = [];

            const state = { actPage$status: Map({ actStatus: { id: 123 } }) };

            await runSaga(
                {
                    dispatch: action => dispatched.push(action),
                    getState: () => state
                },
                fetchReexport
            );

            expect(reexportObject).toBeCalledWith({
                objectName: OBJECT_NAME.ACT,
                objectId: 123
            });
            expect(dispatched).toEqual([{ type: STATUS.REEXPORT_FETCH_ERROR, error }]);
        });
    });

    describe('fetchGetState', () => {
        test('state is WAITING after first request - should request one more time', async () => {
            expect.assertions(3);

            getExportState.mockReturnValueOnce({ state: EXPORT_STATE.WAITING });

            const dispatched = [];

            const state = { actPage$status: Map({ actStatus: { id: 123 } }) };

            await runSaga(
                {
                    dispatch: action => dispatched.push(action),
                    getState: () => state
                },
                fetchGetState,
                {}
            );

            await expect(getExportState).toBeCalledWith({
                className: 'Act',
                queueType: 'OEBS',
                objectId: 123
            });

            expect(delay).toBeCalledWith(5000);

            expect(dispatched).toEqual([{ type: STATUS.REQUEST_GET_EXPORT_STATE, counter: 1 }]);
        });

        test('state is EXPORTED - should stop requesting', async () => {
            expect.assertions(3);

            getExportState.mockReturnValueOnce({
                state: EXPORT_STATE.EXPORTED,
                exportDt: '2019-08-12'
            });

            const dispatched = [];

            const state = { actPage$status: Map({ actStatus: { id: 123 } }) };

            await runSaga(
                {
                    dispatch: action => dispatched.push(action),
                    getState: () => state
                },
                fetchGetState,
                {}
            );

            await expect(getExportState).toBeCalledWith({
                className: 'Act',
                queueType: 'OEBS',
                objectId: 123
            });

            expect(delay).not.toBeCalled();

            expect(dispatched).toEqual([
                {
                    type: STATUS.RECEIVE_GET_EXPORT_STATE,
                    exportDt: '2019-08-12',
                    state: EXPORT_STATE.EXPORTED
                }
            ]);
        });
    });
});
