import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { UID, UserTypes } from 'realty-core/types/common';
import { WeekDays } from 'realty-core/types/datetime';
import { IDeveloper } from 'realty-core/types/developer';
import { IProfileCard } from 'realty-core/types/profileCard';

const developerBaseMock = {
    id: 1,
    name: 'Самолёт',
    sites: 131,
    geoStatistic: {
        current: {
            rgid: 426676,
            subjectFederationName: 'Москва',
            locativeSubjectFederationName: 'в Москве',
            totalSites: 131,
            totalOffers: 2000,
        },
    },
};

export const developerFullInfo: IDeveloper = {
    ...developerBaseMock,
    born: '1993-12-31T21:00:00Z',
    statistic: {
        allHouses: 1198,
        allSites: 131,
        finished: {
            houses: 896,
            sites: 101,
        },
        unfinished: {
            houses: 302,
            sites: 63,
        },
    },
};

export const developerInfoWithoutSomeObjects: IDeveloper = {
    ...developerBaseMock,
    statistic: {
        allHouses: 1198,
        allSites: 131,
        unfinished: {
            houses: 302,
            sites: 63,
        },
    },
};

export const agencyFullInfo: IProfileCard = {
    name: 'Тестовое агенство',
    profileUid: 'someProfileUidMock' as UID,
    userType: UserTypes.AGENCY,
    logo: {
        appSnippetSmall: 'smallSnippetMock',
        appMiddle: 'middleSnippetMock',
    },
    address: {
        unifiedAddress: 'someUnifiedAddress',
        point: {
            latitude: 123,
            longitude: 456,
        },
        rgid: '222333',
    },
    workSchedule: [
        {
            day: WeekDays.MONDAY,
            minutesFrom: 600,
            minutesTo: 1200,
        },
        {
            day: WeekDays.TUESDAY,
            minutesFrom: 600,
            minutesTo: 1200,
        },
        {
            day: WeekDays.WEDNESDAY,
            minutesFrom: 600,
            minutesTo: 1200,
        },
    ],
    description: 'Some profile description',
    creationDate: '2020-06-01T03:00:00.000Z',
    foundationDate: '1993-12-31T21:00:00Z',
    offerCounters: {
        totalOffers: 15,
        filteredOffers: 15,
    },
};

const imageMock = generateImageUrl({ width: 70, height: 70 });

const developerAuthorInfo = {
    allowedCommunicationChannels: ['COM_CALLS'],
    category: 'DEVELOPER',
    creationDate: '2016-08-29T12:22:44Z',
    encryptedPhones: ['KzcF0OHTUJxMLTYN3NPDMR3'],
    id: '0',
    name: 'Сити-XXI век',
    organization: 'Сити-XXI век',
    partnerSaleAgentId: '84207',
    redirectPhones: true,
    redirectPhonesFailed: false,
};

const agencyAuthorInfo = {
    allowedCommunicationChannels: ['COM_CALLS'],
    category: 'AGENCY',
    creationDate: '2016-08-29T12:22:44Z',
    encryptedPhones: ['KzcF0OHTUJxMLTYN3NPDMR3'],
    id: '0',
    name: 'Тестовое агенство',
    organization: 'Тестовое агенство',
    partnerSaleAgentId: '84207',
    redirectPhones: true,
    redirectPhonesFailed: false,
};

const agentAuthorInfo = {
    agentName: 'Тестовый агент',
    allowedCommunicationChannels: ['COM_CALLS'],
    category: 'AGENT',
    creationDate: '2015-06-22T07:50:58Z',
    encryptedPhones: ['aadq23r4adfgJ3OLDIN1MPTkR0'],
    humanPhoto: imageMock,
    id: '0',
    profile: {
        logo: {
            appLarge: imageMock,
            appMiddle: imageMock,
            appSnippetLarge: imageMock,
            appSnippetMiddle: imageMock,
            appSnippetMini: imageMock,
            appSnippetSmall: imageMock,
            cosmic: imageMock,
            large: imageMock,
            large1242: imageMock,
            main: imageMock,
            alike: imageMock,
            optimize: imageMock,
            origin: imageMock,
        },
        name: 'Тестовый агент',
        userType: 'AGENT',
    },
    redirectPhones: true,
    redirectPhonesFailed: false,
};

const ownerAuthorInfo = {
    agentName: 'Тестовый собственник',
    allowedCommunicationChannels: ['COM_CALLS'],
    category: 'OWNER',
    creationDate: '2015-06-22T07:50:58Z',
    encryptedPhones: ['aadq23r4adfgJ3OLDIN1MPTkR0'],
    humanPhoto: imageMock,
    id: '0',
    profile: {
        logo: {
            appLarge: imageMock,
            appMiddle: imageMock,
            appSnippetLarge: imageMock,
            appSnippetMiddle: imageMock,
            appSnippetMini: imageMock,
            appSnippetSmall: imageMock,
            cosmic: imageMock,
            large: imageMock,
            large1242: imageMock,
            main: imageMock,
            alike: imageMock,
            optimize: imageMock,
            origin: imageMock,
        },
        name: 'Тестовый собственник',
        userType: 'OWNER',
    },
    redirectPhones: true,
    redirectPhonesFailed: false,
};

export const developersOfferMock = {
    active: true,
    offerId: '1234567890',
    offerType: 'SELL',
    offerCategory: 'APARTMENT',
    trust: 'NORMAL',
    url: 'someUrl',
    partnerId: '1069184853',
    clusterSize: 1,
    creationDate: '2021-05-13T14:41:36Z',
    floorsOffered: [4],
    flatType: 'NEW_PRIMARY',
    author: developerAuthorInfo,
};

export const agencyOfferMock = {
    active: true,
    offerId: '1234567890',
    offerType: 'SELL',
    offerCategory: 'APARTMENT',
    trust: 'NORMAL',
    url: 'someUrl',
    partnerId: '1069184853',
    clusterSize: 1,
    creationDate: '2021-05-13T14:41:36Z',
    floorsOffered: [4],
    flatType: 'NEW_SECONDARY',
    author: agencyAuthorInfo,
};

export const agentOfferMock = {
    ...agencyOfferMock,
    author: agentAuthorInfo,
    location: {
        structuredAddress: {
            component: [{}, {}, {}],
        },
    },
};

export const ownerOfferMock = {
    ...agencyOfferMock,
    author: ownerAuthorInfo,
    location: {
        structuredAddress: {
            component: [{}, {}, {}],
        },
    },
};
