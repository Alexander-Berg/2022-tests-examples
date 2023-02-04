const getListingStructuredData = require('./getListingStructuredData');
const stateMock = require('autoru-frontend/mocks/ampCatalogStore.mock');
const listingMock = require('autoru-frontend/mockData/state/listing');
const offer = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const MockDate = require('mockdate');

const veryLongString = 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean ma';

describe('Разметка листинга', () => {
    beforeEach(() => {
        MockDate.set('2021-12-01T21:00');
    });

    afterEach(() => {
        MockDate.reset();
    });

    it('Должен отдать структурированные данные для оффера на листинге', () => {
        const state = {
            seoParams: stateMock.seo?.seoParams,
            ratings: stateMock.averageRating?.ratings,
            reviewCountValue: 0,
            image: stateMock.seo?.image,
            listing: {
                data: {
                    ...listingMock.data,
                    offers: [ {
                        ...offer,
                        additional_info: {
                            creation_date: '1553183449000',
                        },
                    } ],
                },
            },
        };
        expect(getListingStructuredData(state)).toMatchSnapshot();
    });

    it('Должен отдать структурированные данные листинга легковых', () => {
        const state = {
            seoParams: stateMock.seo?.seoParams,
            ratings: stateMock.averageRating?.ratings,
            reviewCountValue: 0,
            image: stateMock.seo?.image,
            listing: {
                data: {
                    ...listingMock.data,
                    offers: [ {
                        ...offer,
                        additional_info: {
                            creation_date: '1553183449000',
                        },
                    } ],
                },
            },
            config: {
                data: {
                    pageType: 'listing',
                },
            },
        };
        expect(getListingStructuredData(state)).toMatchSnapshot();
    });

    it('Должен отдать структурированные данные листинга коммерческих машин', () => {
        const state = {
            seoParams: stateMock.seo?.seoParams,
            ratings: stateMock.averageRating?.ratings,
            reviewCountValue: 0,
            image: stateMock.seo?.image,
            listing: {
                data: {
                    ...listingMock.data,
                    offers: [ {
                        ...offer,
                        additional_info: {
                            creation_date: '1553183449000',
                        },
                    } ],
                },
            },
            config: {
                data: {
                    pageType: 'commercial-listing',
                },
            },
        };
        expect(getListingStructuredData(state)).toMatchSnapshot();
    });

    it('Должен отдать структурированные данные со значениями по умолчанию для оффера без lowPrice и валюты', () => {
        const state = {
            seoParams: stateMock.seo?.seoParams,
            ratings: stateMock.averageRating?.ratings,
            reviewCountValue: 0,
            image: stateMock.seo?.image,
            listing: {
                data: {
                    ...listingMock.data,
                    price_range: {},
                },
            },
        };

        expect(getListingStructuredData(state)[0].offers.lowPrice).toEqual(0);
        expect(getListingStructuredData(state)[0].offers.priceCurrency).toEqual('RUB');
    });

    it('Должен отдать структурированные данные c description до 100 символов для оффера', () => {
        const state = {
            seoParams: stateMock.seo?.seoParams,
            ratings: stateMock.averageRating?.ratings,
            reviewCountValue: 0,
            image: stateMock.seo?.image,
            listing: {
                data: {
                    ...listingMock.data,
                    offers: [ {
                        ...offer,
                        description: veryLongString,
                    } ],
                },
            },
        };

        const offerDescription = getListingStructuredData(state)[0].description;

        expect(veryLongString).toHaveLength(101);
        expect(offerDescription).toHaveLength(99);
        expect(offerDescription[ offerDescription.length - 1]).toEqual('…');
    });
});
