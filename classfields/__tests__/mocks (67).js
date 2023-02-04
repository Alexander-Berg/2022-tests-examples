export const getSiteCard = ({
    withOffers = true,
    withBilling = true,
    flatStatus,
    resaleTotalOffers = 1,
    location = {},
    buildingFeatures = {}
} = {}) => ({
    id: 1,
    withOffers,
    withBilling,
    timestamp: 1586963713431,
    flatStatus,
    location,
    buildingFeatures,
    locativeFullName: 'в жилом комплексе «Полярная 25»',
    resaleTotalOffers,
    developers: [],
    salesDepartment: {
        name: 'Группа Компаний ПИК',
        encryptedPhones: [
            {
                phoneHash: 'KzcF5MHTEJ5MLzIN2MPTgRy',
                phoneWithMask: 'test'
            }
        ]
    }
});

export const getInitialState = ({ roomsTotal } = {}) => ({
    filters: {
        'site-plans-search': {
            sections: {
                mainShown: true,
                extraShown: false
            },
            controller: 'newbuilding',
            decl: {
                price: {
                    control: 'number-range',
                    maxLength: 15,
                    fromName: 'priceMin',
                    toName: 'priceMax',
                    placeholderValues: [
                        '2 616 000',
                        '10 007 000'
                    ]
                },
                priceType: {
                    control: 'radioset',
                    defaultValue: 'PER_OFFER',
                    values: [
                        'PER_OFFER',
                        'PER_METER'
                    ]
                },
                area: {
                    control: 'number-range',
                    maxLength: 4,
                    unit: 'unit_square_m',
                    fromName: 'areaMin',
                    toName: 'areaMax',
                    placeholderValues: [
                        20,
                        91
                    ]
                },
                kitchenSpace: {
                    control: 'number-range',
                    maxLength: 4,
                    unit: 'unit_square_m',
                    fromName: 'kitchenSpaceMin',
                    toName: 'kitchenSpaceMax',
                    placeholderValues: [
                        10,
                        31
                    ]
                },
                floors: {
                    control: 'number-range',
                    maxLength: 3,
                    fromName: 'floorMin',
                    toName: 'floorMax',
                    placeholderValues: [
                        1,
                        25
                    ]
                },
                bathroomUnit: {
                    control: 'multi-select',
                    allowed: [
                        [
                            'MATCHED',
                            'SEPARATED'
                        ]
                    ],
                    values: [
                        'MATCHED',
                        'SEPARATED',
                        'TWO_AND_MORE'
                    ]
                },
                decoration: {
                    control: 'multi-select',
                    allowed: [
                        []
                    ],
                    values: [
                        'CLEAN',
                        'ROUGH',
                        'TURNKEY'
                    ]
                },
                houseId: {
                    control: 'multi-select',
                    allowed: [
                        [
                            '756548',
                            '756551',
                            '2048044'
                        ]
                    ],
                    houseData: {
                        756548: {
                            commissioningDate: {
                                constructionState: 'CONSTRUCTION_STATE_FINISHED'
                            },
                            houseName: 'Корпус 3А'
                        },
                        756551: {
                            commissioningDate: {
                                year: 2020,
                                quarter: 4,
                                constructionState: 'CONSTRUCTION_STATE_UNFINISHED'
                            },
                            houseName: 'Корпус 2А'
                        },
                        2048044: {
                            commissioningDate: {
                                year: 2021,
                                quarter: 3,
                                constructionState: 'CONSTRUCTION_STATE_UNFINISHED'
                            }
                        }
                    },
                    values: [
                        '756548',
                        '756551',
                        '2048044'
                    ]
                },
                roomsTotal: {
                    control: 'multi-select',
                    allowed: [
                        [
                            '1',
                            'STUDIO',
                            '3',
                            '2'
                        ]
                    ],
                    values: [
                        'STUDIO',
                        '1',
                        '2',
                        '3',
                        'PLUS_4'
                    ]
                },
                floorExceptFirst: {
                    control: 'checkbox'
                },
                lastFloor: {
                    control: 'checkbox',
                    positiveValue: 'NO'
                }
            },
            data: {
                roomsTotal
            }
        }
    },
    forms: {},
    page: {
        isLoading: false
    },
    sitePlans: {
        plans: {
            items: [
                {
                    roomType: 3,
                    roomCount: 3,
                    wholeArea: {
                        value: 61,
                        unit: 'SQ_M',
                        name: 'whole'
                    },
                    livingArea: {
                        value: 33.8,
                        unit: 'SQ_M',
                        name: 'living'
                    },
                    kitchenArea: {
                        value: 11.7,
                        unit: 'SQ_M',
                        name: 'kitchen'
                    },
                    clusterId: '166185-3EBECA6B2FDD87DE',
                    floors: [
                        6
                    ],
                    commissioningDate: [
                        {
                            year: 2021,
                            quarter: 4,
                            constructionState: 'CONSTRUCTION_STATE_UNKNOWN'
                        }
                    ],
                    pricePerOffer: {
                        currency: 'RUB',
                        from: '8631500',
                        to: '8631500'
                    },
                    pricePerMeter: {
                        currency: 'RUB',
                        from: '141500',
                        to: '141500'
                    },
                    offersCount: 1,
                    offerId: '6302897158554881686',
                    images: {}
                }
            ]
        },
        rooms: [
            {
                roomType: 'STUDIO',
                priceFrom: '4118400',
                areaFrom: 19.8,
                flatPlansCount: 17,
                offersCount: 59
            },
            {
                roomType: 1,
                priceFrom: '5617920',
                areaFrom: 33.6,
                flatPlansCount: 43,
                offersCount: 114
            }
        ]
    },
    user: {},
    cardPhones: {}
});
