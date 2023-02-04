export const getPager = () => ({
    page: 0,
    pageSize: 3,
    totalItems: 3,
    totalPages: 1
});

export const getOffers = () => [
    {
        offerId: '5198848938494795096',
        building: {
            builtYear: 2021,
            builtQuarter: 2,
            buildingState: 'UNFINISHED',
            buildingType: 'MONOLIT',
            improvements: {
                LIFT: true,
                RUBBISH_CHUTE: true
            },
            siteId: 1685695,
            siteName: 'Цветочные поляны',
            siteDisplayName: 'ЖК «Цветочные поляны»',
            houseId: '1990069',
            houseReadableName: 'Корпус 6',
            heatingType: 'UNKNOWN',
            buildingImprovementsMap: {
                LIFT: true,
                RUBBISH_CHUTE: true
            }
        },
        floorsOffered: [ 2 ],
        floorsTotal: 8,
        area: {
            value: 36.14,
            unit: 'SQUARE_METER'
        },
        apartment: {
            siteFlatPlanId: '1685695-6272F4A21E505F06',
            renovation: 'PRIME_RENOVATION'
        },
        price: {
            currency: 'RUR',
            value: 4330726,
            period: 'WHOLE_LIFE',
            unit: 'WHOLE_OFFER',
            trend: 'UNCHANGED',
            hasPriceHistory: false,
            valuePerPart: 119832,
            unitPerPart: 'SQUARE_METER',
            valueForWhole: 4730726,
            unitForWhole: 'WHOLE_OFFER',
            price: {
                value: 4730726,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE'
            },
            pricePerPart: {
                value: 119832,
                currency: 'RUB',
                priceType: 'PER_METER',
                pricingPeriod: 'WHOLE_LIFE'
            },
            priceForWhole: {
                value: 4730000,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE'
            }
        }
    },
    {
        offerId: '5198848938494797793',
        building: {
            builtYear: 2021,
            builtQuarter: 2,
            buildingState: 'HAND_OVER',
            buildingType: 'MONOLIT',
            improvements: {
                LIFT: true,
                RUBBISH_CHUTE: true
            },
            siteId: 1685695,
            siteName: 'Цветочные поляны',
            siteDisplayName: 'ЖК «Цветочные поляны»',
            houseId: '1990069',
            houseReadableName: 'Корпус 6',
            heatingType: 'UNKNOWN',
            buildingImprovementsMap: {
                LIFT: true,
                RUBBISH_CHUTE: true
            }
        },
        floorsOffered: [ 7 ],
        floorsTotal: 7,
        area: {
            value: 36.14,
            unit: 'SQUARE_METER'
        },
        apartment: {
            siteFlatPlanId: '1685695-6272F4A21E505F06',
            renovation: 'CLEAN'
        },
        price: {
            currency: 'UR',
            value: 4875286,
            period: 'WHOLE_LIFE',
            unit: 'WHOLE_OFFER',
            trend: 'UNCHANGED',
            hasPriceHistory: false,
            valuePerPart: 134900,
            unitPerPart: 'SQUARE_METER',
            valueForWhole: 4875286,
            unitForWhole: 'WHOLE_OFFER',
            price: {
                value: 4875286,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE'
            },
            pricePerPart: {
                value: 134900,
                currency: 'RUB',
                priceType: 'PER_METER',
                pricingPeriod: 'WHOLE_LIFE'
            },
            priceForWhole: {
                value: 4875286,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE'
            }
        }
    },
    {
        offerId: '5198848938494795873',
        building: {
            builtYear: 2021,
            builtQuarter: 2,
            buildingState: 'UNFINISHED',
            buildingType: 'MONOLIT',
            improvements: {
                LIFT: true,
                RUBBISH_CHUTE: true
            },
            siteId: 1685695,
            siteName: 'Цветочные поляны',
            siteDisplayName: 'ЖК «Цветочные поляны»',
            houseId: '1990069',
            houseReadableName: 'Корпус 6',
            heatingType: 'UNKNOWN',
            buildingImprovementsMap: {
                LIFT: true,
                RUBBISH_CHUTE: true
            }
        },
        floorsOffered: [ 8 ],
        floorsTotal: 8,
        area: {
            value: 36.14,
            unit: 'SQUARE_METER'
        },
        apartment: {
            siteFlatPlanId: '1685695-6272F4A21E505F06',
            renovation: 'TURNKEY'
        },
        price: {
            currency: 'RUR',
            value: 4875286,
            period: 'WHOLE_LIFE',
            unit: 'WHOLE_OFFER',
            trend: 'UNCHANGED',
            hasPriceHistory: false,
            valuePerPart: 134900,
            unitPerPart: 'SQUARE_METER',
            valueForWhole: 4875286,
            unitForWhole: 'WHOLE_OFFER',
            price: {
                value: 4875286,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE'
            },
            pricePerPart: {
                value: 134900,
                currency: 'RUB',
                priceType: 'PER_METER',
                pricingPeriod: 'WHOLE_LIFE'
            },
            priceForWhole: {
                value: 4875286,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE'
            }
        }
    }
];

export const getPlan = ({
    withKitchenArea = true,
    isStudio = false
} = {}) => ({
    roomType: isStudio ? 'STUDIO' : 3,
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
    kitchenArea: withKitchenArea && {
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
});

export const getCard = ({ isApartment = false, withPhone = false, apartmentType } = {}) => ({
    locativeFullName: 'в жилом районе «Саларьево парк»',
    buildingFeatures: {
        isApartment,
        apartmentType
    },
    ...(withPhone && {
        salesDepartment: {
            id: 918322,
            name: 'Группа Компаний ПИК',
            weekTimetable: [
                {
                    dayFrom: 1,
                    dayTo: 7,
                    timePattern: [
                        {
                            open: '00:00',
                            close: '23:59'
                        }
                    ]
                }
            ]
        },
        phoneHash: withPhone && 'KzcF0OHTUJxNLTMNzOPTAR3'
    })
});

export const getInitialState = () => ({
    user: {
        favorites: [
            '5198848938494797793'
        ],
        favoritesMap: {
            '5198848938494797793': true
        }
    },
    cardPlansOffers: {
        page: 0,
        pager: getPager(),
        offers: getOffers(),
        sort: 'PRICE',
        turnoverOccurrence: []
    }
});
