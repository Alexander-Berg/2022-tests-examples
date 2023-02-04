import { TrafficSourceInfo } from '@vertis/schema-registry/ts-types/realty/event/model';

import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { ISiteCardMobile, ApartmentType } from 'realty-core/types/siteCard';

import { INewbuildingPlansProps } from '../types';

const developerLogo = generateImageUrl({ width: 40, height: 40 });
const similarCardImage = generateImageUrl({ width: 392, height: 240 });

const getSimilar = () => ({
    sites: [
        {
            id: 401524,
            name: 'Headliner',
            fullName: 'квартал «Headliner»',
            locativeFullName: 'в квартале «Headliner»',
            location: {
                geoId: 213,
                rgid: 193368,
                settlementRgid: 587795,
                settlementGeoId: 213,
                address: 'Москва, Шмитовский проезд',
                subjectFederationId: 1,
                subjectFederationRgid: 741964,
                subjectFederationName: 'Москва и МО',
                point: {
                    latitude: 55.75294,
                    longitude: 37.524353,
                    precision: 'EXACT',
                },
                expectedMetroList: [],
                schools: [],
                parks: [
                    {
                        parkId: '1558825102',
                        name: 'парк Причальный',
                        timeOnFoot: 192,
                        distanceOnFoot: 266,
                        latitude: 55.75193,
                        longitude: 37.52375,
                        timeDistanceList: [
                            {
                                transport: 'ON_FOOT',
                                time: 3,
                                distance: 266,
                            },
                        ],
                    },
                ],
                ponds: [
                    {
                        pondId: '137667589',
                        name: 'река Москва',
                        timeOnFoot: 361,
                        distanceOnFoot: 502,
                        latitude: 55.749886,
                        longitude: 37.520546,
                        timeDistanceList: [
                            {
                                transport: 'ON_FOOT',
                                time: 6,
                                distance: 502,
                            },
                        ],
                    },
                ],
                airports: [
                    {
                        id: '878042',
                        name: 'Шереметьево',
                        timeOnCar: 2168,
                        distanceOnCar: 33532,
                        latitude: 55.963852,
                        longitude: 37.4169,
                        timeDistanceList: [
                            {
                                transport: 'ON_CAR',
                                time: 36,
                                distance: 33532,
                            },
                        ],
                    },
                    {
                        id: '878065',
                        name: 'Внуково',
                        timeOnCar: 3013,
                        distanceOnCar: 28827,
                        latitude: 55.604942,
                        longitude: 37.282578,
                        timeDistanceList: [
                            {
                                transport: 'ON_CAR',
                                time: 50,
                                distance: 28827,
                            },
                        ],
                    },
                    {
                        id: '858742',
                        name: 'Домодедово',
                        timeOnCar: 3139,
                        distanceOnCar: 49270,
                        latitude: 55.41435,
                        longitude: 37.90048,
                        timeDistanceList: [
                            {
                                transport: 'ON_CAR',
                                time: 52,
                                distance: 49270,
                            },
                        ],
                    },
                    {
                        id: '878109',
                        name: 'Жуковский (Раменское)',
                        timeOnCar: 3457,
                        distanceOnCar: 52549,
                        latitude: 55.568665,
                        longitude: 38.143654,
                        timeDistanceList: [
                            {
                                transport: 'ON_CAR',
                                time: 57,
                                distance: 52549,
                            },
                        ],
                    },
                ],
                cityCenter: [
                    {
                        transport: 'ON_CAR',
                        time: 1016,
                        distance: 9557,
                        latitude: 55.749058,
                        longitude: 37.612267,
                        timeDistanceList: [
                            {
                                transport: 'ON_CAR',
                                time: 16,
                                distance: 9557,
                            },
                        ],
                    },
                ],
                heatmaps: [
                    {
                        name: 'infrastructure',
                        rgbColor: 'fbae1e',
                        description: 'мало объектов',
                        level: 3,
                        maxLevel: 8,
                        title: 'Инфраструктура',
                    },
                    {
                        name: 'transport',
                        rgbColor: '9ada1b',
                        description: 'высокая доступность',
                        level: 6,
                        maxLevel: 9,
                        title: 'Транспорт',
                    },
                ],
                allHeatmaps: [
                    {
                        name: 'infrastructure',
                        rgbColor: 'fbae1e',
                        description: 'мало объектов',
                        level: 3,
                        maxLevel: 8,
                        title: 'Инфраструктура',
                    },
                    {
                        name: 'price-rent',
                        rgbColor: 'ee4613',
                        description: 'очень высокая',
                        level: 2,
                        maxLevel: 9,
                        title: 'Цена аренды',
                    },
                    {
                        name: 'price-sell',
                        rgbColor: 'fbae1e',
                        description: 'высокая',
                        level: 4,
                        maxLevel: 9,
                        title: 'Цена продажи',
                    },
                    {
                        name: 'profitability',
                        rgbColor: '128000',
                        description: 'очень высокая',
                        level: 9,
                        maxLevel: 9,
                        title: 'Прогноз окупаемости',
                    },
                    {
                        name: 'transport',
                        rgbColor: '9ada1b',
                        description: 'высокая доступность',
                        level: 6,
                        maxLevel: 9,
                        title: 'Транспорт',
                    },
                    {
                        name: 'carsharing',
                        rgbColor: '30ce12',
                        description: 'высокая',
                        level: 6,
                        maxLevel: 8,
                        title: 'Доступность Яндекс.Драйва',
                    },
                ],
                insideMKAD: true,
                routeDistances: [],
                metro: {
                    lineColors: ['ffa8af', 'ffe400', '6fc1ba'],
                    metroGeoId: 152948,
                    rgbColor: 'ffa8af',
                    metroTransport: 'ON_FOOT',
                    name: 'Шелепиха',
                    timeToMetro: 6,
                },
                metroList: [
                    {
                        lineColors: ['ffa8af', 'ffe400', '6fc1ba'],
                        metroGeoId: 152948,
                        rgbColor: 'ffa8af',
                        metroTransport: 'ON_FOOT',
                        name: 'Шелепиха',
                        timeToMetro: 6,
                    },
                    {
                        lineColors: ['ed9f2d'],
                        metroGeoId: 218566,
                        rgbColor: 'ed9f2d',
                        metroTransport: 'ON_FOOT',
                        name: 'Тестовская',
                        timeToMetro: 16,
                    },
                    {
                        lineColors: ['099dd4', 'ed9f2d'],
                        metroGeoId: 20459,
                        rgbColor: '099dd4',
                        metroTransport: 'ON_TRANSPORT',
                        name: 'Фили',
                        timeToMetro: 10,
                    },
                    {
                        lineColors: ['6fc1ba'],
                        metroGeoId: 115085,
                        rgbColor: '6fc1ba',
                        metroTransport: 'ON_TRANSPORT',
                        name: 'Деловой Центр',
                        timeToMetro: 14,
                    },
                ],
            },
            viewTypes: ['GENERAL', 'GENERAL', 'COURTYARD', 'GENERAL', 'COURTYARD', 'GENERAL', 'GENERAL', 'GENERAL'],
            images: [similarCardImage],
            appLargeImages: [similarCardImage],
            appMiddleSnippetImages: [similarCardImage],
            appLargeSnippetImages: [similarCardImage],
            minicardImages: [similarCardImage],
            siteSpecialProposals: [
                {
                    proposalType: 'MORTGAGE',
                    description: 'Ипотека 5.45%',
                    mainProposal: true,
                    specialProposalType: 'mortgage',
                    shortDescription: 'Ипотека 5.45%',
                },
                {
                    proposalType: 'SALE',
                    description: 'Трейд-ин',
                    mainProposal: false,
                    specialProposalType: 'sale',
                    shortDescription: 'Трейд-ин',
                },
                {
                    proposalType: 'INSTALLMENT',
                    description: 'Беспроцентная рассрочка на 6 мес.',
                    interestFee: true,
                    durationMonths: 6,
                    mainProposal: false,
                    specialProposalType: 'installment',
                    shortDescription: 'Беспроцентная рассрочка на 6 мес.',
                },
            ],
            buildingClass: 'BUSINESS',
            state: 'UNFINISHED',
            finishedApartments: true,
            price: {
                from: 13989320,
                to: 47100000,
                currency: 'RUR',
                minPricePerMeter: 265000,
                maxPricePerMeter: 503000,
                averagePricePerMeter: 334459,
                rooms: {
                    '1': {
                        soldout: false,
                        from: 13989320,
                        to: 21376050,
                        currency: 'RUR',
                        areas: {
                            from: '30',
                            to: '46',
                        },
                        hasOffers: true,
                        offersCount: 14,
                        priceRatioToMarket: 0,
                        status: 'ON_SALE',
                    },
                    '2': {
                        soldout: false,
                        from: 18195384,
                        to: 47100000,
                        currency: 'RUR',
                        areas: {
                            from: '45.9',
                            to: '100.3',
                        },
                        hasOffers: true,
                        offersCount: 91,
                        priceRatioToMarket: 0,
                        status: 'ON_SALE',
                    },
                    '3': {
                        soldout: false,
                        from: 22822670,
                        to: 46114880,
                        currency: 'RUR',
                        areas: {
                            from: '71.7',
                            to: '117.6',
                        },
                        hasOffers: true,
                        offersCount: 122,
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
                        currency: 'RUR',
                        hasOffers: false,
                        offersCount: 0,
                        priceRatioToMarket: 0,
                    },
                    PLUS_4: {
                        soldout: false,
                        from: 28794480,
                        to: 43147020,
                        currency: 'RUR',
                        areas: {
                            from: '99.5',
                            to: '120.9',
                        },
                        hasOffers: true,
                        offersCount: 49,
                        priceRatioToMarket: 0,
                        status: 'ON_SALE',
                    },
                },
                totalOffers: 276,
                priceRatioToMarket: 0,
            },
            flatStatus: 'ON_SALE',
            developers: [
                {
                    id: 279866,
                    name: 'ГК «КОРТРОС»',
                    legalNames: [],
                    url: 'http://www.kortros.ru',
                    logo: '',
                    objects: {
                        all: 16,
                        salesOpened: 6,
                        finished: 11,
                        unfinished: 5,
                        suspended: 0,
                    },
                    address: 'Москва, Шмитовский проезд, 39',
                    hasChat: false,
                },
                {
                    id: 401250,
                    name: 'Жилой квартал Сити',
                    legalName: 'ООО «Жилой квартал Сити»',
                    legalNames: ['ООО «Жилой квартал Сити»'],
                    url: '',
                    objects: {
                        all: 1,
                        salesOpened: 1,
                        finished: 0,
                        unfinished: 1,
                        suspended: 0,
                    },
                    address: 'Москва, Пресненская наб., 6, стр. 2,',
                    born: '2016-10-05T21:00:00Z',
                    hasChat: false,
                },
            ],
            salesDepartment: {
                id: 1595957,
                name: 'ГК «КОРТРОС»',
                isRedirectPhones: true,
                weekTimetable: [
                    {
                        dayFrom: 1,
                        dayTo: 7,
                        timePattern: [
                            {
                                open: '09:00',
                                close: '21:00',
                            },
                        ],
                    },
                ],
                logo: '//avatars.mdst.yandex.net/get-realty/2957/company.2194043.1501383694421375657/builder_logo_info',
                phonesWithTag: [
                    {
                        tag: '',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformIosOffer#utmSource=YandexDirect',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformIosOffer#utmSource=RtbHouse',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformTouchOffer#utmSource=CriteoAdv',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'platformIosOffer',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformTouchCampaign#utmSource=RtbHouse',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformAndroidCampaign#utmSource=CriteoAdv',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformAndroidOffer#utmSource=YandexDirect',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'platformAndroidOffer',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformTouchCampaign#utmSource=YandexDirect',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformIosCampaign#utmSource=CriteoAdv',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'platformDesktopCampaign',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'mapsMobile',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformIosCampaign#utmSource=Mytarget',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformAndroidOffer#utmSource=RtbHouse',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'platformAndroidCampaign',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformAndroidOffer#utmSource=GoogleAdwords',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformAndroidOffer#utmSource=CriteoAdv',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformIosOffer#utmSource=CriteoAdv',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'platformTouchCampaign',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformTouchCampaign#utmSource=CriteoAdv',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformAndroidCampaign#utmSource=RtbHouse',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformDesktopOffer#utmSource=YandexDirect',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformDesktopCampaign#utmSource=RtbHouse',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformDesktopOffer#utmSource=CriteoAdv',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformTouchOffer#utmSource=RtbHouse',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformIosOffer#utmSource=GoogleAdwords',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformTouchCampaign#utmSource=GoogleAdwords',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'platformTouchOffer',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformIosCampaign#utmSource=RtbHouse',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformTouchOffer#utmSource=GoogleAdwords',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'platformIosCampaign',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformAndroidCampaign#utmSource=Mytarget',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformDesktopOffer#utmSource=GoogleAdwords',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformDesktopOffer#utmSource=RtbHouse',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformDesktopCampaign#utmSource=GoogleAdwords',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformTouchOffer#utmSource=YandexDirect',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformIosCampaign#utmSource=YandexDirect',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformDesktopCampaign#utmSource=CriteoAdv',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformIosCampaign#utmSource=GoogleAdwords',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformDesktopCampaign#utmSource=YandexDirect',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'platformDesktopOffer',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformAndroidCampaign#utmSource=GoogleAdwords',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformIosOffer#utmSource=Mytarget',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformAndroidCampaign#utmSource=YandexDirect',
                        phone: '+74951063507',
                    },
                    {
                        tag: 'PlatformAndroidOffer#utmSource=Mytarget',
                        phone: '+74951063507',
                    },
                ],
                timetableZoneMinutes: 180,
                statParams: '',
            },
            withBilling: true,
            awards: {},
            limitedCard: false,
        },
        {
            id: 312396,
            name: 'NEVA TOWERS',
            fullName: 'МФК «NEVA TOWERS»',
            locativeFullName: 'в МФК «NEVA TOWERS»',
            location: {
                geoId: 120538,
                rgid: 197177,
                settlementRgid: 587795,
                settlementGeoId: 213,
                address: 'Москва, 1-й Красногвардейский проезд, 22, стр. 1, стр. 2',
                subjectFederationId: 1,
                subjectFederationRgid: 741964,
                subjectFederationName: 'Москва и МО',
                point: {
                    latitude: 55.751797,
                    longitude: 37.533794,
                    precision: 'EXACT',
                },
                expectedMetroList: [],
                schools: [],
                parks: [
                    {
                        parkId: '121375314',
                        name: '',
                        timeOnFoot: 2262,
                        distanceOnFoot: 3142,
                        latitude: 55.748737,
                        longitude: 37.530396,
                        timeDistanceList: [
                            {
                                transport: 'ON_FOOT',
                                time: 37,
                                distance: 3142,
                            },
                        ],
                    },
                ],
                ponds: [],
                airports: [
                    {
                        id: '878042',
                        name: 'Шереметьево',
                        timeOnCar: 2010,
                        distanceOnCar: 32740,
                        latitude: 55.963852,
                        longitude: 37.4169,
                        timeDistanceList: [
                            {
                                transport: 'ON_CAR',
                                time: 33,
                                distance: 32740,
                            },
                        ],
                    },
                    {
                        id: '878065',
                        name: 'Внуково',
                        timeOnCar: 2853,
                        distanceOnCar: 27815,
                        latitude: 55.604942,
                        longitude: 37.282578,
                        timeDistanceList: [
                            {
                                transport: 'ON_CAR',
                                time: 47,
                                distance: 27815,
                            },
                        ],
                    },
                    {
                        id: '858742',
                        name: 'Домодедово',
                        timeOnCar: 2980,
                        distanceOnCar: 48258,
                        latitude: 55.41435,
                        longitude: 37.90048,
                        timeDistanceList: [
                            {
                                transport: 'ON_CAR',
                                time: 49,
                                distance: 48258,
                            },
                        ],
                    },
                    {
                        id: '878109',
                        name: 'Жуковский (Раменское)',
                        timeOnCar: 3291,
                        distanceOnCar: 51536,
                        latitude: 55.568665,
                        longitude: 38.143654,
                        timeDistanceList: [
                            {
                                transport: 'ON_CAR',
                                time: 54,
                                distance: 51536,
                            },
                        ],
                    },
                ],
                cityCenter: [
                    {
                        transport: 'ON_CAR',
                        time: 707,
                        distance: 7777,
                        latitude: 55.749058,
                        longitude: 37.612267,
                        timeDistanceList: [
                            {
                                transport: 'ON_CAR',
                                time: 11,
                                distance: 7777,
                            },
                        ],
                    },
                ],
                heatmaps: [
                    {
                        name: 'infrastructure',
                        rgbColor: '30ce12',
                        description: 'хорошо развитая',
                        level: 6,
                        maxLevel: 8,
                        title: 'Инфраструктура',
                    },
                    {
                        name: 'transport',
                        rgbColor: '9ada1b',
                        description: 'высокая доступность',
                        level: 6,
                        maxLevel: 9,
                        title: 'Транспорт',
                    },
                ],
                allHeatmaps: [
                    {
                        name: 'infrastructure',
                        rgbColor: '30ce12',
                        description: 'хорошо развитая',
                        level: 6,
                        maxLevel: 8,
                        title: 'Инфраструктура',
                    },
                    {
                        name: 'price-rent',
                        rgbColor: 'f87c19',
                        description: 'высокая',
                        level: 3,
                        maxLevel: 9,
                        title: 'Цена аренды',
                    },
                    {
                        name: 'price-sell',
                        rgbColor: 'f87c19',
                        description: 'высокая',
                        level: 3,
                        maxLevel: 9,
                        title: 'Цена продажи',
                    },
                    {
                        name: 'profitability',
                        rgbColor: '30ce12',
                        description: 'высокая',
                        level: 7,
                        maxLevel: 9,
                        title: 'Прогноз окупаемости',
                    },
                    {
                        name: 'transport',
                        rgbColor: '9ada1b',
                        description: 'высокая доступность',
                        level: 6,
                        maxLevel: 9,
                        title: 'Транспорт',
                    },
                    {
                        name: 'carsharing',
                        rgbColor: '30ce12',
                        description: 'высокая',
                        level: 6,
                        maxLevel: 8,
                        title: 'Доступность Яндекс.Драйва',
                    },
                ],
                insideMKAD: true,
                routeDistances: [],
                metro: {
                    lineColors: ['ed9f2d'],
                    metroGeoId: 218566,
                    rgbColor: 'ed9f2d',
                    metroTransport: 'ON_FOOT',
                    name: 'Тестовская',
                    timeToMetro: 6,
                },
                metroList: [
                    {
                        lineColors: ['ed9f2d'],
                        metroGeoId: 218566,
                        rgbColor: 'ed9f2d',
                        metroTransport: 'ON_FOOT',
                        name: 'Тестовская',
                        timeToMetro: 6,
                    },
                    {
                        lineColors: ['6fc1ba'],
                        metroGeoId: 115085,
                        rgbColor: '6fc1ba',
                        metroTransport: 'ON_FOOT',
                        name: 'Деловой Центр',
                        timeToMetro: 7,
                    },
                    {
                        lineColors: ['099dd4'],
                        metroGeoId: 98560,
                        rgbColor: '099dd4',
                        metroTransport: 'ON_FOOT',
                        name: 'Международная',
                        timeToMetro: 9,
                    },
                    {
                        lineColors: ['ffa8af'],
                        metroGeoId: 152947,
                        rgbColor: 'ffa8af',
                        metroTransport: 'ON_FOOT',
                        name: 'Деловой Центр',
                        timeToMetro: 12,
                    },
                    {
                        lineColors: ['099dd4'],
                        metroGeoId: 98559,
                        rgbColor: '099dd4',
                        metroTransport: 'ON_FOOT',
                        name: 'Выставочная',
                        timeToMetro: 14,
                    },
                ],
            },
            viewTypes: ['GENERAL', 'GENERAL', 'GENERAL', 'GENERAL', 'GENERAL'],
            images: [similarCardImage],
            appLargeImages: [similarCardImage],
            appMiddleSnippetImages: [similarCardImage],
            appLargeSnippetImages: [similarCardImage],
            minicardImages: [similarCardImage],
            siteSpecialProposals: [
                {
                    proposalType: 'DISCOUNT',
                    description: 'Скидка 7%',
                    mainProposal: true,
                    specialProposalType: 'discount',
                    shortDescription: 'Скидка 7%',
                },
                {
                    proposalType: 'GIFT',
                    description: 'Отделка в подарок',
                    mainProposal: false,
                    specialProposalType: 'gift',
                    shortDescription: 'Отделка в подарок',
                },
                {
                    proposalType: 'INSTALLMENT',
                    description: 'Беспроцентная рассрочка на 2 года',
                    interestFee: true,
                    durationMonths: 24,
                    mainProposal: false,
                    specialProposalType: 'installment',
                    shortDescription: 'Беспроцентная рассрочка на 2 года',
                },
            ],
            buildingClass: 'ELITE',
            state: 'HAND_OVER',
            finishedApartments: true,
            price: {
                currency: 'RUR',
                rooms: {
                    '1': {
                        soldout: false,
                        currency: 'RUR',
                        areas: {
                            from: '53',
                            to: '85',
                        },
                        hasOffers: false,
                        offersCount: 0,
                        priceRatioToMarket: 0,
                        status: 'ON_SALE',
                    },
                    '2': {
                        soldout: false,
                        currency: 'RUR',
                        areas: {
                            from: '86',
                            to: '140',
                        },
                        hasOffers: false,
                        offersCount: 0,
                        priceRatioToMarket: 0,
                        status: 'ON_SALE',
                    },
                    '3': {
                        soldout: false,
                        currency: 'RUR',
                        areas: {
                            from: '125',
                            to: '226',
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
                        currency: 'RUR',
                        areas: {
                            from: '45',
                            to: '51',
                        },
                        hasOffers: false,
                        offersCount: 0,
                        priceRatioToMarket: 0,
                        status: 'ON_SALE',
                    },
                    PLUS_4: {
                        soldout: false,
                        currency: 'RUR',
                        hasOffers: false,
                        offersCount: 0,
                        priceRatioToMarket: 0,
                    },
                },
                totalOffers: 0,
                priceRatioToMarket: 0,
            },
            flatStatus: 'ON_SALE',
            developers: [
                {
                    id: 312340,
                    name: 'Renaissance Development',
                    legalName: 'ООО «СТ Тауэрс»',
                    legalNames: ['ООО «СТ Тауэрс»'],
                    url: 'http://www.rendvlp.com',
                    logo: '',
                    objects: {
                        all: 1,
                        salesOpened: 1,
                        finished: 1,
                        unfinished: 0,
                        suspended: 0,
                    },
                    born: '2001-12-30T21:00:00Z',
                    hasChat: false,
                },
            ],
            salesDepartment: {
                id: 709333,
                name: 'АН Метриум',
                isRedirectPhones: true,
                weekTimetable: [
                    {
                        dayFrom: 1,
                        dayTo: 5,
                        timePattern: [
                            {
                                open: '10:00',
                                close: '20:00',
                            },
                        ],
                    },
                    {
                        dayFrom: 6,
                        dayTo: 7,
                        timePattern: [
                            {
                                open: '10:00',
                                close: '17:00',
                            },
                        ],
                    },
                ],
                logo: '//avatars.mdst.yandex.net/get-realty/3019/company.1745036.4228806887336934943/builder_logo_info',
                phonesWithTag: [
                    {
                        tag: '',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformIosOffer#utmSource=YandexDirect',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformIosOffer#utmSource=RtbHouse',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformTouchOffer#utmSource=CriteoAdv',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'platformIosOffer',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformTouchCampaign#utmSource=RtbHouse',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformAndroidCampaign#utmSource=CriteoAdv',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformAndroidOffer#utmSource=YandexDirect',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'platformAndroidOffer',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformTouchCampaign#utmSource=YandexDirect',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformIosCampaign#utmSource=CriteoAdv',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'platformDesktopCampaign',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'mapsMobile',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformIosCampaign#utmSource=Mytarget',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformAndroidOffer#utmSource=RtbHouse',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'platformAndroidCampaign',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformAndroidOffer#utmSource=GoogleAdwords',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformAndroidOffer#utmSource=CriteoAdv',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformIosOffer#utmSource=CriteoAdv',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'platformTouchCampaign',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformTouchCampaign#utmSource=CriteoAdv',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformAndroidCampaign#utmSource=RtbHouse',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformDesktopOffer#utmSource=YandexDirect',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformDesktopCampaign#utmSource=RtbHouse',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformDesktopOffer#utmSource=CriteoAdv',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformTouchOffer#utmSource=RtbHouse',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformIosOffer#utmSource=GoogleAdwords',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformTouchCampaign#utmSource=GoogleAdwords',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'platformTouchOffer',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformIosCampaign#utmSource=RtbHouse',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformTouchOffer#utmSource=GoogleAdwords',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'platformIosCampaign',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformAndroidCampaign#utmSource=Mytarget',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformDesktopOffer#utmSource=GoogleAdwords',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformDesktopOffer#utmSource=RtbHouse',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformDesktopCampaign#utmSource=GoogleAdwords',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformTouchOffer#utmSource=YandexDirect',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformIosCampaign#utmSource=YandexDirect',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformDesktopCampaign#utmSource=CriteoAdv',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformIosCampaign#utmSource=GoogleAdwords',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformDesktopCampaign#utmSource=YandexDirect',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'platformDesktopOffer',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformAndroidCampaign#utmSource=GoogleAdwords',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformIosOffer#utmSource=Mytarget',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformAndroidCampaign#utmSource=YandexDirect',
                        phone: '+74951519476',
                    },
                    {
                        tag: 'PlatformAndroidOffer#utmSource=Mytarget',
                        phone: '+74951519476',
                    },
                ],
                timetableZoneMinutes: 180,
                statParams: '',
            },
            withBilling: true,
            awards: {},
            limitedCard: false,
        },
    ],
    'developer-sites': [
        {
            id: 171513,
            name: 'Барвиха Хиллс',
            fullName: 'ЖК «Барвиха Хиллс»',
            locativeFullName: 'в ЖК «Барвиха Хиллс»',
            location: {
                geoId: 21652,
                rgid: 194760,
                settlementRgid: 60038,
                settlementGeoId: 21652,
                address: 'Московская обл., Одинцовский округ, пос. Барвиха, КП «Барвиха Хиллс», 1-29',
                distanceFromRingRoad: 8979,
                subjectFederationId: 1,
                subjectFederationRgid: 741964,
                subjectFederationName: 'Москва и МО',
                point: {
                    latitude: 55.726944,
                    longitude: 37.285942,
                    precision: 'EXACT',
                },
                expectedMetroList: [],
                schools: [],
                parks: [],
                ponds: [
                    {
                        pondId: '164149639',
                        name: 'Первый Шульгинский пруд',
                        timeOnFoot: 310,
                        distanceOnFoot: 430,
                        latitude: 55.725067,
                        longitude: 37.288162,
                        timeDistanceList: [
                            {
                                transport: 'ON_FOOT',
                                time: 5,
                                distance: 430,
                            },
                        ],
                    },
                    {
                        pondId: '137689713',
                        name: 'река Саминка',
                        timeOnFoot: 360,
                        distanceOnFoot: 558,
                        latitude: 55.729656,
                        longitude: 37.27845,
                        timeDistanceList: [
                            {
                                transport: 'ON_FOOT',
                                time: 6,
                                distance: 558,
                            },
                        ],
                    },
                    {
                        pondId: '164149958',
                        name: 'Второй Шульгинский пруд',
                        timeOnFoot: 410,
                        distanceOnFoot: 570,
                        latitude: 55.724293,
                        longitude: 37.29105,
                        timeDistanceList: [
                            {
                                transport: 'ON_FOOT',
                                time: 6,
                                distance: 570,
                            },
                        ],
                    },
                ],
                airports: [
                    {
                        id: '878065',
                        name: 'Внуково',
                        timeOnCar: 2283,
                        distanceOnCar: 19146,
                        latitude: 55.604942,
                        longitude: 37.282578,
                        timeDistanceList: [
                            {
                                transport: 'ON_CAR',
                                time: 38,
                                distance: 19146,
                            },
                        ],
                    },
                    {
                        id: '878042',
                        name: 'Шереметьево',
                        timeOnCar: 2915,
                        distanceOnCar: 46154,
                        latitude: 55.963852,
                        longitude: 37.4169,
                        timeDistanceList: [
                            {
                                transport: 'ON_CAR',
                                time: 48,
                                distance: 46154,
                            },
                        ],
                    },
                    {
                        id: '858742',
                        name: 'Домодедово',
                        timeOnCar: 3704,
                        distanceOnCar: 67865,
                        latitude: 55.41435,
                        longitude: 37.90048,
                        timeDistanceList: [
                            {
                                transport: 'ON_CAR',
                                time: 61,
                                distance: 67865,
                            },
                        ],
                    },
                    {
                        id: '878109',
                        name: 'Жуковский (Раменское)',
                        timeOnCar: 4987,
                        distanceOnCar: 72518,
                        latitude: 55.568665,
                        longitude: 38.143654,
                        timeDistanceList: [
                            {
                                transport: 'ON_CAR',
                                time: 83,
                                distance: 72518,
                            },
                        ],
                    },
                ],
                cityCenter: [
                    {
                        transport: 'ON_CAR',
                        time: 2226,
                        distance: 29219,
                        latitude: 55.749058,
                        longitude: 37.612267,
                        timeDistanceList: [
                            {
                                transport: 'ON_CAR',
                                time: 37,
                                distance: 29219,
                            },
                        ],
                    },
                ],
                heatmaps: [
                    {
                        name: 'infrastructure',
                        rgbColor: 'ee4613',
                        description: 'минимальная',
                        level: 1,
                        maxLevel: 8,
                        title: 'Инфраструктура',
                    },
                    {
                        name: 'transport',
                        rgbColor: 'ee4613',
                        description: 'низкая доступность',
                        level: 2,
                        maxLevel: 9,
                        title: 'Транспорт',
                    },
                ],
                allHeatmaps: [
                    {
                        name: 'infrastructure',
                        rgbColor: 'ee4613',
                        description: 'минимальная',
                        level: 1,
                        maxLevel: 8,
                        title: 'Инфраструктура',
                    },
                    {
                        name: 'transport',
                        rgbColor: 'ee4613',
                        description: 'низкая доступность',
                        level: 2,
                        maxLevel: 9,
                        title: 'Транспорт',
                    },
                ],
                insideMKAD: false,
                routeDistances: [
                    {
                        geoPoint: {
                            latitude: 55.764954,
                            longitude: 37.368774,
                            defined: true,
                        },
                        distance: 8979,
                        highway: {
                            id: '34',
                            name: 'Рублёво-Успенское шоссе',
                        },
                    },
                ],
                metro: {
                    lineColors: ['ed9f2d'],
                    metroGeoId: 218584,
                    rgbColor: 'ed9f2d',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'Одинцово',
                    timeToMetro: 36,
                },
                metroList: [
                    {
                        lineColors: ['ed9f2d'],
                        metroGeoId: 218584,
                        rgbColor: 'ed9f2d',
                        metroTransport: 'ON_TRANSPORT',
                        name: 'Одинцово',
                        timeToMetro: 36,
                    },
                    {
                        lineColors: ['ed9f2d'],
                        metroGeoId: 218562,
                        rgbColor: 'ed9f2d',
                        metroTransport: 'ON_TRANSPORT',
                        name: 'Баковка',
                        timeToMetro: 37,
                    },
                    {
                        lineColors: ['0042a5'],
                        metroGeoId: 20453,
                        rgbColor: '0042a5',
                        metroTransport: 'ON_TRANSPORT',
                        name: 'Крылатское',
                        timeToMetro: 39,
                    },
                    {
                        lineColors: ['ed9f2d'],
                        metroGeoId: 218545,
                        rgbColor: 'ed9f2d',
                        metroTransport: 'ON_TRANSPORT',
                        name: 'Сколково',
                        timeToMetro: 40,
                    },
                ],
            },
            viewTypes: ['GENERAL', 'GENERAL', 'GENERAL', 'GENERAL', 'GENERAL'],
            images: [similarCardImage],
            appLargeImages: [similarCardImage],
            appMiddleSnippetImages: [similarCardImage],
            appLargeSnippetImages: [similarCardImage],
            minicardImages: [similarCardImage],
            buildingClass: 'ELITE',
            state: 'HAND_OVER',
            finishedApartments: true,
            price: {
                currency: 'RUR',
                rooms: {
                    '1': {
                        soldout: false,
                        currency: 'RUR',
                        hasOffers: false,
                        offersCount: 0,
                        priceRatioToMarket: 0,
                    },
                    '2': {
                        soldout: false,
                        currency: 'RUR',
                        hasOffers: false,
                        offersCount: 0,
                        priceRatioToMarket: 0,
                    },
                    '3': {
                        soldout: false,
                        currency: 'RUR',
                        hasOffers: false,
                        offersCount: 0,
                        priceRatioToMarket: 0,
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
                        currency: 'RUR',
                        hasOffers: false,
                        offersCount: 0,
                        priceRatioToMarket: 0,
                    },
                    PLUS_4: {
                        soldout: false,
                        currency: 'RUR',
                        hasOffers: false,
                        offersCount: 0,
                        priceRatioToMarket: 0,
                    },
                },
                totalOffers: 0,
                priceRatioToMarket: 0,
            },
            flatStatus: 'SOLD',
            developers: [
                {
                    id: 268706,
                    name: 'Capital Group',
                    legalNames: [],
                    url: 'http://www.capitalgroup.ru/',
                    logo: '',
                    objects: {
                        all: 33,
                        salesOpened: 17,
                        finished: 24,
                        unfinished: 9,
                        suspended: 0,
                    },
                    address: 'Москва, Пресненская набережная, 8с1',
                    born: '1992-12-31T21:00:00Z',
                    hasChat: false,
                    encryptedPhones: [
                        {
                            phoneWithMask: '+7 495 771 ×× ××',
                            phoneHash: 'KzcF0OHTUJ3NLzEN3NPzcR3',
                            tag: 0,
                        },
                    ],
                },
            ],
            salesDepartment: {
                id: 295494,
                name: 'Capital Group',
                isRedirectPhones: true,
                weekTimetable: [
                    {
                        dayFrom: 1,
                        dayTo: 7,
                        timePattern: [
                            {
                                open: '09:00',
                                close: '21:00',
                            },
                        ],
                    },
                ],
                logo: '//avatars.mdst.yandex.net/get-realty/3274/company.295494.267970144362838907/builder_logo_info',
                phonesWithTag: [
                    {
                        tag: '',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformIosOffer#utmSource=YandexDirect',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformIosOffer#utmSource=RtbHouse',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformTouchOffer#utmSource=CriteoAdv',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'platformIosOffer',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformTouchCampaign#utmSource=RtbHouse',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformAndroidCampaign#utmSource=CriteoAdv',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformAndroidOffer#utmSource=YandexDirect',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'platformAndroidOffer',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformTouchCampaign#utmSource=YandexDirect',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformIosCampaign#utmSource=CriteoAdv',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'platformDesktopCampaign',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'mapsMobile',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformIosCampaign#utmSource=Mytarget',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformAndroidOffer#utmSource=RtbHouse',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'platformAndroidCampaign',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformAndroidOffer#utmSource=GoogleAdwords',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformAndroidOffer#utmSource=CriteoAdv',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformIosOffer#utmSource=CriteoAdv',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'platformTouchCampaign',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformTouchCampaign#utmSource=CriteoAdv',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformAndroidCampaign#utmSource=RtbHouse',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformDesktopOffer#utmSource=YandexDirect',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformDesktopCampaign#utmSource=RtbHouse',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformDesktopOffer#utmSource=CriteoAdv',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformTouchOffer#utmSource=RtbHouse',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformIosOffer#utmSource=GoogleAdwords',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformTouchCampaign#utmSource=GoogleAdwords',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'platformTouchOffer',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformIosCampaign#utmSource=RtbHouse',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformTouchOffer#utmSource=GoogleAdwords',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'platformIosCampaign',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformAndroidCampaign#utmSource=Mytarget',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformDesktopOffer#utmSource=GoogleAdwords',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformDesktopOffer#utmSource=RtbHouse',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformDesktopCampaign#utmSource=GoogleAdwords',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformTouchOffer#utmSource=YandexDirect',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformIosCampaign#utmSource=YandexDirect',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformDesktopCampaign#utmSource=CriteoAdv',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformIosCampaign#utmSource=GoogleAdwords',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformDesktopCampaign#utmSource=YandexDirect',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'platformDesktopOffer',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformAndroidCampaign#utmSource=GoogleAdwords',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformIosOffer#utmSource=Mytarget',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformAndroidCampaign#utmSource=YandexDirect',
                        phone: '+74951234567',
                    },
                    {
                        tag: 'PlatformAndroidOffer#utmSource=Mytarget',
                        phone: '+74951234567',
                    },
                ],
                timetableZoneMinutes: 180,
                statParams: '',
                encryptedPhones: [
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                    },
                ],
                encryptedPhonesWithTag: [
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: '',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformIosOffer#utmSource=YandexDirect',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformIosOffer#utmSource=RtbHouse',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformTouchOffer#utmSource=CriteoAdv',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'platformIosOffer',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformTouchCampaign#utmSource=RtbHouse',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformAndroidCampaign#utmSource=CriteoAdv',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformAndroidOffer#utmSource=YandexDirect',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'platformAndroidOffer',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformTouchCampaign#utmSource=YandexDirect',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformIosCampaign#utmSource=CriteoAdv',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'platformDesktopCampaign',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'mapsMobile',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformIosCampaign#utmSource=Mytarget',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformAndroidOffer#utmSource=RtbHouse',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'platformAndroidCampaign',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformAndroidOffer#utmSource=GoogleAdwords',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformAndroidOffer#utmSource=CriteoAdv',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformIosOffer#utmSource=CriteoAdv',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'platformTouchCampaign',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformTouchCampaign#utmSource=CriteoAdv',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformAndroidCampaign#utmSource=RtbHouse',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformDesktopOffer#utmSource=YandexDirect',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformDesktopCampaign#utmSource=RtbHouse',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformDesktopOffer#utmSource=CriteoAdv',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformTouchOffer#utmSource=RtbHouse',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformIosOffer#utmSource=GoogleAdwords',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformTouchCampaign#utmSource=GoogleAdwords',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'platformTouchOffer',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformIosCampaign#utmSource=RtbHouse',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformTouchOffer#utmSource=GoogleAdwords',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'platformIosCampaign',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformAndroidCampaign#utmSource=Mytarget',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformDesktopOffer#utmSource=GoogleAdwords',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformDesktopOffer#utmSource=RtbHouse',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformDesktopCampaign#utmSource=GoogleAdwords',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformTouchOffer#utmSource=YandexDirect',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformIosCampaign#utmSource=YandexDirect',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformDesktopCampaign#utmSource=CriteoAdv',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformIosCampaign#utmSource=GoogleAdwords',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformDesktopCampaign#utmSource=YandexDirect',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'platformDesktopOffer',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformAndroidCampaign#utmSource=GoogleAdwords',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformIosOffer#utmSource=Mytarget',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformAndroidCampaign#utmSource=YandexDirect',
                    },
                    {
                        phoneWithMask: '+7 495 123 ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
                        tag: 'PlatformAndroidOffer#utmSource=Mytarget',
                    },
                ],
                encryptedDump: '',
            },
            phone: {
                phoneWithMask: '+7 495 123 ×× ××',
                phoneHash: 'KzcF0OHTUJxMLjMN0NPTYR3',
            },
            backCallTrafficInfo: {} as TrafficSourceInfo,
            withBilling: true,
            awards: {},
            limitedCard: false,
        },
        {
            id: 59024,
            name: 'ОКО',
            fullName: 'МФК «ОКО»',
            locativeFullName: 'в МФК «ОКО»',
            location: {
                geoId: 213,
                rgid: 197177,
                settlementRgid: 587795,
                settlementGeoId: 213,
                address: 'Москва, Краснопресненская наб.',
                subjectFederationId: 1,
                subjectFederationRgid: 741964,
                subjectFederationName: 'Москва и МО',
                point: {
                    latitude: 55.750504,
                    longitude: 37.534782,
                    precision: 'EXACT',
                },
                expectedMetroList: [],
                schools: [],
                parks: [],
                ponds: [
                    {
                        pondId: '137667589',
                        name: 'река Москва',
                        timeOnFoot: 420,
                        distanceOnFoot: 623,
                        latitude: 55.74477,
                        longitude: 37.53527,
                        timeDistanceList: [
                            {
                                transport: 'ON_FOOT',
                                time: 7,
                                distance: 623,
                            },
                        ],
                    },
                ],
                airports: [
                    {
                        id: '878042',
                        name: 'Шереметьево',
                        timeOnCar: 1697,
                        distanceOnCar: 29648,
                        latitude: 55.963852,
                        longitude: 37.4169,
                        timeDistanceList: [
                            {
                                transport: 'ON_CAR',
                                time: 28,
                                distance: 29648,
                            },
                        ],
                    },
                    {
                        id: '878065',
                        name: 'Внуково',
                        timeOnCar: 2528,
                        distanceOnCar: 30157,
                        latitude: 55.604942,
                        longitude: 37.282578,
                        timeDistanceList: [
                            {
                                transport: 'ON_CAR',
                                time: 42,
                                distance: 30157,
                            },
                        ],
                    },
                    {
                        id: '858742',
                        name: 'Домодедово',
                        timeOnCar: 3008,
                        distanceOnCar: 48854,
                        latitude: 55.41435,
                        longitude: 37.90048,
                        timeDistanceList: [
                            {
                                transport: 'ON_CAR',
                                time: 50,
                                distance: 48854,
                            },
                        ],
                    },
                    {
                        id: '878109',
                        name: 'Жуковский (Раменское)',
                        timeOnCar: 3303,
                        distanceOnCar: 51886,
                        latitude: 55.568665,
                        longitude: 38.143654,
                        timeDistanceList: [
                            {
                                transport: 'ON_CAR',
                                time: 55,
                                distance: 51886,
                            },
                        ],
                    },
                ],
                cityCenter: [
                    {
                        transport: 'ON_CAR',
                        time: 735,
                        distance: 7281,
                        latitude: 55.749058,
                        longitude: 37.612267,
                        timeDistanceList: [
                            {
                                transport: 'ON_CAR',
                                time: 12,
                                distance: 7281,
                            },
                        ],
                    },
                ],
                heatmaps: [
                    {
                        name: 'infrastructure',
                        rgbColor: '30ce12',
                        description: 'хорошо развитая',
                        level: 6,
                        maxLevel: 8,
                        title: 'Инфраструктура',
                    },
                    {
                        name: 'transport',
                        rgbColor: '9ada1b',
                        description: 'высокая доступность',
                        level: 6,
                        maxLevel: 9,
                        title: 'Транспорт',
                    },
                ],
                allHeatmaps: [
                    {
                        name: 'infrastructure',
                        rgbColor: '30ce12',
                        description: 'хорошо развитая',
                        level: 6,
                        maxLevel: 8,
                        title: 'Инфраструктура',
                    },
                    {
                        name: 'price-rent',
                        rgbColor: 'fbae1e',
                        description: 'высокая',
                        level: 4,
                        maxLevel: 9,
                        title: 'Цена аренды',
                    },
                    {
                        name: 'price-sell',
                        rgbColor: 'f87c19',
                        description: 'высокая',
                        level: 3,
                        maxLevel: 9,
                        title: 'Цена продажи',
                    },
                    {
                        name: 'profitability',
                        rgbColor: '9ada1b',
                        description: 'выше средней',
                        level: 6,
                        maxLevel: 9,
                        title: 'Прогноз окупаемости',
                    },
                    {
                        name: 'transport',
                        rgbColor: '9ada1b',
                        description: 'высокая доступность',
                        level: 6,
                        maxLevel: 9,
                        title: 'Транспорт',
                    },
                    {
                        name: 'carsharing',
                        rgbColor: '9ada1b',
                        description: 'высокая',
                        level: 5,
                        maxLevel: 8,
                        title: 'Доступность Яндекс.Драйва',
                    },
                ],
                insideMKAD: true,
                routeDistances: [],
                metro: {
                    lineColors: ['6fc1ba'],
                    metroGeoId: 115085,
                    rgbColor: '6fc1ba',
                    metroTransport: 'ON_FOOT',
                    name: 'Деловой Центр',
                    timeToMetro: 4,
                },
                metroList: [
                    {
                        lineColors: ['6fc1ba'],
                        metroGeoId: 115085,
                        rgbColor: '6fc1ba',
                        metroTransport: 'ON_FOOT',
                        name: 'Деловой Центр',
                        timeToMetro: 4,
                    },
                    {
                        lineColors: ['099dd4'],
                        metroGeoId: 98560,
                        rgbColor: '099dd4',
                        metroTransport: 'ON_FOOT',
                        name: 'Международная',
                        timeToMetro: 5,
                    },
                    {
                        lineColors: ['ed9f2d'],
                        metroGeoId: 218566,
                        rgbColor: 'ed9f2d',
                        metroTransport: 'ON_FOOT',
                        name: 'Тестовская',
                        timeToMetro: 8,
                    },
                    {
                        lineColors: ['ffa8af'],
                        metroGeoId: 152947,
                        rgbColor: 'ffa8af',
                        metroTransport: 'ON_FOOT',
                        name: 'Деловой Центр',
                        timeToMetro: 8,
                    },
                    {
                        lineColors: ['099dd4'],
                        metroGeoId: 98559,
                        rgbColor: '099dd4',
                        metroTransport: 'ON_FOOT',
                        name: 'Выставочная',
                        timeToMetro: 10,
                    },
                ],
            },
            viewTypes: [
                'GENERAL',
                'PARKING',
                'HALL',
                'HALL',
                'GENERAL',
                'GENERAL',
                'GENERAL',
                'GENERAL',
                'GENERAL',
                'GENERAL',
                'GENERAL',
                'GENERAL',
            ],
            images: [similarCardImage],
            appLargeImages: [similarCardImage],
            appMiddleSnippetImages: [similarCardImage],
            appLargeSnippetImages: [similarCardImage],
            minicardImages: [similarCardImage],
            siteSpecialProposals: [
                {
                    proposalType: 'INSTALLMENT',
                    description: 'Беспроцентная рассрочка',
                    interestFee: true,
                    durationMonths: 24,
                    mainProposal: false,
                    specialProposalType: 'installment',
                    shortDescription: 'Беспроцентная рассрочка',
                },
            ],
            buildingClass: 'ELITE',
            state: 'HAND_OVER',
            finishedApartments: true,
            price: {
                currency: 'RUR',
                rooms: {
                    '1': {
                        soldout: false,
                        currency: 'RUR',
                        areas: {
                            from: '79.6',
                            to: '84.6',
                        },
                        hasOffers: false,
                        offersCount: 0,
                        priceRatioToMarket: 0,
                        status: 'ON_SALE',
                    },
                    '2': {
                        soldout: false,
                        currency: 'RUR',
                        areas: {
                            from: '155.3',
                            to: '166',
                        },
                        hasOffers: false,
                        offersCount: 0,
                        priceRatioToMarket: 0,
                        status: 'ON_SALE',
                    },
                    '3': {
                        soldout: false,
                        currency: 'RUR',
                        areas: {
                            from: '228.3',
                            to: '294.6',
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
                        currency: 'RUR',
                        hasOffers: false,
                        offersCount: 0,
                        priceRatioToMarket: 0,
                    },
                    PLUS_4: {
                        soldout: false,
                        currency: 'RUR',
                        hasOffers: false,
                        offersCount: 0,
                        priceRatioToMarket: 0,
                    },
                },
                totalOffers: 0,
                priceRatioToMarket: 0,
            },
            flatStatus: 'ON_SALE',
            developers: [
                {
                    id: 268706,
                    name: 'Capital Group',
                    legalName: 'ООО «ЛИСАРИО ТРЕЙДИНГ ЛИМИТЕД»',
                    legalNames: ['ООО «ЛИСАРИО ТРЕЙДИНГ ЛИМИТЕД»'],
                    url: 'http://www.capitalgroup.ru/',
                    logo: '',
                    objects: {
                        all: 33,
                        salesOpened: 17,
                        finished: 24,
                        unfinished: 9,
                        suspended: 0,
                    },
                    address: 'Москва, Пресненская набережная, 8с1',
                    born: '1992-12-31T21:00:00Z',
                    hasChat: false,
                    encryptedPhones: [
                        {
                            phoneWithMask: '+7 495 771 ×× ××',
                            phoneHash: 'KzcFgKHDQJ5NLSkNgNPzcRxLTTcV3LXTcZ3',
                            tag: 0,
                        },
                    ],
                },
            ],
            salesDepartment: {
                id: 295494,
                name: 'Capital Group',
                isRedirectPhones: true,
                weekTimetable: [
                    {
                        dayFrom: 1,
                        dayTo: 7,
                        timePattern: [
                            {
                                open: '09:00',
                                close: '21:00',
                            },
                        ],
                    },
                ],
                logo: '//avatars.mdst.yandex.net/get-realty/3274/company.295494.267970144362838907/builder_logo_info',
                phonesWithTag: [
                    {
                        tag: '',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformIosOffer#utmSource=YandexDirect',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformIosOffer#utmSource=RtbHouse',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformTouchOffer#utmSource=CriteoAdv',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'platformIosOffer',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformTouchCampaign#utmSource=RtbHouse',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformAndroidCampaign#utmSource=CriteoAdv',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformAndroidOffer#utmSource=YandexDirect',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'platformAndroidOffer',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformTouchCampaign#utmSource=YandexDirect',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformIosCampaign#utmSource=CriteoAdv',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'platformDesktopCampaign',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'mapsMobile',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformIosCampaign#utmSource=Mytarget',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformAndroidOffer#utmSource=RtbHouse',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'platformAndroidCampaign',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformAndroidOffer#utmSource=GoogleAdwords',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformAndroidOffer#utmSource=CriteoAdv',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformIosOffer#utmSource=CriteoAdv',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'platformTouchCampaign',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformTouchCampaign#utmSource=CriteoAdv',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformAndroidCampaign#utmSource=RtbHouse',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformDesktopOffer#utmSource=YandexDirect',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformDesktopCampaign#utmSource=RtbHouse',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformDesktopOffer#utmSource=CriteoAdv',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformTouchOffer#utmSource=RtbHouse',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformIosOffer#utmSource=GoogleAdwords',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformTouchCampaign#utmSource=GoogleAdwords',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'platformTouchOffer',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformIosCampaign#utmSource=RtbHouse',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformTouchOffer#utmSource=GoogleAdwords',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'platformIosCampaign',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformAndroidCampaign#utmSource=Mytarget',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformDesktopOffer#utmSource=GoogleAdwords',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformDesktopOffer#utmSource=RtbHouse',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformDesktopCampaign#utmSource=GoogleAdwords',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformTouchOffer#utmSource=YandexDirect',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformIosCampaign#utmSource=YandexDirect',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformDesktopCampaign#utmSource=CriteoAdv',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformIosCampaign#utmSource=GoogleAdwords',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformDesktopCampaign#utmSource=YandexDirect',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'platformDesktopOffer',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformAndroidCampaign#utmSource=GoogleAdwords',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformIosOffer#utmSource=Mytarget',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformAndroidCampaign#utmSource=YandexDirect',
                        phone: '+74996486935',
                    },
                    {
                        tag: 'PlatformAndroidOffer#utmSource=Mytarget',
                        phone: '+74996486935',
                    },
                ],
                timetableZoneMinutes: 180,
                statParams: '',
                encryptedPhones: [
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                    },
                ],
                encryptedPhonesWithTag: [
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: '',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformIosOffer#utmSource=YandexDirect',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformIosOffer#utmSource=RtbHouse',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformTouchOffer#utmSource=CriteoAdv',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'platformIosOffer',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformTouchCampaign#utmSource=RtbHouse',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformAndroidCampaign#utmSource=CriteoAdv',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformAndroidOffer#utmSource=YandexDirect',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'platformAndroidOffer',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformTouchCampaign#utmSource=YandexDirect',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformIosCampaign#utmSource=CriteoAdv',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'platformDesktopCampaign',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'mapsMobile',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformIosCampaign#utmSource=Mytarget',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformAndroidOffer#utmSource=RtbHouse',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'platformAndroidCampaign',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformAndroidOffer#utmSource=GoogleAdwords',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformAndroidOffer#utmSource=CriteoAdv',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformIosOffer#utmSource=CriteoAdv',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'platformTouchCampaign',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformTouchCampaign#utmSource=CriteoAdv',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformAndroidCampaign#utmSource=RtbHouse',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformDesktopOffer#utmSource=YandexDirect',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformDesktopCampaign#utmSource=RtbHouse',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformDesktopOffer#utmSource=CriteoAdv',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformTouchOffer#utmSource=RtbHouse',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformIosOffer#utmSource=GoogleAdwords',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformTouchCampaign#utmSource=GoogleAdwords',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'platformTouchOffer',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformIosCampaign#utmSource=RtbHouse',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformTouchOffer#utmSource=GoogleAdwords',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'platformIosCampaign',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformAndroidCampaign#utmSource=Mytarget',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformDesktopOffer#utmSource=GoogleAdwords',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformDesktopOffer#utmSource=RtbHouse',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformDesktopCampaign#utmSource=GoogleAdwords',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformTouchOffer#utmSource=YandexDirect',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformIosCampaign#utmSource=YandexDirect',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformDesktopCampaign#utmSource=CriteoAdv',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformIosCampaign#utmSource=GoogleAdwords',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformDesktopCampaign#utmSource=YandexDirect',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'platformDesktopOffer',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformAndroidCampaign#utmSource=GoogleAdwords',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformIosOffer#utmSource=Mytarget',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformAndroidCampaign#utmSource=YandexDirect',
                    },
                    {
                        phoneWithMask: '+7 499 648 ×× ××',
                        phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
                        tag: 'PlatformAndroidOffer#utmSource=Mytarget',
                    },
                ],
                encryptedDump: '',
            },
            phone: {
                phoneWithMask: '+7 499 648 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDgN2OPTMR1',
            },
            backCallTrafficInfo: {} as TrafficSourceInfo,
            withBilling: true,
            awards: {},
            limitedCard: false,
        },
    ],
});

