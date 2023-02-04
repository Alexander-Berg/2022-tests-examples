const getSeo = require('./getSeo');
const breadcrumbsPublicApiMock = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock');
const { offerNew } = require('autoru-frontend/mockData/responses/offer.mock');
const geo = require('auto-core/react/dataDomain/geo/mocks/geo.mock');
const breadcrumbsPublicApi = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock');
import MockDate from 'mockdate';

describe('index', () => {
    it('сео тексты для страницы', () => {
        const result = getSeo({
            config: {
                data: {
                    pageType: 'index',
                    pageParams: {
                        category: 'cars',
                    },
                },
            },
            breadcrumbsPublicApi,
            geo,
        });

        const { description, title } = result;

        expect({ title, description }).toMatchSnapshot();
    });

    it('должен сгенерировать каноникл с геоурлом для большого региона', () => {
        const result = getSeo({
            config: {
                data: {
                    pageType: 'index',
                    pageParams: {
                        category: 'cars',
                    },
                },
            },
            geo: {
                geoAlias: 'moskva',
                gids: [ 213 ],
                gidsInfo: [],
                geoSource: 'path',
            },
        });

        expect(result).toHaveProperty('canonical', 'https://autoru_frontend.base_domain/moskva/');
    });

    it('должен сгенерировать каноникл без гео параметра', () => {
        const result = getSeo({
            config: {
                data: {
                    pageType: 'stats',
                    pageParams: {
                        category: 'cars',
                        mark: 'bmw',
                    },
                },
            },
            geo: {
                geoAlias: 'moskva',
                gids: [ 213 ],
            },
            breadcrumbs: {
                mmmParams: {
                    mark: 'bmw',
                },
            },
        });

        expect(result).toHaveProperty('canonical', 'https://autoru_frontend.base_domain/stats/cars/bmw/');
    });

    it('должен сгенерировать каноникл только с параметрами mark и model', () => {
        const result = getSeo({
            config: {
                data: {
                    pageType: 'stats',
                    pageParams: {
                        category: 'cars',
                        complectation_id: '2305629__21183069',
                        configuration_id: '2305629',
                        mark: 'bmw',
                        model: '5er',
                        super_gen: '2305607',
                    },
                },
            },
            geo: {
                geoAlias: 'kazan',
                gids: [ 213 ],
            },
            breadcrumbs: {
                mmmParams: {
                    complectation_id: '2305629__21183069',
                    configuration_id: '2305629',
                    mark: 'bmw',
                    model: '5er',
                    super_gen: '2305607',
                },
            },
        });

        expect(result).toHaveProperty('canonical', 'https://autoru_frontend.base_domain/stats/cars/bmw/5er/');
    });

    it('должен сгенерировать каноникл с геоурлом без ?geo_id для маленького региона', () => {
        const result = getSeo({
            config: {
                data: {
                    pageType: 'index',
                    pageParams: {
                        category: 'cars',
                    },
                },
            },
            geo: {
                geoAlias: 'murmansk',
                geoOverride: true,
                gids: [ 10900 ],
                gidsInfo: [],
                geoSource: 'path',
            },
        });

        expect(result).toHaveProperty('canonical', 'https://autoru_frontend.base_domain/murmansk/');
    });

    it('Должен сгенерировать каноникл для отчетов без гет-параметров', () => {
        const result = getSeo({
            config: {
                data: {
                    pageType: 'proauto-report',
                },
            },
            geo: {
                geoAlias: 'bryansk',
                geoSource: 'query',
                geoParents: [],
                multipleGeoIdsParents: { '100670': [ 100670, 162685, 98718, 10650, 3, 225, 10001, 10000 ] },
                regionByIpParents: [ 213, 1, 3, 225, 10001, 10000 ],
                gids: [ 100670 ],
                gidsInfo: [],
                radius: 0,
                radiusAllowed: true,
                dealersRadius: 0,
                geoOverride: true,
                geoSeoOverride: true,
                isGoodRegion: false,
                regionByIp: 213,
                regionByIpCapitalInfo: {
                    id: 213,
                    latitude: 55.753215,
                    longitude: 37.622504,
                    name: 'Москва',
                    parent_id: 1,
                    type: 6,
                    geoAlias: 'moskva',
                },
            },
        });

        expect(result.canonical).not.toMatch(/\?/);
    });
});

