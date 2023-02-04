import arrayToPrettyString from './arrayToPrettyString';

const TESTS = [
    {
        array: [ 'Hello', 'Hello', 'Test', 'Test2', 'abracadabra' ],
        expected: 'Hello, Test, Test2 + 1',
    },
    {
        array: [ 'Hello', 'Hello', 'Test' ],
        expected: 'Hello, Test',
    },
];

describe('правильно переводит массив в строку', () => {
    TESTS.forEach(testcase => it(testcase.array.join(' '), () => {
        expect(arrayToPrettyString(testcase.array)).toEqual(testcase.expected);
    }));
});
