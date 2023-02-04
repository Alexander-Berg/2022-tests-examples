const getSearchParamsFromAnswers = require('./getSearchParamsFromAnswers');

const questions = require('../mocks/questions.mock');

const TESTS = [
    {
        id: 'с инпутами',
        answerValues: [],
        searchParams: {
            category: 'cars',
            section: 'all',
        },
    },
    {
        id: 1,
        answerValues: [
            [ 'От=123', 'До=34567' ],
        ],
        searchParams: {
            price_from: 123,
            price_to: 34567,
            category: 'cars',
            section: 'all',
        },
    },

    {
        id: 'с чекбоксами',
        answerValues: [ null, 'Да' ],
        searchParams: {
            body_type_group: [
                'ALLROAD_5_DOORS',
                'HATCHBACK_5_DOORS',
                'MINIVAN',
                'SEDAN',
                'WAGON',
            ],
            search_tag: [
                'wide-back-seats',
            ],
            category: 'cars',
            section: 'all',
        },
    },

    {
        id: 'с catalog_filter',
        answerValues: [ null, null, [ 'Да' ] ],
        searchParams: {
            catalog_filter: [ 'mark=AUDI,model=A4' ],
            category: 'cars',
            section: 'all',
        },
    },

    {
        id: 'с exclude_catalog_filter',
        answerValues: [ null, null, [ 'Нет' ] ],
        searchParams: {
            exclude_catalog_filter: [ 'mark=AUDI,model=A4' ],
            category: 'cars',
            section: 'all',
        },
    },

    {
        id: 'c конкатенацией параметров',
        answerValues: [ null, null, 'Да', 'Да' ],
        searchParams: {
            catalog_filter: [ 'mark=AUDI,model=A4', 'mark=AUDI,model=A5', 'mark=BMW,model=4,generation=10202932' ],
            exclude_catalog_filter: [
                'mark=LEXUS',
            ],
            category: 'cars',
            section: 'all',
        },
    },

    {
        id: 'c исключением параметров',
        answerValues: [ null, 'Нет', null, null, null, 'Нет' ],
        searchParams: {
            body_type_group: [
                'ALLROAD',
                'SEDAN',
                'WAGON',
            ],
            category: 'cars',
            section: 'all',
        },
    },
    {
        id: 'с clear_param',
        answerValues: [
            null, 'Нет', null, null, 'Седан',
        ],
        searchParams: {
            body_type_group: [
                'SEDAN',
            ],
            category: 'cars',
            section: 'all',
        },
    },
];

TESTS.forEach(({ answerValues, id, searchParams }) => {
    it('должен правильно сконвертировать ответы ' + id, () => {
        expect(getSearchParamsFromAnswers(questions, answerValues)).toEqual(searchParams);
    });
});
