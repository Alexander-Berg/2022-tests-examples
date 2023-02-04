const getOfferSnippets = () => [
    {
        appMiddleImages: [],
        appLargeImages: [],
        area: {
            value: 35.7,
            unit: 'SQUARE_METER'
        },
        author: {
            id: '0',
            category: 'AGENCY',
            organization: 'Астория',
            agentName: 'Пьянова Ирина',
            photo: '',
            creationDate: '2015-06-04T19:53:58Z',
            redirectPhonesFailed: false,
            redirectPhones: true,
            name: 'Астория',
            encryptedPhoneNumbers: [
                {
                    phone: 'KzcF5NHjgJ0NLTENzOPDgR3',
                    redirectId: '+79684513887'
                },
                {
                    phone: 'KzcF5MHjYJzMLzMNyMPDYRy',
                    redirectId: '+79263332062'
                }
            ],
            encryptedPhones: [
                'KzcF5NHjgJ0NLTENzOPDgR3',
                'KzcF5MHjYJzMLzMNyMPDYRy'
            ]
        },
        building: {
            builtYear: 1999,
            buildingType: 'PANEL',
            improvements: {
                LIFT: true,
                RUBBISH_CHUTE: true,
                GUARDED: false,
                SECURITY: false
            },
            buildingId: '5698646160730427077',
            flatsCount: 200,
            porchesCount: 5,
            heatingType: 'UNKNOWN',
            priceStatistics: {
                sellPricePerSquareMeter: {
                    level: 0,
                    maxLevel: 9,
                    rgbColor: '',
                    value: '0',
                    regionValueFrom: '22727',
                    regionValueTo: '2234148'
                },
                sellPriceByRooms: {
                    1: {
                        level: 9,
                        maxLevel: 9,
                        rgbColor: '128000',
                        value: '3500000',
                        regionValueFrom: '300000',
                        regionValueTo: '1989368448'
                    },
                    2: {
                        level: 9,
                        maxLevel: 9,
                        rgbColor: '128000',
                        value: '4800000',
                        regionValueFrom: '700000',
                        regionValueTo: '350577600'
                    }
                }
            },
            buildingImprovementsMap: {
                LIFT: true,
                RUBBISH_CHUTE: true,
                GUARDED: false,
                SECURITY: false
            }
        },
        creationDate: '2019-01-23T12:05:58Z',
        flatType: 'SECONDARY',
        floorsOffered: [
            5
        ],
        floorsTotal: 10,
        fullImages: [],
        house: {
            bathroomUnit: 'SEPARATED',
            housePart: false
        },
        livingSpace: {
            value: 18,
            unit: 'SQUARE_METER'
        },
        location: {
            rgid: 593515,
            geoId: 37147,
            subjectFederationId: 1,
            settlementRgid: 592996,
            settlementGeoId: 10747,
            address: 'Московская область, Подольск, микрорайон Климовск, улица 8 Марта, 12',
            geocoderAddress: 'Россия, Московская область, Подольск, микрорайон Климовск, улица 8 Марта, 12',
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
                        value: 'Подольск',
                        geoId: 10747,
                        regionGraphId: '592996',
                        address: 'Подольск',
                        regionType: 'CITY',
                        queryParams: {
                            rgid: '592996',
                            address: 'Россия, Московская область, Подольск'
                        }
                    },
                    {
                        value: 'Климовск',
                        geoId: 37147,
                        regionGraphId: '593515',
                        address: 'микрорайон Климовск',
                        regionType: 'NOT_ADMINISTRATIVE_DISTRICT',
                        queryParams: {
                            rgid: '593515',
                            address: 'Россия, Московская область, Подольск, микрорайон Климовск'
                        }
                    },
                    {
                        value: 'улица 8 Марта',
                        geoId: 37147,
                        regionGraphId: '593515',
                        address: 'улица 8 Марта',
                        regionType: 'STREET',
                        queryParams: {
                            rgid: '593515',
                            address: 'Россия, Московская область, Подольск, микрорайон Климовск, улица 8 Марта'
                        }
                    },
                    {
                        value: '12',
                        geoId: 37147,
                        regionGraphId: '593515',
                        address: '12',
                        regionType: 'HOUSE',
                        queryParams: {
                            rgid: '593515',
                            address: 'Россия, Московская область, Подольск, микрорайон Климовск, улица 8 Марта, 12'
                        }
                    }
                ]
            },
            point: {
                latitude: 55.3592,
                longitude: 37.538242,
                precision: 'EXACT'
            },
            metro: {
                metroGeoId: 218598,
                name: 'Подольск',
                metroTransport: 'ON_TRANSPORT',
                timeToMetro: 23,
                latitude: 55.4315,
                longitude: 37.565456,
                minTimeToMetro: 23,
                lineColors: [
                    'df477c'
                ],
                rgbColor: 'df477c'
            },
            station: {
                name: 'Гривно',
                distanceKm: 0.762
            },
            streetAddress: 'улица 8 Марта, 12',
            metroList: [
                {
                    metroGeoId: 218598,
                    name: 'Подольск',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 23,
                    latitude: 55.4315,
                    longitude: 37.565456,
                    minTimeToMetro: 23,
                    lineColors: [
                        'df477c'
                    ],
                    rgbColor: 'df477c'
                },
                {
                    metroGeoId: 218586,
                    name: 'Силикатная',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 26,
                    latitude: 55.470463,
                    longitude: 37.555454,
                    minTimeToMetro: 26,
                    lineColors: [
                        'df477c'
                    ],
                    rgbColor: 'df477c'
                },
                {
                    metroGeoId: 218565,
                    name: 'Остафьево',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 30,
                    latitude: 55.486015,
                    longitude: 37.55516,
                    minTimeToMetro: 30,
                    lineColors: [
                        'df477c'
                    ],
                    rgbColor: 'df477c'
                },
                {
                    metroGeoId: 218571,
                    name: 'Щербинка',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 33,
                    latitude: 55.51016,
                    longitude: 37.5622,
                    minTimeToMetro: 33,
                    lineColors: [
                        'df477c'
                    ],
                    rgbColor: 'df477c'
                }
            ],
            airports: [
                {
                    id: '858742',
                    name: 'Домодедово',
                    timeOnCar: 2619,
                    distanceOnCar: 42127,
                    latitude: 55.41435,
                    longitude: 37.90048,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 43,
                            distance: 42127
                        }
                    ]
                },
                {
                    id: '878042',
                    name: 'Шереметьево',
                    timeOnCar: 4661,
                    distanceOnCar: 80945,
                    latitude: 55.963852,
                    longitude: 37.4169,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 77,
                            distance: 80945
                        }
                    ]
                },
                {
                    id: '878065',
                    name: 'Внуково',
                    timeOnCar: 4840,
                    distanceOnCar: 70374,
                    latitude: 55.604942,
                    longitude: 37.282578,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 80,
                            distance: 70374
                        }
                    ]
                },
                {
                    id: '878109',
                    name: 'Жуковский (Раменское)',
                    timeOnCar: 5011,
                    distanceOnCar: 82034,
                    latitude: 55.568665,
                    longitude: 38.143654,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 83,
                            distance: 82034
                        }
                    ]
                }
            ],
            heatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: 'fbae1e',
                    description: 'мало объектов',
                    level: 3,
                    maxLevel: 8,
                    title: 'Инфраструктура'
                },
                {
                    name: 'transport',
                    rgbColor: '620801',
                    description: 'низкая доступность',
                    level: 1,
                    maxLevel: 9,
                    title: 'Транспорт'
                }
            ],
            allHeatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: 'fbae1e',
                    description: 'мало объектов',
                    level: 3,
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
                    rgbColor: '620801',
                    description: 'низкая доступность',
                    level: 1,
                    maxLevel: 9,
                    title: 'Транспорт'
                }
            ],
            routeDistances: [
                {
                    geoPoint: {
                        latitude: 55.57579,
                        longitude: 37.596844
                    },
                    distance: 27594
                },
                {
                    geoPoint: {
                        latitude: 55.57579,
                        longitude: 37.596844
                    },
                    distance: 34531,
                    highway: {
                        id: '27',
                        name: 'Симферопольское шоссе'
                    }
                }
            ],
            subjectFederationName: 'Москва и МО',
            cityCenter: [
                {
                    transport: 'ON_CAR',
                    time: 3158,
                    distance: 48773,
                    latitude: 55.74816,
                    longitude: 37.613552,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 52,
                            distance: 48773
                        }
                    ]
                }
            ]
        },
        newBuilding: false,
        obsolete: false,
        offerId: '5152292921821022978',
        offerCategory: 'APARTMENT',
        offerType: 'SELL',
        openPlan: false,
        predictions: {
            predictedPrice: {
                min: '2218000',
                max: '2710000',
                value: '2464000'
            }
        },
        price: {
            currency: 'RUR',
            value: 3700000,
            period: 'WHOLE_LIFE',
            unit: 'WHOLE_OFFER',
            trend: 'UNCHANGED',
            hasPriceHistory: false,
            valuePerPart: 103641,
            unitPerPart: 'SQUARE_METER',
            valueForWhole: 3700000,
            unitForWhole: 'WHOLE_OFFER',
            price: {
                value: 3700000,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE'
            },
            pricePerPart: {
                value: 103641,
                currency: 'RUB',
                priceType: 'PER_METER',
                pricingPeriod: 'WHOLE_LIFE'
            },
            priceForWhole: {
                value: 3700000,
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
        totalImages: 0,
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
            value: 34,
            unit: 'SQUARE_METER'
        },
        author: {
            id: '0',
            category: 'AGENCY',
            organization: 'Астория',
            agentName: 'Владимир Квасков',
            photo: '',
            creationDate: '2015-06-04T19:53:58Z',
            redirectPhonesFailed: false,
            redirectPhones: true,
            name: 'Астория',
            encryptedPhoneNumbers: [
                {
                    phone: 'KzcF5NHjgJ0NLTENzOPDgRy',
                    redirectId: '+79684513882'
                },
                {
                    phone: 'KzcF5MHjYJzMLzMNyMPDYRy',
                    redirectId: '+79263332062'
                }
            ],
            encryptedPhones: [
                'KzcF5NHjgJ0NLTENzOPDgRy',
                'KzcF5MHjYJzMLzMNyMPDYRy'
            ]
        },
        building: {
            builtYear: 1999,
            buildingType: 'PANEL',
            improvements: {
                LIFT: true,
                RUBBISH_CHUTE: true,
                GUARDED: false,
                SECURITY: false
            },
            parkingType: 'OPEN',
            buildingId: '5698646160730427077',
            flatsCount: 200,
            porchesCount: 5,
            heatingType: 'UNKNOWN',
            priceStatistics: {
                sellPricePerSquareMeter: {
                    level: 0,
                    maxLevel: 9,
                    rgbColor: '',
                    value: '0',
                    regionValueFrom: '22727',
                    regionValueTo: '2234148'
                },
                sellPriceByRooms: {
                    1: {
                        level: 9,
                        maxLevel: 9,
                        rgbColor: '128000',
                        value: '3500000',
                        regionValueFrom: '300000',
                        regionValueTo: '1989368448'
                    },
                    2: {
                        level: 9,
                        maxLevel: 9,
                        rgbColor: '128000',
                        value: '4800000',
                        regionValueFrom: '700000',
                        regionValueTo: '350577600'
                    }
                }
            },
            buildingImprovementsMap: {
                LIFT: true,
                RUBBISH_CHUTE: true,
                GUARDED: false,
                SECURITY: false
            }
        },
        creationDate: '2019-01-25T18:22:34Z',
        flatType: 'SECONDARY',
        floorsOffered: [
            8
        ],
        floorsTotal: 10,
        fullImages: [],
        house: {
            bathroomUnit: 'SEPARATED',
            housePart: false
        },
        livingSpace: {
            value: 18,
            unit: 'SQUARE_METER'
        },
        location: {
            rgid: 593515,
            geoId: 37147,
            subjectFederationId: 1,
            settlementRgid: 592996,
            settlementGeoId: 10747,
            address: 'Московская область, Подольск, микрорайон Климовск, улица 8 Марта, 12',
            geocoderAddress: 'Россия, Московская область, Подольск, микрорайон Климовск, улица 8 Марта, 12',
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
                        value: 'Подольск',
                        geoId: 10747,
                        regionGraphId: '592996',
                        address: 'Подольск',
                        regionType: 'CITY',
                        queryParams: {
                            rgid: '592996',
                            address: 'Россия, Московская область, Подольск'
                        }
                    },
                    {
                        value: 'Климовск',
                        geoId: 37147,
                        regionGraphId: '593515',
                        address: 'микрорайон Климовск',
                        regionType: 'NOT_ADMINISTRATIVE_DISTRICT',
                        queryParams: {
                            rgid: '593515',
                            address: 'Россия, Московская область, Подольск, микрорайон Климовск'
                        }
                    },
                    {
                        value: 'улица 8 Марта',
                        geoId: 37147,
                        regionGraphId: '593515',
                        address: 'улица 8 Марта',
                        regionType: 'STREET',
                        queryParams: {
                            rgid: '593515',
                            address: 'Россия, Московская область, Подольск, микрорайон Климовск, улица 8 Марта'
                        }
                    },
                    {
                        value: '12',
                        geoId: 37147,
                        regionGraphId: '593515',
                        address: '12',
                        regionType: 'HOUSE',
                        queryParams: {
                            rgid: '593515',
                            address: 'Россия, Московская область, Подольск, микрорайон Климовск, улица 8 Марта, 12'
                        }
                    }
                ]
            },
            point: {
                latitude: 55.3592,
                longitude: 37.538242,
                precision: 'EXACT'
            },
            metro: {
                metroGeoId: 218598,
                name: 'Подольск',
                metroTransport: 'ON_TRANSPORT',
                timeToMetro: 23,
                latitude: 55.4315,
                longitude: 37.565456,
                minTimeToMetro: 23,
                lineColors: [
                    'df477c'
                ],
                rgbColor: 'df477c'
            },
            station: {
                name: 'Гривно',
                distanceKm: 0.762
            },
            streetAddress: 'улица 8 Марта, 12',
            metroList: [
                {
                    metroGeoId: 218598,
                    name: 'Подольск',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 23,
                    latitude: 55.4315,
                    longitude: 37.565456,
                    minTimeToMetro: 23,
                    lineColors: [
                        'df477c'
                    ],
                    rgbColor: 'df477c'
                },
                {
                    metroGeoId: 218586,
                    name: 'Силикатная',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 26,
                    latitude: 55.470463,
                    longitude: 37.555454,
                    minTimeToMetro: 26,
                    lineColors: [
                        'df477c'
                    ],
                    rgbColor: 'df477c'
                },
                {
                    metroGeoId: 218565,
                    name: 'Остафьево',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 30,
                    latitude: 55.486015,
                    longitude: 37.55516,
                    minTimeToMetro: 30,
                    lineColors: [
                        'df477c'
                    ],
                    rgbColor: 'df477c'
                },
                {
                    metroGeoId: 218571,
                    name: 'Щербинка',
                    metroTransport: 'ON_TRANSPORT',
                    timeToMetro: 33,
                    latitude: 55.51016,
                    longitude: 37.5622,
                    minTimeToMetro: 33,
                    lineColors: [
                        'df477c'
                    ],
                    rgbColor: 'df477c'
                }
            ],
            airports: [
                {
                    id: '858742',
                    name: 'Домодедово',
                    timeOnCar: 2619,
                    distanceOnCar: 42127,
                    latitude: 55.41435,
                    longitude: 37.90048,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 43,
                            distance: 42127
                        }
                    ]
                },
                {
                    id: '878042',
                    name: 'Шереметьево',
                    timeOnCar: 4661,
                    distanceOnCar: 80945,
                    latitude: 55.963852,
                    longitude: 37.4169,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 77,
                            distance: 80945
                        }
                    ]
                },
                {
                    id: '878065',
                    name: 'Внуково',
                    timeOnCar: 4840,
                    distanceOnCar: 70374,
                    latitude: 55.604942,
                    longitude: 37.282578,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 80,
                            distance: 70374
                        }
                    ]
                },
                {
                    id: '878109',
                    name: 'Жуковский (Раменское)',
                    timeOnCar: 5011,
                    distanceOnCar: 82034,
                    latitude: 55.568665,
                    longitude: 38.143654,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 83,
                            distance: 82034
                        }
                    ]
                }
            ],
            heatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: 'fbae1e',
                    description: 'мало объектов',
                    level: 3,
                    maxLevel: 8,
                    title: 'Инфраструктура'
                },
                {
                    name: 'transport',
                    rgbColor: '620801',
                    description: 'низкая доступность',
                    level: 1,
                    maxLevel: 9,
                    title: 'Транспорт'
                }
            ],
            allHeatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: 'fbae1e',
                    description: 'мало объектов',
                    level: 3,
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
                    rgbColor: '620801',
                    description: 'низкая доступность',
                    level: 1,
                    maxLevel: 9,
                    title: 'Транспорт'
                }
            ],
            routeDistances: [
                {
                    geoPoint: {
                        latitude: 55.57579,
                        longitude: 37.596844
                    },
                    distance: 27594
                },
                {
                    geoPoint: {
                        latitude: 55.57579,
                        longitude: 37.596844
                    },
                    distance: 34531,
                    highway: {
                        id: '27',
                        name: 'Симферопольское шоссе'
                    }
                }
            ],
            subjectFederationName: 'Москва и МО',
            cityCenter: [
                {
                    transport: 'ON_CAR',
                    time: 3158,
                    distance: 48773,
                    latitude: 55.74816,
                    longitude: 37.613552,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 52,
                            distance: 48773
                        }
                    ]
                }
            ]
        },
        newBuilding: false,
        obsolete: false,
        offerId: '5152292921820994243',
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
            value: 3300000,
            period: 'WHOLE_LIFE',
            unit: 'WHOLE_OFFER',
            trend: 'UNCHANGED',
            hasPriceHistory: false,
            valuePerPart: 97059,
            unitPerPart: 'SQUARE_METER',
            valueForWhole: 3300000,
            unitForWhole: 'WHOLE_OFFER',
            price: {
                value: 3300000,
                currency: 'RUB',
                priceType: 'PER_OFFER',
                pricingPeriod: 'WHOLE_LIFE'
            },
            pricePerPart: {
                value: 97059,
                currency: 'RUB',
                priceType: 'PER_METER',
                pricingPeriod: 'WHOLE_LIFE'
            },
            priceForWhole: {
                value: 3300000,
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
        totalImages: 0,
        updateDate: '2019-01-01T10:46:15Z',
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

export const getStateWithDifferentPins = () => ({
    user: {
        favorites: [
            'site_178702',
            '4856437566573186269',
            'site_62334',
            'site_518323',
            'village_1834057',
            '2975611977872477121',
            '6956308905447218176'
        ],
        favoritesMap: {
            site_178702: true,
            '4856437566573186269': true,
            site_62334: true,
            site_518323: true,
            village_1834057: true,
            '2975611977872477121': true,
            '6956308905447218176': true
        }
    },
    favoriteMap: {
        isSnippetLoading: false,
        isSnippetFailed: false,
        activePoint: null,
        snippets: [],
        points: {
            site: [
                {
                    id: 518323,
                    lat: 55.749718,
                    lon: 37.517025,
                    type: 'site',
                    developerId: 52308,
                    constructionState: 'UNDER_CONSTRUCTION'
                },
                {
                    id: 178702,
                    lat: 55.792053,
                    lon: 37.484074,
                    type: 'site',
                    developerId: 21331,
                    constructionState: 'HAND_OVER'
                },
                {
                    id: 62334,
                    lat: 55.724754,
                    lon: 37.4673,
                    type: 'site',
                    developerId: 32607,
                    constructionState: 'SUSPENDED'
                }
            ],
            offer: [
                {
                    id: '2975611977872477121',
                    favoriteOfferIds: [
                        '4856437566573186269',
                        '2975611977872477121'
                    ],
                    lat: 55.781105,
                    lon: 37.448956,
                    type: 'offer',
                    price: 39000000
                },
                {
                    id: '6956308905447218176',
                    favoriteOfferIds: [
                        '6956308905447218176'
                    ],
                    lat: 55.678616,
                    lon: 37.54931,
                    type: 'offer',
                    price: 8888888
                }
            ],
            village: [
                {
                    id: '1834057',
                    lat: 55.80333,
                    lon: 37.380283,
                    type: 'village'
                }
            ]
        }
    },
    siteSpecialProjects: [
        {
            params: {
                geoId: 1
            },
            data: {
                developerId: 52308,
                showPin: true
            }
        }
    ],
    geo: {
        id: 1
    }
});

export const getStateWithDistantPins = () => ({
    user: {
        favorites: [
            'site_1550821',
            'site_539057'
        ],
        favoritesMap: {
            site_1550821: true,
            site_539057: true
        }
    },
    favoriteMap: {
        isSnippetLoading: false,
        isSnippetFailed: false,
        activePoint: null,
        snippets: [],
        points: {
            site: [
                {
                    id: 1550821,
                    lat: 55.812855,
                    lon: 37.739807,
                    type: 'site',
                    developerId: 280116,
                    constructionState: 'UNDER_CONSTRUCTION'
                },
                {
                    id: 539057,
                    lat: 57.627174,
                    lon: 39.850693,
                    type: 'site',
                    developerId: 209705,
                    constructionState: 'UNDER_CONSTRUCTIONS_WITH_HAND_OVER'
                }
            ],
            offer: [],
            village: []
        }
    }
});

export const getStateWithSidebarLoading = () => ({
    user: {
        favorites: [
            'site_1632667',
            'site_189856'
        ],
        favoritesMap: {
            site_1632667: true,
            site_189856: true
        }
    },
    favoriteMap: {
        isSnippetLoading: true,
        activePoint: {
            id: 189856,
            lat: 55.79038,
            lon: 37.47448,
            type: 'site',
            developerId: 279844,
            constructionState: 'UNDER_CONSTRUCTION',
            geoId: 'site-point-55.79038,37.47448'
        },
        snippets: [],
        points: {
            site: [
                {
                    id: 189856,
                    lat: 55.79038,
                    lon: 37.47448,
                    type: 'site',
                    developerId: 279844,
                    constructionState: 'UNDER_CONSTRUCTION'
                }
            ],
            offer: [],
            village: []
        }
    }
});

export const getStateWithOpenedOffers = () => ({
    page: {
        name: 'page-name'
    },
    geo: {
        rgid: 741964
    },
    user: {
        favorites: [
            '5152292921820994243',
            '5152292921821022978'
        ],
        favoritesMap: {
            '5152292921820994243': true,
            '5152292921821022978': true
        },
        comparison: [],
        paymentTypeSuffix: 'natural'
    },
    offerNotes: {
        notes: []
    },
    favoriteMap: {
        isSnippetLoading: false,
        activePoint: {
            id: '5152292921820994243',
            favoriteOfferIds: [
                '5152292921820994243',
                '5152292921821022978'
            ],
            lat: 55.3592,
            lon: 37.538242,
            type: 'offer',
            price: 3300000,
            count: 2,
            geoId: 'offer-point-55.3592,37.538242'
        },
        snippets: getOfferSnippets(),
        points: {
            site: [],
            offer: [
                {
                    id: '5152292921820994243',
                    favoriteOfferIds: [
                        '5152292921820994243',
                        '5152292921821022978'
                    ],
                    lat: 55.3592,
                    lon: 37.538242,
                    type: 'offer',
                    price: 3300000
                }
            ],
            village: []
        }
    }
});

export const getStateWithOpenedOffer = () => ({
    page: {
        name: 'page-name'
    },
    geo: {
        rgid: 741964
    },
    user: {
        favorites: [
            '5152292921821022978'
        ],
        favoritesMap: {
            '5152292921821022978': true
        },
        comparison: [],
        paymentTypeSuffix: 'natural'
    },
    offerNotes: {
        notes: []
    },
    favoriteMap: {
        isSnippetLoading: false,
        activePoint: {
            id: '5152292921821022978',
            favoriteOfferIds: [
                '5152292921821022978'
            ],
            lat: 55.3592,
            lon: 37.538242,
            type: 'offer',
            price: 3300000,
            count: 2,
            geoId: 'offer-point-55.3592,37.538242'
        },
        snippets: [ getOfferSnippets()[0] ],
        points: {
            site: [],
            offer: [
                {
                    id: '5152292921821022978',
                    favoriteOfferIds: [
                        '5152292921821022978'
                    ],
                    lat: 55.3592,
                    lon: 37.538242,
                    type: 'offer',
                    price: 3300000
                }
            ],
            village: []
        }
    }
});

export const getStateWithSiteSnippet = () => ({
    user: {
        favorites: [
            'site_189856'
        ],
        favoritesMap: {
            site_189856: true
        }
    },
    favoriteMap: {
        isSnippetLoading: false,
        isSnippetFailed: false,
        activePoint: {
            id: 189856,
            lat: 55.79038,
            lon: 37.47448,
            type: 'site',
            developerId: 279844,
            constructionState: 'UNDER_CONSTRUCTION',
            geoId: 'site-point-55.79038,37.47448'
        },
        snippets: [
            {
                id: 189856,
                name: 'Октябрьское поле',
                fullName: 'ЖК «Октябрьское поле»',
                locativeFullName: 'в ЖК «Октябрьское поле»',
                location: {
                    geoId: 117054,
                    rgid: 12439,
                    settlementRgid: 165705,
                    settlementGeoId: 213,
                    address: 'Москва, ул. Берзарина',
                    subjectFederationId: 1,
                    subjectFederationRgid: 741964,
                    subjectFederationName: 'Москва и МО',
                    point: {
                        latitude: 55.79038,
                        longitude: 37.47448,
                        precision: 'EXACT'
                    },
                    expectedMetroList: [],
                    schools: [],
                    parks: [],
                    ponds: [],
                    airports: [
                        {
                            id: '878042',
                            name: 'Шереметьево',
                            timeOnCar: 1720,
                            distanceOnCar: 24251,
                            latitude: 55.963852,
                            longitude: 37.4169,
                            timeDistanceList: [
                                {
                                    transport: 'ON_CAR',
                                    time: 28,
                                    distance: 24251
                                }
                            ]
                        },
                        {
                            id: '878065',
                            name: 'Внуково',
                            timeOnCar: 2818,
                            distanceOnCar: 29979,
                            latitude: 55.604942,
                            longitude: 37.282578,
                            timeDistanceList: [
                                {
                                    transport: 'ON_CAR',
                                    time: 46,
                                    distance: 29979
                                }
                            ]
                        },
                        {
                            id: '858742',
                            name: 'Домодедово',
                            timeOnCar: 3730,
                            distanceOnCar: 57417,
                            latitude: 55.41435,
                            longitude: 37.90048,
                            timeDistanceList: [
                                {
                                    transport: 'ON_CAR',
                                    time: 62,
                                    distance: 57417
                                }
                            ]
                        },
                        {
                            id: '878109',
                            name: 'Жуковский (Раменское)',
                            timeOnCar: 4041,
                            distanceOnCar: 60692,
                            latitude: 55.568665,
                            longitude: 38.143654,
                            timeDistanceList: [
                                {
                                    transport: 'ON_CAR',
                                    time: 67,
                                    distance: 60692
                                }
                            ]
                        }
                    ],
                    cityCenter: [
                        {
                            transport: 'ON_CAR',
                            time: 1499,
                            distance: 16322,
                            latitude: 55.749058,
                            longitude: 37.612267,
                            timeDistanceList: [
                                {
                                    transport: 'ON_CAR',
                                    time: 24,
                                    distance: 16322
                                }
                            ]
                        }
                    ],
                    heatmaps: [
                        {
                            name: 'infrastructure',
                            rgbColor: 'fbae1e',
                            description: 'мало объектов',
                            level: 3,
                            maxLevel: 8,
                            title: 'Инфраструктура'
                        },
                        {
                            name: 'transport',
                            rgbColor: 'ffe424',
                            description: 'выше среднего',
                            level: 5,
                            maxLevel: 9,
                            title: 'Транспорт'
                        }
                    ],
                    allHeatmaps: [
                        {
                            name: 'infrastructure',
                            rgbColor: 'fbae1e',
                            description: 'мало объектов',
                            level: 3,
                            maxLevel: 8,
                            title: 'Инфраструктура'
                        },
                        {
                            name: 'price-rent',
                            rgbColor: '9ada1b',
                            description: 'средняя',
                            level: 6,
                            maxLevel: 9,
                            title: 'Цена аренды'
                        },
                        {
                            name: 'price-sell',
                            rgbColor: 'ffe424',
                            description: 'выше средней',
                            level: 5,
                            maxLevel: 9,
                            title: 'Цена продажи'
                        },
                        {
                            name: 'profitability',
                            rgbColor: '9ada1b',
                            description: 'выше средней',
                            level: 6,
                            maxLevel: 9,
                            title: 'Прогноз окупаемости'
                        },
                        {
                            name: 'transport',
                            rgbColor: 'ffe424',
                            description: 'выше среднего',
                            level: 5,
                            maxLevel: 9,
                            title: 'Транспорт'
                        }
                    ],
                    insideMKAD: true,
                    routeDistances: [],
                    metro: {
                        lineColors: [
                            'b1179a'
                        ],
                        metroGeoId: 20368,
                        rgbColor: 'b1179a',
                        metroTransport: 'ON_FOOT',
                        name: 'Октябрьское Поле',
                        timeToMetro: 19
                    },
                    metroList: [
                        {
                            lineColors: [
                                'b1179a'
                            ],
                            metroGeoId: 20368,
                            rgbColor: 'b1179a',
                            metroTransport: 'ON_FOOT',
                            name: 'Октябрьское Поле',
                            timeToMetro: 19
                        },
                        {
                            lineColors: [
                                'b1179a'
                            ],
                            metroGeoId: 20367,
                            rgbColor: 'b1179a',
                            metroTransport: 'ON_TRANSPORT',
                            name: 'Щукинская',
                            timeToMetro: 16
                        },
                        {
                            lineColors: [
                                'ffa8af'
                            ],
                            metroGeoId: 152920,
                            rgbColor: 'ffa8af',
                            metroTransport: 'ON_TRANSPORT',
                            name: 'Панфиловская',
                            timeToMetro: 19
                        },
                        {
                            lineColors: [
                                '4f8242'
                            ],
                            metroGeoId: 20372,
                            rgbColor: '4f8242',
                            metroTransport: 'ON_TRANSPORT',
                            name: 'Сокол',
                            timeToMetro: 22
                        },
                        {
                            lineColors: [
                                'ffa8af'
                            ],
                            metroGeoId: 152919,
                            rgbColor: 'ffa8af',
                            metroTransport: 'ON_TRANSPORT',
                            name: 'Зорге',
                            timeToMetro: 24
                        }
                    ]
                },
                viewTypes: [],
                images: [],
                appLargeImages: [],
                appLargeSnippetImages: [],
                minicardImages: [],
                siteSpecialProposals: [
                    {
                        proposalType: 'SALE',
                        description: 'Господдержка 2020 — Ипотека 5,85%',
                        endDate: '2020-11-01T00:00:00.000+03:00',
                        mainProposal: true,
                        specialProposalType: 'sale'
                    },
                    {
                        proposalType: 'GIFT',
                        description: 'Отделка в подарок',
                        mainProposal: false,
                        specialProposalType: 'gift'
                    },
                    {
                        proposalType: 'MORTGAGE',
                        description: 'Ипотека 4.5%',
                        minRate: '4.5',
                        mainProposal: false,
                        specialProposalType: 'mortgage'
                    }
                ],
                buildingClass: 'BUSINESS',
                state: 'UNFINISHED',
                finishedApartments: true,
                price: {
                    from: 5895795,
                    to: 29752450,
                    currency: 'RUR',
                    minPricePerMeter: 173000,
                    maxPricePerMeter: 251500,
                    rooms: {
                        1: {
                            soldout: false,
                            from: 6786725,
                            to: 8959680,
                            currency: 'RUR',
                            areas: {
                                from: '33.3',
                                to: '44.9'
                            },
                            hasOffers: false,
                            priceRatioToMarket: 0,
                            status: 'ON_SALE'
                        },
                        2: {
                            soldout: false,
                            from: 9258960,
                            to: 17697000,
                            currency: 'RUR',
                            areas: {
                                from: '51.9',
                                to: '75.1'
                            },
                            hasOffers: false,
                            priceRatioToMarket: 0,
                            status: 'ON_SALE'
                        },
                        3: {
                            soldout: false,
                            from: 14241900,
                            to: 23986040,
                            currency: 'RUR',
                            areas: {
                                from: '81.6',
                                to: '96.9'
                            },
                            hasOffers: false,
                            priceRatioToMarket: 0,
                            status: 'ON_SALE'
                        },
                        OPEN_PLAN: {
                            soldout: false,
                            currency: 'RUR',
                            hasOffers: false,
                            priceRatioToMarket: 0
                        },
                        STUDIO: {
                            soldout: false,
                            from: 5895795,
                            to: 6605415,
                            currency: 'RUR',
                            areas: {
                                from: '28.7',
                                to: '30.5'
                            },
                            hasOffers: false,
                            priceRatioToMarket: 0,
                            status: 'ON_SALE'
                        },
                        PLUS_4: {
                            soldout: false,
                            from: 17338060,
                            to: 29752450,
                            currency: 'RUR',
                            areas: {
                                from: '100.2',
                                to: '118.3'
                            },
                            hasOffers: false,
                            priceRatioToMarket: 0,
                            status: 'ON_SALE'
                        }
                    },
                    totalOffers: 0,
                    priceRatioToMarket: 0
                },
                flatStatus: 'ON_SALE',
                developers: [
                    {
                        id: 279844,
                        name: 'РГ-Девелопмент',
                        url: 'https://rg-dev.ru/',
                        logo: '',
                        objects: {
                            all: 8,
                            salesOpened: 7,
                            finished: 4,
                            unfinished: 4,
                            suspended: 0
                        },
                        address: 'Москва, ул. Правды, 26',
                        born: '2012-12-31T20:00:00Z',
                        encryptedPhones: [
                            {
                                phoneWithMask: '+7 495 104 ×× ××',
                                phoneHash: 'KzcF0OHTUJxMLDQN3MPjARz'
                            }
                        ]
                    }
                ],
                salesDepartment: {
                    id: 1706982,
                    name: 'РГ-Девелопмент',
                    weekTimetable: [
                        {
                            dayFrom: 1,
                            dayTo: 7,
                            timePattern: [
                                {
                                    open: '09:00',
                                    close: '21:00'
                                }
                            ]
                        }
                    ],
                    logo: '',
                    phonesWithTag: [
                        {
                            tag: '',
                            phone: '+74951062033'
                        }
                    ],
                    statParams: '',
                    encryptedPhones: [
                        {
                            phoneWithMask: '+7 495 106 ×× ××',
                            phoneHash: 'KzcF0OHTUJxMLDYNyMPDMRz'
                        }
                    ],
                    encryptedDump: ''
                },
                phone: {
                    phoneWithMask: '+7 495 106 ×× ××',
                    phoneHash: 'KzcF0OHTUJxMLDYNyMPDMRz'
                },
                backCallTrafficInfo: {},
                withBilling: true,
                withCustomization: true,
                awards: {
                    movemsk: {
                        endDate: '2020-09-16T21:00:00.000Z',
                        url: 'https://moverealtyawards.ru/'
                    }
                }
            }
        ],
        points: {
            site: [
                {
                    id: 189856,
                    lat: 55.79038,
                    lon: 37.47448,
                    type: 'site',
                    developerId: 279844,
                    constructionState: 'UNDER_CONSTRUCTIONS_WITH_HAND_OVER'
                }
            ],
            offer: [],
            village: []
        }
    }
});

export const getStateWithVillageSnippet = () => ({
    user: {
        favorites: [
            'village_1832934'
        ],
        favoritesMap: {
            village_1832934: true
        }
    },
    favoriteMap: {
        isSnippetLoading: false,
        isSnippetFailed: false,
        activePoint: {
            id: '1832934',
            lat: 55.553368,
            lon: 37.88704,
            type: 'village',
            geoId: 'village-point-55.553368,37.88704'
        },
        snippets: [
            {
                id: '1832934',
                name: 'Орловъ',
                fullName: 'Коттеджный посёлок «Орловъ»',
                deliveryDates: [
                    {
                        phaseName: '1 очередь',
                        phaseIndex: 1,
                        status: 'HAND_OVER',
                        year: 2014,
                        quarter: 2,
                        finished: true
                    },
                    {
                        phaseName: '2 очередь',
                        phaseIndex: 2,
                        status: 'HAND_OVER',
                        year: 2018,
                        quarter: 3,
                        finished: true
                    },
                    {
                        phaseName: '3 очередь',
                        phaseIndex: 3,
                        status: 'HAND_OVER',
                        year: 2019,
                        quarter: 4,
                        finished: true
                    }
                ],
                location: {
                    geocoderAddress: 'Московская область, Ленинский район, деревня Орлово, жилой комплекс Орлов',
                    polygon: {
                        latitudes: [
                            55.548355,
                            55.547764,
                            55.549603,
                            55.552094,
                            55.553085,
                            55.55421,
                            55.554417,
                            55.555695,
                            55.55605,
                            55.556767,
                            55.557167,
                            55.55759,
                            55.55359,
                            55.550964
                        ],
                        longitudes: [
                            37.88548,
                            37.88448,
                            37.882183,
                            37.880684,
                            37.8847,
                            37.884254,
                            37.88581,
                            37.88658,
                            37.887764,
                            37.888084,
                            37.889416,
                            37.89375,
                            37.890423,
                            37.885853
                        ]
                    },
                    rgid: '324708',
                    geoId: 1,
                    point: {
                        latitude: 55.553368,
                        longitude: 37.88704
                    },
                    address: 'Ленинский район, д. Орлово, Володарское ш., 12 км.',
                    subjectFederationId: 1,
                    subjectFederationRgid: '741964',
                    routeDistances: [
                        {
                            geoPoint: {
                                latitude: 55.591656,
                                longitude: 37.729748,
                                defined: true
                            },
                            distance: 14317
                        }
                    ],
                    insideMKAD: false,
                    subjectFederationName: 'Москва и МО'
                },
                images: [],
                villageFeatures: {
                    villageClass: 'COMFORT',
                    totalObjects: 257,
                    soldObjects: 198,
                    infrastructure: [
                        {
                            type: 'GUEST_PARKING'
                        },
                        {
                            type: 'PLAYGROUND'
                        },
                        {
                            type: 'ROADS'
                        },
                        {
                            type: 'FOREST'
                        },
                        {
                            type: 'MARKET'
                        },
                        {
                            type: 'REST',
                            description: '1'
                        },
                        {
                            type: 'FENCE'
                        },
                        {
                            type: 'LIGHTING'
                        },
                        {
                            type: 'PARK',
                            description: '1'
                        },
                        {
                            type: 'MAINTENANCE'
                        }
                    ]
                },
                offerStats: {
                    entries: [
                        {
                            offerType: 'TOWNHOUSE',
                            price: {
                                currency: 'RUB',
                                from: '5797000',
                                to: '8330733'
                            },
                            landArea: {
                                unit: 'SOTKA',
                                from: 1.08,
                                to: 2.91
                            },
                            houseArea: {
                                unit: 'SQ_M',
                                from: 92.1,
                                to: 174.2
                            }
                        },
                        {
                            offerType: 'COTTAGE',
                            price: {
                                currency: 'RUB',
                                from: '11329552',
                                to: '28541130'
                            },
                            landArea: {
                                unit: 'SOTKA',
                                from: 6.5,
                                to: 16.48
                            },
                            houseArea: {
                                unit: 'SQ_M',
                                from: 156,
                                to: 290
                            }
                        }
                    ],
                    primaryPrices: {
                        currency: 'RUB',
                        from: '5797000',
                        to: '28541130'
                    }
                },
                filteredOfferStats: {
                    offerTypes: [
                        'TOWNHOUSE',
                        'COTTAGE'
                    ],
                    primaryPrice: {
                        currency: 'RUB',
                        from: '5797000',
                        to: '28541130'
                    }
                },
                salesDepartment: {
                    id: 1884787,
                    name: 'ГК «Астерра»',
                    weekTimetable: [
                        {
                            dayFrom: 1,
                            dayTo: 7,
                            timePattern: [
                                {
                                    open: '10:00',
                                    close: '19:00'
                                }
                            ]
                        }
                    ],
                    logo: '',
                    statParams: '==',
                    encryptedDump: ''
                },
                developers: [
                    {
                        id: '198558',
                        name: 'Астерра',
                        legalName: 'ООО «Пехра-Покровское»',
                        url: 'http://www.asterra.ru/',
                        logo: '',
                        objects: {
                            all: 12,
                            finished: 10,
                            unfinished: 2,
                            suspended: 0
                        },
                        address: 'Московская обл., Балашиха, мкр.1 мая, 25',
                        encryptedPhone: {
                            phoneWithMask: '+7 495 642 ×× ××',
                            phoneHash: 'KzcFgKHDQJ5NLSkNgNPjQRyLTTYVwLXTYZw'
                        }
                    }
                ],
                phone: {
                    phoneWithMask: '+7 495 642 ×× ××',
                    phoneHash: 'KzcFgKHDQJ5NLSkNgNPjQRyLTTYVwLXTYZw'
                },
                withBilling: true
            }
        ],
        points: {
            site: [],
            offer: [],
            village: [
                {
                    id: '1832934',
                    lat: 55.553368,
                    lon: 37.88704,
                    type: 'village'
                }
            ]
        }
    }
});
