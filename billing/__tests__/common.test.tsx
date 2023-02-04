import React from 'react';
import Enzyme, { mount } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { combineReducers, Reducer } from 'redux';
import { Provider } from 'react-redux';
import SagaTester from 'redux-saga-tester';
import { fetchGet, fetchPost } from 'common/utils/old-fetch';
import { List } from 'immutable';

import { HOST } from 'common/utils/test-utils/common';
import commonReducers from 'common/reducers/common';
import withIntlProvider from 'common/utils/test-utils/with-intl-provider';
import { InitialDataAction, ClientSelectorAction } from 'common/actions';
import { SortOrder } from 'common/types';
import { clientSelectorState as initialClientSelectorState } from 'common/reducers/client-selector';
import { PageSizes } from 'common/constants';
import { getLang } from 'common/utils/body-data';
import { request } from 'common/utils/request';

import { reducers } from '../reducers';
import { InitialState as InitialActionState } from '../reducers/action';
import { InitialState as InitialFilterState, FilterRecord } from '../reducers/filter';
import { InitialState as InitialListState, createCheckboxMap } from '../reducers/list';
import { RootRecord as InitialRootState } from '../reducers/root';
import { rootSaga } from '../sagas';
import {
    DefaultContract,
    DefaultSortOrder,
    DefaultSortKey,
    PaymentStatus,
    SortKey,
    Action,
    ActionResult,
    ALL_LIST_ITEMS
} from '../constants';
import { FilterAction, ListAction, ActionAction, AgencyAction } from '../actions';
import { FieldList as FilterFieldList } from '../components/Filter/types';
import { FieldList as ActionFieldList } from '../components/Action/types';
import { RootContainer } from '../containers/RootContainer';
import {
    serviceMap,
    immutableServiceMap,
    serviceList,
    contract,
    contractMap,
    immutableContractMap,
    contractList,
    agency,
    immutableAgency,
    rawItems,
    items,
    itemsWithSelectable,
    services as commonServices
} from './common.data';
import { fullPerms, services, intercompanies } from './data';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');
jest.mock('common/utils/body-data');

Enzyme.configure({ adapter: new Adapter() });

window['balance-tanker-error-map'] = {
    ru: {
        ['error-map']: {
            CREDIT_ALREADY_INVOICED:
                'Некоторые кредитные размещения уже были включены в счет: <sub f="invoice-eids"/>.'
        }
    }
};

