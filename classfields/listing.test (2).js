const de = require('descript');
const _ = require('lodash');
const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const link = require('auto-core/router/auto.ru/server/link');

const offerNewMock = require('autoru-frontend/mockData/responses/offer-new-group.mock.json');
const offerMock = require('autoru-frontend/mockData/responses/offer.mock.json');

const listing = require('./listing');

describe('редирект на групповую карточку в случае единственного сниппета', () => {
    const hasGroupingByConfiguration = (query) => query.group_by === 'CONFIGURATION';
    const hasGroupingByTechParamAndComplectation = (query) => _.isEqual(query.group_by, [ 'TECHPARAM', 'COMPLECTATION' ]);
    const isNotImportant = (query) => !hasGroupingByConfiguration(query) && !hasGroupingByTechParamAndComplectation(query);

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
        };

        req.originalUrl = link('listing', params, req, { noDomain: true });

        res = createHttpRes();

        params = {
            category: 'cars',
            section: 'new',
            catalog_filter: [ { mark: 'AUDI', model: 'A4' } ],
            group_by: [ 'CONFIGURATION' ],
        };

        publicApi
            .get('/1.0/search/cars')
            .query(hasGroupingByTechParamAndComplectation)
            .reply(200, {
                offers: [ offerNewMock ],
            });

        publicApi
            .get(/.+/)
            .query(isNotImportant)
            .reply(200, {})
            .persist();
    });

    it('должен сделать редирект на групповую карточку, если количество групп === 1', () => {
        publicApi
            .get('/1.0/search/cars')
            .query(hasGroupingByConfiguration)
            .reply(200, {
                offers: [ offerNewMock ],
                grouping: {
                    groups_count: 1,
                },
            });

        req.originalUrl = '/cars/audi/a4/new/?group_by=CONFIGURATION';
        const context = createContext({ req, res });
        context.req.experimentsData.has = exp => exp === 'VTF-1625_yes302_redirect';

        return de.run(listing, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        id: 'REDIRECTED',
                        location: 'https://autoru_frontend.base_domain/cars/new/group/kia/soul/21551393-21551904/?from=single_group_snippet_listing',
                    },
                });
            });
    });

    it('должен сделать редирект на групповую карточку, и не потерять содержимое поля from', () => {
        publicApi
            .get('/1.0/search/cars')
            .query(hasGroupingByConfiguration)
            .reply(200, {
                offers: [ offerNewMock ],
                grouping: {
                    groups_count: 1,
                },
            });

        const from = 'wizard.model';
        const context = createContext({ req, res });
        context.req.experimentsData.has = exp => exp === 'VTF-1625_yes302_redirect';
        const paramsWithFrom = {
            ...params,
            from,
        };

        return de.run(listing, { context, params: paramsWithFrom }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result.error.location).toContain(from);
            });
    });

    it('должен сделать редирект на групповую карточку, если количество групп === 1 и нет куки gids', () => {
        publicApi
            .get('/1.0/search/cars')
            .query(hasGroupingByConfiguration)
            .reply(200, {
                offers: [ offerNewMock ],
                grouping: {
                    groups_count: 1,
                },
            });

        delete req.cookies.gids;
        delete req.geoIds;
        const context = createContext({ req, res });
        context.req.experimentsData.has = exp => exp === 'VTF-1625_yes302_redirect';

        return de.run(listing, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        id: 'REDIRECTED',
                        location: 'https://autoru_frontend.base_domain/cars/new/group/kia/soul/21551393-21551904/?from=single_group_snippet_listing',
                    },
                });
            });
    });

    it('не должен сделать редирект на групповую карточку, если количество групп > 1', () => {
        publicApi
            .get('/1.0/search/cars')
            .query(hasGroupingByConfiguration)
            .reply(200, {
                offers: [ offerNewMock ],
                grouping: {
                    groups_count: 2,
                },

            });

        const context = createContext({ req, res });
        context.req.experimentsData.has = exp => exp === 'VTF-1625_yes302_redirect';

        return de.run(listing, { context, params }).then(
            (result) => expect(result).toBeDefined(),
            () => Promise.reject('UNEXPECTED_REJECT'),
        );
    });

    it('не должен сделать редирект на групповую карточку, если количество групп === 1 и нет эксперимента VTF-1625_yes302_redirect', () => {
        publicApi
            .get('/1.0/search/cars')
            .query(hasGroupingByConfiguration)
            .reply(200, {
                offers: [ offerNewMock ],
                grouping: {
                    groups_count: 1,
                },

            });

        const context = createContext({ req, res });

        return de.run(listing, { context, params }).then(
            (result) => expect(result).toBeDefined(),
            () => Promise.reject('UNEXPECTED_REJECT'),
        );
    });

    it('должен передать регион при редиректе на групповую карточку, если он не совпадает с тем, что в cookies', () => {
        publicApi
            .get('/1.0/search/cars')
            .query(hasGroupingByConfiguration)
            .reply(200, {
                offers: [ offerNewMock ],
                grouping: {
                    groups_count: 1,
                },
            });

        req.geoIds = [ 39 ];

        const context = createContext({ req, res });
        context.req.experimentsData.has = exp => exp === 'VTF-1625_yes302_redirect';

        return de.run(listing, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        code: 'LISTING_SINGLE_GROUP_TO_CARD',
                        id: 'REDIRECTED',
                        location: 'https://autoru_frontend.base_domain/cars/new/group/kia/soul/21551393-21551904/?geo_id=39&from=single_group_snippet_listing',
                    },
                });
            });
    });
});

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
