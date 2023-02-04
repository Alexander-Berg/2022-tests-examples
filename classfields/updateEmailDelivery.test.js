/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const action = require('./updateEmailDelivery');

const {
    SUBSCRIPTION_UPDATE_EMAIL_DELIVERY_PENDING,
    SUBSCRIPTION_UPDATE_EMAIL_DELIVERY_REJECT,
    SUBSCRIPTION_UPDATE_EMAIL_DELIVERY_RESOLVE,
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
        {
            id: '123',
            deliveries: {},
        },
        {
            enabled: true,
            period: '1h',
        },
    ];

    actionPayload = {
        id: '123',
        deliveries: {
            email_delivery: {
                enabled: true,
                period: '1h',
            },
        },
    };
});

it('должен обработать успешный ответ', async() => {
    fetch.mockResponseOnce(JSON.stringify({ status: 'SUCCESS', item: { id: '123' } }));

    const expectedActions = [
        {
            type: SUBSCRIPTION_UPDATE_EMAIL_DELIVERY_PENDING,
            payload: actionPayload,
        },
        {
            type: SUBSCRIPTION_UPDATE_EMAIL_DELIVERY_RESOLVE,
            payload: actionPayload,
        },
    ];

    await expect(
        store.dispatch(action(...actionArgs)),
    ).resolves.toEqual({ status: 'SUCCESS', item: { id: '123' } });
    expect(store.getActions()).toEqual(expectedActions);
});

it('должен обработать неуспешный ответ', async() => {
    fetch.mockResponseOnce(JSON.stringify({ status: 'ERROR' }));

    const expectedActions = [
        {
            type: SUBSCRIPTION_UPDATE_EMAIL_DELIVERY_PENDING,
            payload: actionPayload,
        },
        {
            type: NOTIFIER_SHOW_MESSAGE,
            payload: { message: ERROR_MESSAGE, view: 'error' },
        },
        {
            type: SUBSCRIPTION_UPDATE_EMAIL_DELIVERY_REJECT,
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
