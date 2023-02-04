import merge from 'lodash/merge';
import cloneDeep from 'lodash/cloneDeep';

function getFavoritesFromPoints(points) {
    const { site, offer, village } = points;

    const favoritesMap = [ ...site, ...offer, ...village ].reduce((acc, item) => {
        if (item.type === 'offer') {
            item.favoriteOfferIds.forEach(offerId => acc[offerId] = true);
        } else {
            acc[`${item.type}_${item.id}`] = true;
        }

        return acc;
    }, {});

    return { favoritesMap, favorites: Object.keys(favoritesMap) };
}

const commonProps = {
    points: { site: [], offer: [], village: [] },
    snippets: [],
    geo: {},
    queryId: '',
    specialProject: {
        developerFullName: 'Группа компаний ПИК',
        developerId: 52308,
        developerName: 'ПИК',
        filterText: 'Только ЖК от Группы ПИК',
        hideAds: true,
        showFilter: true,
        showPin: true,
        showTab: true,
        sideMenuText: 'Квартиры от Группы ПИК',
        tabUrl: '/pik/?from=main_menu'
    },
    user: {
        isVosUser: false,
        isAuth: true,
        isJuridical: false
    },
    favoritesMap: {},
    isSnippetLoading: false,
    disableStats: true,
    loadSiteSnippets() {},
    loadVillageSnippets() {},
    loadOffersSnippets() {},
    clearActivePoint() {}
};

const differentPoints = {
    site: [
        {
            constructionState: 'UNDER_CONSTRUCTIONS_WITH_HAND_OVER',
            developerId: 52308,
            id: 375274,
            lat: 55.61857,
            lon: 37.41301,
            type: 'site'
        },
        {
            constructionState: 'UNDER_CONSTRUCTION',
            developerId: 34234,
            id: 375275,
            lat: 55.78462,
            lon: 37.8503,
            type: 'site'
        },
        {
            constructionState: 'SUSPENDED',
            developerId: 54323,
            id: 375276,
            lat: 55.532146,
            lon: 37.61691,
            type: 'site'
        },
        {
            constructionState: 'HAND_OVER',
            developerId: 43325,
            id: 375277,
            lat: 55.739605,
            lon: 38.031696,
            type: 'site'
        }
    ],
    offer: [
        {
            id: '3980990222391809793',
            favoriteOfferIds: [ '3980990222391809793' ],
            lat: 55.667294,
            lon: 37.56969,
            type: 'offer',
            price: 11000000
        },
        {
            id: '7542129395679726080',
            favoriteOfferIds: [ '7542129395679726080' ],
            lat: 55.770267,
            lon: 37.682198,
            type: 'offer',
            price: 12345678
        },
        {
            id: '8224805834868447',
            favoriteOfferIds: [ '8224805834868447', '8224805834868448', '8224805834868449', '8224805834868440' ],
            lat: 55.833607,
            lon: 37.50876,
            type: 'offer',
            price: 8000000
        }
    ],
    village: [
        { id: '1832934', lat: 55.553368, lon: 37.88704, type: 'village' },
        { id: '1783636', lat: 55.568928, lon: 38.300007, type: 'village' }
    ]
};

const farPoints = {
    site: [
        {
            id: 710517,
            lat: 60.056908,
            lon: 30.267998,
            type: 'site',
            developerId: 52308,
            constructionState: 'UNDER_CONSTRUCTIONS_WITH_HAND_OVER'
        }
    ],
    offer: [
        {
            id: '7542129395679726080',
            favoriteOfferIds: [ '7542129395679726080' ],
            lat: 55.770267,
            lon: 37.682198,
            type: 'offer',
            price: 12345678
        }
    ],
    village: []
};

const makeImage = title => `site.com/image${title}.png`;
const images = [ makeImage(1), makeImage(2), makeImage(3) ];

