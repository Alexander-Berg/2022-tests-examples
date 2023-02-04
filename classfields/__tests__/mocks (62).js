export const getInitialState = () => ({
    user: {
        favorites: [],
        favoritesMap: {},
        isAuth: false,
        isJuridical: false,
        paymentTypeSuffix: 'natural',
        comparison: []
    },
    page: {
        name: 'page-name'
    },
    geo: {
        rgid: 741964
    },
    offerNotes: {
        notes: {}
    }
});

export const getOffers = () => [
    {
        appMiddleImages: [],
        appLargeImages: [],
        area: {
            value: 37,
            unit: 'SQUARE_METER'
        },
        author: {
            id: '0',
            category: 'AGENCY',
            organization: 'ОГРК Центр',
            agentName: 'Елена Ларькина',
            photo: '',
            creationDate: '2017-03-13T13:45:14Z',
            redirectPhones: true,
            redirectPhonesFailed: false,
            name: 'ОГРК Центр',
            encryptedPhoneNumbers: [
                {
                    phone: 'KzcF5MHjYJ4MLzINyOPTgR3',
                    redirectId: '+79268322987'
                }
            ],
            encryptedPhones: [
                'KzcF5MHjYJ4MLzINyOPTgR3'
            ]
        },
        building: {
            builtYear: 2003,
            buildingType: 'PANEL',
            improvements: {
                LIFT: true,
                RUBBISH_CHUTE: true,
                SECURITY: false
            },
            buildingId: '971839803098366928',
            flatsCount: 300,
            porchesCount: 5,
            heatingType: 'UNKNOWN',
            priceStatistics: {
                sellPricePerSquareMeter: {
                    level: 9,
                    maxLevel: 9,
                    rgbColor: '128000',
                    value: '96102',
                    regionValueFrom: '22727',
                    regionValueTo: '2234148'
                },
                sellPriceByRooms: {
                    1: {
                        level: 9,
                        maxLevel: 9,
                        rgbColor: '128000',
                        value: '3700000',
                        regionValueFrom: '300000',
                        regionValueTo: '1989368448'
                    }
                },
                profitability: {
                    level: 8,
                    maxLevel: 9,
                    rgbColor: '20a70a',
                    value: '15',
                    regionValueFrom: '2',
                    regionValueTo: '79'
                }
            },
            buildingImprovementsMap: {
                LIFT: true,
                RUBBISH_CHUTE: true,
                SECURITY: false
            }
        },
        creationDate: '2019-01-27T20:07:26Z',
        flatType: 'SECONDARY',
        floorsOffered: [
            11
        ],
        floorsTotal: 16,
        fullImages: [],
        house: {
            bathroomUnit: 'SEPARATED',
            balconyType: 'BALCONY',
            windowView: 'STREET',
            apartments: false,
            housePart: false
        },
        livingSpace: {
            value: 14,
            unit: 'SQUARE_METER'
        },
        location: {
            rgid: 282323,
            geoId: 21622,
            subjectFederationId: 1,
            settlementRgid: 596189,
            settlementGeoId: 10716,
            address: 'Московская область, Балашиха, микрорайон Ольгино, Граничная улица, 20',
            geocoderAddress: 'Россия, Московская область, Балашиха, микрорайон Ольгино, Граничная улица, 20',
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
                        value: 'Московская',
                        geoId: 1,
                        regionGraphId: '587654',
                        address: 'Московская область',
                        regionType: 'SUBJECT_FEDERATION',
                        queryParams: {
                            rgid: '587654',
                            address: 'Россия, Московская область'
                        }
                    },
                    {
                        value: 'округ Балашиха',
                        geoId: 116705,
                        regionGraphId: '2292',
                        address: 'Балашиха (городской округ)',
                        regionType: 'SUBJECT_FEDERATION_DISTRICT',
                        queryParams: {
                            rgid: '2292',
                            address: 'Россия, Московская область, Балашиха (городской округ)'
                        }
                    },
                    {
                        value: 'Балашиха',
                        geoId: 10716,
                        regionGraphId: '596189',
                        address: 'Балашиха',
                        regionType: 'CITY',
                        queryParams: {
                            rgid: '596189',
                            address: 'Россия, Московская область, Балашиха (городской округ), Балашиха'
                        }
                    },
                    {
                        value: 'Ольгино',
                        geoId: 21622,
                        regionGraphId: '282323',
                        address: 'микрорайон Ольгино',
                        regionType: 'NOT_ADMINISTRATIVE_DISTRICT',
                        queryParams: {
                            rgid: '282323',
                            // eslint-disable-next-line max-len
                            address: 'Россия, Московская область, Балашиха (городской округ), Балашиха, микрорайон Ольгино'
                        }
                    },
                    {
                        value: 'Граничная улица',
                        geoId: 21622,
                        regionGraphId: '282323',
                        address: 'Граничная улица',
                        regionType: 'STREET',
                        queryParams: {
                            rgid: '282323',
                            // eslint-disable-next-line max-len
                            address: 'Россия, Московская область, Балашиха (городской округ), Балашиха, микрорайон Ольгино, Граничная улица'
                        }
                    },
                    {
                        value: '20',
                        geoId: 21622,
                        regionGraphId: '282323',
                        address: '20',
                        regionType: 'HOUSE',
                        queryParams: {
                            rgid: '282323',
                            // eslint-disable-next-line max-len
                            address: 'Россия, Московская область, Балашиха (городской округ), Балашиха, микрорайон Ольгино, Граничная улица, 20'
                        }
                    }
                ]
            },
            point: {
                latitude: 55.7467,
                longitude: 37.983574,
                precision: 'EXACT'
            },
            metro: {
                metroGeoId: 114781,
                name: 'Новокосино',
                metroTransport: 'ON_TRANSPORT',
                timeToMetro: 30,
                latitude: 55.745113,
                longitude: 37.864056,
                minTimeToMetro: 30,
                lineColors: [
                    'ffe400'
                ],
                rgbColor: 'ffe400'
            },
            highway: {
                name: 'Носовихинское шоссе',
                distanceKm: 11.3
            },
            station: {
                name: 'Железнодорожная',
                distanceKm: 1.669
            },
            streetAddress: 'Граничная улица, 20',
            metroList: [
                {
                    metroGeoId: 114781,
                    name: 'Новокосино',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 30,
                    latitude: 55.745113,
                    longitude: 37.864056,
                    minTimeToMetro: 30,
                    lineColors: [
                        'ffe400'
                    ],
                    rgbColor: 'ffe400'
                },
                {
                    metroGeoId: 218432,
                    name: 'Некрасовка',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 37,
                    latitude: 55.70297,
                    longitude: 37.928036,
                    minTimeToMetro: 37,
                    lineColors: [
                        'ff66e8'
                    ],
                    rgbColor: 'ff66e8'
                },
                {
                    metroGeoId: 218431,
                    name: 'Лухмановская',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 42,
                    latitude: 55.708515,
                    longitude: 37.900574,
                    minTimeToMetro: 42,
                    lineColors: [
                        'ff66e8'
                    ],
                    rgbColor: 'ff66e8'
                },
                {
                    metroGeoId: 218430,
                    name: 'Улица Дмитриевского',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 44,
                    latitude: 55.71031,
                    longitude: 37.87905,
                    minTimeToMetro: 44,
                    lineColors: [
                        'ff66e8'
                    ],
                    rgbColor: 'ff66e8'
                }
            ],
            airports: [
                {
                    id: '878109',
                    name: 'Жуковский (Раменское)',
                    timeOnCar: 3621,
                    distanceOnCar: 39315,
                    latitude: 55.568665,
                    longitude: 38.143654,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 60,
                            distance: 39315
                        }
                    ]
                },
                {
                    id: '858742',
                    name: 'Домодедово',
                    timeOnCar: 3914,
                    distanceOnCar: 57476,
                    latitude: 55.41435,
                    longitude: 37.90048,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 65,
                            distance: 57476
                        }
                    ]
                },
                {
                    id: '878042',
                    name: 'Шереметьево',
                    timeOnCar: 4810,
                    distanceOnCar: 58615,
                    latitude: 55.963852,
                    longitude: 37.4169,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 80,
                            distance: 58615
                        }
                    ]
                },
                {
                    id: '878065',
                    name: 'Внуково',
                    timeOnCar: 5860,
                    distanceOnCar: 67234,
                    latitude: 55.604942,
                    longitude: 37.282578,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 97,
                            distance: 67234
                        }
                    ]
                }
            ],
            heatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: '9ada1b',
                    description: 'хорошо развитая',
                    level: 5,
                    maxLevel: 8,
                    title: 'Инфраструктура'
                },
                {
                    name: 'transport',
                    rgbColor: 'ee4613',
                    description: 'низкая доступность',
                    level: 2,
                    maxLevel: 9,
                    title: 'Транспорт'
                }
            ],
            allHeatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: '9ada1b',
                    description: 'хорошо развитая',
                    level: 5,
                    maxLevel: 8,
                    title: 'Инфраструктура'
                },
                {
                    name: 'price-rent',
                    rgbColor: '128000',
                    description: 'очень низкая',
                    level: 9,
                    maxLevel: 9,
                    title: 'Цена аренды'
                },
                {
                    name: 'price-sell',
                    rgbColor: '20a70a',
                    description: 'очень низкая',
                    level: 8,
                    maxLevel: 9,
                    title: 'Цена продажи'
                },
                {
                    name: 'profitability',
                    rgbColor: '128000',
                    description: 'очень высокая',
                    level: 9,
                    maxLevel: 9,
                    title: 'Прогноз окупаемости'
                },
                {
                    name: 'transport',
                    rgbColor: 'ee4613',
                    description: 'низкая доступность',
                    level: 2,
                    maxLevel: 9,
                    title: 'Транспорт'
                }
            ],
            routeDistances: [
                {
                    geoPoint: {
                        latitude: 55.744076,
                        longitude: 37.842525
                    },
                    distance: 11300,
                    highway: {
                        id: '30',
                        name: 'Носовихинское шоссе'
                    }
                }
            ],
            subjectFederationName: 'Москва и МО',
            cityCenter: [
                {
                    transport: 'ON_CAR',
                    time: 2615,
                    distance: 29730,
                    latitude: 55.749893,
                    longitude: 37.623425,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 43,
                            distance: 29730
                        }
                    ]
                }
            ]
        },
        minicardImages: [],
        newBuilding: false,
        obsolete: false,
        offerId: '5462851513174486637',
        offerCategory: 'APARTMENT',
        offerType: 'SELL',
        openPlan: false,
        predictions: {
            predictedPrice: {
                min: '1871000',
                max: '2287000',
                value: '2079000'
            }
        },
        price: {
            currency: 'RUR',
            value: 4300000,
            period: 'WHOLE_LIFE',
            unit: 'WHOLE_OFFER',
            trend: 'UNCHANGED',
            hasPriceHistory: false,
            valuePerPart: 116216,
            unitPerPart: 'SQUARE_METER',
            valueForWhole: 4300000,
            unitForWhole: 'WHOLE_OFFER',
            price: {
                value: 4300000,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE'
            },
            pricePerPart: {
                value: 116216,
                currency: 'RUB',
                priceType: 'PER_METER',
                pricingPeriod: 'WHOLE_LIFE'
            },
            priceForWhole: {
                value: 4300000,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE'
            }
        },
        primarySaleV2: false,
        remoteReview: {
            onlineShow: false,
            youtubeVideoReviewUrl: ''
        },
        roomsTotal: 1,
        totalImages: 10,
        transactionConditionsMap: {
            MORTGAGE: true
        },
        vas: {
            raised: false,
            premium: false,
            placement: false,
            promoted: false,
            turboSale: false,
            raisingSale: false,
            tuzFeatured: false,
            vasAvailable: true
        },
        withExcerpt: false
    },
    {
        appMiddleImages: [],
        appLargeImages: [],
        area: {
            value: 74.4,
            unit: 'SQUARE_METER'
        },
        author: {
            id: '0',
            category: 'AGENCY',
            organization: 'ОГРК Центр',
            agentName: 'Галина Амирова',
            photo: '',
            creationDate: '2017-03-13T13:45:14Z',
            redirectPhones: true,
            redirectPhonesFailed: false,
            name: 'ОГРК Центр',
            encryptedPhoneNumbers: [
                {
                    phone: 'KzcF5MHjYJxMLDAN4OPDERz',
                    redirectId: '+79261008813'
                }
            ],
            encryptedPhones: [
                'KzcF5MHjYJxMLDAN4OPDERz'
            ]
        },
        building: {
            builtYear: 2004,
            buildingType: 'PANEL',
            improvements: {
                PARKING: true,
                LIFT: true,
                RUBBISH_CHUTE: true,
                SECURITY: false
            },
            parkingType: 'OPEN',
            buildingId: '971839803098366928',
            flatsCount: 300,
            porchesCount: 5,
            heatingType: 'UNKNOWN',
            priceStatistics: {
                sellPricePerSquareMeter: {
                    level: 9,
                    maxLevel: 9,
                    rgbColor: '128000',
                    value: '96102',
                    regionValueFrom: '22727',
                    regionValueTo: '2234148'
                },
                sellPriceByRooms: {
                    1: {
                        level: 9,
                        maxLevel: 9,
                        rgbColor: '128000',
                        value: '3700000',
                        regionValueFrom: '300000',
                        regionValueTo: '1989368448'
                    }
                },
                profitability: {
                    level: 8,
                    maxLevel: 9,
                    rgbColor: '20a70a',
                    value: '15',
                    regionValueFrom: '2',
                    regionValueTo: '79'
                }
            },
            buildingImprovementsMap: {
                PARKING: true,
                LIFT: true,
                RUBBISH_CHUTE: true,
                SECURITY: false
            }
        },
        creationDate: '2019-01-03T21:00:00Z',
        flatType: 'SECONDARY',
        floorsOffered: [
            3
        ],
        floorsTotal: 17,
        fullImages: [],
        house: {
            bathroomUnit: 'MATCHED',
            balconyType: 'BALCONY',
            windowView: 'STREET',
            apartments: false,
            housePart: false
        },
        livingSpace: {
            value: 39.2,
            unit: 'SQUARE_METER'
        },
        location: {
            rgid: 282323,
            geoId: 21622,
            subjectFederationId: 1,
            settlementRgid: 596189,
            settlementGeoId: 10716,
            address: 'Московская область, Балашиха, микрорайон Ольгино, Граничная улица, 20',
            geocoderAddress: 'Россия, Московская область, Балашиха, микрорайон Ольгино, Граничная улица, 20',
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
                        value: 'Московская',
                        geoId: 1,
                        regionGraphId: '587654',
                        address: 'Московская область',
                        regionType: 'SUBJECT_FEDERATION',
                        queryParams: {
                            rgid: '587654',
                            address: 'Россия, Московская область'
                        }
                    },
                    {
                        value: 'округ Балашиха',
                        geoId: 116705,
                        regionGraphId: '2292',
                        address: 'Балашиха (городской округ)',
                        regionType: 'SUBJECT_FEDERATION_DISTRICT',
                        queryParams: {
                            rgid: '2292',
                            address: 'Россия, Московская область, Балашиха (городской округ)'
                        }
                    },
                    {
                        value: 'Балашиха',
                        geoId: 10716,
                        regionGraphId: '596189',
                        address: 'Балашиха',
                        regionType: 'CITY',
                        queryParams: {
                            rgid: '596189',
                            address: 'Россия, Московская область, Балашиха (городской округ), Балашиха'
                        }
                    },
                    {
                        value: 'Ольгино',
                        geoId: 21622,
                        regionGraphId: '282323',
                        address: 'микрорайон Ольгино',
                        regionType: 'NOT_ADMINISTRATIVE_DISTRICT',
                        queryParams: {
                            rgid: '282323',
                            // eslint-disable-next-line max-len
                            address: 'Россия, Московская область, Балашиха (городской округ), Балашиха, микрорайон Ольгино'
                        }
                    },
                    {
                        value: 'Граничная улица',
                        geoId: 21622,
                        regionGraphId: '282323',
                        address: 'Граничная улица',
                        regionType: 'STREET',
                        queryParams: {
                            rgid: '282323',
                            // eslint-disable-next-line max-len
                            address: 'Россия, Московская область, Балашиха (городской округ), Балашиха, микрорайон Ольгино, Граничная улица'
                        }
                    },
                    {
                        value: '20',
                        geoId: 21622,
                        regionGraphId: '282323',
                        address: '20',
                        regionType: 'HOUSE',
                        queryParams: {
                            rgid: '282323',
                            // eslint-disable-next-line max-len
                            address: 'Россия, Московская область, Балашиха (городской округ), Балашиха, микрорайон Ольгино, Граничная улица, 20'
                        }
                    }
                ]
            },
            point: {
                latitude: 55.7467,
                longitude: 37.983574,
                precision: 'EXACT'
            },
            metro: {
                metroGeoId: 114781,
                name: 'Новокосино',
                metroTransport: 'ON_TRANSPORT',
                timeToMetro: 30,
                latitude: 55.745113,
                longitude: 37.864056,
                minTimeToMetro: 30,
                lineColors: [
                    'ffe400'
                ],
                rgbColor: 'ffe400'
            },
            highway: {
                name: 'Носовихинское шоссе',
                distanceKm: 11.3
            },
            station: {
                name: 'Железнодорожная',
                distanceKm: 1.669
            },
            streetAddress: 'Граничная улица, 20',
            metroList: [
                {
                    metroGeoId: 114781,
                    name: 'Новокосино',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 30,
                    latitude: 55.745113,
                    longitude: 37.864056,
                    minTimeToMetro: 30,
                    lineColors: [
                        'ffe400'
                    ],
                    rgbColor: 'ffe400'
                },
                {
                    metroGeoId: 218432,
                    name: 'Некрасовка',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 37,
                    latitude: 55.70297,
                    longitude: 37.928036,
                    minTimeToMetro: 37,
                    lineColors: [
                        'ff66e8'
                    ],
                    rgbColor: 'ff66e8'
                },
                {
                    metroGeoId: 218431,
                    name: 'Лухмановская',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 42,
                    latitude: 55.708515,
                    longitude: 37.900574,
                    minTimeToMetro: 42,
                    lineColors: [
                        'ff66e8'
                    ],
                    rgbColor: 'ff66e8'
                },
                {
                    metroGeoId: 218430,
                    name: 'Улица Дмитриевского',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 44,
                    latitude: 55.71031,
                    longitude: 37.87905,
                    minTimeToMetro: 44,
                    lineColors: [
                        'ff66e8'
                    ],
                    rgbColor: 'ff66e8'
                }
            ],
            airports: [
                {
                    id: '878109',
                    name: 'Жуковский (Раменское)',
                    timeOnCar: 3621,
                    distanceOnCar: 39315,
                    latitude: 55.568665,
                    longitude: 38.143654,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 60,
                            distance: 39315
                        }
                    ]
                },
                {
                    id: '858742',
                    name: 'Домодедово',
                    timeOnCar: 3914,
                    distanceOnCar: 57476,
                    latitude: 55.41435,
                    longitude: 37.90048,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 65,
                            distance: 57476
                        }
                    ]
                },
                {
                    id: '878042',
                    name: 'Шереметьево',
                    timeOnCar: 4810,
                    distanceOnCar: 58615,
                    latitude: 55.963852,
                    longitude: 37.4169,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 80,
                            distance: 58615
                        }
                    ]
                },
                {
                    id: '878065',
                    name: 'Внуково',
                    timeOnCar: 5860,
                    distanceOnCar: 67234,
                    latitude: 55.604942,
                    longitude: 37.282578,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 97,
                            distance: 67234
                        }
                    ]
                }
            ],
            heatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: '9ada1b',
                    description: 'хорошо развитая',
                    level: 5,
                    maxLevel: 8,
                    title: 'Инфраструктура'
                },
                {
                    name: 'transport',
                    rgbColor: 'ee4613',
                    description: 'низкая доступность',
                    level: 2,
                    maxLevel: 9,
                    title: 'Транспорт'
                }
            ],
            allHeatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: '9ada1b',
                    description: 'хорошо развитая',
                    level: 5,
                    maxLevel: 8,
                    title: 'Инфраструктура'
                },
                {
                    name: 'price-rent',
                    rgbColor: '128000',
                    description: 'очень низкая',
                    level: 9,
                    maxLevel: 9,
                    title: 'Цена аренды'
                },
                {
                    name: 'price-sell',
                    rgbColor: '20a70a',
                    description: 'очень низкая',
                    level: 8,
                    maxLevel: 9,
                    title: 'Цена продажи'
                },
                {
                    name: 'profitability',
                    rgbColor: '128000',
                    description: 'очень высокая',
                    level: 9,
                    maxLevel: 9,
                    title: 'Прогноз окупаемости'
                },
                {
                    name: 'transport',
                    rgbColor: 'ee4613',
                    description: 'низкая доступность',
                    level: 2,
                    maxLevel: 9,
                    title: 'Транспорт'
                }
            ],
            routeDistances: [
                {
                    geoPoint: {
                        latitude: 55.744076,
                        longitude: 37.842525
                    },
                    distance: 11300,
                    highway: {
                        id: '30',
                        name: 'Носовихинское шоссе'
                    }
                }
            ],
            subjectFederationName: 'Москва и МО',
            cityCenter: [
                {
                    transport: 'ON_CAR',
                    time: 2615,
                    distance: 29730,
                    latitude: 55.749893,
                    longitude: 37.623425,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 43,
                            distance: 29730
                        }
                    ]
                }
            ]
        },
        minicardImages: [],
        newBuilding: false,
        obsolete: false,
        offerId: '5462851513174608649',
        offerCategory: 'APARTMENT',
        offerType: 'SELL',
        openPlan: false,
        predictions: {
            predictedPrice: {
                min: '4044000',
                max: '4942000',
                value: '4493000'
            }
        },
        price: {
            currency: 'RUR',
            value: 7150000,
            period: 'WHOLE_LIFE',
            unit: 'WHOLE_OFFER',
            trend: 'DECREASED',
            previous: 7250000,
            hasPriceHistory: true,
            valuePerPart: 96102,
            unitPerPart: 'SQUARE_METER',
            valueForWhole: 7150000,
            unitForWhole: 'WHOLE_OFFER',
            price: {
                value: 7150000,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE'
            },
            pricePerPart: {
                value: 96102,
                currency: 'RUB',
                priceType: 'PER_METER',
                pricingPeriod: 'WHOLE_LIFE'
            },
            priceForWhole: {
                value: 7150000,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE'
            }
        },
        primarySaleV2: false,
        remoteReview: {
            onlineShow: false,
            youtubeVideoReviewUrl: ''
        },
        roomsTotal: 3,
        totalImages: 10,
        transactionConditionsMap: {
            MORTGAGE: true
        },
        updateDate: '2019-01-23T06:16:00Z',
        vas: {
            raised: false,
            premium: false,
            placement: false,
            promoted: false,
            turboSale: false,
            raisingSale: false,
            tuzFeatured: false,
            vasAvailable: true
        },
        withExcerpt: false
    }
];

