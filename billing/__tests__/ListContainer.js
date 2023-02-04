import React from 'react';
import Enzyme, { mount } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { combineReducers } from 'redux';
import { Provider } from 'react-redux';
import { all } from 'redux-saga/effects';
import SagaTester from 'redux-saga-tester';
import { fromJS } from 'immutable';

import { fetchGet } from 'common/utils/old-fetch';
import withIntlProvider from 'common/utils/test-utils/with-intl-provider';
import { SortOrder, SelectEnum } from 'common/constants';

import ListContainer from '../ListContainer';
import reducers from '../../reducers';
import { watchFetchActs } from '../../sagas/list';
import { LIST } from '../../actions';
import { getInitialState as getInitialFilter } from '../../reducers/filter';
import { getInitialList } from '../../reducers/list';
import { initialState as root } from '../../reducers/root';
import { SORT_KEYS } from '../../constants';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');

Enzyme.configure({ adapter: new Adapter() });

const HOST = 'http://snout-test';

const toContainer = window.document.createElement('div');
toContainer.id = 'acts-table-container';
window.document.body.appendChild(toContainer);

const externalId = '377818';
const factura = '070701000003';
const invoiceEid = '1471848';
const contractEid = '4378923';
const actDtFrom = '2018-03-01';
const actDtTo = '2018-11-10';
const managerName = 'Черкасов Арсений Иванович';
const managerId = 20432;

const items = fromJS([
    { value: 1, content: 'one' },
    { value: 2, content: 'two' },
    { value: 3, content: 'three' },
    { value: 4, content: 'four' }
]);

const [serviceid, firmId] = items;
const currencies = fromJS([
    { value: 810, content: 'RUB' },
    { value: 840, content: 'USD' },
    { value: 978, content: 'EUR' }
]);
const currencyMap = new SelectEnum(
    currencies.map(({ value, content }) => ({ id: value, val: content }))
);

const client = { id: 1, name: 'Client Name' };
const person = { id: 23, name: 'Person Name' };

