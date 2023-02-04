import _ from 'lodash';

import billingStateMock from 'auto-core/react/dataDomain/billing/mocks/billing';

import type { TPaymentMethod } from 'auto-core/types/TBilling';

import reducer from './reducer';
import type { StateBillingAction, StateBilling } from './types';
import { BILLING_UPDATE_REPORT_BUNDLE, BILLING_UPDATE_TIED_CARD } from './actionTypes';

it('при экшене BILLING_UPDATE_TIED_CARD должен обновить указанную карту', () => {
    const initialState = {
        paymentInfo: [ {
            payment_methods: [
                { id: 'bank_card', ps_id: 'YANDEXKASSA_V3', name: 'Банковская карта' },
                { id: 'bank_card', ps_id: 'YANDEXKASSA_V3', mask: '555555|4444', brand: 'MASTERCARD', verification_required: true, preferred: false },
                { id: 'bank_card', ps_id: 'YANDEXKASSA', mask: '444444|4448', brand: 'VISA', preferred: false },
            ],
        } ],
    } as StateBilling;

    const action = {
        type: BILLING_UPDATE_TIED_CARD,
        payload: [
            { id: 'bank_card', ps_id: 'YANDEXKASSA_V3', mask: '555555|4444', brand: 'MASTERCARD', verification_required: true, preferred: true },
        ],
    } as StateBillingAction;

    const expectedState = _.cloneDeep(initialState);
    expectedState.paymentInfo[0].payment_methods[1] = {
        ...expectedState.paymentInfo[0].payment_methods[1],
        preferred: true,
    } as TPaymentMethod;

    expect(reducer(initialState, action)).toEqual(expectedState);
});

it('при экшене BILLING_UPDATE_REPORT_BUNDLE должен смержить инфо о бандле', () => {
    const fakeTicketId = 'foo';
    const initialState = _.cloneDeep(billingStateMock) as unknown as StateBilling;
    const payload = {
        counter: billingStateMock.reportsBundles[1].counter,
        ticketId: fakeTicketId,
    };
    const action = { type: BILLING_UPDATE_REPORT_BUNDLE, payload } as unknown as StateBillingAction;

    const expectedState = _.cloneDeep(billingStateMock);
    expectedState.reportsBundles[1].ticketId = fakeTicketId;

    expect(reducer(initialState, action)).toEqual(expectedState);
});