export const getOneOffer = () => [ getOffers()[0] ];

export const getSiteInfo = () => ({
    type: 'site',
    images: [
        {
            src: '//avatars.mds.yandex.net',
            srcX2: '//avatars.mds.yandex.net'
        },
        {
            src: '//avatars.mds.yandex.net',
            srcX2: '//avatars.mds.yandex.net'
        },
        {
            src: '//avatars.mds.yandex.net',
            srcX2: '//avatars.mds.yandex.net'
        }
    ],
    hasFinishedApartments: false,
    buildingState: 'UNFINISHED',
    deliveryDate: {
        year: 2020,
        quarter: 3
    },
    minPricePerMeter: 130000,
    siteId: 2131550,
    siteName: 'Narva Loft',
    buildingFeatures: {
        state: 'UNFINISHED',
        finishedApartments: false,
        class: 'BUSINESS',
        zhkType: 'MFK',
        totalFloors: 8,
        minTotalFloors: 8,
        totalApartments: 420,
        isApartment: true,
        interiorFinish: {
            type: 'ROUGH',
            // eslint-disable-next-line max-len
            text: 'Минимально необходимые работы уже выполнены: подведены холодная, горячая вода и электричество, установлены батареи. В квартире не проводились отделочные работы: не выровнены стены и пол.',
            images: []
        },
        parking: {
            type: 'INDOOR',
            parkingSpaces: 400,
            available: true
        },
        parkings: [
            {
                type: 'CLOSED',
                parkingSpaces: 400,
                available: true
            }
        ],
        walls: {
            type: 'MONOLIT',
            // eslint-disable-next-line max-len
            text: 'Долговечный материал, позволяет приступить к отделочным работам почти сразу же после завершения строительства.'
        },
        decorationInfo: [],
        decorationImages: [],
        wallTypes: [
            {
                type: 'MONOLIT',
                // eslint-disable-next-line max-len
                text: 'Долговечный материал, позволяет приступить к отделочным работам почти сразу же после завершения строительства.'
            }
        ]
    },
    flatStatus: 'ON_SALE'
});

