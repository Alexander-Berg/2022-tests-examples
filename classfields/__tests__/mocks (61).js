export const getInitialState = () => ({
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

export const getInitialStateWithNotes = () => ({
    ...getInitialState(),
    offerNotes: {
        notes: {
            '5031402752833849089': {
                value: 'Note!',
                savedValue: 'Note!',
                isLoading: false,
                isFailed: false
            }
        }
    }
});

export const getOfferWithVas = () => ({
    appMiddleImages: [],
    appLargeImages: [],
    area: {
        value: 5555,
        unit: 'SQUARE_METER'
    },
    author: {
        id: '0',
        category: 'AGENCY',
        agentName: 'Колин Робинсон',
        creationDate: '2019-01-20T12:15:05Z',
        redirectPhones: true,
        redirectPhonesFailed: false,
        encryptedPhoneNumbers: [
            {
                phone: 'KzcF5OHDEJxNLzgN0NPTURw',
                redirectId: '4tjClezHZII'
            },
            {
                phone: 'KzcF0NHDQJ0NLDQN0NPDQR0',
                redirectId: '+74444444444'
            }
        ],
        encryptedPhones: [
            'KzcF5OHDEJxNLzgN0NPTURw',
            'KzcF0NHDQJ0NLDQN0NPDQR0'
        ]
    },
    building: {
        builtYear: 1936,
        buildingType: 'BRICK',
        improvements: {
            LIFT: true,
            RUBBISH_CHUTE: true,
            PASS_BY: true
        },
        parkingType: 'CLOSED',
        buildingId: '54030702295515876',
        heatingType: 'UNKNOWN',
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
            PASS_BY: true
        }
    },
    creationDate: '2019-01-23T09:32:59Z',
    floorsOffered: [
        22
    ],
    floorsTotal: 22,
    fullImages: [],
    house: {
        bathroomUnit: 'MATCHED',
        balconyType: 'BALCONY__LOGGIA',
        windowView: 'YARD_STREET',
        apartments: true,
        housePart: false
    },
    livingSpace: {
        value: 33,
        unit: 'SQUARE_METER'
    },
    location: {
        rgid: 12438,
        geoId: 213,
        subjectFederationId: 1,
        settlementRgid: 165705,
        settlementGeoId: 213,
        address: 'Москва, улица Большая Дмитровка, 1/30',
        geocoderAddress: 'Россия, Москва, улица Большая Дмитровка, 1/30',
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
                    value: 'улица Большая Дмитровка',
                    geoId: 213,
                    regionGraphId: '587795',
                    address: 'улица Большая Дмитровка',
                    regionType: 'STREET',
                    queryParams: {
                        rgid: '587795',
                        address: 'Россия, Москва, улица Большая Дмитровка'
                    }
                },
                {
                    value: '1/30',
                    geoId: 213,
                    regionGraphId: '587795',
                    address: '1/30',
                    regionType: 'HOUSE',
                    queryParams: {
                        rgid: '587795',
                        address: 'Россия, Москва, улица Большая Дмитровка, 1/30'
                    }
                }
            ]
        },
        point: {
            latitude: 55.758923,
            longitude: 37.61644,
            precision: 'EXACT'
        },
        metro: {
            metroGeoId: 20488,
            name: 'Охотный Ряд',
            metroTransport: 'ON_FOOT',
            timeToMetro: 5,
            latitude: 55.75697,
            longitude: 37.615524,
            minTimeToMetro: 5,
            lineColors: [
                'e4402d'
            ],
            rgbColor: 'e4402d'
        },
        streetAddress: 'улица Большая Дмитровка, 1/30',
        metroList: [
            {
                metroGeoId: 20488,
                name: 'Охотный Ряд',
                metroTransport: 'ON_FOOT',
                timeToMetro: 5,
                latitude: 55.75697,
                longitude: 37.615524,
                minTimeToMetro: 5,
                lineColors: [
                    'e4402d'
                ],
                rgbColor: 'e4402d'
            },
            {
                metroGeoId: 20473,
                name: 'Театральная',
                metroTransport: 'ON_FOOT',
                timeToMetro: 5,
                latitude: 55.75797,
                longitude: 37.619,
                minTimeToMetro: 5,
                lineColors: [
                    '4f8242'
                ],
                rgbColor: '4f8242'
            },
            {
                metroGeoId: 20480,
                name: 'Площадь Революции',
                metroTransport: 'ON_FOOT',
                timeToMetro: 9,
                latitude: 55.756805,
                longitude: 37.622726,
                minTimeToMetro: 9,
                lineColors: [
                    '0042a5'
                ],
                rgbColor: '0042a5'
            },
            {
                metroGeoId: 20502,
                name: 'Кузнецкий Мост',
                metroTransport: 'ON_FOOT',
                timeToMetro: 9,
                latitude: 55.76151,
                longitude: 37.624516,
                minTimeToMetro: 9,
                lineColors: [
                    'b1179a'
                ],
                rgbColor: 'b1179a'
            },
            {
                metroGeoId: 20487,
                name: 'Лубянка',
                metroTransport: 'ON_FOOT',
                timeToMetro: 11,
                latitude: 55.75946,
                longitude: 37.62679,
                minTimeToMetro: 8,
                lineColors: [
                    'e4402d'
                ],
                rgbColor: 'e4402d'
            },
            {
                metroGeoId: 20497,
                name: 'Александровский сад',
                metroTransport: 'ON_FOOT',
                timeToMetro: 12,
                latitude: 55.752354,
                longitude: 37.609467,
                minTimeToMetro: 7,
                lineColors: [
                    '099dd4'
                ],
                rgbColor: '099dd4'
            },
            {
                metroGeoId: 20489,
                name: 'Библиотека им. Ленина',
                metroTransport: 'ON_FOOT',
                timeToMetro: 13,
                latitude: 55.75132,
                longitude: 37.610115,
                minTimeToMetro: 6,
                lineColors: [
                    'e4402d'
                ],
                rgbColor: 'e4402d'
            },
            {
                metroGeoId: 20506,
                name: 'Чеховская',
                metroTransport: 'ON_FOOT',
                timeToMetro: 13,
                latitude: 55.765846,
                longitude: 37.60817,
                minTimeToMetro: 11,
                lineColors: [
                    '909090'
                ],
                rgbColor: '909090'
            },
            {
                metroGeoId: 20472,
                name: 'Тверская',
                metroTransport: 'ON_FOOT',
                timeToMetro: 14,
                latitude: 55.764458,
                longitude: 37.605946,
                minTimeToMetro: 9,
                lineColors: [
                    '4f8242'
                ],
                rgbColor: '4f8242'
            },
            {
                metroGeoId: 20507,
                name: 'Боровицкая',
                metroTransport: 'ON_FOOT',
                timeToMetro: 14,
                latitude: 55.75057,
                longitude: 37.60909,
                minTimeToMetro: 6,
                lineColors: [
                    '909090'
                ],
                rgbColor: '909090'
            }
        ],
        schools: [
            {
                schoolId: '1017372140',
                name: 'Школа № 1501',
                address: 'Россия, Москва, Долгоруковская улица, 6, стр. 2',
                ratingPlace: 12,
                timeOnFoot: 1714,
                distanceOnFoot: 2380,
                timeDistanceList: [
                    {
                        transport: 'ON_FOOT',
                        time: 28,
                        distance: 2380
                    }
                ],
                rating: {
                    place: 12,
                    total: 399,
                    high: true,
                    rgbColor: 'c847ff'
                }
            },
            {
                schoolId: '1123021006',
                name: 'Школа № 1574',
                address: 'Россия, Москва, Старопименовский переулок, 5',
                ratingPlace: 52,
                timeOnFoot: 1361,
                distanceOnFoot: 1891,
                timeDistanceList: [
                    {
                        transport: 'ON_FOOT',
                        time: 22,
                        distance: 1891
                    }
                ],
                rating: {
                    place: 52,
                    total: 399,
                    high: true,
                    rgbColor: 'c847ff'
                }
            },
            {
                schoolId: '1020105944',
                name: 'Школа № 1540',
                address: 'Россия, Москва, Новослободская улица, 38, стр. 1',
                ratingPlace: 256,
                timeOnFoot: 2523,
                distanceOnFoot: 3504,
                timeDistanceList: [
                    {
                        transport: 'ON_FOOT',
                        time: 42,
                        distance: 3504
                    }
                ],
                rating: {
                    place: 256,
                    total: 399,
                    high: false,
                    rgbColor: '36107c'
                }
            }
        ],
        airports: [
            {
                id: '878042',
                name: 'Шереметьево',
                timeOnCar: 2109,
                distanceOnCar: 32121,
                latitude: 55.963852,
                longitude: 37.4169,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 35,
                        distance: 32121
                    }
                ]
            },
            {
                id: '878065',
                name: 'Внуково',
                timeOnCar: 3369,
                distanceOnCar: 34104,
                latitude: 55.604942,
                longitude: 37.282578,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 56,
                        distance: 34104
                    }
                ]
            },
            {
                id: '858742',
                name: 'Домодедово',
                timeOnCar: 3465,
                distanceOnCar: 51200,
                latitude: 55.41435,
                longitude: 37.90048,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 57,
                        distance: 51200
                    }
                ]
            },
            {
                id: '878109',
                name: 'Жуковский (Раменское)',
                timeOnCar: 3665,
                distanceOnCar: 49241,
                latitude: 55.568665,
                longitude: 38.143654,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 61,
                        distance: 49241
                    }
                ]
            }
        ],
        heatmaps: [
            {
                name: 'infrastructure',
                rgbColor: '128000',
                description: 'отличная',
                level: 8,
                maxLevel: 8,
                title: 'Инфраструктура'
            },
            {
                name: 'transport',
                rgbColor: '30ce12',
                description: 'высокая доступность',
                level: 7,
                maxLevel: 9,
                title: 'Транспорт'
            }
        ],
        allHeatmaps: [
            {
                name: 'infrastructure',
                rgbColor: '128000',
                description: 'отличная',
                level: 8,
                maxLevel: 8,
                title: 'Инфраструктура'
            },
            {
                name: 'price-rent',
                rgbColor: 'ee4613',
                description: 'очень высокая',
                level: 2,
                maxLevel: 9,
                title: 'Цена аренды'
            },
            {
                name: 'price-sell',
                rgbColor: '620801',
                description: 'очень высокая',
                level: 1,
                maxLevel: 9,
                title: 'Цена продажи'
            },
            {
                name: 'profitability',
                rgbColor: 'ee4613',
                description: 'очень низкая',
                level: 2,
                maxLevel: 9,
                title: 'Прогноз окупаемости'
            },
            {
                name: 'transport',
                rgbColor: '30ce12',
                description: 'высокая доступность',
                level: 7,
                maxLevel: 9,
                title: 'Транспорт'
            }
        ],
        subjectFederationName: 'Москва и МО',
        cityCenter: [
            {
                transport: 'ON_FOOT',
                time: 469,
                distance: 652,
                latitude: 55.755253,
                longitude: 37.616386,
                timeDistanceList: [
                    {
                        transport: 'ON_FOOT',
                        time: 7,
                        distance: 652
                    }
                ]
            },
            {
                transport: 'ON_CAR',
                time: 539,
                distance: 2761,
                latitude: 55.755253,
                longitude: 37.616386,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 8,
                        distance: 2761
                    }
                ]
            }
        ]
    },
    minicardImages: [],
    newBuilding: false,
    obsolete: false,
    offerId: '5031402752833849089',
    offerCategory: 'ROOMS',
    offerType: 'SELL',
    openPlan: false,
    predictions: {
        predictedPrice: {
            min: '1675000',
            max: '2047000',
            value: '1861000'
        }
    },
    price: {
        currency: 'RUR',
        value: 444444442624,
        period: 'WHOLE_LIFE',
        unit: 'WHOLE_OFFER',
        trend: 'UNCHANGED',
        hasPriceHistory: false,
        valuePerPart: 13468013413,
        unitPerPart: 'SQUARE_METER',
        valueForWhole: 444444442624,
        unitForWhole: 'WHOLE_OFFER',
        price: {
            value: 444444442624,
            currency: 'RUB',
            priceType: 'PER_OFFER',
            pricingPeriod: 'WHOLE_LIFE'
        },
        pricePerPart: {
            value: 13468013413,
            currency: 'RUB',
            priceType: 'PER_METER',
            pricingPeriod: 'WHOLE_LIFE'
        },
        priceForWhole: {
            value: 444444442624,
            currency: 'RUB',
            priceType: 'PER_OFFER',
            pricingPeriod: 'WHOLE_LIFE'
        }
    },
    remoteReview: {
        onlineShow: false,
        youtubeVideoReviewUrl: 'https://www.youtube.com/watch?v=jMCv_m_l6fg&t'
    },
    roomsOffered: 2,
    roomsTotal: 5,
    totalImages: 0,
    transactionConditionsMap: {
        HAGGLE: true
    },
    tuzInfo: {
        campaignId: 'cd29e133-252e-48e7-9d21-283188ce8a65',
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
                value: 'ROOMS'
            },
            {
                key: 'tuzParamPartner',
                value: '1035218734'
            },
            {
                key: 'tuzParamUid',
                value: '4048396225'
            },
            {
                key: 'tuzParamClass',
                value: 'ELITE'
            }
        ],
        tuzFeatured: true,
        premium: true,
        promotion: true,
        raising: true,
        clientId: 1337971312,
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
});

