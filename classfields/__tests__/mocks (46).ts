import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { IOfferSnippet } from 'realty-core/types/offerSnippet';
import { AreaUnits } from 'realty-core/types/offerCard';

export const getOffers = () =>
    ([
        {
            uid: '',
            offerType: 'SELL',
            offerCategory: 'APARTMENT',
            offerId: '1',
            active: true,
            price: {
                currency: 'RUR',
                period: 'WHOLE_LIFE',
                value: 55555,
                valueForWhole: 55555,
                unit: 'WHOLE_OFFER',
                trend: 'UNCHANGED',
                hasPriceHistory: false,
                valuePerPart: 55555,
                unitPerPart: AreaUnits.SQUARE_METER,
                unitForWhole: 'WHOLE_OFFER',
                price: { value: 55555, currency: 'RUB', priceType: 'PER_OFFER', pricingPeriod: 'WHOLE_LIFE' },
                pricePerPart: { value: 55555, currency: 'RUB', priceType: 'PER_METER', pricingPeriod: 'WHOLE_LIFE' },
                priceForWhole: { value: 55555, currency: 'RUB', priceType: 'PER_OFFER', pricingPeriod: 'WHOLE_LIFE' },
            },
            area: {
                value: 100,
                unit: 'SQUARE_METER',
            },
            roomsTotal: 1,
            location: {
                address: 'Москва, Чистопрудный бульвар, 11с4',
                rgid: 193389,
                metro: {
                    metroGeoId: 20486,
                    name: 'Чистые Пруды',
                    metroTransport: 'ON_FOOT',
                    timeToMetro: 7,
                    minTimeToMetro: 7,
                    lineColors: ['e4402d'],
                    rgbColor: 'e4402d',
                    latitude: 123123123,
                    longitude: 123123123,
                },
            },
            appMiddleImages: Array(3).fill(generateImageUrl({ width: 900, height: 500 })),
            appLargeImages: Array(3).fill(generateImageUrl({ width: 900, height: 500 })),
            fullImages: Array(3).fill(generateImageUrl({ width: 900, height: 500 })),
            photoPreviews: [],
        },
        {
            uid: '',
            offerType: 'SELL',
            offerCategory: 'APARTMENT',
            offerId: '2',
            active: true,
            price: {
                currency: 'RUR',
                period: 'WHOLE_LIFE',
                value: 122999222,
                valueForWhole: 122999222,
                unit: 'WHOLE_OFFER',
                trend: 'UNCHANGED',
                hasPriceHistory: false,
                valuePerPart: 122999222,
                unitPerPart: AreaUnits.SQUARE_METER,
                unitForWhole: 'WHOLE_OFFER',
                price: { value: 122999222, currency: 'RUB', priceType: 'PER_OFFER', pricingPeriod: 'WHOLE_LIFE' },
                pricePerPart: {
                    value: 122999222,
                    currency: 'RUB',
                    priceType: 'PER_METER',
                    pricingPeriod: 'WHOLE_LIFE',
                },
                priceForWhole: {
                    value: 122999222,
                    currency: 'RUB',
                    priceType: 'PER_OFFER',
                    pricingPeriod: 'WHOLE_LIFE',
                },
            },
            area: {
                value: 84,
                unit: 'SQUARE_METER',
            },
            roomsTotal: 1,
            location: {
                address: 'Москва, Тестовый бульвар без метро, 1',
                rgid: 193389,
            },
            appMiddleImages: Array(3).fill(generateImageUrl({ width: 900, height: 500 })),
            appLargeImages: Array(3).fill(generateImageUrl({ width: 900, height: 500 })),
            fullImages: Array(3).fill(generateImageUrl({ width: 900, height: 500 })),
            photoPreviews: [],
        },
    ] as unknown) as IOfferSnippet[];

export const initialState = {
    user: {
        favorites: [2],
        favoritesMap: {
            2: true,
        },
    },
    page: {
        name: 'serp',
    },
};
