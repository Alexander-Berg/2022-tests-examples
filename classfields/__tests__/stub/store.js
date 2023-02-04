export const emptyStore = {
    comparison: {
        cards: [],
        filterValue: {
            type: 'SELL',
            category: 'APARTMENT',
            idsByFilterValue: {
                SELL: {
                    APARTMENT: []
                }
            },
            moreThanOneFilterValue: false
        },
        onlyDifferent: false
    },
    page: {
        queryId: '123'
    }
};

export const emptyStoreWithMoreThanOneFilterValue = {
    comparison: {
        cards: [],
        filterValue: {
            type: 'SELL',
            category: 'APARTMENT',
            idsByFilterValue: {
                SELL: {
                    APARTMENT: [],
                    HOUSE: []
                }
            },
            moreThanOneFilterValue: true
        },
        onlyDifferent: false
    },
    page: {
        queryId: '123'
    }
};

export const singleCardStore = {
    comparison: {
        cards: [
            {
                offerId: '4728358149333455886',
                trust: 'NORMAL',
                url: '//realty.test.vertis.yandex.ru/offer/4728358149333455886',
                unsignedInternalUrl: '//realty.test.vertis.yandex.ru/offer/4728358149333455886',
                partnerId: '1069251992',
                partnerName: 'Номер квартиры',
                offerType: 'SELL',
                offerCategory: 'APARTMENT',
                clusterId: '4728358149333455886',
                clusterHeader: true,
                clusterSize: 1,
                creationDate: '2020-09-15T12:10:14Z',
                updateDate: '2021-02-10T13:48:00Z',
                roomsTotal: 1,
                floorsTotal: 9,
                floorsOffered: [
                    3
                ],
                flatType: 'SECONDARY',
                area: {
                    value: 35,
                    unit: 'SQUARE_METER'
                },
                price: {
                    currency: 'RUR',
                    value: 130000,
                    period: 'WHOLE_LIFE',
                    unit: 'WHOLE_OFFER',
                    trend: 'UNCHANGED',
                    hasPriceHistory: false,
                    valuePerPart: 3714,
                    unitPerPart: 'SQUARE_METER',
                    valueForWhole: 130000,
                    unitForWhole: 'WHOLE_OFFER',
                    price: {
                        value: 130000,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    pricePerPart: {
                        value: 3714,
                        currency: 'RUB',
                        priceType: 'PER_METER',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    priceForWhole: {
                        value: 130000,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    }
                },
                notForAgents: false,
                totalImages: 1,
                mainImages: [
                    '//avatars.mdst.yandex.net/get-realty/2941/w1316/main'
                ],
                apartment: {},
                location: {
                    rgid: 285001,
                    geoId: 2,
                    subjectFederationId: 10174,
                    subjectFederationRgid: 741965,
                    settlementRgid: 417899,
                    settlementGeoId: 2,
                    address: 'Санкт-Петербург, проспект Металлистов, 82к1',
                    geocoderAddress: 'Россия, Санкт-Петербург, проспект Металлистов, 82к2',
                    structuredAddress: {
                        component: [
                            {
                                value: 'Россия',
                                geoId: 225,
                                regionGraphId: '143',
                                address: 'Россия',
                                regionType: 'COUNTRY',
                                queryParams: {
                                    rgid: '143',
                                    address: 'Россия'
                                }
                            },
                            {
                                value: 'Санкт-Петербург',
                                geoId: 2,
                                regionGraphId: '417899',
                                address: 'Санкт-Петербург',
                                regionType: 'CITY',
                                queryParams: {
                                    rgid: '417899',
                                    address: 'Россия, Санкт-Петербург'
                                }
                            },
                            {
                                value: 'проспект Металлистов',
                                geoId: 2,
                                regionGraphId: '417899',
                                address: 'проспект Металлистов',
                                regionType: 'STREET',
                                queryParams: {
                                    rgid: '417899',
                                    address: 'Россия, Санкт-Петербург, проспект Металлистов'
                                }
                            },
                            {
                                value: '82к2',
                                geoId: 2,
                                regionGraphId: '417899',
                                address: '82к2',
                                regionType: 'HOUSE',
                                queryParams: {
                                    rgid: '417899',
                                    address: 'Россия, Санкт-Петербург, проспект Металлистов, 82к2'
                                }
                            }
                        ]
                    },
                    point: {
                        latitude: 59.966187,
                        longitude: 30.41466,
                        precision: 'APPROXIMATE'
                    },
                    metro: {
                        metroGeoId: 20345,
                        name: 'Ладожская',
                        metroTransport: 'ON_TRANSPORT',
                        timeToMetro: 19,
                        latitude: 59.93243,
                        longitude: 30.439201,
                        minTimeToMetro: 19,
                        lineColors: [
                            'f07c1d'
                        ],
                        rgbColor: 'f07c1d'
                    },
                    streetAddress: 'проспект Металлистов, 82к1',
                    metroList: [
                        {
                            metroGeoId: 20345,
                            name: 'Ладожская',
                            metroTransport: 'ON_TRANSPORT',
                            timeToMetro: 19,
                            latitude: 59.93243,
                            longitude: 30.439201,
                            minTimeToMetro: 19,
                            lineColors: [
                                'f07c1d'
                            ],
                            rgbColor: 'f07c1d'
                        },
                        {
                            metroGeoId: 20331,
                            name: 'Площадь Ленина',
                            metroTransport: 'ON_TRANSPORT',
                            timeToMetro: 20,
                            latitude: 59.956753,
                            longitude: 30.355679,
                            minTimeToMetro: 20,
                            lineColors: [
                                'f03d2f'
                            ],
                            rgbColor: 'f03d2f'
                        },
                        {
                            metroGeoId: 20346,
                            name: 'Новочеркасская',
                            metroTransport: 'ON_TRANSPORT',
                            timeToMetro: 21,
                            latitude: 59.928913,
                            longitude: 30.411482,
                            minTimeToMetro: 21,
                            lineColors: [
                                'f07c1d'
                            ],
                            rgbColor: 'f07c1d'
                        }
                    ],
                    subjectFederationName: 'Санкт-Петербург и ЛО'
                },
                house: {
                    housePart: false
                },
                building: {
                    parkingType: 'OPEN',
                    heatingType: 'UNKNOWN'
                },
                internal: true,
                active: true,
                dealStatus: 'SALE',
                uid: '4057257086',
                partnerInternalId: '022350',
                exclusive: true,
                obsolete: false,
                withExcerpt: false,
                openPlan: false,
                newBuilding: false
            }
        ],
        filterValue: {
            type: 'SELL',
            category: 'APARTMENT',
            idsByFilterValue: {
                SELL: {
                    APARTMENT: [ '4728358149333455886' ]
                }
            },
            moreThanOneFilterValue: false
        },
        onlyDifferent: false
    },
    page: {
        queryId: '123'
    }
};

export const apartmentCardsStore = {
    comparison: {
        cards: [
            {
                offerId: '1782692487993447424',
                trust: 'NORMAL',
                url: '//realty.test.vertis.yandex.ru/offer/1782692487993447424',
                unsignedInternalUrl: '//realty.test.vertis.yandex.ru/offer/1782692487993447424',
                partnerId: '1035218734',
                partnerName: 'Яндекс.Недвижимость',
                offerType: 'SELL',
                offerCategory: 'APARTMENT',
                clusterId: '1782692487993447424',
                clusterHeader: true,
                clusterSize: 1,
                creationDate: '2021-01-14T12:49:37Z',
                updateDate: '2021-02-10T13:34:00Z',
                roomsTotal: 1,
                floorsTotal: 3,
                floorsOffered: [
                    2
                ],
                flatType: 'SECONDARY',
                area: {
                    value: 20,
                    unit: 'SQUARE_METER'
                },
                livingSpace: {
                    value: 10,
                    unit: 'SQUARE_METER'
                },
                price: {
                    currency: 'RUR',
                    value: 5000000,
                    period: 'WHOLE_LIFE',
                    unit: 'WHOLE_OFFER',
                    trend: 'UNCHANGED',
                    hasPriceHistory: false,
                    valuePerPart: 250000,
                    unitPerPart: 'SQUARE_METER',
                    valueForWhole: 5000000,
                    unitForWhole: 'WHOLE_OFFER',
                    price: {
                        value: 5000000,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    pricePerPart: {
                        value: 250000,
                        currency: 'RUB',
                        priceType: 'PER_METER',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    priceForWhole: {
                        value: 5000000,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    }
                },
                notForAgents: false,
                totalImages: 4,
                mainImages: [
                    '//avatars.mdst.yandex.net/get-realty/3022/add.1610628561459d9dd29060e/main'
                ],
                apartment: {},
                location: {
                    rgid: 304079,
                    geoId: 2,
                    subjectFederationId: 10174,
                    subjectFederationRgid: 741965,
                    settlementRgid: 417899,
                    settlementGeoId: 2,
                    address: 'Санкт-Петербург, улица Фучика, 25',
                    geocoderAddress: 'Россия, Санкт-Петербург, улица Фучика, 25',
                    structuredAddress: {
                        component: [
                            {
                                value: 'Россия',
                                geoId: 225,
                                regionGraphId: '143',
                                address: 'Россия',
                                regionType: 'COUNTRY',
                                queryParams: {
                                    rgid: '143',
                                    address: 'Россия'
                                }
                            },
                            {
                                value: 'Санкт-Петербург',
                                geoId: 2,
                                regionGraphId: '417899',
                                address: 'Санкт-Петербург',
                                regionType: 'CITY',
                                queryParams: {
                                    rgid: '417899',
                                    address: 'Россия, Санкт-Петербург'
                                }
                            },
                            {
                                value: 'улица Фучика',
                                geoId: 2,
                                regionGraphId: '417899',
                                address: 'улица Фучика',
                                regionType: 'STREET',
                                queryParams: {
                                    rgid: '417899',
                                    address: 'Россия, Санкт-Петербург, улица Фучика'
                                }
                            },
                            {
                                value: '25',
                                geoId: 2,
                                regionGraphId: '417899',
                                address: '25',
                                regionType: 'HOUSE',
                                queryParams: {
                                    rgid: '417899',
                                    address: 'Россия, Санкт-Петербург, улица Фучика, 25'
                                }
                            }
                        ]
                    },
                    point: {
                        latitude: 59.88384,
                        longitude: 30.386852,
                        precision: 'EXACT'
                    },
                    metro: {
                        metroGeoId: 114839,
                        name: 'Бухарестская',
                        metroTransport: 'ON_FOOT',
                        timeToMetro: 16,
                        latitude: 59.88366,
                        longitude: 30.36954,
                        minTimeToMetro: 16,
                        lineColors: [
                            'c063d1'
                        ],
                        rgbColor: 'c063d1'
                    },
                    streetAddress: 'улица Фучика, 25',
                    metroList: [
                        {
                            metroGeoId: 114839,
                            name: 'Бухарестская',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 16,
                            latitude: 59.88366,
                            longitude: 30.36954,
                            minTimeToMetro: 16,
                            lineColors: [
                                'c063d1'
                            ],
                            rgbColor: 'c063d1'
                        },
                        {
                            metroGeoId: 218469,
                            name: 'Проспект Славы',
                            metroTransport: 'ON_TRANSPORT',
                            timeToMetro: 14,
                            latitude: 59.856613,
                            longitude: 30.395447,
                            minTimeToMetro: 14,
                            lineColors: [
                                'c063d1'
                            ],
                            rgbColor: 'c063d1'
                        },
                        {
                            metroGeoId: 100652,
                            name: 'Волковская',
                            metroTransport: 'ON_TRANSPORT',
                            timeToMetro: 17,
                            latitude: 59.896004,
                            longitude: 30.358223,
                            minTimeToMetro: 17,
                            lineColors: [
                                'c063d1'
                            ],
                            rgbColor: 'c063d1'
                        },
                        {
                            metroGeoId: 114838,
                            name: 'Международная',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 24,
                            latitude: 59.870205,
                            longitude: 30.379295,
                            minTimeToMetro: 18,
                            lineColors: [
                                'c063d1'
                            ],
                            rgbColor: 'c063d1'
                        }
                    ],
                    subjectFederationName: 'Санкт-Петербург и ЛО'
                },
                house: {
                    studio: false,
                    housePart: false
                },
                building: {
                    builtYear: 2017,
                    buildingType: 'PANEL',
                    parkingType: 'OPEN',
                    buildingId: '442134348425311043',
                    heatingType: 'UNKNOWN',
                    priceStatistics: {
                        sellPricePerSquareMeter: {
                            level: 0,
                            maxLevel: 9,
                            rgbColor: '',
                            value: '0',
                            regionValueFrom: '13334',
                            regionValueTo: '2238921'
                        }
                    }
                },
                internal: true,
                active: true,
                dealStatus: 'SALE',
                uid: '4062933126',
                relevance: 0,
                commissioningDateIndexValue: 0,
                partnerInternalId: '1782692487993447424',
                callCenter: false,
                exclusive: true,
                obsolete: false,
                withExcerpt: false,
                freeReportAccessibility: 'FRA_NONE',
                paidReportAccessibility: 'PPA_ALLOWED_TO_BUY_WITH_ADDITIONAL_DATA_REQUIRED',
                remoteReview: {
                    onlineShow: false,
                    youtubeVideoReviewUrl: ''
                },
                socialPessimization: false,
                chargeForCallsType: 'BY_TUZ',
                yandexRent: false,
                openPlan: false,
                primarySaleV2: false,
                raised: true,
                premium: true,
                vasAvailable: true,
                suspicious: false,
                placement: false,
                promoted: true,
                newBuilding: false,
                views: 3,
                isRealtyOffer: true
            },
            {
                offerId: '7810784121041968896',
                trust: 'NORMAL',
                url: '//realty.test.vertis.yandex.ru/offer/7810784121041968896',
                unsignedInternalUrl: '//realty.test.vertis.yandex.ru/offer/7810784121041968896',
                partnerId: '1035218734',
                partnerName: 'Яндекс.Недвижимость',
                offerType: 'SELL',
                offerCategory: 'APARTMENT',
                clusterId: '7810784121041968896',
                clusterHeader: true,
                clusterSize: 1,
                creationDate: '2020-03-20T07:39:05Z',
                updateDate: '2020-09-30T10:47:20Z',
                roomsTotal: 3,
                floorsTotal: 14,
                floorsOffered: [
                    6
                ],
                flatType: 'SECONDARY',
                ceilingHeight: 2.12,
                area: {
                    value: 65.24,
                    unit: 'SQUARE_METER'
                },
                livingSpace: {
                    value: 45,
                    unit: 'SQUARE_METER'
                },
                kitchenSpace: {
                    value: 11,
                    unit: 'SQUARE_METER'
                },
                roomSpace: [
                    {
                        value: 21.24,
                        unit: 'SQUARE_METER'
                    },
                    {
                        value: 10,
                        unit: 'SQUARE_METER'
                    }
                ],
                price: {
                    currency: 'RUR',
                    value: 5500000,
                    period: 'WHOLE_LIFE',
                    unit: 'WHOLE_OFFER',
                    trend: 'UNCHANGED',
                    hasPriceHistory: false,
                    valuePerPart: 84304,
                    unitPerPart: 'SQUARE_METER',
                    valueForWhole: 5500000,
                    unitForWhole: 'WHOLE_OFFER',
                    price: {
                        value: 5500000,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    pricePerPart: {
                        value: 84304,
                        currency: 'RUB',
                        priceType: 'PER_METER',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    priceForWhole: {
                        value: 5500000,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    }
                },
                notForAgents: false,
                totalImages: 4,
                mainImages: [
                    '//avatars.mdst.yandex.net/get-realty/2941/add.31f7eb9cb44d0aa9bb168fbda5763.realty-api-vos/main'
                ],
                apartment: {},
                location: {
                    rgid: 285001,
                    geoId: 2,
                    subjectFederationId: 10174,
                    subjectFederationRgid: 741965,
                    settlementRgid: 417899,
                    settlementGeoId: 2,
                    address: 'Санкт-Петербург, Полюстровский проспект, 7',
                    geocoderAddress: 'Россия, Санкт-Петербург, Полюстровский проспект, 7',
                    structuredAddress: {
                        component: [
                            {
                                value: 'Россия',
                                geoId: 225,
                                regionGraphId: '143',
                                address: 'Россия',
                                regionType: 'COUNTRY',
                                queryParams: {
                                    rgid: '143',
                                    address: 'Россия'
                                }
                            },
                            {
                                value: 'Санкт-Петербург',
                                geoId: 2,
                                regionGraphId: '417899',
                                address: 'Санкт-Петербург',
                                regionType: 'CITY',
                                queryParams: {
                                    rgid: '417899',
                                    address: 'Россия, Санкт-Петербург'
                                }
                            },
                            {
                                value: 'Полюстровский проспект',
                                geoId: 2,
                                regionGraphId: '417899',
                                address: 'Полюстровский проспект',
                                regionType: 'STREET',
                                queryParams: {
                                    rgid: '417899',
                                    address: 'Россия, Санкт-Петербург, Полюстровский проспект'
                                }
                            },
                            {
                                value: '7',
                                geoId: 2,
                                regionGraphId: '417899',
                                address: '7',
                                regionType: 'HOUSE',
                                queryParams: {
                                    rgid: '417899',
                                    address: 'Россия, Санкт-Петербург, Полюстровский проспект, 7'
                                }
                            }
                        ]
                    },
                    point: {
                        latitude: 59.96423,
                        longitude: 30.407164,
                        precision: 'EXACT'
                    },
                    metro: {
                        metroGeoId: 20331,
                        name: 'Площадь Ленина',
                        metroTransport: 'ON_TRANSPORT',
                        timeToMetro: 18,
                        latitude: 59.956753,
                        longitude: 30.355679,
                        minTimeToMetro: 18,
                        lineColors: [
                            'f03d2f'
                        ],
                        rgbColor: 'f03d2f'
                    },
                    streetAddress: 'Полюстровский проспект, 7',
                    metroList: [
                        {
                            metroGeoId: 20331,
                            name: 'Площадь Ленина',
                            metroTransport: 'ON_TRANSPORT',
                            timeToMetro: 18,
                            latitude: 59.956753,
                            longitude: 30.355679,
                            minTimeToMetro: 18,
                            lineColors: [
                                'f03d2f'
                            ],
                            rgbColor: 'f03d2f'
                        },
                        {
                            metroGeoId: 20318,
                            name: 'Лесная',
                            metroTransport: 'ON_TRANSPORT',
                            timeToMetro: 19,
                            latitude: 59.984768,
                            longitude: 30.344358,
                            minTimeToMetro: 19,
                            lineColors: [
                                'f03d2f'
                            ],
                            rgbColor: 'f03d2f'
                        },
                        {
                            metroGeoId: 20330,
                            name: 'Выборгская',
                            metroTransport: 'ON_TRANSPORT',
                            timeToMetro: 21,
                            latitude: 59.97101,
                            longitude: 30.347305,
                            minTimeToMetro: 21,
                            lineColors: [
                                'f03d2f'
                            ],
                            rgbColor: 'f03d2f'
                        },
                        {
                            metroGeoId: 20346,
                            name: 'Новочеркасская',
                            metroTransport: 'ON_TRANSPORT',
                            timeToMetro: 21,
                            latitude: 59.928913,
                            longitude: 30.411482,
                            minTimeToMetro: 21,
                            lineColors: [
                                'f07c1d'
                            ],
                            rgbColor: 'f07c1d'
                        }
                    ],
                    subjectFederationName: 'Санкт-Петербург и ЛО'
                },
                house: {
                    studio: false,
                    apartments: false,
                    housePart: false
                },
                building: {
                    builtYear: 1972,
                    buildingType: 'BRICK',
                    improvements: {
                        LIFT: true,
                        RUBBISH_CHUTE: true,
                        GUARDED: false
                    },
                    parkingType: 'OPEN',
                    buildingId: '4408558359341357012',
                    flatsCount: 97,
                    porchesCount: 1,
                    heatingType: 'CENTRAL',
                    buildingEpoch: 'BUILDING_EPOCH_BREZHNEV',
                    priceStatistics: {
                        sellPricePerSquareMeter: {
                            level: 0,
                            maxLevel: 9,
                            rgbColor: '',
                            value: '0',
                            regionValueFrom: '13334',
                            regionValueTo: '2238921'
                        }
                    },
                    buildingImprovementsMap: {
                        LIFT: true,
                        RUBBISH_CHUTE: true,
                        GUARDED: false
                    }
                },
                hasAlarm: true,
                internal: true,
                active: true,
                dealStatus: 'SALE',
                uid: '4028945580',
                relevance: 0,
                commissioningDateIndexValue: 0,
                partnerInternalId: '7810784121041968896',
                callCenter: false,
                exclusive: true,
                obsolete: false,
                withExcerpt: false,
                freeReportAccessibility: 'FRA_IN_PROGRESS',
                paidReportAccessibility: 'PPA_ALLOWED_TO_BUY',
                remoteReview: {
                    onlineShow: true,
                    youtubeVideoReviewUrl: ''
                },
                socialPessimization: false,
                chargeForCallsType: 'NO_CHARGE',
                yandexRent: false,
                openPlan: false,
                primarySaleV2: false,
                raised: false,
                premium: false,
                vasAvailable: true,
                suspicious: false,
                transactionConditionsMap: {
                    HAGGLE: true
                },
                supplyMap: {
                    GAS: true
                },
                placement: false,
                promoted: false,
                newBuilding: false,
                views: 28,
                isRealtyOffer: true
            }
        ],
        filterValue: {
            type: 'SELL',
            category: 'APARTMENT',
            idsByFilterValue: {
                SELL: {
                    APARTMENT: [
                        '1782692487993447424',
                        '7810784121041968896'
                    ]
                }
            },
            moreThanOneFilterValue: false
        },
        onlyDifferent: false
    },
    page: {
        queryId: '123'
    }
};

export const roomCardsStore = {
    comparison: {
        cards: [
            {
                offerId: '3951760810797329920',
                trust: 'NORMAL',
                url: '//realty.test.vertis.yandex.ru/offer/3951760810797329920',
                unsignedInternalUrl: '//realty.test.vertis.yandex.ru/offer/3951760810797329920',
                partnerId: '1035218734',
                partnerName: 'Яндекс.Недвижимость',
                offerType: 'SELL',
                offerCategory: 'ROOMS',
                clusterId: '3951760810797329920',
                clusterHeader: true,
                clusterSize: 1,
                creationDate: '2020-06-30T07:28:48Z',
                roomsTotal: 3,
                roomsOffered: 1,
                floorsTotal: 17,
                floorsOffered: [
                    3
                ],
                area: {
                    value: 44,
                    unit: 'SQUARE_METER'
                },
                livingSpace: {
                    value: 22,
                    unit: 'SQUARE_METER'
                },
                roomSpace: [
                    {
                        value: 22,
                        unit: 'SQUARE_METER'
                    }
                ],
                price: {
                    currency: 'RUR',
                    value: 2400000,
                    period: 'WHOLE_LIFE',
                    unit: 'WHOLE_OFFER',
                    trend: 'UNCHANGED',
                    hasPriceHistory: false,
                    valuePerPart: 109091,
                    unitPerPart: 'SQUARE_METER',
                    valueForWhole: 2400000,
                    unitForWhole: 'WHOLE_OFFER',
                    price: {
                        value: 2400000,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    pricePerPart: {
                        value: 109091,
                        currency: 'RUB',
                        priceType: 'PER_METER',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    priceForWhole: {
                        value: 2400000,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    }
                },
                notForAgents: false,
                totalImages: 1,
                mainImages: [
                    '//avatars.mdst.yandex.net/get-realty/3019/add.1593502120697b217f0d5cb/main'
                ],
                apartment: {},
                location: {
                    rgid: 193370,
                    geoId: 213,
                    subjectFederationId: 1,
                    subjectFederationRgid: 741964,
                    settlementRgid: 587795,
                    settlementGeoId: 213,
                    address: 'Москва, Мичуринский проспект, 25к4',
                    geocoderAddress: 'Россия, Москва, Мичуринский проспект, 25к4',
                    structuredAddress: {
                        component: [
                            {
                                value: 'Россия',
                                geoId: 225,
                                regionGraphId: '143',
                                address: 'Россия',
                                regionType: 'COUNTRY',
                                queryParams: {
                                    rgid: '143',
                                    address: 'Россия'
                                }
                            },
                            {
                                value: 'Москва',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: 'Москва',
                                regionType: 'CITY',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва'
                                }
                            },
                            {
                                value: 'Мичуринский проспект',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: 'Мичуринский проспект',
                                regionType: 'STREET',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва, Мичуринский проспект'
                                }
                            },
                            {
                                value: '25к4',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: '25к4',
                                regionType: 'HOUSE',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва, Мичуринский проспект, 25к4'
                                }
                            }
                        ]
                    },
                    point: {
                        latitude: 55.695274,
                        longitude: 37.504993,
                        precision: 'EXACT'
                    },
                    metro: {
                        metroGeoId: 163522,
                        name: 'Раменки',
                        metroTransport: 'ON_FOOT',
                        timeToMetro: 6,
                        latitude: 55.697693,
                        longitude: 37.49873,
                        minTimeToMetro: 6,
                        lineColors: [
                            'ffe400'
                        ],
                        rgbColor: 'ffe400'
                    },
                    streetAddress: 'Мичуринский проспект, 25к4',
                    metroList: [
                        {
                            metroGeoId: 163522,
                            name: 'Раменки',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 6,
                            latitude: 55.697693,
                            longitude: 37.49873,
                            minTimeToMetro: 6,
                            lineColors: [
                                'ffe400'
                            ],
                            rgbColor: 'ffe400'
                        },
                        {
                            metroGeoId: 190113,
                            name: 'Мичуринский проспект',
                            metroTransport: 'ON_TRANSPORT',
                            timeToMetro: 10,
                            latitude: 55.68946,
                            longitude: 37.482975,
                            minTimeToMetro: 10,
                            lineColors: [
                                'ffe400'
                            ],
                            rgbColor: 'ffe400'
                        }
                    ],
                    subjectFederationName: 'Москва и МО'
                },
                house: {
                    housePart: false
                },
                building: {
                    builtYear: 1994,
                    buildingType: 'PANEL',
                    buildingSeries: 'П-3',
                    buildingSeriesId: '712089',
                    improvements: {
                        LIFT: true,
                        RUBBISH_CHUTE: true,
                        GUARDED: false,
                        SECURITY: false
                    },
                    parkingType: 'CLOSED',
                    buildingId: '5347996665924353986',
                    flatsCount: 271,
                    porchesCount: 4,
                    heatingType: 'CENTRAL',
                    priceStatistics: {
                        sellPricePerSquareMeter: {
                            level: 0,
                            maxLevel: 9,
                            rgbColor: '',
                            value: '0',
                            regionValueFrom: '22727',
                            regionValueTo: '2234148'
                        }
                    },
                    buildingImprovementsMap: {
                        LIFT: true,
                        RUBBISH_CHUTE: true,
                        GUARDED: false,
                        SECURITY: false
                    }
                },
                internal: true,
                active: true,
                dealStatus: 'SALE',
                uid: '4040730838',
                relevance: 0,
                commissioningDateIndexValue: 0,
                partnerInternalId: '3951760810797329920',
                callCenter: false,
                exclusive: true,
                obsolete: false,
                withExcerpt: false,
                freeReportAccessibility: 'FRA_NONE',
                paidReportAccessibility: 'PPA_NOT_ALLOWED_TO_BUY',
                remoteReview: {
                    onlineShow: false,
                    youtubeVideoReviewUrl: ''
                },
                socialPessimization: false,
                chargeForCallsType: 'BY_TUZ',
                yandexRent: false,
                openPlan: false,
                raised: false,
                premium: false,
                vasAvailable: true,
                suspicious: false,
                supplyMap: {
                    GAS: false
                },
                placement: false,
                promoted: true,
                newBuilding: false,
                views: 14,
                isRealtyOffer: true
            },
            {
                offerId: '8587174824471989504',
                trust: 'NORMAL',
                url: '//realty.test.vertis.yandex.ru/offer/8587174824471989504',
                unsignedInternalUrl: '//realty.test.vertis.yandex.ru/offer/8587174824471989504',
                partnerId: '1035218734',
                partnerName: 'Яндекс.Недвижимость',
                offerType: 'SELL',
                offerCategory: 'ROOMS',
                clusterId: '8587174824471989504',
                clusterHeader: true,
                clusterSize: 16,
                creationDate: '2020-09-21T13:06:39Z',
                updateDate: '2020-09-25T07:31:53Z',
                roomsTotal: 10,
                roomsOffered: 1,
                floorsTotal: 29,
                floorsOffered: [
                    10,
                    11
                ],
                floorCovering: 'PARQUET',
                ceilingHeight: 2.75,
                area: {
                    value: 100,
                    unit: 'SQUARE_METER'
                },
                livingSpace: {
                    value: 70,
                    unit: 'SQUARE_METER'
                },
                kitchenSpace: {
                    value: 10,
                    unit: 'SQUARE_METER'
                },
                roomSpace: [
                    {
                        value: 20,
                        unit: 'SQUARE_METER'
                    },
                    {
                        value: 20,
                        unit: 'SQUARE_METER'
                    }
                ],
                price: {
                    currency: 'RUR',
                    value: 1000000,
                    period: 'WHOLE_LIFE',
                    unit: 'WHOLE_OFFER',
                    trend: 'UNCHANGED',
                    hasPriceHistory: false,
                    valuePerPart: 25000,
                    unitPerPart: 'SQUARE_METER',
                    valueForWhole: 1000000,
                    unitForWhole: 'WHOLE_OFFER',
                    price: {
                        value: 1000000,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    pricePerPart: {
                        value: 25000,
                        currency: 'RUB',
                        priceType: 'PER_METER',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    priceForWhole: {
                        value: 1000000,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    }
                },
                notForAgents: true,
                agentFee: 20.5,
                prepayment: 50,
                totalImages: 4,
                mainImages: [
                    '//avatars.mdst.yandex.net/get-realty/3423/add.15438398084649b5a2b1e05/main'
                ],
                apartment: {
                    renovation: 'COSMETIC_DONE',
                    improvements: {
                        PHONE: true,
                        INTERNET: false,
                        ROOM_FURNITURE: true,
                        KITCHEN_FURNITURE: false,
                        TELEVISION: true,
                        WASHING_MACHINE: false,
                        REFRIGERATOR: true,
                        NO_FURNITURE: false,
                        DISHWASHER: false,
                        AIRCONDITION: true,
                        FLAT_ALARM: false,
                        BUILD_IN_TECH: true
                    }
                },
                location: {
                    rgid: 193332,
                    geoId: 213,
                    subjectFederationId: 1,
                    subjectFederationRgid: 741964,
                    settlementRgid: 587795,
                    settlementGeoId: 213,
                    address: 'Москва, улица Цюрупы, 20к1',
                    geocoderAddress: 'Россия, Москва, улица Цюрупы, 20к1',
                    structuredAddress: {
                        component: [
                            {
                                value: 'Россия',
                                geoId: 225,
                                regionGraphId: '143',
                                address: 'Россия',
                                regionType: 'COUNTRY',
                                queryParams: {
                                    rgid: '143',
                                    address: 'Россия'
                                }
                            },
                            {
                                value: 'Москва',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: 'Москва',
                                regionType: 'CITY',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва'
                                }
                            },
                            {
                                value: 'улица Цюрупы',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: 'улица Цюрупы',
                                regionType: 'STREET',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва, улица Цюрупы'
                                }
                            },
                            {
                                value: '20к1',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: '20к1',
                                regionType: 'HOUSE',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва, улица Цюрупы, 20к1'
                                }
                            }
                        ]
                    },
                    point: {
                        latitude: 55.667294,
                        longitude: 37.56969,
                        precision: 'EXACT'
                    },
                    metro: {
                        metroGeoId: 20436,
                        name: 'Новые Черёмушки',
                        metroTransport: 'ON_FOOT',
                        timeToMetro: 13,
                        latitude: 55.67008,
                        longitude: 37.554493,
                        minTimeToMetro: 13,
                        lineColors: [
                            'ff8103'
                        ],
                        rgbColor: 'ff8103'
                    },
                    streetAddress: 'улица Цюрупы, 20к1',
                    metroList: [
                        {
                            metroGeoId: 20436,
                            name: 'Новые Черёмушки',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 13,
                            latitude: 55.67008,
                            longitude: 37.554493,
                            minTimeToMetro: 13,
                            lineColors: [
                                'ff8103'
                            ],
                            rgbColor: 'ff8103'
                        },
                        {
                            metroGeoId: 20435,
                            name: 'Профсоюзная',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 19,
                            latitude: 55.677933,
                            longitude: 37.562874,
                            minTimeToMetro: 17,
                            lineColors: [
                                'ff8103'
                            ],
                            rgbColor: 'ff8103'
                        },
                        {
                            metroGeoId: 20437,
                            name: 'Калужская',
                            metroTransport: 'ON_TRANSPORT',
                            timeToMetro: 18,
                            latitude: 55.656685,
                            longitude: 37.540077,
                            minTimeToMetro: 18,
                            lineColors: [
                                'ff8103'
                            ],
                            rgbColor: 'ff8103'
                        }
                    ],
                    subjectFederationName: 'Москва и МО'
                },
                house: {
                    bathroomUnit: 'SEPARATED',
                    balconyType: 'BALCONY',
                    windowView: 'YARD',
                    apartments: false,
                    housePart: false
                },
                building: {
                    builtYear: 2014,
                    buildingState: 'UNFINISHED',
                    buildingType: 'PANEL',
                    buildingSeries: 'П-44Т',
                    buildingSeriesId: '663320',
                    improvements: {
                        ELITE: true,
                        PARKING: false,
                        LIFT: true,
                        RUBBISH_CHUTE: false,
                        PASS_BY: false
                    },
                    parkingType: 'UNDERGROUND',
                    buildingId: '971807810781104884',
                    flatsCount: 245,
                    porchesCount: 4,
                    heatingType: 'CENTRAL',
                    priceStatistics: {
                        sellPricePerSquareMeter: {
                            level: 0,
                            maxLevel: 9,
                            rgbColor: '',
                            value: '0',
                            regionValueFrom: '22727',
                            regionValueTo: '2234148'
                        }
                    },
                    buildingImprovementsMap: {
                        ELITE: true,
                        PARKING: false,
                        LIFT: true,
                        RUBBISH_CHUTE: false,
                        PASS_BY: false
                    }
                },
                hasAlarm: true,
                description: 'some description',
                internal: true,
                active: true,
                offerFlags: [
                    'NOT_FOR_AGENTS'
                ],
                uid: '4054826531',
                relevance: 0,
                commissioningDateIndexValue: 0,
                partnerInternalId: '8587174824471989504',
                callCenter: false,
                exclusive: false,
                obsolete: false,
                withExcerpt: false,
                freeReportAccessibility: 'FRA_NONE',
                paidReportAccessibility: 'PPA_NOT_ALLOWED_TO_BUY',
                remoteReview: {
                    onlineShow: false,
                    youtubeVideoReviewUrl: ''
                },
                socialPessimization: false,
                chargeForCallsType: 'NO_CHARGE',
                yandexRent: false,
                openPlan: false,
                raised: false,
                premium: false,
                vasAvailable: true,
                suspicious: false,
                transactionConditionsMap: {
                    HAGGLE: true,
                    MORTGAGE: true
                },
                supplyMap: {
                    GAS: false
                },
                placement: false,
                promoted: false,
                newBuilding: false,
                views: 1,
                isRealtyOffer: true
            }
        ],
        filterValue: {
            type: 'SELL',
            category: 'ROOMS',
            idsByFilterValue: {
                SELL: {
                    ROOMS: [
                        '3951760810797329920',
                        '8587174824471989504'
                    ]
                }
            },
            moreThanOneFilterValue: false
        },
        onlyDifferent: false
    },
    page: {
        queryId: '123'
    }
};

export const houseCardsStore = {
    comparison: {
        cards: [
            {
                offerId: '277964580825907201',
                trust: 'NORMAL',
                url: '//realty.test.vertis.yandex.ru/offer/277964580825907201',
                unsignedInternalUrl: '//realty.test.vertis.yandex.ru/offer/277964580825907201',
                partnerId: '1035218734',
                partnerName: 'Яндекс.Недвижимость',
                offerType: 'SELL',
                offerCategory: 'HOUSE',
                clusterId: '7722365830305549313',
                clusterHeader: false,
                clusterSize: 53,
                creationDate: '2020-10-09T05:32:17Z',
                updateDate: '2021-01-27T12:11:18Z',
                roomsTotal: 10,
                floorsTotal: 29,
                floorCovering: 'PARQUET',
                ceilingHeight: 2.75,
                area: {
                    value: 100,
                    unit: 'SQUARE_METER'
                },
                livingSpace: {
                    value: 70,
                    unit: 'SQUARE_METER'
                },
                kitchenSpace: {
                    value: 10,
                    unit: 'SQUARE_METER'
                },
                roomSpace: [
                    {
                        value: 20,
                        unit: 'SQUARE_METER'
                    },
                    {
                        value: 20,
                        unit: 'SQUARE_METER'
                    }
                ],
                price: {
                    currency: 'RUR',
                    value: 1000000,
                    period: 'WHOLE_LIFE',
                    unit: 'WHOLE_OFFER',
                    trend: 'UNCHANGED',
                    hasPriceHistory: false,
                    valuePerPart: 10000,
                    unitPerPart: 'SQUARE_METER',
                    valueForWhole: 1000000,
                    unitForWhole: 'WHOLE_OFFER',
                    price: {
                        value: 1000000,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    pricePerPart: {
                        value: 10000,
                        currency: 'RUB',
                        priceType: 'PER_METER',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    priceForWhole: {
                        value: 1000000,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    }
                },
                notForAgents: true,
                agentFee: 20.5,
                prepayment: 50,
                totalImages: 4,
                mainImages: [
                    '//avatars.mdst.yandex.net/get-realty/3423/add.15438398084649b5a2b1e05/main'
                ],
                apartment: {
                    renovation: 'COSMETIC_DONE',
                    improvements: {
                        PHONE: true,
                        INTERNET: false,
                        ROOM_FURNITURE: true,
                        KITCHEN_FURNITURE: false,
                        TELEVISION: true,
                        WASHING_MACHINE: false,
                        REFRIGERATOR: true,
                        NO_FURNITURE: false,
                        DISHWASHER: false,
                        AIRCONDITION: true,
                        FLAT_ALARM: false,
                        BUILD_IN_TECH: true
                    }
                },
                location: {
                    rgid: 193332,
                    geoId: 213,
                    subjectFederationId: 1,
                    subjectFederationRgid: 741964,
                    settlementRgid: 587795,
                    settlementGeoId: 213,
                    address: 'Москва, улица Цюрупы, 20к1',
                    geocoderAddress: 'Россия, Москва, улица Цюрупы, 20к1',
                    structuredAddress: {
                        component: [
                            {
                                value: 'Россия',
                                geoId: 225,
                                regionGraphId: '143',
                                address: 'Россия',
                                regionType: 'COUNTRY',
                                queryParams: {
                                    rgid: '143',
                                    address: 'Россия'
                                }
                            },
                            {
                                value: 'Москва',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: 'Москва',
                                regionType: 'CITY',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва'
                                }
                            },
                            {
                                value: 'улица Цюрупы',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: 'улица Цюрупы',
                                regionType: 'STREET',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва, улица Цюрупы'
                                }
                            },
                            {
                                value: '20к1',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: '20к1',
                                regionType: 'HOUSE',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва, улица Цюрупы, 20к1'
                                }
                            }
                        ]
                    },
                    point: {
                        latitude: 55.667294,
                        longitude: 37.56969,
                        precision: 'EXACT'
                    },
                    metro: {
                        metroGeoId: 20436,
                        name: 'Новые Черёмушки',
                        metroTransport: 'ON_FOOT',
                        timeToMetro: 13,
                        latitude: 55.67008,
                        longitude: 37.554493,
                        minTimeToMetro: 13,
                        lineColors: [
                            'ff8103'
                        ],
                        rgbColor: 'ff8103'
                    },
                    streetAddress: 'улица Цюрупы, 20к1',
                    metroList: [
                        {
                            metroGeoId: 20436,
                            name: 'Новые Черёмушки',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 13,
                            latitude: 55.67008,
                            longitude: 37.554493,
                            minTimeToMetro: 13,
                            lineColors: [
                                'ff8103'
                            ],
                            rgbColor: 'ff8103'
                        },
                        {
                            metroGeoId: 20435,
                            name: 'Профсоюзная',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 19,
                            latitude: 55.677933,
                            longitude: 37.562874,
                            minTimeToMetro: 17,
                            lineColors: [
                                'ff8103'
                            ],
                            rgbColor: 'ff8103'
                        },
                        {
                            metroGeoId: 20437,
                            name: 'Калужская',
                            metroTransport: 'ON_TRANSPORT',
                            timeToMetro: 18,
                            latitude: 55.656685,
                            longitude: 37.540077,
                            minTimeToMetro: 18,
                            lineColors: [
                                'ff8103'
                            ],
                            rgbColor: 'ff8103'
                        }
                    ],
                    subjectFederationName: 'Москва и МО'
                },
                house: {
                    bathroomUnit: 'SEPARATED',
                    balconyType: 'BALCONY',
                    windowView: 'YARD',
                    improvements: {
                        KITCHEN: false,
                        POOL: true,
                        BILLIARD: false,
                        SAUNA: true
                    },
                    pmg: true,
                    housePart: false,
                    toilet: 'NONE',
                    shower: 'NONE',
                    houseType: 'HOUSE'
                },
                building: {
                    builtYear: 2014,
                    buildingState: 'UNFINISHED',
                    buildingType: 'PANEL',
                    buildingSeries: 'П-44Т',
                    buildingSeriesId: '663320',
                    improvements: {
                        ELITE: true,
                        PARKING: false,
                        LIFT: true,
                        RUBBISH_CHUTE: false,
                        PASS_BY: false
                    },
                    parkingType: 'UNDERGROUND',
                    buildingId: '971807810781104884',
                    flatsCount: 245,
                    porchesCount: 4,
                    heatingType: 'CENTRAL',
                    priceStatistics: {
                        sellPricePerSquareMeter: {
                            level: 0,
                            maxLevel: 9,
                            rgbColor: '',
                            value: '0',
                            regionValueFrom: '22727',
                            regionValueTo: '2234148'
                        }
                    },
                    buildingImprovementsMap: {
                        ELITE: true,
                        PARKING: false,
                        LIFT: true,
                        RUBBISH_CHUTE: false,
                        PASS_BY: false
                    }
                },
                lot: {
                    lotArea: {
                        value: 0.77,
                        unit: 'ARE'
                    },
                    lotType: 'GARDEN'
                },
                hasAlarm: true,
                description: 'some description',
                internal: true,
                active: true,
                offerFlags: [
                    'NOT_FOR_AGENTS'
                ],
                uid: '4063818736',
                relevance: 0,
                commissioningDateIndexValue: 0,
                partnerInternalId: '277964580825907201',
                callCenter: false,
                exclusive: false,
                obsolete: false,
                withExcerpt: false,
                freeReportAccessibility: 'FRA_NONE',
                paidReportAccessibility: 'PPA_NOT_ALLOWED_TO_BUY',
                remoteReview: {
                    onlineShow: false,
                    youtubeVideoReviewUrl: ''
                },
                socialPessimization: false,
                chargeForCallsType: 'NO_CHARGE',
                yandexRent: false,
                primarySaleV2: false,
                raised: false,
                premium: false,
                vasAvailable: true,
                openPlan: false,
                suspicious: false,
                transactionConditionsMap: {
                    HAGGLE: true,
                    MORTGAGE: true
                },
                supplyMap: {
                    HEATING: false,
                    WATER: true,
                    SEWERAGE: false,
                    ELECTRICITY: true,
                    GAS: false
                },
                placement: false,
                promoted: false,
                newBuilding: false,
                views: 2,
                isRealtyOffer: true
            },
            {
                offerId: '1482178841084579072',
                trust: 'NORMAL',
                url: '//realty.test.vertis.yandex.ru/offer/1482178841084579072',
                unsignedInternalUrl: '//realty.test.vertis.yandex.ru/offer/1482178841084579072',
                partnerId: '1035218734',
                partnerName: 'Яндекс.Недвижимость',
                offerType: 'SELL',
                offerCategory: 'HOUSE',
                clusterId: '7722365830305549313',
                clusterHeader: false,
                clusterSize: 53,
                creationDate: '2020-10-09T05:32:17Z',
                updateDate: '2021-01-25T10:42:30Z',
                roomsTotal: 10,
                floorsTotal: 29,
                floorCovering: 'PARQUET',
                ceilingHeight: 2.75,
                area: {
                    value: 100,
                    unit: 'SQUARE_METER'
                },
                livingSpace: {
                    value: 70,
                    unit: 'SQUARE_METER'
                },
                kitchenSpace: {
                    value: 10,
                    unit: 'SQUARE_METER'
                },
                roomSpace: [
                    {
                        value: 20,
                        unit: 'SQUARE_METER'
                    },
                    {
                        value: 20,
                        unit: 'SQUARE_METER'
                    }
                ],
                price: {
                    currency: 'RUR',
                    value: 1000000,
                    period: 'WHOLE_LIFE',
                    unit: 'WHOLE_OFFER',
                    trend: 'UNCHANGED',
                    hasPriceHistory: false,
                    valuePerPart: 10000,
                    unitPerPart: 'SQUARE_METER',
                    valueForWhole: 1000000,
                    unitForWhole: 'WHOLE_OFFER',
                    price: {
                        value: 1000000,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    pricePerPart: {
                        value: 10000,
                        currency: 'RUB',
                        priceType: 'PER_METER',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    priceForWhole: {
                        value: 1000000,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    }
                },
                notForAgents: true,
                agentFee: 20.5,
                prepayment: 50,
                totalImages: 4,
                mainImages: [
                    '//avatars.mdst.yandex.net/get-realty/3423/add.15438398084649b5a2b1e05/main'
                ],
                apartment: {
                    renovation: 'COSMETIC_DONE',
                    improvements: {
                        PHONE: true,
                        INTERNET: false,
                        ROOM_FURNITURE: true,
                        KITCHEN_FURNITURE: false,
                        TELEVISION: true,
                        WASHING_MACHINE: false,
                        REFRIGERATOR: true,
                        NO_FURNITURE: false,
                        DISHWASHER: false,
                        AIRCONDITION: true,
                        FLAT_ALARM: false,
                        BUILD_IN_TECH: true
                    }
                },
                location: {
                    rgid: 193332,
                    geoId: 213,
                    subjectFederationId: 1,
                    subjectFederationRgid: 741964,
                    settlementRgid: 587795,
                    settlementGeoId: 213,
                    address: 'Москва, улица Цюрупы, 20к1',
                    geocoderAddress: 'Россия, Москва, улица Цюрупы, 20к1',
                    structuredAddress: {
                        component: [
                            {
                                value: 'Россия',
                                geoId: 225,
                                regionGraphId: '143',
                                address: 'Россия',
                                regionType: 'COUNTRY',
                                queryParams: {
                                    rgid: '143',
                                    address: 'Россия'
                                }
                            },
                            {
                                value: 'Москва',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: 'Москва',
                                regionType: 'CITY',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва'
                                }
                            },
                            {
                                value: 'улица Цюрупы',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: 'улица Цюрупы',
                                regionType: 'STREET',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва, улица Цюрупы'
                                }
                            },
                            {
                                value: '20к1',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: '20к1',
                                regionType: 'HOUSE',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва, улица Цюрупы, 20к1'
                                }
                            }
                        ]
                    },
                    point: {
                        latitude: 55.667294,
                        longitude: 37.56969,
                        precision: 'EXACT'
                    },
                    metro: {
                        metroGeoId: 20436,
                        name: 'Новые Черёмушки',
                        metroTransport: 'ON_FOOT',
                        timeToMetro: 13,
                        latitude: 55.67008,
                        longitude: 37.554493,
                        minTimeToMetro: 13,
                        lineColors: [
                            'ff8103'
                        ],
                        rgbColor: 'ff8103'
                    },
                    streetAddress: 'улица Цюрупы, 20к1',
                    metroList: [
                        {
                            metroGeoId: 20436,
                            name: 'Новые Черёмушки',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 13,
                            latitude: 55.67008,
                            longitude: 37.554493,
                            minTimeToMetro: 13,
                            lineColors: [
                                'ff8103'
                            ],
                            rgbColor: 'ff8103'
                        },
                        {
                            metroGeoId: 20435,
                            name: 'Профсоюзная',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 19,
                            latitude: 55.677933,
                            longitude: 37.562874,
                            minTimeToMetro: 17,
                            lineColors: [
                                'ff8103'
                            ],
                            rgbColor: 'ff8103'
                        },
                        {
                            metroGeoId: 20437,
                            name: 'Калужская',
                            metroTransport: 'ON_TRANSPORT',
                            timeToMetro: 18,
                            latitude: 55.656685,
                            longitude: 37.540077,
                            minTimeToMetro: 18,
                            lineColors: [
                                'ff8103'
                            ],
                            rgbColor: 'ff8103'
                        }
                    ],
                    subjectFederationName: 'Москва и МО'
                },
                house: {
                    bathroomUnit: 'SEPARATED',
                    balconyType: 'BALCONY',
                    windowView: 'YARD',
                    improvements: {
                        KITCHEN: false,
                        POOL: true,
                        BILLIARD: false,
                        SAUNA: true
                    },
                    pmg: true,
                    housePart: false,
                    toilet: 'NONE',
                    shower: 'NONE',
                    houseType: 'HOUSE'
                },
                building: {
                    builtYear: 2014,
                    buildingState: 'UNFINISHED',
                    buildingType: 'PANEL',
                    buildingSeries: 'П-44Т',
                    buildingSeriesId: '663320',
                    improvements: {
                        ELITE: true,
                        PARKING: false,
                        LIFT: true,
                        RUBBISH_CHUTE: false,
                        PASS_BY: false
                    },
                    parkingType: 'UNDERGROUND',
                    buildingId: '971807810781104884',
                    flatsCount: 245,
                    porchesCount: 4,
                    heatingType: 'CENTRAL',
                    priceStatistics: {
                        sellPricePerSquareMeter: {
                            level: 0,
                            maxLevel: 9,
                            rgbColor: '',
                            value: '0',
                            regionValueFrom: '22727',
                            regionValueTo: '2234148'
                        }
                    },
                    buildingImprovementsMap: {
                        ELITE: true,
                        PARKING: false,
                        LIFT: true,
                        RUBBISH_CHUTE: false,
                        PASS_BY: false
                    }
                },
                lot: {
                    lotArea: {
                        value: 0.77,
                        unit: 'ARE'
                    },
                    lotType: 'GARDEN'
                },
                hasAlarm: true,
                description: 'some description',
                internal: true,
                active: true,
                offerFlags: [
                    'NOT_FOR_AGENTS'
                ],
                uid: '4063646088',
                relevance: 0,
                commissioningDateIndexValue: 0,
                partnerInternalId: '1482178841084579072',
                callCenter: false,
                exclusive: false,
                obsolete: false,
                withExcerpt: false,
                freeReportAccessibility: 'FRA_NONE',
                paidReportAccessibility: 'PPA_NOT_ALLOWED_TO_BUY',
                remoteReview: {
                    onlineShow: false,
                    youtubeVideoReviewUrl: ''
                },
                socialPessimization: false,
                chargeForCallsType: 'NO_CHARGE',
                yandexRent: false,
                primarySaleV2: false,
                raised: false,
                premium: false,
                vasAvailable: true,
                openPlan: false,
                suspicious: false,
                transactionConditionsMap: {
                    HAGGLE: true,
                    MORTGAGE: true
                },
                supplyMap: {
                    HEATING: false,
                    WATER: true,
                    SEWERAGE: false,
                    ELECTRICITY: true,
                    GAS: false
                },
                placement: false,
                promoted: false,
                newBuilding: false,
                views: 2,
                isRealtyOffer: true
            }
        ],
        filterValue: {
            type: 'SELL',
            category: 'HOUSE',
            idsByFilterValue: {
                SELL: {
                    HOUSE: [
                        '277964580825907201',
                        '1482178841084579072'
                    ]
                }
            },
            moreThanOneFilterValue: false
        },
        onlyDifferent: false
    },
    page: {
        queryId: '123'
    }
};

export const lotCardsStore = {
    comparison: {
        cards: [
            {
                offerId: '3442895372014374145',
                trust: 'NORMAL',
                url: '//realty.test.vertis.yandex.ru/offer/3442895372014374145',
                unsignedInternalUrl: '//realty.test.vertis.yandex.ru/offer/3442895372014374145',
                partnerId: '1035218734',
                partnerName: 'Яндекс.Недвижимость',
                offerType: 'SELL',
                offerCategory: 'LOT',
                clusterId: '3442895372014374145',
                clusterHeader: true,
                clusterSize: 16,
                creationDate: '2020-08-07T13:21:25Z',
                updateDate: '2020-10-08T15:06:18Z',
                area: {
                    value: 77,
                    unit: 'SQUARE_METER'
                },
                price: {
                    currency: 'RUR',
                    value: 1000000,
                    period: 'WHOLE_LIFE',
                    unit: 'WHOLE_OFFER',
                    trend: 'UNCHANGED',
                    hasPriceHistory: false,
                    valuePerPart: 1298701,
                    unitPerPart: 'ARE',
                    valueForWhole: 1000000,
                    unitForWhole: 'WHOLE_OFFER',
                    price: {
                        value: 1000000,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    pricePerPart: {
                        value: 1298701,
                        currency: 'RUB',
                        priceType: 'PER_ARE',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    priceForWhole: {
                        value: 1000000,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    }
                },
                notForAgents: true,
                agentFee: 20.5,
                prepayment: 50,
                totalImages: 2,
                mainImages: [
                    '//avatars.mdst.yandex.net/get-realty/3423/add.15438398084649b5a2b1e05/main'
                ],
                apartment: {},
                location: {
                    rgid: 193332,
                    geoId: 213,
                    subjectFederationId: 1,
                    subjectFederationRgid: 741964,
                    settlementRgid: 587795,
                    settlementGeoId: 213,
                    address: 'Москва, улица Цюрупы, 20к1',
                    geocoderAddress: 'Россия, Москва, улица Цюрупы, 20к1',
                    structuredAddress: {
                        component: [
                            {
                                value: 'Россия',
                                geoId: 225,
                                regionGraphId: '143',
                                address: 'Россия',
                                regionType: 'COUNTRY',
                                queryParams: {
                                    rgid: '143',
                                    address: 'Россия'
                                }
                            },
                            {
                                value: 'Москва',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: 'Москва',
                                regionType: 'CITY',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва'
                                }
                            },
                            {
                                value: 'улица Цюрупы',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: 'улица Цюрупы',
                                regionType: 'STREET',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва, улица Цюрупы'
                                }
                            },
                            {
                                value: '20к1',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: '20к1',
                                regionType: 'HOUSE',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва, улица Цюрупы, 20к1'
                                }
                            }
                        ]
                    },
                    point: {
                        latitude: 55.667294,
                        longitude: 37.56969,
                        precision: 'EXACT'
                    },
                    metro: {
                        metroGeoId: 20436,
                        name: 'Новые Черёмушки',
                        metroTransport: 'ON_FOOT',
                        timeToMetro: 13,
                        latitude: 55.67008,
                        longitude: 37.554493,
                        minTimeToMetro: 13,
                        lineColors: [
                            'ff8103'
                        ],
                        rgbColor: 'ff8103'
                    },
                    streetAddress: 'улица Цюрупы, 20к1',
                    metroList: [
                        {
                            metroGeoId: 20436,
                            name: 'Новые Черёмушки',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 13,
                            latitude: 55.67008,
                            longitude: 37.554493,
                            minTimeToMetro: 13,
                            lineColors: [
                                'ff8103'
                            ],
                            rgbColor: 'ff8103'
                        },
                        {
                            metroGeoId: 20435,
                            name: 'Профсоюзная',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 19,
                            latitude: 55.677933,
                            longitude: 37.562874,
                            minTimeToMetro: 17,
                            lineColors: [
                                'ff8103'
                            ],
                            rgbColor: 'ff8103'
                        },
                        {
                            metroGeoId: 20437,
                            name: 'Калужская',
                            metroTransport: 'ON_TRANSPORT',
                            timeToMetro: 18,
                            latitude: 55.656685,
                            longitude: 37.540077,
                            minTimeToMetro: 18,
                            lineColors: [
                                'ff8103'
                            ],
                            rgbColor: 'ff8103'
                        }
                    ],
                    subjectFederationName: 'Москва и МО'
                },
                house: {
                    housePart: false
                },
                building: {
                    buildingSeries: 'П-44Т',
                    buildingSeriesId: '663320',
                    buildingId: '971807810781104884',
                    flatsCount: 245,
                    porchesCount: 4,
                    heatingType: 'CENTRAL',
                    priceStatistics: {
                        sellPricePerSquareMeter: {
                            level: 0,
                            maxLevel: 9,
                            rgbColor: '',
                            value: '0',
                            regionValueFrom: '22727',
                            regionValueTo: '2234148'
                        }
                    }
                },
                lot: {
                    lotArea: {
                        value: 0.77,
                        unit: 'ARE'
                    },
                    lotType: 'GARDEN'
                },
                description: 'some description',
                internal: true,
                active: true,
                offerFlags: [
                    'NOT_FOR_AGENTS'
                ],
                uid: '4055997783',
                relevance: 0,
                partnerInternalId: '3442895372014374145',
                callCenter: false,
                exclusive: false,
                obsolete: false,
                withExcerpt: false,
                freeReportAccessibility: 'FRA_NONE',
                paidReportAccessibility: 'PPA_NOT_ALLOWED_TO_BUY',
                remoteReview: {
                    onlineShow: false,
                    youtubeVideoReviewUrl: ''
                },
                socialPessimization: false,
                chargeForCallsType: 'NO_CHARGE',
                trustedOfferInfo: {
                    isFullTrustedOwner: false,
                    ownerTrustedStatus: 'NOT_LINKED_MOSRU',
                    isCadastrPersonMatched: false
                },
                yandexRent: false,
                primarySaleV2: false,
                raised: false,
                premium: false,
                vasAvailable: true,
                openPlan: false,
                suspicious: false,
                transactionConditionsMap: {
                    HAGGLE: true,
                    MORTGAGE: true
                },
                supplyMap: {
                    GAS: false
                },
                placement: false,
                promoted: false,
                newBuilding: false,
                views: 1,
                isRealtyOffer: true
            },
            {
                offerId: '8256136832824336129',
                trust: 'NORMAL',
                url: '//realty.test.vertis.yandex.ru/offer/8256136832824336129',
                unsignedInternalUrl: '//realty.test.vertis.yandex.ru/offer/8256136832824336129',
                partnerId: '1035218734',
                partnerName: 'Яндекс.Недвижимость',
                offerType: 'SELL',
                offerCategory: 'LOT',
                clusterId: '8598853159662743553',
                clusterHeader: false,
                clusterSize: 17,
                creationDate: '2020-08-07T13:21:25Z',
                updateDate: '2020-11-17T13:50:19Z',
                area: {
                    value: 77,
                    unit: 'SQUARE_METER'
                },
                price: {
                    currency: 'RUR',
                    value: 1000000,
                    period: 'WHOLE_LIFE',
                    unit: 'WHOLE_OFFER',
                    trend: 'UNCHANGED',
                    hasPriceHistory: false,
                    valuePerPart: 1298701,
                    unitPerPart: 'ARE',
                    valueForWhole: 1000000,
                    unitForWhole: 'WHOLE_OFFER',
                    price: {
                        value: 1000000,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    pricePerPart: {
                        value: 1298701,
                        currency: 'RUB',
                        priceType: 'PER_ARE',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    priceForWhole: {
                        value: 1000000,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    }
                },
                notForAgents: true,
                agentFee: 20.5,
                prepayment: 50,
                totalImages: 2,
                mainImages: [
                    '//avatars.mdst.yandex.net/get-realty/3423/add.15438398084649b5a2b1e05/main'
                ],
                apartment: {},
                location: {
                    rgid: 193332,
                    geoId: 213,
                    subjectFederationId: 1,
                    subjectFederationRgid: 741964,
                    settlementRgid: 587795,
                    settlementGeoId: 213,
                    address: 'Москва, улица Цюрупы, 20к1',
                    geocoderAddress: 'Россия, Москва, улица Цюрупы, 20к1',
                    structuredAddress: {
                        component: [
                            {
                                value: 'Россия',
                                geoId: 225,
                                regionGraphId: '143',
                                address: 'Россия',
                                regionType: 'COUNTRY',
                                queryParams: {
                                    rgid: '143',
                                    address: 'Россия'
                                }
                            },
                            {
                                value: 'Москва',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: 'Москва',
                                regionType: 'CITY',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва'
                                }
                            },
                            {
                                value: 'улица Цюрупы',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: 'улица Цюрупы',
                                regionType: 'STREET',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва, улица Цюрупы'
                                }
                            },
                            {
                                value: '20к1',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: '20к1',
                                regionType: 'HOUSE',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва, улица Цюрупы, 20к1'
                                }
                            }
                        ]
                    },
                    point: {
                        latitude: 55.667294,
                        longitude: 37.56969,
                        precision: 'EXACT'
                    },
                    metro: {
                        metroGeoId: 20436,
                        name: 'Новые Черёмушки',
                        metroTransport: 'ON_FOOT',
                        timeToMetro: 13,
                        latitude: 55.67008,
                        longitude: 37.554493,
                        minTimeToMetro: 13,
                        lineColors: [
                            'ff8103'
                        ],
                        rgbColor: 'ff8103'
                    },
                    streetAddress: 'улица Цюрупы, 20к1',
                    metroList: [
                        {
                            metroGeoId: 20436,
                            name: 'Новые Черёмушки',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 13,
                            latitude: 55.67008,
                            longitude: 37.554493,
                            minTimeToMetro: 13,
                            lineColors: [
                                'ff8103'
                            ],
                            rgbColor: 'ff8103'
                        },
                        {
                            metroGeoId: 20435,
                            name: 'Профсоюзная',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 19,
                            latitude: 55.677933,
                            longitude: 37.562874,
                            minTimeToMetro: 17,
                            lineColors: [
                                'ff8103'
                            ],
                            rgbColor: 'ff8103'
                        },
                        {
                            metroGeoId: 20437,
                            name: 'Калужская',
                            metroTransport: 'ON_TRANSPORT',
                            timeToMetro: 18,
                            latitude: 55.656685,
                            longitude: 37.540077,
                            minTimeToMetro: 18,
                            lineColors: [
                                'ff8103'
                            ],
                            rgbColor: 'ff8103'
                        }
                    ],
                    subjectFederationName: 'Москва и МО'
                },
                house: {
                    housePart: false
                },
                building: {
                    buildingSeries: 'П-44Т',
                    buildingSeriesId: '663320',
                    buildingId: '971807810781104884',
                    flatsCount: 245,
                    porchesCount: 4,
                    heatingType: 'CENTRAL',
                    priceStatistics: {
                        sellPricePerSquareMeter: {
                            level: 0,
                            maxLevel: 9,
                            rgbColor: '',
                            value: '0',
                            regionValueFrom: '22727',
                            regionValueTo: '2234148'
                        }
                    }
                },
                lot: {
                    lotArea: {
                        value: 0.77,
                        unit: 'ARE'
                    },
                    lotType: 'GARDEN'
                },
                description: 'some description',
                internal: true,
                active: true,
                offerFlags: [
                    'NOT_FOR_AGENTS'
                ],
                uid: '4058999306',
                relevance: 0,
                partnerInternalId: '8256136832824336129',
                callCenter: false,
                exclusive: false,
                obsolete: false,
                withExcerpt: false,
                freeReportAccessibility: 'FRA_NONE',
                paidReportAccessibility: 'PPA_NOT_ALLOWED_TO_BUY',
                remoteReview: {
                    onlineShow: false,
                    youtubeVideoReviewUrl: ''
                },
                socialPessimization: false,
                chargeForCallsType: 'NO_CHARGE',
                yandexRent: false,
                openPlan: false,
                primarySaleV2: false,
                raised: false,
                premium: false,
                vasAvailable: true,
                suspicious: false,
                transactionConditionsMap: {
                    HAGGLE: true,
                    MORTGAGE: true
                },
                supplyMap: {
                    GAS: false
                },
                placement: false,
                promoted: false,
                newBuilding: false,
                views: 1,
                isRealtyOffer: true
            }
        ],
        filterValue: {
            type: 'SELL',
            category: 'LOT',
            idsByFilterValue: {
                SELL: {
                    LOT: [
                        '3442895372014374145',
                        '8256136832824336129'
                    ]
                }
            },
            moreThanOneFilterValue: false
        },
        onlyDifferent: false
    },
    page: {
        queryId: '123'
    }
};

