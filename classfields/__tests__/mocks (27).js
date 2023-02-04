export const getOffers = () => [
    {
        offerType: 'SELL',
        offerCategory: 'APARTMENT',
        offerId: '1',
        price: {
            currency: 'RUR',
            period: 'WHOLE_LIFE',
            value: 55555,
            valueForWhole: 55555
        },
        area: {
            value: 100,
            unit: 'SQUARE_METER'
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
                latitude: 55.764797,
                longitude: 37.63869,
                minTimeToMetro: 7,
                lineColors: [ 'e4402d' ],
                rgbColor: 'e4402d'
            }
        }
    },
    {
        offerType: 'SELL',
        offerCategory: 'APARTMENT',
        offerId: '2',
        price: {
            currency: 'RUR',
            period: 'WHOLE_LIFE',
            value: 122999222,
            valueForWhole: 122999222
        },
        area: {
            value: 84,
            unit: 'SQUARE_METER'
        },
        roomsTotal: 1,
        location: {
            address: 'Москва, Тестовый бульвар без метро, 1',
            rgid: 193389
        }
    }
];

export const initialState = {
    user: {
        favorites: [ 2 ],
        favoritesMap: {
            2: true
        }
    },
    page: {
        name: 'serp'
    }
};
