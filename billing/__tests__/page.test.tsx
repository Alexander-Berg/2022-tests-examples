import React from 'react';
import Enzyme, { mount } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { combineReducers, Reducer } from 'redux';
import { Provider } from 'react-redux';
import { all } from 'redux-saga/effects';
import SagaTester from 'redux-saga-tester';
import { fetchGet, fetchPost } from 'common/utils/old-fetch';
import { fromJS } from 'immutable';

import { camelCasePropNames } from 'common/utils';
import { HOST } from 'common/utils/test-utils/common';
import commonReducers from 'common/reducers/common';
import withIntlProvider from 'common/utils/test-utils/with-intl-provider';
import { InitialDataAction } from 'common/actions';
import { reducers } from '../reducers';
import { RootRecord } from '../reducers/root';
import { FilterStateRecord, FilterRecord } from '../reducers/filter';
import { ListStateRecord } from '../reducers/list';
import { StatusItem } from '../reducers/types';
import { watchFetchStatus } from '../sagas/get-status';
import { watchFetchInitialData } from '../sagas/initial-data';
import { watchFetchRescedule } from '../sagas/reschedule';
import { fromQSToState } from '../history';
import { FILTER, STATUS, RESCHEDULE } from '../actions';
import { RootContainer } from '../containers/RootContainer';
import {
    perms,
    initialResponce,
    serviceMap,
    serviceItems,
    nonBlockedStatuses,
    blockedStatuses,
    months
} from './data';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');
jest.mock('../history');

function* rootSaga() {
    yield all([watchFetchInitialData(), watchFetchRescedule(), watchFetchStatus()]);
}

Enzyme.configure({ adapter: new Adapter() });