const siteSnippet = {
    id: 375275,
    name: 'Остафьево',
    fullName: 'ЖК «Остафьево»',
    locativeFullName: 'в ЖК «Остафьево»',
    location: {
        geoId: 121735,
        rgid: 17378342,
        settlementRgid: 17378342,
        settlementGeoId: 121735,
        address: 'Москва, пос. Рязановское, с. Остафьево, ЖК Остафьево',
        distanceFromRingRoad: 13126,
        subjectFederationId: 1,
        subjectFederationRgid: 741964,
        subjectFederationName: 'Москва и МО',
        point: { latitude: 55.78462, longitude: 37.8503, precision: 'EXACT' },
        expectedMetroList: [], schools: [], parks: [], ponds: [],
        airports: [],
        cityCenter: [],
        heatmaps: [],
        allHeatmaps: [],
        insideMKAD: false,
        routeDistances: [],
        metro: {
            lineColors: [ 'df477c' ],
            metroGeoId: 218571,
            rgbColor: 'df477c',
            metroTransport: 'ON_TRANSPORT',
            name: 'Щербинка',
            timeToMetro: 17
        },
        metroList: [
            {
                lineColors: [ 'df477c' ],
                metroGeoId: 218571,
                rgbColor: 'df477c',
                metroTransport: 'ON_TRANSPORT',
                name: 'Щербинка',
                timeToMetro: 17
            }
        ]
    },
    viewTypes: [ 'GENERAL', 'COURTYARD', 'HALL' ],
    images,
    appLargeImages: images,
    appLargeSnippetImages: images,
    minicardImages: images,
    siteSpecialProposals: [],
    buildingClass: 'COMFORT',
    state: 'UNFINISHED',
    finishedApartments: false,
    price: {
        from: 3345836,
        to: 10141968,
        currency: 'RUR',
        minPricePerMeter: 110606,
        maxPricePerMeter: 166576,
        rooms: {
            1: {
                soldout: false, from: 4114030, to: 7132266, currency: 'RUR', areas: { from: '30.9', to: '57.1' },
                hasOffers: false, priceRatioToMarket: 0, status: 'ON_SALE'
            },
            2: {
                soldout: false, from: 5063526, to: 8232732, currency: 'RUR', areas: { from: '45.1', to: '69.8' },
                hasOffers: false, priceRatioToMarket: 0, status: 'ON_SALE'
            },
            3: {
                soldout: false, from: 7966248, to: 10059506, currency: 'RUR', areas: { from: '68.1', to: '79.6' },
                hasOffers: false, priceRatioToMarket: 0, status: 'ON_SALE'
            },
            OPEN_PLAN: { soldout: false, currency: 'RUR', hasOffers: false, priceRatioToMarket: 0 },
            STUDIO: {
                soldout: false, from: 3345836, to: 5073913, currency: 'RUR', areas: { from: '22.9', to: '30.5' },
                hasOffers: false, priceRatioToMarket: 0, status: 'ON_SALE'
            },
            PLUS_4: {
                soldout: false, from: 9830661, to: 10141968, currency: 'RUR', areas: { from: '88.9', to: '91.1' },
                hasOffers: false, priceRatioToMarket: 0, status: 'ON_SALE'
            }
        },
        totalOffers: 0,
        priceRatioToMarket: 0
    },
    flatStatus: 'ON_SALE',
    developers: [
        {
            id: 34234,
            name: 'Группа «Самолет»',
            url: 'http://samoletgroup.ru/',
            logo: images[0],
            objects: { all: 20, salesOpened: 14, finished: 11, unfinished: 9, suspended: 0 },
            address: 'Москва, ул. Ивана Франко, 8',
            born: '2011-12-31T20:00:00Z',
            encryptedPhones: [ { phoneWithMask: '+7 495 182 ×× ××', phoneHash: 'KzcF0OHTUJxOLDIN0MPDAR0' } ]
        }
    ],
    salesDepartment: {
        id: 1554912,
        name: 'Самолет Девелопмент',
        weekTimetable: [ { dayFrom: 1, dayTo: 7, timePattern: [ { open: '09:00', close: '21:00' } ] } ],
        logo: images[0],
        phonesWithTag: [ { tag: '', phone: '+74953239719' } ],
        statParams: 'params',
        encryptedPhones: [ { phoneWithMask: '+7 495 323 ×× ××', phoneHash: 'KzcF0OHTUJzMLjMN5NPzER5' } ],
        encryptedDump: 'dump'
    },
    phone: { phoneWithMask: '+7 495 323 ×× ××', phoneHash: 'cash' },
    backCallTrafficInfo: {},
    withBilling: true,
    awards: {}
};

