import { TrafficSourceInfo } from '@vertis/schema-registry/ts-types/realty/event/model';

import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

const snippet = {
    id: 202457,
    name: 'Пригород Лесное',
    fullName: 'ЖК «Пригород Лесное»',
    locativeFullName: 'в ЖК «Пригород Лесное»',
    location: {
        geoId: 118832,
        rgid: 17401902,
        settlementRgid: 62946,
        settlementGeoId: 118832,
        populatedRgid: 741964,
        address: 'Мисайлово, Молодёжный бул. / Литературный бул.',
        distanceFromRingRoad: 8243,
        subjectFederationId: 1,
        subjectFederationRgid: 741964,
        subjectFederationName: 'Москва и МО',
        point: {
            latitude: 55.558846,
            longitude: 37.81599,
            precision: 'EXACT',
        },
        metro: {
            lineColors: ['4f8242'],
            metroGeoId: 20423,
            rgbColor: '4f8242',
            metroTransport: 'ON_TRANSPORT',
            name: 'Домодедовская',
            timeToMetro: 27,
        },
        metroList: [
            {
                lineColors: ['4f8242'],
                metroGeoId: 20423,
                rgbColor: '4f8242',
                metroTransport: 'ON_TRANSPORT',
                name: 'Домодедовская',
                timeToMetro: 27,
            },
            {
                lineColors: ['4f8242'],
                metroGeoId: 20424,
                rgbColor: '4f8242',
                metroTransport: 'ON_TRANSPORT',
                name: 'Красногвардейская',
                timeToMetro: 32,
            },
            {
                lineColors: ['69cd37'],
                metroGeoId: 20561,
                rgbColor: '69cd37',
                metroTransport: 'ON_TRANSPORT',
                name: 'Зябликово',
                timeToMetro: 33,
            },
            {
                lineColors: ['4f8242'],
                metroGeoId: 114836,
                rgbColor: '4f8242',
                metroTransport: 'ON_TRANSPORT',
                name: 'Алма-Атинская',
                timeToMetro: 35,
            },
            {
                lineColors: ['4f8242', 'df477c'],
                metroGeoId: 20421,
                rgbColor: '4f8242',
                metroTransport: 'ON_TRANSPORT',
                name: 'Царицыно',
                timeToMetro: 35,
            },
        ],
    },
    viewTypes: [
        'GENERAL',
        'GENERAL',
        'GENERAL',
        'GENERAL',
        'COURTYARD',
        'ENTRANCE',
        'ENTRANCE',
        'COURTYARD',
        'GENERAL',
        'GENERAL',
        'COURTYARD',
        'ENTRANCE',
        'HALL',
        'HALL',
        'LIFT',
        'HALL',
        'HALL',
        'COURTYARD',
        'COURTYARD',
        'GENERAL',
        'COURTYARD',
        'GENERAL',
    ],
    images: Array(22).fill(generateImageUrl({ width: 543, height: 332 })),
    appLargeImages: Array(22).fill(generateImageUrl({ width: 883, height: 560 })),
    siteSpecialProposals: [
        {
            proposalType: 'MORTGAGE',
            description: 'Ипотека 2.99% на весь срок',
            mainProposal: true,
            specialProposalType: 'mortgage',
            shortDescription: 'Ипотека 2.99% на весь срок',
        },
        {
            proposalType: 'DISCOUNT',
            description: 'Скидка 110 000 руб.',
            mainProposal: false,
            specialProposalType: 'discount',
            shortDescription: 'Скидка 110 000 руб.',
        },
        {
            proposalType: 'SALE',
            description: 'Первоначальный взнос 0%',
            mainProposal: false,
            specialProposalType: 'sale',
            shortDescription: 'Первоначальный взнос 0%',
        },
        {
            proposalType: 'INSTALLMENT',
            description: 'Беспроцентная рассрочка',
            interestFee: true,
            durationMonths: 3,
            mainProposal: false,
            specialProposalType: 'installment',
            shortDescription: 'Беспроцентная рассрочка',
        },
    ],
    buildingClass: 'ECONOM',
    state: 'UNFINISHED',
    finishedApartments: true,
    price: {
        from: 3559398,
        to: 10628627,
        currency: 'RUR',
        minPricePerMeter: 116400,
        maxPricePerMeter: 170138,
        rooms: {
            '1': {
                soldout: false,
                from: 4748536,
                to: 6634160,
                currency: 'RUR',
                areas: {
                    from: '31.5',
                    to: '44.4',
                },
                hasOffers: false,
                offersCount: 0,
                priceRatioToMarket: 0,
                status: 'ON_SALE',
            },
            '2': {
                soldout: false,
                from: 6313398,
                to: 9706963,
                currency: 'RUR',
                areas: {
                    from: '45.4',
                    to: '62.3',
                },
                hasOffers: false,
                offersCount: 0,
                priceRatioToMarket: 0,
                status: 'ON_SALE',
            },
            '3': {
                soldout: false,
                from: 7836156,
                to: 10271288,
                currency: 'RUR',
                areas: {
                    from: '62.9',
                    to: '77.4',
                },
                hasOffers: false,
                offersCount: 0,
                priceRatioToMarket: 0,
                status: 'ON_SALE',
            },
            OPEN_PLAN: {
                soldout: false,
                currency: 'RUR',
                hasOffers: false,
                offersCount: 0,
                priceRatioToMarket: 0,
            },
            STUDIO: {
                soldout: false,
                from: 3559398,
                to: 5036090,
                currency: 'RUR',
                areas: {
                    from: '21.4',
                    to: '29.6',
                },
                hasOffers: false,
                offersCount: 0,
                priceRatioToMarket: 0,
                status: 'ON_SALE',
            },
            PLUS_4: {
                soldout: false,
                from: 10068638,
                to: 10628627,
                currency: 'RUR',
                areas: {
                    from: '86.5',
                    to: '86.6',
                },
                hasOffers: false,
                offersCount: 0,
                priceRatioToMarket: 0,
                status: 'ON_SALE',
            },
        },
        totalOffers: 0,
        priceRatioToMarket: 0,
        minArea: 21.4,
        maxArea: 86.6,
    },
    flatStatus: 'ON_SALE',
    developers: [
        {
            id: 102320,
            name: 'Группа «Самолет»',
        },
    ],
    phone: {
        phoneWithMask: '+7 495 152 ×× ××',
        phoneHash: 'KzcF0OHTUJxNLTINzMPDERx',
    },
    backCallTrafficInfo: {} as TrafficSourceInfo,
    withBilling: true,
    awards: {},
    limitedCard: false,
};

