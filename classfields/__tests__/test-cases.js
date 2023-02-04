import * as decl from 'realty-core/app/lib/filters/decl/secondary';

const COMMON = {
    SELL: [ 'APARTMENT', 'ROOMS', 'HOUSE', 'LOT', 'GARAGE', 'COMMERCIAL' ],
    RENT: [ 'APARTMENT', 'ROOMS', 'HOUSE', 'GARAGE', 'COMMERCIAL' ],
    DAILY: [ 'APARTMENT', 'ROOMS', 'HOUSE' ]
};

const CASES = {
    common: Object.keys(COMMON).reduce((res, ctype) => [
        ...res,
        ...COMMON[ctype].map(category => [
            ctype.toLowerCase() + '-' + category.toLowerCase(),
            { ctype, category }
        ])
    ], []),
    filled: [
        [
            'filled-and-focused-price',
            {
                ctype: 'SELL',
                category: 'APARTMENT',
                'geo-refinement-multi': [
                    {
                        label: 'Арбатская',
                        data: {
                            type: 'metro',
                            params: {
                                metroGeoId: 20481
                            }
                        }
                    },
                    {
                        label: 'Охотный ряд',
                        data: {
                            type: 'metro',
                            params: {
                                metroGeoId: 20488
                            }
                        }
                    }
                ],
                price: [ 1000000, 90000000 ],
                roomsTotal: [ '1', '2' ],
                area: [ 100, 250 ],
                floor: [ 1, 10 ],
                lastFloor: 'NO',
                floorExceptFirst: true,
                floorExceptLast: true,
                renovation: [ 'COSMETIC_DONE' ],
                balcony: 'ANY',
                kitcheSpaceMin: '7',
                ceilingHeightMin: '2.7',
                bathroomUnit: [ 'TWO_AND_MORE' ],
                builtYear: [ 2000, 2020 ],
                minFloors: '5',
                apartments: 'NO',
                buildingEpoch: 'KHRUSHCHEV',
                buildingSeriesId: [
                    {
                        data: {
                            params: {
                                tagId: '123',
                                tagTitle: '11-123'
                            }
                        }
                    },
                    {
                        data: {
                            params: {
                                tagId: '456',
                                tagTitle: 'индивидуальный план'
                            }
                        }
                    }
                ],
                includeTag: [
                    {
                        data: {
                            params: {
                                tagId: '1',
                                tagTitle: 'отопление'
                            }
                        }
                    },
                    {
                        data: {
                            params: {
                                tagId: '2',
                                tagTitle: 'агент'
                            }
                        }
                    }
                ],
                excludeTag: [
                    {
                        data: {
                            params: {
                                tagId: '3',
                                tagTitle: 'старый фонд'
                            }
                        }
                    },
                    {
                        data: {
                            params: {
                                tagId: '4',
                                tagTitle: 'ясли'
                            }
                        }
                    }
                ],
                agents: true,
                withExcerptsOnly: true
            },
            null,
            page => page.click('.FiltersFormField_name_price-with-type')
        ]
    ],
    main: [
        [
            'opened-geo',
            {
                ctype: 'SELL',
                category: 'APARTMENT',
                'geo-refinement-multi': [
                    {
                        label: 'Тверская',
                        data: {
                            type: 'metro',
                            params: {
                                metroGeoId: 20472
                            }
                        }
                    },
                    {
                        label: 'Театральная',
                        data: {
                            type: 'metro',
                            params: {
                                metroGeoId: 20473
                            }
                        }
                    },
                    {
                        label: 'Арбатская',
                        data: {
                            type: 'metro',
                            params: {
                                metroGeoId: 20481
                            }
                        }
                    },
                    {
                        label: 'Охотный ряд',
                        data: {
                            type: 'metro',
                            params: {
                                metroGeoId: 20488
                            }
                        }
                    },
                    {
                        label: 'Пушкинская',
                        data: {
                            type: 'metro',
                            params: {
                                metroGeoId: 20501
                            }
                        }
                    }
                ]
            },
            null,
            page => page.click('.FiltersFormField__refinements-badges')
        ],
        [
            'opened-category',
            {
                ctype: 'SELL',
                category: 'APARTMENT'
            },
            null,
            page => page.click('.FiltersFormField_name_category')
        ],
        [
            'opened-houseType',
            {
                ctype: 'SELL',
                category: 'HOUSE'
            },
            null,
            page => page.click('.FormField_name_houseType')
        ],
        [
            'opened-garageType',
            {
                ctype: 'SELL',
                category: 'GARAGE'
            },
            null,
            page => page.click('.FormField_name_garageType')
        ],
        [
            'opened-commercialType',
            {
                ctype: 'SELL',
                category: 'COMMERCIAL'
            },
            null,
            page => page.click('.FiltersFormField_name_commercialType')
        ],
        [
            'opened-deliveryDate',
            {
                ctype: 'NEW_SITES'
            },
            null,
            page => page.click('.FormField_name_deliveryDate')
        ]
    ],
    collapsed: [
        [
            'collapsed',
            {
                ctype: 'SELL',
                category: 'APARTMENT'
            },
            false
        ],
        [
            'collapsed-rent',
            {
                ctype: 'RENT',
                category: 'APARTMENT'
            },
            false
        ]
    ],
    search: decl.commercialType.deps.values.reduce((res, ctypeDecl) => [
        ...res,
        ...ctypeDecl[1].map(commercialType => [
            `${ctypeDecl[0].ctype.toLowerCase()}-commercial-${commercialType}`,
            {
                ctype: ctypeDecl[0].ctype,
                commercialType,
                category: 'COMMERCIAL'
            }
        ])
    ], []).concat([
        [
            'sell-apartment-secondary',
            {
                ctype: 'SELL',
                category: 'APARTMENT',
                newFlat: 'NO'
            }
        ],
        [
            'sell-apartment-newflat',
            {
                ctype: 'SELL',
                category: 'APARTMENT',
                newFlat: 'YES'
            }
        ]
    ])
};

Object.keys(CASES).forEach(key => CASES[key] = CASES[key].map(cs => [
    cs[0],
    {
        forms: cs[1],
        extraShown: cs[2] !== false
    },
    cs[3]
]));

export const getCases = page => []
    .concat(CASES.common)
    .concat(CASES.filled)
    .concat(page === 'main' ? CASES.main : CASES.collapsed)
    .concat(page === 'search' || page === 'map' ? CASES.search : []);
