import { RequestStatus } from 'realty-core/types/network';

import { PassportVerificationStatus, OwnerCardsStatus } from 'types/user';

export const store = {
    spa: {
        status: RequestStatus.LOADED,
    },
    user: {
        passportVerificationStatus: PassportVerificationStatus.VERIFIED,
        tenantQuestionnaire: {
            personalActivity: {
                activity: 'STUDY',
            },
        },
        email: '123',
        ownerCardsStatus: OwnerCardsStatus.NO_CARDS_BOUND,
    },
    payments: {
        network: {},
    },
    config: {
        realtyUrl: 'https://realty.yandex.ru',
        serverTimeStamp: new Date('18-11-2021').getTime(),
    },
};
