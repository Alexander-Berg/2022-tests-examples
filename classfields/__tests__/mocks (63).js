export const getOffer = () => ({
    offerType: 'SELL',
    offerCategory: 'APARTMENT',
    offerId: '1',
    floorsTotal: 4,
    floorsOffered: [ 3 ],
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
});

export const getEmptyOffer = () => {
    const offer = getOffer();

    return {
        ...offer,
        floorsOffered: [],
        floorsTotal: undefined,
        location: {
            ...offer.location,
            metro: undefined
        }
    };
};

export const getInitialState = () => ({
    user: {
        favorites: [ 1 ],
        favoritesMap: {
            1: true
        }
    }
});