export const garageCardsStore = {
    comparison: {
        cards: [
            {
                offerId: '3098969087362891521',
                trust: 'NORMAL',
                url: '//realty.test.vertis.yandex.ru/offer/3098969087362891521',
                unsignedInternalUrl: '//realty.test.vertis.yandex.ru/offer/3098969087362891521',
                partnerId: '1035218734',
                partnerName: 'Яндекс.Недвижимость',
                offerType: 'SELL',
                offerCategory: 'GARAGE',
                clusterId: '3098969087362891521',
                clusterHeader: true,
                clusterSize: 2,
                creationDate: '2021-01-13T12:49:42Z',
                price: {
                    currency: 'RUR',
                    value: 9000000,
                    period: 'WHOLE_LIFE',
                    unit: 'WHOLE_OFFER',
                    trend: 'UNCHANGED',
                    hasPriceHistory: false,
                    valueForWhole: 9000000,
                    unitForWhole: 'WHOLE_OFFER',
                    price: {
                        value: 9000000,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    priceForWhole: {
                        value: 9000000,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    }
                },
                notForAgents: false,
                totalImages: 2,
                mainImages: [
                    '//avatars.mdst.yandex.net/get-realty/3423/add.15438398084649b5a2b1e05/main'
                ],
                apartment: {},
                location: {
                    rgid: 193332,
                    geoId: 213,
                    subjectFederationId: 1,
                    subjectFederationRgid: 741964,
                    settlementRgid: 587795,
                    settlementGeoId: 213,
                    address: 'Москва, улица Цюрупы, 20к1',
                    geocoderAddress: 'Россия, Москва, улица Цюрупы, 20к1',
                    structuredAddress: {
                        component: [
                            {
                                value: 'Россия',
                                geoId: 225,
                                regionGraphId: '143',
                                address: 'Россия',
                                regionType: 'COUNTRY',
                                queryParams: {
                                    rgid: '143',
                                    address: 'Россия'
                                }
                            },
                            {
                                value: 'Москва',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: 'Москва',
                                regionType: 'CITY',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва'
                                }
                            },
                            {
                                value: 'улица Цюрупы',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: 'улица Цюрупы',
                                regionType: 'STREET',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва, улица Цюрупы'
                                }
                            },
                            {
                                value: '20к1',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: '20к1',
                                regionType: 'HOUSE',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва, улица Цюрупы, 20к1'
                                }
                            }
                        ]
                    },
                    point: {
                        latitude: 55.667294,
                        longitude: 37.56969,
                        precision: 'EXACT'
                    },
                    metro: {
                        metroGeoId: 20436,
                        name: 'Новые Черёмушки',
                        metroTransport: 'ON_FOOT',
                        timeToMetro: 13,
                        latitude: 55.67008,
                        longitude: 37.554493,
                        minTimeToMetro: 13,
                        lineColors: [
                            'ff8103'
                        ],
                        rgbColor: 'ff8103'
                    },
                    streetAddress: 'улица Цюрупы, 20к1',
                    metroList: [
                        {
                            metroGeoId: 20436,
                            name: 'Новые Черёмушки',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 13,
                            latitude: 55.67008,
                            longitude: 37.554493,
                            minTimeToMetro: 13,
                            lineColors: [
                                'ff8103'
                            ],
                            rgbColor: 'ff8103'
                        },
                        {
                            metroGeoId: 20435,
                            name: 'Профсоюзная',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 19,
                            latitude: 55.677933,
                            longitude: 37.562874,
                            minTimeToMetro: 17,
                            lineColors: [
                                'ff8103'
                            ],
                            rgbColor: 'ff8103'
                        },
                        {
                            metroGeoId: 20437,
                            name: 'Калужская',
                            metroTransport: 'ON_TRANSPORT',
                            timeToMetro: 18,
                            latitude: 55.656685,
                            longitude: 37.540077,
                            minTimeToMetro: 18,
                            lineColors: [
                                'ff8103'
                            ],
                            rgbColor: 'ff8103'
                        }
                    ],
                    subjectFederationName: 'Москва и МО'
                },
                house: {
                    housePart: false
                },
                building: {
                    buildingType: 'BRICK',
                    parkingType: 'OPEN',
                    heatingType: 'UNKNOWN'
                },
                description: 'some description',
                internal: true,
                active: true,
                uid: '4062839112',
                relevance: 0,
                partnerInternalId: '3098969087362891521',
                garage: {
                    garageType: 'BOX',
                    ownershipType: 'PRIVATE'
                },
                callCenter: false,
                exclusive: false,
                obsolete: false,
                withExcerpt: false,
                freeReportAccessibility: 'FRA_NONE',
                paidReportAccessibility: 'PPA_NOT_ALLOWED_TO_BUY',
                remoteReview: {
                    onlineShow: false,
                    youtubeVideoReviewUrl: ''
                },
                socialPessimization: false,
                chargeForCallsType: 'NO_CHARGE',
                yandexRent: false,
                openPlan: false,
                raised: false,
                premium: false,
                vasAvailable: true,
                suspicious: false,
                placement: false,
                promoted: false,
                newBuilding: false,
                views: 2,
                isRealtyOffer: true
            },
            {
                offerId: '6987111862147213825',
                trust: 'NORMAL',
                url: '//realty.test.vertis.yandex.ru/offer/6987111862147213825',
                unsignedInternalUrl: '//realty.test.vertis.yandex.ru/offer/6987111862147213825',
                partnerId: '1035218734',
                partnerName: 'Яндекс.Недвижимость',
                offerType: 'SELL',
                offerCategory: 'GARAGE',
                clusterId: '6987111862147213825',
                clusterHeader: true,
                clusterSize: 1,
                creationDate: '2021-01-13T12:43:27Z',
                price: {
                    currency: 'RUR',
                    value: 8000000,
                    period: 'WHOLE_LIFE',
                    unit: 'WHOLE_OFFER',
                    trend: 'UNCHANGED',
                    hasPriceHistory: false,
                    valueForWhole: 8000000,
                    unitForWhole: 'WHOLE_OFFER',
                    price: {
                        value: 8000000,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    priceForWhole: {
                        value: 8000000,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    }
                },
                notForAgents: false,
                totalImages: 2,
                mainImages: [
                    '//avatars.mdst.yandex.net/get-realty/3423/add.15438398084649b5a2b1e05/main'
                ],
                apartment: {},
                location: {
                    rgid: 193332,
                    geoId: 213,
                    subjectFederationId: 1,
                    subjectFederationRgid: 741964,
                    settlementRgid: 587795,
                    settlementGeoId: 213,
                    address: 'Москва, улица Цюрупы, 20к1',
                    geocoderAddress: 'Россия, Москва, улица Цюрупы, 20к1',
                    structuredAddress: {
                        component: [
                            {
                                value: 'Россия',
                                geoId: 225,
                                regionGraphId: '143',
                                address: 'Россия',
                                regionType: 'COUNTRY',
                                queryParams: {
                                    rgid: '143',
                                    address: 'Россия'
                                }
                            },
                            {
                                value: 'Москва',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: 'Москва',
                                regionType: 'CITY',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва'
                                }
                            },
                            {
                                value: 'улица Цюрупы',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: 'улица Цюрупы',
                                regionType: 'STREET',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва, улица Цюрупы'
                                }
                            },
                            {
                                value: '20к1',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: '20к1',
                                regionType: 'HOUSE',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва, улица Цюрупы, 20к1'
                                }
                            }
                        ]
                    },
                    point: {
                        latitude: 55.667294,
                        longitude: 37.56969,
                        precision: 'EXACT'
                    },
                    metro: {
                        metroGeoId: 20436,
                        name: 'Новые Черёмушки',
                        metroTransport: 'ON_FOOT',
                        timeToMetro: 13,
                        latitude: 55.67008,
                        longitude: 37.554493,
                        minTimeToMetro: 13,
                        lineColors: [
                            'ff8103'
                        ],
                        rgbColor: 'ff8103'
                    },
                    streetAddress: 'улица Цюрупы, 20к1',
                    metroList: [
                        {
                            metroGeoId: 20436,
                            name: 'Новые Черёмушки',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 13,
                            latitude: 55.67008,
                            longitude: 37.554493,
                            minTimeToMetro: 13,
                            lineColors: [
                                'ff8103'
                            ],
                            rgbColor: 'ff8103'
                        },
                        {
                            metroGeoId: 20435,
                            name: 'Профсоюзная',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 19,
                            latitude: 55.677933,
                            longitude: 37.562874,
                            minTimeToMetro: 17,
                            lineColors: [
                                'ff8103'
                            ],
                            rgbColor: 'ff8103'
                        },
                        {
                            metroGeoId: 20437,
                            name: 'Калужская',
                            metroTransport: 'ON_TRANSPORT',
                            timeToMetro: 18,
                            latitude: 55.656685,
                            longitude: 37.540077,
                            minTimeToMetro: 18,
                            lineColors: [
                                'ff8103'
                            ],
                            rgbColor: 'ff8103'
                        }
                    ],
                    subjectFederationName: 'Москва и МО'
                },
                house: {
                    housePart: false
                },
                building: {
                    buildingType: 'BRICK',
                    parkingType: 'OPEN',
                    heatingType: 'UNKNOWN'
                },
                description: 'some description',
                internal: true,
                active: true,
                uid: '4062839112',
                relevance: 0,
                partnerInternalId: '6987111862147213825',
                garage: {
                    garageType: 'BOX',
                    ownershipType: 'PRIVATE'
                },
                callCenter: false,
                exclusive: true,
                obsolete: false,
                withExcerpt: false,
                freeReportAccessibility: 'FRA_NONE',
                paidReportAccessibility: 'PPA_NOT_ALLOWED_TO_BUY',
                remoteReview: {
                    onlineShow: false,
                    youtubeVideoReviewUrl: ''
                },
                socialPessimization: false,
                chargeForCallsType: 'NO_CHARGE',
                yandexRent: false,
                openPlan: false,
                raised: false,
                premium: false,
                vasAvailable: true,
                suspicious: false,
                placement: false,
                promoted: false,
                newBuilding: false,
                views: 1,
                isRealtyOffer: true
            }
        ],
        filterValue: {
            type: 'SELL',
            category: 'GARAGE',
            idsByFilterValue: {
                SELL: {
                    GARAGE: [
                        '3098969087362891521',
                        '6987111862147213825'
                    ]
                }
            },
            moreThanOneFilterValue: false
        },
        onlyDifferent: false
    },
    page: {
        queryId: '123'
    },
    user: {
        comparison: []
    }
};