const getSiteCard = () =>
    (({
        buildingFeatures: {
            state: 'UNFINISHED',
            class: 'BUSINESS',
            zhkType: 'MFK',
            totalFloors: 62,
            minTotalFloors: 39,
            totalApartments: 720,
            isApartment: false,
            apartmentType: ApartmentType.FLATS,
            ceilingHeight: 300,
            interiorFinish: {
                type: 'ROUGH',
                text: '',
                images: [],
            },
            parking: {
                type: 'UNKNOWN',
                available: true,
            },
            parkings: [
                {
                    type: 'UNDERGROUND',
                    parkingSpaces: 1277,
                    available: true,
                },
            ],
            walls: {
                type: 'MONOLIT',
                text: '',
            },
            decorationInfo: [],
            decorationImages: [],
            wallTypes: [
                {
                    type: 'MONOLIT',
                    text: '',
                },
            ],
        },
        deliveryDates: [
            {
                finished: false,
                quarter: 4,
                year: 2021,
                phaseName: '1 очередь',
                houses: 3,
            },
        ],
        deliveryDatesSummary: {
            phases: 1,
            houses: 3,
        },
        documents: {
            buildingPermitDocs: [
                {
                    url:
                        'https://realty.test.vertis.yandex.ru/storage2/testing/realty/4EE8BCA27CB5315E3D77435ED6AB86D3.pdf',
                    name: 'Разрешение на строительство от 27.03.2017',
                    fileSize: 1988220,
                    fileExtension: 'pdf',
                    downloadUrl:
                        'https://realty.test.vertis.yandex.ru/storage2/testing/realty/4EE8BCA27CB5315E3D77435ED6AB86D3.pdf',
                },
            ],
            projectDeclarationDocs: [
                {
                    url:
                        'https://realty.test.vertis.yandex.ru/storage2/testing/realty/4714A10B6AB5EDC1EB1A6722DED2127C.pdf',
                    name: 'Проектная декларация от 20.02.2021',
                    fileSize: 981548,
                    fileExtension: 'pdf',
                    downloadUrl:
                        'https://realty.test.vertis.yandex.ru/storage2/testing/realty/4714A10B6AB5EDC1EB1A6722DED2127C.pdf',
                },
                {
                    url:
                        'https://realty.test.vertis.yandex.ru/storage2/testing/realty/4BCB32381CF113AA7C768F9B7DF26DED.pdf',
                    name: 'Проектная декларация от 09.02.2021',
                    fileSize: 981675,
                    fileExtension: 'pdf',
                    downloadUrl:
                        'https://realty.test.vertis.yandex.ru/storage2/testing/realty/4BCB32381CF113AA7C768F9B7DF26DED.pdf',
                },
                {
                    url:
                        'https://realty.test.vertis.yandex.ru/storage2/testing/realty/83AB59D3F109B7679C9701D85AE9C713.pdf',
                    name: 'Проектная декларация от 05.01.2021',
                    fileSize: 978686,
                    fileExtension: 'pdf',
                    downloadUrl:
                        'https://realty.test.vertis.yandex.ru/storage2/testing/realty/83AB59D3F109B7679C9701D85AE9C713.pdf',
                },
                {
                    url:
                        'https://realty.test.vertis.yandex.ru/storage2/testing/realty/3719AE6A69FBE34C3B3F48F357BABC80.pdf',
                    name: 'Проектная декларация от 20.05.2019',
                    fileSize: 504378,
                    fileExtension: 'pdf',
                    downloadUrl:
                        'https://realty.test.vertis.yandex.ru/storage2/testing/realty/3719AE6A69FBE34C3B3F48F357BABC80.pdf',
                },
                {
                    url:
                        'https://realty.test.vertis.yandex.ru/storage2/testing/realty/E185BDAD96430B4E60F95C70225547A1.pdf',
                    name: 'Проектная декларация от 21.03.2019',
                    fileSize: 731892,
                    fileExtension: 'pdf',
                    downloadUrl:
                        'https://realty.test.vertis.yandex.ru/storage2/testing/realty/E185BDAD96430B4E60F95C70225547A1.pdf',
                },
                {
                    url:
                        'https://realty.test.vertis.yandex.ru/storage2/testing/realty/D22750608B360253B649EFE018969E36.pdf',
                    name: 'Проектная декларация от 03.08.2018',
                    fileSize: 24976997,
                    fileExtension: 'pdf',
                    downloadUrl:
                        'https://realty.test.vertis.yandex.ru/storage2/testing/realty/D22750608B360253B649EFE018969E36.pdf',
                },
                {
                    url:
                        'https://realty.test.vertis.yandex.ru/storage2/testing/realty/8889514C3DFDDD4D783F6C0FA23E6670.pdf',
                    name: 'Проектная декларация от 20.06.2017',
                    fileSize: 24904831,
                    fileExtension: 'pdf',
                    downloadUrl:
                        'https://realty.test.vertis.yandex.ru/storage2/testing/realty/8889514C3DFDDD4D783F6C0FA23E6670.pdf',
                },
            ],
            operationActDocs: [],
        },
        fullName: 'МФК «Capital Towers»',
        locativeFullName: 'в МФК «Capital Towers»',
        id: 872687,
        location: {
            rgid: 197177,
            settlementRgid: 587795,
            address: 'Москва, наб. Краснопресненская, вл. 14',
            subjectFederationId: 1,
            subjectFederationRgid: 741964,
            expectedMetroList: [],
            schools: [],
            parks: [
                {
                    parkId: '121405908',
                    name: 'парк Красная Пресня',
                    timeOnFoot: 529,
                    distanceOnFoot: 736,
                    latitude: 55.752327,
                    longitude: 37.54937,
                    timeDistanceList: [
                        {
                            transport: 'ON_FOOT',
                            time: 8,
                            distance: 736,
                        },
                    ],
                },
            ],
            ponds: [
                {
                    pondId: '137667589',
                    name: 'река Москва',
                    timeOnFoot: 180,
                    distanceOnFoot: 253,
                    latitude: 55.749245,
                    longitude: 37.54954,
                    timeDistanceList: [
                        {
                            transport: 'ON_FOOT',
                            time: 3,
                            distance: 253,
                        },
                    ],
                },
                {
                    pondId: '164173396',
                    name: 'Краснопресненские пруды',
                    timeOnFoot: 180,
                    distanceOnFoot: 286,
                    latitude: 55.753666,
                    longitude: 37.550816,
                    timeDistanceList: [
                        {
                            transport: 'ON_FOOT',
                            time: 3,
                            distance: 286,
                        },
                    ],
                },
                {
                    pondId: '1703440097',
                    name: 'Нижний Красногвардейский пруд',
                    timeOnFoot: 300,
                    distanceOnFoot: 469,
                    latitude: 55.755585,
                    longitude: 37.547,
                    timeDistanceList: [
                        {
                            transport: 'ON_FOOT',
                            time: 5,
                            distance: 469,
                        },
                    ],
                },
            ],
            airports: [
                {
                    id: '878042',
                    name: 'Шереметьево',
                    timeOnCar: 2370,
                    distanceOnCar: 37383,
                    latitude: 55.963852,
                    longitude: 37.4169,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 39,
                            distance: 37383,
                        },
                    ],
                },
                {
                    id: '878065',
                    name: 'Внуково',
                    timeOnCar: 3097,
                    distanceOnCar: 31312,
                    latitude: 55.604942,
                    longitude: 37.282578,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 51,
                            distance: 31312,
                        },
                    ],
                },
                {
                    id: '858742',
                    name: 'Домодедово',
                    timeOnCar: 3182,
                    distanceOnCar: 49515,
                    latitude: 55.41435,
                    longitude: 37.90048,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 53,
                            distance: 49515,
                        },
                    ],
                },
                {
                    id: '878109',
                    name: 'Жуковский (Раменское)',
                    timeOnCar: 3758,
                    distanceOnCar: 55630,
                    latitude: 55.568665,
                    longitude: 38.143654,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 62,
                            distance: 55630,
                        },
                    ],
                },
            ],
            cityCenter: [
                {
                    transport: 'ON_CAR',
                    time: 733,
                    distance: 6745,
                    latitude: 55.749058,
                    longitude: 37.612267,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 12,
                            distance: 6745,
                        },
                    ],
                },
            ],
            heatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: 'ffe424',
                    description: 'достаточно развитая',
                    level: 4,
                    maxLevel: 8,
                    title: 'Инфраструктура',
                },
                {
                    name: 'transport',
                    rgbColor: '9ada1b',
                    description: 'высокая доступность',
                    level: 6,
                    maxLevel: 9,
                    title: 'Транспорт',
                },
            ],
            allHeatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: 'ffe424',
                    description: 'достаточно развитая',
                    level: 4,
                    maxLevel: 8,
                    title: 'Инфраструктура',
                },
                {
                    name: 'price-rent',
                    rgbColor: 'ee4613',
                    description: 'очень высокая',
                    level: 2,
                    maxLevel: 9,
                    title: 'Цена аренды',
                },
                {
                    name: 'transport',
                    rgbColor: '9ada1b',
                    description: 'высокая доступность',
                    level: 6,
                    maxLevel: 9,
                    title: 'Транспорт',
                },
                {
                    name: 'carsharing',
                    rgbColor: '30ce12',
                    description: 'высокая',
                    level: 6,
                    maxLevel: 8,
                    title: 'Доступность Яндекс.Драйва',
                },
            ],
            insideMKAD: true,
            routeDistances: [],
            metro: {
                lineColors: ['099dd4'],
                metroGeoId: 98559,
                rgbColor: '099dd4',
                metroTransport: 'ON_FOOT',
                name: 'Выставочная',
                timeToMetro: 11,
            },
            metroList: [
                {
                    lineColors: ['099dd4'],
                    metroGeoId: 98559,
                    rgbColor: '099dd4',
                    metroTransport: 'ON_FOOT',
                    name: 'Выставочная',
                    timeToMetro: 11,
                },
                {
                    lineColors: ['6fc1ba'],
                    metroGeoId: 115085,
                    rgbColor: '6fc1ba',
                    metroTransport: 'ON_FOOT',
                    name: 'Деловой Центр',
                    timeToMetro: 12,
                },
                {
                    lineColors: ['099dd4'],
                    metroGeoId: 98560,
                    rgbColor: '099dd4',
                    metroTransport: 'ON_FOOT',
                    name: 'Международная',
                    timeToMetro: 17,
                },
                {
                    lineColors: ['ed9f2d'],
                    metroGeoId: 218566,
                    rgbColor: 'ed9f2d',
                    metroTransport: 'ON_FOOT',
                    name: 'Тестовская',
                    timeToMetro: 20,
                },
                {
                    lineColors: ['ffa8af'],
                    metroGeoId: 152947,
                    rgbColor: 'ffa8af',
                    metroTransport: 'ON_FOOT',
                    name: 'Деловой Центр',
                    timeToMetro: 20,
                },
            ],
        },
        name: 'Capital Towers',
        price: {
            currency: 'RUR',
            rooms: {
                '1': {
                    soldout: false,
                    currency: 'RUR',
                    areas: {
                        from: '50.4',
                        to: '104.3',
                    },
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                    status: 'ON_SALE',
                },
                '2': {
                    soldout: false,
                    currency: 'RUR',
                    areas: {
                        from: '75.6',
                        to: '117.5',
                    },
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                    status: 'ON_SALE',
                },
                '3': {
                    soldout: false,
                    currency: 'RUR',
                    areas: {
                        from: '109.1',
                        to: '133.9',
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
                    soldout: true,
                    currency: 'RUR',
                    areas: {
                        from: '28',
                        to: '30.7',
                    },
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                    status: 'NOT_ON_SALE',
                },
                PLUS_4: {
                    soldout: false,
                    currency: 'RUR',
                    areas: {
                        from: '153.3',
                        to: '168.4',
                    },
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                    status: 'ON_SALE',
                },
            },
            totalOffers: 0,
            priceRatioToMarket: 0,
        },
        salesClosed: false,
        images: {
            main: '//avatars.mds.yandex.net/get-verba/216201/2a0000016097f0fa5c5f789fde549131a9b6/realty_main',
            list: [],
        },
        transactionTerms: {
            mortgage: true,
            installment: true,
            agreementType: 'DDU',
            documentsUrl: 'http://capital-towers.ru/upload/docs/docs_CT.zip',
            fz214: true,
            permission: true,
            smarthome: false,
        },
        description: '',
        permalink: '24684153505',
        panoramasTagged: [
            {
                tag: 'street-view-1',
                urls: [
                    'https://yandex.ru/maps/213/moscow/?from=api-maps&ll=37.548495,55.751399&mode=whatshere&origin=jsapi_2_1_74&panorama[direction]=226.587773,3.942607&panorama[full]=true&panorama[point]=37.549522,55.752142&panorama[span]=120.000000,63.850531&whatshere[point]=37.549729,55.751199&whatshere[zoom]=17&z=17',
                ],
            },
        ],
        flatStatus: 'ON_SALE',
        constructionState: 'UNDER_CONSTRUCTION',
        limitedCard: false,
        siteType: 'dd',
        hasSideAdfox: false,
        regionInfo: {
            parents: [
                {
                    id: 120538,
                    rgid: '193368',
                    name: 'Пресненский район',
                    type: 'CITY_DISTRICT',
                },
                {
                    id: 20279,
                    rgid: '596687',
                    name: 'Центральный административный округ',
                    type: 'CITY_DISTRICT',
                },
                {
                    id: 213,
                    rgid: '165705',
                    name: 'Москва (без Новой Москвы)',
                    type: 'CITY',
                },
                {
                    id: 213,
                    rgid: '587795',
                    name: 'Москва',
                    type: 'CITY',
                },
                {
                    id: 1,
                    rgid: '741964',
                    name: 'Москва и МО',
                    type: 'SUBJECT_FEDERATION',
                },
                {
                    id: 225,
                    rgid: '143',
                    name: 'Россия',
                    type: 'COUNTRY',
                },
                {
                    id: 0,
                    rgid: '0',
                    name: 'Весь мир',
                    type: 'UNKNOWN',
                },
            ],
            rgid: 197177,
            name: 'Московский Международный Деловой Центр Москва-Сити',
            sitesRgids: {
                district: 741964,
                mainCity: 587795,
            },
            locative: 'Москва-Сити',
            isInLO: false,
            isInMO: true,
        },
        construction: [],
        phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
        salesDepartment: {
            id: 295494,
            name: 'Capital Group',
            isRedirectPhones: true,
            weekTimetable: [
                {
                    dayFrom: 1,
                    dayTo: 7,
                    timePattern: [
                        {
                            open: '09:00',
                            close: '21:00',
                        },
                    ],
                },
            ],
            logo: developerLogo,
            timetableZoneMinutes: 180,
            statParams:
                // eslint-disable-next-line max-len
                'lBe5lMmwQWWe63CMAjVmPLo/EOM+g236K7RFoSluCvaYiIe1aSjzUoYyIfijuEu9CmxpZBEA2wXMeDb/hfATwpXHkggcuvoPkAzODrs2lymljDgNA4byvlQ5CtqcGF4xRM4ZKFB/ED8FQO7C8BN2yh0Ix1uY7CPQIlz5xm/GWYXy0YPn+VRwmcflymgkTF15/EqL5H8driaioalaLwgT1lzVLJ5onqr1BJWH0hWmCdLSMx4TjIEE2IvM6Fbvstqc/mvJXeMOkn7cSlcvF1YwWFS7N1i/47bf3uKHT+0ILklPwkuCLzzhNasN+t9avREIzoX1Nk3v6Xkr/8qESkPWMt8aMbarO/5hHo87ojhlLp1k7AnaNA6I+qV+4Y7HsZ5xCmxpZBEA2wXMeDb/hfATwpn02CFSg/YgKInsdWAutseciILCSb+gPE36U9F6aMaHn8F+866oZM6E9nx5/Vc6poKw9b7rSDDtSufLUaGlXmRkLnk3IO5wzXK9pUSGiL5CjeUQONV5L8unFga5IvP8DxesLjrS2sWi5MDnOjUfTDS1+bSuUqIGxZ42UBIc6OW0R+nDt0V+LorRYZMiTjV8aF9mluBgR8P58+NKMpzGp4UtxWHRvlMnwrM5QPat+l6bLeHAXdztHmcIBL7W7wOgdzeomXQUtOsVPo0qfQT+mzv94PDGpxPQCZB+XCr8/vro2gXzyEZIGAsSS69G++32J0FWgTQt0RtPqq1ZAYTvgEi5BQxLm4s9WVILgl1adGCxGGZyKfLpGZr6wq3exHicb7qtWD2JWfnAzO4+Bb81LWPhF9CyHodZBrcC7wa0v0GCYBBw8VyV9EeqZdsWKXd3K8kzKDyIwUDqIUhbjF4qUoyJTaW6yEfUNML/YIINjLNs4B/vc9BuyAE3u65/OlwZe4zuJaR5kgBPPzrIlWw8e/G7aK100sRCWYtWbIduiyis7KW+KZElY7IOTAEUwfAS2ubiXeHtNhTFuurdKxIC4wpjPeEZGVSNHU9zx94S4jmFg1vrQOStl9NMAnnYvykTHd0NSultCSTqmwK0keKU7FJji8Nxz35i01WyXu9AAy4ncj/PerovgbodBn0uH+lt439O5GuE/nKXWvrar9dAHZ0/wDyDnAAibIzmfUSwBeFW3naiM2FbCN895zCa+b07DKbcR4SSgLRlyt5/JW611JlCigBsL2FL4yso4unovXKBrvtTeXR8JEsZSGaCxDIvbzQRVXekIA6qZlWET4nFkJ8Z39A2uOdnY+XPfTHXWmsf1fXB14E5Lm/t6WDSjDvN8mOU5bX3cHzjBDxXpslTebX1L8dQaDnjKtvjNu5fxxYco/lUqLnjWYRe+c8BeZ9tU8P5oN/oPP3qs5SAadUdlprPYqe3uf0tg+tlFpyp/20nP+zhAHSqp20agM4QCaFZ/W6WIxdvb4x/iwU8pW+W1UkuLosIsUnBUWdCKkWQaBEObsV12x/alyBWOJ7UN46UptYjogN3d7sp4scIqPLKgbSesXxF3PCyBRzy6DTB8jUPiXNmlAEI3giqQ4Thli10QEAIV7X0Iw957UxRQRsfdhyFfoUZFZaz0KrakNI2NkbOQooAbC9hS+MrKOLp6L1yga77U3l0fCRLGUhmgsQyL29lc94QF+hq5HLArfI2RdWOx00IU409GdZXXDDGirFJj6tk/SroywWyGztW+WE4qEoVdhmVAxhUA4qCyL+xak0qKw8fm7Kjyr4LyN/uy0ECxKnk8SFRqohE1qUBVf0n/ISBVtTxROLCbLgRFJYsJXTR/WV0rwTioso9UeZaJyAsqcQLYqNL6aXI4zgWWLQjDj+JaysyhOhcAiFYmGutP4gHWLfsMDFezdqoVHsh7wCh4N1l2Su+tvdxXBgEsqXQG/VNMwf+EIxCa/YAVtlGHrOCxdpbdldcEnAG59MwdgF/jONgx/V7ycS2RmyTv3RBNh7lAEiG7jU9igeJh/NaainW23faQ9umW5IusuEshz9Ko1u1qonUxvK5NZXgxtpCbLCvr+aE715L89WjuVXI0+xkue5TWjca3kJdWcz2tTb8CUN8slwlJuFphgmaw9n18KKsrbQGr9YGDMJR2m1cwL6ZHQjHW5jsI9AiXPnGb8ZZhRimPCqSM5g94fAr5vzA5E5c1SyeaJ6q9QSVh9IVpgnSRiKTb1auwrIVl46VmyHe2RVr+y7hNfOLIEaAvDGCWey4iVypoDanzzlwp4IcxkOxyjWqx7p2S1IBV7uIS1vsV1F/fbMmj1AzqcbjlPp0cBwjS2SB5F40XUBansncVFS1Gq3WHC6iFqScWkzXT8pKROj8vdRdU6BxdHg8FCYK8gYyHtb3KC/M0AiPlfhQYrs9uxuBxpZY87VPR6NrAZ0DhB0H91ROM96yMHyt89nRdaZuliMXb2+Mf4sFPKVvltVJ4DB9t7uh7dTibHZuN9i5F7sR0S3taIBq2i2nh0l1/pQKbGlkEQDbBcx4Nv+F8BPC7kDzjVTXjNn5llvYDbh1TWvv8wUPV3scCFAzEJjuDbddW8bUMur7J62ayszSfO6EXnFeDNq9LX2AQHxC+T4fxRpWd3JJ12+1Nb8LGa0EzzhC/tiqJRVTZZZf3U7e0ie4SUo+GnJKZhIBM0QHs6ZXsvjoCXNhXWvnf5JNIQ5jgPW1bdy1SSC+KJMntlZcGtcHlOXLHuxG5E4/PZLde7jc+mTsCdo0Doj6pX7hjsexnnEsMPgcIBoL4ID23KtRYkUjwBl1Q9fV0yd/XYiTYJp4gA==',
            encryptedPhones: [
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                },
            ],
            encryptedPhonesWithTag: [
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: '',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformIosOffer#utmSource=YandexDirect',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformIosOffer#utmSource=RtbHouse',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformTouchOffer#utmSource=CriteoAdv',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'platformIosOffer',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformTouchCampaign#utmSource=RtbHouse',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformAndroidCampaign#utmSource=CriteoAdv',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformAndroidOffer#utmSource=YandexDirect',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'platformAndroidOffer',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformTouchCampaign#utmSource=YandexDirect',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformIosCampaign#utmSource=CriteoAdv',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'platformDesktopCampaign',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'mapsMobile',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformIosCampaign#utmSource=Mytarget',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformAndroidOffer#utmSource=RtbHouse',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'platformAndroidCampaign',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformAndroidOffer#utmSource=GoogleAdwords',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformAndroidOffer#utmSource=CriteoAdv',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformIosOffer#utmSource=CriteoAdv',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'platformTouchCampaign',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformTouchCampaign#utmSource=CriteoAdv',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformAndroidCampaign#utmSource=RtbHouse',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformDesktopOffer#utmSource=YandexDirect',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformDesktopCampaign#utmSource=RtbHouse',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformDesktopOffer#utmSource=CriteoAdv',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformTouchOffer#utmSource=RtbHouse',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformIosOffer#utmSource=GoogleAdwords',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformTouchCampaign#utmSource=GoogleAdwords',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'platformTouchOffer',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformIosCampaign#utmSource=RtbHouse',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformTouchOffer#utmSource=GoogleAdwords',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'platformIosCampaign',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformAndroidCampaign#utmSource=Mytarget',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformDesktopOffer#utmSource=GoogleAdwords',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformDesktopOffer#utmSource=RtbHouse',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformDesktopCampaign#utmSource=GoogleAdwords',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformTouchOffer#utmSource=YandexDirect',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformIosCampaign#utmSource=YandexDirect',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformDesktopCampaign#utmSource=CriteoAdv',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformIosCampaign#utmSource=GoogleAdwords',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformDesktopCampaign#utmSource=YandexDirect',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'platformDesktopOffer',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformAndroidCampaign#utmSource=GoogleAdwords',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformIosOffer#utmSource=Mytarget',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformAndroidCampaign#utmSource=YandexDirect',
                },
                {
                    phoneWithMask: '+7 499 649 ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'PlatformAndroidOffer#utmSource=Mytarget',
                },
            ],
            encryptedDump:
                // eslint-disable-next-line max-len
                'wYGpH7ysR9eP9EPuMriL7llI4Es6xBaFKl/BY1pFlX8d4cWKHqLUhhnJ7568AtN2Ess3QMrS/3KQlluZt1jLytJzn5fs3v2TfQuTNaq709hz7i6HAjUWj3rmgpSnE3tWSPpm3ZlWl6IqsnYqDa0NNNP1tF5KaP8B7sgEnBu432Oo0aViiPfBUv7pY3VmAp5vC7NZQyMGhomsFC8S569qu5pXehvzApFvQygKqHcly1qw4StplGFiLINx5lZfzK6O3e3s7ov/rUw/FbS5CGJ+KmTUnbWo8wAnM3EX7MvR/w5GGVskVvVmm/2XAK6JDfAX112rk7gvyFvF3ypodHYoNUG/j5ZQaCrb4CSLW17M3sEKlElknigrckPOMO37e41008Vaafxwz/+/enFUTgaumyzhE2JQUaqmu12OdYER8AsHq1NnudqR2vXtF0Vb39ZaoHVy/EIky/pnB2kt67fvYFyiUoCZMuJT4wNir2YSHCMmL1UuCnbyvX8QphdynIWKVuA3cTdwGLzsHt3oauVl6E7TUTtpgBryoqLUh0X31Cq5HJU6V+A47GBEovrYzedH0IZNh5RGwPIX2rz1SO9bJU3DNNFAeN9ntAo97qgE8LgQEwDTtGsK5EqrKWd44EIdKWViaXcjOSUtjFlLfgP7oVrkY3yHLA6k8n/gJE1ZRtv1vU15E9vaC4bKpaqHN0g0qgI7s4MlGKw7m8T6D+PdCoc56/JG1/IJKs4aeV6YHuTYtc4Rt4EMyMe3To5u49RMya3Fi+VlUFwHXJ3pTJ59Tct4vsFz73edbacXi8hMlE2g5f5uFiU61PQznXYoRPtQ43KxDq+y2pFCesiLDg4t1wq2wFeoqvxQweGc2+CUqfH39lyI6qj1hsTP2xdO1ADVkyARtIpfbFDYBKVmQQ==',
        },
        developer: {
            id: 268706,
            name: 'Capital Group',
            legalName: 'ООО «МЕГАПОЛИС ГРУП»',
            legalNames: ['ООО «МЕГАПОЛИС ГРУП»'],
            url: 'http://www.capitalgroup.ru/',
            logo: developerLogo,
            objects: {
                all: 33,
                salesOpened: 17,
                finished: 24,
                unfinished: 9,
                suspended: 0,
            },
            address: 'Москва, Пресненская набережная, 8с1',
            born: '1992-12-31T21:00:00Z',
            hasChat: true,
        },
        developers: [
            {
                id: 268706,
                name: 'Capital Group',
                legalName: 'ООО «МЕГАПОЛИС ГРУП»',
                legalNames: ['ООО «МЕГАПОЛИС ГРУП»'],
                url: 'http://www.capitalgroup.ru/',
                logo: developerLogo,
                objects: {
                    all: 33,
                    salesOpened: 17,
                    finished: 24,
                    unfinished: 9,
                    suspended: 0,
                },
                address: 'Москва, Пресненская набережная, 8с1',
                born: '1992-12-31T21:00:00Z',
                hasChat: true,
                encryptedPhones: [
                    {
                        phoneWithMask: '+7 495 021 ×× ××',
                        phoneHash: 'KzcF0OHTUJwMLjEN2NPjQRw',
                        tag: 0,
                    },
                ],
            },
        ],
        isFromPromo: false,
        areasRange: [28, 168.4],
        mortgages: [],
        siteSpecialProposals: [
            {
                specialProposalType: 'sale',
                proposalType: 'sale',
                shortDescription: 'Ставка по ипотеке в 0.1%',
                fullDescription: 'Ставка в рамках программы составит 0.1% на первые 6 мес. выплат, далее 8.1% годовых.',
                mainProposal: true,
            },
            {
                specialProposalType: 'installment',
                proposalType: 'installment',
                shortDescription: 'Беспроцентная рассрочка',
                fullDescription: 'Беспроцентная рассрочка до двух лет от компании Система Лизинг 24 (АО).',
                freeInstallment: true,
                durationMonths: 24,
                mainProposal: false,
                objectType: 'flat',
            },
        ],
        hasMilitaryMortgage: false,
        withOffers: true,
        resaleTotalOffers: 0,
        timestamp: 1626809121238,
        referer:
            'https://realty-frontend.realty.local.dev.vertis.yandex.ru/moskva/kupit/novostrojka/capital-towers-872687/ipoteka/',
        from: 'direct',
        backCallTrafficInfo: {} as TrafficSourceInfo,
        withBilling: true,
    } as unknown) as ISiteCardMobile);

