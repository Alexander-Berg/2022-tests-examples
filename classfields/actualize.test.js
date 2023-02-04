/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const actualize = require('./actualize');

const {
    OFFER_ACTUALIZE_FAILED,
    OFFER_ACTUALIZE_SUCCESS,
} = require('../actionTypes');
const { NOTIFIER_SHOW_MESSAGE } = require('auto-core/react/dataDomain/notifier/types');
const { ERROR_MESSAGE } = require('auto-core/react/dataDomain/notifier/actions/notifier');

let store;
beforeEach(() => {
    fetch.resetMocks();
    store = mockStore({});
});

it('должен отправить экшен "OFFER_ACTUALIZE_SUCCESS", если запрос прошел успешно', async() => {
    fetch.mockResponseOnce(JSON.stringify({ status: 'SUCCESS', timestamp: 12345 }));

    const expectedActions = [
        {
            type: OFFER_ACTUALIZE_SUCCESS,
            payload: {
                params: { offer: { category: 'cars' } },
                result: { status: 'SUCCESS', timestamp: 12345 },
                timestamp: 12345,
            },
        },
    ];

    await store.dispatch(actualize({ offer: { category: 'cars' } }));
    expect(store.getActions()).toEqual(expectedActions);
});

it('должен вернуть resolved промис с ответом бекенда, если запрос прошел успешно', () => {
    fetch.mockResponseOnce(JSON.stringify({ status: 'SUCCESS', timestamp: 12345 }));

    return store.dispatch(actualize({ offer: { category: 'cars' } })).then(result => {
        expect(result).toEqual({ status: 'SUCCESS', timestamp: 12345 });
    });
});

it('должен отправить экшен "OFFER_ACTUALIZE_FAILED", если запрос прошел неуспешно', async() => {
    expect.assertions(1);

    fetch.mockResponseOnce(JSON.stringify({ status: 'ERROR' }));

    const expectedActions = [
        {
            type: NOTIFIER_SHOW_MESSAGE,
            payload: {
                message: ERROR_MESSAGE,
                view: 'error',
            },
        },
        {
            type: OFFER_ACTUALIZE_FAILED,
            payload: {
                status: 'ERROR',
            },
        },
    ];

    try {
        await store.dispatch(actualize({ offer: { category: 'cars' } }));
    } catch {}

    expect(store.getActions()).toEqual(expectedActions);
});

it('должен вернуть rejected промис с ответом бекенда, если запрос прошел неуспешно', async() => {
    expect.assertions(1);

    fetch.mockResponseOnce(JSON.stringify({ status: 'ERROR' }));

    await expect(
        store.dispatch(actualize({ offer: { category: 'cars' } })),
    ).rejects.toEqual({ status: 'ERROR' });
});
