const getName = require('./getName');

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
        result: 'index',
    },

    // like
    {
        data: {
            config: {
                data: {
                    pageType: 'like',
                },
            },
        },
        result: 'like',
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
        result: 'listing',
    },
    {
        data: {
            config: {
                data: {
                    pageType: 'commercial-listing',
                },
            },
        },
        result: 'listing',
    },

    // card
    {
        data: {
            config: {
                data: {
                    pageType: 'card',
                },
            },
        },
        result: 'card',
    },
    {
        data: {
            config: {
                data: {
                    pageType: 'card',
                },
            },
        },
        result: 'card',
    },
    {
        data: {
            config: {
                data: {
                    pageType: 'card-new',
                },
            },
        },
        result: 'card',
    },
    {
        data: {
            config: {
                data: {
                    pageType: 'sale',
                },
            },
        },
        result: 'card',
    },

    // catalog
    {
        data: {
            config: {
                data: {
                    pageType: 'catalog-generation-listing',
                },
            },
        },
        result: 'catalog_listing',
    },
    {
        data: {
            config: {
                data: {
                    pageType: 'catalog-model-listing',
                },
            },
        },
        result: 'catalog_listing',
    },
    {
        data: {
            config: {
                data: {
                    pageType: 'catalog-card',
                },
            },
        },
        result: 'catalog_card',
    },
    {
        data: {
            config: {
                data: {
                    pageType: 'catalog-card-specifications',
                },
            },
        },
        result: 'catalog_card_specifications',
    },
    {
        data: {
            config: {
                data: {
                    pageType: 'catalog-card-equipment',
                },
            },
        },
        result: 'catalog_card_equipment',
    },

    // dealers
    {
        data: {
            config: {
                data: {
                    pageType: 'dealers-listing',
                },
            },
        },
        result: 'dealers_listing',
    },
    {
        data: {
            config: {
                data: {
                    pageType: 'dealer-page',
                },
            },
        },
        result: 'dealer_page',
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
        result: 'journal',
    },
    {
        data: {
            config: {
                data: {
                    pageType: 'mag-article',
                },
            },
        },
        result: 'journal',
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
        result: null,
    },
];

TESTS.forEach(function(testCase) {
    it(`Test function getName in method getPageName, data: ${ JSON.stringify(testCase.data) }`,
        function() {
            expect(getName(testCase.data)).toEqual(testCase.result);
        },
    );

});