const acts = [
    {
        act_id: 1088831,
        nds_pct: '18.00',
        factura: null,
        person_type: 'ur',
        paysys_certificate: '0',
        contract_id: null,
        paid_amount: '6855.60',
        invoice_eid: 'Б-1319798-1',
        overdraft: false,
        act_dt: '2006-12-20T00:00:00',
        invoice_amount: '17700.00',
        person_id: 170830,
        invoice_closed: true,
        person_name: 'ИнКоннэкт',
        contract_eid: null,
        paysys_cc: 'ur',
        amount_nds: '1045.77',
        firm_id: 1,
        client_id: 11243,
        invoice_currency: 'RUR',
        our_fault: null,
        payment_term_dt: null,
        paysys_name: 'Банк для юридических лиц',
        currency: 'RUR',
        invoice_id: 1071987,
        passport_id: 11625537,
        credit: false,
        amount: '6855.60',
        external_id: '00000485309',
        client_name: 'ИнКоннэкт'
    },
    {
        act_id: 1049537,
        nds_pct: '18.00',
        factura: null,
        person_type: 'ur',
        paysys_certificate: '0',
        contract_id: null,
        paid_amount: '10844.40',
        invoice_eid: 'Б-1319798-1',
        overdraft: false,
        act_dt: '2006-11-30T00:00:00',
        invoice_amount: '17700.00',
        person_id: 170830,
        invoice_closed: true,
        person_name: 'ИнКоннэкт',
        contract_eid: null,
        paysys_cc: 'ur',
        amount_nds: '1654.23',
        firm_id: 1,
        client_id: 11243,
        invoice_currency: 'RUR',
        our_fault: null,
        payment_term_dt: null,
        paysys_name: 'Банк для юридических лиц',
        currency: 'RUR',
        invoice_id: 1071987,
        passport_id: 11625537,
        credit: false,
        amount: '10844.40',
        external_id: '00000457716',
        client_name: 'ИнКоннэкт'
    },
    {
        act_id: 965230,
        nds_pct: '18.00',
        factura: null,
        person_type: 'ur',
        paysys_certificate: '0',
        contract_id: null,
        paid_amount: '1176.00',
        invoice_eid: 'Б-845532-1',
        overdraft: false,
        act_dt: '2006-10-11T00:00:00',
        invoice_amount: '10258.88',
        person_id: 108171,
        invoice_closed: false,
        person_name: 'ИнКоннэкт',
        contract_eid: null,
        paysys_cc: 'ur',
        amount_nds: '179.39',
        firm_id: 1,
        client_id: 11243,
        invoice_currency: 'RUR',
        our_fault: null,
        payment_term_dt: null,
        paysys_name: 'Банк для юридических лиц',
        currency: 'RUR',
        invoice_id: 692230,
        passport_id: 11625537,
        credit: false,
        amount: '1176.00',
        external_id: '00000361531',
        client_name: 'ИнКоннэкт'
    },
    {
        act_id: 939746,
        nds_pct: '18.00',
        factura: null,
        person_type: 'ur',
        paysys_certificate: '0',
        contract_id: null,
        paid_amount: '4002.71',
        invoice_eid: 'Б-845532-1',
        overdraft: false,
        act_dt: '2006-09-30T00:00:00',
        invoice_amount: '10258.88',
        person_id: 108171,
        invoice_closed: false,
        person_name: 'ИнКоннэкт',
        contract_eid: null,
        paysys_cc: 'ur',
        amount_nds: '610.58',
        firm_id: 1,
        client_id: 11243,
        invoice_currency: 'RUR',
        our_fault: null,
        payment_term_dt: null,
        paysys_name: 'Банк для юридических лиц',
        currency: 'RUR',
        invoice_id: 692230,
        passport_id: 11625537,
        credit: false,
        amount: '4002.71',
        external_id: '00000336730',
        client_name: 'ИнКоннэкт'
    },
    {
        act_id: 894471,
        nds_pct: '18.00',
        factura: null,
        person_type: 'ur',
        paysys_certificate: '0',
        contract_id: null,
        paid_amount: '652.33',
        invoice_eid: 'Б-845532-1',
        overdraft: false,
        act_dt: '2006-08-31T00:00:00',
        invoice_amount: '10258.88',
        person_id: 108171,
        invoice_closed: false,
        person_name: 'ИнКоннэкт',
        contract_eid: null,
        paysys_cc: 'ur',
        amount_nds: '99.51',
        firm_id: 1,
        client_id: 11243,
        invoice_currency: 'RUR',
        our_fault: null,
        payment_term_dt: null,
        paysys_name: 'Банк для юридических лиц',
        currency: 'RUR',
        invoice_id: 692230,
        passport_id: 11625537,
        credit: false,
        amount: '652.33',
        external_id: '00000290524',
        client_name: 'ИнКоннэкт'
    },
    {
        act_id: 852700,
        nds_pct: '18.00',
        factura: null,
        person_type: 'ur',
        paysys_certificate: '0',
        contract_id: null,
        paid_amount: '445.71',
        invoice_eid: 'Б-845532-1',
        overdraft: false,
        act_dt: '2006-07-31T00:00:00',
        invoice_amount: '10258.88',
        person_id: 108171,
        invoice_closed: false,
        person_name: 'ИнКоннэкт',
        contract_eid: null,
        paysys_cc: 'ur',
        amount_nds: '67.98',
        firm_id: 1,
        client_id: 11243,
        invoice_currency: 'RUR',
        our_fault: null,
        payment_term_dt: null,
        paysys_name: 'Банк для юридических лиц',
        currency: 'RUR',
        invoice_id: 692230,
        passport_id: 11625537,
        credit: false,
        amount: '445.71',
        external_id: '00000248959',
        client_name: 'ИнКоннэкт'
    },
    {
        act_id: 812539,
        nds_pct: '18.00',
        factura: null,
        person_type: 'ur',
        paysys_certificate: '0',
        contract_id: null,
        paid_amount: '418.76',
        invoice_eid: 'Б-845532-1',
        overdraft: false,
        act_dt: '2006-06-30T00:00:00',
        invoice_amount: '10258.88',
        person_id: 108171,
        invoice_closed: false,
        person_name: 'ИнКоннэкт',
        contract_eid: null,
        paysys_cc: 'ur',
        amount_nds: '63.88',
        firm_id: 1,
        client_id: 11243,
        invoice_currency: 'RUR',
        our_fault: null,
        payment_term_dt: null,
        paysys_name: 'Банк для юридических лиц',
        currency: 'RUR',
        invoice_id: 692230,
        passport_id: 11625537,
        credit: false,
        amount: '418.76',
        external_id: '00000208900',
        client_name: 'ИнКоннэкт'
    },
    {
        act_id: 772115,
        nds_pct: '18.00',
        factura: null,
        person_type: 'ur',
        paysys_certificate: '0',
        contract_id: null,
        paid_amount: '604.81',
        invoice_eid: 'Б-845532-1',
        overdraft: false,
        act_dt: '2006-05-31T00:00:00',
        invoice_amount: '10258.88',
        person_id: 108171,
        invoice_closed: false,
        person_name: 'ИнКоннэкт',
        contract_eid: null,
        paysys_cc: 'ur',
        amount_nds: '92.26',
        firm_id: 1,
        client_id: 11243,
        invoice_currency: 'RUR',
        our_fault: null,
        payment_term_dt: null,
        paysys_name: 'Банк для юридических лиц',
        currency: 'RUR',
        invoice_id: 692230,
        passport_id: 11625537,
        credit: false,
        amount: '604.81',
        external_id: '00000168433',
        client_name: 'ИнКоннэкт'
    },
    {
        act_id: 732347,
        nds_pct: '18.00',
        factura: null,
        person_type: 'ur',
        paysys_certificate: '0',
        contract_id: null,
        paid_amount: '619.88',
        invoice_eid: 'Б-845532-1',
        overdraft: false,
        act_dt: '2006-04-30T00:00:00',
        invoice_amount: '10258.88',
        person_id: 108171,
        invoice_closed: false,
        person_name: 'ИнКоннэкт',
        contract_eid: null,
        paysys_cc: 'ur',
        amount_nds: '94.56',
        firm_id: 1,
        client_id: 11243,
        invoice_currency: 'RUR',
        our_fault: null,
        payment_term_dt: null,
        paysys_name: 'Банк для юридических лиц',
        currency: 'RUR',
        invoice_id: 692230,
        passport_id: 11625537,
        credit: false,
        amount: '619.88',
        external_id: '00000129043',
        client_name: 'ИнКоннэкт'
    },
    {
        act_id: 694116,
        nds_pct: '18.00',
        factura: null,
        person_type: 'ur',
        paysys_certificate: '0',
        contract_id: null,
        paid_amount: '685.67',
        invoice_eid: 'Б-845532-1',
        overdraft: false,
        act_dt: '2006-03-31T00:00:00',
        invoice_amount: '10258.88',
        person_id: 108171,
        invoice_closed: false,
        person_name: 'ИнКоннэкт',
        contract_eid: null,
        paysys_cc: 'ur',
        amount_nds: '104.60',
        firm_id: 1,
        client_id: 11243,
        invoice_currency: 'RUR',
        our_fault: null,
        payment_term_dt: null,
        paysys_name: 'Банк для юридических лиц',
        currency: 'RUR',
        invoice_id: 692230,
        passport_id: 11625537,
        credit: false,
        amount: '685.67',
        external_id: '00000091741',
        client_name: 'ИнКоннэкт'
    },
    {
        act_id: 656810,
        nds_pct: '18.00',
        factura: null,
        person_type: 'ur',
        paysys_certificate: '0',
        contract_id: null,
        paid_amount: '661.61',
        invoice_eid: 'Б-845532-1',
        overdraft: false,
        act_dt: '2006-02-28T00:00:00',
        invoice_amount: '10258.88',
        person_id: 108171,
        invoice_closed: false,
        person_name: 'ИнКоннэкт',
        contract_eid: null,
        paysys_cc: 'ur',
        amount_nds: '100.92',
        firm_id: 1,
        client_id: 11243,
        invoice_currency: 'RUR',
        our_fault: null,
        payment_term_dt: null,
        paysys_name: 'Банк для юридических лиц',
        currency: 'RUR',
        invoice_id: 692230,
        passport_id: 11625537,
        credit: false,
        amount: '661.61',
        external_id: '00000052933',
        client_name: 'ИнКоннэкт'
    },
    {
        act_id: 625881,
        nds_pct: '18.00',
        factura: null,
        person_type: 'ur',
        paysys_certificate: '0',
        contract_id: null,
        paid_amount: '728.26',
        invoice_eid: 'Б-845532-1',
        overdraft: false,
        act_dt: '2006-01-31T00:00:00',
        invoice_amount: '10258.88',
        person_id: 108171,
        invoice_closed: false,
        person_name: 'ИнКоннэкт',
        contract_eid: null,
        paysys_cc: 'ur',
        amount_nds: '111.09',
        firm_id: 1,
        client_id: 11243,
        invoice_currency: 'RUR',
        our_fault: null,
        payment_term_dt: null,
        paysys_name: 'Банк для юридических лиц',
        currency: 'RUR',
        invoice_id: 692230,
        passport_id: 11625537,
        credit: false,
        amount: '728.26',
        external_id: '00000023123',
        client_name: 'ИнКоннэкт'
    },
    {
        act_id: 600290,
        nds_pct: '18.00',
        factura: null,
        person_type: 'ur',
        paysys_certificate: '0',
        contract_id: null,
        paid_amount: '263.14',
        invoice_eid: 'Б-845532-1',
        overdraft: false,
        act_dt: '2005-12-31T00:00:00',
        invoice_amount: '10258.88',
        person_id: 108171,
        invoice_closed: false,
        person_name: 'ИнКоннэкт',
        contract_eid: null,
        paysys_cc: 'ur',
        amount_nds: '40.14',
        firm_id: 1,
        client_id: 11243,
        invoice_currency: 'RUR',
        our_fault: null,
        payment_term_dt: null,
        paysys_name: 'Банк для юридических лиц',
        currency: 'RUR',
        invoice_id: 692230,
        passport_id: 11625537,
        credit: false,
        amount: '263.14',
        external_id: '01000183675',
        client_name: 'ИнКоннэкт'
    },
    {
        act_id: 580756,
        nds_pct: '18.00',
        factura: null,
        person_type: 'ur',
        paysys_certificate: '0',
        contract_id: null,
        paid_amount: '76.79',
        invoice_eid: 'Б-655717-1',
        overdraft: false,
        act_dt: '2005-12-13T00:00:00',
        invoice_amount: '10105.39',
        person_id: 108171,
        invoice_closed: false,
        person_name: 'ИнКоннэкт',
        contract_eid: null,
        paysys_cc: 'ur',
        amount_nds: '11.71',
        firm_id: 1,
        client_id: 11243,
        invoice_currency: 'RUR',
        our_fault: null,
        payment_term_dt: null,
        paysys_name: 'Банк для юридических лиц',
        currency: 'RUR',
        invoice_id: 550717,
        passport_id: 11625537,
        credit: false,
        amount: '76.79',
        external_id: '01000162509',
        client_name: 'ИнКоннэкт'
    },
    {
        act_id: 562642,
        nds_pct: '18.00',
        factura: null,
        person_type: 'ur',
        paysys_certificate: '0',
        contract_id: null,
        paid_amount: '1938.86',
        invoice_eid: 'Б-655717-1',
        overdraft: false,
        act_dt: '2005-11-30T00:00:00',
        invoice_amount: '10105.39',
        person_id: 108171,
        invoice_closed: false,
        person_name: 'ИнКоннэкт',
        contract_eid: null,
        paysys_cc: 'ur',
        amount_nds: '295.76',
        firm_id: 1,
        client_id: 11243,
        invoice_currency: 'RUR',
        our_fault: null,
        payment_term_dt: null,
        paysys_name: 'Банк для юридических лиц',
        currency: 'RUR',
        invoice_id: 550717,
        passport_id: 11625537,
        credit: false,
        amount: '1938.86',
        external_id: '01000144294',
        client_name: 'ИнКоннэкт'
    },
    {
        act_id: 518897,
        nds_pct: '18.00',
        factura: null,
        person_type: 'ur',
        paysys_certificate: '0',
        contract_id: null,
        paid_amount: '6951.31',
        invoice_eid: 'Б-655717-1',
        overdraft: false,
        act_dt: '2005-10-31T00:00:00',
        invoice_amount: '10105.39',
        person_id: 108171,
        invoice_closed: false,
        person_name: 'ИнКоннэкт',
        contract_eid: null,
        paysys_cc: 'ur',
        amount_nds: '1060.37',
        firm_id: 1,
        client_id: 11243,
        invoice_currency: 'RUR',
        our_fault: null,
        payment_term_dt: null,
        paysys_name: 'Банк для юридических лиц',
        currency: 'RUR',
        invoice_id: 550717,
        passport_id: 11625537,
        credit: false,
        amount: '6951.31',
        external_id: '01000112711',
        client_name: 'ИнКоннэкт'
    },
    {
        act_id: 487283,
        nds_pct: '18.00',
        factura: null,
        person_type: 'ur',
        paysys_certificate: '0',
        contract_id: null,
        paid_amount: '1138.43',
        invoice_eid: 'Б-655717-1',
        overdraft: false,
        act_dt: '2005-09-30T00:00:00',
        invoice_amount: '10105.39',
        person_id: 108171,
        invoice_closed: false,
        person_name: 'ИнКоннэкт',
        contract_eid: null,
        paysys_cc: 'ur',
        amount_nds: '173.66',
        firm_id: 1,
        client_id: 11243,
        invoice_currency: 'RUR',
        our_fault: null,
        payment_term_dt: null,
        paysys_name: 'Банк для юридических лиц',
        currency: 'RUR',
        invoice_id: 550717,
        passport_id: 11625537,
        credit: false,
        amount: '1138.43',
        external_id: '10000104413',
        client_name: 'ИнКоннэкт'
    },
    {
        act_id: 476976,
        nds_pct: '18.00',
        factura: null,
        person_type: 'ur',
        paysys_certificate: '0',
        contract_id: null,
        paid_amount: '10118.88',
        invoice_eid: 'Б-621372-1',
        overdraft: false,
        act_dt: '2005-09-28T00:00:00',
        invoice_amount: '10118.88',
        person_id: 108171,
        invoice_closed: false,
        person_name: 'ИнКоннэкт',
        contract_eid: null,
        paysys_cc: 'ur',
        amount_nds: '1543.56',
        firm_id: 1,
        client_id: 11243,
        invoice_currency: 'RUR',
        our_fault: null,
        payment_term_dt: null,
        paysys_name: 'Банк для юридических лиц',
        currency: 'RUR',
        invoice_id: 524212,
        passport_id: 11625537,
        credit: false,
        amount: '10118.88',
        external_id: '10000093772',
        client_name: 'ИнКоннэкт'
    },
    {
        act_id: 457924,
        nds_pct: '18.00',
        factura: null,
        person_type: 'ur',
        paysys_certificate: '0',
        contract_id: null,
        paid_amount: '593.15',
        invoice_eid: 'Б-584399-1',
        overdraft: false,
        act_dt: '2005-09-02T00:00:00',
        invoice_amount: '10133.92',
        person_id: 97160,
        invoice_closed: false,
        person_name: 'ИнКоннэкт',
        contract_eid: null,
        paysys_cc: 'ur',
        amount_nds: '90.48',
        firm_id: 1,
        client_id: 11243,
        invoice_currency: 'RUR',
        our_fault: null,
        payment_term_dt: null,
        paysys_name: 'Банк для юридических лиц',
        currency: 'RUR',
        invoice_id: 495891,
        passport_id: 11625537,
        credit: false,
        amount: '593.15',
        external_id: '10000078152',
        client_name: 'ИнКоннэкт'
    },
    {
        act_id: 454403,
        nds_pct: '18.00',
        factura: null,
        person_type: 'ur',
        paysys_certificate: '0',
        contract_id: null,
        paid_amount: '8273.46',
        invoice_eid: 'Б-584399-1',
        overdraft: false,
        act_dt: '2005-08-31T00:00:00',
        invoice_amount: '10133.92',
        person_id: 97160,
        invoice_closed: false,
        person_name: 'ИнКоннэкт',
        contract_eid: null,
        paysys_cc: 'ur',
        amount_nds: '1262.05',
        firm_id: 1,
        client_id: 11243,
        invoice_currency: 'RUR',
        our_fault: null,
        payment_term_dt: null,
        paysys_name: 'Банк для юридических лиц',
        currency: 'RUR',
        invoice_id: 495891,
        passport_id: 11625537,
        credit: false,
        amount: '8273.46',
        external_id: '10000075593',
        client_name: 'ИнКоннэкт'
    }
];