const villageSnippet = {
    id: '1832934',
    name: 'Орловъ',
    fullName: 'Коттеджный посёлок «Орловъ»',
    deliveryDates: [
        { phaseName: '1 очередь', phaseIndex: 1, status: 'HAND_OVER', year: 2014, quarter: 2, finished: true },
        { phaseName: '2 очередь', phaseIndex: 2, status: 'HAND_OVER', year: 2018, quarter: 3, finished: true },
        { phaseName: '3 очередь', phaseIndex: 3, status: 'HAND_OVER', year: 2019, quarter: 4, finished: true }
    ],
    location: {
        geocoderAddress: 'Московская область, Ленинский район, деревня Орлово, жилой комплекс Орлов',
        polygon: {
            latitudes: [ 55.548355, 55.547764, 55.549603, 55.552094, 55.553085, 55.55421, 55.554417, 55.555695,
                55.55605, 55.556767, 55.557167, 55.55759, 55.55359, 55.550964 ],
            longitudes: [ 37.88548, 37.88448, 37.882183, 37.880684, 37.8847, 37.884254, 37.88581, 37.88658,
                37.887764, 37.888084, 37.889416, 37.89375, 37.890423, 37.885853 ]
        },
        rgid: '324708',
        geoId: 1,
        point: { latitude: 55.553368, longitude: 37.88704 },
        address: 'Ленинский район, д. Орлово, Володарское ш., 12 км.',
        subjectFederationId: 1,
        subjectFederationRgid: '741964',
        routeDistances: [
            { geoPoint: { latitude: 55.591656, longitude: 37.729748, defined: true }, distance: 14317 }
        ],
        insideMKAD: false,
        subjectFederationName: 'Москва и МО'
    },
    images: Array(5).fill(undefined).map((item, index) => ({
        photoType: 'COMMON',
        image: {
            full: makeImage(index + 2),
            cosmic: makeImage(index + 2),
            mini: makeImage(index + 2),
            appMiddle: makeImage(index + 2),
            appLarge: makeImage(index + 2),
            appMiniSnippet: makeImage(index + 2),
            appSmallSnippet: makeImage(index + 2),
            appMiddleSnippet: makeImage(index + 2),
            appLargeSnippet: makeImage(index + 2)
        }
    })),
    mainPhoto: {
        full: images[0],
        cosmic: images[0],
        mini: images[0],
        appMiddle: images[0],
        appLarge: images[0],
        appMiniSnippet: images[0],
        appSmallSnippet: images[0],
        appMiddleSnippet: images[0],
        appLargeSnippet: images[0]
    },
    villageFeatures: {
        villageClass: 'COMFORT',
        totalObjects: 257,
        soldObjects: 198,
        infrastructure: [
            { type: 'GUEST_PARKING' }, { type: 'PLAYGROUND' }, { type: 'ROADS' }, { type: 'FOREST' },
            { type: 'MARKET' }, { type: 'REST', description: '1' }, { type: 'FENCE' }, { type: 'LIGHTING' },
            { type: 'PARK', description: '1' }, { type: 'MAINTENANCE' }
        ]
    },
    offerStats: {
        entries: [],
        primaryPrices: { currency: 'RUB', from: '5797000', to: '28541130' }
    },
    filteredOfferStats: {
        offerTypes: [ 'TOWNHOUSE', 'COTTAGE' ],
        primaryPrice: { currency: 'RUB', from: '5797000', to: '28541130' }
    },
    salesDepartment: {
        id: 1884787,
        name: 'ГК «Астерра»',
        weekTimetable: [ { dayFrom: 1, dayTo: 7, timePattern: [ { open: '10:00', close: '19:00' } ] } ],
        logo: images[0],
        statParams: 'params',
        encryptedDump: 'dump'
    },
    developers: [ {
        id: '198558',
        name: 'Астерра',
        legalName: 'ООО «Пехра-Покровское»',
        url: 'http://www.asterra.ru/',
        logo: images[0],
        objects: { all: 12, finished: 10, unfinished: 2, suspended: 0 },
        address: 'Московская обл., Балашиха, мкр.1 мая, 25',
        encryptedPhone: { phoneWithMask: '+7 495 642 ×× ××', phoneHash: 'KzcFgKHDQJ5NLSkNgNPjQRyLTTYVwLXTYZw' }
    } ],
    phone: { phoneWithMask: '+7 495 642 ×× ××', phoneHash: 'KzcFgKHDQJ5NLSkNgNPjQRyLTTYVwLXTYZw' },
    withBilling: true
};

