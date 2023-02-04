import { generateImageAliases } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

const defaultOffer = {
    type: 'SELL',
    offerId: '1',
    url: 'http://example.ru/123',
    offerCategory: 'APARTMENT',
    floorsOffered: [ 1 ],
    floorsTotal: 9,
    price: {
        value: 30000000,
        currency: 'RUR'
    },
    area: {
        unit: 'SQUARE_METER',
        value: 55
    },
    roomsTotal: 3
};

const room = {
    ...defaultOffer,
    offerCategory: 'ROOMS',
    roomsTotal: 5,
    roomsOffered: 2,
    roomSpace: [
        {
            value: 10
        },
        {
            value: 12
        }
    ]
};

const house = {
    ...defaultOffer,
    offerId: '2',
    offerCategory: 'HOUSE',
    price: {
        value: 80000000,
        currency: 'RUR'
    },
    floorsOffered: undefined,
    lot: {
        lotArea: {
            value: 3,
            unit: 'ARE'
        }
    }
};

const lot = {
    ...house,
    floorsOffered: undefined,
    floorsTotal: undefined,
    roomsTotal: undefined,
    area: undefined,
    offerCategory: 'LOT'
};

const commercialLand = {
    ...defaultOffer,
    roomsTotal: undefined,
    area: {
        value: 3,
        unit: 'HECTARE'
    },
    offerCategory: 'COMMERCIAL',
    commercial: {
        commercialTypes: [ 'LAND' ]
    }
};

const commercialBusiness = {
    ...defaultOffer,
    roomsTotal: undefined,
    area: {
        value: 300,
        unit: 'SQUARE_METER'
    },
    offerCategory: 'COMMERCIAL',
    commercial: {
        commercialTypes: [ 'BUSINESS' ]
    }
};

const garage = {
    ...defaultOffer,
    offerCategory: 'GARAGE',
    floorsOffered: undefined,
    floorsTotal: undefined,
    roomsTotal: undefined,
    garage: {
        garageType: 'PARKING_PLACE'
    }
};

const rentApartment = {
    ...defaultOffer,
    offerType: 'RENT',
    price: {
        value: 30000,
        currency: 'RUR',
        period: 'PER_MONTH'
    }
};

const rentRoom = {
    ...defaultOffer,
    offerType: 'RENT',
    price: {
        value: 2500,
        currency: 'RUR',
        period: 'PER_DAY'
    }
};

const withoutFloors = {
    ...defaultOffer,
    floorsOffered: undefined,
    floorsTotal: undefined
};

const withoutArea = {
    ...defaultOffer,
    area: undefined
};

const withoutRooms = {
    ...defaultOffer,
    roomsTotal: undefined
};

const withImage = {
    ...defaultOffer,
    appMiniSnippetImages: [ '//avatars.mdst.yandex.net/get-realty/2957/add.15868778558788cee57eca4/app_snippet_mini' ]
};

export default {
    apartment: defaultOffer,
    room,
    house,
    lot,
    commercialLand,
    commercialBusiness,
    garage,
    rentApartment,
    rentRoom,
    manyOffers: house,
    manyOffersRoom: room,
    manyOffersCommercialLand: commercialLand,
    withoutFloors,
    withoutArea,
    withoutRooms,
    withImage
};

const images = generateImageAliases({ width: 200, height: 86 });

export const defaultSite = {
    id: 85411,
    price: {
        from: 4913200,
        to: 10485395,
        currency: 'RUR',
        minPricePerMeter: 127619,
        maxPricePerMeter: 180000,
        rooms: {
            1: {
                soldout: false,
                from: 5497800,
                to: 6176000,
                currency: 'RUR',
                areas: {
                    from: '35.7',
                    to: '40.7'
                },
                hasOffers: false,
                priceRatioToMarket: 0,
                status: 'ON_SALE'
            },
            2: {
                soldout: false,
                from: 6912000,
                to: 9191160,
                currency: 'RUR',
                areas: {
                    from: '49.3',
                    to: '63.3'
                },
                hasOffers: false,
                priceRatioToMarket: 0,
                status: 'ON_SALE'
            },
            3: {
                soldout: false,
                from: 7848600,
                to: 10485395,
                currency: 'RUR',
                areas: {
                    from: '61.5',
                    to: '85.7'
                },
                hasOffers: false,
                priceRatioToMarket: 0,
                status: 'ON_SALE'
            },
            OPEN_PLAN: {
                soldout: false,
                currency: 'RUR',
                hasOffers: false,
                priceRatioToMarket: 0
            },
            STUDIO: {
                soldout: false,
                from: 4913200,
                to: 5274000,
                currency: 'RUR',
                areas: {
                    from: '25.7',
                    to: '29.3'
                },
                hasOffers: false,
                priceRatioToMarket: 0,
                status: 'ON_SALE'
            },
            PLUS_4: {
                soldout: true,
                from: 9452000,
                to: 9452000,
                currency: 'RUR',
                areas: {
                    from: '94.5',
                    to: '94.5'
                },
                hasOffers: false,
                priceRatioToMarket: 0,
                status: 'NOT_ON_SALE'
            }
        },
        totalOffers: 0,
        priceRatioToMarket: 0
    },
    buildingFeatures: {
        state: 'UNFINISHED',
        finishedApartments: true,
        class: 'COMFORT',
        zhkType: 'UNKNOWN',
        totalFloors: 17,
        minTotalFloors: 12,
        totalApartments: 9360,
        isApartment: false,
        apartmentType: 'FLATS',
        ceilingHeight: 264,
        security: true,
        parking: {
            type: 'INDOOR',
            parkingSpaces: 569,
            available: true
        },
        parkings: [
            {
                type: 'OPEN',
                available: true
            },
            {
                type: 'CLOSED',
                parkingSpaces: 569,
                available: true
            },
            {
                type: 'UNDERGROUND',
                parkingSpaces: 519,
                available: true
            }
        ],
        wallTypes: []
    },
    location: {
        rgid: 197919
    },
    fullName: 'новые Ватутинки мкр. «Центральный»',
    appSmallSnippetImages: [
        images.appSnippetSmall
    ],
    appMiddleImages: [
        images.appSnippetMiddle
    ]
};

const emptyRoomsInfo = {
    soldout: false,
    currency: 'RUR',
    hasOffers: false,
    priceRatioToMarket: 0
};

export const siteWithoutRoomsInfo = {
    ...defaultSite,
    price: {
        ...defaultSite.price,
        rooms: {
            1: emptyRoomsInfo,
            2: emptyRoomsInfo,
            3: emptyRoomsInfo,
            PLUS_4: emptyRoomsInfo,
            STUDIO: emptyRoomsInfo
        }
    }
};

export const siteWithOneRoom = {
    ...defaultSite,
    price: {
        ...defaultSite.price,
        rooms: {
            1: emptyRoomsInfo,
            2: {
                soldout: false,
                from: 6912000,
                to: 9191160,
                currency: 'RUR',
                areas: {
                    from: '49.3',
                    to: '63.3'
                },
                hasOffers: false,
                priceRatioToMarket: 0,
                status: 'ON_SALE'
            },
            3: emptyRoomsInfo,
            PLUS_4: emptyRoomsInfo,
            STUDIO: emptyRoomsInfo
        }
    }
};