const pageNumber = 1;
const pageSize = acts.length;
const totalPages = 5;
const totalCount = 100;

describe('acts list', () => {
    beforeAll(initializeDesktopRegistry);

    describe('getting acts', () => {
        afterEach(() => {
            jest.resetAllMocks();
        });

        test('try change sort order', async () => {
            expect.assertions(5);

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
                    },
                    currentFilter: {
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
                list: getInitialList({
                    pageSize,
                    nextPageSize: pageSize,
                    pageNumber,
                    nextPageNumber: pageNumber,
                    totalCount,
                    totalPages,
                    acts
                })
            };

            const sagaTester = new SagaTester({
                initialState,
                reducers: combineReducers({ ...reducers }),
                middlewares: []
            });

            sagaTester.start(rootSaga);

            let Container = () => (
                <Provider store={sagaTester.store}>
                    <ListContainer />
                </Provider>
            );

            Container = withIntlProvider(Container);

            fetchGet.mockResolvedValueOnce({
                data: {
                    items: [],
                    total_row_count: 0
                }
            });

            const wrapper = mount(<Container />);

            wrapper.find('th').at(2).find('FormattedMessage').simulate('click');

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
                    pagination_pn: pageNumber,
                    pagination_ps: pageSize,
                    sort_key: SORT_KEYS.ACT_DT,
                    sort_order: SortOrder.ASC,
                    show_totals: false
                },
                false,
                false
            );

            expect(sagaTester.getState().list.nextSort.order).toEqual(SortOrder.ASC);
            expect(sagaTester.getState().list.nextSort.key).toEqual(SORT_KEYS.ACT_DT);
            expect(sagaTester.getState().list.currentSort.order).toEqual(SortOrder.ASC);
            expect(sagaTester.getState().list.currentSort.key).toEqual(SORT_KEYS.ACT_DT);
        });

        test('try change sort key', async () => {
            expect.assertions(5);

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
                    },
                    currentFilter: {
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
                list: getInitialList({
                    pageSize,
                    nextPageSize: pageSize,
                    pageNumber,
                    nextPageNumber: pageNumber,
                    totalCount,
                    totalPages,
                    acts
                })
            };

            const sagaTester = new SagaTester({
                initialState,
                reducers: combineReducers({ ...reducers })
            });

            sagaTester.start(rootSaga);

            let Container = () => (
                <Provider store={sagaTester.store}>
                    <ListContainer />
                </Provider>
            );

            Container = withIntlProvider(Container);

            fetchGet.mockResolvedValueOnce({
                data: {
                    items: [],
                    total_row_count: 0
                }
            });

            const wrapper = mount(<Container />);

            const sortLink = wrapper.find('th').at(3).find('FormattedMessage').at(0);

            sortLink.simulate('click');

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
                    pagination_pn: pageNumber,
                    pagination_ps: pageSize,
                    sort_key: SORT_KEYS.INVOICE_EID,
                    sort_order: SortOrder.DESC,
                    show_totals: false
                },
                false,
                false
            );

            expect(sagaTester.getState().list.nextSort.order).toEqual(SortOrder.DESC);
            expect(sagaTester.getState().list.nextSort.key).toEqual(SORT_KEYS.INVOICE_EID);
            expect(sagaTester.getState().list.currentSort.order).toEqual(SortOrder.DESC);
            expect(sagaTester.getState().list.currentSort.key).toEqual(SORT_KEYS.INVOICE_EID);
        });

        test('try change page', async () => {
            expect.assertions(3);

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
                    },
                    currentFilter: {
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
                list: getInitialList({
                    pageSize,
                    nextPageSize: pageSize,
                    pageNumber,
                    nextPageNumber: pageNumber,
                    totalCount,
                    totalPages,
                    acts
                })
            };

            const sagaTester = new SagaTester({
                initialState,
                reducers: combineReducers({ ...reducers }),
                middlewares: []
            });

            sagaTester.start(rootSaga);

            let Container = () => (
                <Provider store={sagaTester.store}>
                    <ListContainer />
                </Provider>
            );

            Container = withIntlProvider(Container);

            fetchGet.mockResolvedValueOnce({
                data: {
                    items: [],
                    total_row_count: 0
                }
            });

            const wrapper = mount(<Container />);

            wrapper.find('button').at(1).simulate('click');

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
                    pagination_pn: 2,
                    pagination_ps: pageSize,
                    sort_key: SORT_KEYS.ACT_DT,
                    sort_order: SortOrder.DESC,
                    show_totals: false
                },
                false,
                false
            );

            expect(sagaTester.getState().list.nextPageNumber).toEqual(2);
            expect(sagaTester.getState().list.pageNumber).toEqual(2);
        });

        test('try change page size', async () => {
            expect.assertions(3);

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
                    },
                    currentFilter: {
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
                list: getInitialList({
                    pageSize,
                    nextPageSize: pageSize,
                    pageNumber,
                    nextPageNumber: pageNumber,
                    totalCount,
                    totalPages,
                    acts
                })
            };

            const sagaTester = new SagaTester({
                initialState,
                reducers: combineReducers({ ...reducers }),
                middlewares: []
            });

            sagaTester.start(rootSaga);

            let Container = () => (
                <Provider store={sagaTester.store}>
                    <ListContainer />
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

            wrapper.find('button').last().simulate('click');

            await sagaTester.waitFor(LIST.RECEIVE);

            const newPageSize = pageSize * totalPages;

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
                    pagination_pn: pageNumber,
                    pagination_ps: newPageSize,
                    sort_key: SORT_KEYS.ACT_DT,
                    sort_order: SortOrder.DESC,
                    show_totals: false
                },
                false,
                false
            );

            expect(sagaTester.getState().list.nextPageSize).toEqual(newPageSize);
            expect(sagaTester.getState().list.pageSize).toEqual(newPageSize);
        });
    });
});
