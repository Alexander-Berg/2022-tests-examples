import { NotificationCardName } from 'auto-core/types/Notifications';

import getSeenKey from './getSeenKey';

it('правильно формирует ключ для пары аргументов', () => {
    const result = getSeenKey(NotificationCardName.CALLS_COUNT, '1071183995-f2176e');
    expect(result).toBe('callsCount_1071183995-f2176e');
});
