const breadcrumbsPublicApiMock = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock');

const listingCommon = require('./index');

const mockState = (searchParams, options = {}) => {
    const { geo, pagination } = options;
    const state = {
        geo: {
            gidsInfo: geo || [],
        },
        listing: {
            data: {
                pagination: pagination || {},
                search_parameters: searchParams || {},
            },
        },
        breadcrumbsPublicApi: breadcrumbsPublicApiMock,
    };

    return state;
};

const allSearchCarsParametersMock = {
    catalog_filter: [ { mark: 'FORD', model: 'ECOSPORT', generation: '20104320' } ],
    body_type_group: [ 'HATCHBACK_3_DOORS' ],
    color: [ '040001' ],
    engine_group: [ 'DIESEL' ],
    gear_type: [ 'REAR_DRIVE' ],
    price_to: 10000000,
    section: 'new',
    steering_wheel: 'LEFT',
    transmission: [ 'VARIATOR' ],
    year_from: 2014,
    year_to: 2014,
    catalog_equipment: [ 'seats-4' ],
    displacement_from: 3000,
    displacement_to: 3000,
};

const allSearchTrucksParametersMock = {
    catalog_filter: [ { mark: 'FORD', model: 'ECOSPORT', generation: '20104320' } ],
    light_truck_type: [ 'PICKUP' ],
    color: [ '040001' ],
    engine_type: [ 'GASOLINE' ],
    gear_type: [ 'BACK' ],
    price_to: 10000000,
    section: 'new',
    steering_wheel: 'LEFT',
    transmission: [ 'AUTOMATIC' ],
    year_from: 2014,
    year_to: 2014,
    seats_to: 4,
};

