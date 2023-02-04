module.exports = {
    firstMockVariant: {
        page: {
            isLoading: false,
            isLocked: false,
            name: 'mortgage-calculator',
            isFailed: false,
            params: {
                downPaymentSum: '900000',
                periodYears: '15',
                propertyCost: '3000000',
                rate: '8.3',
                rgid: 741965
            },
            isFromGate: true
        },
        geo: {
            id: 10174,
            type: 'SUBJECT_FEDERATION',
            rgid: 741965,
            populatedRgid: 741965,
            name: 'Санкт-Петербург и ЛО',
            locative: 'в Санкт-Петербурге и ЛО',
            parents: [
                {
                    id: 225,
                    rgid: '143',
                    name: 'Россия',
                    type: 'COUNTRY',
                    genitive: 'России'
                },
                {
                    id: 0,
                    rgid: '0',
                    name: 'Весь мир',
                    type: 'UNKNOWN'
                }
            ],
            heatmaps: [
                'infrastructure',
                'price-rent',
                'price-sell',
                'profitability',
                'transport',
                'carsharing'
            ],
            refinements: [
                'metro',
                'sub-localities',
                'map-area'
            ],
            sitesRgids: {
                district: 741965,
                mainCity: 417899
            },
            latitude: 59.938953,
            longitude: 30.31564,
            zoom: 10,
            ridWithMetro: 2,
            country: 225,
            currencies: [
                'RUR',
                'USD',
                'EUR'
            ],
            heatmapInfos: [
                {
                    name: 'infrastructure-10174',
                    type: 'infrastructure',
                    palette: [
                        {
                            level: 1,
                            from: 1,
                            to: 2,
                            color: 'ee4613'
                        },
                        {
                            level: 2,
                            from: 2,
                            to: 3,
                            color: 'f87c19'
                        },
                        {
                            level: 3,
                            from: 3,
                            to: 4,
                            color: 'fbae1e'
                        },
                        {
                            level: 4,
                            from: 4,
                            to: 5,
                            color: 'ffe424'
                        },
                        {
                            level: 5,
                            from: 5,
                            to: 6,
                            color: '9ada1b'
                        },
                        {
                            level: 6,
                            from: 6,
                            to: 7,
                            color: '30ce12'
                        },
                        {
                            level: 7,
                            from: 7,
                            to: 8,
                            color: '20a70a'
                        },
                        {
                            level: 8,
                            from: 8,
                            to: 9,
                            color: '128000'
                        }
                    ]
                },
                {
                    name: 'price-rent-10174',
                    type: 'price-rent',
                    palette: [
                        {
                            level: 1,
                            from: 57570,
                            to: 45600,
                            color: '620801'
                        },
                        {
                            level: 2,
                            from: 45600,
                            to: 39539,
                            color: 'ee4613'
                        },
                        {
                            level: 3,
                            from: 39539,
                            to: 35758,
                            color: 'f87c19'
                        },
                        {
                            level: 4,
                            from: 35758,
                            to: 32414,
                            color: 'fbae1e'
                        },
                        {
                            level: 5,
                            from: 32414,
                            to: 28082,
                            color: 'ffe424'
                        },
                        {
                            level: 6,
                            from: 28082,
                            to: 24624,
                            color: '9ada1b'
                        },
                        {
                            level: 7,
                            from: 24624,
                            to: 22154,
                            color: '30ce12'
                        },
                        {
                            level: 8,
                            from: 22154,
                            to: 19836,
                            color: '20a70a'
                        },
                        {
                            level: 9,
                            from: 19836,
                            to: 7448,
                            color: '128000'
                        }
                    ]
                },
                {
                    name: 'price-sell-10174',
                    type: 'price-sell',
                    palette: [
                        {
                            level: 1,
                            from: 795539,
                            to: 328330,
                            color: '620801'
                        },
                        {
                            level: 2,
                            from: 328330,
                            to: 262902,
                            color: 'ee4613'
                        },
                        {
                            level: 3,
                            from: 262902,
                            to: 224550,
                            color: 'f87c19'
                        },
                        {
                            level: 4,
                            from: 224550,
                            to: 198302,
                            color: 'fbae1e'
                        },
                        {
                            level: 5,
                            from: 198302,
                            to: 180556,
                            color: 'ffe424'
                        },
                        {
                            level: 6,
                            from: 180556,
                            to: 163623,
                            color: '9ada1b'
                        },
                        {
                            level: 7,
                            from: 163623,
                            to: 148304,
                            color: '30ce12'
                        },
                        {
                            level: 8,
                            from: 148304,
                            to: 129684,
                            color: '20a70a'
                        },
                        {
                            level: 9,
                            from: 129684,
                            to: 11527,
                            color: '128000'
                        }
                    ]
                },
                {
                    name: 'profitability-10174',
                    type: 'profitability',
                    palette: [
                        {
                            level: 1,
                            from: 47,
                            to: 33,
                            color: '620801'
                        },
                        {
                            level: 2,
                            from: 33,
                            to: 29,
                            color: 'ee4613'
                        },
                        {
                            level: 3,
                            from: 29,
                            to: 27,
                            color: 'f87c19'
                        },
                        {
                            level: 4,
                            from: 27,
                            to: 26,
                            color: 'fbae1e'
                        },
                        {
                            level: 5,
                            from: 26,
                            to: 25,
                            color: 'ffe424'
                        },
                        {
                            level: 6,
                            from: 25,
                            to: 24,
                            color: '9ada1b'
                        },
                        {
                            level: 7,
                            from: 24,
                            to: 22,
                            color: '30ce12'
                        },
                        {
                            level: 8,
                            from: 22,
                            to: 21,
                            color: '20a70a'
                        },
                        {
                            level: 9,
                            from: 21,
                            to: 8,
                            color: '128000'
                        }
                    ]
                },
                {
                    name: 'transport-10174',
                    type: 'transport',
                    palette: [
                        {
                            level: 1,
                            from: 1,
                            to: 2,
                            color: '620801'
                        },
                        {
                            level: 2,
                            from: 2,
                            to: 3,
                            color: 'ee4613'
                        },
                        {
                            level: 3,
                            from: 3,
                            to: 4,
                            color: 'f87c19'
                        },
                        {
                            level: 4,
                            from: 4,
                            to: 5,
                            color: 'fbae1e'
                        },
                        {
                            level: 5,
                            from: 5,
                            to: 6,
                            color: 'ffe424'
                        },
                        {
                            level: 6,
                            from: 6,
                            to: 7,
                            color: '9ada1b'
                        },
                        {
                            level: 7,
                            from: 7,
                            to: 8,
                            color: '30ce12'
                        },
                        {
                            level: 8,
                            from: 8,
                            to: 9,
                            color: '20a70a'
                        },
                        {
                            level: 9,
                            from: 9,
                            to: 10,
                            color: '128000'
                        }
                    ]
                },
                {
                    name: 'carsharing-10174',
                    type: 'carsharing',
                    palette: [
                        {
                            level: 0,
                            from: 0,
                            to: 1,
                            color: '620801'
                        },
                        {
                            level: 1,
                            from: 1,
                            to: 2,
                            color: 'ee4613'
                        },
                        {
                            level: 2,
                            from: 2,
                            to: 3,
                            color: 'f87c19'
                        },
                        {
                            level: 3,
                            from: 3,
                            to: 4,
                            color: 'fbae1e'
                        },
                        {
                            level: 4,
                            from: 4,
                            to: 5,
                            color: 'ffe424'
                        },
                        {
                            level: 5,
                            from: 5,
                            to: 6,
                            color: '9ada1b'
                        },
                        {
                            level: 6,
                            from: 6,
                            to: 7,
                            color: '30ce12'
                        },
                        {
                            level: 7,
                            from: 7,
                            to: 8,
                            color: '20a70a'
                        },
                        {
                            level: 8,
                            from: 8,
                            to: 9,
                            color: '128000'
                        }
                    ]
                }
            ],
            hasConcierge: true,
            hasCommercialBuildings: true,
            hasMetro: true,
            hasSites: true,
            hasVillages: true,
            hasPik: true,
            hasYandexRent: true,
            showMap: true,
            isInOrlovskObl: false,
            isInPrimorskiyKrai: false,
            isInHabarovskKrai: false,
            isInTomskObl: false,
            isInKurganskObl: false,
            isInKirovskObl: false,
            isInUlyanovskObl: false,
            isInVologodskObl: false,
            isInPskovskObl: false,
            isInKalinigradskObl: false,
            isInIvanovskObl: false,
            isInChuvashskiya: false,
            isInUdmurtiya: false,
            isInSmolenskObl: false,
            isInKemerovskObl: false,
            isInTambovskObl: false,
            isInOmskObl: false,
            isInVladimirskObl: false,
            isInAltayskKrai: false,
            isInOrenburgObl: false,
            isInIrkutskObl: false,
            isInBashkortostan: false,
            isInStavropolskKrai: false,
            isInYaroslavskObl: false,
            isInVolgogradskObl: false,
            isInSevastopol: false,
            isInCrimea: false,
            isInBryanskObl: false,
            isInRyazanObl: false,
            isInLipetskObl: false,
            isInTverObl: false,
            isInTulaObl: false,
            isInTatarstan: false,
            isInKurskObl: false,
            isInKrasnoyarskKrai: false,
            isInTyumenObl: false,
            isInSaratovObl: false,
            isInChelyabinskObl: false,
            isInNovosibirskObl: false,
            isInSamaraObl: false,
            isInVoronezhObl: false,
            isInNizhnyNovgorodObl: false,
            isInPermKrai: false,
            isInKalugaObl: false,
            isInYaroslavlObl: false,
            isInAdygeya: false,
            isInSverdObl: false,
            isInRostovObl: false,
            isInBelgorodskObl: false,
            isInPenzenskObl: false,
            isInArhangelskObl: false,
            isInMO: false,
            isInLO: true,
            isInKK: false,
            isObninsk: false,
            isRostovOnDon: false,
            isMsk: false,
            isSpb: false,
            rgidVoronezh: 569171,
            rgidVoronezhObl: 475531,
            rgidSverdObl: 326698,
            rgidRostovObl: 211571,
            rgidKK: 353118,
            rgidMsk: 587795,
            rgidMO: 741964,
            rgidLO: 741965,
            rgidSpb: 417899,
            rgidTatarstan: 426660,
            rgidNizhnyNovgorodObl: 426764
        },
        mortgagePrograms: {
            items: [],
            pager: {
                totalItems: 75,
                page: 0,
                pageSize: 20
            },
            isMoreLoading: false,
            queryId: 'a01a5361ffbd33643304e1914534704c',
            calculatorLimits: {
                minCreditAmount: 1000000,
                maxCreditAmount: 300000000,
                minPropertyCost: 1176471,
                maxPropertyCost: 200000000,
                minPeriodYears: 15,
                maxPeriodYears: 50,
                minDownPayment: 15,
                minDownPaymentSum: 176471,
                maxDownPayment: 100,
                maxDownPaymentSum: 200000000,
                minRate: 2,
                maxRate: 20
            },
            defaultCalculatorLimits: {
                minCreditAmount: 1000000,
                maxCreditAmount: 300000000,
                minPropertyCost: 1176471,
                maxPropertyCost: 200000000,
                minPeriodYears: 15,
                maxPeriodYears: 50,
                minDownPayment: 15,
                minDownPaymentSum: 176471,
                maxDownPayment: 100,
                maxDownPaymentSum: 200000000,
                minRate: 2,
                maxRate: 20
            },
            searchQuery: {
                regionId: [
                    1
                ],
                flatType: [
                    'NEW_FLAT'
                ],
                propertyCost: 10000000,
                downPayment: 35,
                downPaymentSum: 3500000,
                periodYears: 26,
                sort: 'BANK_PRIORITY'
            },
            defaultSearchQuery: {
                propertyCost: 10000000,
                downPayment: 35,
                downPaymentSum: 3500000,
                periodYears: 30
            },
            banks: [
                {
                    id: '358023',
                    name: 'Альфа-Банк',
                    logo: '//avatars.mds.yandex.net/get-verba/216201/2a0000016cb04a09bb8d84d276bc2531af2f/optimize'
                },
                {
                    id: '350567',
                    name: 'Росбанк Дом',
                    logo: '//avatars.mds.yandex.net/get-verba/1030388/2a0000016cb069ba8dc0d5041a1bf2491124/optimize'
                },
                {
                    id: '386832',
                    name: 'АТБ',
                    logo: '//avatars.mds.yandex.net/get-verba/216201/2a0000016cb048654f13eb5f9dcc71f67653/optimize'
                },
                {
                    id: '322407',
                    name: 'Абсолют',
                    logo: '//avatars.mds.yandex.net/get-verba/787013/2a0000016cb048fbff6b3cc18ca7fbe77698/optimize'
                },
                {
                    id: '323165',
                    name: 'Ак Барс',
                    logo: '//avatars.mds.yandex.net/get-verba/216201/2a0000016cb04931eebf3f709fef545d488a/optimize'
                },
                {
                    id: '327536',
                    name: 'Банк Зенит',
                    logo: '//avatars.mds.yandex.net/get-verba/997355/2a0000017a7556b6320e02b5e28b5f0671ae/optimize'
                },
                {
                    id: '322371',
                    name: 'ВТБ',
                    logo: '//avatars.mds.yandex.net/get-verba/1540742/2a0000016cb05a8c66526d9d2f4837191af4/optimize'
                },
                {
                    id: '904709',
                    name: 'Всероссийский банк развития регионов',
                    logo: '//avatars.mds.yandex.net/get-verba/1540742/2a0000016cb05b09144af2993c043e3814b6/optimize'
                },
                {
                    id: '322392',
                    name: 'Газпромбанк',
                    logo: '//avatars.mds.yandex.net/get-verba/1030388/2a0000016cb05b420f0066f1a08b3d67ecd9/optimize'
                },
                {
                    id: '322404',
                    name: 'ДОМ.РФ',
                    logo: '//avatars.mds.yandex.net/get-verba/787013/2a0000016cb05bc0cc4d5ceb7e5d458c3b83/optimize'
                },
                {
                    id: '324749',
                    name: 'Московский кредитный банк',
                    logo: '//avatars.mds.yandex.net/get-verba/787013/2a0000016cb0624c861840940beb527939ac/optimize'
                },
                {
                    id: '323579',
                    name: 'Открытие',
                    logo: '//avatars.mds.yandex.net/get-verba/787013/2a0000016cb0514b425b20f7acbd844e7c51/optimize'
                },
                {
                    id: '323157',
                    name: 'Промсвязьбанк',
                    logo: '//avatars.mds.yandex.net/get-verba/1030388/2a0000016cb067983e263e779d3b3cd6b0d3/optimize'
                },
                {
                    id: '326507',
                    name: 'РНКБ',
                    logo: '//avatars.mds.yandex.net/get-verba/787013/2a0000016cb068de71c446043b537681b732/optimize'
                },
                {
                    id: '322384',
                    name: 'Райффайзенбанк',
                    logo: '//avatars.mds.yandex.net/get-verba/1030388/2a0000016cb06919d3572e9ae8519a7b509d/optimize'
                },
                {
                    id: '322396',
                    name: 'Россельхозбанк',
                    logo: '//avatars.mds.yandex.net/get-verba/787013/2a0000016cb06b54ae25ae66d10d6813d4eb/optimize'
                },
                {
                    id: '327488',
                    name: 'СМП Банк',
                    logo: '//avatars.mds.yandex.net/get-verba/787013/2a0000016cb06d47c01efb04b31b981c375b/optimize'
                },
                {
                    id: '322400',
                    name: 'Санкт-Петербург',
                    logo: '//avatars.mds.yandex.net/get-verba/787013/2a0000016cb05570d531739f160d5c3badc5/optimize'
                },
                {
                    id: '316194',
                    name: 'Сбербанк',
                    logo: '//avatars.mds.yandex.net/get-verba/1540742/2a00000175211ef03a6f9f87ac362783610f/optimize'
                },
                {
                    id: '491213',
                    name: 'Совкомбанк',
                    logo: '//avatars.mds.yandex.net/get-verba/1030388/2a0000016cb0717de5ac405515d13fd77b80/optimize'
                },
                {
                    id: '383421',
                    name: 'Сургутнефтегазбанк',
                    logo: '//avatars.mds.yandex.net/get-verba/787013/2a0000016cb071c2b61dc7f9745695d4b5f2/optimize'
                },
                {
                    id: '322375',
                    name: 'Транскапиталбанк',
                    logo: '//avatars.mds.yandex.net/get-verba/1030388/2a0000016cb07329e90947f5494e9317884e/optimize'
                },
                {
                    id: '322388',
                    name: 'Уралсиб',
                    logo: '//avatars.mds.yandex.net/get-verba/1030388/2a0000016cb073dfb1714861cbc6ed387916/optimize'
                }
            ],
            filtersCount: 75,
            isFiltersCountLoading: false,
            getSnippetsStatus: 'LOADED',
            bankCount: 19,
            minRate: 4.39,
            isLoading: false
        },
        mortgageProgram: {
            similarPrograms: [],
            calculateMortgageStatus: 'LOADED',
            otherPrograms: [],
            getSnippetsStatus: 'LOADED'
        },
        mortgageBank: {},
        mortgageCalculator: {
            calculator: {
                creditAmount: 2100000,
                monthlyPayment: 20434,
                monthlyPaymentParams: {
                    rate: 8.3,
                    propertyCost: 3000000,
                    periodYears: 15,
                    downPayment: 30,
                    downPaymentSum: 900000
                },
                calculatorLimits: {
                    minCreditAmount: 300000,
                    maxCreditAmount: 30000000,
                    minPropertyCost: 333334,
                    maxPropertyCost: 50000000,
                    minPeriodYears: 10,
                    maxPeriodYears: 30,
                    minDownPayment: 10,
                    minDownPaymentSum: 33334,
                    maxDownPayment: 100,
                    maxDownPaymentSum: 50000000,
                    minRate: 1,
                    maxRate: 15
                },
                overpayment: '1578120',
                queryId: '0cfbe66ddf2ce772f4d758cdc529a790'
            },
            isMortgageProgramsLoading: false,
            isMortgageProgramsError: false,
            isMoreMortgageProgramsLoading: false,
            offersRequestStatus: 'LOADED',
            programs: {
                pager: {
                    totalItems: 121,
                    page: 0,
                    pageSize: 20
                },
                calculatorLimits: {
                    minCreditAmount: 300000,
                    maxCreditAmount: 30000000,
                    minPropertyCost: 333334,
                    maxPropertyCost: 50000000,
                    minPeriodYears: 10,
                    maxPeriodYears: 30,
                    minDownPayment: 10,
                    minDownPaymentSum: 33334,
                    maxDownPayment: 100,
                    maxDownPaymentSum: 50000000,
                    minRate: 1,
                    maxRate: 15
                },
                defaultCalculatorLimits: {
                    minCreditAmount: 300000,
                    maxCreditAmount: 30000000,
                    minPropertyCost: 333334,
                    maxPropertyCost: 50000000,
                    minPeriodYears: 10,
                    maxPeriodYears: 30,
                    minDownPayment: 10,
                    minDownPaymentSum: 33334,
                    maxDownPayment: 100,
                    maxDownPaymentSum: 50000000,
                    minRate: 1,
                    maxRate: 15
                },
                searchQuery: {
                    regionId: [
                        10174
                    ],
                    propertyCost: 3000000,
                    downPayment: 30,
                    downPaymentSum: 900000,
                    periodYears: 15,
                    maxRate: 8.3,
                    sort: 'BANK_PRIORITY'
                },
                defaultSearchQuery: {
                    propertyCost: 3000000,
                    downPayment: 30,
                    downPaymentSum: 900000,
                    periodYears: 15
                },
                fallback: false,
                bankCount: 23,
                minRate: 0.01
            }
        },
        mortgageApplicationForm: {
            applicationFromScreen: 'APPLICATION',
            applicationCreationStatus: 'LOADED',
            applicationConfirmationStatus: 'LOADED'
        }
    },
    secondMockVariant: {
        geo: {
            id: 1,
            type: 'SUBJECT_FEDERATION',
            rgid: 741964,
            populatedRgid: 741964,
            name: 'Москва и МО',
            locative: 'в Москве и МО',
            parents: [
                {
                    id: 225,
                    rgid: '143',
                    name: 'Россия',
                    type: 'COUNTRY',
                    genitive: 'России'
                },
                {
                    id: 0,
                    rgid: '0',
                    name: 'Весь мир',
                    type: 'UNKNOWN'
                }
            ],
            heatmaps: [
                'infrastructure',
                'price-rent',
                'price-sell',
                'profitability',
                'transport',
                'carsharing'
            ],
            refinements: [
                'metro',
                'directions',
                'sub-localities',
                'map-area'
            ],
            sitesRgids: {
                district: 741964,
                mainCity: 587795
            },
            latitude: 55.75322,
            longitude: 37.62251,
            zoom: 10,
            ridWithMetro: 213,
            country: 225,
            currencies: [
                'RUR',
                'USD',
                'EUR'
            ],
            heatmapInfos: [
                {
                    name: 'infrastructure-1',
                    type: 'infrastructure',
                    palette: [
                        {
                            level: 1,
                            from: 1,
                            to: 2,
                            color: 'ee4613'
                        },
                        {
                            level: 2,
                            from: 2,
                            to: 3,
                            color: 'f87c19'
                        },
                        {
                            level: 3,
                            from: 3,
                            to: 4,
                            color: 'fbae1e'
                        },
                        {
                            level: 4,
                            from: 4,
                            to: 5,
                            color: 'ffe424'
                        },
                        {
                            level: 5,
                            from: 5,
                            to: 6,
                            color: '9ada1b'
                        },
                        {
                            level: 6,
                            from: 6,
                            to: 7,
                            color: '30ce12'
                        },
                        {
                            level: 7,
                            from: 7,
                            to: 8,
                            color: '20a70a'
                        },
                        {
                            level: 8,
                            from: 8,
                            to: 9,
                            color: '128000'
                        }
                    ]
                },
                {
                    name: 'price-rent-1',
                    type: 'price-rent',
                    palette: [
                        {
                            level: 1,
                            from: 128478,
                            to: 73416,
                            color: '620801'
                        },
                        {
                            level: 2,
                            from: 73416,
                            to: 62358,
                            color: 'ee4613'
                        },
                        {
                            level: 3,
                            from: 62358,
                            to: 53751,
                            color: 'f87c19'
                        },
                        {
                            level: 4,
                            from: 53751,
                            to: 47500,
                            color: 'fbae1e'
                        },
                        {
                            level: 5,
                            from: 47500,
                            to: 42541,
                            color: 'ffe424'
                        },
                        {
                            level: 6,
                            from: 42541,
                            to: 38380,
                            color: '9ada1b'
                        },
                        {
                            level: 7,
                            from: 38380,
                            to: 34542,
                            color: '30ce12'
                        },
                        {
                            level: 8,
                            from: 34542,
                            to: 29184,
                            color: '20a70a'
                        },
                        {
                            level: 9,
                            from: 29184,
                            to: 10374,
                            color: '128000'
                        }
                    ]
                },
                {
                    name: 'price-sell-1',
                    type: 'price-sell',
                    palette: [
                        {
                            level: 1,
                            from: 1858678,
                            to: 538026,
                            color: '620801'
                        },
                        {
                            level: 2,
                            from: 538026,
                            to: 411297,
                            color: 'ee4613'
                        },
                        {
                            level: 3,
                            from: 411297,
                            to: 347648,
                            color: 'f87c19'
                        },
                        {
                            level: 4,
                            from: 347648,
                            to: 308167,
                            color: 'fbae1e'
                        },
                        {
                            level: 5,
                            from: 308167,
                            to: 274577,
                            color: 'ffe424'
                        },
                        {
                            level: 6,
                            from: 274577,
                            to: 240300,
                            color: '9ada1b'
                        },
                        {
                            level: 7,
                            from: 240300,
                            to: 208315,
                            color: '30ce12'
                        },
                        {
                            level: 8,
                            from: 208315,
                            to: 139831,
                            color: '20a70a'
                        },
                        {
                            level: 9,
                            from: 139831,
                            to: 18182,
                            color: '128000'
                        }
                    ]
                },
                {
                    name: 'profitability-1',
                    type: 'profitability',
                    palette: [
                        {
                            level: 1,
                            from: 82,
                            to: 33,
                            color: '620801'
                        },
                        {
                            level: 2,
                            from: 33,
                            to: 29,
                            color: 'ee4613'
                        },
                        {
                            level: 3,
                            from: 29,
                            to: 27,
                            color: 'f87c19'
                        },
                        {
                            level: 4,
                            from: 27,
                            to: 26,
                            color: 'fbae1e'
                        },
                        {
                            level: 5,
                            from: 26,
                            to: 25,
                            color: 'ffe424'
                        },
                        {
                            level: 6,
                            from: 25,
                            to: 23,
                            color: '9ada1b'
                        },
                        {
                            level: 7,
                            from: 23,
                            to: 22,
                            color: '30ce12'
                        },
                        {
                            level: 8,
                            from: 22,
                            to: 20,
                            color: '20a70a'
                        },
                        {
                            level: 9,
                            from: 20,
                            to: 5,
                            color: '128000'
                        }
                    ]
                },
                {
                    name: 'transport-1',
                    type: 'transport',
                    palette: [
                        {
                            level: 1,
                            from: 1,
                            to: 2,
                            color: '620801'
                        },
                        {
                            level: 2,
                            from: 2,
                            to: 3,
                            color: 'ee4613'
                        },
                        {
                            level: 3,
                            from: 3,
                            to: 4,
                            color: 'f87c19'
                        },
                        {
                            level: 4,
                            from: 4,
                            to: 5,
                            color: 'fbae1e'
                        },
                        {
                            level: 5,
                            from: 5,
                            to: 6,
                            color: 'ffe424'
                        },
                        {
                            level: 6,
                            from: 6,
                            to: 7,
                            color: '9ada1b'
                        },
                        {
                            level: 7,
                            from: 7,
                            to: 8,
                            color: '30ce12'
                        },
                        {
                            level: 8,
                            from: 8,
                            to: 9,
                            color: '20a70a'
                        },
                        {
                            level: 9,
                            from: 9,
                            to: 10,
                            color: '128000'
                        }
                    ]
                },
                {
                    name: 'carsharing-1',
                    type: 'carsharing',
                    palette: [
                        {
                            level: 0,
                            from: 0,
                            to: 1,
                            color: '620801'
                        },
                        {
                            level: 1,
                            from: 1,
                            to: 2,
                            color: 'ee4613'
                        },
                        {
                            level: 2,
                            from: 2,
                            to: 3,
                            color: 'f87c19'
                        },
                        {
                            level: 3,
                            from: 3,
                            to: 4,
                            color: 'fbae1e'
                        },
                        {
                            level: 4,
                            from: 4,
                            to: 5,
                            color: 'ffe424'
                        },
                        {
                            level: 5,
                            from: 5,
                            to: 6,
                            color: '9ada1b'
                        },
                        {
                            level: 6,
                            from: 6,
                            to: 7,
                            color: '30ce12'
                        },
                        {
                            level: 7,
                            from: 7,
                            to: 8,
                            color: '20a70a'
                        },
                        {
                            level: 8,
                            from: 8,
                            to: 9,
                            color: '128000'
                        }
                    ]
                }
            ],
            hasConcierge: true,
            hasCommercialBuildings: true,
            hasMetro: true,
            hasSites: true,
            hasVillages: true,
            hasPik: true,
            hasYandexRent: true,
            showMap: true,
            schoolInfo: {
                total: 399,
                highRatingColor: 'c847ff',
                lowRatingColor: '36107c'
            },
            isInOrlovskObl: false,
            isInPrimorskiyKrai: false,
            isInHabarovskKrai: false,
            isInTomskObl: false,
            isInKurganskObl: false,
            isInKirovskObl: false,
            isInUlyanovskObl: false,
            isInVologodskObl: false,
            isInPskovskObl: false,
            isInKalinigradskObl: false,
            isInIvanovskObl: false,
            isInChuvashskiya: false,
            isInUdmurtiya: false,
            isInSmolenskObl: false,
            isInKemerovskObl: false,
            isInTambovskObl: false,
            isInOmskObl: false,
            isInVladimirskObl: false,
            isInAltayskKrai: false,
            isInOrenburgObl: false,
            isInIrkutskObl: false,
            isInBashkortostan: false,
            isInStavropolskKrai: false,
            isInYaroslavskObl: false,
            isInVolgogradskObl: false,
            isInSevastopol: false,
            isInCrimea: false,
            isInBryanskObl: false,
            isInRyazanObl: false,
            isInLipetskObl: false,
            isInTverObl: false,
            isInTulaObl: false,
            isInTatarstan: false,
            isInKurskObl: false,
            isInKrasnoyarskKrai: false,
            isInTyumenObl: false,
            isInSaratovObl: false,
            isInChelyabinskObl: false,
            isInNovosibirskObl: false,
            isInSamaraObl: false,
            isInVoronezhObl: false,
            isInNizhnyNovgorodObl: false,
            isInPermKrai: false,
            isInKalugaObl: false,
            isInYaroslavlObl: false,
            isInAdygeya: false,
            isInSverdObl: false,
            isInRostovObl: false,
            isInBelgorodskObl: false,
            isInPenzenskObl: false,
            isInArhangelskObl: false,
            isInMO: true,
            isInLO: false,
            isInKK: false,
            isObninsk: false,
            isRostovOnDon: false,
            isMsk: false,
            isSpb: false,
            rgidVoronezh: 569171,
            rgidVoronezhObl: 475531,
            rgidSverdObl: 326698,
            rgidRostovObl: 211571,
            rgidKK: 353118,
            rgidMsk: 587795,
            rgidMO: 741964,
            rgidLO: 741965,
            rgidSpb: 417899,
            rgidTatarstan: 426660,
            rgidNizhnyNovgorodObl: 426764
        },
        user: {
            crc: 'y2fd7659d4c9b3ba8010cf1b0a5a3bf21',
            uid: '',
            yuid: '393167231637669633',
            isVosUser: false,
            isAuth: false,
            isJuridical: false,
            paymentTypeSuffix: 'natural',
            promoSubscription: {},
            avatarHost: 'avatars.mdst.yandex.net',
            emails: [],
            emailHash: '',
            defaultPhone: null,
            passHost: 'https://pass-test.yandex.ru',
            passportHost: 'https://passport-test.yandex.ru',
            passportApiHost: 'https://api.passport-test.yandex.ru',
            passportOrigin: 'realty_moscow',
            passportPhones: [],
            favorites: [],
            favoritesMap: {},
            comparison: [],
            statistics: {},
            walletInfo: {
                isFilled: false
            }
        },
        params: {
            'mortgage-calculator': [
                'rgid',
                'sublocality',
                'propertyCost',
                'downPaymentSum',
                'periodYears',
                'rate'
            ],
            geo: [
                'rgid',
                'sublocality'
            ],
            seo: [
                'rgid',
                'sublocality'
            ],
            'site-special-projects': [
                'rgid',
                'sublocality'
            ]
        },
        mortgagePrograms: {
            items: [],
            pager: {
                totalItems: 71,
                page: 0,
                pageSize: 20
            },
            isMoreLoading: false,
            queryId: 'a01a5361ffbd33643304e1914534704c',
            calculatorLimits: {
                minCreditAmount: 1000000,
                maxCreditAmount: 300000000,
                minPropertyCost: 1176471,
                maxPropertyCost: 200000000,
                minPeriodYears: 15,
                maxPeriodYears: 50,
                minDownPayment: 15,
                minDownPaymentSum: 176471,
                maxDownPayment: 100,
                maxDownPaymentSum: 200000000,
                minRate: 2,
                maxRate: 20
            },
            defaultCalculatorLimits: {
                minCreditAmount: 1000000,
                maxCreditAmount: 300000000,
                minPropertyCost: 1176471,
                maxPropertyCost: 200000000,
                minPeriodYears: 15,
                maxPeriodYears: 50,
                minDownPayment: 15,
                minDownPaymentSum: 176471,
                maxDownPayment: 100,
                maxDownPaymentSum: 200000000,
                minRate: 2,
                maxRate: 20
            },
            searchQuery: {
                regionId: [
                    1
                ],
                mortgageType: [
                    'YOUNG_FAMILY'
                ],
                propertyCost: 10000000,
                downPayment: 35,
                downPaymentSum: 3500000,
                periodYears: 26,
                sort: 'BANK_PRIORITY'
            },
            defaultSearchQuery: {
                propertyCost: 10000000,
                downPayment: 35,
                downPaymentSum: 3500000,
                periodYears: 30
            },
            banks: [
                {
                    id: '358023',
                    name: 'Альфа-Банк',
                    logo: '//avatars.mds.yandex.net/get-verba/216201/2a0000016cb04a09bb8d84d276bc2531af2f/optimize'
                },
                {
                    id: '350567',
                    name: 'Росбанк Дом',
                    logo: '//avatars.mds.yandex.net/get-verba/1030388/2a0000016cb069ba8dc0d5041a1bf2491124/optimize'
                },
                {
                    id: '386832',
                    name: 'АТБ',
                    logo: '//avatars.mds.yandex.net/get-verba/216201/2a0000016cb048654f13eb5f9dcc71f67653/optimize'
                },
                {
                    id: '322407',
                    name: 'Абсолют',
                    logo: '//avatars.mds.yandex.net/get-verba/787013/2a0000016cb048fbff6b3cc18ca7fbe77698/optimize'
                },
                {
                    id: '323165',
                    name: 'Ак Барс',
                    logo: '//avatars.mds.yandex.net/get-verba/216201/2a0000016cb04931eebf3f709fef545d488a/optimize'
                },
                {
                    id: '327536',
                    name: 'Банк Зенит',
                    logo: '//avatars.mds.yandex.net/get-verba/997355/2a0000017a7556b6320e02b5e28b5f0671ae/optimize'
                },
                {
                    id: '322371',
                    name: 'ВТБ',
                    logo: '//avatars.mds.yandex.net/get-verba/1540742/2a0000016cb05a8c66526d9d2f4837191af4/optimize'
                },
                {
                    id: '904709',
                    name: 'Всероссийский банк развития регионов',
                    logo: '//avatars.mds.yandex.net/get-verba/1540742/2a0000016cb05b09144af2993c043e3814b6/optimize'
                },
                {
                    id: '322392',
                    name: 'Газпромбанк',
                    logo: '//avatars.mds.yandex.net/get-verba/1030388/2a0000016cb05b420f0066f1a08b3d67ecd9/optimize'
                },
                {
                    id: '322404',
                    name: 'ДОМ.РФ',
                    logo: '//avatars.mds.yandex.net/get-verba/787013/2a0000016cb05bc0cc4d5ceb7e5d458c3b83/optimize'
                },
                {
                    id: '324749',
                    name: 'Московский кредитный банк',
                    logo: '//avatars.mds.yandex.net/get-verba/787013/2a0000016cb0624c861840940beb527939ac/optimize'
                },
                {
                    id: '323579',
                    name: 'Открытие',
                    logo: '//avatars.mds.yandex.net/get-verba/787013/2a0000016cb0514b425b20f7acbd844e7c51/optimize'
                },
                {
                    id: '323157',
                    name: 'Промсвязьбанк',
                    logo: '//avatars.mds.yandex.net/get-verba/1030388/2a0000016cb067983e263e779d3b3cd6b0d3/optimize'
                },
                {
                    id: '326507',
                    name: 'РНКБ',
                    logo: '//avatars.mds.yandex.net/get-verba/787013/2a0000016cb068de71c446043b537681b732/optimize'
                },
                {
                    id: '322384',
                    name: 'Райффайзенбанк',
                    logo: '//avatars.mds.yandex.net/get-verba/1030388/2a0000016cb06919d3572e9ae8519a7b509d/optimize'
                },
                {
                    id: '322396',
                    name: 'Россельхозбанк',
                    logo: '//avatars.mds.yandex.net/get-verba/787013/2a0000016cb06b54ae25ae66d10d6813d4eb/optimize'
                },
                {
                    id: '327488',
                    name: 'СМП Банк',
                    logo: '//avatars.mds.yandex.net/get-verba/787013/2a0000016cb06d47c01efb04b31b981c375b/optimize'
                },
                {
                    id: '322400',
                    name: 'Санкт-Петербург',
                    logo: '//avatars.mds.yandex.net/get-verba/787013/2a0000016cb05570d531739f160d5c3badc5/optimize'
                },
                {
                    id: '316194',
                    name: 'Сбербанк',
                    logo: '//avatars.mds.yandex.net/get-verba/1540742/2a00000175211ef03a6f9f87ac362783610f/optimize'
                },
                {
                    id: '491213',
                    name: 'Совкомбанк',
                    logo: '//avatars.mds.yandex.net/get-verba/1030388/2a0000016cb0717de5ac405515d13fd77b80/optimize'
                }
            ],
            filtersCount: 71,
            isFiltersCountLoading: false,
            getSnippetsStatus: 'LOADED',
            bankCount: 21,
            minRate: 4.39,
            isLoading: false
        },
        mortgageProgram: {
            similarPrograms: [],
            calculateMortgageStatus: 'LOADED',
            otherPrograms: [],
            getSnippetsStatus: 'LOADED'
        },
        mortgageBank: {},
        mortgageCalculator: {
            calculator: {
                creditAmount: 7140000,
                monthlyPayment: 67302,
                monthlyPaymentParams: {
                    rate: 8.3,
                    propertyCost: 34810000,
                    periodYears: 16,
                    downPayment: 79,
                    downPaymentSum: 27670000
                },
                calculatorLimits: {
                    minCreditAmount: 1000000,
                    maxCreditAmount: 300000000,
                    minPropertyCost: 1176471,
                    maxPropertyCost: 200000000,
                    minPeriodYears: 15,
                    maxPeriodYears: 50,
                    minDownPayment: 15,
                    minDownPaymentSum: 176471,
                    maxDownPayment: 100,
                    maxDownPaymentSum: 200000000,
                    minRate: 2,
                    maxRate: 20
                },
                overpayment: '5781984',
                queryId: '3f9129af8e9458f279a6a402bdec828d'
            },
            isMortgageProgramsLoading: false,
            isMortgageProgramsError: false,
            isMoreMortgageProgramsLoading: false,
            offersRequestStatus: 'LOADED',
            programs: {
                items: [],
                pager: {
                    totalItems: 80,
                    page: 0,
                    pageSize: 20
                },
                calculatorLimits: {
                    minCreditAmount: 1000000,
                    maxCreditAmount: 300000000,
                    minPropertyCost: 1176471,
                    maxPropertyCost: 200000000,
                    minPeriodYears: 15,
                    maxPeriodYears: 50,
                    minDownPayment: 15,
                    minDownPaymentSum: 176471,
                    maxDownPayment: 100,
                    maxDownPaymentSum: 200000000,
                    minRate: 2,
                    maxRate: 20
                },
                defaultCalculatorLimits: {
                    minCreditAmount: 1000000,
                    maxCreditAmount: 300000000,
                    minPropertyCost: 1176471,
                    maxPropertyCost: 200000000,
                    minPeriodYears: 15,
                    maxPeriodYears: 50,
                    minDownPayment: 15,
                    minDownPaymentSum: 176471,
                    maxDownPayment: 100,
                    maxDownPaymentSum: 200000000,
                    minRate: 2,
                    maxRate: 20
                },
                searchQuery: {
                    regionId: [
                        1
                    ],
                    propertyCost: 34810000,
                    downPayment: 79,
                    downPaymentSum: 27670000,
                    periodYears: 16,
                    maxRate: 8.3,
                    sort: 'BANK_PRIORITY'
                },
                defaultSearchQuery: {
                    propertyCost: 10000000,
                    downPayment: 35,
                    downPaymentSum: 3500000,
                    periodYears: 30
                },
                fallback: false,
                bankCount: 21,
                minRate: 3.5
            }
        },
        mortgageApplicationForm: {
            applicationFromScreen: 'APPLICATION',
            applicationCreationStatus: 'LOADED',
            applicationConfirmationStatus: 'LOADED'
        }
    },
    mockWithoutPrograms: {
        geo: {
            id: 1,
            type: 'SUBJECT_FEDERATION',
            rgid: 741964,
            populatedRgid: 741964,
            name: 'Москва и МО',
            locative: 'в Москве и МО',
            parents: [
                {
                    id: 225,
                    rgid: '143',
                    name: 'Россия',
                    type: 'COUNTRY',
                    genitive: 'России'
                },
                {
                    id: 0,
                    rgid: '0',
                    name: 'Весь мир',
                    type: 'UNKNOWN'
                }
            ],
            heatmaps: [
                'infrastructure',
                'price-rent',
                'price-sell',
                'profitability',
                'transport',
                'carsharing'
            ],
            refinements: [
                'metro',
                'directions',
                'sub-localities',
                'map-area'
            ],
            sitesRgids: {
                district: 741964,
                mainCity: 587795
            },
            latitude: 55.75322,
            longitude: 37.62251,
            zoom: 10,
            ridWithMetro: 213,
            country: 225,
            currencies: [
                'RUR',
                'USD',
                'EUR'
            ],
            heatmapInfos: [
                {
                    name: 'infrastructure-1',
                    type: 'infrastructure',
                    palette: [
                        {
                            level: 1,
                            from: 1,
                            to: 2,
                            color: 'ee4613'
                        },
                        {
                            level: 2,
                            from: 2,
                            to: 3,
                            color: 'f87c19'
                        },
                        {
                            level: 3,
                            from: 3,
                            to: 4,
                            color: 'fbae1e'
                        },
                        {
                            level: 4,
                            from: 4,
                            to: 5,
                            color: 'ffe424'
                        },
                        {
                            level: 5,
                            from: 5,
                            to: 6,
                            color: '9ada1b'
                        },
                        {
                            level: 6,
                            from: 6,
                            to: 7,
                            color: '30ce12'
                        },
                        {
                            level: 7,
                            from: 7,
                            to: 8,
                            color: '20a70a'
                        },
                        {
                            level: 8,
                            from: 8,
                            to: 9,
                            color: '128000'
                        }
                    ]
                },
                {
                    name: 'price-rent-1',
                    type: 'price-rent',
                    palette: [
                        {
                            level: 1,
                            from: 128478,
                            to: 73416,
                            color: '620801'
                        },
                        {
                            level: 2,
                            from: 73416,
                            to: 62358,
                            color: 'ee4613'
                        },
                        {
                            level: 3,
                            from: 62358,
                            to: 53751,
                            color: 'f87c19'
                        },
                        {
                            level: 4,
                            from: 53751,
                            to: 47500,
                            color: 'fbae1e'
                        },
                        {
                            level: 5,
                            from: 47500,
                            to: 42541,
                            color: 'ffe424'
                        },
                        {
                            level: 6,
                            from: 42541,
                            to: 38380,
                            color: '9ada1b'
                        },
                        {
                            level: 7,
                            from: 38380,
                            to: 34542,
                            color: '30ce12'
                        },
                        {
                            level: 8,
                            from: 34542,
                            to: 29184,
                            color: '20a70a'
                        },
                        {
                            level: 9,
                            from: 29184,
                            to: 10374,
                            color: '128000'
                        }
                    ]
                },
                {
                    name: 'price-sell-1',
                    type: 'price-sell',
                    palette: [
                        {
                            level: 1,
                            from: 1858678,
                            to: 538026,
                            color: '620801'
                        },
                        {
                            level: 2,
                            from: 538026,
                            to: 411297,
                            color: 'ee4613'
                        },
                        {
                            level: 3,
                            from: 411297,
                            to: 347648,
                            color: 'f87c19'
                        },
                        {
                            level: 4,
                            from: 347648,
                            to: 308167,
                            color: 'fbae1e'
                        },
                        {
                            level: 5,
                            from: 308167,
                            to: 274577,
                            color: 'ffe424'
                        },
                        {
                            level: 6,
                            from: 274577,
                            to: 240300,
                            color: '9ada1b'
                        },
                        {
                            level: 7,
                            from: 240300,
                            to: 208315,
                            color: '30ce12'
                        },
                        {
                            level: 8,
                            from: 208315,
                            to: 139831,
                            color: '20a70a'
                        },
                        {
                            level: 9,
                            from: 139831,
                            to: 18182,
                            color: '128000'
                        }
                    ]
                },
                {
                    name: 'profitability-1',
                    type: 'profitability',
                    palette: [
                        {
                            level: 1,
                            from: 82,
                            to: 33,
                            color: '620801'
                        },
                        {
                            level: 2,
                            from: 33,
                            to: 29,
                            color: 'ee4613'
                        },
                        {
                            level: 3,
                            from: 29,
                            to: 27,
                            color: 'f87c19'
                        },
                        {
                            level: 4,
                            from: 27,
                            to: 26,
                            color: 'fbae1e'
                        },
                        {
                            level: 5,
                            from: 26,
                            to: 25,
                            color: 'ffe424'
                        },
                        {
                            level: 6,
                            from: 25,
                            to: 23,
                            color: '9ada1b'
                        },
                        {
                            level: 7,
                            from: 23,
                            to: 22,
                            color: '30ce12'
                        },
                        {
                            level: 8,
                            from: 22,
                            to: 20,
                            color: '20a70a'
                        },
                        {
                            level: 9,
                            from: 20,
                            to: 5,
                            color: '128000'
                        }
                    ]
                },
                {
                    name: 'transport-1',
                    type: 'transport',
                    palette: [
                        {
                            level: 1,
                            from: 1,
                            to: 2,
                            color: '620801'
                        },
                        {
                            level: 2,
                            from: 2,
                            to: 3,
                            color: 'ee4613'
                        },
                        {
                            level: 3,
                            from: 3,
                            to: 4,
                            color: 'f87c19'
                        },
                        {
                            level: 4,
                            from: 4,
                            to: 5,
                            color: 'fbae1e'
                        },
                        {
                            level: 5,
                            from: 5,
                            to: 6,
                            color: 'ffe424'
                        },
                        {
                            level: 6,
                            from: 6,
                            to: 7,
                            color: '9ada1b'
                        },
                        {
                            level: 7,
                            from: 7,
                            to: 8,
                            color: '30ce12'
                        },
                        {
                            level: 8,
                            from: 8,
                            to: 9,
                            color: '20a70a'
                        },
                        {
                            level: 9,
                            from: 9,
                            to: 10,
                            color: '128000'
                        }
                    ]
                },
                {
                    name: 'carsharing-1',
                    type: 'carsharing',
                    palette: [
                        {
                            level: 0,
                            from: 0,
                            to: 1,
                            color: '620801'
                        },
                        {
                            level: 1,
                            from: 1,
                            to: 2,
                            color: 'ee4613'
                        },
                        {
                            level: 2,
                            from: 2,
                            to: 3,
                            color: 'f87c19'
                        },
                        {
                            level: 3,
                            from: 3,
                            to: 4,
                            color: 'fbae1e'
                        },
                        {
                            level: 4,
                            from: 4,
                            to: 5,
                            color: 'ffe424'
                        },
                        {
                            level: 5,
                            from: 5,
                            to: 6,
                            color: '9ada1b'
                        },
                        {
                            level: 6,
                            from: 6,
                            to: 7,
                            color: '30ce12'
                        },
                        {
                            level: 7,
                            from: 7,
                            to: 8,
                            color: '20a70a'
                        },
                        {
                            level: 8,
                            from: 8,
                            to: 9,
                            color: '128000'
                        }
                    ]
                }
            ],
            hasConcierge: true,
            hasCommercialBuildings: true,
            hasMetro: true,
            hasSites: true,
            hasVillages: true,
            hasPik: true,
            hasYandexRent: true,
            showMap: true,
            schoolInfo: {
                total: 399,
                highRatingColor: 'c847ff',
                lowRatingColor: '36107c'
            },
            isInOrlovskObl: false,
            isInPrimorskiyKrai: false,
            isInHabarovskKrai: false,
            isInTomskObl: false,
            isInKurganskObl: false,
            isInKirovskObl: false,
            isInUlyanovskObl: false,
            isInVologodskObl: false,
            isInPskovskObl: false,
            isInKalinigradskObl: false,
            isInIvanovskObl: false,
            isInChuvashskiya: false,
            isInUdmurtiya: false,
            isInSmolenskObl: false,
            isInKemerovskObl: false,
            isInTambovskObl: false,
            isInOmskObl: false,
            isInVladimirskObl: false,
            isInAltayskKrai: false,
            isInOrenburgObl: false,
            isInIrkutskObl: false,
            isInBashkortostan: false,
            isInStavropolskKrai: false,
            isInYaroslavskObl: false,
            isInVolgogradskObl: false,
            isInSevastopol: false,
            isInCrimea: false,
            isInBryanskObl: false,
            isInRyazanObl: false,
            isInLipetskObl: false,
            isInTverObl: false,
            isInTulaObl: false,
            isInTatarstan: false,
            isInKurskObl: false,
            isInKrasnoyarskKrai: false,
            isInTyumenObl: false,
            isInSaratovObl: false,
            isInChelyabinskObl: false,
            isInNovosibirskObl: false,
            isInSamaraObl: false,
            isInVoronezhObl: false,
            isInNizhnyNovgorodObl: false,
            isInPermKrai: false,
            isInKalugaObl: false,
            isInYaroslavlObl: false,
            isInAdygeya: false,
            isInSverdObl: false,
            isInRostovObl: false,
            isInBelgorodskObl: false,
            isInPenzenskObl: false,
            isInArhangelskObl: false,
            isInMO: true,
            isInLO: false,
            isInKK: false,
            isObninsk: false,
            isRostovOnDon: false,
            isMsk: false,
            isSpb: false,
            rgidVoronezh: 569171,
            rgidVoronezhObl: 475531,
            rgidSverdObl: 326698,
            rgidRostovObl: 211571,
            rgidKK: 353118,
            rgidMsk: 587795,
            rgidMO: 741964,
            rgidLO: 741965,
            rgidSpb: 417899,
            rgidTatarstan: 426660,
            rgidNizhnyNovgorodObl: 426764
        },
        user: {
            crc: 'y2fd7659d4c9b3ba8010cf1b0a5a3bf21',
            uid: '',
            yuid: '393167231637669633',
            isVosUser: false,
            isAuth: false,
            isJuridical: false,
            paymentTypeSuffix: 'natural',
            promoSubscription: {},
            avatarHost: 'avatars.mdst.yandex.net',
            emails: [],
            emailHash: '',
            defaultPhone: null,
            passHost: 'https://pass-test.yandex.ru',
            passportHost: 'https://passport-test.yandex.ru',
            passportApiHost: 'https://api.passport-test.yandex.ru',
            passportOrigin: 'realty_moscow',
            passportPhones: [],
            favorites: [],
            favoritesMap: {},
            comparison: [],
            statistics: {},
            walletInfo: {
                isFilled: false
            }
        },
        params: {
            'mortgage-calculator': [
                'rgid',
                'sublocality',
                'propertyCost',
                'downPaymentSum',
                'periodYears',
                'rate'
            ],
            geo: [
                'rgid',
                'sublocality'
            ],
            seo: [
                'rgid',
                'sublocality'
            ],
            'site-special-projects': [
                'rgid',
                'sublocality'
            ]
        },
        mortgagePrograms: {
            items: [],
            pager: {
                totalItems: 0,
                page: 0,
                pageSize: 20
            },
            isMoreLoading: false,
            queryId: 'a01a5361ffbd33643304e1914534704c',
            calculatorLimits: {
                minCreditAmount: 1000000,
                maxCreditAmount: 300000000,
                minPropertyCost: 1176471,
                maxPropertyCost: 200000000,
                minPeriodYears: 15,
                maxPeriodYears: 50,
                minDownPayment: 15,
                minDownPaymentSum: 176471,
                maxDownPayment: 100,
                maxDownPaymentSum: 200000000,
                minRate: 2,
                maxRate: 20
            },
            defaultCalculatorLimits: {
                minCreditAmount: 1000000,
                maxCreditAmount: 300000000,
                minPropertyCost: 1176471,
                maxPropertyCost: 200000000,
                minPeriodYears: 15,
                maxPeriodYears: 50,
                minDownPayment: 15,
                minDownPaymentSum: 176471,
                maxDownPayment: 100,
                maxDownPaymentSum: 200000000,
                minRate: 2,
                maxRate: 20
            },
            searchQuery: {
                regionId: [
                    1
                ],
                flatType: [
                    'NEW_FLAT'
                ],
                propertyCost: 10000000,
                downPayment: 35,
                downPaymentSum: 3500000,
                periodYears: 26,
                sort: 'BANK_PRIORITY'
            },
            defaultSearchQuery: {
                propertyCost: 10000000,
                downPayment: 35,
                downPaymentSum: 3500000,
                periodYears: 30
            },
            banks: [],
            filtersCount: 0,
            isFiltersCountLoading: false,
            getSnippetsStatus: 'LOADED',
            bankCount: 0,
            minRate: 4.39,
            isLoading: false
        },
        mortgageProgram: {
            similarPrograms: [],
            calculateMortgageStatus: 'LOADED',
            otherPrograms: [],
            getSnippetsStatus: 'LOADED'
        },
        mortgageBank: {},
        mortgageCalculator: {
            calculator: {
                creditAmount: 7140000,
                monthlyPayment: 67302,
                monthlyPaymentParams: {
                    rate: 8.3,
                    propertyCost: 34810000,
                    periodYears: 16,
                    downPayment: 79,
                    downPaymentSum: 27670000
                },
                calculatorLimits: {
                    minCreditAmount: 1000000,
                    maxCreditAmount: 300000000,
                    minPropertyCost: 1176471,
                    maxPropertyCost: 200000000,
                    minPeriodYears: 15,
                    maxPeriodYears: 50,
                    minDownPayment: 15,
                    minDownPaymentSum: 176471,
                    maxDownPayment: 100,
                    maxDownPaymentSum: 200000000,
                    minRate: 2,
                    maxRate: 20
                },
                overpayment: '5781984',
                queryId: '3f9129af8e9458f279a6a402bdec828d'
            },
            isMortgageProgramsLoading: false,
            isMortgageProgramsError: false,
            isMoreMortgageProgramsLoading: false,
            offersRequestStatus: 'LOADED',
            programs: {
                items: [],
                pager: {
                    totalItems: 0,
                    page: 0,
                    pageSize: 20
                },
                calculatorLimits: {
                    minCreditAmount: 1000000,
                    maxCreditAmount: 300000000,
                    minPropertyCost: 1176471,
                    maxPropertyCost: 200000000,
                    minPeriodYears: 15,
                    maxPeriodYears: 50,
                    minDownPayment: 15,
                    minDownPaymentSum: 176471,
                    maxDownPayment: 100,
                    maxDownPaymentSum: 200000000,
                    minRate: 2,
                    maxRate: 20
                },
                defaultCalculatorLimits: {
                    minCreditAmount: 1000000,
                    maxCreditAmount: 300000000,
                    minPropertyCost: 1176471,
                    maxPropertyCost: 200000000,
                    minPeriodYears: 15,
                    maxPeriodYears: 50,
                    minDownPayment: 15,
                    minDownPaymentSum: 176471,
                    maxDownPayment: 100,
                    maxDownPaymentSum: 200000000,
                    minRate: 2,
                    maxRate: 20
                },
                searchQuery: {
                    regionId: [
                        1
                    ],
                    propertyCost: 34810000,
                    downPayment: 79,
                    downPaymentSum: 27670000,
                    periodYears: 16,
                    maxRate: 8.3,
                    sort: 'BANK_PRIORITY'
                },
                defaultSearchQuery: {
                    propertyCost: 10000000,
                    downPayment: 35,
                    downPaymentSum: 3500000,
                    periodYears: 30
                },
                fallback: false,
                bankCount: 0,
                minRate: 3.5
            }
        },
        mortgageApplicationForm: {
            applicationFromScreen: 'APPLICATION',
            applicationCreationStatus: 'LOADED',
            applicationConfirmationStatus: 'LOADED'
        }
    }
};
