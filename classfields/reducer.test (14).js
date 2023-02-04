const reducer = require('./reducer');

const actionTypes = require('./actionTypes');
const { PAGE_LOADING_SUCCESS } = require('auto-core/react/actionTypes');

it('должен обновлять способ оплаты', () => {
    const state = {
        paymentActions: {
            invoice: true,
            overdraft: false,
        },
    };

    const action = {
        type: actionTypes.UPDATE_PAYMENT_ACTIONS,
        payload: {
            overdraft: true,
        },
    };

    const expectedState = {
        paymentActions: {
            invoice: true,
            overdraft: true,
        },
    };

    expect(reducer(state, action)).toEqual(expectedState);
});

it('должен обновлять параметры страницы при диспатче экшена PAGE_LOADING_SUCCESS', () => {
    const state = {
        foo: 123,
    };

    const dealerAccountPayload = {
        balance: '345',
        payment_actions: [ 1, 2, 3 ],
        overdraft: {},
        rest_days: 5,
        average_outcome: 100,
    };

    const expected = {
        foo: 123,
        balance: 345,
        paymentActions: [ 1, 2, 3 ],
        overdraftInfo: {},
        restDays: 5,
        averageOutcome: 100,
        invoicePersons: [ 222 ],
    };

    const action = {
        type: PAGE_LOADING_SUCCESS,
        payload: {
            getDealerAccount: dealerAccountPayload,
            invoicePersons: { result: [ 222 ] },
        },
    };

    const newState = reducer(state, action);
    expect(newState).toEqual(expected);
});