export const getVillageInfo = () => ({
    type: 'village',
    images: [
        {
            src: '//avatars.mdst.yandex.net',
            srcX2: '//avatars.mdst.yandex.net'
        },
        {
            src: '//avatars.mdst.yandex.net',
            srcX2: '//avatars.mdst.yandex.net'
        },
        {
            src: '//avatars.mdst.yandex.net',
            srcX2: '//avatars.mdst.yandex.net'
        }
    ],
    offerStats: {
        entries: [
            {
                offerType: 'COTTAGE',
                price: {
                    currency: 'RUB',
                    from: '128880000',
                    to: '250600000'
                },
                landArea: {
                    unit: 'SOTKA',
                    from: 22,
                    to: 38
                },
                houseArea: {
                    unit: 'SQ_M',
                    from: 432,
                    to: 1200
                },
                amount: 3
            }
        ],
        secondaryOffersCount: 8,
        primaryPrices: {
            currency: 'RUB',
            from: '128880000',
            to: '250600000'
        },
        villageSecondaryStats: {
            houseOffersCount: 8
        }
    },
    villageId: '1828507',
    fullName: 'Коттеджный посёлок «Бельгийская деревня»',
    name: 'Бельгийская деревня'
});

export const getSiteOffers = () => [
    {
        appMiddleImages: [],
        appLargeImages: [],
        area: {
            value: 62.97,
            unit: 'SQUARE_METER'
        },
        author: {
            id: '0',
            category: 'AGENCY',
            organization: 'ЭТАЖИ',
            agentName: 'Taras',
            photo: '//avatars.mdst.yandex.net',
            creationDate: '2019-01-22T20:03:33Z',
            profile: {
                userType: 'AGENCY',
                name: 'SOL',
                logo: {}
            },
            redirectPhones: true,
            redirectPhonesFailed: false,
            name: 'ЭТАЖИ',
            encryptedPhoneNumbers: [
                {
                    phone: 'KzcF5MHjEJ0NLDIN4NPTkR4',
                    redirectId: '+79214428598'
                }
            ],
            encryptedPhones: [
                'KzcF5MHjEJ0NLDIN4NPTkR4'
            ]
        },
        building: {
            builtYear: 2020,
            builtQuarter: 3,
            buildingState: 'UNFINISHED',
            parkingType: 'OPEN',
            siteId: 2131550,
            siteName: 'Narva Loft',
            siteDisplayName: 'МФК «Narva Loft»',
            houseId: '2131592',
            heatingType: 'UNKNOWN'
        },
        creationDate: '2019-01-20T10:00:00Z',
        flatType: 'NEW_FLAT',
        floorsOffered: [
            2
        ],
        floorsTotal: 8,
        fullImages: [],
        house: {
            studio: false,
            apartments: true,
            housePart: false
        },
        location: {
            rgid: 193307,
            geoId: 213,
            subjectFederationId: 1,
            settlementRgid: 165705,
            settlementGeoId: 213,
            address: 'Москва, Нарвская улица, 2',
            geocoderAddress: 'Россия, Москва, Нарвская улица, 2с5',
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
                        value: 'Нарвская улица',
                        geoId: 213,
                        regionGraphId: '587795',
                        address: 'Нарвская улица',
                        regionType: 'STREET',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва, Нарвская улица'
                        }
                    },
                    {
                        value: '2с5',
                        geoId: 213,
                        regionGraphId: '587795',
                        address: '2с5',
                        regionType: 'HOUSE',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва, Нарвская улица, 2с5'
                        }
                    }
                ]
            },
            point: {
                latitude: 55.833607,
                longitude: 37.50876,
                precision: 'EXACT'
            },
            metro: {
                metroGeoId: 20370,
                name: 'Водный Стадион',
                metroTransport: 'ON_FOOT',
                timeToMetro: 21,
                latitude: 55.839886,
                longitude: 37.48678,
                minTimeToMetro: 13,
                lineColors: [
                    '4f8242'
                ],
                rgbColor: '4f8242'
            },
            streetAddress: 'Нарвская улица, 2с5',
            metroList: [
                {
                    metroGeoId: 20370,
                    name: 'Водный Стадион',
                    metroTransport: 'ON_FOOT',
                    timeToMetro: 21,
                    latitude: 55.839886,
                    longitude: 37.48678,
                    minTimeToMetro: 13,
                    lineColors: [
                        '4f8242'
                    ],
                    rgbColor: '4f8242'
                },
                {
                    metroGeoId: 152923,
                    name: 'Коптево',
                    metroTransport: 'ON_FOOT',
                    timeToMetro: 21,
                    latitude: 55.840084,
                    longitude: 37.520706,
                    minTimeToMetro: 12,
                    lineColors: [
                        'ffa8af'
                    ],
                    rgbColor: 'ffa8af'
                },
                {
                    metroGeoId: 20371,
                    name: 'Войковская',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 13,
                    latitude: 55.818928,
                    longitude: 37.497795,
                    minTimeToMetro: 13,
                    lineColors: [
                        '4f8242'
                    ],
                    rgbColor: '4f8242'
                },
                {
                    metroGeoId: 152922,
                    name: 'Балтийская',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 21,
                    latitude: 55.825886,
                    longitude: 37.496365,
                    minTimeToMetro: 21,
                    lineColors: [
                        'ffa8af'
                    ],
                    rgbColor: 'ffa8af'
                },
                {
                    metroGeoId: 218561,
                    name: 'Красный Балтиец',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 27,
                    latitude: 55.815483,
                    longitude: 37.526627,
                    minTimeToMetro: 27,
                    lineColors: [
                        'df477c'
                    ],
                    rgbColor: 'df477c'
                }
            ],
            airports: [
                {
                    id: '878042',
                    name: 'Шереметьево',
                    timeOnCar: 1288,
                    distanceOnCar: 18665,
                    latitude: 55.963852,
                    longitude: 37.4169,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 21,
                            distance: 18665
                        }
                    ]
                },
                {
                    id: '878065',
                    name: 'Внуково',
                    timeOnCar: 3313,
                    distanceOnCar: 46326,
                    latitude: 55.604942,
                    longitude: 37.282578,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 55,
                            distance: 46326
                        }
                    ]
                },
                {
                    id: '858742',
                    name: 'Домодедово',
                    timeOnCar: 3955,
                    distanceOnCar: 61565,
                    latitude: 55.41435,
                    longitude: 37.90048,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 65,
                            distance: 61565
                        }
                    ]
                },
                {
                    id: '878109',
                    name: 'Жуковский (Раменское)',
                    timeOnCar: 4121,
                    distanceOnCar: 62146,
                    latitude: 55.568665,
                    longitude: 38.143654,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 68,
                            distance: 62146
                        }
                    ]
                }
            ],
            subjectFederationName: 'Москва и МО',
            cityCenter: [
                {
                    transport: 'ON_CAR',
                    time: 1536,
                    distance: 15133,
                    latitude: 55.755253,
                    longitude: 37.616386,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 25,
                            distance: 15133
                        }
                    ]
                }
            ]
        },
        minicardImages: [],
        newBuilding: true,
        obsolete: false,
        offerId: '8224807745834868444',
        offerCategory: 'APARTMENT',
        offerType: 'SELL',
        openPlan: false,
        price: {
            currency: 'RUR',
            value: 11000000,
            period: 'WHOLE_LIFE',
            unit: 'WHOLE_OFFER',
            trend: 'UNCHANGED',
            hasPriceHistory: false,
            valuePerPart: 174686,
            unitPerPart: 'SQUARE_METER',
            valueForWhole: 11000000,
            unitForWhole: 'WHOLE_OFFER',
            price: {
                value: 11000000,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE'
            },
            pricePerPart: {
                value: 174686,
                currency: 'RUB',
                priceType: 'PER_METER',
                pricingPeriod: 'WHOLE_LIFE'
            },
            priceForWhole: {
                value: 11000000,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE'
            }
        },
        primarySaleV2: false,
        remoteReview: {
            onlineShow: false,
            youtubeVideoReviewUrl: ''
        },
        roomsTotal: 2,
        totalImages: 3,
        transactionConditionsMap: {
            MORTGAGE: true
        },
        tuzInfo: {
            campaignId: 'd57f26ca-3590-406b-8a69-14fef0b9c570',
            active: true,
            tuzParams: [
                {
                    key: 'tuzParamRgid',
                    value: '741964'
                },
                {
                    key: 'tuzParamType',
                    value: 'SELL'
                },
                {
                    key: 'tuzParamCategory',
                    value: 'APARTMENT'
                },
                {
                    key: 'tuzParamPartner',
                    value: '1069250970'
                },
                {
                    key: 'tuzParamUid',
                    value: '4045447503'
                },
                {
                    key: 'tuzParamClass',
                    value: 'BUSINESS'
                },
                {
                    key: 'tuzParamReassignment',
                    value: 'yes'
                }
            ],
            tuzFeatured: true,
            premium: true,
            promotion: true,
            raising: true,
            clientId: 135109318,
            tuzType: {
                maximum: {}
            }
        },
        updateDate: '2019-01-23T06:16:00Z',
        vas: {
            raised: true,
            premium: true,
            placement: false,
            promoted: true,
            turboSale: false,
            raisingSale: false,
            tuzFeatured: true,
            vasAvailable: true
        },
        withExcerpt: false
    },
    {
        appMiddleImages: [],
        appLargeImages: [],
        area: {
            value: 62.97,
            unit: 'SQUARE_METER'
        },
        author: {
            id: '0',
            category: 'AGENCY',
            organization: 'ЭТАЖИ',
            agentName: 'Taras',
            photo: '',
            creationDate: '2019-01-22T20:03:33Z',
            profile: {
                userType: 'AGENCY',
                name: 'SOL',
                logo: {}
            },
            redirectPhones: true,
            redirectPhonesFailed: false,
            name: 'ЭТАЖИ',
            encryptedPhoneNumbers: [
                {
                    phone: 'KzcF5MHjEJ0NLDIN4NPTkR4',
                    redirectId: '+79214428598'
                }
            ],
            encryptedPhones: [
                'KzcF5MHjEJ0NLDIN4NPTkR4'
            ]
        },
        building: {
            builtYear: 2020,
            builtQuarter: 3,
            buildingState: 'UNFINISHED',
            parkingType: 'OPEN',
            siteId: 2131550,
            siteName: 'Narva Loft',
            siteDisplayName: 'МФК «Narva Loft»',
            houseId: '2131592',
            heatingType: 'UNKNOWN'
        },
        creationDate: '2019-01-20T10:00:00Z',
        flatType: 'NEW_FLAT',
        floorsOffered: [
            2
        ],
        floorsTotal: 8,
        fullImages: [],
        house: {
            studio: false,
            apartments: true,
            housePart: false
        },
        location: {
            rgid: 193307,
            geoId: 213,
            subjectFederationId: 1,
            settlementRgid: 165705,
            settlementGeoId: 213,
            address: 'Москва, Нарвская улица, 2',
            geocoderAddress: 'Россия, Москва, Нарвская улица, 2с5',
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
                        value: 'Нарвская улица',
                        geoId: 213,
                        regionGraphId: '587795',
                        address: 'Нарвская улица',
                        regionType: 'STREET',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва, Нарвская улица'
                        }
                    },
                    {
                        value: '2с5',
                        geoId: 213,
                        regionGraphId: '587795',
                        address: '2с5',
                        regionType: 'HOUSE',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва, Нарвская улица, 2с5'
                        }
                    }
                ]
            },
            point: {
                latitude: 55.833607,
                longitude: 37.50876,
                precision: 'EXACT'
            },
            metro: {
                metroGeoId: 20370,
                name: 'Водный Стадион',
                metroTransport: 'ON_FOOT',
                timeToMetro: 21,
                latitude: 55.839886,
                longitude: 37.48678,
                minTimeToMetro: 13,
                lineColors: [
                    '4f8242'
                ],
                rgbColor: '4f8242'
            },
            streetAddress: 'Нарвская улица, 2с5',
            metroList: [
                {
                    metroGeoId: 20370,
                    name: 'Водный Стадион',
                    metroTransport: 'ON_FOOT',
                    timeToMetro: 21,
                    latitude: 55.839886,
                    longitude: 37.48678,
                    minTimeToMetro: 13,
                    lineColors: [
                        '4f8242'
                    ],
                    rgbColor: '4f8242'
                },
                {
                    metroGeoId: 152923,
                    name: 'Коптево',
                    metroTransport: 'ON_FOOT',
                    timeToMetro: 21,
                    latitude: 55.840084,
                    longitude: 37.520706,
                    minTimeToMetro: 12,
                    lineColors: [
                        'ffa8af'
                    ],
                    rgbColor: 'ffa8af'
                },
                {
                    metroGeoId: 20371,
                    name: 'Войковская',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 13,
                    latitude: 55.818928,
                    longitude: 37.497795,
                    minTimeToMetro: 13,
                    lineColors: [
                        '4f8242'
                    ],
                    rgbColor: '4f8242'
                },
                {
                    metroGeoId: 152922,
                    name: 'Балтийская',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 21,
                    latitude: 55.825886,
                    longitude: 37.496365,
                    minTimeToMetro: 21,
                    lineColors: [
                        'ffa8af'
                    ],
                    rgbColor: 'ffa8af'
                },
                {
                    metroGeoId: 218561,
                    name: 'Красный Балтиец',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 27,
                    latitude: 55.815483,
                    longitude: 37.526627,
                    minTimeToMetro: 27,
                    lineColors: [
                        'df477c'
                    ],
                    rgbColor: 'df477c'
                }
            ],
            airports: [
                {
                    id: '878042',
                    name: 'Шереметьево',
                    timeOnCar: 1288,
                    distanceOnCar: 18665,
                    latitude: 55.963852,
                    longitude: 37.4169,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 21,
                            distance: 18665
                        }
                    ]
                },
                {
                    id: '878065',
                    name: 'Внуково',
                    timeOnCar: 3313,
                    distanceOnCar: 46326,
                    latitude: 55.604942,
                    longitude: 37.282578,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 55,
                            distance: 46326
                        }
                    ]
                },
                {
                    id: '858742',
                    name: 'Домодедово',
                    timeOnCar: 3955,
                    distanceOnCar: 61565,
                    latitude: 55.41435,
                    longitude: 37.90048,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 65,
                            distance: 61565
                        }
                    ]
                },
                {
                    id: '878109',
                    name: 'Жуковский (Раменское)',
                    timeOnCar: 4121,
                    distanceOnCar: 62146,
                    latitude: 55.568665,
                    longitude: 38.143654,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 68,
                            distance: 62146
                        }
                    ]
                }
            ],
            subjectFederationName: 'Москва и МО',
            cityCenter: [
                {
                    transport: 'ON_CAR',
                    time: 1536,
                    distance: 15133,
                    latitude: 55.755253,
                    longitude: 37.616386,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 25,
                            distance: 15133
                        }
                    ]
                }
            ]
        },
        minicardImages: [],
        newBuilding: true,
        obsolete: false,
        offerId: '8224807745834868445',
        offerCategory: 'APARTMENT',
        offerType: 'SELL',
        openPlan: false,
        price: {
            currency: 'RUR',
            value: 11000000,
            period: 'WHOLE_LIFE',
            unit: 'WHOLE_OFFER',
            trend: 'UNCHANGED',
            hasPriceHistory: false,
            valuePerPart: 174686,
            unitPerPart: 'SQUARE_METER',
            valueForWhole: 11000000,
            unitForWhole: 'WHOLE_OFFER',
            price: {
                value: 11000000,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE'
            },
            pricePerPart: {
                value: 174686,
                currency: 'RUB',
                priceType: 'PER_METER',
                pricingPeriod: 'WHOLE_LIFE'
            },
            priceForWhole: {
                value: 11000000,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE'
            }
        },
        primarySaleV2: false,
        remoteReview: {
            onlineShow: false,
            youtubeVideoReviewUrl: ''
        },
        roomsTotal: 2,
        totalImages: 3,
        transactionConditionsMap: {
            MORTGAGE: true
        },
        tuzInfo: {
            campaignId: 'd57f26ca-3590-406b-8a69-14fef0b9c570',
            active: true,
            tuzParams: [
                {
                    key: 'tuzParamRgid',
                    value: '741964'
                },
                {
                    key: 'tuzParamType',
                    value: 'SELL'
                },
                {
                    key: 'tuzParamCategory',
                    value: 'APARTMENT'
                },
                {
                    key: 'tuzParamPartner',
                    value: '1069250970'
                },
                {
                    key: 'tuzParamUid',
                    value: '4045447503'
                },
                {
                    key: 'tuzParamClass',
                    value: 'BUSINESS'
                },
                {
                    key: 'tuzParamReassignment',
                    value: 'yes'
                }
            ],
            tuzFeatured: true,
            premium: true,
            promotion: true,
            raising: true,
            clientId: 135109318,
            tuzType: {
                maximum: {}
            }
        },
        updateDate: '2019-01-23T06:16:00Z',
        vas: {
            raised: true,
            premium: true,
            placement: false,
            promoted: true,
            turboSale: false,
            raisingSale: false,
            tuzFeatured: true,
            vasAvailable: true
        },
        withExcerpt: false
    }
];

