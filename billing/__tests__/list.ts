import { combineReducers, Reducer } from 'redux';
import { all } from 'redux-saga/effects';
import SagaTester from 'redux-saga-tester';

import { fetchGet as mockedFetchGet } from 'common/utils/old-fetch';
import { HISTORY } from 'common/actions';
import { SortOrder, SelectEnum } from 'common/constants';
import reducers from '../../reducers';
import { getInitialState as getInitialFilter } from '../../reducers/filter';
import { getInitialList } from '../../reducers/list';
import { initialState as root } from '../../reducers/root';
import { FILTER, LIST } from '../../actions';
import { SORT_KEYS } from '../../constants';
import { watchFetchActs } from '../list';

jest.mock('common/utils/old-fetch');

const externalId = '377818';
const factura = '070701000003';
const invoiceEid = '1471848';
const contractEid = '4378923';
const actDtFrom = '2018-03-01';
const actDtTo = '2018-11-10';
const managerName = 'Черкасов Арсений Иванович';
const managerId = 20432;

const items = [
    { val: 1, text: 'one' },
    { val: 2, text: 'two' },
    { val: 3, text: 'three' },
    { val: 4, text: 'four' }
];

const [serviceId, firmId] = items;
const currencies = [
    { val: 810, text: 'RUB' },
    { val: 840, text: 'USD' },
    { val: 978, text: 'EUR' }
];
const [currencyCode] = currencies;
const currencyMap = new SelectEnum(currencies.map(({ val, text }) => ({ id: val, val: text })));

const client = { id: 1, name: 'Client Name' };
const person = { id: 23, name: 'Person Name' };

const managers = [{ id: managerId, val: managerName }];

function* rootSaga() {
    yield all([watchFetchActs()]);
}

const fetchGet = mockedFetchGet as jest.Mock;

