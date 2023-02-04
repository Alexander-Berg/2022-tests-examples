const starsMarkup = require('auto-core/react/lib/starsMarkup');

const TESTS = {
    '5': [ 'full', 'full', 'full', 'full', 'full' ],
    '4.9': [ 'full', 'full', 'full', 'full', 'full' ],
    '4.8': [ 'full', 'full', 'full', 'full', 'full' ],
    '4.7': [ 'full', 'full', 'full', 'full', 'half' ],
    '4.5': [ 'full', 'full', 'full', 'full', 'half' ],
    '4.3': [ 'full', 'full', 'full', 'full', 'half' ],
    '4.2': [ 'full', 'full', 'full', 'full', 'empty' ],
    '4': [ 'full', 'full', 'full', 'full', 'empty' ],
    '3.8': [ 'full', 'full', 'full', 'full', 'empty' ],
    '3.7': [ 'full', 'full', 'full', 'half', 'empty' ],
    '3.3': [ 'full', 'full', 'full', 'half', 'empty' ],
    '3.2': [ 'full', 'full', 'full', 'empty', 'empty' ],
    '3': [ 'full', 'full', 'full', 'empty', 'empty' ],
};

Object.keys(TESTS).forEach((rating) => {
    it(`should return ${ TESTS[rating].join(',') } for "${ rating }" rating`, () => {
        expect(starsMarkup(rating)).toEqual(TESTS[rating]);
    });
});
