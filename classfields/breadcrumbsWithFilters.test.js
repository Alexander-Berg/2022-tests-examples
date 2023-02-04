const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const breadcrumbsWithFilters = require('./breadcrumbsWithFilters');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

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

describe('обработка ошибок', () => {
    it('должен упасть, если крошки ответили 500', () => {
        publicApi
            .get('/1.0/search/cars/breadcrumbs')
            .times(2)
            .reply(500);

        return de.run(breadcrumbsWithFilters, {
            context,
        }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        id: 'REQUIRED_BLOCK_FAILED',
                    },
                });
            });
    });

    it('должен оборвать запрос, если крошки ответили 400', () => {
        publicApi
            .get('/1.0/search/cars/breadcrumbs')
            .reply(400);

        return de.run(breadcrumbsWithFilters, {
            context,
        }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        id: 'BREADCRUMBS_UNEXPECTED_RESULT',
                        status_code: 404,
                    },
                });
            });
    });

    it('должен оборвать запрос, если крошки ответили 404', () => {
        publicApi
            .get('/1.0/search/cars/breadcrumbs')
            .reply(404);

        return de.run(breadcrumbsWithFilters, {
            context,
        }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        id: 'BREADCRUMBS_UNEXPECTED_RESULT',
                        status_code: 404,
                    },
                });
            });
    });
});

describe('склейка ответа', () => {
    it('не должен склеивать ответ, если не ответили /mark-model-filters', () => {
        publicApi
            .get('/1.0/search/cars/breadcrumbs')
            .reply(200, {
                breadcrumbs: [
                    {
                        entities: [
                            {
                                id: 'AUDI',
                                mark: { cyrillic_name: 'Ауди' },
                                name: 'AudiName',
                                offers_count: 3,
                            },
                        ],
                        meta_level: 'MARK_LEVEL',
                    },
                ],
                status: 'SUCCESS',
            });

        publicApi
            .get('/1.0/search/cars/mark-model-filters?category=cars&context=listing')
            .reply(500);

        return de.run(breadcrumbsWithFilters, {
            context,
        })
            .then((result) => {
                expect(result).toMatchObject([
                    {
                        entities: [
                            {
                                id: 'AUDI',
                                count: 3,
                            },
                        ],
                        meta_level: 'MARK_LEVEL',
                    },
                ]);
            });
    });

    it('должен склеивать ответ, если ответили /mark-model-filters', () => {
        publicApi
            .get('/1.0/search/cars/breadcrumbs')
            .reply(200, {
                breadcrumbs: [
                    {
                        entities: [
                            {
                                id: 'AUDI',
                                mark: { cyrillic_name: 'Ауди' },
                                name: 'AudiName',
                                offers_count: 3,
                            },
                        ],
                        meta_level: 'MARK_LEVEL',
                    },
                ],
                status: 'SUCCESS',
            });

        publicApi
            .get('/1.0/search/cars/mark-model-filters?category=cars&context=listing')
            .reply(200, {
                mark_entries: [
                    {
                        mark_code: 'AUDI',
                        models: [
                            { model_code: 'CLIO_RS', offers_count: 5 },
                        ],
                    },
                ],
            });

        return de.run(breadcrumbsWithFilters, {
            context,
        })
            .then((result) => {
                expect(result).toMatchObject([
                    {
                        entities: [
                            {
                                id: 'AUDI',
                                count: 5,
                            },
                        ],
                        meta_level: 'MARK_LEVEL',
                    },
                ]);
            });
    });
});
