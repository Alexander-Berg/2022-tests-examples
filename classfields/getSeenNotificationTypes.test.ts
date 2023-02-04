jest.mock('auto-core/react/lib/localstorage', () => ({
    getItem: jest.fn(),
    setItem: jest.fn(),
}));

import ls from 'auto-core/react/lib/localstorage';

import getSeenNotificationTypes from './getSeenNotificationTypes';

const SALE_HASH_ID = '1234567890-abcdef';
const PARAM_NAME = 'notifications_seen';
const SEEN_NOTIFICATION = [
    `getCallsStats_${ SALE_HASH_ID }`,
    `viewsOverall_${ SALE_HASH_ID }`,
];
const NOTIFICATION_KEY_1 = 'callsCount_1071183995-f2176e';

it('достанет предыдущие нотификации из ls', () => {
    getSeenNotificationTypes(SALE_HASH_ID);
    expect(ls.getItem).toHaveBeenCalledTimes(1);
    expect(ls.getItem).toHaveBeenCalledWith(PARAM_NAME);
});

it('если нотификаций нет, вернет пустой массив', () => {
    const result = getSeenNotificationTypes(SALE_HASH_ID);
    expect(result).toEqual([]);
});

it('если нотификации есть, вернет массив из их типов', () => {
    (ls.getItem as jest.MockedFunction<typeof ls.getItem>).mockImplementationOnce(() => [
        SEEN_NOTIFICATION,
        NOTIFICATION_KEY_1,
    ].join(','));
    const result = getSeenNotificationTypes(SALE_HASH_ID);
    expect(result).toEqual([ 'getCallsStats', 'viewsOverall' ]);
});
