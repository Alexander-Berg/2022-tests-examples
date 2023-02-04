const phpBillingTypeToSearcher = require('./phpBillingTypeToSearcher');

const TESTS = [
    { php: [ 'all_sale_add' ], searcher: [ 'add' ] },
    { php: [ 'package_turbo' ], searcher: [ 'turbo' ] },
    { php: [ 'all_sale_color' ], searcher: [ 'color' ] },
    { php: [ 'all_sale_special' ], searcher: [ 'special' ] },
    { php: [ 'all_sale_toplist' ], searcher: [ 'top' ] },
    { php: [ 'all_sale_toplist', 'package_turbo' ], searcher: [ 'top', 'turbo' ] },
    { php: [], searcher: [] },
    { php: [ 'some_unknown_value' ], searcher: [ 'some_unknown_value' ] },
];

TESTS.forEach((testCase) => {
    it(`should transform "${ testCase.php }" to "${ testCase.searcher }"`, () => {
        expect(phpBillingTypeToSearcher(testCase.php)).toEqual(testCase.searcher);
    });
});