describe('Testing list saga', () => {
    afterEach(() => {
        jest.resetAllMocks();
    });

    it('try with empty filter', async () => {
        const initialState = {
            root,
            filter: getInitialFilter({
                currencies,
                managers,
                currencyMap,
                firms: items,
                services: items
            }),
            list: getInitialList()
        };

        const sagaTester = new SagaTester({
            initialState,
            reducers: combineReducers({ ...reducers }) as Reducer,
            middlewares: []
        });

        sagaTester.start(rootSaga);

        fetchGet.mockResolvedValueOnce({
            data: {
                items: [],
                total_row_count: 0,
                totals: null,
                gtotals: null
            }
        });

        sagaTester.dispatch({ type: FILTER.APPLY });

        await sagaTester.waitFor(LIST.RECEIVE);

        expect(sagaTester.getState().list.acts).toEqual([]);
        expect(sagaTester.getState().list.totalCount).toEqual(null);
    });

    it('try with externalId', async () => {
        const initialState = {
            root,
            filter: getInitialFilter({
                currencies,
                managers,
                currencyMap,
                firms: items,
                services: items,
                nextFilter: {
                    externalId
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

        fetchGet.mockResolvedValueOnce({
            data: {
                items: [],
                total_row_count: 0,
                totals: null,
                gtotals: null
            }
        });

        sagaTester.dispatch({ type: FILTER.APPLY });

        await sagaTester.waitFor(LIST.RECEIVE);

        expect(sagaTester.getState().list.acts).toEqual([]);
        expect(sagaTester.getState().list.totalCount).toEqual(0);
    });

    it('try with factura', async () => {
        const initialState = {
            root,
            filter: getInitialFilter({
                currencies,
                managers,
                currencyMap,
                firms: items,
                services: items,
                nextFilter: {
                    factura
                },
                currentFilter: {
                    factura
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

        fetchGet.mockResolvedValueOnce({
            data: {
                items: [],
                total_row_count: 0,
                totals: null,
                gtotals: null
            }
        });

        sagaTester.dispatch({ type: FILTER.APPLY });

        await sagaTester.waitFor(LIST.RECEIVE);

        expect(sagaTester.getState().list.acts).toEqual([]);
        expect(sagaTester.getState().list.totalCount).toEqual(0);
    });

    it('try with invoiceEid', async () => {
        const initialState = {
            root,
            filter: getInitialFilter({
                currencies,
                managers,
                currencyMap,
                firms: items,
                services: items,
                nextFilter: {
                    invoiceEid
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

        fetchGet.mockResolvedValueOnce({
            data: {
                items: [],
                total_row_count: 0,
                totals: null,
                gtotals: null
            }
        });

        sagaTester.dispatch({ type: FILTER.APPLY });

        await sagaTester.waitFor(LIST.RECEIVE);

        expect(sagaTester.getState().list.acts).toEqual([]);
        expect(sagaTester.getState().list.totalCount).toEqual(0);
    });

    it('try with contractEid', async () => {
        const initialState = {
            root,
            filter: getInitialFilter({
                currencies,
                managers,
                currencyMap,
                firms: items,
                services: items,
                nextFilter: {
                    contractEid
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

        fetchGet.mockResolvedValueOnce({
            data: {
                items: [],
                total_row_count: 0,
                totals: null,
                gtotals: null
            }
        });

        sagaTester.dispatch({ type: FILTER.APPLY });

        await sagaTester.waitFor(LIST.RECEIVE);

        expect(sagaTester.getState().list.acts).toEqual([]);
        expect(sagaTester.getState().list.totalCount).toEqual(0);
    });

    it('try with actDtFrom', async () => {
        const initialState = {
            root,
            filter: getInitialFilter({
                currencies,
                managers,
                currencyMap,
                firms: items,
                services: items,
                nextFilter: {
                    actDtFrom
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

        fetchGet.mockResolvedValueOnce({
            data: {
                items: [],
                total_row_count: 0,
                totals: null,
                gtotals: null
            }
        });

        sagaTester.dispatch({ type: FILTER.APPLY });

        await sagaTester.waitFor(LIST.RECEIVE);

        expect(sagaTester.getState().list.acts).toEqual([]);
        expect(sagaTester.getState().list.totalCount).toEqual(0);
    });

    it('try with actDtTo', async () => {
        const initialState = {
            root,
            filter: getInitialFilter({
                currencies,
                managers,
                currencyMap,
                firms: items,
                services: items,
                nextFilter: {
                    actDtTo
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

        fetchGet.mockResolvedValueOnce({
            data: {
                items: [],
                total_row_count: 0,
                totals: null,
                gtotals: null
            }
        });

        sagaTester.dispatch({ type: FILTER.APPLY });

        await sagaTester.waitFor(LIST.RECEIVE);

        expect(sagaTester.getState().list.acts).toEqual([]);
        expect(sagaTester.getState().list.totalCount).toEqual(0);
    });

    it('try with manager', async () => {
        const initialState = {
            root,
            filter: getInitialFilter({
                currencies,
                managers,
                currencyMap,
                firms: items,
                services: items,
                nextFilter: {
                    managerName,
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

        fetchGet.mockResolvedValueOnce({
            data: {
                items: [],
                total_row_count: 0,
                totals: null,
                gtotals: null
            }
        });

        sagaTester.dispatch({ type: FILTER.APPLY });

        await sagaTester.waitFor(LIST.RECEIVE);

        expect(sagaTester.getState().list.acts).toEqual([]);
        expect(sagaTester.getState().list.totalCount).toEqual(0);
    });

    it('try with managerName without code', async () => {
        const initialState = {
            root,
            filter: getInitialFilter({
                currencies,
                managers,
                currencyMap,
                firms: items,
                services: items,
                nextFilter: {
                    managerName
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

        fetchGet.mockResolvedValueOnce({
            data: {
                items: [],
                total_row_count: 0,
                totals: null,
                gtotals: null
            }
        });

        sagaTester.dispatch({ type: FILTER.APPLY });

        await sagaTester.waitFor(LIST.RECEIVE);

        expect(sagaTester.getState().list.acts).toEqual([]);
        expect(sagaTester.getState().list.totalCount).toEqual(null);
    });

    it('try with client', async () => {
        const initialState = {
            root,
            filter: getInitialFilter({
                currencies,
                managers,
                currencyMap,
                firms: items,
                services: items,
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

        fetchGet.mockResolvedValueOnce({
            data: {
                items: [],
                total_row_count: 0,
                totals: null,
                gtotals: null
            }
        });

        sagaTester.dispatch({ type: FILTER.APPLY });

        await sagaTester.waitFor(LIST.RECEIVE);

        expect(sagaTester.getState().list.acts).toEqual([]);
        expect(sagaTester.getState().list.totalCount).toEqual(0);
    });

    it('try with person', async () => {
        const initialState = {
            root,
            filter: getInitialFilter({
                currencies,
                managers,
                currencyMap,
                firms: items,
                services: items,
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

        fetchGet.mockResolvedValueOnce({
            data: {
                items: [],
                total_row_count: 0,
                totals: null,
                gtotals: null
            }
        });

        sagaTester.dispatch({ type: FILTER.APPLY });

        await sagaTester.waitFor(LIST.RECEIVE);

        expect(sagaTester.getState().list.acts).toEqual([]);
        expect(sagaTester.getState().list.totalCount).toEqual(0);
    });

    it('try with currency', async () => {
        const initialState = {
            root,
            filter: getInitialFilter({
                currencies,
                managers,
                currencyMap,
                firms: items,
                services: items,
                nextFilter: {
                    currencyCode
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

        fetchGet.mockResolvedValueOnce({
            data: {
                items: [],
                total_row_count: 0,
                totals: null,
                gtotals: null
            }
        });

        sagaTester.dispatch({ type: FILTER.APPLY });

        await sagaTester.waitFor(LIST.RECEIVE);

        expect(sagaTester.getState().list.acts).toEqual([]);
        expect(sagaTester.getState().list.totalCount).toEqual(0);
    });

    it('try with firm', async () => {
        const initialState = {
            root,
            filter: getInitialFilter({
                currencies,
                managers,
                currencyMap,
                firms: items,
                services: items,
                nextFilter: {
                    firmId
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

        fetchGet.mockResolvedValueOnce({
            data: {
                items: [],
                total_row_count: 0,
                totals: null,
                gtotals: null
            }
        });

        sagaTester.dispatch({ type: FILTER.APPLY });

        await sagaTester.waitFor(LIST.RECEIVE);

        expect(sagaTester.getState().list.acts).toEqual([]);
        expect(sagaTester.getState().list.totalCount).toEqual(0);
    });

    it('try with service', async () => {
        const initialState = {
            root,
            filter: getInitialFilter({
                currencies,
                managers,
                currencyMap,
                firms: items,
                services: items,
                nextFilter: {
                    serviceId
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

        fetchGet.mockResolvedValueOnce({
            data: {
                items: [],
                total_row_count: 0,
                totals: null,
                gtotals: null
            }
        });

        sagaTester.dispatch({ type: FILTER.APPLY });

        await sagaTester.waitFor(LIST.RECEIVE);

        expect(sagaTester.getState().list.acts).toEqual([]);
        expect(sagaTester.getState().list.totalCount).toEqual(0);
    });

    it('try fetch page', async () => {
        const initialState = {
            root,
            filter: getInitialFilter({
                currencies,
                managers,
                currencyMap,
                firms: items,
                services: items,
                nextFilter: {
                    externalId
                },
                currentFilter: {
                    externalId
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

        fetchGet.mockResolvedValueOnce({
            data: {
                items: [],
                total_row_count: 0,
                totals: null,
                gtotals: null
            }
        });

        expect(sagaTester.getState().list.pageNumber).toEqual(1);

        sagaTester.dispatch({ type: LIST.FETCH_PAGE, nextPageNumber: 2 });

        await sagaTester.waitFor(LIST.RECEIVE);

        expect(sagaTester.getState().list.acts).toEqual([]);
        expect(sagaTester.getState().list.totalCount).toEqual(0);
        expect(sagaTester.getState().list.pageNumber).toEqual(2);
    });

    it('try resize page', async () => {
        const initialState = {
            root,
            filter: getInitialFilter({
                currencies,
                managers,
                currencyMap,
                firms: items,
                services: items,
                nextFilter: {
                    externalId
                },
                currentFilter: {
                    externalId
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

        fetchGet.mockResolvedValueOnce({
            data: {
                items: [],
                total_row_count: 0,
                totals: null,
                gtotals: null
            }
        });

        expect(sagaTester.getState().list.pageSize).toEqual(10);

        sagaTester.dispatch({ type: LIST.RESIZE_PAGE, nextPageSize: 20 });

        await sagaTester.waitFor(LIST.RECEIVE);

        expect(sagaTester.getState().list.acts).toEqual([]);
        expect(sagaTester.getState().list.totalCount).toEqual(0);
        expect(sagaTester.getState().list.pageSize).toEqual(20);
    });

    it('try apply history', async () => {
        const initialState = {
            root,
            filter: getInitialFilter({
                currencies,
                managers,
                currencyMap,
                firms: items,
                services: items,
                nextFilter: {
                    externalId
                },
                currentFilter: {
                    externalId
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

        fetchGet.mockResolvedValueOnce({
            data: {
                items: [],
                total_row_count: 0,
                totals: null,
                gtotals: null
            }
        });

        sagaTester.dispatch({
            type: HISTORY.APPLY_QS_LIST,
            list: {
                pageNumber: 3,
                pageSize: 40,
                sortKey: SORT_KEYS.PAYSYS_NAME,
                sortOrder: SortOrder.ASC
            }
        });

        expect(sagaTester.getState().list.pageNumber).toEqual(1);
        expect(sagaTester.getState().list.nextPageNumber).toEqual(3);
        expect(sagaTester.getState().list.pageSize).toEqual(10);
        expect(sagaTester.getState().list.nextPageSize).toEqual(40);
        expect(sagaTester.getState().list.currentSort.key).toEqual(SORT_KEYS.ACT_DT);
        expect(sagaTester.getState().list.currentSort.order).toEqual(SortOrder.DESC);
        expect(sagaTester.getState().list.nextSort.key).toEqual(SORT_KEYS.PAYSYS_NAME);
        expect(sagaTester.getState().list.nextSort.otrder).toEqual(SortOrder.ASK);

        await sagaTester.waitFor(LIST.RECEIVE);

        expect(sagaTester.getState().list.acts).toEqual([]);
        expect(sagaTester.getState().list.totalCount).toEqual(0);
        expect(sagaTester.getState().list.pageNumber).toEqual(3);
        expect(sagaTester.getState().list.nextPageNumber).toEqual(3);
        expect(sagaTester.getState().list.pageSize).toEqual(40);
        expect(sagaTester.getState().list.nextPageSize).toEqual(40);
        expect(sagaTester.getState().list.currentSort.key).toEqual(SORT_KEYS.PAYSYS_NAME);
        expect(sagaTester.getState().list.currentSort.otrder).toEqual(SortOrder.ASK);
        expect(sagaTester.getState().list.nextSort.key).toEqual(SORT_KEYS.PAYSYS_NAME);
        expect(sagaTester.getState().list.nextSort.otrder).toEqual(SortOrder.ASK);
    });

    describe('try resort', () => {
        afterEach(() => {
            jest.resetAllMocks();
        });

        it('try change sort key', async () => {
            const initialState = {
                root,
                filter: getInitialFilter({
                    currencies,
                    managers,
                    currencyMap,
                    firms: items,
                    services: items,
                    nextFilter: {
                        externalId
                    },
                    currentFilter: {
                        externalId
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

            fetchGet.mockResolvedValueOnce({
                data: {
                    items: [],
                    total_row_count: 0,
                    totals: null,
                    gtotals: null
                }
            });

            sagaTester.dispatch({
                type: LIST.SORT,
                sortKey: SORT_KEYS.PAYSYS_NAME
            });

            expect(sagaTester.getState().list.currentSort.key).toEqual(SORT_KEYS.ACT_DT);
            expect(sagaTester.getState().list.currentSort.order).toEqual(SortOrder.DESC);
            expect(sagaTester.getState().list.nextSort.key).toEqual(SORT_KEYS.PAYSYS_NAME);
            expect(sagaTester.getState().list.nextSort.order).toEqual(SortOrder.DESC);

            await sagaTester.waitFor(LIST.RECEIVE);

            expect(sagaTester.getState().list.acts).toEqual([]);
            expect(sagaTester.getState().list.totalCount).toEqual(0);

            expect(sagaTester.getState().list.currentSort.key).toEqual(SORT_KEYS.PAYSYS_NAME);
            expect(sagaTester.getState().list.currentSort.order).toEqual(SortOrder.DESC);
            expect(sagaTester.getState().list.nextSort.key).toEqual(SORT_KEYS.PAYSYS_NAME);
            expect(sagaTester.getState().list.nextSort.order).toEqual(SortOrder.DESC);
        });
    });
});
