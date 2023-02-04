const searchParametersDescription = require('./index');

const EQUIPMENT_DICTIONARY = {
    'rain-sensor': { code: 'rain-sensor', name: 'название rain-sensor' },
    'led-lights': { code: 'led-lights', name: 'название led-lights' },
    xenon: { code: 'xenon', name: 'название xenon' },
    'light-cleaner': { code: 'light-cleaner', name: 'название light-cleaner' },
};

const SEARCH_TAG_DICTIONARY = [
    {
        code: 'handling',
        name: 'Хорошая управляемость',
    },
    {
        code: 'economical',
        name: 'Экономичный',
    },
];

const TESTS = [
    // displacement_from / displacement_to
    {
        category: 'CARS',
        params: {
            displacement_from: 600,
            displacement_to: 1000,
            section: 'used',
        },
    },
    {
        category: 'CARS',
        params: {
            displacement_from: 600,
            section: 'used',
        },
    },
    {
        category: 'CARS',
        params: {
            displacement_to: 1000,
            section: 'used',
        },
    },
    {
        category: 'CARS',
        params: {
            year_to: 2019,
            year_from: 2019,
            section: 'used',
        },
    },
    {
        category: 'MOTO',
        params: {
            displacement_from: 250,
            displacement_to: 1000,
            section: 'used',
        },
    },
    {
        category: 'TRUCKS',
        params: {
            displacement_from: 1200,
            displacement_to: 2600,
            section: 'used',
        },
    },

    // price + currency
    {
        category: 'CARS',
        params: {
            currency: 'USD',
            price_from: 2000,
            section: 'used',
        },
    },
    {
        category: 'CARS',
        params: {
            price_from: 2000,
            section: 'used',
        },
    },

    // cars
    {
        category: 'CARS',
        params: { has_image: false, has_history: true, section: 'used' },
    },
    {
        category: 'CARS',
        params: { has_image: true, has_history: true, section: 'used' },
    },
    // CARS catalog_equipment
    {
        category: 'CARS',
        params: {
            catalog_equipment: [ 'led-lights,xenon', 'rain-sensor' ],
            section: 'all',
        },
    },
    // CARS search_tag
    {
        category: 'CARS',
        params: {
            search_tag: [ 'handling', 'economical' ],
            section: 'all',
        },
    },
    // CARS engine_group
    {
        category: 'CARS',
        params: {
            engine_group: [ 'ELECTRO', 'TURBO', 'ATMO' ],
            section: 'all',
        },
    },
    // moto ATV
    {
        category: 'MOTO',
        params: {
            atv_type: [ 'UTILITARIAN', 'AMPHIBIAN', 'TOURIST', 'CHILDISH', 'BUGGI', 'SPORTS' ],
            moto_type: 'ATV',
            section: 'all',
        },
    },
    // moto MOTORCYCLE
    {
        category: 'MOTO',
        params: {
            section: 'all',
            moto_type: 'MOTORCYCLE',
            transmission: [ 'TRANSMISSION_6', 'AUTOMATIC', 'AUTOMATIC_2_SPEED', 'ROBOTIC' ],
        },
    },
    {
        category: 'MOTO',
        params: {
            section: 'all',
            moto_type: 'MOTORCYCLE',
            engine_type: [ 'INJECTOR', 'TURBO' ],
        },
    },
    // trucks AGRICULTURAL
    {
        category: 'TRUCKS',
        params: {
            agricultural_type: [ 'SEEDER' ],
            category: 'TRUCKS',
            operating_hours_from: 1,
            operating_hours_to: 10,
            trucks_category: 'AGRICULTURAL',
            section: 'all',
        },
    },
    // trucks AUTOLOADER
    {
        category: 'TRUCKS',
        params: {
            autoloader_type: [ 'GRAB_LOADERS' ],
            category: 'TRUCKS',
            trucks_category: 'AUTOLOADER',
            section: 'all',
        },
    },
    // trucks BULLDOZERS
    {
        category: 'TRUCKS',
        params: {
            bulldozer_type: [ 'CRAWLERS_BULLDOZER' ],
            category: 'TRUCKS',
            traction_class: [ 'TRACTION_10', 'TRACTION_6' ],
            trucks_category: 'BULLDOZERS',
            section: 'all',
        },
    },
    // trucks CRANE
    {
        category: 'TRUCKS',
        params: {
            category: 'TRUCKS',
            crane_radius_from: 1,
            trucks_category: 'CRANE',
            section: 'all',
        },
    },
    // trucks DREDGE
    {
        category: 'TRUCKS',
        params: {
            dredge_type: [ 'CRAWLER_EXCAVATOR', 'TRENCH_EXCAVATOR' ],
            price_from: 1,
            price_to: 2,
            trucks_category: 'DREDGE',
            section: 'all',
        },
    },
    {
        category: 'TRUCKS',
        params: {
            bucket_volume_from: 1,
            bucket_volume_to: 20,
            trucks_category: 'DREDGE',
            section: 'all',
        },
    },
    // trucks LCV
    {
        category: 'TRUCKS',
        params: {
            category: 'TRUCKS',
            seats_from: 1,
            seats_to: 10,
            trucks_category: 'LCV',
            section: 'all',
        },
    },
    // trucks MUNICIPAL
    {
        category: 'TRUCKS',
        params: {
            category: 'TRUCKS',
            engine_type: [ 'DIESEL' ],
            municipal_type: [ 'WATERING_MACHINE' ],
            trucks_category: 'MUNICIPAL',
            section: 'all',
        },
    },
    // trucks TRUCK
    {
        category: 'TRUCKS',
        params: {
            category: 'TRUCKS',
            euro_class: [ 'EURO_2', 'EURO_GREEN' ],
            trucks_category: 'TRUCK',
            section: 'all',
        },
    },
    {
        category: 'TRUCKS',
        params: {
            category: 'TRUCKS',
            engine_type: [ 'GASOLINE' ],
            trucks_category: 'TRUCK',
            section: 'all',
            suspension_chassis: [ 'SPRING_PNEUMO' ],
            wheel_drive: [ 'WD_6x4' ],
        },
    },
    // TRUCKS haggle=HAGGLE_POSSIBLE
    {
        category: 'TRUCKS',
        params: {
            category: 'TRUCKS',
            haggle: 'HAGGLE_POSSIBLE',
            section: 'all',
            trucks_category: 'TRUCK',
        },
    },
    // options.type === 'snippet'
    {
        category: 'CARS',
        params: {
            year_to: 2017,
            year_from: 2019,
            displacement_from: 250,
            displacement_to: 1000,
            section: 'used',
            has_image: false,
            has_history: true,
        },
        options: {
            type: 'snippet',
        },
    },
];

TESTS.forEach(item => {
    it(`должен вернуть правильное описание параметров поиска для ${ item.category } ${ JSON.stringify(item.params) }`, () => {
        expect(searchParametersDescription(item, EQUIPMENT_DICTIONARY, SEARCH_TAG_DICTIONARY, undefined, item.options)).toMatchSnapshot();
    });
});
