export const getInitialState = ({
    data = {},
    matchedQuantity = 0,
    houseIdValues = [ '1658465', '1696962' ]
} = {}) => ({
    filters: {
        card: {
            type: 'card',
            decl: {
                roomsTotal: {
                    type: 'multienum',
                    disabled: [
                        'PLUS_4'
                    ],
                    values: [
                        'STUDIO',
                        '1',
                        '2',
                        '3',
                        'PLUS_4'
                    ]
                },
                houseId: {
                    type: 'multienum',
                    allowed: true,
                    houseData: {
                        1658465: {
                            commissioningDate: {
                                year: 2021,
                                quarter: 1,
                                constructionState: 'CONSTRUCTION_STATE_UNFINISHED'
                            },
                            houseName: 'Корпус 25'
                        },
                        1696962: {
                            commissioningDate: {
                                year: 2021,
                                quarter: 4,
                                constructionState: 'CONSTRUCTION_STATE_UNFINISHED'
                            },
                            houseName: 'Корпус 27'
                        }
                    },
                    values: houseIdValues
                },
                price: {
                    type: 'range',
                    fromName: 'priceMin',
                    toName: 'priceMax'
                },
                priceType: {
                    defaultValue: 'PER_OFFER',
                    allowed: true,
                    values: [
                        'PER_OFFER',
                        'PER_METER'
                    ]
                },
                area: {
                    type: 'range',
                    fromName: 'areaMin',
                    toName: 'areaMax'
                },
                kitchenSpace: {
                    type: 'range',
                    fromName: 'kitchenSpaceMin',
                    toName: 'kitchenSpaceMax'
                },
                decoration: {
                    type: 'multienum',
                    allowed: [
                        [
                            'CLEAN'
                        ]
                    ],
                    values: [
                        'CLEAN',
                        'ROUGH',
                        'TURNKEY'
                    ]
                },
                bathroomUnit: {
                    type: 'multienum',
                    allowed: [
                        [
                            'SEPARATED',
                            'MATCHED'
                        ]
                    ],
                    values: [
                        'MATCHED',
                        'SEPARATED',
                        'TWO_AND_MORE'
                    ]
                },
                floor: {
                    type: 'range',
                    fromName: 'floorMin',
                    toName: 'floorMax'
                },
                lastFloor: {
                    values: [
                        'YES',
                        'NO'
                    ]
                },
                floorExceptFirst: {}
            },
            data: {
                priceType: 'PER_OFFER',
                ...data
            },
            sections: {
                mainShown: true,
                extraShown: false
            },
            matchedQuantity: -1
        }
    },
    forms: {},
    page: {
        isLoading: false
    },
    sitePlans: {
        matchedQuantity
    }
});
