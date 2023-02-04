import { ISiteSnippetType } from 'realty-core/types/siteSnippet';
import { ISiteCard } from 'realty-core/types/siteCard';
import { ILocation } from 'realty-core/types/location';

const commonSiteData = {
    price: {
        rooms: {},
    },
    flatStatus: 'ON_SALE' as const,
    siteSpecialProposals: [],
    images: Array(6).fill('site.com/image.png'),
    appMiddleImages: Array(6).fill('site.com/image.png'),
    appLargeImages: Array(6).fill('site.com/image.png'),
    viewTypes: Array(6).fill('GENERAL'),
};

export const site1 = ({
    id: 1,
    name: 'Вишневый сад',
    fullName: 'ЖК «Вишневый сад»',
    location: {
        rgid: 193290,
        settlementRgid: 165705,
        populatedRgid: 741964,
        address: 'Москва, ул. Выборгская / ул. Адмирала Макарова',
        metro: {
            metroCityRgid: 741964,
            metroGeoId: 20370,
            rgbColor: '4f8242',
            metroTransport: 'ON_FOOT',
            name: 'Водный Стадион',
            timeToMetro: 7,
            lineColors: ['4f8242'],
        },
        subjectFederationRgid: 1,
    },
    developers: [
        {
            id: 32522,
            name: 'Концерн «КРОСТ»',
        },
    ],
    ...commonSiteData,
} as unknown) as ISiteSnippetType;

export const site2 = ({
    id: 2,
    name: 'Невский',
    fullName: 'ЖК «Невский»',
    location: {
        rgid: 193290,
        settlementRgid: 165705,
        populatedRgid: 741964,
        address: 'Москва, ул. Заречная, вл. 2/1',
        metro: {
            metroCityRgid: 741964,
            metroGeoId: 20459,
            rgbColor: '099dd4',
            metroTransport: 'ON_FOOT',
            name: 'Фили',
            timeToMetro: 15,
            lineColors: ['099dd4', 'ed9f2d'],
        },
        subjectFederationRgid: 1,
    },
    developers: [
        {
            id: 324,
            name: 'Группа Компаний ПИК',
        },
    ],
    ...commonSiteData,
} as unknown) as ISiteSnippetType;

export const site3 = ({
    id: 3,
    name: 'Счастье есть',
    fullName: 'ЖК «Счастье есть»',
    location: {
        rgid: 193290,
        settlementRgid: 165705,
        populatedRgid: 741964,
        address: 'Москва, пос. Московский, Родниковая ул.',
        metro: {
            metroCityRgid: 741964,
            metroGeoId: 144826,
            rgbColor: 'e4402d',
            metroTransport: 'ON_TRANSPORT',
            name: 'Саларьево',
            timeToMetro: 9,
            lineColors: ['e4402d'],
        },
        subjectFederationRgid: 1,
    },
    developers: [
        {
            id: 23,
            name: 'Элита',
        },
        {
            id: 56,
            name: 'Строим как можем',
        },
    ],
    ...commonSiteData,
} as unknown) as ISiteSnippetType;

export const site4 = ({
    id: 4,
    name: 'Мой двор',
    fullName: 'ЖК «Мой двро»',
    location: {
        rgid: 193290,
        settlementRgid: 165705,
        populatedRgid: 741964,
        address: 'Москва, ул. Зеленой травы, 99',
        metro: {
            metroCityRgid: 741964,
            metroGeoId: 20370,
            rgbColor: '4f8242',
            metroTransport: 'ON_FOOT',
            name: 'Водный Стадион',
            timeToMetro: 7,
            lineColors: ['4f8242'],
        },
        subjectFederationRgid: 1,
    },
    developers: [
        {
            id: 32522,
            name: 'Главстрой',
        },
    ],
    ...commonSiteData,
} as unknown) as ISiteSnippetType;

export const site5 = ({
    id: 6,
    name: 'Космический район',
    fullName: 'ЖК «Космический район»',
    location: {
        rgid: 193290,
        settlementRgid: 165705,
        populatedRgid: 741964,
        address: 'Москва, ул. Марсовая',
        metro: {
            metroCityRgid: 741964,
            metroGeoId: 20459,
            rgbColor: '099dd4',
            metroTransport: 'ON_FOOT',
            name: 'Фили',
            timeToMetro: 15,
            lineColors: ['099dd4', 'ed9f2d'],
        },
        subjectFederationRgid: 1,
    },
    developers: [
        {
            id: 324,
            name: 'ШлакоСтройГлавСнаб',
        },
    ],
    ...commonSiteData,
} as unknown) as ISiteSnippetType;

export const siteCard: ISiteCard = {
    id: 1,
    fullName: 'ЖК «Октябрьское поле»',
    locativeFullName: 'в ЖК «Октябрьское поле»',
    name: 'Октябрьское поле',
    resaleTotalOffers: 0,
    flatStatus: 'SOLD',
    timestamp: 0,
    location: ({
        address: 'Москва, ш. Киевское, пос. Московский',
        rgid: 1,
        settlementRgid: 165705,
        populatedRgid: 741964,
        subjectFederationRgid: 1,
        subjectFederationId: 1,
        metroList: [],
    } as unknown) as ILocation,
    developers: [],
    isFromPik: false,
    regionInfo: {
        parents: [
            {
                id: 120538,
                rgid: '193368',
                name: 'Пресненский район',
                type: 'CITY_DISTRICT',
            },
        ],
        rgid: 197177,
        populatedRgid: 741964,
        name: 'Московский Международный Деловой Центр Москва-Сити',
        locative: 'Москва-Сити',
        isInLO: false,
        isInMO: true,
    },
};