const breadcrumbs = [
    {
        title: 'Я.Недвижимость',
        link: '/',
    },
    {
        title: 'Москва и МО',
        link: '/moskva_i_moskovskaya_oblast/',
    },
    {
        title: 'Купить',
        link: '/moskva_i_moskovskaya_oblast/kupit/',
    },
    {
        title: 'Квартира в новостройке',
        link: '/moskva_i_moskovskaya_oblast/kupit/novostrojka/',
    },
    {
        title: 'МФК «Capital Towers»',
        link: '',
    },
    {
        title: 'Планировки в МФК «Capital Towers»',
        link: '',
    },
];

export const getInitialState = () => ({
    filters: {
        card: {
            sections: {
                mainShown: true,
                extraShown: false,
            },
            controller: 'newbuilding',
            decl: {
                price: {
                    control: 'number-range',
                    maxLength: 15,
                    fromName: 'priceMin',
                    toName: 'priceMax',
                    placeholderValues: ['2 616 000', '10 007 000'],
                },
                priceType: {
                    control: 'radioset',
                    defaultValue: 'PER_OFFER',
                    values: ['PER_OFFER', 'PER_METER'],
                },
                area: {
                    control: 'number-range',
                    maxLength: 4,
                    unit: 'unit_square_m',
                    fromName: 'areaMin',
                    toName: 'areaMax',
                    placeholderValues: [20, 91],
                },
                kitchenSpace: {
                    control: 'number-range',
                    maxLength: 4,
                    unit: 'unit_square_m',
                    fromName: 'kitchenSpaceMin',
                    toName: 'kitchenSpaceMax',
                    placeholderValues: [10, 31],
                },
                floors: {
                    control: 'number-range',
                    maxLength: 3,
                    fromName: 'floorMin',
                    toName: 'floorMax',
                    placeholderValues: [1, 25],
                },
                bathroomUnit: {
                    control: 'multi-select',
                    allowed: [['MATCHED', 'SEPARATED']],
                    values: ['MATCHED', 'SEPARATED', 'TWO_AND_MORE'],
                },
                decoration: {
                    control: 'multi-select',
                    allowed: [[]],
                    values: ['CLEAN', 'ROUGH', 'TURNKEY'],
                },
                houseId: {
                    control: 'multi-select',
                    allowed: [['756548', '756551', '2048044']],
                    houseData: {
                        756548: {
                            commissioningDate: {
                                constructionState: 'CONSTRUCTION_STATE_FINISHED',
                            },
                            houseName: 'Корпус 3А',
                        },
                        756551: {
                            commissioningDate: {
                                year: 2020,
                                quarter: 4,
                                constructionState: 'CONSTRUCTION_STATE_UNFINISHED',
                            },
                            houseName: 'Корпус 2А',
                        },
                        2048044: {
                            commissioningDate: {
                                year: 2021,
                                quarter: 3,
                                constructionState: 'CONSTRUCTION_STATE_UNFINISHED',
                            },
                        },
                    },
                    values: ['756548', '756551', '2048044'],
                },
                roomsTotal: {
                    control: 'multi-select',
                    allowed: [['1', 'STUDIO', '3', '2']],
                    values: ['STUDIO', '1', '2', '3', 'PLUS_4'],
                },
                floorExceptFirst: {
                    control: 'checkbox',
                },
                lastFloor: {
                    control: 'checkbox',
                    positiveValue: 'NO',
                },
            },
        },
    },
    forms: {},
    page: {
        isLoading: false,
    },
    sitePlans: {
        plans: {
            items: [
                {
                    roomType: 3,
                    roomCount: 3,
                    wholeArea: {
                        value: 61,
                        unit: 'SQ_M',
                        name: 'whole',
                    },
                    livingArea: {
                        value: 33.8,
                        unit: 'SQ_M',
                        name: 'living',
                    },
                    kitchenArea: {
                        value: 11.7,
                        unit: 'SQ_M',
                        name: 'kitchen',
                    },
                    clusterId: '166185-3EBECA6B2FDD87DE',
                    floors: [6],
                    commissioningDate: [
                        {
                            year: 2021,
                            quarter: 4,
                            constructionState: 'CONSTRUCTION_STATE_UNKNOWN',
                        },
                    ],
                    pricePerOffer: {
                        currency: 'RUB',
                        from: '8631500',
                        to: '8631500',
                    },
                    pricePerMeter: {
                        currency: 'RUB',
                        from: '141500',
                        to: '141500',
                    },
                    offersCount: 1,
                    offerId: '6302897158554881686',
                    images: {},
                },
            ],
        },
    },
    breadcrumbs,
    backCall: {
        value: '',
        savedPhone: '',
        status: 'normal',
    },
    similar: getSimilar(),
    newbuildingCardPage: {
        card: getSiteCard(),
    },
    user: {
        favorites: [],
        favoritesMap: {},
    },
});

export const getProps = () =>
    (({
        card: getSiteCard(),
        plansFilters: {},
        link: () => '',
    } as unknown) as INewbuildingPlansProps);
