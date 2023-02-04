/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const downloadRecord = require('./downloadRecord');
jest.mock('www-cabinet/react/dataDomain/notifier/actions', () => ({
    showErrorNotification: () => ({ type: 'SHOW_ERROR_NOTIFICATION' }),
    showInfoNotification: () => ({ type: 'SHOW_INFO_NOTIFICATION' }),
}));

const mockStore = require('autoru-frontend/mocks/mockStore').default;

let originalWindowLocation;
beforeEach(() => {
    originalWindowLocation = global.window.location;
    delete global.window.location;

    global.window.location = {};
});

afterEach(() => {
    global.window.location = originalWindowLocation;
});

it('должен сеттить правильный url в window.location', () => {
    const gateApi = require('auto-core/react/lib/gateApi');
    const store = mockStore({});
    gateApi.getResource = jest.fn(() => Promise.resolve(true));

    return store.dispatch(downloadRecord('call_id_1'))
        .then(() => {
            expect(global.window.location.href).toBe('/calls/record/?call_id=call_id_1&download=true');
        });
});

it('должен показать ошибку, если запись звонка не найдена', () => {
    const gateApi = require('auto-core/react/lib/gateApi');
    const store = mockStore({});
    gateApi.getResource = jest.fn(() => Promise.reject());

    return store.dispatch(
        downloadRecord('call_id_1'),
    ).then(() => {
        expect(store.getActions()).toEqual([
            { type: 'SHOW_ERROR_NOTIFICATION' },
        ]);
    });
});
