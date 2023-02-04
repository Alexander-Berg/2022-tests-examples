import noop from 'lodash/noop';

import { ISitePlansV2Props } from '../types';

const getPlans = () => [
    {
        roomType: 'STUDIO',
        roomCount: 0,
        wholeArea: {
            value: 21.5,
            unit: 'SQ_M',
        },
        livingArea: {
            value: 10.3,
            unit: 'SQ_M',
        },
        images: {},
        clusterId: '375274-1200-E6D72ED0C12B3EFC',
        floors: [3, 5, 7, 9, 11, 13, 15, 17],
        commissioningDate: [
            {
                year: 2023,
                quarter: 1,
                constructionState: 'CONSTRUCTION_STATE_UNFINISHED',
            },
            {
                year: 2023,
                quarter: 2,
                constructionState: 'CONSTRUCTION_STATE_UNFINISHED',
            },
            {
                year: 2023,
                quarter: 3,
                constructionState: 'CONSTRUCTION_STATE_UNFINISHED',
            },
        ],
        pricePerOffer: {
            currency: 'RUB',
            from: '5873800',
            to: '7030500',
        },
        pricePerMeter: {
            currency: 'RUB',
            from: '273200',
            to: '327000',
        },
        offersCount: 2,
        offerId: '5444798587497977345',
    },
    {
        roomType: 'STUDIO',
        roomCount: 0,
        wholeArea: {
            value: 19.7,
            unit: 'SQ_M',
        },
        livingArea: {
            value: 9.4,
            unit: 'SQ_M',
        },
        images: {},
        clusterId: '375274-1200-8F0B651CDE02929A',
        floors: [2, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16],
        commissioningDate: [
            {
                year: 2023,
                quarter: 2,
                constructionState: 'CONSTRUCTION_STATE_UNFINISHED',
            },
            {
                year: 2023,
                quarter: 3,
                constructionState: 'CONSTRUCTION_STATE_UNFINISHED',
            },
        ],
        pricePerOffer: {
            currency: 'RUB',
            from: '5811500',
            to: '6422200',
        },
        pricePerMeter: {
            currency: 'RUB',
            from: '294999',
            to: '325999',
        },
        offersCount: 1,
        offerId: '6258444696742167171',
    },
];