describe('listing', () => {
    beforeEach(() => {
        MockDate.set('2021-12-01T21:00');
    });

    afterEach(() => {
        MockDate.reset();
    });

    it('должен сгенерировать каноникл с гео столицы, а не региона', () => {
        const result = getSeo({
            config: {
                data: {
                    pageType: 'listing',
                    pageParams: {
                        category: 'cars',
                        section: 'all',
                    },
                },
            },
            listing: {
                data: {
                    offers: [ offerNew, offerNew, offerNew, offerNew ],
                },
            },
            geo: {
                geoAlias: 'okrug_samara',
                geoCapital: {
                    geoAlias: 'samara',
                },
                gids: [ 120861 ],
            },
        });

        expect(result).toHaveProperty('canonical', 'https://autoru_frontend.base_domain/samara/cars/all/');
        expect(result).toHaveProperty('amphtml', 'https://autoru_frontend.base_domain/samara/amp/cars/all/');
    });

    it('должен сгенерировать url amp-версии листинга без гео, если это /rossiya', () => {
        const result = getSeo({
            config: {
                data: {
                    pageType: 'listing',
                    pageParams: {
                        category: 'cars',
                        section: 'all',
                    },
                },
            },
            geo: {
                gids: [ 225 ],
            },
        });

        expect(result).toHaveProperty('canonical', 'https://autoru_frontend.base_domain/cars/all/');
        expect(result).toHaveProperty('amphtml', 'https://autoru_frontend.base_domain/amp/cars/all/');
    });

    it('должен сгенерировать правильные данные для листинга по тегу', () => {
        const result = getSeo({
            config: {
                data: {
                    pageType: 'listing',
                    pageParams: {
                        search_tag: [ 'compact' ],
                    },
                },
            },
            listing: {
                data: {
                    search_parameters: {
                        search_tag: [ 'compact' ],
                    },
                },
            },
            searchTagDictionary: {
                data: [
                    {
                        code: 'compact',
                        tag: {
                            seo_name: 'компактные автомобили',
                        },
                    },
                ],
            },
        });

        expect(result).toMatchSnapshot();
    });

    it('должен сгенерировать правильные данные для листинга по цвету', () => {
        const result = getSeo({
            config: {
                data: {
                    pageType: 'listing',
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
                    pageType: 'listing',
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
                    pageType: 'listing',
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
                    pageType: 'listing',
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
        expect(result).toHaveProperty('amphtml', 'https://autoru_frontend.base_domain/amp/cars/daewoo/all/do-600000/');
    });
});

it('страница /history', () => {
    const result = getSeo({
        config: {
            data: {
                pageType: 'proauto-landing',
            },
        },
    });

    expect(result).toMatchSnapshot();
});

it('страница /promo/finance', () => {
    const result = getSeo({
        config: {
            data: {
                pageType: 'broker-promo',
            },
        },
    });

    expect(result).toMatchSnapshot();
});

it('страница /garage', () => {
    const result = getSeo({
        config: {
            data: {
                pageType: 'garage',
            },
        },
    });

    expect(result).toMatchSnapshot();
});

it('страница /buyout', () => {
    const result = getSeo({
        config: {
            data: {
                pageType: 'c2b-auction-landing',
            },
        },
    });

    expect(result).toMatchSnapshot();
});

it('страница /buyout/advance/about', () => {
    const result = getSeo({
        config: {
            data: {
                pageType: 'c2b-auction-advance-about',
            },
        },
    });

    expect(result).toMatchSnapshot();
});

it('страница /buyout/advance/prepare', () => {
    const result = getSeo({
        config: {
            data: {
                pageType: 'c2b-auction-advance-prepare',
            },
        },
    });

    expect(result).toMatchSnapshot();
});

it('должен сгенерировать каноникл c гео на главной странице', () => {
    const result = getSeo({
        config: {
            data: {
                pageType: 'index',
                pageParams: {
                    category: 'cars',
                    mark: 'bmw',
                    model: '5er',
                },
            },
        },
        geo: {
            geoAlias: 'moskva',
            gids: [ 213 ],
            geoSource: 'path',
        },
    });

    expect(result).toHaveProperty('canonical', 'https://autoru_frontend.base_domain/moskva/');
});

it('должен сгенерировать каноникл без гео на главной странице', () => {
    const result = getSeo({
        config: {
            data: {
                pageType: 'index',
                pageParams: {
                    category: 'cars',
                    mark: 'bmw',
                    model: '5er',
                },
            },
        },
        geo: {
            geoAlias: 'moskva',
            gids: [ 213 ],
            geoSource: 'cookies',
        },
    });

    expect(result).toHaveProperty('canonical', 'https://autoru_frontend.base_domain/');
});

it('страница /promo/safe-deal', () => {
    const result = getSeo({
        config: {
            data: {
                pageType: 'safe-deal-promo',
            },
        },
    });

    expect(result).toMatchSnapshot();
});
