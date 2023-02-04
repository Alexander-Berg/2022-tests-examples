const catalogFilterToBcLookup = require('./catalogFilterToBcLookup');

const TEST = [
    { catalog_filter: '', bc_lookup: '' },
    { catalog_filter: { mark: undefined, model: undefined, generation: undefined, configuration: undefined }, bc_lookup: '' },
    { catalog_filter: { mark: 'AUDI' }, bc_lookup: 'AUDI' },
    { catalog_filter: { mark: 'AUDI', model: 'A4' }, bc_lookup: 'AUDI#A4' },
    { catalog_filter: { mark: 'AUDI', model: 'A4', generation: '123' }, bc_lookup: 'AUDI#A4#123' },
    { catalog_filter: { mark: 'AUDI', model: 'A4', nameplate: '456', generation: '123' }, bc_lookup: 'AUDI#A4#123' },
];

TEST.forEach(testCase => {
    it(`should convert "${ JSON.stringify(testCase.catalog_filter) }" to "${ testCase.bc_lookup }"`, () => {
        expect(catalogFilterToBcLookup(testCase.catalog_filter)).toEqual(testCase.bc_lookup);
    });
});
