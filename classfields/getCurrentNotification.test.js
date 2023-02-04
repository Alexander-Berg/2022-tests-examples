const _ = require('lodash');

const clientMock = require('www-cabinet/react/dataDomain/client/mocks/active.mock');
const balanceRechargeMock = require('www-cabinet/react/dataDomain/balanceRecharge/mocks/withBalance.mock');

const getCurrentNotification = require('./getCurrentNotification');

const ROUTES = require('auto-core/router/cabinet.auto.ru/route-names');
const NOTIFICATION_TYPES = require('www-cabinet/data/state/notification-types');

let state;

beforeEach(() => {
    state = {
        client: _.cloneDeep(clientMock),
        config: {
            routeName: ROUTES.wallet,
        },
        balanceRecharge: _.cloneDeep(balanceRechargeMock),
        callsCampaign: {
            inactiveReason: 'DAILY_LIMIT_EXCEEDED',
        },
        cookies: {},
    };
});

it('не должен отдавать нотификацию, если стоит кука', () => {
    state.cookies = { ['is_hide_notice_banner_calls']: '1' };

    const result = getCurrentNotification(state);

    expect(result).toBeUndefined();
});

it('не должен отдавать нотификацию для нового клиента', () => {
    state.client.status = 'new';

    const result = getCurrentNotification(state);

    expect(result).toBeUndefined();
});

it('не должен отдавать нотификацию для роута start', () => {
    state.config.routeName = ROUTES.start;

    const result = getCurrentNotification(state);

    expect(result).toBeUndefined();
});

it('должен отдавать нотификацию о заблокированном кабинете для статуса клиента "freezed"', () => {
    state.client.status = 'freezed';

    const result = getCurrentNotification(state);

    expect(result).toBe(NOTIFICATION_TYPES.CABINET_IS_BLOCKED);
});

it('должен отдавать нотификацию о заканчивающихся средствах, если осталось мало rest days', () => {
    state.balanceRecharge.restDays = 4;

    const result = getCurrentNotification(state);

    expect(result).toBe(NOTIFICATION_TYPES.SMALL_BALANCE_AMOUNT);
});

it('должен отдавать нотификацию об отсутствии баланса, если оставшийся баланс меньше среднего расхода', () => {
    state.balanceRecharge.restDays = 0;
    state.balanceRecharge.balance = 100;

    const result = getCurrentNotification(state);

    expect(result).toBe(NOTIFICATION_TYPES.NOT_ENOUGH_BALANCE_AMOUNT);
});

it('должен отдавать нотификацию о блокировке тарифа звонков по дневному лимиту', () => {
    state.callsCampaign.inactiveReason = 'DAILY_LIMIT_EXCEEDED';

    const result = getCurrentNotification(state);

    expect(result).toBe(NOTIFICATION_TYPES.CALLS_DAILY_LIMIT_EXCEEDED);
});

it('должен отдавать нотификацию о блокировке тарифа звонков по депозиту', () => {
    state.callsCampaign.inactiveReason = 'DEPOSIT_LIMIT_EXCEEDED';

    const result = getCurrentNotification(state);

    expect(result).toBe(NOTIFICATION_TYPES.CALLS_DEPOSIT_LIMIT_EXCEEDED);
});

it('должен вернуть undefined, в случае ошибки баланса (balanceRecharge = {})', () => {
    state.balanceRecharge = {};
    state.callsCampaign = {};

    const result = getCurrentNotification(state);

    expect(result).toBe(undefined);
});
