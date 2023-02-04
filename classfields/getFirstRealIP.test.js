const getFirstRealIP = require('./getFirstRealIP');

const TESTS = [
    {
        test: [ '10.0.0.1', '212.38.108.149' ],
        result: '212.38.108.149',
    },
    {
        test: [ '10.0.0.1', '2a02:6b8:b010:57::23' ],
        result: '2a02:6b8:b010:57::23',
    },
    {
        test: [ '10.0.0.1', ' 2a02:6b8:b010:57::23' ],
        result: '2a02:6b8:b010:57::23',
    },
    {
        test: [ 'fe80::861:a5bb:3d89:70e', '212.38.108.149' ],
        result: '212.38.108.149',
    },
    {
        test: [ 'fe80::861:a5bb:3d89:70e%en3', '212.38.108.149' ],
        result: '212.38.108.149',
    },
    {
        test: [ 'fe80::861:a5bb:3d89:70e%en3', ' 212.38.108.149' ],
        result: '212.38.108.149',
    },
    {
        test: [ 'unknown', '212.38.108.149' ],
        result: '212.38.108.149',
    },
    {
        test: [ '311.285.35.394', '83.69.30.106' ],
        result: '83.69.30.106',
    },

];

TESTS.forEach((testCase) => {
    it(`should return "${ testCase.result }" from "${ JSON.stringify(testCase.test) }"`, () => {
        expect(getFirstRealIP(testCase.test)).toEqual(testCase.result);
    });
});