const state = {
    user: {
        favoritesMap: {},
    },
    geo: {
        rgid: 741965,
        name: 'Санкт-Петербург и ЛО',
        locative: 'в Санкт-Петербурге и ЛО',
    },
};

export const baseState = {
    ...state,
    samoletCard: {
        sites: {
            items: Array(6).fill(snippet),
            pager: {
                page: 0,
                pageSize: 6,
                sitesPageSize: 6,
                totalItems: 6,
                totalPages: 1,
            },
            searchQuery: {},
        },
        slides: [],
        isSitesMoreLoading: false,
    },
    loader: {},
};

export const emptyListState = {
    ...state,
    samoletCard: {
        sites: {
            items: [],
            pager: {
                page: 0,
                pageSize: 6,
                sitesPageSize: 6,
                totalItems: 0,
                totalPages: 0,
            },
            slides: [],
            searchQuery: {},
        },
        slides: [],
        isSitesMoreLoading: false,
    },
    loader: {},
};

export const hasMoreState = {
    ...state,
    samoletCard: {
        sites: {
            items: Array(6).fill(snippet),
            pager: {
                page: 0,
                pageSize: 6,
                sitesPageSize: 6,
                totalItems: 8,
                totalPages: 2,
            },
            searchQuery: {},
        },
        slides: [],
        isSitesMoreLoading: false,
    },
    loader: {},
};

export const loadingMoreState = {
    ...state,
    samoletCard: {
        sites: {
            items: Array(6).fill(snippet),
            pager: {
                page: 0,
                pageSize: 6,
                sitesPageSize: 6,
                totalItems: 8,
                totalPages: 2,
            },
            searchQuery: {},
        },
        slides: [],
        isSitesMoreLoading: true,
    },
    loader: {
        sites: { isLoading: true },
    },
};

export const sliderState = {
    ...state,
    samoletCard: {
        sites: {
            items: Array(6).fill(snippet),
            pager: {
                page: 0,
                pageSize: 6,
                sitesPageSize: 6,
                totalItems: 6,
                totalPages: 1,
            },
            searchQuery: {},
        },
        slides: [
            {
                id: '2',
                rgid: 2,
                siteId: '2',
                type: 'text',
                image: generateImageUrl({ width: 2000, height: 900 }),
                title: 'Белая Дача парк',
                description: '',
                hasButton: true,
                buttonText: 'Выбрать квартиру',
            },
            {
                id: '3',
                rgid: 3,
                siteId: '3',
                type: 'text',
                image: generateImageUrl({ width: 2000, height: 900 }),
                title: '',
                description: 'Ипотека 4.6% на срок до 30 лет',
                hasButton: true,
                buttonText: 'Выбрать квартиру',
            },
        ],
        isSitesMoreLoading: false,
    },
    loader: {},
};
