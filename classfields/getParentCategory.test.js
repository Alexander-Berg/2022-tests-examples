const getParentCategory = require('./getParentCategory');

const TESTS = [
    { category: 'cars', parent: 'cars' },

    { category: 'artic', parent: 'trucks' },
    { category: 'bus', parent: 'trucks' },
    { category: 'lcv', parent: 'trucks' },
    { category: 'LCV', parent: 'trucks' },
    { category: 'municipal', parent: 'trucks' },
    { category: 'trailer', parent: 'trucks' },
    { category: 'truck', parent: 'trucks' },

    { category: 'atv', parent: 'moto' },
    { category: 'ATV', parent: 'moto' },
    { category: 'motorcycle', parent: 'moto' },
    { category: 'scooters', parent: 'moto' },
    { category: 'snowmobile', parent: 'moto' },

    { category: 'moto', parent: 'moto' },
    { category: 'trucks', parent: 'trucks' },
    { category: 'commercial', parent: 'trucks' },
];

TESTS.forEach(function(testCase) {
    it(`должен вернуть "${ testCase.parent }" для "${ testCase.category }"`, function() {
        expect(getParentCategory(testCase.category)).toEqual(testCase.parent);
    });
});

it('должен вернуть "cars" для неизвестной категории', () => {
    expect(getParentCategory('noop')).toEqual('cars');
});

it('должен вернуть "cars" для пустой категории', () => {
    expect(getParentCategory()).toEqual('cars');
});
