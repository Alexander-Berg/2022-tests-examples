const router = require('../desktop');

// Search Engine Friendly URL - ЧПУ

const test = (route, testCases) => {
    testCases.forEach(({ defaultParams, title, testCases }) => {
        testCases.forEach(({ testTitle, testParams, result }) => {
            it(`Должен правильно построить ЧПУ ${title} ${testTitle}`, function() {
                expect(route.build({
                    ...defaultParams,
                    ...testParams,
                })).toBe(result);
            });

            it(`Должен правильно распарсить URL ${title} ${testTitle}`, function() {
                expect(route.match(result)).toEqual({
                    ...defaultParams,
                    ...testParams,
                });
            });
        });
    });
};

describe(' Валидация ЧПУ для года сдачи новостроек', function() {
    const route = router.getRouteByName('newbuilding-search');

    const VALID_YEARS = [ '2022', '2023', '2024' ];
    const INVALID_YEARS = [ '3_2022', '4_2025' ];

    VALID_YEARS.forEach(year => {
        const testCase = [
            {
                defaultParams: {
                    type: 'SELL',
                    deliveryDate: `4_${year}`,
                    rgid: 587795,
                },
                title: `${year} год сдачи`,
                testCases: [
                    {
                        testTitle: 'с гео',
                        result: `/moskva/kupit/novostrojka/sdacha-${year}/`,
                        testParams: {}
                    }
                ]
            }
        ];

        test(route, testCase);
    });

    INVALID_YEARS.forEach(year => {
        const testCase = [
            {
                defaultParams: {
                    type: 'SELL',
                    deliveryDate: year,
                    rgid: 587795,
                },
                title: `для ${year} значения deliveryDate`,
                testCases: [
                    {
                        testTitle: 'с гео',
                        result: `/moskva/kupit/novostrojka/?deliveryDate=${year}`,
                        testParams: {}
                    }
                ]
            }
        ];

        test(route, testCase);
    });
});

describe('Валидация ЧПУ для продажи квартир ценовой диапазон', function() {
    const route = router.getRouteByName('search');

    const priceRanges = [
        '800000',
        '500000',
        '1000000',
        '1500000',
        '2000000',
        '2500000',
        '3000000',
        '3500000',
        '4000000',
        '5000000',
        '6000000'
    ];

    priceRanges.forEach(price => {
        const testCase = [
            {
                defaultParams: {
                    type: 'SELL',
                    category: 'APARTMENT',
                    priceMax: price,
                    rgid: 587795,
                },
                title: `по цене до ${price} рублей`,
                testCases: [
                    {
                        testTitle: 'с гео',
                        result: `/moskva/kupit/kvartira/do-${price}/`,
                        testParams: {},
                    },
                    {
                        testTitle: 'со вторым выбранным фильтром разбивает на get параметры',
                        result: `/moskva/kupit/kvartira/novostroyki-i-do-${price}/`,
                        testParams: {
                            newFlat: 'YES',
                        },
                    },
                    {
                        testTitle: 'c комнатностью',
                        result: `/moskva/kupit/kvartira/odnokomnatnaya/do-${price}/`,
                        testParams: {
                            roomsTotal: '1',
                        },
                    },
                    {
                        testTitle: 'c метро',
                        result: `/moskva/kupit/kvartira/metro-teatralnaya/do-${price}/`,
                        testParams: {
                            metroGeoId: '20473'
                        },
                    },
                    {
                        testTitle: 'c улицей',
                        result: `/moskva/kupit/kvartira/st-ulica-ohotnyj-ryad-20945/do-${price}/`,
                        testParams: {
                            streetId: '20945',
                            streetName: 'ulica-ohotnyj-ryad'
                        },
                    },
                    {
                        testTitle: 'с балконом (или любым другим кроме newFlat)',
                        result: `/moskva/kupit/kvartira/?priceMax=${price}&balcony=BALCONY`,
                        testParams: {
                            balcony: 'BALCONY'
                        }
                    }
                ]
            },
        ];

        test(route, testCase);
    });
});

