const breadcrumbsPublicApiMock = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock');

const listingCommon = require('./index');

const mockState = (searchParams, options = {}, priceRange) => {
    const { geo, pagination } = options;
    const state = {
        geo: {
            gidsInfo: geo || [],
        },
        listing: {
            data: {
                pagination: pagination || {},
                search_parameters: searchParams || {},
                price_range: priceRange || {},
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
    section: 'new',
    steering_wheel: 'LEFT',
    transmission: [ 'VARIATOR' ],
    year_from: 2014,
    year_to: 2014,
    catalog_equipment: [ 'seats-4' ],
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
        state: mockState({ category: 'cars' }, { geo: [ { linguistics: { preposition: 'в', prepositional: 'Москве' } } ], pagination: { total_offers_count: 771 } }),
        result: 'Купить автомобили в Москве - более 771 автомобиля на Авто.ру',
    },
    {
        state: mockState({ category: 'moto', moto_category: 'scooters' }),
        result: 'Купить скутеры - много скутеров на Авто.ру',
    },
    {
        state: mockState({ category: 'moto', moto_category: 'motorcycle' }),
        result: 'Купить мотоциклы - много мотоциклов на Авто.ру',
    },
    {
        state: mockState({ category: 'moto', moto_category: 'atv' }),
        result: 'Купить мотовездеходы - много мотовездеходов на Авто.ру',
    },
    {
        state: mockState({ category: 'moto', moto_category: 'snowmobile' }),
        result: 'Купить снегоходы - много снегоходов на Авто.ру',
    },
    {
        state: mockState({ category: 'trucks', trucks_category: 'lcv' }, { geo: [ { linguistics: { preposition: 'в', prepositional: 'Москве' } } ] }),
        result: 'Купить лёгкий коммерческий транспорт в Москве - много лёгкого коммерческого транспорта на Авто.ру',
    },
    {
        state: mockState({ category: 'trucks', trucks_category: 'lcv' }, { geo: [ { linguistics: { preposition: 'в', prepositional: 'Москве' } } ], pagination: { total_offers_count: 771 } }),
        result: 'Купить лёгкий коммерческий транспорт в Москве - более 771 объявления о лёгком коммерческом транспорте на Авто.ру',
    },
    {
        state: mockState({ category: 'trucks', trucks_category: 'truck' }),
        result: 'Купить грузовики - много грузовиков на Авто.ру',
    },
    {
        state: mockState({ category: 'trucks', trucks_category: 'artic' }),
        result: 'Купить седельные тягачи - много седельных тягачей на Авто.ру',
    },
    {
        state: mockState({ category: 'trucks', trucks_category: 'bus' }),
        result: 'Купить автобусы - много автобусов на Авто.ру',
    },
    {
        state: mockState({ category: 'trucks', trucks_category: 'trailer' }),
        result: 'Купить прицепы - много прицепов на Авто.ру',
    },
    {
        state: mockState({ category: 'trucks', trucks_category: 'agricultural' }),
        result: 'Купить сельскохозяйственную технику - много сельскохозяйственной техники на Авто.ру',
    },
    {
        state: mockState({ category: 'trucks', trucks_category: 'construction' }),
        result: 'Купить строительную и дорожную технику - много строительной и дорожной техники на Авто.ру',
    },
    {
        state: mockState({ category: 'trucks', trucks_category: 'autoloader' }),
        result: 'Купить автопогрузчики - много автопогрузчиков на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', trucks_category: 'crane' }),
        result: 'Купить автокраны - много автокранов на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', trucks_category: 'dredge' }),
        result: 'Купить экскаваторы - много экскаваторов на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', trucks_category: 'bulldozers' }),
        result: 'Купить бульдозеры - много бульдозеров на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', trucks_category: 'crane_hydraulics' }),
        result: 'Купить самопогрузчики - много самопогрузчиков на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', trucks_category: 'municipal' }),
        result: 'Купить коммунальную технику - много коммунальной техники на Авто.ру',
    },

    /* NEW */

    {
        state: mockState({ category: 'cars', section: 'new' }),
        result: 'Купить новые автомобили - много автомобилей новых на Авто.ру',
    },

    {
        state: mockState({ category: 'moto', section: 'new', moto_category: 'motorcycle' }),
        result: 'Купить новые мотоциклы - много мотоциклов новых на Авто.ру',
    },

    {
        state: mockState({ category: 'moto', section: 'new', moto_category: 'scooters' }),
        result: 'Купить новые скутеры - много скутеров новых на Авто.ру',
    },

    {
        state: mockState({ category: 'moto', section: 'new', moto_category: 'atv' }),
        result: 'Купить новые мотовездеходы - много мотовездеходов новых на Авто.ру',
    },

    {
        state: mockState({ category: 'moto', section: 'new', moto_category: 'snowmobile' }),
        result: 'Купить новые снегоходы - много снегоходов новых на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'lcv' }),
        result: 'Купить новый лёгкий коммерческий транспорт - много лёгкого коммерческого транспорта нового на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'truck' }),
        result: 'Купить новые грузовики - много грузовиков новых на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'artic' }),
        result: 'Купить новые седельные тягачи - много седельных тягачей новых на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'bus' }),
        result: 'Купить новые автобусы - много автобусов новых на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'trailer' }),
        result: 'Купить новые прицепы - много прицепов новых на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'agricultural' }),
        result: 'Купить новую сельскохозяйственную технику - много сельскохозяйственной техники новой на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'construction' }),
        result: 'Купить новую строительную и дорожную технику - много строительной и дорожной техники новой на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'autoloader' }),
        result: 'Купить новые автопогрузчики - много автопогрузчиков новых на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'crane' }),
        result: 'Купить новые автокраны - много автокранов новых на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'dredge' }),
        result: 'Купить новые экскаваторы - много экскаваторов новых на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'bulldozers' }),
        result: 'Купить новые бульдозеры - много бульдозеров новых на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'crane_hydraulics' }),
        result: 'Купить новые самопогрузчики - много самопогрузчиков новых на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'municipal' }),
        result: 'Купить новую коммунальную технику - много коммунальной техники новой на Авто.ру',
    },

    /* USED */

    {
        state: mockState({ category: 'cars', section: 'used' }),
        result: 'Купить автомобили с пробегом - много автомобилей б/у на Авто.ру',
    },

    {
        state: mockState({ category: 'moto', section: 'used', moto_category: 'motorcycle' }),
        result: 'Купить мотоциклы с пробегом - много мотоциклов б/у на Авто.ру',
    },

    {
        state: mockState({ category: 'moto', section: 'used', moto_category: 'scooters' }),
        result: 'Купить скутеры с пробегом - много скутеров б/у на Авто.ру',
    },

    {
        state: mockState({ category: 'moto', section: 'used', moto_category: 'atv' }),
        result: 'Купить мотовездеходы с пробегом - много мотовездеходов б/у на Авто.ру',
    },

    {
        state: mockState({ category: 'moto', section: 'used', moto_category: 'snowmobile' }),
        result: 'Купить снегоходы с пробегом - много снегоходов б/у на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'lcv' }),
        result: 'Купить лёгкий коммерческий транспорт с пробегом - много лёгкого коммерческого транспорта б/у на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'truck' }),
        result: 'Купить грузовики с пробегом - много грузовиков б/у на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'artic' }),
        result: 'Купить седельные тягачи с пробегом - много седельных тягачей б/у на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'bus' }),
        result: 'Купить автобусы с пробегом - много автобусов б/у на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'trailer' }),
        result: 'Купить прицепы с пробегом - много прицепов б/у на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'agricultural' }),
        result: 'Купить сельскохозяйственную технику с пробегом - много сельскохозяйственной техники б/у на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'construction' }),
        result: 'Купить строительную и дорожную технику с пробегом - много строительной и дорожной техники б/у на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'autoloader' }),
        result: 'Купить автопогрузчики с пробегом - много автопогрузчиков б/у на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'crane' }),
        result: 'Купить автокраны с пробегом - много автокранов б/у на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'dredge' }),
        result: 'Купить экскаваторы с пробегом - много экскаваторов б/у на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'bulldozers' }),
        result: 'Купить бульдозеры с пробегом - много бульдозеров б/у на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'crane_hydraulics' }),
        result: 'Купить самопогрузчики с пробегом - много самопогрузчиков б/у на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'used', trucks_category: 'municipal' }),
        result: 'Купить коммунальную технику с пробегом - много коммунальной техники б/у на Авто.ру',
    },

    // VENDORS
    {
        state: mockState({ category: 'cars', section: 'all', catalog_filter: [ { vendor: 'VENDOR2' } ] }),
        result: 'Купить иномарки - много автомобилей на Авто.ру',
    },

    {
        state: mockState({ category: 'cars', section: 'new', catalog_filter: [ { vendor: 'VENDOR5' } ] }),
        result: 'Купить новые французские иномарки - много автомобилей новых на Авто.ру',
    },

    {
        state: mockState({ category: 'cars', section: 'used', catalog_filter: [ { vendor: 'VENDOR1' } ] }),
        result: 'Купить отечественные автомобили с пробегом - много автомобилей б/у на Авто.ру',
    },

    // ALL TYPES WITH MARK MODEL GENERATION + FILTERS

    {
        state: mockState({ category: 'cars', price_to: 10000000, ...allSearchCarsParametersMock },
            { geo: [ { linguistics: { preposition: 'в', prepositional: 'Москве' } } ], pagination: { page: 2, total_offers_count: 777 } }),
        result: 'Купить новые Ford EcoSport I трехдверный хэтчбэк c задним приводом чёрного цвета с левым рулем с вариатором 2014 года с дизельным двигателем 4-местный до 10 000 000 рублей в Москве, страница №2 - более 777 Форд ЭкоСпорт I новых в кузове трехдверный хэтчбэк c задним приводом чёрного цвета с вариатором 2014 года с дизельным двигателем на Авто.ру',
    },

    {
        state: mockState({ category: 'cars', ...allSearchCarsParametersMock },
            { geo: [ { linguistics: { preposition: 'в', prepositional: 'Москве' } } ], pagination: { page: 2, total_offers_count: 777 } },
            { min: { price: 50000 } }),
        result: 'Купить новые Ford EcoSport I трехдверный хэтчбэк c задним приводом чёрного цвета с левым рулем с вариатором 2014 года с дизельным двигателем 4-местный по цене от 50 000 рублей в Москве, страница №2 - более 777 Форд ЭкоСпорт I новых в кузове трехдверный хэтчбэк c задним приводом чёрного цвета с вариатором 2014 года с дизельным двигателем на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'lcv', ...allSearchTrucksParametersMock },
            { geo: [ { linguistics: { preposition: 'в', prepositional: 'Москве' } } ], pagination: { page: 2, total_offers_count: 777 } }),
        result: 'Купить новый лёгкий коммерческий транспорт Ford EcoSport I чёрного цвета с левым рулем с АКПП 2014 года до 10 000 000 рублей в Москве, страница №2 - более 777 объявлений о лёгком коммерческом транспорте нового Форд ЭкоСпорт I чёрного цвета с автоматической коробкой передач 2014 года на Авто.ру',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'lcv', ...allSearchTrucksParametersMock },
            { geo: [ { linguistics: { preposition: 'в', prepositional: 'Москве' } } ] }),
        result: 'Купить новый лёгкий коммерческий транспорт Ford EcoSport I чёрного цвета с левым рулем с АКПП 2014 года до 10 000 000 рублей в Москве - много лёгкого коммерческого транспорта нового Форд ЭкоСпорт I чёрного цвета с автоматической коробкой передач 2014 года на Авто.ру',
    },

    {
        state: mockState({
            category: 'cars',
            ...allSearchCarsParametersMock,
            section: 'all',
            on_credit: true,
        },
        { geo: [ { linguistics: { preposition: 'в', prepositional: 'Москве' } } ], pagination: { page: 1, total_offers_count: 777 } }),
        result: 'Купить в кредит Ford EcoSport I трехдверный хэтчбэк c задним приводом чёрного цвета с левым рулем с вариатором 2014 года с дизельным двигателем 4-местный в Москве - более 777 Форд ЭкоСпорт I в кредит в кузове трехдверный хэтчбэк c задним приводом чёрного цвета с вариатором 2014 года с дизельным двигателем на Авто.ру',
    },
];

