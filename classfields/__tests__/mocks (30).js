export const getInitialState = () => ({
    user: {
        favorites: [],
        favoritesMap: {}
    }
});

export const getCard = ({ isApartment = false } = {}) => ({
    buildingFeatures: {
        isApartment
    }
});

export const getRooms = () => [
    {
        roomType: 'STUDIO',
        priceFrom: '2616240',
        areaFrom: 19,
        areaTo: 19,
        flatPlansCount: 9,
        offersCount: 1
    },
    {
        roomType: 1,
        priceFrom: '3802400',
        areaFrom: 32.5,
        areaTo: 54,
        flatPlansCount: 15,
        offersCount: 52
    },
    {
        roomType: 2,
        priceFrom: '5472360',
        areaFrom: 49.2,
        areaTo: 71,
        flatPlansCount: 1,
        offersCount: 52
    },
    {
        roomType: 3,
        priceFrom: '7446250',
        areaFrom: 76.5,
        areaTo: 87.6,
        flatPlansCount: 2,
        offersCount: 20
    }
];

export const getPlans = ({ withoutPager = false, page = 1 } = {}) => ({
    items: [
        {
            roomType: 3,
            roomCount: 3,
            wholeArea: {
                value: 80.7,
                unit: 'SQ_M',
                name: 'whole'
            },
            livingArea: {
                value: 40.9,
                unit: 'SQ_M',
                name: 'living'
            },
            kitchenArea: {
                value: 20,
                unit: 'SQ_M',
                name: 'kitchen'
            },
            clusterId: '710517-49C24F945D0E8164',
            floors: [
                6,
                8,
                15,
                18,
                19,
                23,
                24
            ],
            commissioningDate: [
                {
                    year: 2021,
                    quarter: 3,
                    constructionState: 'CONSTRUCTION_STATE_UNKNOWN'
                }
            ],
            pricePerOffer: {
                currency: 'RUB',
                from: '7446250',
                to: '7803690'
            },
            pricePerMeter: {
                currency: 'RUB',
                from: '92500',
                to: '96700'
            },
            offersCount: 9,
            offerId: '6302897158550949374',
            images: {}
        },
        {
            roomType: 3,
            roomCount: 3,
            wholeArea: {
                value: 82,
                unit: 'SQ_M',
                name: 'whole'
            },
            livingArea: {
                value: 40.7,
                unit: 'SQ_M',
                name: 'living'
            },
            kitchenArea: {
                value: 24.5,
                unit: 'SQ_M',
                name: 'kitchen'
            },
            clusterId: '710517-E7550DDB67ABB7C5',
            floors: [
                1
            ],
            commissioningDate: [
                {
                    year: 2021,
                    quarter: 3,
                    constructionState: 'CONSTRUCTION_STATE_UNKNOWN'
                }
            ],
            pricePerOffer: {
                currency: 'RUB',
                from: '7535800',
                to: '7535800'
            },
            pricePerMeter: {
                currency: 'RUB',
                from: '91900',
                to: '91900'
            },
            offersCount: 1,
            offerId: '6302897158553062643',
            images: {}
        },
        {
            roomType: 3,
            roomCount: 3,
            wholeArea: {
                value: 87.2,
                unit: 'SQ_M',
                name: 'whole'
            },
            livingArea: {
                value: 44.8,
                unit: 'SQ_M',
                name: 'living'
            },
            kitchenArea: {
                value: 15.1,
                unit: 'SQ_M',
                name: 'kitchen'
            },
            clusterId: '710517-387CC9D5408298CA',
            floors: [
                2,
                6,
                7,
                10
            ],
            commissioningDate: [
                {
                    year: 2020,
                    quarter: 4,
                    constructionState: 'CONSTRUCTION_STATE_UNKNOWN'
                }
            ],
            pricePerOffer: {
                currency: 'RUB',
                from: '8083440',
                to: '8798480'
            },
            pricePerMeter: {
                currency: 'RUB',
                from: '92700',
                to: '100900'
            },
            offersCount: 6,
            offerId: '6302897158578719293',
            images: {}
        }
    ],
    pager: {
        page,
        size: 3,
        total: withoutPager ? 1 : 2
    }
});
