const getNewParentCategory = require('./getNewParentCategory');
const TESTS_WITH_PARENTS = [
    { category: 'cars', parent: 'cars' },
    { category: 'lcv', parent: 'commercial' },
    { category: 'truck', parent: 'commercial' },
    { category: 'artic', parent: 'commercial' },
    { category: 'bus', parent: 'commercial' },
    { category: 'trailer', parent: 'commercial' },
    { category: 'motorcycle', parent: 'moto' },
    { category: 'scooters', parent: 'moto' },
    { category: 'atv', parent: 'moto' },
    { category: 'snowmobile', parent: 'moto' },
    { category: 'moto', parent: 'moto' },
    { category: 'commercial', parent: 'commercial' },
];
const TESTS_WITHOUT_PARENTS = [
    { category: 'cars', parent: 'cars' },
    { category: 'lcv', parent: 'commercial' },
    { category: 'truck', parent: 'commercial' },
    { category: 'artic', parent: 'commercial' },
    { category: 'bus', parent: 'commercial' },
    { category: 'trailer', parent: 'commercial' },
    { category: 'motorcycle', parent: 'moto' },
    { category: 'scooters', parent: 'moto' },
    { category: 'atv', parent: 'moto' },
    { category: 'snowmobile', parent: 'moto' },
    { category: 'moto', parent: undefined },
    { category: 'commercial', parent: undefined },
];

describe('result with proccessing parent categories', function() {
    TESTS_WITH_PARENTS.forEach(function(testCase) {
        it(`should return a parent category of "${ testCase.category }"`, function() {
            expect(getNewParentCategory(testCase.category, true)).toEqual(testCase.parent);
        });
    });
});

describe('result without proccessing parent categories', function() {
    TESTS_WITHOUT_PARENTS.forEach(function(testCase) {
        it(`should return a parent category of "${ testCase.category }"`, function() {
            expect(getNewParentCategory(testCase.category)).toEqual(testCase.parent);
        });
    });
});
