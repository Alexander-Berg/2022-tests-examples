const encodeAnswerValues = require('./encodeAnswerValues');

const TESTS = [
    {
        id: 'Пустой массив',
        answerValues: [],
        result: {},
    },

    {
        id: 'Не массив',
        answerValues: {},
        result: null,
    },

    {
        id: 'Undefined',
        answerValues: undefined,
        result: null,
    },

    {
        id: 'Ответ с мультивыбором',
        answerValues: [ 1, 2, [ 'Да', 'Иногда' ] ],
        result: {
            gurua0: 1,
            gurua1: 2,
            gurua2: 'Да#Иногда',
        },
    },

    {
        id: 'Ответы с пропусками',
        answerValues: [ 1, null, 2, null, [ 'Да', 'Иногда' ] ],
        result: {
            gurua0: 1,
            gurua1: null,
            gurua2: 2,
            gurua3: null,
            gurua4: 'Да#Иногда',
        },
    },

    {
        id: 'Ответ с пользовательским вводом',
        answerValues: [ [ 'От=100', 'До=23456' ] ],
        result: {
            gurua0: 'От=100#До=23456',
        },
    },

];

TESTS.forEach(({ answerValues, id, result }) => {
    it('Должен правильно преобразовать ответы. ' + id, () => {
        expect(encodeAnswerValues(answerValues)).toEqual(result);
    });
});
