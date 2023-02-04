jest.mock('www-cabinet/react/dataDomain/state/selectors/getCurrentNotification', () => {
    return jest.fn();
});

jest.mock('auto-core/react/dataDomain/cookies/actions/setToRoot', () => {
    return {
        'default': jest.fn(() => () => {}),
    };
});

const MockDate = require('mockdate');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const getCurrentNotification = require('www-cabinet/react/dataDomain/state/selectors/getCurrentNotification');
const setToRoot = require('auto-core/react/dataDomain/cookies/actions/setToRoot').default;

const hideNotification = require('./hideNotification');

const NOTIFICATION_TYPES = require('www-cabinet/data/state/notification-types');

const TEST_CASES = [
    { type: NOTIFICATION_TYPES.SMALL_BALANCE_AMOUNT, cookie: 'is_hide_notice_banner_forecast' },
    { type: NOTIFICATION_TYPES.NOT_ENOUGH_BALANCE_AMOUNT, cookie: 'is_hide_notice_banner_forecast' },
    { type: NOTIFICATION_TYPES.CALLS_DAILY_LIMIT_EXCEEDED, cookie: 'is_hide_notice_banner_calls' },
    { type: NOTIFICATION_TYPES.CALLS_DEPOSIT_LIMIT_EXCEEDED, cookie: 'is_hide_notice_banner_calls' },
];

TEST_CASES.forEach((testCase) => {
    it(`должен сеттить куку с правильными параметрами для нотификации "${ testCase.type }"`, () => {
        MockDate.set('2019-03-01 13:13:13');

        getCurrentNotification.mockImplementation(() => testCase.type);

        const store = mockStore();

        store.dispatch(
            hideNotification(),
        );

        expect(setToRoot).toHaveBeenCalledWith(testCase.cookie, '1', { expires: 1 });
    });
});
