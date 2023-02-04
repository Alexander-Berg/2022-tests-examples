export const getInitialState = () => ({
    user: {
        favorites: [],
        favoritesMap: {}
    }
});

export const getSiteSerpSnippet = () => ({
    id: 375274,
    name: 'Саларьево парк',
    fullName: 'жилой район «Саларьево парк»',
    locativeFullName: 'в жилом районе «Саларьево парк»',
    deliveryDates: [
        {
            finished: true,
            quarter: 4,
            year: 2018,
            phaseName: '1 очередь',
            houses: 7,
            housesInfo: [
                {
                    id: '375472',
                    name: '1',
                    maxFloor: 25
                },
                {
                    id: '408841',
                    name: '1',
                    maxFloor: 25
                },
                {
                    id: '408836',
                    name: '2',
                    maxFloor: 25
                },
                {
                    id: '408843',
                    name: '2',
                    maxFloor: 25
                },
                {
                    id: '408839',
                    name: '3',
                    maxFloor: 25
                },
                {
                    id: '509384',
                    name: '4',
                    maxFloor: 25
                },
                {
                    id: '509376',
                    name: '5',
                    maxFloor: 25
                }
            ],
            buildingState: 'HAND_OVER'
        },
        {
            finished: true,
            quarter: 1,
            year: 2019,
            phaseName: '2 очередь',
            houses: 2,
            housesInfo: [
                {
                    id: '548471',
                    name: '3',
                    maxFloor: 25
                },
                {
                    id: '548476',
                    name: '3',
                    maxFloor: 25
                }
            ],
            buildingState: 'HAND_OVER'
        },
        {
            finished: true,
            quarter: 4,
            year: 2019,
            phaseName: '3 очередь',
            houses: 3,
            housesInfo: [
                {
                    id: '710827',
                    name: '2',
                    maxFloor: 25
                },
                {
                    id: '710832',
                    name: '2',
                    maxFloor: 25
                },
                {
                    id: '710839',
                    name: '2',
                    maxFloor: 25
                }
            ],
            buildingState: 'HAND_OVER'
        },
        {
            finished: true,
            quarter: 1,
            year: 2020,
            phaseName: '4 очередь',
            houses: 3,
            housesInfo: [
                {
                    id: '961568',
                    name: '1',
                    maxFloor: 24
                },
                {
                    id: '961559',
                    name: '2',
                    maxFloor: 24
                },
                {
                    id: '871122',
                    name: '3',
                    maxFloor: 25
                }
            ],
            buildingState: 'HAND_OVER'
        },
        {
            finished: false,
            quarter: 3,
            year: 2020,
            phaseName: '5 очередь',
            houses: 4,
            housesInfo: [
                {
                    id: '1658373',
                    name: '19',
                    maxFloor: 24
                },
                {
                    id: '1549314',
                    name: '18/1',
                    maxFloor: 25
                },
                {
                    id: '1658411',
                    name: '18/2',
                    maxFloor: 25
                },
                {
                    id: '871148',
                    maxFloor: 25
                }
            ],
            buildingState: 'UNFINISHED'
        },
        {
            finished: false,
            quarter: 4,
            year: 2020,
            phaseName: '6 очередь',
            houses: 3,
            housesInfo: [
                {
                    id: '1658419',
                    name: '21',
                    maxFloor: 25
                },
                {
                    id: '1658438',
                    maxFloor: 24
                },
                {
                    id: '1658454',
                    maxFloor: 24
                }
            ],
            buildingState: 'UNFINISHED'
        },
        {
            finished: false,
            quarter: 1,
            year: 2021,
            phaseName: '7 очередь',
            houses: 2,
            housesInfo: [
                {
                    id: '1658462',
                    maxFloor: 16
                },
                {
                    id: '1658465',
                    maxFloor: 16
                }
            ],
            buildingState: 'UNFINISHED'
        },
        {
            finished: false,
            quarter: 2,
            year: 2021,
            phaseName: '8 очередь',
            houses: 3,
            housesInfo: [
                {
                    id: '1937795',
                    maxFloor: 15
                },
                {
                    id: '1937865',
                    maxFloor: 15
                },
                {
                    id: '1940088',
                    maxFloor: 15
                }
            ],
            buildingState: 'UNFINISHED'
        },
        {
            finished: false,
            quarter: 3,
            year: 2021,
            phaseName: '9 очередь',
            houses: 4,
            housesInfo: [
                {
                    id: '1696962',
                    maxFloor: 15
                },
                {
                    id: '1696969',
                    maxFloor: 16
                },
                {
                    id: '1696973',
                    maxFloor: 16
                },
                {
                    id: '1696977',
                    maxFloor: 16
                }
            ],
            buildingState: 'UNFINISHED'
        },
        {
            finished: false,
            quarter: 4,
            year: 2021,
            phaseName: '10 очередь',
            houses: 3,
            housesInfo: [
                {
                    id: '1940092',
                    maxFloor: 15
                },
                {
                    id: '1940113',
                    maxFloor: 16
                },
                {
                    id: '1940131',
                    maxFloor: 16
                }
            ],
            buildingState: 'UNFINISHED'
        },
        {
            finished: false,
            quarter: 1,
            year: 2022,
            phaseName: '11 очередь',
            houses: 3,
            housesInfo: [
                {
                    id: '1696993',
                    maxFloor: 16
                },
                {
                    id: '1696998',
                    maxFloor: 16
                },
                {
                    id: '1940097',
                    maxFloor: 15
                }
            ],
            buildingState: 'UNFINISHED'
        },
        {
            finished: false,
            quarter: 3,
            year: 2022,
            phaseName: '12 очередь',
            houses: 2,
            housesInfo: [
                {
                    id: '1940136',
                    maxFloor: 15
                },
                {
                    id: '1940141',
                    maxFloor: 22
                }
            ],
            buildingState: 'UNFINISHED'
        },
        {
            finished: false,
            quarter: 4,
            year: 2022,
            phaseName: '13 очередь',
            houses: 2,
            housesInfo: [
                {
                    id: '1940158',
                    maxFloor: 15
                },
                {
                    id: '1940163',
                    maxFloor: 22
                }
            ],
            buildingState: 'UNFINISHED'
        }
    ],
    buildingFeatures: {
        state: 'UNFINISHED',
        finishedApartments: true,
        class: 'COMFORT',
        zhkType: 'ZHILOY_RAYON',
        totalFloors: 25,
        minTotalFloors: 15,
        totalApartments: 13822,
        isApartment: false,
        ceilingHeight: 260,
        interiorFinish: {
            type: 'CLEAN',
            // eslint-disable-next-line max-len
            text: 'Все подготовительные работы по ремонту уже выполнены застройщиком: стены и пол выровнены и подготовлены к отделке; подведены холодная и горячая вода, выполнена полная разводка электричества. Остается поклеить обои или покрасить стены, положить напольное покрытие и плитку в санузлах, установить межкомнатные двери и сантехнику.',
            images: []
        },
        parking: {
            type: 'INDOOR',
            available: true
        },
        parkings: [
            {
                type: 'OPEN',
                available: true
            },
            {
                type: 'CLOSED',
                available: true
            },
            {
                type: 'UNDERGROUND',
                available: true
            }
        ],
        walls: {
            type: 'MONOLIT',
            // eslint-disable-next-line max-len
            text: 'Долговечный материал, позволяет приступить к отделочным работам почти сразу же после завершения строительства.'
        },
        decorationInfo: [
            {
                type: 'CLEAN',
                // eslint-disable-next-line max-len
                description: 'Все подготовительные работы по ремонту уже выполнены застройщиком: стены и пол выровнены и подготовлены к отделке; подведены холодная и горячая вода, выполнена полная разводка электричества. Остается поклеить обои или покрасить стены, положить напольное покрытие и плитку в санузлах, установить межкомнатные двери и сантехнику.'
            }
        ],
        decorationImages: [],
        wallTypes: [
            {
                type: 'MONOLIT',
                // eslint-disable-next-line max-len
                text: 'Долговечный материал, позволяет приступить к отделочным работам почти сразу же после завершения строительства.'
            },
            {
                type: 'PANEL',
                // eslint-disable-next-line max-len
                text: 'Обеспечивает высокую скорость строительства. Собирается из готовых блоков, что позволяет лучше контролировать качество.'
            }
        ]
    },
    flatStatus: 'ON_SALE',
    constructionState: 'UNDER_CONSTRUCTIONS_WITH_HAND_OVER',
    siteSpecialProposals: [
        {
            description: 'Ипотека 6.5%',
            specialProposalType: 'sale',
            mainProposal: true
        }
    ],
    images: [],
    location: {
        geoId: 114619,
        rgid: 17385368,
        settlementRgid: 94,
        settlementGeoId: 213,
        address: 'Москва, ш. Киевское, пос. Московский',
        distanceFromRingRoad: 4170,
        subjectFederationId: 1,
        subjectFederationRgid: 741964,
        subjectFederationName: 'Москва и МО',
        point: {
            latitude: 55.61857,
            longitude: 37.41301,
            precision: 'EXACT'
        },
        expectedMetroList: [],
        schools: [],
        parks: [
            {
                parkId: '121406591',
                name: 'Ульяновский лесопарк',
                timeOnFoot: 89,
                distanceOnFoot: 124,
                latitude: 55.61996,
                longitude: 37.41289,
                timeDistanceList: [
                    {
                        transport: 'ON_FOOT',
                        time: 1,
                        distance: 124
                    }
                ]
            }
        ],
        ponds: [
            {
                pondId: '137693384',
                name: 'река Сетунь',
                timeOnFoot: 454,
                distanceOnFoot: 630,
                latitude: 55.615074,
                longitude: 37.412415,
                timeDistanceList: [
                    {
                        transport: 'ON_FOOT',
                        time: 7,
                        distance: 630
                    }
                ]
            }
        ],
        cityCenter: [
            {
                transport: 'ON_CAR',
                time: 1710,
                distance: 20395,
                latitude: 55.749058,
                longitude: 37.612267,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 28,
                        distance: 20395
                    }
                ]
            }
        ],
        insideMKAD: false,
        routeDistances: [
            {
                geoPoint: {
                    latitude: 55.6387,
                    longitude: 37.459286,
                    defined: true
                },
                distance: 4170
            }
        ],
        metro: {
            lineColors: [
                'e4402d'
            ],
            metroGeoId: 144826,
            rgbColor: 'e4402d',
            metroTransport: 'ON_FOOT',
            name: 'Саларьево',
            timeToMetro: 9
        },
        metroList: [
            {
                lineColors: [
                    'e4402d'
                ],
                metroGeoId: 144826,
                rgbColor: 'e4402d',
                metroTransport: 'ON_FOOT',
                name: 'Саларьево',
                timeToMetro: 9
            },
            {
                lineColors: [
                    'e4402d'
                ],
                metroGeoId: 218467,
                rgbColor: 'e4402d',
                metroTransport: 'ON_TRANSPORT',
                name: 'Филатов Луг',
                timeToMetro: 12
            },
            {
                lineColors: [
                    'e4402d'
                ],
                metroGeoId: 144612,
                rgbColor: 'e4402d',
                metroTransport: 'ON_TRANSPORT',
                name: 'Румянцево',
                timeToMetro: 16
            },
            {
                lineColors: [
                    'e4402d'
                ],
                metroGeoId: 218466,
                rgbColor: 'e4402d',
                metroTransport: 'ON_TRANSPORT',
                name: 'Прокшино',
                timeToMetro: 18
            }
        ]
    },
    // eslint-disable-next-line max-len
    description: 'Жилой район «Саларьево парк» расположен на Юго-Западе Москвы рядом с метро «Саларьево». Масштабная концепция развития района предусматривает  возведение современных жилых домов и всей необходимой инфраструктуры в несколько очередей.',
    developers: [
        {
            id: 52308,
            name: 'Группа Компаний ПИК',
            legalName: 'ООО «Тирон»',
            url: 'http://www.pik.ru',
            logo: '//avatars.mdst.yandex.net/get-realty/2935/company.918322.1165353079517089892/builder_logo_info',
            objects: {
                all: 117,
                salesOpened: 67,
                finished: 62,
                unfinished: 55,
                suspended: 0
            },
            address: 'Москва, Баррикадная ул., 19, стр.1',
            born: '1993-12-31T21:00:00Z',
            encryptedPhones: [
                {
                    phoneWithMask: '+7 495 120 ×× ××',
                    phoneHash: 'KzcF0OHTUJxMLjANxMPzUR4',
                    tag: 0
                }
            ]
        }
    ]
});

