jest.mock('auto-core/react/lib/localstorage', () => ({
    getItem: jest.fn(),
    setItem: jest.fn(),
}));

import _ from 'lodash';

import ls from 'auto-core/react/lib/localstorage';

import writeSeenNotification from './writeSeenNotification';

const SALE_HASH_ID = '1234567890-abcdef';
const PARAM_NAME = 'notifications_seen';
const SEEN_NOTIFICATION = [
    `getCallsStats_${ SALE_HASH_ID }`,
    `viewsOverall_${ SALE_HASH_ID }`,
];
const NOTIFICATION_KEY_1 = 'callsCount_1071183995-f2176e';

it('достанет предыдущие нотификации из ls', () => {
    writeSeenNotification(NOTIFICATION_KEY_1);
    expect(ls.getItem).toHaveBeenCalledTimes(1);
    expect(ls.getItem).toHaveBeenCalledWith(PARAM_NAME);
});

it('если не передан ключ, то ничего не сделает', () => {
    writeSeenNotification();
    expect(ls.getItem).toHaveBeenCalledTimes(0);
    expect(ls.setItem).toHaveBeenCalledTimes(0);
});

it('если передан ключ, то добавит его к просмотренным нотификациям', () => {
    (ls.getItem as jest.MockedFunction<typeof ls.getItem>).mockImplementationOnce(() => [
        SEEN_NOTIFICATION,
    ].join(','));
    writeSeenNotification(NOTIFICATION_KEY_1);
    expect(ls.setItem).toHaveBeenCalledTimes(1);
    expect(ls.setItem).toHaveBeenCalledWith(PARAM_NAME, [ SEEN_NOTIFICATION, NOTIFICATION_KEY_1 ].join(','));
});

it('не будет запоминать больше, чем 100 нотификаций', () => {
    (ls.getItem as jest.MockedFunction<typeof ls.getItem>).mockImplementationOnce(() =>
        _.range(100).map(() => SEEN_NOTIFICATION[0]).join(','),
    );
    writeSeenNotification(NOTIFICATION_KEY_1);

    const newStorageValue = (ls.setItem as jest.MockedFunction<typeof ls.setItem>).mock.calls[0][1].split(',');
    expect(newStorageValue).toHaveLength(100);
});