describe('admin - completions - containers - root', () => {
    beforeAll(initializeDesktopRegistry);

    afterEach(() => {
        jest.resetAllMocks();
    });

    test('open page - should request initial data', async () => {
        expect.assertions(7);

        const initialState = {
            perms,
            root: RootRecord(),
            filter: FilterStateRecord(),
            list: ListStateRecord()
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

        // @ts-ignore
        fromQSToState.mockReturnValue({});
        // @ts-ignore
        fetchGet.mockResolvedValueOnce(initialResponce);

        mount(<Container />);

        // Отправляется запрос на получение начальных данных
        await sagaTester.waitFor(InitialDataAction.REQUEST);

        expect(fetchGet).toBeCalledWith(
            `${HOST}/completions/list_services`,
            undefined,
            false,
            false
        );

        expect(sagaTester.getState().root.get('isFetching')).toBe(true);

        await sagaTester.waitFor(InitialDataAction.RECEIVE);

        const { root, filter } = sagaTester.getState();

        expect(root.get('isFetching')).toBe(false);
        expect(root.get('serviceMap')).toEqual(serviceMap);
        expect(filter.get('serviceItems').toJS()).toEqual(serviceItems);
        expect(filter.get('next').equals(FilterRecord())).toBe(true);

        await sagaTester.waitFor(STATUS.NO_FETCH);

        expect(sagaTester.getState().list.get('isFetching')).toBe(false);
    });

    test('open page - should request initial data with history', async () => {
        expect.assertions(11);

        const initialState = {
            perms,
            root: RootRecord(),
            filter: FilterStateRecord(),
            list: ListStateRecord()
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

        // @ts-ignore
        fromQSToState.mockReturnValue({
            next: {
                month: months[0],
                service: 1,
                preaggregate: true
            }
        });
        // @ts-ignore
        fetchGet.mockResolvedValueOnce(initialResponce);

        mount(<Container />);

        // Отправляется запрос на получение начальных данных
        await sagaTester.waitFor(InitialDataAction.REQUEST);

        expect(fetchGet).toBeCalledWith(
            `${HOST}/completions/list_services`,
            undefined,
            false,
            false
        );

        expect(sagaTester.getState().root.get('isFetching')).toBe(true);

        //Поскольку страница заполнена, подтягиваем статусы
        // @ts-ignore
        fetchGet.mockResolvedValueOnce(nonBlockedStatuses);
        await sagaTester.waitFor(STATUS.RECEIVE);

        expect(fetchGet).toBeCalledWith(
            `${HOST}/completions/get_status`,
            { service_id: 1, start_dt: '2019-05-01', end_dt: '2019-05-31' },
            false,
            false
        );

        const { root, filter, list } = sagaTester.getState();

        expect(root.get('isFetching')).toBe(false);
        expect(root.get('serviceMap')).toEqual(serviceMap);
        expect(filter.get('serviceItems').toJS()).toEqual(serviceItems);
        expect(filter.getIn(['next', 'service'])).toBe(1);
        expect(filter.getIn(['next', 'month'])).toBe(months[0]);
        expect(filter.getIn(['next', 'preaggregate'])).toBe(true);
        expect(list.get('isFetching')).toBe(false);
        expect(list.get('items')).toEqual(camelCasePropNames(nonBlockedStatuses.data));
    });

    test('select month without service - should not request status', async () => {
        expect.assertions(2);

        const initialState = {
            perms,
            root: RootRecord({ serviceMap, isFetching: false }),
            filter: FilterStateRecord({
                serviceItems: fromJS(serviceItems),
                next: FilterRecord({ month: months[0] })
            }),
            list: ListStateRecord()
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

        // Имитируем выбор месяца назначения через выпадающий список.
        sagaTester.dispatch({
            type: FILTER.FIELD_CHANGED,
            field: { name: 'month', value: months[1] }
        });

        const { filter } = sagaTester.getState();
        expect(filter.getIn(['next', 'service'])).toBe('');
        expect(filter.getIn(['next', 'month'])).toBe(months[1]);

        await sagaTester.waitFor(STATUS.NO_FETCH);
    });

    test('select service - should request status', async () => {
        expect.assertions(9);

        const initialState = {
            perms,
            root: RootRecord({ isFetching: false, serviceMap }),
            filter: FilterStateRecord({
                serviceItems: fromJS(serviceItems),
                next: FilterRecord({ month: months[0] })
            }),
            list: ListStateRecord()
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
        fetchGet.mockResolvedValueOnce(nonBlockedStatuses);
        // Имитируем выбор сервиса назначения через выпадающий список.
        sagaTester.dispatch({
            type: FILTER.FIELD_CHANGED,
            field: { name: 'service', value: 1 }
        });

        await sagaTester.waitFor(STATUS.RECEIVE);

        expect(fetchGet).toBeCalledWith(
            `${HOST}/completions/get_status`,
            { service_id: 1, start_dt: '2019-05-01', end_dt: '2019-05-31' },
            false,
            false
        );

        const { root, filter, list } = sagaTester.getState();

        expect(root.get('isFetching')).toBe(false);
        expect(root.get('serviceMap')).toEqual(serviceMap);
        expect(filter.get('serviceItems').toJS()).toEqual(serviceItems);
        expect(filter.getIn(['next', 'service'])).toBe(1);
        expect(filter.getIn(['next', 'month'])).toBe(months[0]);
        expect(filter.getIn(['next', 'preaggregate'])).toBe(false);
        expect(list.get('isFetching')).toBe(false);
        expect(list.get('items')).toEqual(camelCasePropNames(nonBlockedStatuses.data));
    });

    test('select month with existing service - should request status', async () => {
        expect.assertions(9);

        const initialState = {
            perms,
            root: RootRecord({ serviceMap, isFetching: false }),
            filter: FilterStateRecord({
                serviceItems: fromJS(serviceItems),
                next: FilterRecord({ month: months[0], service: 1 })
            }),
            list: ListStateRecord()
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
        fetchGet.mockResolvedValueOnce(nonBlockedStatuses);
        // Имитируем выбор месяца назначения через выпадающий список.
        sagaTester.dispatch({
            type: FILTER.FIELD_CHANGED,
            field: { name: 'month', value: months[1] }
        });

        await sagaTester.waitFor(STATUS.RECEIVE);

        expect(fetchGet).toBeCalledWith(
            `${HOST}/completions/get_status`,
            { service_id: 1, start_dt: '2019-07-01', end_dt: '2019-07-31' },
            false,
            false
        );

        const { root, filter, list } = sagaTester.getState();

        expect(root.get('isFetching')).toBe(false);
        expect(root.get('serviceMap')).toEqual(serviceMap);
        expect(filter.get('serviceItems').toJS()).toEqual(serviceItems);
        expect(filter.getIn(['next', 'service'])).toBe(1);
        expect(filter.getIn(['next', 'month'])).toBe(months[1]);
        expect(filter.getIn(['next', 'preaggregate'])).toBe(false);
        expect(list.get('isFetching')).toBe(false);
        expect(list.get('items')).toEqual(camelCasePropNames(nonBlockedStatuses.data));
    });

    test('reschedule', async () => {
        expect.assertions(10);

        const initialState = {
            perms,
            root: RootRecord({ serviceMap, isFetching: false }),
            filter: FilterStateRecord({
                serviceItems: fromJS(serviceItems),
                next: FilterRecord({
                    month: months[0],
                    service: 1,
                    preaggregate: true
                })
            }),
            list: ListStateRecord({
                items: camelCasePropNames(nonBlockedStatuses.data) as StatusItem[]
            })
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
            data: { status: 'success', message: 'success' }
        });

        // @ts-ignore
        fetchGet.mockResolvedValueOnce(blockedStatuses);
        // Пробуем поставить в очередь
        wrapper.find('.yb-completions-search form').simulate('submit');

        await sagaTester.waitFor(RESCHEDULE.RECEIVE);

        expect(fetchPost).toBeCalledWith(
            `${HOST}/completions/reschedule`,
            {
                service_id: 1,
                start_dt: '2019-05-01',
                end_dt: '2019-05-31',
                with_aggregation: true,
                _csrf: 'csrf'
            },
            false
        );

        // После постновки в очередь, нужно запросить статус
        await sagaTester.waitFor(STATUS.RECEIVE);

        expect(fetchGet).toBeCalledWith(
            `${HOST}/completions/get_status`,
            { service_id: 1, start_dt: '2019-05-01', end_dt: '2019-05-31' },
            false,
            false
        );

        const { root, filter, list } = sagaTester.getState();

        expect(root.get('isFetching')).toBe(false);
        expect(root.get('serviceMap')).toEqual(serviceMap);
        expect(filter.get('serviceItems').toJS()).toEqual(serviceItems);
        expect(filter.getIn(['next', 'service'])).toBe(1);
        expect(filter.getIn(['next', 'month'])).toBe(months[0]);
        expect(filter.getIn(['next', 'preaggregate'])).toBe(true);
        expect(list.get('isFetching')).toBe(false);
        expect(list.get('items')).toEqual(camelCasePropNames(blockedStatuses.data));
    });
});
