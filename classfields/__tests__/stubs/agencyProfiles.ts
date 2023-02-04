import { generateImageAliases } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { UserTypes } from 'realty-core/types/common';

const baseProfile = {
    logo: generateImageAliases({ height: 72, width: 72 }),
    foundationDate: '2010-02-28T21:00:00Z',
    address: { unifiedAddress: 'Иркутск, улица Баррикад, 32', point: { latitude: 52.29173, longitude: 52.29173 } },
    description: 'kek',
    userType: UserTypes.AGENT,
} as const;

export const agencyProfiles = {
    order: [
        '4043632751',
        '9999900',
        '123123123',
        '1231231223123',
        '123',
        '321',
        '999911',
        '9999111',
        '9999112',
        '9999113',
    ],
    ids: {
        '123': {
            name: 'Вел',
            profileUid: '123',
            creationDate: '2020-04-26T12:57:50Z',
            ...baseProfile,
        },
        '321': {
            name: 'А роза упала на лапу Азора',
            profileUid: '321',
            creationDate: '2020-03-26T12:57:50Z',
            ...baseProfile,
        },
        '999911': {
            name: 'Sum summus mus',
            profileUid: '999911',
            creationDate: '2020-05-26T12:57:50Z',
            ...baseProfile,
        },
        '9999111': {
            name: 'Dis aliter visum',
            profileUid: '9999111',
            creationDate: '2019-11-26T12:57:50Z',
            ...baseProfile,
        },
        '9999112': {
            name: 'Уникальный agency',
            profileUid: '9999112',
            creationDate: '2018-11-17T12:57:50Z',
            ...baseProfile,
        },
        '9999113': {
            name: 'Вершины достигаются не сразу',
            profileUid: '9999113',
            creationDate: '2020-08-26T12:57:50Z',
            ...baseProfile,
        },
        '9999900': {
            name: 'Домру',
            profileUid: '9999900',
            creationDate: '2020-04-26T12:57:50Z',
            ...baseProfile,
        },
        '123123123': {
            name: 'Родительский дом',
            profileUid: '123123123',
            creationDate: '2020-05-26T12:57:50Z',
            ...baseProfile,
        },
        '4043632751': {
            name: 'agency',
            profileUid: '4043632751',
            creationDate: '2020-05-30T12:57:50Z',
            ...baseProfile,
        },
        '1231231223123': {
            name: 'Чуваки из планеты N 7',
            profileUid: '1231231223123',
            creationDate: '2020-05-26T12:57:50Z',
            ...baseProfile,
        },
    },
};
