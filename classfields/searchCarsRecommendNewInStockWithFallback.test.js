const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const block = require('./searchCarsRecommendNewInStockWithFallback');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const mockOffer = require('autoru-frontend/mockData/responses/offer.mock').offer;
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();

    context = createContext({ req, res });
});

it('если по марке-модели есть офферы, то не должен отправлять второй запрос только по марке', () => {
    publicApi
        .get('/1.0/search/cars/context/recommend-new-in-stock')
        .query(true)
        .reply(200, {
            pagination: {
                total_offers_count: 1,
            },
            offers: [
                mockOffer,
            ],
            title_code: '',
            status: 'SUCCESS',
        });

    return de.run(block, {
        context,
        params: {
            catalog_filter: [
                {
                    mark: 'AUDI',
                    model: 'A6',
                },
            ],
        },
    }).then(result => {
        // чтобы не писать огромный снэпшот, проверяем, какая создалась ссылка
        expect(result.listingUrl).toEqual('https://autoru_frontend.base_domain/cars/audi/a6/new/');
    });
});

it('если по марке-модели ничего не нашлось, то должен искать только по марке', () => {
    publicApi
        .get('/1.0/search/cars/context/recommend-new-in-stock')
        .query(query => query.catalog_filter === 'mark=AUDI,model=A6')
        .reply(200, {
            pagination: {
                total_offers_count: 0,
            },
            offers: [],
            status: 'SUCCESS',
        });

    publicApi
        .get('/1.0/search/cars/context/recommend-new-in-stock')
        .query(query => query.catalog_filter === 'mark=AUDI')
        .reply(200, {
            pagination: {
                total_offers_count: 1,
            },
            offers: [
                mockOffer,
            ],
            status: 'SUCCESS',
        });

    return de.run(block, {
        context,
        params: {
            catalog_filter: [
                {
                    mark: 'AUDI',
                    model: 'A6',
                },
            ],
        },
    }).then(result => {
        // чтобы не писать огромный снэпшот, проверяем, какая создалась ссылка
        expect(result.listingUrl).toEqual('https://autoru_frontend.base_domain/cars/audi/new/');
    });
});

it('если в catalog_filter несколько условий, то не должен искать только по марке', () => {
    publicApi
        .get('/1.0/search/cars/context/recommend-new-in-stock')
        .query(query => !query.group_by)
        .reply(200, {
            pagination: {
                total_offers_count: 0,
            },
            offers: [],
            status: 'SUCCESS',
        });

    publicApi
        .get('/1.0/search/cars/context/recommend-new-in-stock')
        .query(query => Boolean(query.group_by))
        .reply(200, {
            pagination: {
                total_offers_count: 1,
            },
            offers: [
                mockOffer,
            ],
            status: 'SUCCESS',
        });

    return de.run(block, {
        context,
        params: {
            catalog_filter: [
                {
                    mark: 'AUDI',
                    model: 'A6',
                },
                {
                    mark: 'AUDI',
                    model: 'A8',
                },
            ],
        },
    }).then(result => {
        // чтобы не писать огромный снэпшот, проверяем, какая создалась ссылка
        // eslint-disable-next-line max-len
        expect(result.listingUrl).toEqual('https://autoru_frontend.base_domain/cars/new/?catalog_filter=mark%3DAUDI%2Cmodel%3DA6&catalog_filter=mark%3DAUDI%2Cmodel%3DA8');
    });
});

it('отправляет запрос по Москве, если в ответе пришел NEW_IN_STOCK_MOSCOW', () => {
    publicApi
        .get('/1.0/search/cars/context/recommend-new-in-stock')
        .query(true)
        .reply(200, {
            pagination: {
                total_offers_count: 1,
            },
            offers: [
                mockOffer,
            ],
            title_code: 'NEW_IN_STOCK_MOSCOW',
            status: 'SUCCESS',
        });

    return de.run(block, {
        context,
        params: {
            catalog_filter: [
                {
                    mark: 'AUDI',
                    model: 'A6',
                },
            ],
        },
    }).then(result => {
        // чтобы не писать огромный снэпшот, проверяем, какая создалась ссылка
        expect(result.listingUrl).toEqual('https://autoru_frontend.base_domain/moskva/cars/audi/a6/new/');
    });
});

