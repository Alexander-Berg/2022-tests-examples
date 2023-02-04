/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const action = require('./create');

const {
    SUBSCRIPTION_CREATE_PENDING,
    SUBSCRIPTION_CREATE_REJECT,
    SUBSCRIPTION_CREATE_RESOLVE,
} = require('../actionTypes');
const { NOTIFIER_SHOW_MESSAGE } = require('auto-core/react/dataDomain/notifier/types');
const { ERROR_MESSAGE } = require('auto-core/react/dataDomain/notifier/actions/notifier');

let store;
beforeEach(() => {
    fetch.resetMocks();
    store = mockStore({});
});

it('должен обработать успешный ответ', async() => {
    fetch.mockResponseOnce(JSON.stringify({ status: 'SUCCESS', item: { id: '123' } }));

    const body = { foo: 'bar' };

    const expectedActions = [
        {
            type: SUBSCRIPTION_CREATE_PENDING,
            payload: body,
        },
        {
            type: NOTIFIER_SHOW_MESSAGE,
            payload: { message: 'Поиск сохранён', view: 'success' },
        },
        {
            type: SUBSCRIPTION_CREATE_RESOLVE,
            payload: { id: '123' },
        },
    ];

    await expect(
        store.dispatch(action(body)),
    ).resolves.toEqual({ status: 'SUCCESS', item: { id: '123' } });
    expect(store.getActions()).toEqual(expectedActions);
});

it('должен обработать неуспешный ответ', async() => {
    fetch.mockResponseOnce(JSON.stringify({ status: 'ERROR' }));

    const body = { foo: 'bar' };

    const expectedActions = [
        {
            type: SUBSCRIPTION_CREATE_PENDING,
            payload: body,
        },
        {
            type: NOTIFIER_SHOW_MESSAGE,
            payload: { message: ERROR_MESSAGE, view: 'error' },
        },
        {
            type: SUBSCRIPTION_CREATE_REJECT,
            payload: {
                body,
                error: { status: 'ERROR' },
            },
        },
    ];

    await expect(
        store.dispatch(action(body)),
    ).rejects.toEqual({ status: 'ERROR' });
    expect(store.getActions()).toEqual(expectedActions);
});
