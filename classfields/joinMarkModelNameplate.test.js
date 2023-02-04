const joinMarkModelNameplate = require('./joinMarkModelNameplate');

const TESTS = [
    { test: [], result: '' },
    { test: [ '' ], result: '' },
    { test: [ 'AUDI' ], result: 'AUDI' },
    { test: [ 'AUDI', '' ], result: 'AUDI' },
    { test: [ 'AUDI', 'A4' ], result: 'AUDI#A4' },
    { test: [ 'AUDI', 'A4', '' ], result: 'AUDI#A4' },
    { test: [ 'AUDI', 'A4', '1' ], result: 'AUDI#A4#1' },
    { test: [ 'AUDI', 'A4', '1', '2' ], result: 'AUDI#A4#1#2' },
    { test: [ 'AUDI', 'A4', '1', '' ], result: 'AUDI#A4#1' },
    { test: [ 'AUDI', 'A4', '', '2' ], result: 'AUDI#A4##2' },
    { test: [ 'AUDI', 'A4', undefined, '2' ], result: 'AUDI#A4##2' },
    // @see http://www.ecma-international.org/ecma-262/5.1/#sec-11.1.4
    // eslint-disable-next-line no-sparse-arrays
    { test: [ 'AUDI', 'A4',, '2' ], result: 'AUDI#A4##2' },
];

TESTS.forEach((testCase) => {
    it(`should join ${ JSON.stringify(testCase.test) } as "${ testCase.result }"`, () => {
        expect(joinMarkModelNameplate(testCase.test)).toEqual(testCase.result);
    });
});
