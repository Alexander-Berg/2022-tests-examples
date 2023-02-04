export const getInitialState = ({
    extraShown = false,
    houseIdValues = [
        '756548',
        '756551',
        '2048044'
    ]
} = {}) => ({
    filters: {
        'site-plans-search': {
            sections: {
                mainShown: true,
                extraShown
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
                    values: houseIdValues
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
                floorExceptFirst: { control: 'checkbox', type: 'tag', view: 'blue' },
                lastFloor: {
                    control: 'radioset',
                    type: 'tag',
                    view: 'blue',
                    allowUncheck: true,
                    values: [
                        'NO',
                        'YES'
                    ]
                }
            }
        }
    },
    forms: {},
    page: {
        isLoading: false
    }
});
