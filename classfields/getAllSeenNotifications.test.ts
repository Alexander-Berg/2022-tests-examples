jest.mock('auto-core/react/lib/localstorage', () => ({
    getItem: jest.fn(),
    setItem: jest.fn(),
}));

import ls from 'auto-core/react/lib/localstorage';

import getAllSeenNotifications from './getAllSeenNotifications';
import { PARAM_NAME } from './consts';

const SEEN_NOTIFICATION = [
    'getCallsStats_1234567890-abcdef',
    'viewsOverall_1234567890-abcdef',
    'callsCount_1071183995-f2176e',
];

it('достанет предыдущие нотификации из ls', () => {
    (ls.getItem as jest.MockedFunction<typeof ls.getItem>).mockImplementationOnce(() => SEEN_NOTIFICATION.join(','));

    const result = getAllSeenNotifications();
    expect(ls.getItem).toHaveBeenCalledTimes(1);
    expect(ls.getItem).toHaveBeenCalledWith(PARAM_NAME);
    expect(result).toEqual([ 'getCallsStats_1234567890-abcdef', 'viewsOverall_1234567890-abcdef', 'callsCount_1071183995-f2176e' ]);
});
