import { generateImageAliases } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { UserTypes } from 'realty-core/types/common';

const baseProfile = {
    logo: generateImageAliases({ height: 72, width: 72 }),
    foundationDate: '2010-02-28T21:00:00Z',
    address: { unifiedAddress: 'Иркутск, улица Баррикад, 32', point: { latitude: 52.29173, longitude: 52.29173 } },
    description: 'kek',
    userType: UserTypes.AGENCY,
} as const;

export const agentProfiles = {
    order: ['007', '0071', '123123123', '4043632751', '123', '321', '999911', '9999111', '9999112', '9999113'],
    ids: {
        '123': { name: 'Роберт Ханссен', profileUid: '123', creationDate: '2020-04-26T12:57:50Z', ...baseProfile },
        '321': {
            name: 'Мата Хари',
            profileUid: '321',
            creationDate: '2020-03-26T12:57:50Z',
            ...baseProfile,
        },
        '999911': {
            name: 'Клаус Фукс',
            profileUid: '999911',
            creationDate: '2020-08-26T12:57:50Z',
            ...baseProfile,
        },
        '9999111': {
            name: 'Сидней Рейли',
            profileUid: '9999111',
            creationDate: '2020-09-26T12:57:50Z',
            ...baseProfile,
        },
        '9999112': {
            name: 'Ким Филби',
            profileUid: '9999112',
            creationDate: '2020-05-23T12:57:50Z',
            ...baseProfile,
        },
        '9999113': {
            name: 'Олдрич Эймс',
            profileUid: '9999113',
            creationDate: '2020-04-26T12:57:50Z',
            ...baseProfile,
        },
        '123123123': {
            name: 'Рихард Зорге',
            profileUid: '123123123',
            creationDate: '2020-05-26T12:57:50Z',
            ...baseProfile,
        },
        '4043632751': {
            name: 'Ольга Чехова',
            profileUid: '4043632751',
            creationDate: '2020-05-26T12:57:50Z',
            ...baseProfile,
        },
        '007': {
            name: 'Бонд',
            profileUid: '007',
            creationDate: '2020-05-30T12:57:50Z',
            ...baseProfile,
        },
        '0071': {
            name: 'Джеймс Бонд',
            profileUid: '0071',
            creationDate: '2020-04-11T12:57:50Z',
            ...baseProfile,
        },
    } as const,
};

export const agentProfiles2 = {
    order: ['007', '008'],
    ids: {
        '007': {
            name: 'Ян Флеминг 2',
            profileUid: '007',
            creationDate: '2020-05-23T12:57:50Z',
            ...baseProfile,
        },
        '008': {
            name: 'Сидней Рейли',
            profileUid: '008',
            creationDate: '2020-04-11T12:57:50Z',
            ...baseProfile,
        },
    },
};
