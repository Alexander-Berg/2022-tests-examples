/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const action = require('./sendEmailConfirmation');

const {
    SUBSCRIPTION_EMAIL_CONFIRMATION_PENDING,
    SUBSCRIPTION_EMAIL_CONFIRMATION_REJECT,
    SUBSCRIPTION_EMAIL_CONFIRMATION_RESOLVE,
    SUBSCRIPTION_EMAIL_CONFIRMATION_YA,
} = require('../actionTypes');
const { NOTIFIER_SHOW_MESSAGE } = require('auto-core/react/dataDomain/notifier/types');
const { ERROR_MESSAGE } = require('auto-core/react/dataDomain/notifier/actions/notifier');

let store;
beforeEach(() => {
    fetch.resetMocks();
    store = mockStore({});
});

let actionArgs;
let actionPayload;
beforeEach(() => {
    actionArgs = [
        { id: '123' },
    ];

    actionPayload = {
        id: '123',
    };
});

it('должен обработать успешный ответ', async() => {
    fetch.mockResponseOnce(JSON.stringify({ status: 'SUCCESS' }));

    const expectedActions = [
        {
            type: SUBSCRIPTION_EMAIL_CONFIRMATION_PENDING,
            payload: actionPayload,
        },
        {
            type: SUBSCRIPTION_EMAIL_CONFIRMATION_RESOLVE,
            payload: actionPayload,
        },
    ];

    await expect(
        store.dispatch(action(...actionArgs)),
    ).resolves.toEqual({ status: 'SUCCESS' });
    expect(store.getActions()).toEqual(expectedActions);
});

it('должен обработать неуспешный ответ', async() => {
    fetch.mockResponseOnce(JSON.stringify({ status: 'ERROR' }));

    const expectedActions = [
        {
            type: SUBSCRIPTION_EMAIL_CONFIRMATION_PENDING,
            payload: actionPayload,
        },
        {
            type: NOTIFIER_SHOW_MESSAGE,
            payload: { message: ERROR_MESSAGE, view: 'error' },
        },
        {
            type: SUBSCRIPTION_EMAIL_CONFIRMATION_REJECT,
            payload: {
                id: '123',
                error: { status: 'ERROR' },
            },
        },
    ];

    await expect(
        store.dispatch(action(...actionArgs)),
    ).rejects.toEqual({ status: 'ERROR' });
    expect(store.getActions()).toEqual(expectedActions);
});

it('должен обработать ответ { error: "PASSWORD_AUTH_REQUIRED" }', async() => {
    fetch.mockResponseOnce(JSON.stringify({ status: 'ERROR', error: 'PASSWORD_AUTH_REQUIRED' }));

    const expectedActions = [
        {
            type: SUBSCRIPTION_EMAIL_CONFIRMATION_PENDING,
            payload: actionPayload,
        },
        {
            type: SUBSCRIPTION_EMAIL_CONFIRMATION_YA,
            payload: actionPayload,
        },
    ];

    await expect(
        store.dispatch(action(...actionArgs)),
    ).rejects.toEqual({ status: 'ERROR', error: 'PASSWORD_AUTH_REQUIRED' });
    expect(store.getActions()).toEqual(expectedActions);
});