export const getRentOfferWithVas = () => ({
    agentFee: 22,
    appMiddleImages: [],
    appLargeImages: [],
    area: {
        value: 3333,
        unit: 'SQUARE_METER'
    },
    author: {
        id: '0',
        category: 'AGENCY',
        agentName: 'ztuzmax1',
        creationDate: '2019-01-20T12:15:05Z',
        redirectPhones: true,
        redirectPhonesFailed: false,
        encryptedPhoneNumbers: [
            {
                phone: 'KzcF5MHTEJ5MLzIN3MPDQRz',
                redirectId: 'z0HH762VpYE'
            }
        ],
        encryptedPhones: [
            'KzcF5MHTEJ5MLzIN3MPDQRz'
        ]
    },
    building: {
        builtYear: 1944,
        buildingType: 'BRICK',
        improvements: {
            LIFT: true,
            RUBBISH_CHUTE: true,
            PASS_BY: true
        },
        parkingType: 'CLOSED',
        buildingId: '8315373809405496126',
        flatsCount: 182,
        porchesCount: 2,
        heatingType: 'UNKNOWN',
        priceStatistics: {
            sellPricePerSquareMeter: {
                level: 9,
                maxLevel: 9,
                rgbColor: '128000',
                value: '106296',
                regionValueFrom: '22727',
                regionValueTo: '2234148'
            },
            sellPriceByRooms: {
                2: {
                    level: 9,
                    maxLevel: 9,
                    rgbColor: '128000',
                    value: '5100000',
                    regionValueFrom: '700000',
                    regionValueTo: '350577600'
                }
            },
            profitability: {
                level: 7,
                maxLevel: 9,
                rgbColor: '30ce12',
                value: '16',
                regionValueFrom: '2',
                regionValueTo: '79'
            }
        },
        buildingImprovementsMap: {
            LIFT: true,
            RUBBISH_CHUTE: true,
            PASS_BY: true
        }
    },
    creationDate: '2019-01-23T09:35:04Z',
    floorsOffered: [
        3
    ],
    floorsTotal: 14,
    fullImages: [],
    house: {
        bathroomUnit: 'MATCHED',
        balconyType: 'LOGGIA',
        windowView: 'YARD',
        housePart: false
    },
    livingSpace: {
        value: 198,
        unit: 'SQUARE_METER'
    },
    location: {
        rgid: 10571,
        geoId: 21622,
        subjectFederationId: 1,
        settlementRgid: 596189,
        settlementGeoId: 10716,
        address: 'Московская область, Балашиха, микрорайон Железнодорожный, Пролетарская улица, 5',
        geocoderAddress: 'Россия, Московская область, Балашиха, микрорайон Железнодорожный, Пролетарская улица, 5',
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
                    value: 'Железнодорожный',
                    geoId: 21622,
                    regionGraphId: '10571',
                    address: 'микрорайон Железнодорожный',
                    regionType: 'NOT_ADMINISTRATIVE_DISTRICT',
                    queryParams: {
                        rgid: '10571',
                        // eslint-disable-next-line max-len
                        address: 'Россия, Московская область, Балашиха (городской округ), Балашиха, микрорайон Железнодорожный'
                    }
                },
                {
                    value: 'Пролетарская улица',
                    geoId: 21622,
                    regionGraphId: '10571',
                    address: 'Пролетарская улица',
                    regionType: 'STREET',
                    queryParams: {
                        rgid: '10571',
                        // eslint-disable-next-line max-len
                        address: 'Россия, Московская область, Балашиха (городской округ), Балашиха, микрорайон Железнодорожный, Пролетарская улица'
                    }
                },
                {
                    value: '5',
                    geoId: 21622,
                    regionGraphId: '10571',
                    address: '5',
                    regionType: 'HOUSE',
                    queryParams: {
                        rgid: '10571',
                        // eslint-disable-next-line max-len
                        address: 'Россия, Московская область, Балашиха (городской округ), Балашиха, микрорайон Железнодорожный, Пролетарская улица, 5'
                    }
                }
            ]
        },
        point: {
            latitude: 55.74801,
            longitude: 38.00967,
            precision: 'EXACT'
        },
        metro: {
            metroGeoId: 114781,
            name: 'Новокосино',
            metroTransport: 'ON_TRANSPORT',
            timeToMetro: 24,
            latitude: 55.745113,
            longitude: 37.864056,
            minTimeToMetro: 24,
            lineColors: [
                'ffe400'
            ],
            rgbColor: 'ffe400'
        },
        station: {
            name: 'Железнодорожная',
            distanceKm: 0.485
        },
        streetAddress: 'Пролетарская улица, 5',
        metroList: [
            {
                metroGeoId: 114781,
                name: 'Новокосино',
                metroTransport: 'ON_TRANSPORT',
                timeToMetro: 24,
                latitude: 55.745113,
                longitude: 37.864056,
                minTimeToMetro: 24,
                lineColors: [
                    'ffe400'
                ],
                rgbColor: 'ffe400'
            },
            {
                metroGeoId: 218432,
                name: 'Некрасовка',
                metroTransport: 'ON_TRANSPORT',
                timeToMetro: 35,
                latitude: 55.70297,
                longitude: 37.928036,
                minTimeToMetro: 35,
                lineColors: [
                    'ff66e8'
                ],
                rgbColor: 'ff66e8'
            },
            {
                metroGeoId: 218430,
                name: 'Улица Дмитриевского',
                metroTransport: 'ON_TRANSPORT',
                timeToMetro: 40,
                latitude: 55.71031,
                longitude: 37.87905,
                minTimeToMetro: 40,
                lineColors: [
                    'ff66e8'
                ],
                rgbColor: 'ff66e8'
            },
            {
                metroGeoId: 152937,
                name: 'Новохохловская',
                metroTransport: 'ON_TRANSPORT',
                timeToMetro: 47,
                latitude: 55.724255,
                longitude: 37.71714,
                minTimeToMetro: 47,
                lineColors: [
                    'ffa8af',
                    'df477c'
                ],
                rgbColor: 'ffa8af'
            }
        ],
        airports: [
            {
                id: '878109',
                name: 'Жуковский (Раменское)',
                timeOnCar: 3768,
                distanceOnCar: 30928,
                latitude: 55.568665,
                longitude: 38.143654,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 62,
                        distance: 30928
                    }
                ]
            },
            {
                id: '858742',
                name: 'Домодедово',
                timeOnCar: 3820,
                distanceOnCar: 58044,
                latitude: 55.41435,
                longitude: 37.90048,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 63,
                        distance: 58044
                    }
                ]
            },
            {
                id: '878042',
                name: 'Шереметьево',
                timeOnCar: 4161,
                distanceOnCar: 57097,
                latitude: 55.963852,
                longitude: 37.4169,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 69,
                        distance: 57097
                    }
                ]
            },
            {
                id: '878065',
                name: 'Внуково',
                timeOnCar: 6050,
                distanceOnCar: 69817,
                latitude: 55.604942,
                longitude: 37.282578,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 100,
                        distance: 69817
                    }
                ]
            }
        ],
        heatmaps: [
            {
                name: 'infrastructure',
                rgbColor: '20a70a',
                description: 'отличная',
                level: 7,
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
                rgbColor: '20a70a',
                description: 'отличная',
                level: 7,
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
                distance: 11142
            }
        ],
        subjectFederationName: 'Москва и МО',
        cityCenter: [
            {
                transport: 'ON_CAR',
                time: 2543,
                distance: 28960,
                latitude: 55.749893,
                longitude: 37.623425,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 42,
                        distance: 28960
                    }
                ]
            }
        ]
    },
    minicardImages: [],
    newBuilding: false,
    obsolete: false,
    offerId: '843081945331029249',
    offerCategory: 'ROOMS',
    offerType: 'RENT',
    openPlan: false,
    prepayment: 33,
    price: {
        currency: 'RUR',
        value: 444444442624,
        period: 'PER_DAY',
        unit: 'WHOLE_OFFER',
        trend: 'UNCHANGED',
        hasPriceHistory: false,
        valuePerPart: 2244668902,
        unitPerPart: 'SQUARE_METER',
        valueForWhole: 444444442624,
        unitForWhole: 'WHOLE_OFFER',
        price: {
            value: 444444442624,
            currency: 'RUB',
            priceType: 'PER_OFFER',
            pricingPeriod: 'PER_DAY'
        },
        pricePerPart: {
            value: 2244668902,
            currency: 'RUB',
            priceType: 'PER_METER',
            pricingPeriod: 'PER_DAY'
        },
        priceForWhole: {
            value: 444444442624,
            currency: 'RUB',
            priceType: 'PER_OFFER',
            pricingPeriod: 'PER_DAY'
        }
    },
    remoteReview: {
        onlineShow: true,
        youtubeVideoReviewUrl: 'https://www.youtube.com/watch?v=jMCv_m_l6fg&t'
    },
    roomsOffered: 6,
    roomsTotal: 7,
    totalImages: 0,
    transactionConditionsMap: {
        HAGGLE: true,
        RENT_PLEDGE: true
    },
    tuzInfo: {
        campaignId: 'cd29e133-252e-48e7-9d21-283188ce8a65',
        active: true,
        tuzParams: [
            {
                key: 'tuzParamRgid',
                value: '741964'
            },
            {
                key: 'tuzParamType',
                value: 'RENT'
            },
            {
                key: 'tuzParamCategory',
                value: 'ROOMS'
            },
            {
                key: 'tuzParamPartner',
                value: '1035218734'
            },
            {
                key: 'tuzParamUid',
                value: '4048396225'
            },
            {
                key: 'tuzParamRentTime',
                value: 'SHORT'
            }
        ],
        clientId: 1337971312,
        tuzType: {
            maximum: {}
        }
    },
    updateDate: '2019-01-23T06:16:00Z',
    utilitiesIncluded: true,
    vas: {
        raised: true,
        premium: true,
        placement: false,
        promoted: true,
        turboSale: false,
        raisingSale: false,
        tuzFeatured: false,
        vasAvailable: true
    },
    withExcerpt: false
});