/* eslint-disable max-len */
const TESTS = [

    /* GENERAL TEMPLATE */

    /*  ALL TYPES */

    {
        state: mockState({ category: 'cars' }, { geo: [ { linguistics: { preposition: 'в', prepositional: 'Москве' } } ] }),
        result: 'Полный модельный ряд автомобилей в Москве - много автомобилей, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },
    {
        state: mockState({ category: 'cars' }, { geo: [ { linguistics: { preposition: 'в', prepositional: 'Москве' } } ], pagination: { total_offers_count: 770000 } }),
        result: 'Полный модельный ряд автомобилей в Москве - более 770 000 автомобилей, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },
    {
        state: mockState({ category: 'cars' }, { geo: [ { linguistics: { preposition: 'в', prepositional: 'Москве' } } ], pagination: { total_offers_count: 771 } }),
        result: 'Полный модельный ряд автомобилей в Москве - более 771 автомобиля, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },
    {
        state: mockState({ category: 'moto', moto_category: 'scooters' }),
        result: 'Полный модельный ряд скутеров - много скутеров, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },
    {
        state: mockState({ category: 'moto', moto_category: 'motorcycle' }),
        result: 'Полный модельный ряд мотоциклов - много мотоциклов, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },
    {
        state: mockState({ category: 'moto', moto_category: 'atv' }),
        result: 'Полный модельный ряд мотовездеходов - много мотовездеходов, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },
    {
        state: mockState({ category: 'moto', moto_category: 'snowmobile' }),
        result: 'Полный модельный ряд снегоходов - много снегоходов, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },
    {
        state: mockState({ category: 'trucks', trucks_category: 'lcv' }),
        result: 'Полный модельный ряд лёгкого коммерческого транспорта - много лёгкого коммерческого транспорта, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },
    {
        state: mockState({ category: 'trucks', trucks_category: 'truck' }),
        result: 'Полный модельный ряд грузовиков - много грузовиков, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },
    {
        state: mockState({ category: 'trucks', trucks_category: 'artic' }),
        result: 'Полный модельный ряд седельных тягачей - много седельных тягачей, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },
    {
        state: mockState({ category: 'trucks', trucks_category: 'bus' }),
        result: 'Полный модельный ряд автобусов - много автобусов, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },
    {
        state: mockState({ category: 'trucks', trucks_category: 'trailer' }),
        result: 'Полный модельный ряд прицепов - много прицепов, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },
    {
        state: mockState({ category: 'trucks', trucks_category: 'agricultural' }),
        result: 'Полный модельный ряд сельскохозяйственной техники - много сельскохозяйственной техники, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },
    {
        state: mockState({ category: 'trucks', trucks_category: 'construction' }),
        result: 'Полный модельный ряд строительной и дорожной техники - много строительной и дорожной техники, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },
    {
        state: mockState({ category: 'trucks', trucks_category: 'autoloader' }),
        result: 'Полный модельный ряд автопогрузчиков - много автопогрузчиков, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', trucks_category: 'crane' }),
        result: 'Полный модельный ряд автокранов - много автокранов, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', trucks_category: 'dredge' }),
        result: 'Полный модельный ряд экскаваторов - много экскаваторов, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', trucks_category: 'bulldozers' }),
        result: 'Полный модельный ряд бульдозеров - много бульдозеров, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', trucks_category: 'crane_hydraulics' }),
        result: 'Полный модельный ряд самопогрузчиков - много самопогрузчиков, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', trucks_category: 'municipal' }),
        result: 'Полный модельный ряд коммунальной техники - много коммунальной техники, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    /* NEW */

    {
        state: mockState({ category: 'cars', section: 'new' }),
        result: 'Полный модельный ряд автомобилей - много автомобилей новых, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'moto', section: 'new', moto_category: 'motorcycle' }),
        result: 'Полный модельный ряд мотоциклов - много мотоциклов новых, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'moto', section: 'new', moto_category: 'scooters' }),
        result: 'Полный модельный ряд скутеров - много скутеров новых, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'moto', section: 'new', moto_category: 'atv' }),
        result: 'Полный модельный ряд мотовездеходов - много мотовездеходов новых, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'moto', section: 'new', moto_category: 'snowmobile' }),
        result: 'Полный модельный ряд снегоходов - много снегоходов новых, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'lcv' }),
        result: 'Полный модельный ряд лёгкого коммерческого транспорта - много лёгкого коммерческого транспорта нового, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'truck' }),
        result: 'Полный модельный ряд грузовиков - много грузовиков новых, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'artic' }),
        result: 'Полный модельный ряд седельных тягачей - много седельных тягачей новых, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'bus' }),
        result: 'Полный модельный ряд автобусов - много автобусов новых, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'trailer' }),
        result: 'Полный модельный ряд прицепов - много прицепов новых, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'agricultural' }),
        result: 'Полный модельный ряд сельскохозяйственной техники - много сельскохозяйственной техники новой, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'construction' }),
        result: 'Полный модельный ряд строительной и дорожной техники - много строительной и дорожной техники новой, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'autoloader' }),
        result: 'Полный модельный ряд автопогрузчиков - много автопогрузчиков новых, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'crane' }),
        result: 'Полный модельный ряд автокранов - много автокранов новых, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'dredge' }),
        result: 'Полный модельный ряд экскаваторов - много экскаваторов новых, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'bulldozers' }),
        result: 'Полный модельный ряд бульдозеров - много бульдозеров новых, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'crane_hydraulics' }),
        result: 'Полный модельный ряд самопогрузчиков - много самопогрузчиков новых, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'municipal' }),
        result: 'Полный модельный ряд коммунальной техники - много коммунальной техники новой, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    /* USED */

    {
        state: mockState({ category: 'cars', section: 'used' }),
        result: 'Полный модельный ряд автомобилей - много автомобилей б/у, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'moto', section: 'used', moto_category: 'motorcycle' }),
        result: 'Полный модельный ряд мотоциклов - много мотоциклов б/у, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'moto', section: 'used', moto_category: 'scooters' }),
        result: 'Полный модельный ряд скутеров - много скутеров б/у, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'moto', section: 'used', moto_category: 'atv' }),
        result: 'Полный модельный ряд мотовездеходов - много мотовездеходов б/у, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'moto', section: 'used', moto_category: 'snowmobile' }),
        result: 'Полный модельный ряд снегоходов - много снегоходов б/у, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'lcv' }),
        result: 'Полный модельный ряд лёгкого коммерческого транспорта - много лёгкого коммерческого транспорта б/у, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'truck' }),
        result: 'Полный модельный ряд грузовиков - много грузовиков б/у, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'artic' }),
        result: 'Полный модельный ряд седельных тягачей - много седельных тягачей б/у, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'bus' }),
        result: 'Полный модельный ряд автобусов - много автобусов б/у, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'trailer' }),
        result: 'Полный модельный ряд прицепов - много прицепов б/у, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'agricultural' }),
        result: 'Полный модельный ряд сельскохозяйственной техники - много сельскохозяйственной техники б/у, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'construction' }),
        result: 'Полный модельный ряд строительной и дорожной техники - много строительной и дорожной техники б/у, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'autoloader' }),
        result: 'Полный модельный ряд автопогрузчиков - много автопогрузчиков б/у, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'crane' }),
        result: 'Полный модельный ряд автокранов - много автокранов б/у, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'dredge' }),
        result: 'Полный модельный ряд экскаваторов - много экскаваторов б/у, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'bulldozers' }),
        result: 'Полный модельный ряд бульдозеров - много бульдозеров б/у, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'crane_hydraulics' }),
        result: 'Полный модельный ряд самопогрузчиков - много самопогрузчиков б/у, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'municipal' }),
        result: 'Полный модельный ряд коммунальной техники - много коммунальной техники б/у, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    // VENDORS
    {
        state: mockState({ category: 'cars', section: 'all', catalog_filter: [ { vendor: 'VENDOR2' } ] }),
        result: 'Полный модельный ряд иномарок - много автомобилей, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'cars', section: 'new', catalog_filter: [ { vendor: 'VENDOR2' } ] }),
        result: 'Полный модельный ряд иномарок - много автомобилей новых, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'cars', section: 'used', catalog_filter: [ { vendor: 'VENDOR2' } ] }),
        result: 'Полный модельный ряд иномарок - много автомобилей б/у, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({
            category: 'cars',
            ...allSearchCarsParametersMock,
            section: 'all',
            on_credit: true,
        }),
        result: 'Полный модельный ряд Ford EcoSport I трехдверный хэтчбэк c задним приводом чёрного цвета с левым рулем с вариатором 2014 года с дизельным двигателем с объёмом двигателя 3.0л 4 места до 10 000 000 рублей - много Форд ЭкоСпорт I в кредит, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    // ALL TYPES WITH MARK MODEL GENERATION + FILTERS

    {
        state: mockState({ category: 'cars', ...allSearchCarsParametersMock },
            { geo: [ { linguistics: { preposition: 'в', prepositional: 'Москве' } } ], pagination: { page: 2, total_offers_count: 77000 } }),
        result: 'Полный модельный ряд Ford EcoSport I трехдверный хэтчбэк c задним приводом чёрного цвета с левым рулем с вариатором 2014 года с дизельным двигателем с объёмом двигателя 3.0л 4 места до 10 000 000 рублей в Москве, страница №2 - более 77 000 Форд ЭкоСпорт I новых, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'lcv', ...allSearchTrucksParametersMock },
            { geo: [ { linguistics: { preposition: 'в', prepositional: 'Москве' } } ], pagination: { page: 2, total_offers_count: 77701 } }),
        result: 'Полный модельный ряд лёгкого коммерческого транспорта Ford EcoSport I чёрного цвета с левым рулем с АКПП 2014 года до 10 000 000 рублей в Москве, страница №2 - более 77 701 объявления о лёгком коммерческом транспорте нового Форд ЭкоСпорт I, свежие объявления о продаже от частников и дилеров на Авто.ру.',
    },
];

TESTS.forEach((test) => it(test.result, () => {
    expect(listingCommon.description(test.state)).toEqual(test.result);
}));

const TESTS_OG = [
    {
        state: mockState({ category: 'cars', section: 'new' }),
        result: 'Выбирайте автомобили - много свежих объявлений о продаже на Авто.ру.',
    },

    {
        state: mockState({ category: 'moto', section: 'new', moto_category: 'motorcycle' }),
        result: 'Выбирайте мотоциклы - много свежих объявлений о продаже на Авто.ру.',
    },

    {
        state: mockState({ category: 'moto', section: 'new', moto_category: 'scooters' }),
        result: 'Выбирайте скутеры - много свежих объявлений о продаже на Авто.ру.',
    },

    {
        state: mockState({ category: 'moto', section: 'new', moto_category: 'atv' }),
        result: 'Выбирайте мотовездеходы - много свежих объявлений о продаже на Авто.ру.',
    },

    {
        state: mockState({ category: 'moto', section: 'new', moto_category: 'snowmobile' }),
        result: 'Выбирайте снегоходы - много свежих объявлений о продаже на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'lcv' }),
        result: 'Выбирайте лёгкий коммерческий транспорт - много свежих объявлений о продаже на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'truck' }),
        result: 'Выбирайте грузовики - много свежих объявлений о продаже на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'artic' }),
        result: 'Выбирайте седельные тягачи - много свежих объявлений о продаже на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'bus' }),
        result: 'Выбирайте автобусы - много свежих объявлений о продаже на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'trailer' }),
        result: 'Выбирайте прицепы - много свежих объявлений о продаже на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'agricultural' }),
        result: 'Выбирайте сельскохозяйственную технику - много свежих объявлений о продаже на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'construction' }),
        result: 'Выбирайте строительную и дорожную технику - много свежих объявлений о продаже на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'autoloader' }),
        result: 'Выбирайте автопогрузчики - много свежих объявлений о продаже на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'crane' }),
        result: 'Выбирайте автокраны - много свежих объявлений о продаже на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'dredge' }),
        result: 'Выбирайте экскаваторы - много свежих объявлений о продаже на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'bulldozers' }),
        result: 'Выбирайте бульдозеры - много свежих объявлений о продаже на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'crane_hydraulics' }),
        result: 'Выбирайте самопогрузчики - много свежих объявлений о продаже на Авто.ру.',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'municipal' }),
        result: 'Выбирайте коммунальную технику - много свежих объявлений о продаже на Авто.ру.',
    },

];

TESTS_OG.forEach((test) => it(test.result, () => expect(listingCommon.ogDescription(test.state)).toEqual(test.result)));
