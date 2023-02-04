const getFirstRealIPFromXForwardedForHeader = require('./getFirstRealIPFromXForwardedForHeader');

const TESTS = [
    {
        test: '10.100.20.45, 195.162.33.125, 2a02:6b8:b010:57::52, 2a02:6b8:c02:422:0:1464:0:c48',
        result: '195.162.33.125',
    },
    {
        test: '10.0.0.1, 2a02:6b8:b010:57::23',
        result: '2a02:6b8:b010:57::23',
    },
    {
        test: 'fe80::861:a5bb:3d89:70e, 212.38.108.149',
        result: '212.38.108.149',
    },
    {
        test: '46.3.8.107, unix:',
        result: '46.3.8.107',
    },
];

TESTS.forEach(testCase => {
    it(`должен вернуть "${ testCase.result }" из "${ testCase.test }"`, () => {
        expect(getFirstRealIPFromXForwardedForHeader(testCase.test)).toEqual(testCase.result);
    });
});