export const getCommercialOffer = () => ({
    appMiddleImages: [],
    appLargeImages: [],
    area: {
        value: 444444,
        unit: 'SQUARE_METER'
    },
    author: {
        id: '0',
        category: 'AGENCY',
        agentName: 'ztuzmax1',
        creationDate: '2019-01-20T12:15:05Z',
        redirectPhones: true,
        redirectPhonesFailed: false,
        encryptedPhoneNumbers: [
            {
                phone: 'KzcF5OHDEJxNLzgN0NPTIRx',
                redirectId: 'OmbhSqfCEKc'
            }
        ],
        encryptedPhones: [
            'KzcF5OHDEJxNLzgN0NPTIRx'
        ]
    },
    building: {
        buildingState: 'HAND_OVER',
        siteId: 31783,
        siteName: 'Dominion',
        siteDisplayName: 'Квартал «Dominion»',
        officeClass: 'A',
        houseId: '31804',
        houseReadableName: 'Корпус 1',
        heatingType: 'UNKNOWN'
    },
    commercial: {
        commercialTypes: [
            'RETAIL'
        ],
        commercialBuildingType: 'BUSINESS_CENTER',
        purposes: [
            'BANK',
            'MEDICAL_CENTER',
            'FOOD_STORE',
            'SHOW_ROOM',
            'BEAUTY_SHOP',
            'TOURAGENCY'
        ]
    },
    creationDate: '2019-01-23T09:36:46Z',
    floorsOffered: [
        3
    ],
    floorsTotal: 16,
    fullImages: [],
    house: {
        apartments: false,
        housePart: false,
        entranceType: 'SEPARATE'
    },
    location: {
        rgid: 193370,
        geoId: 213,
        subjectFederationId: 1,
        settlementRgid: 165705,
        settlementGeoId: 213,
        address: 'Москва, Ломоносовский проспект, 25к1',
        geocoderAddress: 'Россия, Москва, Ломоносовский проспект, 25к1',
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
                    value: 'Ломоносовский проспект',
                    geoId: 213,
                    regionGraphId: '587795',
                    address: 'Ломоносовский проспект',
                    regionType: 'STREET',
                    queryParams: {
                        rgid: '587795',
                        address: 'Россия, Москва, Ломоносовский проспект'
                    }
                },
                {
                    value: '25к1',
                    geoId: 213,
                    regionGraphId: '587795',
                    address: '25к1',
                    regionType: 'HOUSE',
                    queryParams: {
                        rgid: '587795',
                        address: 'Россия, Москва, Ломоносовский проспект, 25к1'
                    }
                }
            ]
        },
        point: {
            latitude: 55.693592,
            longitude: 37.53244,
            precision: 'EXACT'
        },
        metro: {
            metroGeoId: 20444,
            name: 'Университет',
            metroTransport: 'ON_FOOT',
            timeToMetro: 3,
            latitude: 55.69261,
            longitude: 37.533524,
            minTimeToMetro: 3,
            lineColors: [
                'e4402d'
            ],
            rgbColor: 'e4402d'
        },
        streetAddress: 'Ломоносовский проспект, 25к1',
        metroList: [
            {
                metroGeoId: 20444,
                name: 'Университет',
                metroTransport: 'ON_FOOT',
                timeToMetro: 3,
                latitude: 55.69261,
                longitude: 37.533524,
                minTimeToMetro: 3,
                lineColors: [
                    'e4402d'
                ],
                rgbColor: 'e4402d'
            },
            {
                metroGeoId: 20451,
                name: 'Проспект Вернадского',
                metroTransport: 'ON_TRANSPORT',
                timeToMetro: 9,
                latitude: 55.676914,
                longitude: 37.505836,
                minTimeToMetro: 9,
                lineColors: [
                    'e4402d'
                ],
                rgbColor: 'e4402d'
            },
            {
                metroGeoId: 163520,
                name: 'Ломоносовский проспект',
                metroTransport: 'ON_TRANSPORT',
                timeToMetro: 14,
                latitude: 55.70706,
                longitude: 37.516068,
                minTimeToMetro: 14,
                lineColors: [
                    'ffe400'
                ],
                rgbColor: 'ffe400'
            },
            {
                metroGeoId: 163522,
                name: 'Раменки',
                metroTransport: 'ON_TRANSPORT',
                timeToMetro: 20,
                latitude: 55.697693,
                longitude: 37.49873,
                minTimeToMetro: 20,
                lineColors: [
                    'ffe400'
                ],
                rgbColor: 'ffe400'
            }
        ],
        expectedMetroList: [
            {
                metroId: '1561877',
                year: 2021,
                name: 'Улица Строителей',
                lineName: 'Коммунарская',
                lineId: '44',
                metroTransport: 'ON_FOOT',
                timeToMetro: 15,
                latitude: 55.687214,
                longitude: 37.534756,
                rgbColor: 'a2a5b4',
                // eslint-disable-next-line max-len
                description: 'Станция «Улица Строителей» расположится вдоль Ленинского проспекта на пересечении с Ломоносовским проспектом.',
                timeUnit: 'minutes'
            }
        ],
        airports: [
            {
                id: '878065',
                name: 'Внуково',
                timeOnCar: 2336,
                distanceOnCar: 25123,
                latitude: 55.604942,
                longitude: 37.282578,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 38,
                        distance: 25123
                    }
                ]
            },
            {
                id: '878042',
                name: 'Шереметьево',
                timeOnCar: 2392,
                distanceOnCar: 39616,
                latitude: 55.963852,
                longitude: 37.4169,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 39,
                        distance: 39616
                    }
                ]
            },
            {
                id: '858742',
                name: 'Домодедово',
                timeOnCar: 2862,
                distanceOnCar: 41332,
                latitude: 55.41435,
                longitude: 37.90048,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 47,
                        distance: 41332
                    }
                ]
            },
            {
                id: '878109',
                name: 'Жуковский (Раменское)',
                timeOnCar: 3253,
                distanceOnCar: 49263,
                latitude: 55.568665,
                longitude: 38.143654,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 54,
                        distance: 49263
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
                rgbColor: '9ada1b',
                description: 'высокая доступность',
                level: 6,
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
                rgbColor: 'ee4613',
                description: 'очень высокая',
                level: 2,
                maxLevel: 9,
                title: 'Цена аренды'
            },
            {
                name: 'price-sell',
                rgbColor: '620801',
                description: 'очень высокая',
                level: 1,
                maxLevel: 9,
                title: 'Цена продажи'
            },
            {
                name: 'profitability',
                rgbColor: 'ee4613',
                description: 'очень низкая',
                level: 2,
                maxLevel: 9,
                title: 'Прогноз окупаемости'
            },
            {
                name: 'transport',
                rgbColor: '9ada1b',
                description: 'высокая доступность',
                level: 6,
                maxLevel: 9,
                title: 'Транспорт'
            }
        ],
        subjectFederationName: 'Москва и МО',
        cityCenter: [
            {
                transport: 'ON_CAR',
                time: 917,
                distance: 9866,
                latitude: 55.749058,
                longitude: 37.612267,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 15,
                        distance: 9866
                    }
                ]
            }
        ]
    },
    minicardImages: [],
    newBuilding: false,
    obsolete: false,
    offerId: '8268535154074659841',
    offerCategory: 'COMMERCIAL',
    offerType: 'SELL',
    openPlan: false,
    price: {
        currency: 'RUR',
        value: 33333332,
        period: 'WHOLE_LIFE',
        unit: 'WHOLE_OFFER',
        trend: 'UNCHANGED',
        hasPriceHistory: false,
        valuePerPart: 75,
        unitPerPart: 'SQUARE_METER',
        valueForWhole: 33333332,
        unitForWhole: 'WHOLE_OFFER',
        price: {
            value: 33333332,
            currency: 'RUB',
            priceType: 'PER_OFFER',
            pricingPeriod: 'WHOLE_LIFE'
        },
        pricePerPart: {
            value: 75,
            currency: 'RUB',
            priceType: 'PER_METER',
            pricingPeriod: 'WHOLE_LIFE'
        },
        priceForWhole: {
            value: 33333332,
            currency: 'RUB',
            priceType: 'PER_OFFER',
            pricingPeriod: 'WHOLE_LIFE'
        }
    },
    remoteReview: {
        onlineShow: false,
        youtubeVideoReviewUrl: ''
    },
    roomsTotal: 33,
    totalImages: 0,
    tuzInfo: {
        campaignId: 'cd29e133-252e-48e7-9d21-283188ce8a65',
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
                value: 'COMMERCIAL'
            },
            {
                key: 'tuzParamPartner',
                value: '1035218734'
            },
            {
                key: 'tuzParamUid',
                value: '4048396225'
            },
            {
                key: 'tuzParamClass',
                value: 'COMFORT'
            }
        ],
        clientId: 1337971312,
        tuzType: {
            maximum: {}
        }
    },
    vas: {
        raised: false,
        premium: false,
        placement: false,
        promoted: false,
        turboSale: false,
        raisingSale: false,
        vasUnavailableReasons: [
            'NO_PHOTO'
        ],
        tuzFeatured: false,
        vasAvailable: false
    },
    withExcerpt: false
});