describe('Валидация ЧПУ для аренды квартир', function() {
    const route = router.getRouteByName('search');

    const priceRanges = [
        '5000',
        '10000',
        '15000',
        '8000',
        '20000',
    ];

    priceRanges.forEach(price => {
        const testCase = [
            {
                defaultParams: {
                    type: 'RENT',
                    category: 'APARTMENT',
                    priceMax: price,
                    rgid: 587795,
                },
                title: `по цене до ${price} рублей`,
                testCases: [
                    {
                        testTitle: 'с гео',
                        result: `/moskva/snyat/kvartira/do-${price}/`,
                        testParams: {},
                    },
                    {
                        testTitle: 'c комнатностью',
                        result: `/moskva/snyat/kvartira/odnokomnatnaya/do-${price}/`,
                        testParams: {
                            roomsTotal: '1',
                        },
                    },
                    {
                        testTitle: 'c метро',
                        result: `/moskva/snyat/kvartira/metro-teatralnaya/do-${price}/`,
                        testParams: {
                            metroGeoId: '20473'
                        },
                    },
                    {
                        testTitle: 'c улицей',
                        result: `/moskva/snyat/kvartira/st-ulica-ohotnyj-ryad-20945/do-${price}/`,
                        testParams: {
                            streetId: '20945',
                            streetName: 'ulica-ohotnyj-ryad'

                        },
                    },
                ]
            },
        ];

        test(route, testCase);
    });
});

