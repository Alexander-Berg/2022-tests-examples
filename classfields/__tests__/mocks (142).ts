import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { IUserStore } from 'realty-core/view/react/common/reducers/user';
import { ISiteCard, ApartmentType } from 'realty-core/types/siteCard';

export const user = ({} as unknown) as IUserStore;

const developerLogo = generateImageUrl({ width: 40, height: 40 });

export const defaultState = {
    backCall: {
        value: '',
        savedPhone: '',
        status: 'normal',
    },
    user: {
        favoritesMap: {},
    },
    siteSpecialProjectsSecondPackage: [
        {
            params: {
                geoId: [1, 10174],
                startDate: '2020-12-01',
                endDate: '2021-12-31',
            },
            data: {
                developerId: 102320,
                developerName: 'Группа «Самолет»',
                phones: {
                    '1': '+7 (999) 999-99-99',
                    '10174': '+7 (888) 888-88-88',
                },
                pinnedSiteIds: [],
            },
        },
    ],
};

export const stateWithFavorites = {
    ...defaultState,
    user: {
        favoritesMap: {
            site_872687: true,
        },
    },
};

export const stateWithSalesDepartment = {
    ...defaultState,
    salesDepartment: {
        newbuilding_872687: {
            id: 295494,
            name: 'Capital Group',
            phones: ['+74996495079'],
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
            dump: null,
            phonesWithTag: [
                {
                    tag: 'platformDesktopCampaign',
                    phone: '+74996495079',
                },
            ],
            statParams: '',
            timetableZoneMinutes: 180,
            encryptedDump: '',
        },
    },
};

export const menu = [
    { title: 'Скидки и акции', value: 'proposals' },
    { title: 'Ипотека', value: 'mortgages' },
    { title: 'Описание', value: 'description' },
    { title: 'Инфраструктура', value: 'infrastructure' },
    { title: 'Ход строительства', value: 'progress' },
    { title: 'Отзывы', value: 'reviews' },
];

