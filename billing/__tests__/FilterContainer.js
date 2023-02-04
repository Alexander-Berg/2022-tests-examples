import React from 'react';
import Enzyme, { mount } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { combineReducers } from 'redux';
import { Provider } from 'react-redux';
import { all } from 'redux-saga/effects';
import SagaTester from 'redux-saga-tester';

import { fetchGet } from 'common/utils/old-fetch';
import withIntlProvider from 'common/utils/test-utils/with-intl-provider';
import { SortOrder, SelectEnum } from 'common/constants';
import reducers from '../../reducers';
import FilterContainer from '../FilterContainer';
import { watchFetchActs } from '../../sagas/list';
import { watchRequestManagers } from '../../sagas/filter';
import { LIST, FILTER, MANAGER } from '../../actions';

import { getInitialState as getInitialFilter } from '../../reducers/filter';
import { getInitialList } from '../../reducers/list';
import { initialState as root } from '../../reducers/root';
import { SORT_KEYS } from '../../constants';
import { fromJS } from 'immutable';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');

Enzyme.configure({ adapter: new Adapter() });

const HOST = 'http://snout-test';

const toContainer = window.document.createElement('div');
toContainer.id = 'acts-search-container';
window.document.body.appendChild(toContainer);

const externalId = '377818';
const factura = '070701000003';
const invoiceEid = '1471848';
const contractEid = '4378923';
const actDtFrom = '2018-03-01';
const actDtTo = '2018-11-10';
const managerName = 'Черкасов Арсений Иванович';
const managerId = 20432;
const parentsNames = "['department', 'subdep']";

const items = fromJS([
    { value: 1, content: 'one' },
    { value: 2, content: 'two' },
    { value: 3, content: 'three' },
    { value: 4, content: 'four' }
]);

const currencies = fromJS([
    { value: 810, content: 'RUB' },
    { value: 840, content: 'USD' },
    { value: 978, content: 'EUR' }
]);
const currencyMap = new SelectEnum(
    currencies.map(({ value, content }) => ({ id: value, val: content }))
);

describe('acts filter', () => {
    beforeAll(initializeDesktopRegistry);

    beforeAll(initializeDesktopRegistry);

    describe('filtering acts', () => {
        afterEach(() => {
            jest.resetAllMocks();
        });

        test('try submit with data', async () => {
            expect.assertions(1);

            function* rootSaga() {
                yield all([watchFetchActs()]);
            }

            const initialState = {
                root,
                filter: getInitialFilter({
                    currencies,
                    currencyMap,
                    firms: items,
                    services: items,
                    nextFilter: {
                        externalId,
                        factura,
                        invoiceEid,
                        contractEid,
                        actDtFrom,
                        actDtTo,
                        managerName,
                        managerId
                    }
                }),
                list: getInitialList()
            };

            const sagaTester = new SagaTester({
                initialState,
                reducers: combineReducers({ ...reducers }),
                middlewares: []
            });

            sagaTester.start(rootSaga);

            let Container = () => (
                <Provider store={sagaTester.store}>
                    <FilterContainer />
                </Provider>
            );

            Container = withIntlProvider(Container);

            fetchGet.mockResolvedValueOnce({
                data: {
                    items: [],
                    total_row_count: 0,
                    totals: null,
                    gtotals: null
                }
            });

            const wrapper = mount(<Container />);

            wrapper.find('form').simulate('submit');

            await sagaTester.waitFor(LIST.RECEIVE);

            expect(fetchGet).toBeCalledWith(
                `${HOST}/act/list`,
                {
                    factura,
                    act_eid: externalId,
                    invoice_eid: invoiceEid,
                    contract_eid: contractEid,
                    dt_from: actDtFrom,
                    dt_to: actDtTo,
                    manager_code: managerId,
                    pagination_pn: 1,
                    pagination_ps: 10,
                    sort_key: SORT_KEYS.ACT_DT,
                    sort_order: SortOrder.DESC,
                    show_totals: false
                },
                false,
                false
            );
        });

        test('try cancel', async () => {
            expect.assertions(2);

            function* rootSaga() {
                yield all([watchFetchActs()]);
            }

            const initialState = {
                root,
                filter: getInitialFilter({
                    currencies: currencies,
                    currencyMap,
                    firms: items,
                    services: items,
                    nextFilter: {
                        externalId,
                        factura,
                        invoiceEid,
                        contractEid,
                        actDtFrom,
                        actDtTo,
                        managerName,
                        managerId
                    }
                }),
                list: getInitialList()
            };

            const sagaTester = new SagaTester({
                initialState,
                reducers: combineReducers({ ...reducers }),
                middlewares: []
            });

            sagaTester.start(rootSaga);

            let Container = () => (
                <Provider store={sagaTester.store}>
                    <FilterContainer />
                </Provider>
            );

            Container = withIntlProvider(Container);

            fetchGet.mockResolvedValueOnce({
                data: {
                    items: [],
                    total_row_count: 0,
                    totals: null,
                    gtotals: null
                }
            });

            const wrapper = mount(<Container />);

            wrapper.find('form').simulate('submit');

            wrapper.update().find('button.yb-search-filter__button-cancel').simulate('click');

            await sagaTester.waitFor(FILTER.CANCEL);

            const list = sagaTester.getState().list;

            expect(list.isFetching).toBe(false);
            expect(list.isCanceled).toBe(true);
        });
    });
});