describe('Валидация ЧПУ для продажи домов', function() {
    const route = router.getRouteByName('search');

    const testCases = [
        {
            defaultParams: {
                type: 'SELL',
                category: 'HOUSE',
                buildingType: 'BRICK',
            },
            title: 'для кирпичных домов',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/dom/kirpichnye/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/dom/kirpichnye/',
                    testParams: {
                        rgid: 417899,
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/dom/?buildingType=BRICK',
                    testParams: {
                        rgid: 31412,
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/dom/st-ulica-hachaturyana-123/kirpichnye/',
                    testParams: {
                        rgid: 587795,
                        streetId: '123',
                        streetName: 'ulica-hachaturyana',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'HOUSE',
                buildingType: 'WOOD',
            },
            title: 'для деревянных домов',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/dom/derevyannye/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/dom/derevyannye/',
                    testParams: {
                        rgid: 417899,
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/dom/?buildingType=WOOD',
                    testParams: {
                        rgid: 31412,
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/dom/st-ulica-hachaturyana-123/derevyannye/',
                    testParams: {
                        rgid: 587795,
                        streetId: '123',
                        streetName: 'ulica-hachaturyana',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'HOUSE',
                maxFloors: 1,
            },
            title: 'для одноэтажных домов',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/dom/odnoetazhnye/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/dom/odnoetazhnye/',
                    testParams: {
                        rgid: 417899,
                    },
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/dom/st-ulica-hachaturyana-123/odnoetazhnye/',
                    testParams: {
                        rgid: 587795,
                        streetId: '123',
                        streetName: 'ulica-hachaturyana',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'HOUSE',
                maxFloors: 2,
            },
            title: 'для двухэтажных домов',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/dom/dvuhetazhnye/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/dom/dvuhetazhnye/',
                    testParams: {
                        rgid: 417899,
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/dom/?maxFloors=2',
                    testParams: {
                        rgid: 31412,
                        maxFloors: '2',
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/dom/st-ulica-hachaturyana-123/dvuhetazhnye/',
                    testParams: {
                        rgid: 587795,
                        streetId: '123',
                        streetName: 'ulica-hachaturyana',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'HOUSE',
                minFloors: 3,
            },
            title: 'для трехэтажных домов',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/dom/trehetazhye/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/dom/trehetazhye/',
                    testParams: {
                        rgid: 417899,
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/dom/?minFloors=3',
                    testParams: {
                        rgid: 31412,
                        minFloors: '3',
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/dom/st-ulica-hachaturyana-123/trehetazhye/',
                    testParams: {
                        rgid: 587795,
                        streetId: '123',
                        streetName: 'ulica-hachaturyana',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'HOUSE',
                hasFurniture: 'NO',
            },
            title: 'для домов без мебели',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/dom/bez-mebeli/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/dom/bez-mebeli/',
                    testParams: {
                        rgid: 417899,
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/dom/?hasFurniture=NO',
                    testParams: {
                        rgid: 31412,
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/dom/st-ulica-hachaturyana-123/bez-mebeli/',
                    testParams: {
                        rgid: 587795,
                        streetId: '123',
                        streetName: 'ulica-hachaturyana',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'HOUSE',
                hasFurniture: 'YES',
            },
            title: 'для домов c мебелью',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/dom/s-mebeliu/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/dom/s-mebeliu/',
                    testParams: {
                        rgid: 417899,
                    },
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/dom/st-ulica-hachaturyana-123/s-mebeliu/',
                    testParams: {
                        rgid: 587795,
                        streetId: '123',
                        streetName: 'ulica-hachaturyana',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'HOUSE',
                includeTag: [ '1794368' ],
                minFloors: 3
            },
            title: 'для трехэтажных домов с бассейном',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/dom/trehetazhye-s-basseinom/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/dom/trehetazhye-s-basseinom/',
                    testParams: {
                        rgid: 417899,
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/dom/?includeTag=1794368&minFloors=3',
                    testParams: {
                        includeTag: '1794368',
                        minFloors: '3',
                        rgid: 31412,
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/dom/st-ulica-hachaturyana-123/trehetazhye-s-basseinom/',
                    testParams: {
                        rgid: 587795,
                        streetId: '123',
                        streetName: 'ulica-hachaturyana',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'HOUSE',
                includeTag: [ '1794390' ],
                minFloors: 2
            },
            title: 'для двухэтажных домов с гаражом',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/dom/dvuhetazhnye-s-garazhom/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/dom/dvuhetazhnye-s-garazhom/',
                    testParams: {
                        rgid: 417899,
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/dom/?includeTag=1794390&minFloors=2',
                    testParams: {
                        includeTag: '1794390',
                        minFloors: '2',
                        rgid: 31412,
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/dom/st-ulica-hachaturyana-123/dvuhetazhnye-s-garazhom/',
                    testParams: {
                        rgid: 587795,
                        streetId: '123',
                        streetName: 'ulica-hachaturyana',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'HOUSE',
                minFloors: 2
            },
            title: 'для многоэтажных домов',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/dom/mnogoetaznye/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/dom/mnogoetaznye/',
                    testParams: {
                        rgid: 417899,
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/dom/?minFloors=2',
                    testParams: {
                        minFloors: '2',
                        rgid: 31412,
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/dom/st-ulica-hachaturyana-123/mnogoetaznye/',
                    testParams: {
                        rgid: 587795,
                        streetId: '123',
                        streetName: 'ulica-hachaturyana',
                    }
                }
            ]
        },
    ];

    test(route, testCases);
});

describe('Валидация ЧПУ для продажи квартир', function() {
    const route = router.getRouteByName('search');

    const testCases = [
        {
            defaultParams: {
                type: 'SELL',
                category: 'APARTMENT',
                dealStatus: 'COUNTERSALE'
            },
            title: 'с возможностью обмена',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/kvartira/s-obmenom/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/kvartira/s-obmenom/',
                    testParams: {
                        rgid: 417899,
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/kvartira/?dealStatus=COUNTERSALE',
                    testParams: {
                        rgid: 31412,
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/kvartira/st-ulica-hachaturyana-123/s-obmenom/',
                    testParams: {
                        rgid: 587795,
                        streetId: '123',
                        streetName: 'ulica-hachaturyana',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'APARTMENT',
                dealStatus: 'REASSIGNMENT'
            },
            title: 'с возможностью переуступки',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/kvartira/s-pereustupkoy/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/kvartira/s-pereustupkoy/',
                    testParams: {
                        rgid: 417899,
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/kvartira/?dealStatus=REASSIGNMENT',
                    testParams: {
                        rgid: 31412,
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/kvartira/st-ulica-hachaturyana-123/s-pereustupkoy/',
                    testParams: {
                        rgid: 587795,
                        streetId: '123',
                        streetName: 'ulica-hachaturyana',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'APARTMENT',
                buildingSeriesId: 1564792
            },
            title: 'для квартиры в домах 137 серии',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/kvartira/seriya-137/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/kvartira/seriya-137/',
                    testParams: {
                        rgid: 417899,
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/kvartira/?buildingSeriesId=1564792',
                    testParams: {
                        rgid: 31412,
                        buildingSeriesId: '1564792'
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/kvartira/st-ulica-hachaturyana-123/seriya-137/',
                    testParams: {
                        rgid: 587795,
                        streetId: '123',
                        streetName: 'ulica-hachaturyana',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'APARTMENT',
                buildingSeriesId: 1564801
            },
            title: 'для квартиры в домах 504 серии',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/kvartira/seriya-504/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/kvartira/seriya-504/',
                    testParams: {
                        rgid: 417899,
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/kvartira/?buildingSeriesId=1564801',
                    testParams: {
                        rgid: 31412,
                        buildingSeriesId: '1564801'
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/kvartira/st-ulica-hachaturyana-504/seriya-504/',
                    testParams: {
                        rgid: 587795,
                        streetId: '504',
                        streetName: 'ulica-hachaturyana',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'APARTMENT',
                buildingSeriesId: 1568621
            },
            title: 'для квартиры в домах Башня Вулха',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/kvartira/seriya-bashnya-vulha/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/kvartira/seriya-bashnya-vulha/',
                    testParams: {
                        rgid: 417899,
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/kvartira/?buildingSeriesId=1568621',
                    testParams: {
                        rgid: 31412,
                        buildingSeriesId: '1568621'
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/kvartira/st-ulica-hachaturyana-504/seriya-bashnya-vulha/',
                    testParams: {
                        rgid: 587795,
                        streetId: '504',
                        streetName: 'ulica-hachaturyana',
                    }
                },
                {
                    testTitle: 'с метро',
                    result: '/moskva/kupit/kvartira/metro-chkalovskaya-1/seriya-bashnya-vulha/',
                    testParams: {
                        rgid: 587795,
                        metroGeoId: '20515',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'APARTMENT',
                buildingSeriesId: 663311
            },
            title: 'для квартиры в домах П44К серии',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/kvartira/seriya-p44k/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/kvartira/seriya-p44k/',
                    testParams: {
                        rgid: 417899,
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/kvartira/?buildingSeriesId=663311',
                    testParams: {
                        rgid: 31412,
                        buildingSeriesId: '663311'
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/kvartira/st-ulica-hachaturyana-504/seriya-p44k/',
                    testParams: {
                        rgid: 587795,
                        streetId: '504',
                        streetName: 'ulica-hachaturyana',
                    }
                },
                {
                    testTitle: 'с метро',
                    result: '/moskva/kupit/kvartira/metro-chkalovskaya-1/seriya-p44k/',
                    testParams: {
                        rgid: 587795,
                        metroGeoId: '20515',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'APARTMENT',
                buildingSeriesId: 663302
            },
            title: 'для квартиры в домах П44 серии',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/kvartira/seriya-p44/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/kvartira/seriya-p44/',
                    testParams: {
                        rgid: 417899,
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/kvartira/?buildingSeriesId=663302',
                    testParams: {
                        rgid: 31412,
                        buildingSeriesId: '663302'
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/kvartira/st-ulica-hachaturyana-504/seriya-p44/',
                    testParams: {
                        rgid: 587795,
                        streetId: '504',
                        streetName: 'ulica-hachaturyana',
                    }
                },
                {
                    testTitle: 'с метро',
                    result: '/moskva/kupit/kvartira/metro-chkalovskaya-1/seriya-p44/',
                    testParams: {
                        rgid: 587795,
                        metroGeoId: '20515',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'APARTMENT',
                buildingSeriesId: 712104
            },
            title: 'для квартиры в домах П46 серии',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/kvartira/seriya-p46/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/kvartira/seriya-p46/',
                    testParams: {
                        rgid: 417899,
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/kvartira/?buildingSeriesId=712104',
                    testParams: {
                        rgid: 31412,
                        buildingSeriesId: '712104'
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/kvartira/st-ulica-hachaturyana-504/seriya-p46/',
                    testParams: {
                        rgid: 587795,
                        streetId: '504',
                        streetName: 'ulica-hachaturyana',
                    }
                },
                {
                    testTitle: 'с метро',
                    result: '/moskva/kupit/kvartira/metro-chkalovskaya-1/seriya-p46/',
                    testParams: {
                        rgid: 587795,
                        metroGeoId: '20515',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'APARTMENT',
                buildingSeriesId: 712117
            },
            title: 'для квартиры в домах П47 серии',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/kvartira/seriya-p47/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/kvartira/seriya-p47/',
                    testParams: {
                        rgid: 417899
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/kvartira/?buildingSeriesId=712117',
                    testParams: {
                        rgid: 31412,
                        buildingSeriesId: '712117'
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/kvartira/st-ulica-hachaturyana-504/seriya-p47/',
                    testParams: {
                        rgid: 587795,
                        streetId: '504',
                        streetName: 'ulica-hachaturyana',
                    }
                },
                {
                    testTitle: 'с метро',
                    result: '/moskva/kupit/kvartira/metro-chkalovskaya-1/seriya-p47/',
                    testParams: {
                        rgid: 587795,
                        metroGeoId: '20515',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'APARTMENT',
                includeTag: [ '1700320' ],
            },
            title: 'для квартиры в двухуровневых домах',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/kvartira/dvuhurovnevye/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/kvartira/dvuhurovnevye/',
                    testParams: {
                        rgid: 417899
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/kvartira/?includeTag=1700320',
                    testParams: {
                        rgid: 31412,
                        includeTag: '1700320'
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/kvartira/st-ulica-hachaturyana-504/dvuhurovnevye/',
                    testParams: {
                        rgid: 587795,
                        streetId: '504',
                        streetName: 'ulica-hachaturyana',
                    }
                },
                {
                    testTitle: 'с метро',
                    result: '/moskva/kupit/kvartira/metro-chkalovskaya-1/dvuhurovnevye/',
                    testParams: {
                        rgid: 587795,
                        metroGeoId: '20515',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'APARTMENT',
                includeTag: [ '1794567' ],
            },
            title: 'для квартиры пентхауса',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/kvartira/penthouse/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/kvartira/penthouse/',
                    testParams: {
                        rgid: 417899
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/kvartira/?includeTag=1794567',
                    testParams: {
                        rgid: 31412,
                        includeTag: '1794567'
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/kvartira/st-ulica-hachaturyana-504/penthouse/',
                    testParams: {
                        rgid: 587795,
                        streetId: '504',
                        streetName: 'ulica-hachaturyana',
                    }
                },
                {
                    testTitle: 'с метро',
                    result: '/moskva/kupit/kvartira/metro-chkalovskaya-1/penthouse/',
                    testParams: {
                        rgid: 587795,
                        metroGeoId: '20515',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'APARTMENT',
                maxFloors: 5
            },
            title: 'для пятиэтажки',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/kvartira/v-pyatietazhnom-dome/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/kvartira/v-pyatietazhnom-dome/',
                    testParams: {
                        rgid: 417899
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/kvartira/?maxFloors=5',
                    testParams: {
                        rgid: 31412,
                        maxFloors: '5',
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/kvartira/st-ulica-hachaturyana-504/v-pyatietazhnom-dome/',
                    testParams: {
                        rgid: 587795,
                        streetId: '504',
                        streetName: 'ulica-hachaturyana',
                    }
                },
                {
                    testTitle: 'с метро',
                    result: '/moskva/kupit/kvartira/metro-chkalovskaya-1/v-pyatietazhnom-dome/',
                    testParams: {
                        rgid: 587795,
                        metroGeoId: '20515',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'APARTMENT',
                maxFloors: 3
            },
            title: 'для малоэтажного дома',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/kvartira/v-maloetazhnom-dome/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/kvartira/v-maloetazhnom-dome/',
                    testParams: {
                        rgid: 417899
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/kvartira/?maxFloors=3',
                    testParams: {
                        rgid: 31412,
                        maxFloors: '3',
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/kvartira/st-ulica-hachaturyana-504/v-maloetazhnom-dome/',
                    testParams: {
                        rgid: 587795,
                        streetId: '504',
                        streetName: 'ulica-hachaturyana',
                    }
                },
                {
                    testTitle: 'с метро',
                    result: '/moskva/kupit/kvartira/metro-chkalovskaya-1/v-maloetazhnom-dome/',
                    testParams: {
                        rgid: 587795,
                        metroGeoId: '20515',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'APARTMENT',
                minFloors: 20
            },
            title: 'для высотки',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/kvartira/v-vysotke/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/kvartira/v-vysotke/',
                    testParams: {
                        rgid: 417899
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/kvartira/?minFloors=20',
                    testParams: {
                        rgid: 31412,
                        minFloors: '20',
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/kvartira/st-ulica-hachaturyana-504/v-vysotke/',
                    testParams: {
                        rgid: 587795,
                        streetId: '504',
                        streetName: 'ulica-hachaturyana',
                    }
                },
                {
                    testTitle: 'с метро',
                    result: '/moskva/kupit/kvartira/metro-chkalovskaya-1/v-vysotke/',
                    testParams: {
                        rgid: 587795,
                        metroGeoId: '20515',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'APARTMENT',
                includeTag: [ '1700312', '1794463' ]
            },
            title: 'для лофта',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/kvartira/loft/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/kvartira/loft/',
                    testParams: {
                        rgid: 417899
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/kvartira/?includeTag=1700312&includeTag=1794463',
                    testParams: {
                        rgid: 31412,

                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/kvartira/st-ulica-hachaturyana-504/loft/',
                    testParams: {
                        rgid: 587795,
                        streetId: '504',
                        streetName: 'ulica-hachaturyana',
                    }
                },
                {
                    testTitle: 'с метро',
                    result: '/moskva/kupit/kvartira/metro-chkalovskaya-1/loft/',
                    testParams: {
                        rgid: 587795,
                        metroGeoId: '20515',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'APARTMENT',
                includeTag: [ '1794377' ],
            },
            title: 'для квартиры с панорамными окнами',
            testCases: [
                {
                    testTitle: 'для Москвы и МО',
                    result: '/moskva_i_moskovskaya_oblast/kupit/kvartira/s-panoramnymi-oknami/',
                    testParams: {
                        rgid: 741964,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга и ЛО',
                    result: '/sankt-peterburg_i_leningradskaya_oblast/kupit/kvartira/s-panoramnymi-oknami/',
                    testParams: {
                        rgid: 741965
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме столиц областей и миллионников',
                    result: '/grafskiy_2-y/kupit/kvartira/?includeTag=1794377',
                    testParams: {
                        rgid: 31412,
                        includeTag: '1794377',
                    }
                },
                {
                    testTitle: 'с улицей',
                    result:
                        '/moskva_i_moskovskaya_oblast/kupit/kvartira/st-ulica-hachaturyana-504/s-panoramnymi-oknami/',
                    testParams: {
                        rgid: 741964,
                        streetId: '504',
                        streetName: 'ulica-hachaturyana',
                    }
                },
                {
                    testTitle: 'с метро',
                    result: '/moskva_i_moskovskaya_oblast/kupit/kvartira/metro-chkalovskaya-1/s-panoramnymi-oknami/',
                    testParams: {
                        rgid: 741964,
                        metroGeoId: '20515',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'APARTMENT',
                buildingEpoch: 'STALIN',
                minFloors: 20
            },
            title: 'для квартиры в высотных сталинских домах',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/kvartira/v-vysotke-stalinke/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/kvartira/v-vysotke-stalinke/',
                    testParams: {
                        rgid: 417899
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/kvartira/?buildingEpoch=STALIN&minFloors=20',
                    testParams: {
                        rgid: 31412,
                        minFloors: '20',
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/kvartira/st-ulica-hachaturyana-504/v-vysotke-stalinke/',
                    testParams: {
                        rgid: 587795,
                        streetId: '504',
                        streetName: 'ulica-hachaturyana',
                    }
                },
                {
                    testTitle: 'с метро',
                    result: '/moskva/kupit/kvartira/metro-chkalovskaya-1/v-vysotke-stalinke/',
                    testParams: {
                        rgid: 587795,
                        metroGeoId: '20515',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'APARTMENT',
                roomsTotal: [
                    '1',
                    'STUDIO'
                ],
                includeTag: [ '1700312' ],
                newFlat: 'NO'
            },
            title: 'для лофта эконом класса',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/kvartira/loft-econom-klassa/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/kvartira/loft-econom-klassa/',
                    testParams: {
                        rgid: 417899
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/kvartira/studiya,1-komnatnie/?includeTag=1700312&newFlat=NO',
                    testParams: {
                        rgid: 31412,
                        includeTag: '1700312'
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/kvartira/st-ulica-hachaturyana-504/loft-econom-klassa/',
                    testParams: {
                        rgid: 587795,
                        streetId: '504',
                        streetName: 'ulica-hachaturyana',
                    }
                },
                {
                    testTitle: 'с метро',
                    result: '/moskva/kupit/kvartira/metro-chkalovskaya-1/loft-econom-klassa/',
                    testParams: {
                        rgid: 587795,
                        metroGeoId: '20515',
                    }
                }
            ]
        },
        {
            defaultParams: {
                type: 'SELL',
                category: 'APARTMENT',
                roomsTotal: 'STUDIO',
                areaMax: 13
            },
            title: 'для квартиры в путинке',
            testCases: [
                {
                    testTitle: 'для Москвы',
                    result: '/moskva/kupit/kvartira/v-putinke/',
                    testParams: {
                        rgid: 587795,
                    },
                },
                {
                    testTitle: 'для Санкт-Петербурга',
                    result: '/sankt-peterburg/kupit/kvartira/v-putinke/',
                    testParams: {
                        rgid: 417899
                    },
                },
                {
                    testTitle: '- не строим ЧПУ для других гео кроме Москвы и Санкт-Петербурга',
                    result: '/grafskiy_2-y/kupit/kvartira/studiya/?areaMax=13',
                    testParams: {
                        rgid: 31412,
                        areaMax: '13',
                    }
                },
                {
                    testTitle: 'с улицей',
                    result: '/moskva/kupit/kvartira/st-ulica-hachaturyana-504/v-putinke/',
                    testParams: {
                        rgid: 587795,
                        streetId: '504',
                        streetName: 'ulica-hachaturyana',
                    }
                },
                {
                    testTitle: 'с метро',
                    result: '/moskva/kupit/kvartira/metro-chkalovskaya-1/v-putinke/',
                    testParams: {
                        rgid: 587795,
                        metroGeoId: '20515',
                    }
                }
            ]
        },
    ];

    test(route, testCases);
});

describe('Валидация ЧПУ для коммерческих помещений =>', () => {
    const route = router.getRouteByName('search');
    const metroGeoId = '20473';
    const streetParams = {
        streetId: '20945',
        streetName: 'ulica-ohotnyj-ryad'
    };
    const testCases = [
        {
            defaultParams: {
                type: 'SELL',
                category: 'COMMERCIAL',
            },
            title: '',
            testCases: [
                {
                    testTitle: 'для бизнес центра',
                    result: '/moskva/kupit/kommercheskaya-nedvizhimost/biznes-center/',
                    testParams: {
                        rgid: 587795,
                        commercialBuildingType: 'BUSINESS_CENTER'
                    },
                },
                {
                    testTitle: 'для бизнес центра + улицы',
                    result: '/moskva/kupit/kommercheskaya-nedvizhimost/st-ulica-ohotnyj-ryad-20945/biznes-center/',
                    testParams: {
                        rgid: 587795,
                        commercialBuildingType: 'BUSINESS_CENTER',
                        ...streetParams
                    },
                },
                {
                    testTitle: 'для бизнес центра + метро',
                    result: '/moskva/kupit/kommercheskaya-nedvizhimost/metro-teatralnaya/biznes-center/',
                    testParams: {
                        rgid: 587795,
                        commercialBuildingType: 'BUSINESS_CENTER',
                        metroGeoId
                    },
                },
                {
                    testTitle: 'для торгового центра',
                    result: '/moskva/kupit/kommercheskaya-nedvizhimost/torgoviy-center/',
                    testParams: {
                        rgid: 587795,
                        commercialBuildingType: 'SHOPPING_CENTER'
                    },
                },
                {
                    testTitle: 'для торгового центра + улицы',
                    result: '/moskva/kupit/kommercheskaya-nedvizhimost/st-ulica-ohotnyj-ryad-20945/torgoviy-center/',
                    testParams: {
                        rgid: 587795,
                        commercialBuildingType: 'SHOPPING_CENTER',
                        ...streetParams
                    },
                },
                {
                    testTitle: 'для торгового центра + метро',
                    result: '/moskva/kupit/kommercheskaya-nedvizhimost/metro-teatralnaya/torgoviy-center/',
                    testParams: {
                        rgid: 587795,
                        commercialBuildingType: 'SHOPPING_CENTER',
                        metroGeoId
                    },
                },
                {
                    testTitle: 'для скалад',
                    result: '/moskva/kupit/kommercheskaya-nedvizhimost/sklad/',
                    testParams: {
                        rgid: 587795,
                        commercialBuildingType: 'WAREHOUSE'
                    },
                },
                {
                    testTitle: 'для склада + улицы',
                    result: '/moskva/kupit/kommercheskaya-nedvizhimost/st-ulica-ohotnyj-ryad-20945/sklad/',
                    testParams: {
                        rgid: 587795,
                        commercialBuildingType: 'WAREHOUSE',
                        ...streetParams
                    },
                },
                {
                    testTitle: 'для склада + метро',
                    result: '/moskva/kupit/kommercheskaya-nedvizhimost/metro-teatralnaya/sklad/',
                    testParams: {
                        rgid: 587795,
                        commercialBuildingType: 'WAREHOUSE',
                        metroGeoId
                    },
                },
                {
                    testTitle: 'для отдельно стоящего здания ',
                    result: '/moskva/kupit/kommercheskaya-nedvizhimost/zdaniye/',
                    testParams: {
                        rgid: 587795,
                        commercialBuildingType: 'DETACHED_BUILDING'
                    },
                },
                {
                    testTitle: 'для отдельно стоящего здания + улицы',
                    result: '/moskva/kupit/kommercheskaya-nedvizhimost/st-ulica-ohotnyj-ryad-20945/zdaniye/',
                    testParams: {
                        rgid: 587795,
                        commercialBuildingType: 'DETACHED_BUILDING',
                        ...streetParams
                    },
                },
                {
                    testTitle: 'для отдельно стоящего здания + метро',
                    result: '/moskva/kupit/kommercheskaya-nedvizhimost/metro-teatralnaya/zdaniye/',
                    testParams: {
                        rgid: 587795,
                        commercialBuildingType: 'DETACHED_BUILDING',
                        metroGeoId
                    },
                },
                {
                    testTitle: 'для жилого дома',
                    result: '/moskva/kupit/kommercheskaya-nedvizhimost/zhiloy-dom/',
                    testParams: {
                        rgid: 587795,
                        commercialBuildingType: 'RESIDENTIAL_BUILDING'
                    },
                },
                {
                    testTitle: 'для жилого дома + улицы',
                    result: '/moskva/kupit/kommercheskaya-nedvizhimost/st-ulica-ohotnyj-ryad-20945/zhiloy-dom/',
                    testParams: {
                        rgid: 587795,
                        commercialBuildingType: 'RESIDENTIAL_BUILDING',
                        ...streetParams
                    },
                },
                {
                    testTitle: 'для жилого дома+ метро',
                    result: '/moskva/kupit/kommercheskaya-nedvizhimost/metro-teatralnaya/zhiloy-dom/',
                    testParams: {
                        rgid: 587795,
                        commercialBuildingType: 'RESIDENTIAL_BUILDING',
                        metroGeoId
                    },
                },
            ]
        }
    ];

    test(route, testCases);
});
