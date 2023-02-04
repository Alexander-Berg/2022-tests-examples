const getCanonicalHttpQuery = require('./getCanonicalHttpQuery');

const TESTS = [
    {
        params: { year_to: 2017, price_from: 1000 },
        query: 'price_from=1000&year_to=2017',
    },
    {
        params: { rid: [ 213 ], catalog_filter: [ { mark: 'AUDI' } ] },
        query: 'catalog_filter=mark=AUDI&rid=213',
    },
    {
        params: { sort: 'fresh_relevance_1-desc', gear_type: [ 'REAR_DRIVE', 'FORWARD_CONTROL' ], section: 'all' },
        query: 'gear_type=FORWARD_CONTROL&gear_type=REAR_DRIVE&section=all',
    },
    {
        params: { sort: 'fresh_relevance_1-desc', top_days: '2', gear_type: [ 'REAR_DRIVE', 'FORWARD_CONTROL' ], section: 'all' },
        query: 'gear_type=FORWARD_CONTROL&gear_type=REAR_DRIVE&section=all',
    },
];

TESTS.forEach(testCase => {
    it(`should return "${ testCase.query }" for ${ JSON.stringify(testCase.params) }`, () => {
        expect(getCanonicalHttpQuery(testCase.params)).toEqual(testCase.query);
    });
});
