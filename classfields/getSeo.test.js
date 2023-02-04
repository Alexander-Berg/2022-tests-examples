const getSeo = require('./getSeo');
const breadcrumbsPublicApiMock = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock');
const { offerNew } = require('autoru-frontend/mockData/responses/offer.mock');
const MockDate = require('mockdate');

describe('listing', () => {
    beforeEach(() => {
        MockDate.set('2021-12-01T21:00');
    });

    afterEach(() => {
        MockDate.reset();
    });

    it('должен сгенерировать правильные данные для листинга по цвету', () => {
        const result = getSeo({
            config: {
                data: {
                    pageType: 'listing-amp',
                    pageParams: {
                        category: 'cars',
                        section: 'all',
                    },
                },
            },
            geo: {
                gids: [ 213 ],
            },
            listing: {
                data: {
                    offers: [ offerNew, offerNew, offerNew, offerNew ],
                    search_parameters: {
                        category: 'cars',
                        color: [ '040001' ],
                        catalog_filter: [
                            { mark: 'AUDI' },
                        ],
                        price_to: 600000,
                    },
                    section: 'all',
                },
            },
            breadcrumbsPublicApi: breadcrumbsPublicApiMock,
        });

        expect(result).toMatchSnapshot();
    });

    it('должен сгенерировать правильные данные для листинга по двигателю', () => {
        const result = getSeo({
            config: {
                data: {
                    pageType: 'listing-amp',
                    pageParams: {
                        category: 'cars',
                        section: 'all',
                    },
                },
            },
            geo: {
                gids: [ 213 ],
            },
            listing: {
                data: {
                    offers: [ offerNew, offerNew, offerNew, offerNew ],
                    search_parameters: {
                        category: 'cars',
                        engine_group: [ 'DIESEL' ],
                        catalog_filter: [
                            { mark: 'AUDI' },
                        ],
                        price_to: 600000,
                    },
                    section: 'all',
                },
            },
            breadcrumbsPublicApi: breadcrumbsPublicApiMock,
        });

        expect(result).toMatchSnapshot();
    });

    it('должен сгенерировать правильные данные для листинга по кузову и цвету', () => {
        const result = getSeo({
            config: {
                data: {
                    pageType: 'listing-amp',
                    pageParams: {
                        category: 'cars',
                        section: 'all',
                    },
                },
            },
            geo: {
                gids: [ 213 ],
            },
            listing: {
                data: {
                    offers: [ offerNew, offerNew, offerNew, offerNew ],
                    search_parameters: {
                        category: 'cars',
                        body_type_group: [ 'SEDAN' ],
                        catalog_filter: [
                            { mark: 'AUDI' },
                        ],
                        color: [ '040001' ],
                        price_to: 600000,
                    },
                    section: 'all',
                },
            },
            breadcrumbsPublicApi: breadcrumbsPublicApiMock,
        });

        expect(result).toMatchSnapshot();
    });

    it('должен сгенерировать правильные данные для листинга по цене', () => {
        const result = getSeo({
            config: {
                data: {
                    pageType: 'listing-amp',
                    pageParams: {
                        category: 'cars',
                        section: 'all',
                    },
                },
            },
            geo: {
                gids: [ 213 ],
            },
            listing: {
                data: {
                    search_parameters: {
                        category: 'cars',
                        catalog_filter: [
                            { mark: 'DAEWOO' },
                        ],
                        price_to: 600000,
                    },
                    offers: [ offerNew, offerNew, offerNew, offerNew ],
                    section: 'all',
                },
            },
            listingPriceRanges: {
                data: [ { price_to: 600000 } ],
            },
            breadcrumbsPublicApi: breadcrumbsPublicApiMock,
        });

        expect(result).toHaveProperty('canonical', 'https://autoru_frontend.base_domain/cars/daewoo/all/do-600000/');
    });
});

it('страница /history', () => {
    const result = getSeo({
        config: {
            data: {
                pageType: 'proauto-landing-amp',
            },
        },
    });

    expect(result).toMatchSnapshot();
});
