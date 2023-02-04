export const getPlans = ({ withoutPager = false, page = 1 } = {}) => ({
    items: [
        {
            roomType: 1,
            roomCount: 1,
            wholeArea: {
                value: 38.8,
                unit: 'SQ_M',
                name: 'whole'
            },
            livingArea: {
                value: 13.9,
                unit: 'SQ_M',
                name: 'living'
            },
            kitchenArea: {
                value: 17.7,
                unit: 'SQ_M',
                name: 'kitchen'
            },
            clusterId: '710517-499CF0F85AB8E4A1',
            floors: [
                4,
                12,
                14,
                20,
                22,
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
                from: '3880000',
                to: '4066240'
            },
            pricePerMeter: {
                currency: 'RUB',
                from: '100000',
                to: '104800'
            },
            offersCount: 7,
            offerId: '6302897158550237178',
            images: {}
        },
        {
            roomType: 1,
            roomCount: 1,
            wholeArea: {
                value: 32.5,
                unit: 'SQ_M',
                name: 'whole'
            },
            livingArea: {
                value: 11,
                unit: 'SQ_M',
                name: 'living'
            },
            kitchenArea: {
                value: 11.2,
                unit: 'SQ_M',
                name: 'kitchen'
            },
            clusterId: '710517-506A781742D212CC',
            floors: [
                8,
                12
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
                from: '4082000',
                to: '4130750'
            },
            pricePerMeter: {
                currency: 'RUB',
                from: '125600',
                to: '127100'
            },
            offersCount: 2,
            offerId: '6302897158558608357',
            images: {}
        }
    ],
    pager: {
        page,
        size: 3,
        total: withoutPager ? 1 : 2
    }
});
