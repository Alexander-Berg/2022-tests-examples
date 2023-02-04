import { Payment_Status } from '@vertis/schema-registry/ts-types-snake/auto/api/user/payment';

import { PAGE_LOADING_SUCCESS } from 'auto-core/react/actionTypes';

import reducer from './reducer';
import actionTypes from './actionTypes';
import { paymentMock, paymentStateMock } from './mocks';
import type { ActionPageLoadingSuccess, StatePaymentsActionRejected, StatePaymentsActionPending, StatePaymentsActionResolved } from './types';

describe('PAGE_LOADING_SUCCESS', () => {
    it('вернет дефолтный стейт если нет данных', () => {
        const state = paymentStateMock.value();
        const action: ActionPageLoadingSuccess = { type: PAGE_LOADING_SUCCESS };
        const nextState = reducer(state, action);

        expect(nextState).toEqual(state);
    });

    it('добавит данные в стейт если данные есть', () => {
        const state = paymentStateMock.value();
        const action: ActionPageLoadingSuccess = {
            type: PAGE_LOADING_SUCCESS,
            data: {
                payments: paymentStateMock.withPayments([ paymentMock.value() ]).value().data,
            },
        };
        const nextState = reducer(state, action);

        expect(nextState.data?.payments).toHaveLength(1);
    });
});

it('PAYMENTS_PENDING: переключит isFetching в true', () => {
    const state = { isFetching: false };
    const action: StatePaymentsActionPending = {
        type: actionTypes.PAYMENTS_PENDING,
    };
    const nextState = reducer(state, action);

    expect(nextState.isFetching).toBe(true);
});

it('PAYMENTS_REJECTED: переключит isFetching в false', () => {
    const state = { isFetching: true };
    const action: StatePaymentsActionRejected = {
        type: actionTypes.PAYMENTS_REJECTED,
    };
    const nextState = reducer(state, action);

    expect(nextState.isFetching).toBe(false);
});

describe('PAYMENTS_RESOLVED', () => {
    it('переключит isFetching в false и обновит платежи если они были', () => {
        const state = paymentStateMock.withPayments([ paymentMock.value() ]).value();
        const action: StatePaymentsActionResolved = {
            type: actionTypes.PAYMENTS_RESOLVED,
            payload: paymentStateMock.withPayments([ paymentMock.withStatus(Payment_Status.NEW).value() ]).value().data!,
        };
        const nextState = reducer(state, action);

        expect(nextState.isFetching).toBe(false);
        expect(nextState.data?.payments[0].status).toBe(Payment_Status.NEW);
    });

    it('переключит isFetching в false и добавит платежи если их не было', () => {
        const state = paymentStateMock.value();
        const action: StatePaymentsActionResolved = {
            type: actionTypes.PAYMENTS_RESOLVED,
            payload: paymentStateMock.withPayments([ paymentMock.withStatus(Payment_Status.NEW).value() ]).value().data!,
        };
        const nextState = reducer(state, action);

        expect(nextState.isFetching).toBe(false);
        expect(nextState.data?.payments[0].status).toBe(Payment_Status.NEW);
    });
});