const offerSnippet = {
    appMiddleImages: [ images[0] ],
    appLargeImages: [ images[0] ],
    area: { value: 55, unit: 'SQUARE_METER' },
    author: {
        id: '0',
        category: 'AGENCY',
        agentName: 'SOL',
        creationDate: '2020-06-22T20:03:33Z',
        profile: {
            userType: 'AGENCY',
            name: 'SOL',
            logo: {
                origin: images[0],
                minicard: images[0],
                main: images[0],
                alike: images[0],
                large: images[0],
                cosmic: images[0],
                appLarge: images[0],
                appSnippetMini: images[0],
                appSnippetSmall: images[0],
                appSnippetMiddle: images[0],
                appSnippetLarge: images[0],
                optimize: images[0]
            }
        },
        redirectPhones: true,
        redirectPhonesFailed: false,
        encryptedPhoneNumbers: [ { phone: 'phone', redirectId: '+79214428598' } ],
        encryptedPhones: [ 'phone' ]
    },
    building: { buildingType: 'MONOLIT', parkingType: 'OPEN', heatingType: 'UNKNOWN' },
    creationDate: '2020-06-26T12:53:28Z',
    flatType: 'SECONDARY',
    floorsOffered: [ 11 ],
    floorsTotal: 12,
    fullImages: [ images[0] ],
    house: { studio: false, apartments: false, housePart: false },
    livingSpace: { value: 45, unit: 'SQUARE_METER' },
    location: {
        rgid: 193389,
        geoId: 213,
        subjectFederationId: 1,
        settlementRgid: 165705,
        settlementGeoId: 213,
        address: 'Москва, Посланников переулок, 5соор1',
        geocoderAddress: 'Россия, Москва, Посланников переулок, 5соор1',
        structuredAddress: {},
        point: { latitude: 55.770267, longitude: 37.682198, precision: 'EXACT' },
        metro: {
            metroGeoId: 20478,
            name: 'Бауманская',
            metroTransport: 'ON_FOOT',
            timeToMetro: 5,
            latitude: 55.772408,
            longitude: 37.679043,
            minTimeToMetro: 4,
            lineColors: [ '0042a5' ],
            rgbColor: '0042a5'
        },
        streetAddress: 'Посланников переулок, 5соор1',
        metroList: [ {
            metroGeoId: 20478,
            name: 'Бауманская',
            metroTransport: 'ON_FOOT',
            timeToMetro: 5,
            latitude: 55.772408,
            longitude: 37.679043,
            minTimeToMetro: 4,
            lineColors: [ '0042a5' ],
            rgbColor: '0042a5'
        } ],
        heatmaps: [],
        allHeatmaps: [],
        subjectFederationName: 'Москва и МО'
    },
    minicardImages: [ images[0] ],
    newBuilding: false,
    obsolete: false,
    offerId: '7542129395679726080',
    offerCategory: 'APARTMENT',
    offerType: 'SELL',
    openPlan: false,
    predictions: {
        predictedPrice: { min: '13791000', max: '16855000', value: '15323000' },
        predictedPriceAdvice: { summary: 'LOW', predictionDiff: 2977322 }
    },
    price: {
        currency: 'RUR',
        value: 12345678,
        period: 'WHOLE_LIFE',
        unit: 'WHOLE_OFFER',
        trend: 'UNCHANGED',
        hasPriceHistory: false,
        valuePerPart: 224467,
        unitPerPart: 'SQUARE_METER',
        valueForWhole: 12345678,
        unitForWhole: 'WHOLE_OFFER',
        price: { value: 12345678, currency: 'RUB', priceType: 'PER_OFFER', pricingPeriod: 'WHOLE_LIFE' },
        pricePerPart: { value: 224467, currency: 'RUB', priceType: 'PER_METER', pricingPeriod: 'WHOLE_LIFE' },
        priceForWhole: { value: 12345678, currency: 'RUB', priceType: 'PER_OFFER', pricingPeriod: 'WHOLE_LIFE' }
    },
    primarySaleV2: false,
    remoteReview: { onlineShow: false, youtubeVideoReviewUrl: '' },
    roomsTotal: 1,
    totalImages: 1,
    tuzInfo: {
        campaignId: 'd57f26ca-3590-406b-8a69-14fef0b9c570',
        active: true,
        tuzParams: [
            { key: 'tuzParamRgid', value: '741964' },
            { key: 'tuzParamType', value: 'SELL' },
            { key: 'tuzParamCategory', value: 'APARTMENT' },
            { key: 'tuzParamPartner', value: '1035218734' },
            { key: 'tuzParamUid', value: '4045447503' },
            { key: 'tuzParamClass', value: 'BUSINESS' }
        ],
        tuzFeatured: true,
        premium: true,
        promotion: true,
        raising: true,
        clientId: 135109318,
        tuzType: { maximum: {} }
    },
    updateDate: '2020-07-27T09:10:00Z',
    vas: { raised: true, premium: true, placement: false, promoted: true, turboSale: false, raisingSale: false,
        tuzFeatured: true, vasAvailable: true },
    withExcerpt: false
};

