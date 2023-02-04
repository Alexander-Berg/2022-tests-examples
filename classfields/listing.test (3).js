const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const link = require('auto-core/router/auto.ru/server/link');

const offerMock = require('autoru-frontend/mockData/responses/offer.mock.json');

const listing = require('./listing');

describe('ЧПУ для тегов', () => {
    let req;
    let res;
    let params;

    beforeEach(() => {
        req = {
            ...createHttpReq(),
            cookies: {
                gids: '213',
            },
            geoIds: [ 213 ],
            router: {
                route: {
                    getName: () => 'listing',
                },
            },
        };

        req.originalUrl = link('listing', params, req, { noDomain: true });

        res = createHttpRes();

        params = {
            category: 'cars',
            section: 'all',
            search_tag: [ 'compact' ],
        };

        publicApi
            .get('/1.0/search/cars')
            .query(true)
            .reply(200, {
                offers: [ offerMock ],
            })
            .persist();

        publicApi
            .get('/1.0/search/cars/breadcrumbs')
            .query(true)
            .reply(200, {});

    });

    it('должен вернуть ошибку, если тега нет в словаре', () => {

        publicApi
            .get('/1.0/reference/catalog/tags/v1')
            .query(true)
            .reply(200, {
                dictionary_v1: {
                    values: [],
                },
                status: 'SUCCESS',
            });

        req.originalUrl = '/cars/all/tag/compact/';
        const context = createContext({ req, res });

        return de.run(listing, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        id: 'SEARCH_TAG_NOT_FOUND',
                    },
                });
            });
    });

    it('должен вернуть ошибку, если тег есть в словаре, но у него нет seo_name', () => {

        publicApi
            .get('/1.0/reference/catalog/tags/v1')
            .query(true)
            .reply(200, {
                dictionary_v1: {
                    values: [
                        {
                            code: 'compact',
                            tag: {},
                        },
                    ],
                },
                status: 'SUCCESS',
            });

        req.originalUrl = '/cars/all/tag/compact/';
        const context = createContext({ req, res });

        return de.run(listing, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        id: 'SEARCH_TAG_NOT_FOUND',
                    },
                });
            });
    });

    it('не должен вернуть ошибку, если тег есть в словаре и у него есть seo_name', () => {

        publicApi
            .get('/1.0/reference/catalog/tags/v1')
            .query(true)
            .reply(200, {
                dictionary_v1: {
                    values: [
                        {
                            code: 'compact',
                            tag: {
                                seo_name: 'Компактные автомобили',
                            },
                        },
                    ],
                },
                status: 'SUCCESS',
            });

        req.originalUrl = '/cars/all/tag/compact/';
        const context = createContext({ req, res });

        return de.run(listing, { context, params }).then(
            (result) => expect(result).toBeDefined());
    });
});
