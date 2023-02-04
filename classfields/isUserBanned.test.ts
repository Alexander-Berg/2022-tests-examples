import { DomainBan_ResellerType } from '@vertis/schema-registry/ts-types-snake/vertis/passport/common_model';

import type { StateUserData } from 'auto-core/react/dataDomain/user/types';

import isUserBanned from './isUserBanned';

it('вернет true, если у пользователя один бан, но не за перекупа', () => {
    const user: StateUserData = {
        isModerator: false,
        auth: true,
        moderation_status: {
            bans: {
                all: { reasons: [ 'ANY_OTHER_REASON' ], enrichedReasons: [], resellerType: DomainBan_ResellerType.UNKNOWN },
            },
            reseller_flag_updated_by_domain: { all: '' },
            reseller: false,
        },
    };
    expect(isUserBanned(user)).toBe(true);
});

it('вернет true, если у пользователя несколько банов, не только перекуп', () => {
    const user: StateUserData = {
        isModerator: false,
        auth: true,
        moderation_status: {
            bans: {
                all: { reasons: [ 'USER_RESELLER', 'ANY_OTHER_REASON' ], enrichedReasons: [], resellerType: DomainBan_ResellerType.FAST_RESALE },
            },
            reseller_flag_updated_by_domain: { all: '' },
            reseller: true,
        },
    };
    expect(isUserBanned(user)).toBe(true);
});

it('вернет false, если у пользователя только бан за перекупа', () => {
    const user: StateUserData = {
        isModerator: false,
        auth: true,
        moderation_status: {
            bans: {
                all: { reasons: [ 'USER_RESELLER' ], enrichedReasons: [], resellerType: DomainBan_ResellerType.UNKNOWN },
            },
            reseller_flag_updated_by_domain: { all: '' },
            reseller: true,
        },
    };
    expect(isUserBanned(user)).toBe(false);
});

it('вернет false, если у пользователя нет банов', () => {
    const user: StateUserData = {
        isModerator: false,
        auth: true,
    };
    expect(isUserBanned(user)).toBe(false);
});
