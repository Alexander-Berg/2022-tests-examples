/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const action = require('./remove');

const {
    SUBSCRIPTION_REMOVE_PENDING,
    SUBSCRIPTION_REMOVE_REJECT,
    SUBSCRIPTION_REMOVE_RESOLVE,
} = require('../actionTypes');
const { NOTIFIER_SHOW_MESSAGE } = require('auto-core/react/dataDomain/notifier/types');
const { ERROR_MESSAGE } = require('auto-core/react/dataDomain/notifier/actions/notifier');

let store;
beforeEach(() => {
    fetch.resetMocks();
    store = mockStore({});
});

it('должен обработать успешный ответ', async() => {
    fetch.mockResponseOnce(JSON.stringify({ status: 'SUCCESS' }));

    const id = '123';

    const expectedActions = [
        {
            type: SUBSCRIPTION_REMOVE_PENDING,
            payload: { id },
        },
        {
            type: NOTIFIER_SHOW_MESSAGE,
            payload: { message: 'Поиск удалён', view: 'success' },
        },
        {
            type: SUBSCRIPTION_REMOVE_RESOLVE,
            payload: { id },
        },
    ];

    await expect(
        store.dispatch(action(id)),
    ).resolves.toEqual({ status: 'SUCCESS' });
    expect(store.getActions()).toEqual(expectedActions);
});

it('должен обработать неуспешный ответ', async() => {
    fetch.mockResponseOnce(JSON.stringify({ status: 'ERROR' }));

    const id = '123';

    const expectedActions = [
        {
            type: SUBSCRIPTION_REMOVE_PENDING,
            payload: { id },
        },
        {
            type: NOTIFIER_SHOW_MESSAGE,
            payload: { message: ERROR_MESSAGE, view: 'error' },
        },
        {
            type: SUBSCRIPTION_REMOVE_REJECT,
            payload: {
                error: { status: 'ERROR' },
                id,
            },
        },
    ];

    await expect(
        store.dispatch(action(id)),
    ).rejects.toEqual({ status: 'ERROR' });
    expect(store.getActions()).toEqual(expectedActions);
});
