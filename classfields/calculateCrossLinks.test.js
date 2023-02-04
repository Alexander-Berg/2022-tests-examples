const calculateCrossLinks = require('./calculateCrossLinks');

const RESULT_MOCK = {
    colors: {
        '200204': 1,
        '660099': 0,
        FFD600: 0,
        DEA522: 0,
        '97948F': 4,
        FAFBFB: 88,
        '941C35': 7, // невалидный для ЧПУ цвет
    },
    body_types: {
        COUPE: 48,
        SEDAN: 794,
        CABRIO: 0,
        MINIVAN: 0,
    },
    gear_types: {
        ALL_WHEEL_DRIVE: 1199,
        FORWARD_CONTROL: 2,
        REAR_DRIVE: 0,
    },
    engine_types: {
        GASOLINE: 1381,
        DIESEL: 324,
        ELECTRO: 1,
    },
    transmission_types: {
        ROBOT: 1381,
        MECHANICAL: 324,
        VARIATOR: 1,
    },
    // параметры при выборе Марка+Модель
    generations: {
        '2305282': 129,
        '3473199': 100,
        '4720892': 0,
    },
    nameplates: {
        'e-tron': 20,
        'g-tron': 40,
    },
    seats: {
        '1': 19,
        '4': 19,
        '5': 6,
    },
    displacements: {
        '4.0': 10,
        '2.8': 10,
        '4.6': 10,
    },
};

const SEARCH_PARAMS_MOCK = {
    section: 'all',
    catalog_filter: [ { mark: 'AUDI', model: 'A3' } ],
};

// Должно быть не меньше 3 офферов на листинге + не менее 2 ненулевых значений фильтров
it('Должен подготовить перекрестные ссылки для всех фильтров', () => {
    expect(calculateCrossLinks({
        result: RESULT_MOCK,
        params: SEARCH_PARAMS_MOCK,
    })).toMatchSnapshot();
});

const RESULT_MOCK_SINGLE_FILTER = {
    colors: {
        '200204': 0,
        '660099': 0,
        FFD600: 0,
        DEA522: 0,
        '97948F': 4,
        FAFBFB: 0,
    },
};

it('Должен вернуть пустые значния для ссылок, если только 1 результат по фильтрам', () => {
    expect(calculateCrossLinks({
        result: RESULT_MOCK_SINGLE_FILTER,
        params: SEARCH_PARAMS_MOCK,
    })).toEqual({
        colors: [],
    },
    );
});