TESTS.forEach((test) => it(test.result, () => expect(listingCommon.title(test.state)).toEqual(test.result)));

const TESTS_OG = [
    {
        state: mockState({ category: 'cars', section: 'new' }),
        result: 'Смотрите, что нашлось на Авто.ру: новые автомобили - много объявлений',
    },

    {
        state: mockState({ category: 'moto', section: 'new', moto_category: 'motorcycle' }),
        result: 'Смотрите, что нашлось на Авто.ру: новые мотоциклы - много объявлений',
    },

    {
        state: mockState({ category: 'moto', section: 'new', moto_category: 'scooters' }),
        result: 'Смотрите, что нашлось на Авто.ру: новые скутеры - много объявлений',
    },

    {
        state: mockState({ category: 'moto', section: 'new', moto_category: 'atv' }),
        result: 'Смотрите, что нашлось на Авто.ру: новые мотовездеходы - много объявлений',
    },

    {
        state: mockState({ category: 'moto', section: 'new', moto_category: 'snowmobile' }),
        result: 'Смотрите, что нашлось на Авто.ру: новые снегоходы - много объявлений',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'lcv' }),
        result: 'Смотрите, что нашлось на Авто.ру: новый лёгкий коммерческий транспорт - много объявлений',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'truck' }),
        result: 'Смотрите, что нашлось на Авто.ру: новые грузовики - много объявлений',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'artic' }),
        result: 'Смотрите, что нашлось на Авто.ру: новые седельные тягачи - много объявлений',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'bus' }),
        result: 'Смотрите, что нашлось на Авто.ру: новые автобусы - много объявлений',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'trailer' }),
        result: 'Смотрите, что нашлось на Авто.ру: новые прицепы - много объявлений',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'agricultural' }),
        result: 'Смотрите, что нашлось на Авто.ру: новая сельскохозяйственная техника - много объявлений',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'construction' }),
        result: 'Смотрите, что нашлось на Авто.ру: новая строительная и дорожная техника - много объявлений',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'autoloader' }),
        result: 'Смотрите, что нашлось на Авто.ру: новые автопогрузчики - много объявлений',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'crane' }),
        result: 'Смотрите, что нашлось на Авто.ру: новые автокраны - много объявлений',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'dredge' }),
        result: 'Смотрите, что нашлось на Авто.ру: новые экскаваторы - много объявлений',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'bulldozers' }),
        result: 'Смотрите, что нашлось на Авто.ру: новые бульдозеры - много объявлений',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'crane_hydraulics' }),
        result: 'Смотрите, что нашлось на Авто.ру: новые самопогрузчики - много объявлений',
    },

    {
        state: mockState({ category: 'trucks', section: 'new', trucks_category: 'municipal' }),
        result: 'Смотрите, что нашлось на Авто.ру: новая коммунальная техника - много объявлений',
    },

];

TESTS_OG.forEach((test) => it(test.result, () => expect(listingCommon.ogTitle(test.state)).toEqual(test.result)));
