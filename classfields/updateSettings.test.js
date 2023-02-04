/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(() => Promise.resolve({})),
}));

const { getResource } = require('auto-core/react/lib/gateApi');
const actionTypes = require('../actionTypes');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const notifierActionTypes = require('auto-core/react/dataDomain/notifier/types');

const updateSettings = require('./updateSettings');

const clientId = '20101';
let store;
let actionParams;

beforeEach(() => {
    store = mockStore({ config: { client: { id: clientId } } });

    actionParams = {
        calltracking_enabled: true,
        offers_stat_enabled: true,
        target_call_duration: { seconds: 45 },
        unique_call_period: { days: 10 },
        notification_email: 'demo@auto.ru',
        auto_tags: [ { value: 'трейдин' } ],
        tags: [ { value: 'кредит' } ],
    };
});

it('должен вызвать ресурс "updateCallTrackingSettings" с правильно замапленными параметрами', () => {
    store.dispatch(
        updateSettings(actionParams),
    );

    expect(getResource).toHaveBeenCalledWith('updateCallTrackingSettings', {
        settings: {
            calltracking_enabled: true,
            offers_stat_enabled: true,
            unique_call_period: { days: 10 },
            target_call_duration: { seconds: 45 },
            notification_email: 'demo@auto.ru',
            auto_tags: [ { value: 'трейдин' } ],
            tags: [ { value: 'кредит' } ],
        },
        dealer_id: clientId,
    });
});

it('для успешного ответа должен диспатчить экшн обновления настроек и стандартную успешную нотификацию', () => {
    expect.assertions(1);

    return store.dispatch(
        updateSettings(actionParams),
    )
        .then(() => {
            const actions = store.getActions();

            const callsSettingsAction = {
                type: actionTypes.UPDATE_SETTINGS,
                payload: { settings: {} },
            };

            const notificationAction = {
                type: notifierActionTypes.NOTIFIER_SHOW_MESSAGE,
                payload: { message: 'Настройки успешно обновлены', view: 'success' },
            };

            expect(actions).toEqual([ callsSettingsAction, notificationAction ]);
        });
});

it('для неуспешных ответов должен реджектить промис и показывать стандартную нотификацию с ошибкой', () => {
    expect.assertions(1);

    getResource.mockImplementation(() => Promise.reject());

    return store.dispatch(
        updateSettings(actionParams),
    )
        .then(() => {
            const actions = store.getActions();

            const notificationAction = {
                type: notifierActionTypes.NOTIFIER_SHOW_MESSAGE,
                payload: { message: expect.any(String), view: 'error' },
            };

            return expect(actions).toEqual([ notificationAction ]);
        });
});
