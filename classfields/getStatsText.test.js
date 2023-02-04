const getStatsText = require('./getStatsText');

it('Должен сгенерировать текст, если есть только марка и модель', () => {
    const state = {
        breadcrumbs: {
            data: {
                FORD: {
                    'autoru-alias': 'ford',
                    id: 'FORD',
                    name: 'Ford',
                    count: 716,
                    'cyrillic-name': 'Форд',
                    popular: true,
                    reviews_count: 3824,
                },
            },
        },
        config: {
            data: {
                pageParams: {
                    mark: 'FORD',
                    model: 'ECOSPORT',
                    category: 'CARS',
                },
            },
        },
    };

    const snippet = getStatsText(state);

    expect(snippet).toEqual('Статистика цен на Ford по России.');
});

it('Должен сгенерировать текст, когда есть марка, модель и все нужные данные из priceStatsPublicApi', () => {
    const state = {
        priceStatsPublicApi: {
            data: {
                price: {
                    min_price: 505000,
                    average_price: 1085765,
                    offers_count: 270,
                },

                tech_params: {
                    data_source: 'MODEL',
                    most_popular_tech_param: {
                        mark: 'AUDI',
                        mark_name: 'Audi',
                        model: 'A4',
                        model_name: 'A4',
                        super_gen_id: '21460328',
                        super_gen_name: 'A4 B9',
                        configuration_id: '21596324',
                        tech_param_id: '21596327',
                        body_type: 'SEDAN',
                        engine_type: 'GASOLINE',
                        displacement: 1984,
                        transmission: 'ROBOT',
                        horse_power: 150,
                        mmmAliases: [
                            'Audi',
                            'A4',
                            'A4 B9',
                            'Седан',
                            '2.0 AMT 150 л.c.',
                        ],
                    },
                    displacement_segments: {
                        '1400': 9,
                        '1800': 31,
                        '2000': 60,
                    },
                    engine_type_segments: {
                        DIESEL: 6,
                        GASOLINE: 94,
                    },
                    transmission_segments: {
                        VARIATOR: 34,
                        MECHANICAL: 1,
                        ROBOT: 64,
                        AUTOMATIC: 1,
                    },
                    gear_type_segments: {
                        FORWARD_CONTROL: 75,
                        ALL_WHEEL_DRIVE: 25,
                    },
                },
                duration_of_sale: {
                    data_source: 'MODEL',
                    avg: 27,
                    vas: 22,
                    cert: 19,
                },
            },
            pending: false,
        },
        breadcrumbs: {
            data: {
                FORD: {
                    'autoru-alias': 'ford',
                    id: 'FORD',
                    name: 'Ford',
                    count: 716,
                    'cyrillic-name': 'Форд',
                    popular: true,
                    reviews_count: 3824,
                },
            },
        },
        config: {
            data: {
                pageParams: {
                    mark: 'FORD',
                    model: 'ECOSPORT',
                    category: 'CARS',
                },
            },
        },
    };
    const snippet = getStatsText(state);
    const result = 'Статистика цен на Ford по России. Средняя цена на Форд на основе 270 объявлений - 1 085 765 ₽, минимальная цена - 505 000 ₽. ' +
    'Самая популярная модификация - Audi A4 A4 B9 Седан 2.0 AMT 150 л.c., самый популярный двигатель - 2.0 л.с коробкой робот и передним приводом.' +
    ' Время продажи автомобиля на Авто.ру от 22 до 27 дней, сейчас в продаже 270 автомобилей Ford.';

    expect(snippet).toEqual(result);
});

it('Должен сгенерировать пустую строку, если нет никаких параметров', () => {
    const snippet = getStatsText({});

    expect(snippet).toEqual('');
});
