import { DeepPartial } from 'utility-types';

import { ISiteCardBaseType } from 'realty-core/types/siteCard';
import { OfferCategory, OfferType } from 'realty-core/types/offerCard';

import { IMicrodataCoreStore } from '../types';

export const MOCK_REVIEWS = {
    entries: [
        {
            type: 'YANDEX',
            id: 'VpYjOhftOLsLra1HYfhziyxVlogo5G',
            text: 'Мне прям очень понравилось',
            rating: 5,
            isAnonymous: false,
            updatedTime: '2020-08-04T15:14:35.498Z',
            author: {
                name: 'Vasya Pupkin',
                avatarUrl: '',
            },
            orgId: '84620679165',
        },
        {
            type: 'YANDEX',
            id: 'VpYjOhftOLsLra1HYfhziyxVlogo54',
            text: 'Очень плохой отзыв!',
            rating: 1,
            isAnonymous: false,
            updatedTime: '2020-04-04T15:14:35.498Z',
            author: {
                name: 'Vasya Ivanov',
                avatarUrl: '',
            },
            orgId: '84620679163',
        },
    ],
    myReview: {
        type: 'YANDEX',
        id: '',
        text: '',
        rating: 0,
        isAnonymous: false,
        orgId: '',
    },
    pager: {
        rating: {
            Value: 4.3,
            Count: 602,
        },
        page: 1,
        totalPages: 1,
        count: 1,
    },
};

export const MOCK_DEFAULT_SITE_CARD: DeepPartial<ISiteCardBaseType> = {
    fullName: 'ЖК «Октябрьское поле»',
    salesDepartment: {
        name: 'РГ-Девелопмент',
    },
    description: 'Проект, общей площадью 188 тыс. кв. м.',
    images: {
        list: [
            {
                full: '//avatars.mds.yandex.net/get-verba/1672712/2a000001730e6b37eda3c893c05ac71a5854/realty_large',
            },
            {
                full: '//avatars.mds.yandex.net/get-verba/216201/2a000001730e6b80f572e2dc76c1cbea7b05/realty_large',
            },
        ],
    },
    id: 189856,
    price: {
        from: 9324250,
        to: 39270000,
        totalOffers: 12,
    },
};

export const MOCK_ZERO_OFFERS_SITE_CARD: DeepPartial<ISiteCardBaseType> = {
    fullName: 'ЖК «Октябрьское поле»',
    salesDepartment: {
        name: 'РГ-Девелопмент',
    },
    description: 'Проект, общей площадью 188 тыс. кв. м.',
    images: {
        list: [
            {
                full: '//avatars.mds.yandex.net/get-verba/1672712/2a000001730e6b37eda3c893c05ac71a5854/realty_large',
            },
            {
                full: '//avatars.mds.yandex.net/get-verba/216201/2a000001730e6b80f572e2dc76c1cbea7b05/realty_large',
            },
        ],
    },
    id: 189856,
    price: {
        from: 9324250,
        to: 39270000,
        totalOffers: 0,
    },
};

export const MOCK_STORE_NEWBUILDING: DeepPartial<IMicrodataCoreStore> = {
    page: {
        name: 'newbuilding',
    },
    cards: {
        sites: MOCK_DEFAULT_SITE_CARD,
    },
    similar: {
        sites: [
            MOCK_DEFAULT_SITE_CARD,
            { ...MOCK_DEFAULT_SITE_CARD, fullName: 'ЖК «Крепкие работяги»', id: 34123 },
            { ...MOCK_DEFAULT_SITE_CARD, fullName: 'ЖК «Твиттерский»', id: 453235 },
        ],
    },
    reviews: MOCK_REVIEWS,
    geo: {
        type: 'SUBJECT_FEDERATION',
        rgid: 741964,
        name: 'Москва и МО',
        locative: 'в Москве и МО',
    },
};

export const MOCK_STORE_NEWBUILDING_ZERO_OFFERS: DeepPartial<IMicrodataCoreStore> = {
    page: {
        name: 'newbuilding',
    },
    cards: {
        sites: MOCK_ZERO_OFFERS_SITE_CARD,
    },
    similar: {
        sites: [
            MOCK_DEFAULT_SITE_CARD,
            { ...MOCK_DEFAULT_SITE_CARD, fullName: 'ЖК «Крепкие работяги»', id: 34123 },
            { ...MOCK_DEFAULT_SITE_CARD, fullName: 'ЖК «Твиттерский»', id: 453235 },
        ],
    },
    reviews: MOCK_REVIEWS,
    geo: {
        type: 'SUBJECT_FEDERATION',
        rgid: 741964,
        name: 'Москва и МО',
        locative: 'в Москве и МО',
    },
};