const getOffers = () => [
    {
        offerId: '6258444696742167171',
        building: {
            builtYear: 2023,
            builtQuarter: 3,
            buildingState: 'UNFINISHED',
            buildingType: 'MONOLIT',
            improvements: {
                PARKING: true,
                LIFT: true,
            },
            parkingType: 'OPEN',
            siteId: 375274,
            siteName: 'Саларьево парк',
            siteDisplayName: 'Жилой район «Саларьево парк»',
            developerIds: [52308],
            houseId: '2704153',
            houseReadableName: 'Корпус 55',
            heatingType: 'UNKNOWN',
            hasDeveloperChat: true,
            apartmentType: 'FLATS',
            buildingImprovementsMap: {
                PARKING: true,
                LIFT: true,
            },
        },
        floorsOffered: [3],
        floorsTotal: 17,
        area: {
            value: 19.7,
            unit: 'SQUARE_METER',
        },
        apartment: {
            renovation: 'TURNKEY',
            decoration: 'TURNKEY',
            siteFlatPlanId: '375274-1200-8F0B651CDE02929A',
        },
        price: {
            currency: 'RUR',
            value: 5811500,
            period: 'WHOLE_LIFE',
            unit: 'WHOLE_OFFER',
            trend: 'INCREASED',
            previous: 5772100,
            hasPriceHistory: true,
            valuePerPart: 295000,
            unitPerPart: 'SQUARE_METER',
            valueForWhole: 5811500,
            unitForWhole: 'WHOLE_OFFER',
            price: {
                value: 5811500,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE',
            },
            pricePerPart: {
                value: 295000,
                currency: 'RUB',
                priceType: 'PER_METER',
                pricingPeriod: 'WHOLE_LIFE',
            },
            priceForWhole: {
                value: 5811500,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE',
            },
        },
        flatType: 'NEW_FLAT',
        offerType: 'SELL',
        offerCategory: 'APARTMENT',
        house: {
            bathroomUnit: 'MATCHED',
            windowView: 'YARD',
            studio: true,
            apartments: false,
            housePart: false,
        },
        partnerId: '1069052819',
        uid: '561248415',
        primarySaleV2: true,
        location: {
            rgid: 17385368,
            geoId: 114619,
            populatedRgid: 741964,
            subjectFederationId: 1,
            subjectFederationRgid: 741964,
            settlementRgid: 587795,
            settlementGeoId: 213,
            address: 'Москва, поселение Сосенское, жилой комплекс Саларьево Парк, 55',
            geocoderAddress: 'Россия, Москва, поселение Сосенское, жилой комплекс Саларьево Парк, 55',
            structuredAddress: {
                component: [
                    {
                        value: 'Россия',
                        geoId: 225,
                        regionGraphId: '143',
                        address: 'Россия',
                        regionType: 'COUNTRY',
                        queryParams: {
                            rgid: '143',
                            address: 'Россия',
                        },
                    },
                    {
                        value: 'Москва',
                        geoId: 213,
                        regionGraphId: '587795',
                        address: 'Москва',
                        regionType: 'CITY',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва',
                        },
                    },
                    {
                        value: 'Новомосковский административный округ',
                        geoId: 213,
                        regionGraphId: '587795',
                        address: 'Новомосковский административный округ',
                        regionType: 'SUBJECT_FEDERATION_DISTRICT',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва, Новомосковский административный округ',
                        },
                    },
                    {
                        value: 'поселение Московский',
                        geoId: 213,
                        regionGraphId: '587795',
                        address: 'поселение Московский',
                        regionType: 'SUBJECT_FEDERATION_DISTRICT',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва, Новомосковский административный округ, поселение Московский',
                        },
                    },
                    {
                        value: 'квартал № 77',
                        geoId: 213,
                        regionGraphId: '587795',
                        address: 'квартал № 77',
                        queryParams: {
                            rgid: '587795',
                            address:
                                // eslint-disable-next-line max-len
                                'Россия, Москва, Новомосковский административный округ, поселение Московский, квартал № 77',
                        },
                    },
                ],
            },
            point: {
                latitude: 55.6068,
                longitude: 37.41277,
                precision: 'EXACT',
            },
            metro: {
                metroGeoId: 218467,
                name: 'Филатов Луг',
                metroTransport: 'ON_FOOT',
                timeToMetro: 12,
                latitude: 55.60118,
                longitude: 37.407997,
                minTimeToMetro: 12,
                lineColors: ['e4402d'],
                rgbColor: 'e4402d',
            },
            station: {
                name: 'Новопеределкино',
                distanceKm: 3.886,
            },
            streetAddress: 'Киевское шоссе 23-й км, 5к1',
            metroList: [
                {
                    metroGeoId: 218467,
                    name: 'Филатов Луг',
                    metroTransport: 'ON_FOOT',
                    timeToMetro: 12,
                    latitude: 55.60118,
                    longitude: 37.407997,
                    minTimeToMetro: 12,
                    lineColors: ['e4402d'],
                    rgbColor: 'e4402d',
                },
                {
                    metroGeoId: 144826,
                    name: 'Саларьево',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 12,
                    latitude: 55.621815,
                    longitude: 37.424057,
                    minTimeToMetro: 12,
                    lineColors: ['e4402d'],
                    rgbColor: 'e4402d',
                },
                {
                    metroGeoId: 218466,
                    name: 'Прокшино',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 17,
                    latitude: 55.586456,
                    longitude: 37.43322,
                    minTimeToMetro: 17,
                    lineColors: ['e4402d'],
                    rgbColor: 'e4402d',
                },
            ],
            parks: [
                {
                    parkId: '121406591',
                    name: 'Ульяновский лесопарк',
                    timeOnFoot: 180,
                    distanceOnFoot: 250,
                    latitude: 55.61996,
                    longitude: 37.41289,
                    timeDistanceList: [
                        {
                            transport: 'ON_FOOT',
                            time: 3,
                            distance: 250,
                        },
                    ],
                },
            ],
            ponds: [
                {
                    pondId: '137693384',
                    name: 'река Сетунь',
                    timeOnFoot: 456,
                    distanceOnFoot: 633,
                    latitude: 55.615074,
                    longitude: 37.412415,
                    timeDistanceList: [
                        {
                            transport: 'ON_FOOT',
                            time: 7,
                            distance: 633,
                        },
                    ],
                },
            ],
            airports: [
                {
                    id: '878065',
                    name: 'Внуково',
                    timeOnCar: 1511,
                    distanceOnCar: 13636,
                    latitude: 55.604942,
                    longitude: 37.282578,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 25,
                            distance: 13636,
                        },
                    ],
                },
                {
                    id: '858742',
                    name: 'Домодедово',
                    timeOnCar: 2587,
                    distanceOnCar: 47452,
                    latitude: 55.41435,
                    longitude: 37.90048,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 43,
                            distance: 47452,
                        },
                    ],
                },
                {
                    id: '878042',
                    name: 'Шереметьево',
                    timeOnCar: 2861,
                    distanceOnCar: 48436,
                    latitude: 55.963852,
                    longitude: 37.4169,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 47,
                            distance: 48436,
                        },
                    ],
                },
                {
                    id: '878109',
                    name: 'Жуковский (Раменское)',
                    timeOnCar: 4518,
                    distanceOnCar: 67358,
                    latitude: 55.568665,
                    longitude: 38.143654,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 75,
                            distance: 67358,
                        },
                    ],
                },
            ],
            allHeatmaps: [
                {
                    name: 'carsharing',
                    rgbColor: 'ee4613',
                    description: 'очень низкая',
                    level: 1,
                    maxLevel: 8,
                    title: 'Доступность Яндекс.Драйва',
                },
            ],
            routeDistances: [
                {
                    geoPoint: {
                        latitude: 55.6387,
                        longitude: 37.459286,
                    },
                    distance: 5716,
                },
            ],
            subjectFederationName: 'Москва и МО',
            cityCenter: [
                {
                    transport: 'ON_CAR',
                    time: 2124,
                    distance: 27211,
                    latitude: 55.749058,
                    longitude: 37.612267,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 35,
                            distance: 27211,
                        },
                    ],
                },
            ],
        },
        queryId: '56d1e267c75af91073eb4fbef09633a0',
        author: {
            category: 'DEVELOPER',
        },
    },
    {
        offerId: '4603801968465129531',
        building: {
            builtYear: 2023,
            builtQuarter: 3,
            buildingState: 'UNFINISHED',
            buildingType: 'MONOLIT',
            improvements: {
                PARKING: true,
                LIFT: true,
            },
            parkingType: 'OPEN',
            siteId: 375274,
            siteName: 'Саларьево парк',
            siteDisplayName: 'Жилой район «Саларьево парк»',
            developerIds: [52308],
            houseId: '2704153',
            houseReadableName: 'Корпус 55',
            heatingType: 'UNKNOWN',
            hasDeveloperChat: true,
            apartmentType: 'FLATS',
            buildingImprovementsMap: {
                PARKING: true,
                LIFT: true,
            },
        },
        floorsOffered: [6],
        floorsTotal: 17,
        area: {
            value: 19.7,
            unit: 'SQUARE_METER',
        },
        apartment: {
            renovation: 'TURNKEY',
            decoration: 'TURNKEY',
            siteFlatPlanId: '375274-1200-8F0B651CDE02929A',
        },
        price: {
            currency: 'RUR',
            value: 5815440,
            period: 'WHOLE_LIFE',
            unit: 'WHOLE_OFFER',
            trend: 'DECREASED',
            previous: 5854840,
            hasPriceHistory: true,
            valuePerPart: 295200,
            unitPerPart: 'SQUARE_METER',
            valueForWhole: 5815440,
            unitForWhole: 'WHOLE_OFFER',
            price: {
                value: 5815440,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE',
            },
            pricePerPart: {
                value: 295200,
                currency: 'RUB',
                priceType: 'PER_METER',
                pricingPeriod: 'WHOLE_LIFE',
            },
            priceForWhole: {
                value: 5815440,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE',
            },
        },
        flatType: 'NEW_FLAT',
        offerType: 'SELL',
        offerCategory: 'APARTMENT',
        house: {
            bathroomUnit: 'MATCHED',
            windowView: 'YARD',
            studio: true,
            apartments: false,
            housePart: false,
        },
        partnerId: '1069052819',
        uid: '561248415',
        primarySaleV2: true,
        location: {
            rgid: 17385368,
            geoId: 114619,
            populatedRgid: 741964,
            subjectFederationId: 1,
            subjectFederationRgid: 741964,
            settlementRgid: 587795,
            settlementGeoId: 213,
            address: 'Москва, поселение Сосенское, жилой комплекс Саларьево Парк, 55',
            geocoderAddress: 'Россия, Москва, поселение Сосенское, жилой комплекс Саларьево Парк, 55',
            structuredAddress: {
                component: [
                    {
                        value: 'Россия',
                        geoId: 225,
                        regionGraphId: '143',
                        address: 'Россия',
                        regionType: 'COUNTRY',
                        queryParams: {
                            rgid: '143',
                            address: 'Россия',
                        },
                    },
                    {
                        value: 'Москва',
                        geoId: 213,
                        regionGraphId: '587795',
                        address: 'Москва',
                        regionType: 'CITY',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва',
                        },
                    },
                    {
                        value: 'Новомосковский административный округ',
                        geoId: 213,
                        regionGraphId: '587795',
                        address: 'Новомосковский административный округ',
                        regionType: 'SUBJECT_FEDERATION_DISTRICT',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва, Новомосковский административный округ',
                        },
                    },
                    {
                        value: 'поселение Московский',
                        geoId: 213,
                        regionGraphId: '587795',
                        address: 'поселение Московский',
                        regionType: 'SUBJECT_FEDERATION_DISTRICT',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва, Новомосковский административный округ, поселение Московский',
                        },
                    },
                    {
                        value: 'квартал № 77',
                        geoId: 213,
                        regionGraphId: '587795',
                        address: 'квартал № 77',
                        queryParams: {
                            rgid: '587795',
                            address:
                                // eslint-disable-next-line max-len
                                'Россия, Москва, Новомосковский административный округ, поселение Московский, квартал № 77',
                        },
                    },
                ],
            },
            point: {
                latitude: 55.6068,
                longitude: 37.41277,
                precision: 'EXACT',
            },
            metro: {
                metroGeoId: 218467,
                name: 'Филатов Луг',
                metroTransport: 'ON_FOOT',
                timeToMetro: 12,
                latitude: 55.60118,
                longitude: 37.407997,
                minTimeToMetro: 12,
                lineColors: ['e4402d'],
                rgbColor: 'e4402d',
            },
            station: {
                name: 'Новопеределкино',
                distanceKm: 3.886,
            },
            streetAddress: 'Киевское шоссе 23-й км, 5к1',
            metroList: [
                {
                    metroGeoId: 218467,
                    name: 'Филатов Луг',
                    metroTransport: 'ON_FOOT',
                    timeToMetro: 12,
                    latitude: 55.60118,
                    longitude: 37.407997,
                    minTimeToMetro: 12,
                    lineColors: ['e4402d'],
                    rgbColor: 'e4402d',
                },
                {
                    metroGeoId: 144826,
                    name: 'Саларьево',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 12,
                    latitude: 55.621815,
                    longitude: 37.424057,
                    minTimeToMetro: 12,
                    lineColors: ['e4402d'],
                    rgbColor: 'e4402d',
                },
                {
                    metroGeoId: 218466,
                    name: 'Прокшино',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 17,
                    latitude: 55.586456,
                    longitude: 37.43322,
                    minTimeToMetro: 17,
                    lineColors: ['e4402d'],
                    rgbColor: 'e4402d',
                },
            ],
            parks: [
                {
                    parkId: '121406591',
                    name: 'Ульяновский лесопарк',
                    timeOnFoot: 180,
                    distanceOnFoot: 250,
                    latitude: 55.61996,
                    longitude: 37.41289,
                    timeDistanceList: [
                        {
                            transport: 'ON_FOOT',
                            time: 3,
                            distance: 250,
                        },
                    ],
                },
            ],
            ponds: [
                {
                    pondId: '137693384',
                    name: 'река Сетунь',
                    timeOnFoot: 456,
                    distanceOnFoot: 633,
                    latitude: 55.615074,
                    longitude: 37.412415,
                    timeDistanceList: [
                        {
                            transport: 'ON_FOOT',
                            time: 7,
                            distance: 633,
                        },
                    ],
                },
            ],
            airports: [
                {
                    id: '878065',
                    name: 'Внуково',
                    timeOnCar: 1511,
                    distanceOnCar: 13636,
                    latitude: 55.604942,
                    longitude: 37.282578,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 25,
                            distance: 13636,
                        },
                    ],
                },
                {
                    id: '858742',
                    name: 'Домодедово',
                    timeOnCar: 2587,
                    distanceOnCar: 47452,
                    latitude: 55.41435,
                    longitude: 37.90048,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 43,
                            distance: 47452,
                        },
                    ],
                },
                {
                    id: '878042',
                    name: 'Шереметьево',
                    timeOnCar: 2861,
                    distanceOnCar: 48436,
                    latitude: 55.963852,
                    longitude: 37.4169,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 47,
                            distance: 48436,
                        },
                    ],
                },
                {
                    id: '878109',
                    name: 'Жуковский (Раменское)',
                    timeOnCar: 4518,
                    distanceOnCar: 67358,
                    latitude: 55.568665,
                    longitude: 38.143654,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 75,
                            distance: 67358,
                        },
                    ],
                },
            ],
            allHeatmaps: [
                {
                    name: 'carsharing',
                    rgbColor: 'ee4613',
                    description: 'очень низкая',
                    level: 1,
                    maxLevel: 8,
                    title: 'Доступность Яндекс.Драйва',
                },
            ],
            routeDistances: [
                {
                    geoPoint: {
                        latitude: 55.6387,
                        longitude: 37.459286,
                    },
                    distance: 5716,
                },
            ],
            subjectFederationName: 'Москва и МО',
            cityCenter: [
                {
                    transport: 'ON_CAR',
                    time: 2124,
                    distance: 27211,
                    latitude: 55.749058,
                    longitude: 37.612267,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 35,
                            distance: 27211,
                        },
                    ],
                },
            ],
        },
        queryId: '56d1e267c75af91073eb4fbef09633a0',
        author: {
            category: 'DEVELOPER',
        },
    },
];

export const getSiteCard = () => ({
    withOffers: true,
});

export const getProps = () => {
    const plans = getPlans();
    const offers = getOffers();

    return ({
        card: getSiteCard(),
        plans,
        offers,
        plansCount: plans.length,
        offersCount: offers.length,
        filtersCount: plans.length,
        turnoverOccurrence: [],
        loadPlans: noop,
        loadMorePlans: noop,
        loadOffers: noop,
        loadMoreOffers: noop,
        loadStats: noop,
    } as unknown) as ISitePlansV2Props;
};