it('отправляет старый запрос, если регион не входит в список московских', () => {
    publicApi
        .get('/1.0/search/cars/context/recommend-new-in-stock')
        .query(true)
        .reply(200, {
            pagination: {
                total_offers_count: 1,
            },
            offers: [
                mockOffer,
            ],
            status: 'SUCCESS',
        });

    return de.run(block, {
        context,
        params: {
            catalog_filter: [
                {
                    mark: 'AUDI',
                    model: 'A6',
                },
            ],
        },
    }).then(result => {
        // чтобы не писать огромный снэпшот, проверяем, какая создалась ссылка
        expect(result.listingUrl).toEqual('https://autoru_frontend.base_domain/cars/audi/a6/new/');
    });
});

it('проставляет region=msk если в ответ пришел NEW_IN_STOCK_MOSCOW и регион не Москва', () => {
    publicApi
        .get('/1.0/search/cars/context/recommend-new-in-stock')
        .query(true)
        .reply(200, {
            pagination: {
                total_offers_count: 1,
            },
            offers: [
                mockOffer,
            ],
            title_code: 'NEW_IN_STOCK_MOSCOW',
            status: 'SUCCESS',
        });

    return de.run(block, {
        context: {
            ...context,
            req: {
                ...context.req,
                geoIds: [ 49 ],
            },
        },
        params: {
            catalog_filter: [
                {
                    mark: 'AUDI',
                    model: 'A6',
                },
            ],
        },
    }).then(result => {
        expect(result.region).toEqual('msk');
    });
});

it('проставляет region=null если в ответ пришел NEW_IN_STOCK_MOSCOW и регион Москва', () => {
    publicApi
        .get('/1.0/search/cars/context/recommend-new-in-stock')
        .query(true)
        .reply(200, {
            pagination: {
                total_offers_count: 1,
            },
            offers: [
                mockOffer,
            ],
            title_code: 'NEW_IN_STOCK_MOSCOW',
            status: 'SUCCESS',
        });

    return de.run(block, {
        context: {
            ...context,
            req: {
                ...context.req,
                geoIds: [ 213 ],
            },
        },
        params: {
            catalog_filter: [
                {
                    mark: 'AUDI',
                    model: 'A6',
                },
            ],
        },
    }).then(result => {
        expect(result.region).toBeNull();
    });
});

it('проставляет region=spb если в ответ пришел NEW_IN_STOCK_SPB и регион не Питер', () => {
    publicApi
        .get('/1.0/search/cars/context/recommend-new-in-stock')
        .query(true)
        .reply(200, {
            pagination: {
                total_offers_count: 1,
            },
            offers: [
                mockOffer,
            ],
            title_code: 'NEW_IN_STOCK_SPB',
            status: 'SUCCESS',
        });

    return de.run(block, {
        context: {
            ...context,
            req: {
                ...context.req,
                geoIds: [ 13 ],
            },
        },
        params: {
            catalog_filter: [
                {
                    mark: 'AUDI',
                    model: 'A6',
                },
            ],
        },
    }).then(result => {
        expect(result.region).toEqual('spb');
    });
});

it('проставляет region=null если в ответ пришел NEW_IN_STOCK_SPB и регион Питер', () => {
    publicApi
        .get('/1.0/search/cars/context/recommend-new-in-stock')
        .query(true)
        .reply(200, {
            pagination: {
                total_offers_count: 1,
            },
            offers: [
                mockOffer,
            ],
            title_code: 'NEW_IN_STOCK_SPB',
            status: 'SUCCESS',
        });

    return de.run(block, {
        context: {
            ...context,
            req: {
                ...context.req,
                geoIds: [ 2 ],
            },
        },
        params: {
            catalog_filter: [
                {
                    mark: 'AUDI',
                    model: 'A6',
                },
            ],
        },
    }).then((result) => {
        expect(result.region).toBeNull();
    });
});
