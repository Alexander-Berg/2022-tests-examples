const reducer = require('./reducer');

const actionTypes = require('./actionTypes');
const { PAGE_LOADING_SUCCESS } = require('auto-core/react/actionTypes');
const ROUTES = require('auto-core/router/cabinet.auto.ru/route-names');

const settingsPayload = {
    calltracking_enabled: true,
    offers_stat_enabled: true,
    unique_call_period: { days: 10 },
    target_call_duration: { seconds: 45 },
    notification_email: 'demo@auto.ru',
    auto_tags: [ { value: 'трейдин' } ],
    tags: [ { value: 'кредит' } ],
};

const expected = {
    settings: {
        calltracking_enabled: true,
        offers_stat_enabled: true,
        target_call_duration: { seconds: 45 },
        unique_call_period: { days: 10 },
        notification_email: 'demo@auto.ru',
        auto_tags: [ { value: 'трейдин' } ],
        tags: [ { value: 'кредит' } ],
    },
};

it('должен обновлять настройки коллттрекинга при диспатче экшена PAGE_LOADING_SUCCESS', () => {
    const state = {};

    const action = {
        type: PAGE_LOADING_SUCCESS,
        payload: {
            [ROUTES.callsSettings]: { settings: settingsPayload },
        },
    };

    const newState = reducer(state, action);
    expect(newState).toEqual(expected);
});

it('должен обновлять настройки коллттрекинга при диспатче экшена UPDATE_SETTINGS', () => {
    const state = {};

    const action = {
        type: actionTypes.UPDATE_SETTINGS,
        payload: {
            settings: { settings: settingsPayload },
        },
    };

    const newState = reducer(state, action);
    expect(newState).toEqual(expected);
});
