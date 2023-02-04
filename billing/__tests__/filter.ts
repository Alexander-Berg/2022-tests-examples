import { combineReducers, Reducer } from 'redux';
import { all } from 'redux-saga/effects';
import SagaTester from 'redux-saga-tester';

import { fetchGet as mockedFetchGet } from 'common/utils/old-fetch';
import { request } from 'common/utils/request';
import { HISTORY, InitialDataAction } from 'common/actions';
import { formatMessage } from 'common/utils/formatters';
import { clientSelectorState } from 'common/reducers/client-selector';

import reducers from '../../reducers';
import { getInitialState as getInitialFilter } from '../../reducers/filter';
import { getInitialList } from '../../reducers/list';
import { initialState as root } from '../../reducers/root';
import { FILTER, CLIENT, PERSON, MANAGER } from '../../actions';
import {
    watchRequestInitialData,
    watchRequestClient,
    watchRequestPerson,
    watchRequestManager,
    watchApplyQSFilter,
    watchReceiveInitialData,
    watchHumarizeData
} from '../filter';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');
jest.mock('common/utils/formatters');

const managerName = 'Черкасов Арсений Иванович';
const managerId = '20432';

const items = [
    { value: 1, content: 'one' },
    { value: 2, content: 'two' },
    { value: 3, content: 'three' },
    { value: 4, content: 'four' }
];

const currencies = [
    { value: 'USD', content: 'USD' },
    { value: 'RUR', content: 'RUR' },
    { value: 'EUR', content: 'EUR' }
];

const client = { id: 1, name: 'Client Name' };
const person = { id: 23, name: 'Person Name' };
const parentsNames = "['department', 'subdep']";

function* rootSaga() {
    yield all([
        watchApplyQSFilter(),
        watchRequestManager(),
        watchRequestPerson(),
        watchRequestClient(),
        watchRequestInitialData(),
        watchReceiveInitialData(),
        watchHumarizeData()
    ]);
}

(formatMessage as jest.Mock).mockResolvedValue('__mock__');

const fetchGet = mockedFetchGet as jest.Mock;
const requestGet = request.get as jest.Mock;