export const getOfferWithBadges = (freeReportAccessibility = 'FRA_READY') => ({
    ...getOfferWithVas(),
    remoteReview: {
        onlineShow: true
    },
    freeReportAccessibility
});

export const getSiteOffer = () => ({
    appMiddleImages: [],
    appLargeImages: [],
    area: {
        value: 55.4,
        unit: 'SQUARE_METER'
    },
    author: {
        id: '0',
        category: 'AGENCY',
        creationDate: '2017-01-20T11:12:45Z',
        redirectPhones: true,
        redirectPhonesFailed: false,
        encryptedPhoneNumbers: [
            {
                phone: 'KzcF0OHTUJxMLjUNxNPzkR1',
                redirectId: '+74951251795'
            }
        ],
        encryptedPhones: [
            'KzcF0OHTUJxMLjUNxNPzkR1'
        ]
    },
    building: {
        builtYear: 2018,
        builtQuarter: 1,
        buildingState: 'HAND_OVER',
        improvements: {
            LIFT: false,
            RUBBISH_CHUTE: false
        },
        siteId: 69533,
        siteName: 'Лица',
        siteDisplayName: 'ЖК «Лица»',
        houseId: '69552',
        heatingType: 'UNKNOWN',
        buildingImprovementsMap: {
            LIFT: false,
            RUBBISH_CHUTE: false
        }
    },
    creationDate: '2018-10-19T15:16:52Z',
    flatType: 'NEW_SECONDARY',
    floorsOffered: [
        22
    ],
    floorsTotal: 24,
    fullImages: [],
    house: {
        studio: true,
        apartments: false,
        housePart: false
    },
    location: {
        rgid: 281229,
        geoId: 213,
        subjectFederationId: 1,
        settlementRgid: 165705,
        settlementGeoId: 213,
        address: 'Москва, улица Авиаконструктора Сухого, 2к1',
        geocoderAddress: 'Россия, Москва, улица Авиаконструктора Сухого, 2к1',
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
                    value: 'улица Авиаконструктора Сухого',
                    geoId: 213,
                    regionGraphId: '587795',
                    address: 'улица Авиаконструктора Сухого',
                    regionType: 'STREET',
                    queryParams: {
                        rgid: '587795',
                        address: 'Россия, Москва, улица Авиаконструктора Сухого'
                    }
                },
                {
                    value: '2к1',
                    geoId: 213,
                    regionGraphId: '587795',
                    address: '2к1',
                    regionType: 'HOUSE',
                    queryParams: {
                        rgid: '587795',
                        address: 'Россия, Москва, улица Авиаконструктора Сухого, 2к1'
                    }
                }
            ]
        },
        point: {
            latitude: 55.786083,
            longitude: 37.541992,
            precision: 'EXACT'
        },
        metro: {
            metroGeoId: 189492,
            name: 'ЦСКА',
            metroTransport: 'ON_FOOT',
            timeToMetro: 10,
            latitude: 55.786606,
            longitude: 37.533306,
            minTimeToMetro: 10,
            lineColors: [
                'ffe400',
                '6fc1ba'
            ],
            rgbColor: 'ffe400'
        },
        streetAddress: 'улица Авиаконструктора Сухого, 2к1',
        metroList: [
            {
                metroGeoId: 189492,
                name: 'ЦСКА',
                metroTransport: 'ON_FOOT',
                timeToMetro: 10,
                latitude: 55.786606,
                longitude: 37.533306,
                minTimeToMetro: 10,
                lineColors: [
                    'ffe400',
                    '6fc1ba'
                ],
                rgbColor: 'ffe400'
            },
            {
                metroGeoId: 152918,
                name: 'Хорошёво',
                metroTransport: 'ON_FOOT',
                timeToMetro: 14,
                latitude: 55.782288,
                longitude: 37.52821,
                minTimeToMetro: 12,
                lineColors: [
                    'ffa8af'
                ],
                rgbColor: 'ffa8af'
            },
            {
                metroGeoId: 189451,
                name: 'Петровский парк',
                metroTransport: 'ON_FOOT',
                timeToMetro: 17,
                latitude: 55.791847,
                longitude: 37.557194,
                minTimeToMetro: 17,
                lineColors: [
                    'ffe400',
                    '6fc1ba'
                ],
                rgbColor: 'ffe400'
            },
            {
                metroGeoId: 20558,
                name: 'Динамо',
                metroTransport: 'ON_FOOT',
                timeToMetro: 17,
                latitude: 55.789707,
                longitude: 37.558212,
                minTimeToMetro: 12,
                lineColors: [
                    '4f8242'
                ],
                rgbColor: '4f8242'
            },
            {
                metroGeoId: 20375,
                name: 'Беговая',
                metroTransport: 'ON_TRANSPORT',
                timeToMetro: 16,
                latitude: 55.77365,
                longitude: 37.546295,
                minTimeToMetro: 16,
                lineColors: [
                    'ed9f2d',
                    'b1179a'
                ],
                rgbColor: 'ed9f2d'
            },
            {
                metroGeoId: 20374,
                name: 'Полежаевская',
                metroTransport: 'ON_TRANSPORT',
                timeToMetro: 18,
                latitude: 55.777447,
                longitude: 37.519318,
                minTimeToMetro: 18,
                lineColors: [
                    'b1179a'
                ],
                rgbColor: 'b1179a'
            },
            {
                metroGeoId: 189452,
                name: 'Хорошёвская',
                metroTransport: 'ON_TRANSPORT',
                timeToMetro: 18,
                latitude: 55.776863,
                longitude: 37.520798,
                minTimeToMetro: 18,
                lineColors: [
                    'ffe400',
                    '6fc1ba'
                ],
                rgbColor: 'ffe400'
            }
        ],
        airports: [
            {
                id: '878042',
                name: 'Шереметьево',
                timeOnCar: 1629,
                distanceOnCar: 26638,
                latitude: 55.963852,
                longitude: 37.4169,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 27,
                        distance: 26638
                    }
                ]
            },
            {
                id: '878065',
                name: 'Внуково',
                timeOnCar: 2814,
                distanceOnCar: 35160,
                latitude: 55.604942,
                longitude: 37.282578,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 46,
                        distance: 35160
                    }
                ]
            },
            {
                id: '858742',
                name: 'Домодедово',
                timeOnCar: 3293,
                distanceOnCar: 53858,
                latitude: 55.41435,
                longitude: 37.90048,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 54,
                        distance: 53858
                    }
                ]
            },
            {
                id: '878109',
                name: 'Жуковский (Раменское)',
                timeOnCar: 3467,
                distanceOnCar: 54441,
                latitude: 55.568665,
                longitude: 38.143654,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 57,
                        distance: 54441
                    }
                ]
            }
        ],
        heatmaps: [
            {
                name: 'infrastructure',
                rgbColor: 'f87c19',
                description: 'минимальная',
                level: 2,
                maxLevel: 8,
                title: 'Инфраструктура'
            },
            {
                name: 'transport',
                rgbColor: '9ada1b',
                description: 'высокая доступность',
                level: 6,
                maxLevel: 9,
                title: 'Транспорт'
            }
        ],
        allHeatmaps: [
            {
                name: 'infrastructure',
                rgbColor: 'f87c19',
                description: 'минимальная',
                level: 2,
                maxLevel: 8,
                title: 'Инфраструктура'
            },
            {
                name: 'price-rent',
                rgbColor: '620801',
                description: 'очень высокая',
                level: 1,
                maxLevel: 9,
                title: 'Цена аренды'
            },
            {
                name: 'price-sell',
                rgbColor: 'f87c19',
                description: 'высокая',
                level: 3,
                maxLevel: 9,
                title: 'Цена продажи'
            },
            {
                name: 'profitability',
                rgbColor: '20a70a',
                description: 'очень высокая',
                level: 8,
                maxLevel: 9,
                title: 'Прогноз окупаемости'
            },
            {
                name: 'transport',
                rgbColor: '9ada1b',
                description: 'высокая доступность',
                level: 6,
                maxLevel: 9,
                title: 'Транспорт'
            }
        ],
        subjectFederationName: 'Москва и МО',
        cityCenter: [
            {
                transport: 'ON_CAR',
                time: 934,
                distance: 10072,
                latitude: 55.749058,
                longitude: 37.612267,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 15,
                        distance: 10072
                    }
                ]
            }
        ]
    },
    minicardImages: [],
    newBuilding: false,
    obsolete: false,
    offerId: '5153057909170727562',
    offerCategory: 'APARTMENT',
    offerType: 'SELL',
    openPlan: false,
    predictions: {
        predictedPrice: {
            min: '6101000',
            max: '7457000',
            value: '6779000'
        }
    },
    price: {
        currency: 'RUR',
        value: 19800000,
        period: 'WHOLE_LIFE',
        unit: 'WHOLE_OFFER',
        trend: 'INCREASED',
        previous: 18592794,
        hasPriceHistory: true,
        valuePerPart: 357401,
        unitPerPart: 'SQUARE_METER',
        valueForWhole: 19800000,
        unitForWhole: 'WHOLE_OFFER',
        price: {
            value: 19800000,
            currency: 'RUB',
            priceType: 'PER_OFFER',
            pricingPeriod: 'WHOLE_LIFE'
        },
        pricePerPart: {
            value: 357401,
            currency: 'RUB',
            priceType: 'PER_METER',
            pricingPeriod: 'WHOLE_LIFE'
        },
        priceForWhole: {
            value: 19800000,
            currency: 'RUB',
            priceType: 'PER_OFFER',
            pricingPeriod: 'WHOLE_LIFE'
        }
    },
    primarySaleV2: true,
    remoteReview: {
        onlineShow: false,
        youtubeVideoReviewUrl: ''
    },
    salesDepartments: [
        {
            id: 519200,
            name: 'Компания Бульварное кольцо',
            weekTimetable: [
                {
                    dayFrom: 1,
                    dayTo: 5,
                    timePattern: [
                        {
                            open: '11:00',
                            close: '19:30'
                        }
                    ]
                },
                {
                    dayFrom: 6,
                    dayTo: 7,
                    timePattern: [
                        {
                            open: '10:00',
                            close: '17:00'
                        }
                    ]
                }
            ],
            logo: '',
            encryptedPhones: [
                'KzcF0OHTUJxMLjUNxNPzkR1'
            ],
            encryptedDump: '=='
        }
    ],
    totalImages: 0,
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
});
