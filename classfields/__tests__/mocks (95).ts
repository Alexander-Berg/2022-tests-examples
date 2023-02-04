import noop from 'lodash/noop';

import createRootReducer from 'realty-core/view/react/libs/create-page-root-reducer';
import {
    ISitePlansOffersStore,
    sitePlansOffersReducer,
} from 'realty-core/view/react/modules/site-plans-offers/redux/reducer';
import { ApartmentType, ISiteCardMobile } from 'realty-core/types/siteCard';
import { SitePlansOffersSortTypes } from 'realty-core/types/cardPlansOffers';

import { ISitePlansOffersBaseProps } from '../types';

export const getSiteCard = (apartmentType: ApartmentType) =>
    ({
        buildingFeatures: {
            apartmentType,
        },
    } as ISiteCardMobile);

const getRooms = () => [
    {
        roomType: 'STUDIO',
        priceFrom: '5811500',
        priceTo: '7960050',
        areaFrom: 19.6,
        areaTo: 28.5,
        flatPlansCount: 13,
    },
    {
        roomType: '1',
        priceFrom: '7523880',
        priceTo: '10485240',
        areaFrom: 32,
        areaTo: 45.5,
        offersCount: 361,
    },
    {
        roomType: '2',
        priceFrom: '7764960',
        priceTo: '13492340',
        areaFrom: 33.6,
        areaTo: 72.7,
    },
    {
        roomType: '3',
        priceFrom: '12090100',
        priceTo: '16927000',
        areaFrom: 67.8,
        areaTo: 94.7,
        flatPlansCount: 24,
        offersCount: 132,
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

export const getInitialState = (sitePlansOffers: Partial<ISitePlansOffersStore>) => {
    return {
        user: {
            favorites: [],
            favoritesMap: {},
        },
        sitePlansOffers: {
            rooms: getRooms(),
            turnoverOccurrence: [],
            offers: [],
            offersCount: 0,
            mainFiltersCount: 0,
            filtersCount: 0,
            filterParams: {},
            sort: SitePlansOffersSortTypes.PRICE,
            ...sitePlansOffers,
        },
    };
};

export const defaultGate = {
    get(action: string) {
        const offers = getOffers();

        switch (action) {
            case 'site-plans.getPrimarySaleOffers':
                return Promise.resolve({
                    offers: offers,
                    pager: { page: 1, totalItems: offers.length },
                });

            case 'site-plans.getStats':
                return Promise.resolve({ totalPrimarySaleOffers: 2 });

            default:
                return null;
        }
    },
};

export const gateWithLoadMore = {
    get() {
        const offers = getOffers();

        return Promise.resolve({
            offers: offers,
            pager: { page: 1, totalItems: offers.length + 1 },
        });
    },
};

export const gateWithoutOffers = {
    get() {
        return Promise.resolve({
            offers: [],
            pager: { page: 1, totalItems: 0 },
        });
    },
};

export const gateLoadingOffers = {
    get() {
        return new Promise(noop);
    },
};

export const gateErrorOffers = {
    get() {
        return Promise.reject();
    },
};

export const getProps = (apartmentType: ApartmentType): ISitePlansOffersBaseProps => {
    return {
        card: getSiteCard(apartmentType),
    };
};

export const reducer = createRootReducer({
    sitePlansOffers: sitePlansOffersReducer,
});
