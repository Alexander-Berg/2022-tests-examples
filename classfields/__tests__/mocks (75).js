export const getInitialState = () => ({
    cards: {
        site: {
            id: 1839196,
            phoneHash: 'KzcF0OHTUJxOLDEN0NPTQRy',
            withOffers: true,
            resaleTotalOffers: 10,
            timestamp: 1643887486673,
            location: {}
        }
    },
    page: {
        isLoading: false,
        isLocked: false,
        name: 'widget-site-offers',
        route: 'widget-site-offers',
        path: '/widget-site-offers/?from=yandex_wizard_maps&siteId=1839196&theme=dark',
        queryId: '35d37f787b7057eed52b232ea0fedbdc',
        isFailed: false,
        params: {
            from: 'yandex_wizard_maps',
            siteId: '1839196',
            theme: 'dark'
        }
    },
    sitePlans: {
        plans: {
            items: [],
            pager: null
        },
        rooms: []
    }
});

export const gateSitePlans = {
    plans: [
        {
            roomType: 'STUDIO',
            roomCount: 0,
            wholeArea: {
                value: 19.4,
                unit: 'SQ_M'
            },
            livingArea: {
                value: 11.9,
                unit: 'SQ_M'
            },
            images: {},
            clusterId: '1839196-1200-E50CC178863DFDCB',
            floors: [
                7,
                9,
                11,
                12,
                13,
                14,
                15,
                16,
                17
            ],
            commissioningDate: [
                {
                    year: 2023,
                    quarter: 1,
                    constructionState: 'CONSTRUCTION_STATE_UNFINISHED'
                },
                {
                    year: 2023,
                    quarter: 3,
                    constructionState: 'CONSTRUCTION_STATE_UNFINISHED'
                }
            ],
            pricePerOffer: {
                currency: 'RUB',
                from: '3789674',
                to: '4257834'
            },
            pricePerMeter: {
                currency: 'RUB',
                from: '195344',
                to: '219475'
            },
            offersCount: 15,
            offerId: '1040588801758632802'
        },
        {
            roomType: 'STUDIO',
            roomCount: 0,
            wholeArea: {
                value: 19.8,
                unit: 'SQ_M'
            },
            livingArea: {
                value: 11.9,
                unit: 'SQ_M'
            },
            images: {},
            clusterId: '1839196-1200-27E71D6D6E073861',
            floors: [
                2
            ],
            commissioningDate: [
                {
                    year: 2023,
                    quarter: 3,
                    constructionState: 'CONSTRUCTION_STATE_UNFINISHED'
                }
            ],
            pricePerOffer: {
                currency: 'RUB',
                from: '3799204',
                to: '3800000'
            },
            pricePerMeter: {
                currency: 'RUB',
                from: '191878',
                to: '191919'
            },
            offersCount: 2,
            offerId: '1040588801755951521'
        },
        {
            roomType: 'STUDIO',
            roomCount: 0,
            wholeArea: {
                value: 21.2,
                unit: 'SQ_M'
            },
            livingArea: {
                value: 12.9,
                unit: 'SQ_M'
            },
            images: {},
            clusterId: '1839196-1200-F9461055F0CCA521',
            floors: [
                7,
                10,
                14,
                17
            ],
            commissioningDate: [
                {
                    year: 2023,
                    quarter: 3,
                    constructionState: 'CONSTRUCTION_STATE_UNFINISHED'
                }
            ],
            pricePerOffer: {
                currency: 'RUB',
                from: '3928254',
                to: '4006016'
            },
            pricePerMeter: {
                currency: 'RUB',
                from: '185294',
                to: '188963'
            },
            offersCount: 4,
            offerId: '1040588801757672616'
        },
        {
            roomType: 'STUDIO',
            roomCount: 0,
            wholeArea: {
                value: 21.6,
                unit: 'SQ_M'
            },
            livingArea: {
                value: 12.9,
                unit: 'SQ_M'
            },
            images: {},
            clusterId: '1839196-1200-585775DC70B69001',
            floors: [
                2
            ],
            commissioningDate: [
                {
                    year: 2023,
                    quarter: 3,
                    constructionState: 'CONSTRUCTION_STATE_UNFINISHED'
                }
            ],
            pricePerOffer: {
                currency: 'RUB',
                from: '3950856',
                to: '3950856'
            },
            pricePerMeter: {
                currency: 'RUB',
                from: '182909',
                to: '182909'
            },
            offersCount: 1,
            offerId: '1040588801755947868'
        },
        {
            roomType: 'STUDIO',
            roomCount: 0,
            wholeArea: {
                value: 24.6,
                unit: 'SQ_M'
            },
            livingArea: {
                value: 10.8,
                unit: 'SQ_M'
            },
            images: {},
            clusterId: '1839196-1200-15DDF2E0F869412B',
            floors: [
                3,
                4,
                6,
                7,
                8,
                9,
                10,
                11,
                13,
                14,
                15,
                16,
                17
            ],
            commissioningDate: [
                {
                    year: 2022,
                    quarter: 4,
                    constructionState: 'CONSTRUCTION_STATE_UNFINISHED'
                },
                {
                    year: 2023,
                    quarter: 3,
                    constructionState: 'CONSTRUCTION_STATE_UNFINISHED'
                }
            ],
            pricePerOffer: {
                currency: 'RUB',
                from: '4022543',
                to: '5398082'
            },
            pricePerMeter: {
                currency: 'RUB',
                from: '163518',
                to: '219078'
            },
            offersCount: 32,
            offerId: '1040588801758721186'
        }
    ],
    total: 44,
    pager: {
        page: 1,
        size: 5,
        total: 9
    }
};

export const gateSitePlansStats = {
    rooms: [
        {
            roomType: 'STUDIO',
            priceFrom: '3789674',
            priceTo: '6790106',
            areaFrom: 19.4,
            areaTo: 32.2,
            flatPlansCount: 44,
            offersCount: 302
        }
    ]
};
