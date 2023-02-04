const filter = require('./filter');

const mockReq = {
    empty: {
        query: {},
        experimentsData: {},
        geoIdsParents: {},
    },
    unknownPage: {
        query: {
            page: 'UNKNOWN_PAGE',
        },
        experimentsData: {},
        geoIdsParents: {},
    },
};

const TESTS = {
    counters: [
        {
            data: mockReq.empty,
            result: null,
        },
        {
            data: mockReq.unknownPage,
            result: null,
        },
    ],
    events: [
        {
            data: {},
            events: null,
            result: null,
        },
        {
            data: null,
            events: [],
            result: null,
        },
    ],
};

describe('Filter retargeting counters and events for iframe', function() {

    TESTS.counters.forEach(function(testCase) {
        it(`Test data: ${ JSON.stringify(testCase.data) }`, function() {
            expect(filter(testCase.data)).toEqual(testCase.result);
        });

    });

});