const createOfferSnippet = diff => merge(cloneDeep(offerSnippet), {
    flatType: 'NEW_FLAT',
    building: { builtYear: 2020, builtQuarter: 3, buildingState: 'UNFINISHED', parkingType: 'OPEN',
        siteId: 2131550, siteName: 'Narva Loft', siteDisplayName: 'МФК «Narva Loft»',
        houseId: '2131592', heatingType: 'UNKNOWN'
    },
    floorsTotal: 8,
    location: {
        metro: {
            metroGeoId: 20370, name: 'Водный Стадион', metroTransport: 'ON_FOOT', timeToMetro: 21,
            latitude: 55.839886, longitude: 37.48678, minTimeToMetro: 13, lineColors: [ '4f8242' ],
            rgbColor: '4f8242'
        },
        metroList: [ {
            metroGeoId: 20370, name: 'Водный Стадион', metroTransport: 'ON_FOOT', timeToMetro: 21,
            latitude: 55.839886, longitude: 37.48678, minTimeToMetro: 13, lineColors: [ '4f8242' ],
            rgbColor: '4f8242'
        } ]
    },
    predictions: {
        predictedPriceAdvice: { summary: 'HIGH' }
    },
    newBuilding: true,
    ...diff
});

export const differentPointsProps = {
    ...commonProps,
    points: differentPoints,
    ...getFavoritesFromPoints(differentPoints)
};

export const farPointsProps = {
    ...commonProps,
    points: farPoints,
    ...getFavoritesFromPoints(farPoints)
};

export const siteSnippetProps = {
    ...differentPointsProps,
    activePoint: {
        constructionState: 'UNDER_CONSTRUCTION',
        developerId: 34234,
        id: 375275,
        lat: 55.78462,
        lon: 37.8503,
        type: 'site',
        geoId: 'site-point-55.78462,37.8503'
    },
    snippets: [ siteSnippet ]
};

export const villageSnippetProps = {
    ...differentPointsProps,
    activePoint: {
        id: '1832934',
        lat: 55.553368,
        lon: 37.88704,
        type: 'village',
        geoId: 'village-point-55.553368,37.88704'
    },
    snippets: [ villageSnippet ]
};

export const offerSnippetProps = {
    ...differentPointsProps,
    activePoint: {
        id: '7542129395679726080',
        favoriteOfferIds: [ '7542129395679726080' ],
        lat: 55.770267,
        lon: 37.682198,
        type: 'offer',
        price: 12345678,
        count: 1,
        geoId: 'offer-point-55.770267,37.682198'
    },
    snippets: [ offerSnippet ]
};

export const offerSeveralSnippetsProps = {
    ...differentPointsProps,
    activePoint: {
        id: '8224807745834868447',
        favoriteOfferIds: [ '8224805834868447', '8224805834868448', '8224805834868449', '8224805834868440' ],
        lat: 55.833607,
        lon: 37.50876,
        type: 'offer',
        price: 8000000,
        count: 1,
        geoId: 'offer-point-55.833607,37.50876'
    },
    snippets: [
        createOfferSnippet({
            area: { value: 52.96, unit: 'SQUARE_METER' },
            floorsOffered: [ 4 ],
            roomsTotal: 1,
            offerId: '8224805834868447',
            price: {
                value: 8000000,
                valuePerPart: 151057
            }
        }),
        createOfferSnippet({
            area: { value: 86.3, unit: 'SQUARE_METER' },
            floorsOffered: [ 7 ],
            roomsTotal: 3,
            offerId: '8224805834868448',
            price: {
                value: 13550000,
                valuePerPart: 157010
            }
        }),
        createOfferSnippet({
            area: { value: 65.5, unit: 'SQUARE_METER' },
            floorsOffered: [ 1 ],
            roomsTotal: 2,
            offerId: '8224805834868449',
            price: {
                value: 10234900,
                valuePerPart: 156258
            }
        }),
        createOfferSnippet({
            area: { value: 54, unit: 'SQUARE_METER' },
            floorsOffered: [ 3 ],
            roomsTotal: 1,
            offerId: '8224805834868440',
            price: {
                value: 8050000,
                valuePerPart: 152033
            }
        })
    ]
};