describe('admin', () => {
    beforeAll(initializeDesktopRegistry);

    describe('deferpays', () => {
        afterEach(() => {
            jest.resetAllMocks();
        });

        describe('Initial data', () => {
            afterEach(() => {
                jest.resetAllMocks();
            });

            test('try to mount and fetch initial data', async () => {
                expect.assertions(3);

                // @ts-ignore
                getLang.mockReturnValue('ru');

                const initialState = {
                    perms: fullPerms,
                    root: InitialRootState(),
                    filter: InitialFilterState(),
                    list: InitialListState(),
                    action: InitialActionState(),
                    clientSelector: initialClientSelectorState
                };

                const rootReducer: Reducer = combineReducers({
                    ...commonReducers,
                    ...reducers
                });

                const sagaTester = new SagaTester({
                    initialState,
                    reducers: rootReducer
                });

                sagaTester.start(rootSaga);

                // @ts-ignore
                const store = sagaTester.store;

                const Container = withIntlProvider(() => (
                    <Provider store={store}>
                        <RootContainer />
                    </Provider>
                ));

                (request.get as jest.Mock)
                    .mockResolvedValueOnce(services.response)
                    .mockResolvedValueOnce(intercompanies.response);

                mount(<Container />);

                await sagaTester.waitFor(InitialDataAction.RECEIVE);

                expect(request.get).toBeCalledWith(services.request);
                expect(request.get).toBeCalledWith(intercompanies.request);

                const { filter } = sagaTester.getState();

                expect(filter.get('serviceMap').toJS()).toEqual(serviceMap);
            });

            test('try to fetch data with agency', async () => {
                expect.assertions(10);

                const url = `${HOST}/deferpays.xml`;
                // @ts-ignore
                delete global.window.location;
                // @ts-ignore
                global.window = Object.create(window);
                // @ts-ignore
                global.window.location = {
                    href: url,
                    search: '?client_id=1'
                };

                // @ts-ignore
                getLang.mockReturnValue('ru');

                const initialState = {
                    perms: fullPerms,
                    root: InitialRootState(),
                    filter: InitialFilterState(),
                    list: InitialListState(),
                    action: InitialActionState()
                };

                const rootReducer: Reducer = combineReducers({
                    ...commonReducers,
                    ...reducers
                });

                const sagaTester = new SagaTester({
                    initialState,
                    reducers: rootReducer
                });

                sagaTester.start(rootSaga);

                // @ts-ignore
                const store = sagaTester.store;

                const Container = withIntlProvider(() => (
                    <Provider store={store}>
                        <RootContainer />
                    </Provider>
                ));

                (request.get as jest.Mock)
                    .mockResolvedValueOnce(services.response)
                    .mockResolvedValueOnce(intercompanies.response);

                (fetchGet as jest.Mock)
                    .mockResolvedValueOnce({
                        data: {
                            id: 1,
                            name: 'clientName'
                        }
                    })
                    .mockResolvedValueOnce({
                        data: [
                            {
                                external_id: contract.externalId,
                                id: contract.id,
                                type: contract.type
                            }
                        ]
                    })
                    .mockResolvedValueOnce({
                        data: { total_row_count: 0, items: [] }
                    });

                mount(<Container />);

                await sagaTester.waitFor(InitialDataAction.RECEIVE);

                expect(request.get).toBeCalledWith(services.request);
                expect(request.get).toBeCalledWith(intercompanies.request);

                expect(fetchGet).toBeCalledWith(`${HOST}/client`, { client_id: 1 }, false, false);

                expect(fetchGet).toBeCalledWith(
                    `${HOST}/deferpay/contracts`,
                    { client_id: 1 },
                    false,
                    false
                );

                const { filter } = sagaTester.getState();

                expect(filter.get('serviceMap').toJS()).toEqual(serviceMap);
                expect(filter.get('contractMap').toJS()).toEqual(contractMap);
                expect(filter.getIn(['next', FilterFieldList.agency]).toJS()).toEqual(agency);

                // Должны подгрузиться данные
                await sagaTester.waitFor(ListAction.RECEIVE);

                expect(fetchGet).toBeCalledWith(
                    `${HOST}/deferpay/list`,
                    {
                        pagination_pn: 1,
                        contract_id: 0,
                        sort_key: DefaultSortKey,
                        payment_status: PaymentStatus.UNDEFINED,
                        pagination_ps: PageSizes[0],
                        sort_order: DefaultSortOrder,
                        client_id: 1
                    },
                    false,
                    false
                );

                const { list } = sagaTester.getState();

                expect(list.get('items').size).toEqual(0);
                expect(list.get('totalCount')).toBe(0);
            });
        });

        describe('Filter', () => {
            afterEach(() => {
                jest.resetAllMocks();
            });

            test('try to select agency', async () => {
                expect.assertions(3);

                // @ts-ignore
                getLang.mockReturnValue('ru');

                const initialState = {
                    perms: fullPerms,
                    root: InitialRootState({ isFetching: false }),
                    filter: InitialFilterState({
                        serviceMap: immutableServiceMap,
                        services: serviceList
                    }),
                    list: InitialListState(),
                    action: InitialActionState()
                };

                const rootReducer: Reducer = combineReducers({
                    ...commonReducers,
                    ...reducers
                });

                const sagaTester = new SagaTester({
                    initialState,
                    reducers: rootReducer
                });

                sagaTester.start(rootSaga);

                // @ts-ignore
                const store = sagaTester.store;

                const Container = withIntlProvider(() => (
                    <Provider store={store}>
                        <RootContainer />
                    </Provider>
                ));

                (fetchGet as jest.Mock)
                    .mockResolvedValueOnce({ data: [] })
                    .mockResolvedValueOnce({ data: [] });

                mount(<Container />);

                sagaTester.dispatch({
                    type: ClientSelectorAction.SELECT,
                    id: 1,
                    name: 'clientName'
                });

                await sagaTester.waitFor(AgencyAction.RECEIVE);

                expect(fetchGet).toBeCalledWith(
                    `${HOST}/deferpay/contracts`,
                    { client_id: 1 },
                    false,
                    false
                );

                const { filter } = sagaTester.getState();

                expect(filter.get('contractMap').toJS()).toEqual({
                    [DefaultContract.id]: DefaultContract
                });
                expect(filter.get('next').get(FilterFieldList.agency).toJS()).toEqual({
                    id: 1,
                    name: 'clientName'
                });
            });

            test('try select data and get list', async () => {
                expect.assertions(9);

                // @ts-ignore
                getLang.mockReturnValue('ru');

                const initialState = {
                    perms: fullPerms,
                    root: InitialRootState({ isFetching: false }),
                    filter: InitialFilterState({
                        serviceMap: immutableServiceMap,
                        services: serviceList,
                        contractMap: immutableContractMap,
                        contracts: contractList,
                        next: FilterRecord({
                            agency: immutableAgency
                        })
                    }),
                    list: InitialListState(),
                    action: InitialActionState()
                };

                const rootReducer: Reducer = combineReducers({
                    ...commonReducers,
                    ...reducers
                });

                const sagaTester = new SagaTester({
                    initialState,
                    reducers: rootReducer
                });

                sagaTester.start(rootSaga);

                // @ts-ignore
                const store = sagaTester.store;

                const Container = withIntlProvider(() => (
                    <Provider store={store}>
                        <RootContainer />
                    </Provider>
                ));

                const wrapper = mount(<Container />);

                sagaTester.dispatch({
                    type: FilterAction.FIELD_CHANGE,
                    name: FilterFieldList.paymentStatus,
                    value: PaymentStatus.ALL_ORDERS
                });

                expect(
                    sagaTester.getState().filter.get('next').get(FilterFieldList.paymentStatus)
                ).toEqual(PaymentStatus.ALL_ORDERS);

                sagaTester.dispatch({
                    type: FilterAction.FIELD_CHANGE,
                    name: FilterFieldList.service,
                    value: commonServices[0].id
                });

                expect(
                    sagaTester.getState().filter.get('next').get(FilterFieldList.service)
                ).toEqual(commonServices[0].id);

                sagaTester.dispatch({
                    type: FilterAction.FIELD_CHANGE,
                    name: FilterFieldList.contract,
                    value: 100
                });

                expect(
                    sagaTester.getState().filter.get('next').get(FilterFieldList.contract)
                ).toEqual(100);

                sagaTester.dispatch({
                    type: FilterAction.FIELD_CHANGE,
                    name: FilterFieldList.dateFrom,
                    value: '2020-01-09T00:00:00'
                });

                expect(
                    sagaTester.getState().filter.get('next').get(FilterFieldList.dateFrom)
                ).toEqual('2020-01-09T00:00:00');

                sagaTester.dispatch({
                    type: FilterAction.FIELD_CHANGE,
                    name: FilterFieldList.dateTo,
                    value: '2020-01-15T00:00:00'
                });

                expect(
                    sagaTester.getState().filter.get('next').get(FilterFieldList.dateTo)
                ).toEqual('2020-01-15T00:00:00');

                wrapper.find('.yb-deferpays-search__order-id Textinput input').simulate('change', {
                    target: {
                        value: '50',
                        name: 'serviceOrderId',
                        id: 'deferpays-order-id-input'
                    }
                });

                await sagaTester.waitFor(FilterAction.FIELD_CHANGE);

                expect(
                    sagaTester.getState().filter.get('next').get(FilterFieldList.serviceOrderId)
                ).toEqual('50');

                // @ts-ignore
                fetchGet.mockResolvedValueOnce({
                    data: { total_row_count: 0, items: [] }
                });

                wrapper.find('.yb-search-filter form').simulate('submit');

                await sagaTester.waitFor(ListAction.RECEIVE);

                expect(fetchGet).toBeCalledWith(
                    `${HOST}/deferpay/list`,
                    {
                        pagination_pn: 1,
                        contract_id: 100,
                        dt_from: '2020-01-09T00:00:00',
                        dt_to: '2020-01-15T00:00:00',
                        sort_key: DefaultSortKey,
                        payment_status: PaymentStatus.ALL_ORDERS,
                        service_cc: 'adfox',
                        service_order_id: '50',
                        pagination_ps: PageSizes[0],
                        sort_order: DefaultSortOrder,
                        client_id: 1
                    },
                    false,
                    false
                );

                const { list } = sagaTester.getState();

                expect(list.get('items').size).toEqual(0);
                expect(list.get('totalCount')).toBe(0);
            });
        });

        describe('List', () => {
            afterEach(() => {
                jest.resetAllMocks();
            });

            test('try to change page', async () => {
                // expect.assertions(3);

                // @ts-ignore
                getLang.mockReturnValue('ru');

                const usedItems = items.slice(0, 10);

                const initialState = {
                    perms: fullPerms,
                    root: InitialRootState({ isFetching: false }),
                    filter: InitialFilterState({
                        serviceMap: immutableServiceMap,
                        services: serviceList,
                        contractMap: immutableContractMap,
                        next: FilterRecord({
                            agency: immutableAgency
                        }),
                        current: FilterRecord({
                            agency: immutableAgency
                        })
                    }),
                    list: InitialListState({
                        totalCount: 1000,
                        items: List(usedItems),
                        checkboxMap: createCheckboxMap(usedItems)
                    }),
                    action: InitialActionState()
                };

                const rootReducer: Reducer = combineReducers({
                    ...commonReducers,
                    ...reducers
                });

                const sagaTester = new SagaTester({
                    initialState,
                    reducers: rootReducer
                });

                sagaTester.start(rootSaga);

                // @ts-ignore
                const store = sagaTester.store;

                const Container = withIntlProvider(() => (
                    <Provider store={store}>
                        <RootContainer />
                    </Provider>
                ));

                mount(<Container />);

                // @ts-ignore
                fetchGet.mockResolvedValueOnce({
                    data: { total_row_count: 1000, items: rawItems.slice(10) }
                });

                sagaTester.dispatch({
                    type: ListAction.PAGE_NUMBER_CHANGE,
                    pageNumber: 2,
                    shouldApplyFilterToHistory: true
                });

                await sagaTester.waitFor(ListAction.RECEIVE);

                expect(fetchGet).toBeCalledWith(
                    `${HOST}/deferpay/list`,
                    {
                        pagination_pn: 2,
                        contract_id: 0,
                        payment_status: PaymentStatus.UNDEFINED,
                        pagination_ps: PageSizes[0],
                        sort_key: DefaultSortKey,
                        sort_order: DefaultSortOrder,
                        client_id: 1
                    },
                    false,
                    false
                );

                const { list } = sagaTester.getState();
                expect(list.get('items').toArray()).toEqual(items.slice(10));
                expect(list.getIn(['current', 'pageNumber'])).toEqual(2);
            });

            test('try to resize page', async () => {
                expect.assertions(3);

                // @ts-ignore
                getLang.mockReturnValue('ru');

                const usedItems = items.slice(0, 10);

                const initialState = {
                    perms: fullPerms,
                    root: InitialRootState({ isFetching: false }),
                    filter: InitialFilterState({
                        serviceMap: immutableServiceMap,
                        services: serviceList,
                        contractMap: immutableContractMap,
                        contracts: contractList,
                        next: FilterRecord({
                            agency: immutableAgency
                        }),
                        current: FilterRecord({
                            agency: immutableAgency
                        })
                    }),
                    list: InitialListState({
                        totalCount: 1000,
                        items: List(usedItems),
                        checkboxMap: createCheckboxMap(usedItems)
                    }),
                    action: InitialActionState()
                };

                const rootReducer: Reducer = combineReducers({
                    ...commonReducers,
                    ...reducers
                });

                const sagaTester = new SagaTester({
                    initialState,
                    reducers: rootReducer
                });

                sagaTester.start(rootSaga);

                // @ts-ignore
                const store = sagaTester.store;

                const Container = withIntlProvider(() => (
                    <Provider store={store}>
                        <RootContainer />
                    </Provider>
                ));

                mount(<Container />);

                // @ts-ignore
                fetchGet.mockResolvedValueOnce({
                    data: { total_row_count: 1000, items: rawItems }
                });

                sagaTester.dispatch({
                    type: ListAction.PAGE_SIZE_CHANGE,
                    pageSize: 25,
                    shouldApplyFilterToHistory: true
                });

                await sagaTester.waitFor(ListAction.RECEIVE);

                expect(fetchGet).toBeCalledWith(
                    `${HOST}/deferpay/list`,
                    {
                        pagination_pn: 1,
                        contract_id: 0,
                        payment_status: PaymentStatus.UNDEFINED,
                        pagination_ps: 25,
                        sort_key: DefaultSortKey,
                        sort_order: DefaultSortOrder,
                        client_id: 1
                    },
                    false,
                    false
                );

                const { list } = sagaTester.getState();
                expect(list.get('items').toArray()).toEqual(items);
                expect(list.getIn(['current', 'pageSize'])).toEqual(25);
            });

            test('Try change sort key', async () => {
                expect.assertions(4);

                // @ts-ignore
                getLang.mockReturnValue('ru');

                const usedItems = items.slice(0, 10);

                const initialState = {
                    perms: fullPerms,
                    root: InitialRootState({ isFetching: false }),
                    filter: InitialFilterState({
                        serviceMap: immutableServiceMap,
                        services: serviceList,
                        contractMap: immutableContractMap,
                        contracts: contractList,
                        next: FilterRecord({
                            agency: immutableAgency
                        }),
                        current: FilterRecord({
                            agency: immutableAgency
                        })
                    }),
                    list: InitialListState({
                        totalCount: 1000,
                        items: List(usedItems),
                        checkboxMap: createCheckboxMap(usedItems)
                    }),
                    action: InitialActionState()
                };

                const rootReducer: Reducer = combineReducers({
                    ...commonReducers,
                    ...reducers
                });

                const sagaTester = new SagaTester({
                    initialState,
                    reducers: rootReducer
                });

                sagaTester.start(rootSaga);

                // @ts-ignore
                const store = sagaTester.store;

                const Container = withIntlProvider(() => (
                    <Provider store={store}>
                        <RootContainer />
                    </Provider>
                ));

                mount(<Container />);

                // @ts-ignore
                fetchGet.mockResolvedValueOnce({
                    data: { total_row_count: 0, items: [] }
                });

                sagaTester.dispatch({
                    type: ListAction.RESORT,
                    sortKey: SortKey.REPAYMENT_DT
                });

                await sagaTester.waitFor(ListAction.RECEIVE);

                expect(fetchGet).toBeCalledWith(
                    `${HOST}/deferpay/list`,
                    {
                        pagination_pn: 1,
                        contract_id: 0,
                        payment_status: PaymentStatus.UNDEFINED,
                        pagination_ps: PageSizes[0],
                        sort_key: SortKey.REPAYMENT_DT,
                        sort_order: DefaultSortOrder,
                        client_id: 1
                    },
                    false,
                    false
                );

                const { list } = sagaTester.getState();
                expect(list.get('items').toArray()).toEqual([]);
                expect(list.getIn(['current', 'sort', 'key'])).toEqual(SortKey.REPAYMENT_DT);
                expect(list.getIn(['current', 'sort', 'order'])).toEqual(DefaultSortOrder);
            });

            test('Try change sort order', async () => {
                expect.assertions(4);

                // @ts-ignore
                getLang.mockReturnValue('ru');

                const usedItems = items.slice(0, 10);

                const initialState = {
                    perms: fullPerms,
                    root: InitialRootState({ isFetching: false }),
                    filter: InitialFilterState({
                        serviceMap: immutableServiceMap,
                        services: serviceList,
                        contractMap: immutableContractMap,
                        contracts: contractList,
                        next: FilterRecord({
                            agency: immutableAgency
                        }),
                        current: FilterRecord({
                            agency: immutableAgency
                        })
                    }),
                    list: InitialListState({
                        totalCount: 1000,
                        items: List(usedItems),
                        checkboxMap: createCheckboxMap(usedItems)
                    }),
                    action: InitialActionState()
                };

                const rootReducer: Reducer = combineReducers({
                    ...commonReducers,
                    ...reducers
                });

                const sagaTester = new SagaTester({
                    initialState,
                    reducers: rootReducer
                });

                sagaTester.start(rootSaga);

                // @ts-ignore
                const store = sagaTester.store;

                const Container = withIntlProvider(() => (
                    <Provider store={store}>
                        <RootContainer />
                    </Provider>
                ));

                mount(<Container />);

                // @ts-ignore
                fetchGet.mockResolvedValueOnce({
                    data: { total_row_count: 0, items: [] }
                });

                sagaTester.dispatch({
                    type: ListAction.RESORT,
                    sortKey: DefaultSortKey
                });

                await sagaTester.waitFor(ListAction.RECEIVE);

                expect(fetchGet).toBeCalledWith(
                    `${HOST}/deferpay/list`,
                    {
                        pagination_pn: 1,
                        contract_id: 0,
                        payment_status: PaymentStatus.UNDEFINED,
                        pagination_ps: PageSizes[0],
                        sort_key: DefaultSortKey,
                        sort_order: SortOrder.ASC,
                        client_id: 1
                    },
                    false,
                    false
                );

                const { list } = sagaTester.getState();
                expect(list.get('items').toArray()).toEqual([]);
                expect(list.getIn(['current', 'sort', 'key'])).toEqual(DefaultSortKey);
                expect(list.getIn(['current', 'sort', 'order'])).toEqual(SortOrder.ASC);
            });

            test('Try select item', () => {
                expect.assertions(2);

                // @ts-ignore
                getLang.mockReturnValue('ru');

                const initialState = {
                    perms: fullPerms,
                    root: InitialRootState({ isFetching: false }),
                    filter: InitialFilterState({
                        serviceMap: immutableServiceMap,
                        services: serviceList,
                        contractMap: immutableContractMap,
                        contracts: contractList,
                        next: FilterRecord({
                            agency: immutableAgency
                        }),
                        current: FilterRecord({
                            agency: immutableAgency
                        })
                    }),
                    list: InitialListState({
                        totalCount: 1000,
                        items: List(itemsWithSelectable),
                        checkboxMap: createCheckboxMap(itemsWithSelectable)
                    }),
                    action: InitialActionState()
                };

                const rootReducer: Reducer = combineReducers({
                    ...commonReducers,
                    ...reducers
                });

                const sagaTester = new SagaTester({
                    initialState,
                    reducers: rootReducer
                });

                sagaTester.start(rootSaga);

                // @ts-ignore
                const store = sagaTester.store;

                const Container = withIntlProvider(() => (
                    <Provider store={store}>
                        <RootContainer />
                    </Provider>
                ));

                const wrapper = mount(<Container />);

                const [{ deferpayId }] = itemsWithSelectable;

                wrapper
                    .find(`.yb-deferpays-table__checkbox-${deferpayId} input`)
                    .simulate('change');

                const { list } = sagaTester.getState();
                expect(list.getIn(['checkboxMap', deferpayId, 'value'])).toEqual(true);

                wrapper
                    .find(`.yb-deferpays-table__checkbox-${deferpayId} input`)
                    .simulate('change');

                const { list: newList } = sagaTester.getState();
                expect(newList.getIn(['checkboxMap', deferpayId, 'value'])).toEqual(false);
            });

            test('Try select all items', () => {
                const checkboxMap = createCheckboxMap(itemsWithSelectable);
                expect.assertions(2 * checkboxMap.size);

                // @ts-ignore
                getLang.mockReturnValue('ru');

                const initialState = {
                    perms: fullPerms,
                    root: InitialRootState({ isFetching: false }),
                    filter: InitialFilterState({
                        serviceMap: immutableServiceMap,
                        services: serviceList,
                        contractMap: immutableContractMap,
                        contracts: contractList,
                        next: FilterRecord({
                            agency: immutableAgency
                        }),
                        current: FilterRecord({
                            agency: immutableAgency
                        })
                    }),
                    list: InitialListState({
                        totalCount: 4,
                        items: List(itemsWithSelectable),
                        checkboxMap
                    }),
                    action: InitialActionState()
                };

                const rootReducer: Reducer = combineReducers({
                    ...commonReducers,
                    ...reducers
                });

                const sagaTester = new SagaTester({
                    initialState,
                    reducers: rootReducer
                });

                sagaTester.start(rootSaga);

                // @ts-ignore
                const store = sagaTester.store;

                const Container = withIntlProvider(() => (
                    <Provider store={store}>
                        <RootContainer />
                    </Provider>
                ));

                const wrapper = mount(<Container />);

                wrapper.find(`input#deferpays_list__checkbox-${ALL_LIST_ITEMS}`).simulate('change');

                const { list } = sagaTester.getState();

                const keys = Object.keys(checkboxMap.toJS());

                keys.forEach(key => expect(list.getIn(['checkboxMap', key, 'value'])).toBe(true));

                wrapper.find(`input#deferpays_list__checkbox-${ALL_LIST_ITEMS}`).simulate('change');

                const { list: newList } = sagaTester.getState();

                keys.forEach(key =>
                    expect(newList.getIn(['checkboxMap', key, 'value'])).toBe(false)
                );
            });
        });

        describe('Action', () => {
            afterEach(() => {
                jest.resetAllMocks();
            });

            test('try to pay of', async () => {
                expect.assertions(3);

                // @ts-ignore
                getLang.mockReturnValue('ru');

                const [{ deferpayId }] = itemsWithSelectable;

                const initialState = {
                    perms: fullPerms,
                    root: InitialRootState({ isFetching: false }),
                    filter: InitialFilterState({
                        serviceMap: immutableServiceMap,
                        services: serviceList,
                        contractMap: immutableContractMap,
                        contracts: contractList,
                        next: FilterRecord({
                            agency: immutableAgency
                        }),
                        current: FilterRecord({
                            agency: immutableAgency
                        })
                    }),
                    list: InitialListState({
                        totalCount: 4,
                        items: List(itemsWithSelectable),
                        checkboxMap: createCheckboxMap(itemsWithSelectable).setIn(
                            [deferpayId, 'value'],
                            true
                        )
                    }),
                    action: InitialActionState()
                };

                const rootReducer: Reducer = combineReducers({
                    ...commonReducers,
                    ...reducers
                });

                const sagaTester = new SagaTester({
                    initialState,
                    reducers: rootReducer
                });

                sagaTester.start(rootSaga);

                // @ts-ignore
                const store = sagaTester.store;

                const Container = withIntlProvider(() => (
                    <Provider store={store}>
                        <RootContainer />
                    </Provider>
                ));

                const wrapper = mount(<Container />);

                // @ts-ignore
                fetchPost.mockResolvedValueOnce({
                    data: {}
                });

                // @ts-ignore
                fetchGet.mockResolvedValueOnce({
                    data: { total_row_count: 0, items: [] }
                });

                sagaTester.dispatch({
                    type: ActionAction.FIELD_CHANGE,
                    name: ActionFieldList.action,
                    value: Action.PAY_OFF
                });
                sagaTester.dispatch({
                    type: ActionAction.FIELD_CHANGE,
                    name: ActionFieldList.date,
                    value: '2020-02-13T00:00:00'
                });

                wrapper.find(`form#deferpays-action-form`).simulate('submit');

                await sagaTester.waitFor(ActionAction.RECEIVE);

                expect(fetchPost).toBeCalledWith(
                    `${HOST}/deferpay/action/repayment-invoice`,
                    {
                        deferpay_ids: deferpayId,
                        invoice_dt: '2020-02-13T00:00:00',
                        _csrf: 'csrf'
                    },
                    false
                );

                await sagaTester.waitFor(ListAction.RECEIVE);

                expect(fetchGet).toBeCalledWith(
                    `${HOST}/deferpay/list`,
                    {
                        pagination_pn: 1,
                        contract_id: 0,
                        payment_status: PaymentStatus.UNDEFINED,
                        pagination_ps: PageSizes[0],
                        sort_key: DefaultSortKey,
                        sort_order: DefaultSortOrder,
                        client_id: 1
                    },
                    false,
                    false
                );

                expect(sagaTester.getState().action.get('result')).toBe(ActionResult.SUCCESS);
            });

            test('try to confirm', async () => {
                expect.assertions(3);

                // @ts-ignore
                getLang.mockReturnValue('ru');

                const [, { deferpayId }] = itemsWithSelectable;

                const initialState = {
                    perms: fullPerms,
                    root: InitialRootState({ isFetching: false }),
                    filter: InitialFilterState({
                        serviceMap: immutableServiceMap,
                        services: serviceList,
                        contractMap: immutableContractMap,
                        contracts: contractList,
                        next: FilterRecord({
                            agency: immutableAgency
                        }),
                        current: FilterRecord({
                            agency: immutableAgency
                        })
                    }),
                    list: InitialListState({
                        totalCount: 4,
                        items: List(itemsWithSelectable),
                        checkboxMap: createCheckboxMap(itemsWithSelectable).setIn(
                            [deferpayId, 'value'],
                            true
                        )
                    }),
                    action: InitialActionState()
                };

                const rootReducer: Reducer = combineReducers({
                    ...commonReducers,
                    ...reducers
                });

                const sagaTester = new SagaTester({
                    initialState,
                    reducers: rootReducer
                });

                sagaTester.start(rootSaga);

                // @ts-ignore
                const store = sagaTester.store;

                const Container = withIntlProvider(() => (
                    <Provider store={store}>
                        <RootContainer />
                    </Provider>
                ));

                const wrapper = mount(<Container />);

                // @ts-ignore
                fetchPost.mockResolvedValueOnce({
                    data: {}
                });

                // @ts-ignore
                fetchGet.mockResolvedValueOnce({
                    data: { total_row_count: 0, items: [] }
                });

                sagaTester.dispatch({
                    type: ActionAction.FIELD_CHANGE,
                    name: ActionFieldList.action,
                    value: Action.CONFIRM
                });

                wrapper.find(`form#deferpays-action-form`).simulate('submit');

                await sagaTester.waitFor(ActionAction.RECEIVE);

                expect(fetchPost).toBeCalledWith(
                    `${HOST}/deferpay/action/confirm-invoices`,
                    {
                        deferpay_ids: deferpayId,
                        _csrf: 'csrf'
                    },
                    false
                );

                await sagaTester.waitFor(ListAction.RECEIVE);

                expect(fetchGet).toBeCalledWith(
                    `${HOST}/deferpay/list`,
                    {
                        pagination_pn: 1,
                        contract_id: 0,
                        payment_status: PaymentStatus.UNDEFINED,
                        pagination_ps: PageSizes[0],
                        sort_key: DefaultSortKey,
                        sort_order: DefaultSortOrder,
                        client_id: 1
                    },
                    false,
                    false
                );

                expect(sagaTester.getState().action.get('result')).toBe(ActionResult.SUCCESS);
            });

            test('try to delete', async () => {
                expect.assertions(3);

                // @ts-ignore
                getLang.mockReturnValue('ru');

                const [, { deferpayId }] = itemsWithSelectable;

                const initialState = {
                    perms: fullPerms,
                    root: InitialRootState({ isFetching: false }),
                    filter: InitialFilterState({
                        serviceMap: immutableServiceMap,
                        services: serviceList,
                        contractMap: immutableContractMap,
                        contracts: contractList,
                        next: FilterRecord({
                            agency: immutableAgency
                        }),
                        current: FilterRecord({
                            agency: immutableAgency
                        })
                    }),
                    list: InitialListState({
                        totalCount: 4,
                        items: List(itemsWithSelectable),
                        checkboxMap: createCheckboxMap(itemsWithSelectable).setIn(
                            [deferpayId, 'value'],
                            true
                        )
                    }),
                    action: InitialActionState()
                };

                const rootReducer: Reducer = combineReducers({
                    ...commonReducers,
                    ...reducers
                });

                const sagaTester = new SagaTester({
                    initialState,
                    reducers: rootReducer
                });

                sagaTester.start(rootSaga);

                // @ts-ignore
                const store = sagaTester.store;

                const Container = withIntlProvider(() => (
                    <Provider store={store}>
                        <RootContainer />
                    </Provider>
                ));

                const wrapper = mount(<Container />);

                // @ts-ignore
                fetchPost.mockResolvedValueOnce({
                    data: {}
                });

                // @ts-ignore
                fetchGet.mockResolvedValueOnce({
                    data: { total_row_count: 0, items: [] }
                });

                sagaTester.dispatch({
                    type: ActionAction.FIELD_CHANGE,
                    name: ActionFieldList.action,
                    value: Action.DELETE
                });

                wrapper.find(`form#deferpays-action-form`).simulate('submit');

                await sagaTester.waitFor(ActionAction.RECEIVE);

                expect(fetchPost).toBeCalledWith(
                    `${HOST}/deferpay/action/decline-invoices`,
                    {
                        deferpay_ids: `${deferpayId}`,
                        _csrf: 'csrf'
                    },
                    false
                );

                await sagaTester.waitFor(ListAction.RECEIVE);

                expect(fetchGet).toBeCalledWith(
                    `${HOST}/deferpay/list`,
                    {
                        pagination_pn: 1,
                        contract_id: 0,
                        payment_status: PaymentStatus.UNDEFINED,
                        pagination_ps: PageSizes[0],
                        sort_key: DefaultSortKey,
                        sort_order: DefaultSortOrder,
                        client_id: 1
                    },
                    false,
                    false
                );

                expect(sagaTester.getState().action.get('result')).toBe(ActionResult.SUCCESS);
            });

            test('try to get error', async () => {
                expect.assertions(3);

                // @ts-ignore
                getLang.mockReturnValue('ru');

                const [, { deferpayId }] = itemsWithSelectable;

                const initialState = {
                    perms: fullPerms,
                    root: InitialRootState({ isFetching: false }),
                    filter: InitialFilterState({
                        serviceMap: immutableServiceMap,
                        services: serviceList,
                        contractMap: immutableContractMap,
                        contracts: contractList,
                        next: FilterRecord({
                            agency: immutableAgency
                        }),
                        current: FilterRecord({
                            agency: immutableAgency
                        })
                    }),
                    list: InitialListState({
                        totalCount: 4,
                        items: List(itemsWithSelectable),
                        checkboxMap: createCheckboxMap(itemsWithSelectable).setIn(
                            [deferpayId, 'value'],
                            true
                        )
                    }),
                    action: InitialActionState()
                };

                const rootReducer: Reducer = combineReducers({
                    ...commonReducers,
                    ...reducers
                });

                const sagaTester = new SagaTester({
                    initialState,
                    reducers: rootReducer
                });

                sagaTester.start(rootSaga);

                // @ts-ignore
                const store = sagaTester.store;

                const Container = withIntlProvider(() => (
                    <Provider store={store}>
                        <RootContainer />
                    </Provider>
                ));

                const wrapper = mount(<Container />);

                const error = new Error();

                // @ts-ignore
                error.data = {
                    error: 'CREDIT_ALREADY_INVOICED',
                    tankerContext: {}
                };

                // @ts-ignore
                fetchPost.mockRejectedValueOnce(error);

                // @ts-ignore
                fetchGet.mockResolvedValueOnce({
                    data: { total_row_count: 0, items: [] }
                });

                sagaTester.dispatch({
                    type: ActionAction.FIELD_CHANGE,
                    name: ActionFieldList.action,
                    value: Action.DELETE
                });

                wrapper.find(`form#deferpays-action-form`).simulate('submit');

                await sagaTester.waitFor(ActionAction.ERROR);

                expect(fetchPost).toBeCalledWith(
                    `${HOST}/deferpay/action/decline-invoices`,
                    {
                        deferpay_ids: `${deferpayId}`,
                        _csrf: 'csrf'
                    },
                    false
                );

                const { action } = sagaTester.getState();
                expect(action.get('result')).toBe(ActionResult.FAIL);
                expect(action.get('data').toJS()).toEqual({
                    id: 'CREDIT_ALREADY_INVOICED',
                    context: {}
                });
            });
        });
    });
});
