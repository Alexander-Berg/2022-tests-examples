const phpBodySerializer = require('./phpBodySerializer');

const TESTS = [
    {
        body: { a: 1, b: 2 },
        result: { a: 1, b: 2 },
    },
    {
        body: { a: 1, b: [ 1, 2 ] },
        result: { a: 1, 'b[]': [ 1, 2 ] },
    },
    {
        body: { a: 1, b: { c: 3, d: 4 } },
        result: { a: 1, 'b[c]': 3, 'b[d]': 4 },
    },
];

TESTS.forEach(testCase => {
    it(`should return ${ JSON.stringify(testCase.result) } from ${ JSON.stringify(testCase.body) }`, () => {
        expect(phpBodySerializer(testCase.body)).toEqual(testCase.result);
    });
});