export const card = ({
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
        list: [
            {
                viewType: 'GENERAL',
                appLarge: generateImageUrl({ width: 1000, height: 800 }),
                full: generateImageUrl({ width: 1500, height: 1000 }),
                cosmic: generateImageUrl({ width: 100, height: 80 }),
            },
        ],
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
            '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',
        encryptedPhones: [
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
            },
        ],
        encryptedPhonesWithTag: [
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: '',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformIosOffer#utmSource=YandexDirect',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformIosOffer#utmSource=RtbHouse',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformTouchOffer#utmSource=CriteoAdv',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'platformIosOffer',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformTouchCampaign#utmSource=RtbHouse',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformAndroidCampaign#utmSource=CriteoAdv',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformAndroidOffer#utmSource=YandexDirect',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'platformAndroidOffer',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformTouchCampaign#utmSource=YandexDirect',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformIosCampaign#utmSource=CriteoAdv',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'platformDesktopCampaign',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'mapsMobile',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformIosCampaign#utmSource=Mytarget',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformAndroidOffer#utmSource=RtbHouse',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'platformAndroidCampaign',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformAndroidOffer#utmSource=GoogleAdwords',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformAndroidOffer#utmSource=CriteoAdv',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformIosOffer#utmSource=CriteoAdv',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'platformTouchCampaign',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformTouchCampaign#utmSource=CriteoAdv',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformAndroidCampaign#utmSource=RtbHouse',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformDesktopOffer#utmSource=YandexDirect',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformDesktopCampaign#utmSource=RtbHouse',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformDesktopOffer#utmSource=CriteoAdv',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformTouchOffer#utmSource=RtbHouse',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformIosOffer#utmSource=GoogleAdwords',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformTouchCampaign#utmSource=GoogleAdwords',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'platformTouchOffer',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformIosCampaign#utmSource=RtbHouse',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformTouchOffer#utmSource=GoogleAdwords',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'platformIosCampaign',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformAndroidCampaign#utmSource=Mytarget',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformDesktopOffer#utmSource=GoogleAdwords',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformDesktopOffer#utmSource=RtbHouse',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformDesktopCampaign#utmSource=GoogleAdwords',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformTouchOffer#utmSource=YandexDirect',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformIosCampaign#utmSource=YandexDirect',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformDesktopCampaign#utmSource=CriteoAdv',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformIosCampaign#utmSource=GoogleAdwords',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformDesktopCampaign#utmSource=YandexDirect',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'platformDesktopOffer',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformAndroidCampaign#utmSource=GoogleAdwords',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformIosOffer#utmSource=Mytarget',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformAndroidCampaign#utmSource=YandexDirect',
            },
            {
                phoneWithMask: '+7 499 649 ×× ××',
                phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                tag: 'PlatformAndroidOffer#utmSource=Mytarget',
            },
        ],
        encryptedDump:
            // eslint-disable-next-line max-len
            'wYGpH7ysR9eP9EPuMriL7llI4Es6xBaFKl/BY1pFlX8d4cWKHqLUhhnJ7568AtN2Ess3QMrS/3KQlluZt1jLytJzn5fs3v2TfQuTNaq709hz7i6HAjUWj3rmgpSnE3tWSPpm3ZlWl6IqsnYqDa0NNNP1tF5KaP8B7sgEnBu432Oo0aViiPfBUv7pY3VmAp5vC7NZQyMGhomsFC8S569qu5pXehvzApFvQygKqHcly1qw4StplGFiLINx5lZfzK6O3e3s7ov/rUw/FbS5CGJ+KmTUnbWo8wAnM3EX7MvR/w5GGVskVvVmm/2XAK6JDfAX112rk7gvyFvF3ypodHYoNUG/j5ZQaCrb4CSLW17M3sEKlElknigrckPOMO37e41008Vaafxwz/+/enFUTgaumyzhE2JQUaqmu12OdYER8AsHq1NnudqR2vXtF0Vb39ZaoHVy/EIky/pnB2kt67fvYFyiUoCZMuJT4wNir2YSHCMmL1UuCnbyvX8QphdynIWKVuA3cTdwGLzsHt3oauVl6E7TUTtpgBryoqLUh0X31Cq5HJU6V+A47GBEovrYzedH0IZNh5RGwPIX2rz1SO9bJU3DNNFAeN9ntAo97qgE8LgQEwDTtGsK5EqrKWd44EIdKWViaXcjOSUtjFlLfgP7oVrkY3yHLA6k8n/gJE1ZRtv1vU15E9vaC4bKpaqHN0g0qgI7s4MlGKw7m8T6D+PdCoc56/JG1/IJKs4aeV6YHuTYtc4Rt4EMyMe3To5u49RMya3Fi+VlUFwHXJ3pTJ59Tct4vsFz73edbacXi8hMlE2g5f5uFiU61PQznXYoRPtQ43KxDq+y2pFCesiLDg4t1wq2wFeoqvxQweGc2+CUqfH39lyI6qj1hsTP2xdO1ADVkyARtIpfbFDYBKVmQQ==',
    },
    developer: {
        id: 102320,
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
    },
    developers: [
        {
            id: 102320,
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
                    phoneWithMask: '+7 495 021 ×× ××',
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
    withOffers: false,
    resaleTotalOffers: 0,
    timestamp: 1626809121238,
    referer:
        'https://realty-frontend.realty.local.dev.vertis.yandex.ru/moskva/kupit/novostrojka/capital-towers-872687/ipoteka/',
    from: 'direct',
    backCallTrafficInfo: {},
    withBilling: true,
} as unknown) as ISiteCard;

export const cardWithDifferentImages = {
    ...card,
    images: {
        ...card.images,
        list: ['GENERAL', 'GENPLAN', 'COURTYARD', 'ENTRANCE', 'LIFT', 'PARKING', 'HALL'].map((viewType) => ({
            viewType,
            appLarge: generateImageUrl({ width: 1000, height: 800 }),
            full: generateImageUrl({ width: 1500, height: 1000 }),
            cosmic: generateImageUrl({ width: 100, height: 80 }),
        })),
    },
} as ISiteCard;
