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