describe('Testing filter saga', () => {
    afterEach(() => {
        jest.resetAllMocks();
    });

    describe('try fetch initial data', () => {
        afterEach(() => {
            jest.resetAllMocks();
        });

        it('fetch minimal data', async () => {
            const initialState = {
                root,
                filter: getInitialFilter(),
                list: getInitialList()
            };

            const sagaTester = new SagaTester({
                initialState,
                reducers: combineReducers({ ...reducers }) as Reducer,
                middlewares: []
            });

            sagaTester.start(rootSaga);

            requestGet
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([]);

            sagaTester.dispatch({ type: InitialDataAction.REQUEST });

            await sagaTester.waitFor(InitialDataAction.RECEIVE);

            expect(sagaTester.getState().filter.currencies.toJS()).toEqual([]);
            expect(sagaTester.getState().filter.firms.toJS()).toEqual([]);
            expect(sagaTester.getState().filter.services.toJS()).toEqual([]);
        });

        it('fetch with client', async () => {
            const initialState = {
                root,
                filter: getInitialFilter({
                    nextFilter: {
                        clientId: client.id
                    }
                }),
                list: getInitialList(),
                clientSelector: clientSelectorState
            };

            const sagaTester = new SagaTester({
                initialState,
                reducers: combineReducers({ ...reducers }) as Reducer,
                middlewares: []
            });

            sagaTester.start(rootSaga);

            requestGet
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([]);

            fetchGet.mockResolvedValueOnce({ data: client });

            sagaTester.dispatch({ type: InitialDataAction.REQUEST, intercompaniesOptions: [] });

            await sagaTester.waitFor(InitialDataAction.RECEIVE);

            expect(sagaTester.getState().filter.currencies.toJS()).toEqual([]);
            expect(sagaTester.getState().filter.firms.toJS()).toEqual([]);
            expect(sagaTester.getState().filter.services.toJS()).toEqual([]);
            expect(sagaTester.getState().filter.next.clientId).toEqual(client.id);

            await sagaTester.waitFor(FILTER.APPLY);

            expect(sagaTester.getState().filter.next.clientId).toEqual(client.id);

            await sagaTester.waitFor(CLIENT.RECEIVE);
            expect(sagaTester.getState().filter.next.clientId).toEqual(client.id);
            expect(sagaTester.getState().filter.next.clientName).toEqual(client.name);
        });

        it('fetch with person', async () => {
            const initialState = {
                root,
                filter: getInitialFilter({
                    nextFilter: {
                        personId: person.id
                    }
                }),
                list: getInitialList()
            };

            const sagaTester = new SagaTester({
                initialState,
                reducers: combineReducers({ ...reducers }) as Reducer,
                middlewares: []
            });

            sagaTester.start(rootSaga);

            requestGet
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([]);

            fetchGet.mockResolvedValueOnce({ data: person });

            sagaTester.dispatch({ type: InitialDataAction.REQUEST });

            await sagaTester.waitFor(InitialDataAction.RECEIVE);

            expect(sagaTester.getState().filter.currencies.toJS()).toEqual([]);
            expect(sagaTester.getState().filter.firms.toJS()).toEqual([]);
            expect(sagaTester.getState().filter.services.toJS()).toEqual([]);
            expect(sagaTester.getState().filter.next.personId).toEqual(person.id);

            await sagaTester.waitFor(FILTER.APPLY);

            expect(sagaTester.getState().filter.current.personId).toEqual(person.id);

            await sagaTester.waitFor(PERSON.RECEIVE);
            expect(sagaTester.getState().filter.next.personId).toEqual(person.id);
            expect(sagaTester.getState().filter.next.personName).toEqual(person.name);
        });

        it('fetch with manager', async () => {
            const initialState = {
                root,
                filter: getInitialFilter({
                    nextFilter: {
                        managerId: managerId
                    }
                }),
                list: getInitialList()
            };

            const sagaTester = new SagaTester({
                initialState,
                reducers: combineReducers({ ...reducers }) as Reducer,
                middlewares: []
            });

            sagaTester.start(rootSaga);

            requestGet
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce({
                    id: managerId,
                    name: managerName,
                    parents_names: parentsNames
                });

            sagaTester.dispatch({ type: InitialDataAction.REQUEST });

            await sagaTester.waitFor(InitialDataAction.RECEIVE);

            expect(sagaTester.getState().filter.currencies.toJS()).toEqual([]);
            expect(sagaTester.getState().filter.firms.toJS()).toEqual([]);
            expect(sagaTester.getState().filter.services.toJS()).toEqual([]);
            expect(sagaTester.getState().filter.next.managerId).toEqual(managerId);

            await sagaTester.waitFor(FILTER.APPLY);

            expect(sagaTester.getState().filter.current.managerId).toEqual(managerId);

            await sagaTester.waitFor(MANAGER.RECEIVE);
            expect(sagaTester.getState().filter.next.managerId).toEqual(managerId);
            expect(sagaTester.getState().filter.next.managerName).toEqual(managerName);
        });

        it('fetch with client, person and manager', async () => {
            const initialState = {
                root,
                filter: getInitialFilter({
                    nextFilter: {
                        clientId: client.id,
                        personId: person.id,
                        managerId: managerId
                    }
                }),
                list: getInitialList()
            };

            const sagaTester = new SagaTester({
                initialState,
                reducers: combineReducers({ ...reducers }) as Reducer,
                middlewares: []
            });

            sagaTester.start(rootSaga);

            requestGet
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([]);

            fetchGet
                .mockResolvedValueOnce({ data: client })
                .mockResolvedValueOnce({ data: person });

            requestGet.mockResolvedValueOnce({
                id: managerId,
                name: managerName,
                parents_names: parentsNames
            });

            sagaTester.dispatch({ type: InitialDataAction.REQUEST });

            await sagaTester.waitFor(InitialDataAction.RECEIVE);

            expect(sagaTester.getState().filter.currencies.toJS()).toEqual([]);
            expect(sagaTester.getState().filter.firms.toJS()).toEqual([]);
            expect(sagaTester.getState().filter.services.toJS()).toEqual([]);
            expect(sagaTester.getState().filter.next.clientId).toEqual(client.id);
            expect(sagaTester.getState().filter.next.personId).toEqual(person.id);
            expect(sagaTester.getState().filter.next.managerId).toEqual(managerId);

            await sagaTester.waitFor(FILTER.APPLY);

            expect(sagaTester.getState().filter.current.clientId).toEqual(client.id);
            expect(sagaTester.getState().filter.current.personId).toEqual(person.id);
            expect(sagaTester.getState().filter.current.managerId).toEqual(managerId);

            await sagaTester.waitFor(CLIENT.RECEIVE);
            expect(sagaTester.getState().filter.next.clientId).toEqual(client.id);
            expect(sagaTester.getState().filter.next.clientName).toEqual(client.name);

            await sagaTester.waitFor(PERSON.RECEIVE);
            expect(sagaTester.getState().filter.next.personId).toEqual(person.id);
            expect(sagaTester.getState().filter.next.personName).toEqual(person.name);

            await sagaTester.waitFor(MANAGER.RECEIVE);
            expect(sagaTester.getState().filter.next.managerId).toEqual(managerId);
            expect(sagaTester.getState().filter.next.managerName).toEqual(managerName);
        });

        it('check receive initial data', async () => {
            const initialState = {
                root,
                filter: getInitialFilter({
                    nextFilter: {
                        clientId: client.id,
                        clientName: client.name
                    }
                }),
                list: getInitialList()
            };

            const sagaTester = new SagaTester({
                initialState,
                reducers: combineReducers({ ...reducers }) as Reducer,
                middlewares: []
            });

            sagaTester.start(rootSaga);

            sagaTester.dispatch({
                type: InitialDataAction.RECEIVE,
                currencies,
                firms: items,
                services: items,
                intercompaniesOptions: []
            });

            expect(sagaTester.getState().filter.currencies.toJS()).toEqual(currencies);
            expect(sagaTester.getState().filter.firms.toJS()).toEqual(items);
            expect(sagaTester.getState().filter.services.toJS()).toEqual(items);
            expect(sagaTester.getState().filter.next.clientId).toEqual(client.id);
            expect(sagaTester.getState().filter.next.clientName).toEqual(client.name);

            await sagaTester.waitFor(FILTER.APPLY);

            expect(sagaTester.getState().filter.current.clientId).toEqual(client.id);
            expect(sagaTester.getState().filter.current.clientName).toEqual(client.name);
        });

        it('receive client', async () => {
            const initialState = {
                root,
                filter: getInitialFilter({
                    nextFilter: {
                        clientId: client.id
                    }
                }),
                list: getInitialList()
            };

            const sagaTester = new SagaTester({
                initialState,
                reducers: combineReducers({ ...reducers }) as Reducer,
                middlewares: []
            });

            sagaTester.start(rootSaga);

            fetchGet.mockResolvedValueOnce({ data: client });

            sagaTester.dispatch({
                type: CLIENT.REQUEST,
                data: { clientId: client.id }
            });

            await sagaTester.waitFor(CLIENT.RECEIVE);

            expect(sagaTester.getState().filter.next.clientId).toEqual(client.id);
            expect(sagaTester.getState().filter.next.clientName).toEqual(client.name);
        });

        it('receive person', async () => {
            const initialState = {
                root,
                filter: getInitialFilter({
                    nextFilter: {
                        personId: person.id
                    }
                }),
                list: getInitialList()
            };

            const sagaTester = new SagaTester({
                initialState,
                reducers: combineReducers({ ...reducers }) as Reducer,
                middlewares: []
            });

            sagaTester.start(rootSaga);

            fetchGet.mockResolvedValueOnce({ data: person });

            sagaTester.dispatch({
                type: PERSON.REQUEST,
                data: { id: person.id }
            });

            await sagaTester.waitFor(PERSON.RECEIVE);

            expect(sagaTester.getState().filter.next.personId).toEqual(person.id);
            expect(sagaTester.getState().filter.next.personName).toEqual(person.name);
        });

        it('receive manager', async () => {
            const initialState = {
                root,
                filter: getInitialFilter({
                    nextFilter: {
                        managerId
                    }
                }),
                list: getInitialList()
            };

            const sagaTester = new SagaTester({
                initialState,
                reducers: combineReducers({ ...reducers }) as Reducer,
                middlewares: []
            });

            sagaTester.start(rootSaga);

            requestGet.mockResolvedValueOnce({
                id: managerId,
                name: managerName,
                parents_names: parentsNames
            });

            sagaTester.dispatch({
                type: MANAGER.REQUEST,
                data: { managerId }
            });

            await sagaTester.waitFor(MANAGER.RECEIVE);

            expect(sagaTester.getState().filter.next.managerId).toEqual(managerId);
            expect(sagaTester.getState().filter.next.managerName).toEqual(managerName);
        });
    });

    it('apply history', async () => {
        const initialState = {
            root,
            filter: getInitialFilter(),
            list: getInitialList()
        };

        const sagaTester = new SagaTester({
            initialState,
            reducers: combineReducers({ ...reducers }) as Reducer,
            middlewares: []
        });

        sagaTester.start(rootSaga);

        sagaTester.dispatch({
            type: HISTORY.APPLY_QS_FILTER,
            filter: {
                clientId: client.id,
                clientName: client.name
            }
        });

        await sagaTester.waitFor(FILTER.APPLY);

        expect(sagaTester.getState().filter.next.clientId).toEqual(client.id);
        expect(sagaTester.getState().filter.next.clientName).toEqual(client.name);

        expect(sagaTester.getState().filter.current.clientId).toEqual(client.id);
        expect(sagaTester.getState().filter.current.clientName).toEqual(client.name);
    });
});