export const commercialCardsStore = {
    comparison: {
        cards: [
            {
                offerId: '4728410863441080523',
                trust: 'NORMAL',
                url: '//realty.test.vertis.yandex.ru/offer/4728410863441080523',
                unsignedInternalUrl: '//realty.test.vertis.yandex.ru/offer/4728410863441080523',
                partnerId: '1069251798',
                partnerName: 'бф5',
                offerType: 'SELL',
                offerCategory: 'COMMERCIAL',
                clusterId: '4728410863441080523',
                clusterHeader: true,
                clusterSize: 1,
                creationDate: '2020-08-19T21:00:01Z',
                updateDate: '2020-10-09T10:00:01Z',
                floorsTotal: 73,
                floorsOffered: [
                    11
                ],
                area: {
                    value: 187.8,
                    unit: 'SQUARE_METER'
                },
                price: {
                    currency: 'RUR',
                    value: 124658448,
                    period: 'WHOLE_LIFE',
                    unit: 'WHOLE_OFFER',
                    trend: 'UNCHANGED',
                    hasPriceHistory: false,
                    valuePerPart: 663783,
                    unitPerPart: 'SQUARE_METER',
                    valueForWhole: 124658449,
                    unitForWhole: 'WHOLE_OFFER',
                    price: {
                        value: 124658448,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    pricePerPart: {
                        value: 663783,
                        currency: 'RUB',
                        priceType: 'PER_METER',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    priceForWhole: {
                        value: 124658449,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    }
                },
                notForAgents: false,
                totalImages: 15,
                mainImages: [
                    '//avatars.mdst.yandex.net/get-realty/2991/w1842/main'
                ],
                apartment: {
                    renovation: 'COSMETIC_DONE',
                    improvements: {
                        INTERNET: true,
                        AIRCONDITION: true,
                        VENTILATION: true,
                        FIRE_ALARM: true
                    }
                },
                commercial: {
                    commercialTypes: [
                        'OFFICE'
                    ],
                    commercialBuildingType: 'BUSINESS_CENTER'
                },
                location: {
                    rgid: 197177,
                    geoId: 213,
                    subjectFederationId: 1,
                    subjectFederationRgid: 741964,
                    settlementRgid: 587795,
                    settlementGeoId: 213,
                    address: 'Москва, Пресненская набережная, 8с1',
                    geocoderAddress: 'Россия, Москва, Пресненская набережная, 8с1',
                    structuredAddress: {
                        component: [
                            {
                                value: 'Россия',
                                geoId: 225,
                                regionGraphId: '143',
                                address: 'Россия',
                                regionType: 'COUNTRY',
                                queryParams: {
                                    rgid: '143',
                                    address: 'Россия'
                                }
                            },
                            {
                                value: 'Москва',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: 'Москва',
                                regionType: 'CITY',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва'
                                }
                            },
                            {
                                value: 'Пресненская набережная',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: 'Пресненская набережная',
                                regionType: 'STREET',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва, Пресненская набережная'
                                }
                            },
                            {
                                value: '8с1',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: '8с1',
                                regionType: 'HOUSE',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва, Пресненская набережная, 8с1'
                                }
                            }
                        ]
                    },
                    point: {
                        latitude: 55.747528,
                        longitude: 37.538673,
                        precision: 'EXACT'
                    },
                    metro: {
                        metroGeoId: 115085,
                        name: 'Деловой Центр',
                        metroTransport: 'ON_FOOT',
                        timeToMetro: 1,
                        latitude: 55.74903,
                        longitude: 37.539913,
                        minTimeToMetro: 1,
                        lineColors: [
                            '6fc1ba'
                        ],
                        rgbColor: '6fc1ba'
                    },
                    streetAddress: 'Пресненская набережная, 8с1',
                    metroList: [
                        {
                            metroGeoId: 115085,
                            name: 'Деловой Центр',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 1,
                            latitude: 55.74903,
                            longitude: 37.539913,
                            minTimeToMetro: 1,
                            lineColors: [
                                '6fc1ba'
                            ],
                            rgbColor: '6fc1ba'
                        },
                        {
                            metroGeoId: 98560,
                            name: 'Международная',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 3,
                            latitude: 55.748314,
                            longitude: 37.534264,
                            minTimeToMetro: 3,
                            lineColors: [
                                '099dd4'
                            ],
                            rgbColor: '099dd4'
                        },
                        {
                            metroGeoId: 98559,
                            name: 'Выставочная',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 6,
                            latitude: 55.750076,
                            longitude: 37.54139,
                            minTimeToMetro: 6,
                            lineColors: [
                                '099dd4'
                            ],
                            rgbColor: '099dd4'
                        },
                        {
                            metroGeoId: 152947,
                            name: 'Деловой Центр',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 7,
                            latitude: 55.74709,
                            longitude: 37.53209,
                            minTimeToMetro: 7,
                            lineColors: [
                                'ffa8af'
                            ],
                            rgbColor: 'ffa8af'
                        },
                        {
                            metroGeoId: 218566,
                            name: 'Тестовская',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 15,
                            latitude: 55.75415,
                            longitude: 37.531693,
                            minTimeToMetro: 15,
                            lineColors: [
                                'ed9f2d'
                            ],
                            rgbColor: 'ed9f2d'
                        },
                        {
                            metroGeoId: 152948,
                            name: 'Шелепиха',
                            metroTransport: 'ON_TRANSPORT',
                            timeToMetro: 8,
                            latitude: 55.757122,
                            longitude: 37.524075,
                            minTimeToMetro: 8,
                            lineColors: [
                                'ffa8af',
                                'ffe400',
                                '6fc1ba'
                            ],
                            rgbColor: 'ffa8af'
                        }
                    ],
                    subjectFederationName: 'Москва и МО'
                },
                house: {
                    apartments: true,
                    housePart: false
                },
                building: {
                    buildingState: 'HAND_OVER',
                    improvements: {
                        LIFT: true,
                        ACCESS_CONTROL_SYSTEM: true,
                        TWENTY_FOUR_SEVEN: true,
                        SECURITY: true
                    },
                    parkingType: 'OPEN',
                    siteId: 59066,
                    siteName: 'Город Столиц',
                    siteDisplayName: 'МФК «Город Столиц»',
                    developerIds: [
                        268706,
                        23314
                    ],
                    officeClass: 'A_PLUS',
                    houseId: '59086',
                    heatingType: 'UNKNOWN',
                    buildingImprovementsMap: {
                        LIFT: true,
                        ACCESS_CONTROL_SYSTEM: true,
                        TWENTY_FOUR_SEVEN: true,
                        SECURITY: true
                    }
                },
                internal: true,
                active: true,
                uid: '4055507047',
                relevance: 0,
                partnerInternalId: '264379',
                callCenter: false,
                exclusive: true,
                obsolete: false,
                withExcerpt: false,
                freeReportAccessibility: 'FRA_NONE',
                paidReportAccessibility: 'PPA_NOT_ALLOWED_TO_BUY',
                remoteReview: {
                    onlineShow: false,
                    youtubeVideoReviewUrl: ''
                },
                socialPessimization: false,
                chargeForCallsType: 'BY_TUZ',
                yandexRent: false,
                openPlan: false,
                raised: false,
                premium: false,
                vasAvailable: true,
                suspicious: false,
                supplyMap: {
                    HEATING: true,
                    ELECTRICITY: true
                },
                placement: false,
                promoted: true,
                newBuilding: false,
                views: 3
            },
            {
                offerId: '4728410863441082631',
                trust: 'NORMAL',
                url: '//realty.test.vertis.yandex.ru/offer/4728410863441082631',
                unsignedInternalUrl: '//realty.test.vertis.yandex.ru/offer/4728410863441082631',
                partnerId: '1069251798',
                partnerName: 'бф5',
                offerType: 'SELL',
                offerCategory: 'COMMERCIAL',
                clusterId: '4728410863441082631',
                clusterHeader: true,
                clusterSize: 1,
                creationDate: '2020-08-18T21:00:01Z',
                updateDate: '2020-10-09T10:00:01Z',
                floorsTotal: 73,
                floorsOffered: [
                    6
                ],
                area: {
                    value: 823.8,
                    unit: 'SQUARE_METER'
                },
                price: {
                    currency: 'RUR',
                    value: 617589696,
                    period: 'WHOLE_LIFE',
                    unit: 'WHOLE_OFFER',
                    trend: 'UNCHANGED',
                    hasPriceHistory: false,
                    valuePerPart: 749684,
                    unitPerPart: 'SQUARE_METER',
                    valueForWhole: 617589670,
                    unitForWhole: 'WHOLE_OFFER',
                    price: {
                        value: 617589696,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    pricePerPart: {
                        value: 749684,
                        currency: 'RUB',
                        priceType: 'PER_METER',
                        pricingPeriod: 'WHOLE_LIFE'
                    },
                    priceForWhole: {
                        value: 617589670,
                        currency: 'RUB',
                        priceType: 'PER_OFFER',
                        pricingPeriod: 'WHOLE_LIFE'
                    }
                },
                notForAgents: false,
                totalImages: 11,
                mainImages: [
                    '//avatars.mdst.yandex.net/get-realty/2991/w1553/main'
                ],
                apartment: {
                    renovation: 'COSMETIC_DONE',
                    improvements: {
                        INTERNET: true,
                        AIRCONDITION: true,
                        VENTILATION: true,
                        FIRE_ALARM: true
                    }
                },
                commercial: {
                    commercialTypes: [
                        'OFFICE'
                    ],
                    commercialBuildingType: 'BUSINESS_CENTER'
                },
                location: {
                    rgid: 197177,
                    geoId: 213,
                    subjectFederationId: 1,
                    subjectFederationRgid: 741964,
                    settlementRgid: 587795,
                    settlementGeoId: 213,
                    address: 'Москва, Пресненская набережная, 8с1',
                    geocoderAddress: 'Россия, Москва, Пресненская набережная, 8с1',
                    structuredAddress: {
                        component: [
                            {
                                value: 'Россия',
                                geoId: 225,
                                regionGraphId: '143',
                                address: 'Россия',
                                regionType: 'COUNTRY',
                                queryParams: {
                                    rgid: '143',
                                    address: 'Россия'
                                }
                            },
                            {
                                value: 'Москва',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: 'Москва',
                                regionType: 'CITY',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва'
                                }
                            },
                            {
                                value: 'Пресненская набережная',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: 'Пресненская набережная',
                                regionType: 'STREET',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва, Пресненская набережная'
                                }
                            },
                            {
                                value: '8с1',
                                geoId: 213,
                                regionGraphId: '587795',
                                address: '8с1',
                                regionType: 'HOUSE',
                                queryParams: {
                                    rgid: '587795',
                                    address: 'Россия, Москва, Пресненская набережная, 8с1'
                                }
                            }
                        ]
                    },
                    point: {
                        latitude: 55.747528,
                        longitude: 37.538673,
                        precision: 'EXACT'
                    },
                    metro: {
                        metroGeoId: 115085,
                        name: 'Деловой Центр',
                        metroTransport: 'ON_FOOT',
                        timeToMetro: 1,
                        latitude: 55.74903,
                        longitude: 37.539913,
                        minTimeToMetro: 1,
                        lineColors: [
                            '6fc1ba'
                        ],
                        rgbColor: '6fc1ba'
                    },
                    streetAddress: 'Пресненская набережная, 8с1',
                    metroList: [
                        {
                            metroGeoId: 115085,
                            name: 'Деловой Центр',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 1,
                            latitude: 55.74903,
                            longitude: 37.539913,
                            minTimeToMetro: 1,
                            lineColors: [
                                '6fc1ba'
                            ],
                            rgbColor: '6fc1ba'
                        },
                        {
                            metroGeoId: 98560,
                            name: 'Международная',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 3,
                            latitude: 55.748314,
                            longitude: 37.534264,
                            minTimeToMetro: 3,
                            lineColors: [
                                '099dd4'
                            ],
                            rgbColor: '099dd4'
                        },
                        {
                            metroGeoId: 98559,
                            name: 'Выставочная',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 6,
                            latitude: 55.750076,
                            longitude: 37.54139,
                            minTimeToMetro: 6,
                            lineColors: [
                                '099dd4'
                            ],
                            rgbColor: '099dd4'
                        },
                        {
                            metroGeoId: 152947,
                            name: 'Деловой Центр',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 7,
                            latitude: 55.74709,
                            longitude: 37.53209,
                            minTimeToMetro: 7,
                            lineColors: [
                                'ffa8af'
                            ],
                            rgbColor: 'ffa8af'
                        },
                        {
                            metroGeoId: 218566,
                            name: 'Тестовская',
                            metroTransport: 'ON_FOOT',
                            timeToMetro: 15,
                            latitude: 55.75415,
                            longitude: 37.531693,
                            minTimeToMetro: 15,
                            lineColors: [
                                'ed9f2d'
                            ],
                            rgbColor: 'ed9f2d'
                        },
                        {
                            metroGeoId: 152948,
                            name: 'Шелепиха',
                            metroTransport: 'ON_TRANSPORT',
                            timeToMetro: 8,
                            latitude: 55.757122,
                            longitude: 37.524075,
                            minTimeToMetro: 8,
                            lineColors: [
                                'ffa8af',
                                'ffe400',
                                '6fc1ba'
                            ],
                            rgbColor: 'ffa8af'
                        }
                    ],
                    subjectFederationName: 'Москва и МО'
                },
                house: {
                    apartments: true,
                    housePart: false
                },
                building: {
                    buildingState: 'HAND_OVER',
                    improvements: {
                        LIFT: true,
                        ACCESS_CONTROL_SYSTEM: true,
                        TWENTY_FOUR_SEVEN: true,
                        SECURITY: true
                    },
                    parkingType: 'OPEN',
                    siteId: 59066,
                    siteName: 'Город Столиц',
                    siteDisplayName: 'МФК «Город Столиц»',
                    developerIds: [
                        268706,
                        23314
                    ],
                    officeClass: 'A_PLUS',
                    houseId: '59086',
                    heatingType: 'UNKNOWN',
                    buildingImprovementsMap: {
                        LIFT: true,
                        ACCESS_CONTROL_SYSTEM: true,
                        TWENTY_FOUR_SEVEN: true,
                        SECURITY: true
                    }
                },
                internal: true,
                active: true,
                uid: '4055507047',
                relevance: 0,
                partnerInternalId: '264119',
                callCenter: false,
                exclusive: true,
                obsolete: false,
                withExcerpt: false,
                freeReportAccessibility: 'FRA_NONE',
                paidReportAccessibility: 'PPA_NOT_ALLOWED_TO_BUY',
                remoteReview: {
                    onlineShow: false,
                    youtubeVideoReviewUrl: ''
                },
                socialPessimization: false,
                chargeForCallsType: 'BY_TUZ',
                trustedOfferInfo: {
                    isFullTrustedOwner: false,
                    ownerTrustedStatus: 'NOT_LINKED_MOSRU',
                    isCadastrPersonMatched: false
                },
                yandexRent: false,
                raised: false,
                premium: false,
                vasAvailable: true,
                openPlan: false,
                suspicious: false,
                supplyMap: {
                    HEATING: true,
                    ELECTRICITY: true
                },
                placement: false,
                promoted: true,
                newBuilding: false,
                views: 3
            }
        ],
        filterValue: {
            type: 'SELL',
            category: 'COMMERCIAL',
            idsByFilterValue: {
                SELL: {
                    COMMERCIAL: [
                        '4728410863441080523',
                        '4728410863441082631'
                    ]
                }
            },
            moreThanOneFilterValue: false
        },
        onlyDifferent: false
    },
    page: {
        queryId: '123'
    }
};

