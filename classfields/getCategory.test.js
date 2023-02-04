const getCategory = require('./getCategory');

const TESTS = [
    // index
    {
        data: {
            config: {
                data: {
                    pageType: 'index',
                },
            },
        },
        result: '',
    },

    // like
    {
        data: {
            config: {
                data: {
                    pageType: 'like',
                    url: '/like/',
                },
            },
        },
        result: '',
    },

    // listing
    {
        data: {
            config: {
                data: {
                    pageType: 'listing',
                },
            },
        },
        result: 'cars',
    },
    {
        data: {
            route: {
                name: 'moto-listing',
            },
        },
        result: 'moto',
    },
    {
        data: {
            route: {
                name: 'commercial-listing',
            },
        },
        result: 'commercial',
    },

    // card
    {
        data: {
            card: {
                category: 'cars',
            },
        },
        result: 'cars',
    },
    {
        data: {
            route: {
                params: {
                    category: 'mototsikly',
                },
            },
        },
        result: 'moto',
    },
    {
        data: {
            getOffer: {
                auto_type: 'commercial',
            },
        },
        result: 'commercial',
    },

    // journal
    {
        data: {
            config: {
                data: {
                    pageType: 'mag-index',
                },
            },
        },
        result: '',
    },
    {
        data: {
            config: {
                data: {
                    pageType: 'mag-article',
                },
            },
        },
        result: '',
    },

    // any page
    {
        data: {
            config: {
                data: {
                    pageType: 'unknown_page',
                },
            },
        },
        result: '',
    },
];

TESTS.forEach(function(testCase) {
    it(`Test function getCategory in method getPageName, data: ${ JSON.stringify(testCase.data) }`,
        function() {
            expect(getCategory(testCase.data)).toEqual(testCase.result);
        },
    );
});
