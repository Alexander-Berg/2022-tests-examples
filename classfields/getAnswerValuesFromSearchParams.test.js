const getAnswerValuesFromSearchParams = require('./getAnswerValuesFromSearchParams');

const TESTS = [
    {
        id: 'Пустой объект',
        searchParams: {},
        answerValues: [],
    },

    {
        id: 'Undefined в пустой массив',
        searchParams: undefined,
        answerValues: [],
    },

    {
        id: 'Number кастуется в String',
        searchParams: {
            gurua0: 1,
            gurua1: 2,
        },
        answerValues: [ [ '1' ], [ '2' ] ],
    },

    {
        id: 'Ответ с мультивыбором',
        searchParams: {
            gurua0: 1,
            gurua1: 2,
            gurua2: 'Да#Иногда',
        },
        answerValues: [ [ '1' ], [ '2' ], [ 'Да', 'Иногда' ] ],
    },

    {
        id: 'Ответы с пропусками',
        searchParams: {
            gurua0: 1,
            gurua1: null,
            gurua2: 2,
            gurua3: null,
            gurua4: 'Да#Иногда',
        },
        answerValues: [ [ '1' ], null, [ '2' ], null, [ 'Да', 'Иногда' ] ],
    },

    {
        id: 'Ответ с пользовательским вводом',
        searchParams: {
            gurua0: 'От=100#До=23456',
        },
        answerValues: [ [ 'От=100', 'До=23456' ] ],
    },

];

TESTS.forEach(({ answerValues, id, searchParams }) => {
    it('Должен правильно собрать ответы из searchParams. ' + id, () => {
        expect(getAnswerValuesFromSearchParams(searchParams)).toEqual(answerValues);
    });
});