export const MOCK_STORE_OFFER_CARD: DeepPartial<IMicrodataCoreStore> = {
    page: {
        name: 'offer',
    },
    offerCard: {
        card: {
            author: {},
            offerCategory: OfferCategory.APARTMENT,
            offerType: OfferType.SELL,
            url: '//avatars.mds.yandex.net/get-verba/1672712/2a000001730e6b37eda3c893c05ac71a5854/realty_large',
            offerId: '1241421512412',
            price: {
                currency: 'RUR',
                value: 2300000,
                period: 'WHOLE_LIFE',
                unit: 'WHOLE_OFFER',
                trend: 'DECREASED',
                previous: 8888888,
                hasPriceHistory: true,
                valuePerPart: 141414,
                valueForWhole: 2300000,
                unitForWhole: 'WHOLE_OFFER',
            },
            location: {
                rgid: 193279,
                geoId: 213,
                populatedRgid: 741964,
                subjectFederationId: 1,
                subjectFederationRgid: 741964,
                settlementRgid: 587795,
                settlementGeoId: 213,
                address: 'Москва, улица Верхняя Масловка, 25к1',
                geocoderAddress: 'Россия, Москва, улица Верхняя Масловка, 25к1',
            },
            active: true,
        },
    },
    geo: {
        type: 'SUBJECT_FEDERATION',
        rgid: 741964,
        name: 'Москва и МО',
        locative: 'в Москве и МО',
        isInMO: true,
    },
};

export const MOCK_STORE_OFFER_CARD_INACTIVE: DeepPartial<IMicrodataCoreStore> = {
    page: {
        name: 'offer',
    },
    offerCard: {
        card: {
            author: {},
            offerCategory: OfferCategory.APARTMENT,
            offerType: OfferType.SELL,
            url: '//avatars.mds.yandex.net/get-verba/1672712/2a000001730e6b37eda3c893c05ac71a5854/realty_large',
            offerId: '1241421512412',
            price: {
                currency: 'RUR',
                value: 2300000,
                period: 'WHOLE_LIFE',
                unit: 'WHOLE_OFFER',
                trend: 'DECREASED',
                previous: 8888888,
                hasPriceHistory: true,
                valuePerPart: 141414,
                valueForWhole: 2300000,
                unitForWhole: 'WHOLE_OFFER',
            },
            location: {
                rgid: 193279,
                geoId: 213,
                populatedRgid: 741964,
                subjectFederationId: 1,
                subjectFederationRgid: 741964,
                settlementRgid: 587795,
                settlementGeoId: 213,
                address: 'Москва, улица Верхняя Масловка, 25к1',
                geocoderAddress: 'Россия, Москва, улица Верхняя Масловка, 25к1',
            },
            active: false,
        },
    },
    geo: {
        type: 'SUBJECT_FEDERATION',
        rgid: 741964,
        name: 'Москва и МО',
        locative: 'в Москве и МО',
        isInMO: true,
    },
};

export const MOCK_SEARCH_STORE: DeepPartial<IMicrodataCoreStore> = {
    page: {
        name: 'search',
    },
    search: {
        offers: {
            pager: {
                totalItems: 4,
                minPriceTotalItems: 2300000,
                maxPriceTotalItems: 12000000,
            },
            entities: [
                {
                    price: {
                        currency: 'RUR',
                        value: 7777777,
                        period: 'WHOLE_LIFE',
                        unit: 'WHOLE_OFFER',
                        trend: 'DECREASED',
                        previous: 8888888,
                        hasPriceHistory: true,
                        valuePerPart: 141414,
                        valueForWhole: 7777777,
                        unitForWhole: 'WHOLE_OFFER',
                    },
                },
                {
                    price: {
                        currency: 'RUR',
                        value: 4200000,
                        period: 'WHOLE_LIFE',
                        unit: 'WHOLE_OFFER',
                        trend: 'DECREASED',
                        previous: 8888888,
                        hasPriceHistory: true,
                        valuePerPart: 141414,
                        valueForWhole: 4200000,
                        unitForWhole: 'WHOLE_OFFER',
                    },
                },
                {
                    price: {
                        currency: 'RUR',
                        value: 2300000,
                        period: 'WHOLE_LIFE',
                        unit: 'WHOLE_OFFER',
                        trend: 'DECREASED',
                        previous: 8888888,
                        hasPriceHistory: true,
                        valuePerPart: 141414,
                        valueForWhole: 2300000,
                        unitForWhole: 'WHOLE_OFFER',
                    },
                },
                {
                    price: {
                        currency: 'RUR',
                        value: 12000000,
                        period: 'WHOLE_LIFE',
                        unit: 'WHOLE_OFFER',
                        trend: 'DECREASED',
                        previous: 8888888,
                        hasPriceHistory: true,
                        valuePerPart: 141414,
                        valueForWhole: 12000000,
                        unitForWhole: 'WHOLE_OFFER',
                    },
                },
            ],
        },
    },
    geo: {
        type: 'SUBJECT_FEDERATION',
        rgid: 741964,
        name: 'Москва и МО',
        locative: 'в Москве и МО',
        isInMO: true,
    },
};

export const MOCK_STORE_WITHOUT_WEBSITE_RATING: DeepPartial<IMicrodataCoreStore> = {
    page: {
        name: 'developer',
    },
};

export const MOCK_USUAL_STORE: DeepPartial<IMicrodataCoreStore> = {
    page: {
        name: 'index',
    },
};

export const MOCK_MOSKVA_I_MO: DeepPartial<IMicrodataCoreStore> = {
    page: {
        name: 'index',
    },
    geo: {
        isInMO: true,
    },
};

export const MOCK_MOSKVA: DeepPartial<IMicrodataCoreStore> = {
    page: {
        name: 'index',
    },
    geo: {
        isMsk: true,
    },
};

export const MOCK_NOT_MOSKVA_OR_MOSKVA_I_MO: DeepPartial<IMicrodataCoreStore> = {
    page: {
        name: 'index',
    },
    geo: {
        isMsk: false,
        isInMO: false,
    },
};