export const siteSerpSpbSnippet = {
    id: 15510,
    name: 'INKERI',
    fullName: 'ЖК «INKERI»',
    locativeFullName: 'в ЖК «INKERI»',
    location: {
        geoId: 10884,
        rgid: 286693,
        settlementRgid: 417908,
        settlementGeoId: 10884,
        address: 'Пушкин, ул. Камероновская',
        distanceFromRingRoad: 15924,
        subjectFederationId: 10174,
        subjectFederationRgid: 741965,
        subjectFederationName: 'Санкт-Петербург и ЛО',
        point: {
            latitude: 59.698246,
            longitude: 30.406132,
            precision: 'EXACT'
        },
        expectedMetroList: [],
        schools: [],
        parks: [],
        ponds: [
            {
                pondId: '1652193431',
                name: 'Гуммолосарский ручей',
                timeOnFoot: 209,
                distanceOnFoot: 290,
                latitude: 59.700203,
                longitude: 30.407053,
                timeDistanceList: [
                    {
                        transport: 'ON_FOOT',
                        time: 3,
                        distance: 290
                    }
                ]
            }
        ],
        airports: [
            {
                id: '858726',
                name: 'Пулково',
                timeOnCar: 2139,
                distanceOnCar: 21009,
                latitude: 59.79992,
                longitude: 30.271744,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 35,
                        distance: 21009
                    }
                ]
            }
        ],
        cityCenter: [
            {
                transport: 'ON_CAR',
                time: 2762,
                distance: 34830,
                latitude: 59.933926,
                longitude: 30.307991,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 46,
                        distance: 34830
                    }
                ]
            }
        ],
        heatmaps: [
            {
                name: 'infrastructure',
                rgbColor: 'ee4613',
                description: 'минимальная',
                level: 1,
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
                rgbColor: 'ee4613',
                description: 'минимальная',
                level: 1,
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
                rgbColor: '30ce12',
                description: 'низкая',
                level: 7,
                maxLevel: 9,
                title: 'Цена продажи'
            },
            {
                name: 'profitability',
                rgbColor: '30ce12',
                description: 'высокая',
                level: 7,
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
        metro: {
            lineColors: [
                '16bdf0'
            ],
            metroGeoId: 20305,
            rgbColor: '16bdf0',
            metroTransport: 'ON_TRANSPORT',
            name: 'Купчино',
            timeToMetro: 40
        },
        metroList: [
            {
                lineColors: [
                    '16bdf0'
                ],
                metroGeoId: 20305,
                rgbColor: '16bdf0',
                metroTransport: 'ON_TRANSPORT',
                name: 'Купчино',
                timeToMetro: 40
            },
            {
                lineColors: [
                    '16bdf0'
                ],
                metroGeoId: 20307,
                rgbColor: '16bdf0',
                metroTransport: 'ON_TRANSPORT',
                name: 'Московская',
                timeToMetro: 46
            },
            {
                lineColors: [
                    '16bdf0'
                ],
                metroGeoId: 20306,
                rgbColor: '16bdf0',
                metroTransport: 'ON_TRANSPORT',
                name: 'Звёздная',
                timeToMetro: 48
            },
            {
                lineColors: [
                    'c063d1'
                ],
                metroGeoId: 218470,
                rgbColor: 'c063d1',
                metroTransport: 'ON_TRANSPORT',
                name: 'Дунайская',
                timeToMetro: 54
            }
        ]
    },
    viewTypes: [
        'GENERAL',
        'GENERAL',
        'COURTYARD',
        'GENERAL',
        'GENERAL',
        'GENERAL',
        'GENERAL'
    ],
    images: [],
    siteSpecialProposals: [
        {
            proposalType: 'DISCOUNT',
            description: 'Скидка 1% при повторной покупке',
            mainProposal: true,
            specialProposalType: 'discount',
            shortDescription: 'Скидка 1% при повторной покупке'
        },
        {
            proposalType: 'MORTGAGE',
            description: 'Ипотека 5.85%',
            mainProposal: false,
            specialProposalType: 'mortgage',
            shortDescription: 'Ипотека 5.85%'
        }
    ],
    buildingClass: 'COMFORT',
    state: 'UNFINISHED',
    finishedApartments: true,
    price: {
        from: 3823848,
        to: 10873296,
        currency: 'RUR',
        minPricePerMeter: 102959,
        maxPricePerMeter: 181260,
        averagePricePerMeter: 147158,
        rooms: {
            1: {
                soldout: false,
                from: 3823848,
                to: 6767964,
                currency: 'RUR',
                areas: {
                    from: '24.8',
                    to: '64.8'
                },
                hasOffers: true,
                priceRatioToMarket: 0,
                status: 'ON_SALE'
            },
            2: {
                soldout: false,
                from: 6456294,
                to: 9831780,
                currency: 'RUR',
                areas: {
                    from: '44',
                    to: '75.6'
                },
                hasOffers: true,
                priceRatioToMarket: 0,
                status: 'ON_SALE'
            },
            3: {
                soldout: false,
                from: 9072360,
                to: 10873296,
                currency: 'RUR',
                areas: {
                    from: '62.2',
                    to: '87.4'
                },
                hasOffers: true,
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
                currency: 'RUR',
                hasOffers: false,
                priceRatioToMarket: 0
            },
            PLUS_4: {
                soldout: false,
                currency: 'RUR',
                hasOffers: false,
                priceRatioToMarket: 0
            }
        },
        totalOffers: 144,
        priceRatioToMarket: 0
    },
    flatStatus: 'ON_SALE',
    developers: [
        {
            id: 2725,
            name: 'ЮИТ',
            legalName: 'АО «ЮИТ Санкт-Петербург»',
            legalNames: [
                'АО «ЮИТ Санкт-Петербург»'
            ],
            url: 'http://www.yit-dom.ru/',
            objects: {
                all: 58,
                salesOpened: 15,
                finished: 46,
                unfinished: 12,
                suspended: 0
            },
            address: 'Московская область, Люберцы, Красная улица, 4',
            born: '1911-12-31T21:29:43Z',
            hasChat: false,
            encryptedPhones: [
                {
                    phoneWithMask: '+7 812 210 ×× ××',
                    phoneHash: 'KzcF4MHTIJyMLTAN2MPjcR3',
                    tag: 0
                }
            ]
        }
    ],
    salesDepartment: {
        id: 187879,
        name: 'ТД Невский',
        isRedirectPhones: true,
        weekTimetable: [
            {
                dayFrom: 1,
                dayTo: 5,
                timePattern: [
                    {
                        open: '10:00',
                        close: '19:00'
                    }
                ]
            }
        ],
        logo: '',
        phonesWithTag: [
            {
                tag: '',
                phone: '+78127482181'
            },
            {
                tag: 'paidExp',
                phone: '+78127482181'
            },
            {
                tag: 'paidControl',
                phone: '+78127482181'
            },
            {
                tag: 'mobileRedirectControl',
                phone: '+78127482181'
            },
            {
                tag: 'mapsMobile',
                phone: '+78127482181'
            },
            {
                tag: 'mobileRedirectExp',
                phone: '+78127482181'
            }
        ],
        timetableZoneMinutes: 180,
        encryptedPhones: [
            {
                phoneWithMask: '+7 812 748 ×× ××',
                phoneHash: 'KzcF4MHTIJ3NLDgNyMPTgRx'
            }
        ],
        encryptedPhonesWithTag: [
            {
                phoneWithMask: '+7 812 748 ×× ××',
                phoneHash: 'KzcF4MHTIJ3NLDgNyMPTgRx',
                tag: ''
            },
            {
                phoneWithMask: '+7 812 748 ×× ××',
                phoneHash: 'KzcF4MHTIJ3NLDgNyMPTgRx',
                tag: 'paidExp'
            },
            {
                phoneWithMask: '+7 812 748 ×× ××',
                phoneHash: 'KzcF4MHTIJ3NLDgNyMPTgRx',
                tag: 'paidControl'
            },
            {
                phoneWithMask: '+7 812 748 ×× ××',
                phoneHash: 'KzcF4MHTIJ3NLDgNyMPTgRx',
                tag: 'mobileRedirectControl'
            },
            {
                phoneWithMask: '+7 812 748 ×× ××',
                phoneHash: 'KzcF4MHTIJ3NLDgNyMPTgRx',
                tag: 'mapsMobile'
            },
            {
                phoneWithMask: '+7 812 748 ×× ××',
                phoneHash: 'KzcF4MHTIJ3NLDgNyMPTgRx',
                tag: 'mobileRedirectExp'
            }
        ]
    },
    phone: {
        phoneWithMask: '+7 812 748 ×× ××',
        phoneHash: 'KzcF4MHTIJ3NLDgNyMPTgRx'
    },
    withBilling: true,
    awards: {},
    page: 0,
    idx: 0,
    queryId: 'e50055e43b7c174464ba3dbeea48a2ff'
};
