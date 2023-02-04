import React from 'react';
// import { List as ImmutableList } from 'immutable';
import Enzyme, { mount } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { combineReducers, Reducer } from 'redux';
import { Provider } from 'react-redux';
import { all } from 'redux-saga/effects';
import SagaTester from 'redux-saga-tester';
import { fetchGet, fetchPost } from 'common/utils/old-fetch';
// @ts-ignore
import cloneDeep from 'lodash.clonedeep';

import { camelCasePropNames } from 'common/utils/camel-case';
// import { loggerMiddleware } from 'common/utils/test-utils/common';
import { HOST } from 'common/utils/test-utils/common';
import commonReducers from 'common/reducers/common';
import withIntlProvider from 'common/utils/test-utils/with-intl-provider';
import reducers from '../reducers';
import { List } from '../reducers/cart';
import {
    watchItemsRequest,
    watchCreateRequest,
    watchCheckRequest
    // watchItemDeleteRequest
} from '../sagas/cart';
import { RootContainer } from '../containers/RootContainer';
import { CART } from '../actions';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');

Enzyme.configure({ adapter: new Adapter() });

describe('user - cart - page', () => {
    beforeAll(initializeDesktopRegistry);

    afterEach(() => {
        jest.resetAllMocks();
    });

    test('open page - should request list of orders and invoices and check request', async () => {
        expect.assertions(5);

        const initialState = {
            perms: null,
            isAdmin: false,
            cart: List()
        };

        function* rootSaga() {
            yield all([watchItemsRequest(), watchCheckRequest()]);
        }

        const rootReducer: Reducer = combineReducers({
            ...commonReducers,
            ...reducers
        });

        const { list, checkRequest } = require('./data');

        // @ts-ignore
        fetchGet.mockResolvedValueOnce(list);
        // @ts-ignore
        fetchGet.mockResolvedValueOnce(checkRequest);

        const sagaTester = new SagaTester({
            initialState,
            reducers: rootReducer
            // middlewares: [loggerMiddleware]
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
        await sagaTester.waitFor(CART.REQUEST);

        expect(fetchGet).toBeCalledWith(
            `${HOST}/cart/item/list`,
            {
                detailed: true,
                service_id: '42' // см. __mocks__
            },
            false,
            false
        );

        // @ts-ignore
        await sagaTester.waitFor(CART.CHECK_REQUEST_SUCCESS);

        const itemIds = list.data.items.map((i: any) => i.id).join(',');
        const invoiceIds = list.data.invoices.map((i: any) => i.id).join(',');

        expect(fetchGet).toBeCalledWith(
            `${HOST}/cart/check-request`,
            {
                item_ids: itemIds,
                invoice_ids: invoiceIds
            },
            false,
            false
        );

        wrapper.update();

        const itemsTable = wrapper.find('List table').at(0);
        const invoicesTable = wrapper.find('List table').at(1);

        expect(itemsTable.find('tbody tr').length).toBe(list.data.items.length);
        expect(invoicesTable.find('tbody tr').length).toBe(list.data.invoices.length);

        const confirm = wrapper.find('Confirm');
        expect(confirm.prop('disabled')).not.toBe(checkRequest.data.available_payment);
    });

    test('check or uncheck item - should check request and update submit button status', async () => {
        expect.assertions(2);

        const initialState = {
            perms: null,
            isAdmin: false,
            cart: List()
        };

        function* rootSaga() {
            yield all([watchCheckRequest()]);
        }

        const rootReducer: Reducer = combineReducers({
            ...commonReducers,
            ...reducers
        });

        const { list, checkRequest } = require('./data');
        const secondCheckRequest = cloneDeep(checkRequest);
        secondCheckRequest.data.available_payment = false;
        const thirdCheckRequest = cloneDeep(checkRequest);

        // @ts-ignore
        fetchGet.mockResolvedValueOnce(checkRequest);
        // @ts-ignore
        fetchGet.mockResolvedValueOnce(secondCheckRequest);
        // @ts-ignore
        fetchGet.mockResolvedValueOnce(thirdCheckRequest);

        const sagaTester = new SagaTester({
            initialState,
            reducers: rootReducer
            // middlewares: [loggerMiddleware]
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
            // @ts-ignore
            type: CART.RECEIVE,
            items: camelCasePropNames(list.data.items),
            invoices: camelCasePropNames(list.data.invoices)
        });

        // @ts-ignore
        await sagaTester.waitFor(CART.CHECK_REQUEST_SUCCESS);

        wrapper.update();

        // клик по заказу
        const itemsTable = wrapper.find('List table').at(0);
        const firstItemCheckbox = itemsTable.find('tbody tr').at(0).find('Checkbox').at(0);
        // @ts-ignore
        firstItemCheckbox.prop('onChange')();

        // @ts-ignore
        await sagaTester.waitFor(CART.CHECK_REQUEST_SUCCESS, true);

        wrapper.update();
        let confirm = wrapper.find('Confirm');
        expect(confirm.prop('disabled')).not.toBe(secondCheckRequest.data.available_payment);

        // клик по счету
        const invoicesTable = wrapper.find('List table').at(1);
        const firstInvoiceCheckbox = invoicesTable.find('tbody tr').at(0).find('Checkbox').at(0);
        // @ts-ignore
        firstInvoiceCheckbox.prop('onChange')();

        // @ts-ignore
        await sagaTester.waitFor(CART.CHECK_REQUEST_SUCCESS, true);

        wrapper.update();
        confirm = wrapper.find('Confirm');
        expect(confirm.prop('disabled')).not.toBe(thirdCheckRequest.data.available_payment);
    });

    test('clicking submit button - should create request and navigate to received URL', async () => {
        expect.assertions(4);

        const initialState = {
            perms: null,
            isAdmin: false,
            cart: List()
        };

        function* rootSaga() {
            yield all([watchCheckRequest(), watchCreateRequest()]);
        }

        const rootReducer: Reducer = combineReducers({
            ...commonReducers,
            ...reducers
        });

        const { list, checkRequest, createRequest } = require('./data');

        // @ts-ignore
        fetchGet.mockResolvedValueOnce(checkRequest);
        // @ts-ignore
        fetchGet.mockResolvedValueOnce(checkRequest);
        // @ts-ignore
        fetchPost.mockResolvedValueOnce(createRequest);

        // @ts-ignore
        delete window.location;
        const replace = jest.fn();
        // @ts-ignore
        window.location = { replace };

        const sagaTester = new SagaTester({
            initialState,
            reducers: rootReducer
            // middlewares: [loggerMiddleware]
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
            // @ts-ignore
            type: CART.RECEIVE,
            items: camelCasePropNames(list.data.items),
            invoices: camelCasePropNames(list.data.invoices)
        });

        // @ts-ignore
        await sagaTester.waitFor(CART.CHECK_REQUEST_SUCCESS);

        wrapper.update();

        // снимаем выбор первого счета
        // клик по счето
        const invoicesTable = wrapper.find('List table').at(1);
        const firstInvoiceCheckbox = invoicesTable.find('tbody tr').at(0).find('Checkbox').at(0);
        // @ts-ignore
        firstInvoiceCheckbox.prop('onChange')();

        // @ts-ignore
        await sagaTester.waitFor(CART.CHECK_REQUEST_SUCCESS, true);

        const btnConfirm = wrapper.find('Confirm').find('[type="submit"]').at(1);
        btnConfirm.simulate('submit');

        // console.log(confirm.debug());

        // @ts-ignore
        await sagaTester.waitFor(CART.CREATE_REQUEST_SUCCESS);

        // console.log(confirm.debug());

        const itemIds = list.data.items.map((i: any) => i.id).join(',');
        const invoiceIds = list.data.invoices.map((i: any) => i.id).join(',');
        const invoices = [...list.data.invoices];
        invoices.shift();
        const invoiceIdsAfterUncheck = invoices.map((i: any) => i.id).join(',');

        expect(fetchGet).nthCalledWith(
            1,
            `${HOST}/cart/check-request`,
            {
                item_ids: itemIds,
                invoice_ids: invoiceIds
            },
            false,
            false
        );

        expect(fetchGet).nthCalledWith(
            2,
            `${HOST}/cart/check-request`,
            {
                item_ids: itemIds,
                invoice_ids: invoiceIdsAfterUncheck
            },
            false,
            false
        );

        expect(fetchPost).toBeCalledWith(
            `${HOST}/cart/create-request`,
            {
                service_id: '42', // см. __mocks__
                item_ids: itemIds,
                invoice_ids: invoiceIdsAfterUncheck,
                _csrf: 'csrf'
            },
            false
        );

        expect(replace).toBeCalledWith(createRequest.data.user_path);
    });
});