export const getVillageOffers = () => [
    {
        appMiddleImages: [],
        appLargeImages: [],
        area: {
            value: 481,
            unit: 'SQUARE_METER'
        },
        author: {
            id: '0',
            category: 'AGENCY',
            organization: 'Агентство недвижимости Vesco Realty',
            agentName: 'Vesco-Realty',
            photo: '',
            creationDate: '2018-09-11T13:23:50Z',
            redirectPhones: true,
            redirectPhonesFailed: false,
            name: 'Агентство недвижимости Vesco Realty',
            encryptedPhoneNumbers: [
                {
                    phone: 'KzcF0OHTUJyMLTUNwOPDUR3',
                    redirectId: '+74952150857'
                }
            ],
            encryptedPhones: [
                'KzcF0OHTUJyMLTUNwOPDUR3'
            ]
        },
        building: {
            heatingType: 'UNKNOWN'
        },
        creationDate: '2019-01-09T17:21:36Z',
        fullImages: [],
        house: {
            housePart: false,
            houseType: 'HOUSE'
        },
        location: {
            rgid: 195482,
            geoId: 121682,
            subjectFederationId: 1,
            settlementRgid: 195482,
            settlementGeoId: 121682,
            address: 'Москва, поселение Сосенское, деревня Летово, коттеджный посёлок Бельгийская деревня',
            // eslint-disable-next-line max-len
            geocoderAddress: 'Россия, Москва, поселение Сосенское, деревня Летово, коттеджный посёлок Бельгийская деревня',
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
                        value: 'Новомосковский административный округ',
                        geoId: 213,
                        regionGraphId: '587795',
                        address: 'Новомосковский административный округ',
                        regionType: 'SUBJECT_FEDERATION_DISTRICT',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва, Новомосковский административный округ'
                        }
                    },
                    {
                        value: 'поселение Сосенское',
                        geoId: 213,
                        regionGraphId: '587795',
                        address: 'поселение Сосенское',
                        regionType: 'SUBJECT_FEDERATION_DISTRICT',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва, Новомосковский административный округ, поселение Сосенское'
                        }
                    },
                    {
                        value: 'деревня Летово',
                        geoId: 213,
                        regionGraphId: '587795',
                        address: 'деревня Летово',
                        regionType: 'CITY',
                        queryParams: {
                            rgid: '587795',
                            // eslint-disable-next-line max-len
                            address: 'Россия, Москва, Новомосковский административный округ, поселение Сосенское, деревня Летово'
                        }
                    },
                    {
                        value: 'коттеджный посёлок Бельгийская деревня',
                        geoId: 213,
                        regionGraphId: '587795',
                        address: 'коттеджный посёлок Бельгийская деревня',
                        regionType: 'CITY',
                        queryParams: {
                            rgid: '587795',
                            // eslint-disable-next-line max-len
                            address: 'Россия, Москва, Новомосковский административный округ, поселение Сосенское, деревня Летово, коттеджный посёлок Бельгийская деревня'
                        }
                    }
                ]
            },
            point: {
                latitude: 55.562,
                longitude: 37.38649,
                precision: 'EXACT'
            },
            metro: {
                metroGeoId: 218465,
                name: 'Ольховая',
                metroTransport: 'ON_TRANSPORT',
                timeToMetro: 30,
                latitude: 55.56898,
                longitude: 37.459057,
                minTimeToMetro: 30,
                lineColors: [
                    'e4402d'
                ],
                rgbColor: 'e4402d'
            },
            highway: {
                name: 'Калужское шоссе',
                distanceKm: 12.906
            },
            station: {
                name: 'Новопеределкино',
                distanceKm: 8.412
            },
            metroList: [
                {
                    metroGeoId: 218465,
                    name: 'Ольховая',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 30,
                    latitude: 55.56898,
                    longitude: 37.459057,
                    minTimeToMetro: 30,
                    lineColors: [
                        'e4402d'
                    ],
                    rgbColor: 'e4402d'
                },
                {
                    metroGeoId: 218464,
                    name: 'Коммунарка',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 37,
                    latitude: 55.560074,
                    longitude: 37.46865,
                    minTimeToMetro: 37,
                    lineColors: [
                        'e4402d'
                    ],
                    rgbColor: 'e4402d'
                },
                {
                    metroGeoId: 144826,
                    name: 'Саларьево',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 39,
                    latitude: 55.621815,
                    longitude: 37.424057,
                    minTimeToMetro: 39,
                    lineColors: [
                        'e4402d'
                    ],
                    rgbColor: 'e4402d'
                },
                {
                    metroGeoId: 218467,
                    name: 'Филатов Луг',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 43,
                    latitude: 55.60118,
                    longitude: 37.407997,
                    minTimeToMetro: 43,
                    lineColors: [
                        'e4402d'
                    ],
                    rgbColor: 'e4402d'
                }
            ],
            routeDistances: [
                {
                    geoPoint: {
                        latitude: 55.610943,
                        longitude: 37.491734
                    },
                    distance: 12906,
                    highway: {
                        id: '25',
                        name: 'Калужское шоссе'
                    }
                },
                {
                    geoPoint: {
                        latitude: 55.6387,
                        longitude: 37.459286
                    },
                    distance: 14761,
                    highway: {
                        id: '11',
                        name: 'Киевское шоссе'
                    }
                }
            ],
            subjectFederationName: 'Москва и МО'
        },
        lot: {
            lotArea: {
                value: 20,
                unit: 'ARE'
            },
            lotType: 'IGS'
        },
        minicardImages: [],
        newBuilding: false,
        obsolete: false,
        offerId: '5179413035661422791',
        offerCategory: 'HOUSE',
        offerType: 'SELL',
        openPlan: false,
        predictions: {
            predictedPrice: {
                min: '6685000',
                max: '8171000',
                value: '7428000'
            }
        },
        price: {
            currency: 'RUR',
            value: 94500000,
            period: 'WHOLE_LIFE',
            unit: 'WHOLE_OFFER',
            trend: 'UNCHANGED',
            hasPriceHistory: false,
            valuePerPart: 196466,
            unitPerPart: 'SQUARE_METER',
            valueForWhole: 94500000,
            unitForWhole: 'WHOLE_OFFER',
            price: {
                value: 94500000,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE'
            },
            pricePerPart: {
                value: 196466,
                currency: 'RUB',
                priceType: 'PER_METER',
                pricingPeriod: 'WHOLE_LIFE'
            },
            priceForWhole: {
                value: 94500000,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE'
            }
        },
        primarySaleV2: false,
        remoteReview: {
            onlineShow: false,
            youtubeVideoReviewUrl: ''
        },
        roomsTotal: 6,
        totalImages: 14,
        updateDate: '2019-01-23T06:16:00Z',
        vas: {
            raised: false,
            premium: false,
            placement: false,
            promoted: false,
            turboSale: false,
            raisingSale: false,
            tuzFeatured: false,
            vasAvailable: true
        },
        village: {
            id: '1828507',
            name: 'Бельгийская деревня',
            fullName: 'Коттеджный посёлок «Бельгийская деревня»'
        },
        withExcerpt: false
    },
    {
        appMiddleImages: [],
        appLargeImages: [],
        area: {
            value: 481,
            unit: 'SQUARE_METER'
        },
        author: {
            id: '0',
            category: 'AGENCY',
            organization: 'Агентство недвижимости Vesco Realty',
            agentName: 'Vesco-Realty',
            photo: '',
            creationDate: '2018-09-11T13:23:50Z',
            redirectPhones: true,
            redirectPhonesFailed: false,
            name: 'Агентство недвижимости Vesco Realty',
            encryptedPhoneNumbers: [
                {
                    phone: 'KzcF0OHTUJyMLTUNwOPDUR3',
                    redirectId: '+74952150857'
                }
            ],
            encryptedPhones: [
                'KzcF0OHTUJyMLTUNwOPDUR3'
            ]
        },
        building: {
            heatingType: 'UNKNOWN'
        },
        creationDate: '2019-01-09T17:21:36Z',
        fullImages: [],
        house: {
            housePart: false,
            houseType: 'HOUSE'
        },
        location: {
            rgid: 195482,
            geoId: 121682,
            subjectFederationId: 1,
            settlementRgid: 195482,
            settlementGeoId: 121682,
            address: 'Москва, поселение Сосенское, деревня Летово, коттеджный посёлок Бельгийская деревня',
            // eslint-disable-next-line max-len
            geocoderAddress: 'Россия, Москва, поселение Сосенское, деревня Летово, коттеджный посёлок Бельгийская деревня',
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
                        value: 'Новомосковский административный округ',
                        geoId: 213,
                        regionGraphId: '587795',
                        address: 'Новомосковский административный округ',
                        regionType: 'SUBJECT_FEDERATION_DISTRICT',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва, Новомосковский административный округ'
                        }
                    },
                    {
                        value: 'поселение Сосенское',
                        geoId: 213,
                        regionGraphId: '587795',
                        address: 'поселение Сосенское',
                        regionType: 'SUBJECT_FEDERATION_DISTRICT',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва, Новомосковский административный округ, поселение Сосенское'
                        }
                    },
                    {
                        value: 'деревня Летово',
                        geoId: 213,
                        regionGraphId: '587795',
                        address: 'деревня Летово',
                        regionType: 'CITY',
                        queryParams: {
                            rgid: '587795',
                            // eslint-disable-next-line max-len
                            address: 'Россия, Москва, Новомосковский административный округ, поселение Сосенское, деревня Летово'
                        }
                    },
                    {
                        value: 'коттеджный посёлок Бельгийская деревня',
                        geoId: 213,
                        regionGraphId: '587795',
                        address: 'коттеджный посёлок Бельгийская деревня',
                        regionType: 'CITY',
                        queryParams: {
                            rgid: '587795',
                            // eslint-disable-next-line max-len
                            address: 'Россия, Москва, Новомосковский административный округ, поселение Сосенское, деревня Летово, коттеджный посёлок Бельгийская деревня'
                        }
                    }
                ]
            },
            point: {
                latitude: 55.562,
                longitude: 37.38649,
                precision: 'EXACT'
            },
            metro: {
                metroGeoId: 218465,
                name: 'Ольховая',
                metroTransport: 'ON_TRANSPORT',
                timeToMetro: 30,
                latitude: 55.56898,
                longitude: 37.459057,
                minTimeToMetro: 30,
                lineColors: [
                    'e4402d'
                ],
                rgbColor: 'e4402d'
            },
            highway: {
                name: 'Калужское шоссе',
                distanceKm: 12.906
            },
            station: {
                name: 'Новопеределкино',
                distanceKm: 8.412
            },
            metroList: [
                {
                    metroGeoId: 218465,
                    name: 'Ольховая',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 30,
                    latitude: 55.56898,
                    longitude: 37.459057,
                    minTimeToMetro: 30,
                    lineColors: [
                        'e4402d'
                    ],
                    rgbColor: 'e4402d'
                },
                {
                    metroGeoId: 218464,
                    name: 'Коммунарка',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 37,
                    latitude: 55.560074,
                    longitude: 37.46865,
                    minTimeToMetro: 37,
                    lineColors: [
                        'e4402d'
                    ],
                    rgbColor: 'e4402d'
                },
                {
                    metroGeoId: 144826,
                    name: 'Саларьево',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 39,
                    latitude: 55.621815,
                    longitude: 37.424057,
                    minTimeToMetro: 39,
                    lineColors: [
                        'e4402d'
                    ],
                    rgbColor: 'e4402d'
                },
                {
                    metroGeoId: 218467,
                    name: 'Филатов Луг',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 43,
                    latitude: 55.60118,
                    longitude: 37.407997,
                    minTimeToMetro: 43,
                    lineColors: [
                        'e4402d'
                    ],
                    rgbColor: 'e4402d'
                }
            ],
            routeDistances: [
                {
                    geoPoint: {
                        latitude: 55.610943,
                        longitude: 37.491734
                    },
                    distance: 12906,
                    highway: {
                        id: '25',
                        name: 'Калужское шоссе'
                    }
                },
                {
                    geoPoint: {
                        latitude: 55.6387,
                        longitude: 37.459286
                    },
                    distance: 14761,
                    highway: {
                        id: '11',
                        name: 'Киевское шоссе'
                    }
                }
            ],
            subjectFederationName: 'Москва и МО'
        },
        lot: {
            lotArea: {
                value: 20,
                unit: 'ARE'
            },
            lotType: 'IGS'
        },
        minicardImages: [],
        newBuilding: false,
        obsolete: false,
        offerId: '5179413035661422790',
        offerCategory: 'HOUSE',
        offerType: 'SELL',
        openPlan: false,
        predictions: {
            predictedPrice: {
                min: '6685000',
                max: '8171000',
                value: '7428000'
            }
        },
        price: {
            currency: 'RUR',
            value: 94500000,
            period: 'WHOLE_LIFE',
            unit: 'WHOLE_OFFER',
            trend: 'UNCHANGED',
            hasPriceHistory: false,
            valuePerPart: 196466,
            unitPerPart: 'SQUARE_METER',
            valueForWhole: 94500000,
            unitForWhole: 'WHOLE_OFFER',
            price: {
                value: 94500000,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE'
            },
            pricePerPart: {
                value: 196466,
                currency: 'RUB',
                priceType: 'PER_METER',
                pricingPeriod: 'WHOLE_LIFE'
            },
            priceForWhole: {
                value: 94500000,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE'
            }
        },
        primarySaleV2: false,
        remoteReview: {
            onlineShow: false,
            youtubeVideoReviewUrl: ''
        },
        roomsTotal: 6,
        totalImages: 14,
        updateDate: '2019-01-23T06:16:00Z',
        vas: {
            raised: false,
            premium: false,
            placement: false,
            promoted: false,
            turboSale: false,
            raisingSale: false,
            tuzFeatured: false,
            vasAvailable: true
        },
        village: {
            id: '1828507',
            name: 'Бельгийская деревня',
            fullName: 'Коттеджный посёлок «Бельгийская деревня»'
        },
        withExcerpt: false
    }
];
