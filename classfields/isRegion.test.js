const { isRegion } = require('./isRegion');

const TESTS = [
    {
        data: [ undefined, undefined ],
        result: null,
    },
    {
        data: [ 213, [ 213, 1, 3, 225, 10001, 10000 ] ],
        result: true,
    },
    {
        data: [ 213, [ 1, 2, 3, 4, 5, 6 ] ],
        result: false,
    },
    {
        data: [ 213, [ { id: 213 }, { id: 1 }, { id: 3 }, { id: 225 }, { id: 10001 }, { id: 10000 } ] ],
        result: true,
    },
    {
        data: [ 213, [ { id: 1 }, { id: 2 }, { id: 3 }, { id: 4 }, { id: 5 }, { id: 6 } ] ],
        result: false,
    },
    {
        data: [ 213, [ { '213': [ 213, 1, 3, 225, 10001, 10000 ] } ] ],
        result: true,
    },
    {
        data: [ 213, [ { '0': [ 1, 2, 3, 4, 5, 6 ] } ] ],
        result: false,
    },
    {
        data: [ 213, 213 ],
        result: true,
    },
    {
        data: [ 213, '213' ],
        result: true,
    },
];

describe('Checking region in geoParents', function() {

    TESTS.forEach(function(testCase) {
        it(JSON.stringify(testCase.data), function() {
            expect(isRegion(...testCase.data)).toEqual(testCase.result);
        });

    });

});
