const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const indexBreadcrumbs = require('./indexBreadcrumbs');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

describe('тотал объявлений', () => {
    it('суммирует из ручки фильтров, если она ответила', () => {
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
                            {
                                id: 'BMW',
                                mark: { cyrillic_name: 'БМВ' },
                                name: 'BmwName',
                                offers_count: 7,
                            },
                        ],
                        meta_level: 'MARK_LEVEL',
                        offers_count: 3,
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
                    {
                        mark_code: 'BMW',
                        models: [
                            { model_code: 'X5', offers_count: 10 },
                        ],
                    },
                ],
            });

        return de.run(indexBreadcrumbs, { context })
            .then((result) => {
                expect(result.count).toBe(15);
            });
    });

    it('суммируется из ручки крошек, если фильтры не ответили', () => {
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
                            {
                                id: 'BMW',
                                mark: { cyrillic_name: 'БМВ' },
                                name: 'BmwName',
                                offers_count: 7,
                            },
                        ],
                        meta_level: 'MARK_LEVEL',
                        offers_count: 7,
                    },
                ],
                status: 'SUCCESS',
            });

        publicApi
            .get('/1.0/search/cars/mark-model-filters?category=cars&context=listing')
            .reply(500);

        return de.run(indexBreadcrumbs, { context })
            .then((result) => {
                expect(result.count).toBe(10);
            });
    });

    it('будет 0, если нет ни того, ни другого', () => {
        publicApi
            .get('/1.0/search/cars/breadcrumbs')
            .reply(200, {
                breadcrumbs: [],
                status: 'SUCCESS',
            });

        publicApi
            .get('/1.0/search/cars/mark-model-filters?category=cars&context=listing')
            .reply(500);

        return de.run(indexBreadcrumbs, { context })
            .then((result) => {
                expect(result.count).toBe(0);
            });
    });
});
