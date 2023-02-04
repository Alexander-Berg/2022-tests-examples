import React from 'react';
import Enzyme, { mount } from 'enzyme';
import { fromJS, Map } from 'immutable';
import Adapter from 'enzyme-adapter-react-16';
import { combineReducers } from 'redux';
import { Provider } from 'react-redux';
import { all } from 'redux-saga/effects';
import SagaTester from 'redux-saga-tester';

import { HOST } from '../../../../../common/utils/test-utils/common';
import FilterContainer from '../FilterContainer';
import commonReducers from 'common/reducers/common';
import reducers from '../../reducers';
import { watchRequestInitialData } from '../../sagas/filter';
import { watchListRequest } from '../../sagas/list';
import withIntlProvider from 'common/utils/test-utils/with-intl-provider';
import { fetchGet } from 'common/utils/old-fetch';
import { FilterAction, ListAction } from '../../actions';
import { FilterState } from '../../reducers/filter';
import { PaymentStatuses } from 'common/constants';
import { DateTypes, PostPayTypes, SortKeys, TroubleTypes } from '../../constants';

import { SortOrder, PageSizes } from 'common/constants';
import { InitialDataAction } from 'common/actions';
import { request } from 'common/utils/request';
import { removeEmptyFields } from 'admin/api/data-processors/common';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

describe('admin - invoices - containers - filter', () => {
    beforeAll(initializeDesktopRegistry);

    afterEach(() => {
        jest.resetAllMocks();
    });

    test('request initial data', async () => {
        expect.assertions(6);

        function* rootSaga() {
            yield all([watchRequestInitialData()]);
        }

        const initialState = {
            filter: Map({
                isFetching: true,
                isPaysysSelectorOpened: false,
                firms: null,
                services: null,
                groupedPaysyses: null,
                serviceCodes: null,
                current: FilterState(),
                next: FilterState()
            })
        };

        const sagaTester = new SagaTester({
            initialState,
            reducers: combineReducers({ ...commonReducers, ...reducers }),
            middlewares: []
        });

        sagaTester.start(rootSaga);

        const Container = withIntlProvider(() => (
            <Provider store={sagaTester.store}>
                <FilterContainer />
            </Provider>
        ));

        const initialRequestGetDataUrls = [
            { url: '/firm/list' },
            { url: '/service/list', data: {} },
            { url: '/firm/intercompany_list' },
            { url: '/person/category/list' }
        ];

        for (let i = 0; i < initialRequestGetDataUrls.length; i++) {
            request.get.mockResolvedValueOnce([]);
        }

        const initialFetchGetDataUrls = ['/product/service-code/list', '/paysys/list'];

        for (let i = 0; i < initialFetchGetDataUrls.length; i++) {
            fetchGet.mockResolvedValueOnce({ data: [] });
        }

        const wrapper = mount(<Container />);

        await sagaTester.waitFor(InitialDataAction.RECEIVE);

        initialRequestGetDataUrls.forEach(({ url, data }) => {
            expect(request.get).toBeCalledWith(removeEmptyFields({ url: `${HOST}${url}`, data }));
        });

        initialFetchGetDataUrls.forEach(url => {
            expect(fetchGet).toBeCalledWith(`${HOST}${url}`, undefined, false, false);
        });
    });

    test('find items with original filter', async () => {
        expect.assertions(1);

        function* rootSaga() {
            yield all([watchListRequest()]);
        }

        const initialState = {
            filter: Map({
                isFetching: false,
                isPaysysSelectorOpened: false,
                firms: fromJS([]),
                services: fromJS([]),
                groupedPaysyses: fromJS([]),
                serviceCodes: fromJS([]),
                current: FilterState(),
                next: FilterState()
            })
        };

        const sagaTester = new SagaTester({
            initialState,
            reducers: combineReducers({ ...commonReducers, ...reducers }),
            middlewares: []
        });

        sagaTester.start(rootSaga);

        const Container = withIntlProvider(() => (
            <Provider store={sagaTester.store}>
                <FilterContainer />
            </Provider>
        ));

        fetchGet.mockResolvedValueOnce({
            data: { total_row_count: 0, gtotals: null, items: [] }
        });

        const wrapper = mount(<Container />);

        wrapper.find('[type="submit"]').at(1).simulate('submit');

        await sagaTester.waitFor(ListAction.RECEIVE);

        expect(fetchGet).toBeCalledWith(
            `${HOST}/invoice/list`,
            {
                invoice_type: DateTypes.INVOICE,
                payment_status: PaymentStatuses.ALL,
                post_pay_type: PostPayTypes.ALL,
                trouble_type: TroubleTypes.NONE,
                contract_eid_strict: false,
                pagination_pn: 1,
                pagination_ps: PageSizes[0],
                show_totals: false,
                sort_key: SortKeys.INVOICE_DT,
                sort_order: SortOrder.DESC
            },
            false,
            false
        );
    });

    test('find items with filled filter', async () => {
        expect.assertions(1);

        function* rootSaga() {
            yield all([watchListRequest()]);
        }

        const initialState = {
            filter: Map({
                isFetching: false,
                isPaysysSelectorOpened: false,
                firms: fromJS([]),
                services: fromJS([]),
                groupedPaysyses: fromJS([]),
                serviceCodes: fromJS([]),
                current: FilterState(),
                next: FilterState()
            })
        };

        const sagaTester = new SagaTester({
            initialState,
            reducers: combineReducers({ ...commonReducers, ...reducers }),
            middlewares: []
        });

        sagaTester.start(rootSaga);

        const Container = withIntlProvider(() => (
            <Provider store={sagaTester.store}>
                <FilterContainer />
            </Provider>
        ));

        fetchGet.mockResolvedValueOnce({
            data: { total_row_count: 0, gtotals: null, items: [] }
        });

        const wrapper = mount(<Container />);

        sagaTester.store.dispatch({
            type: FilterAction.DATE_TYPE_CHANGE,
            val: DateTypes.REQUEST
        });

        sagaTester.store.dispatch({
            type: FilterAction.DATE_FROM_CHANGE,
            val: '2019-04-01T00:00:00'
        });

        sagaTester.store.dispatch({
            type: FilterAction.DATE_TO_CHANGE,
            val: '2019-04-07T00:00:00'
        });

        wrapper.find('[type="submit"]').at(1).simulate('submit');

        await sagaTester.waitFor(ListAction.RECEIVE);

        await expect(fetchGet).toBeCalledWith(
            `${HOST}/invoice/list`,
            {
                invoice_type: DateTypes.REQUEST,
                from_dt: '2019-04-01T00:00:00',
                to_dt: '2019-04-07T00:00:00',
                payment_status: PaymentStatuses.ALL,
                post_pay_type: PostPayTypes.ALL,
                trouble_type: TroubleTypes.NONE,
                pagination_pn: 1,
                pagination_ps: PageSizes[0],
                show_totals: false,
                contract_eid_strict: false,
                sort_key: SortKeys.INVOICE_DT,
                sort_order: SortOrder.DESC
            },
            false,
            false
        );
    });
});
