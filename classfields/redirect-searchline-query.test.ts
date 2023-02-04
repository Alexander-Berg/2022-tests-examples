import type { Request } from 'express';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import RedirectError from 'auto-core/lib/handledErrors/RedirectError';

import publicApi from 'auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures';

import type { THttpRequest, THttpResponse } from 'auto-core/http';

import redirectSearchlineQuery from './redirect-searchline-query';

let req: Request &THttpRequest ;
let res: THttpResponse;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    if (req.router) {
        req.router.route.getName = () => 'index';
    }
    req.url = '/';
    req.path = '/';
});

it('должен средиректить на листинг, когда есть только query в параметрах', () => {
    publicApi
        .get('/1.0/searchline/suggest/cars')
        .query({
            query: 'test',
            state_group: 'ALL',
        })
        .reply(200, {
            query: 'test',
            suggests: [
                {
                    params: {
                        catalog_filter: [ { mark: 'OPEL', model: 'ASTRA' } ],
                    },
                },
            ],
            status: 'SUCCESS',
        });

    if (req.router) {
        req.router.params = {
            category: 'cars',
            section: 'all',
            query: 'test',
        };
    }

    return new Promise<void>((done) => {
        redirectSearchlineQuery(req, res, (error) => {
            expect(error).toMatchObject({
                code: RedirectError.CODES.QUERY_SEARCHLINE_TO_PAGE,
                data: {
                    location: 'https://autoru_frontend.base_domain/cars/opel/astra/all/?query=test&from=searchline',
                    status: 301,
                },
            });
            done();
        });
    });
});

it('должен средиректить на листинг и сохранить utm метки', () => {
    publicApi
        .get('/1.0/searchline/suggest/cars')
        .query({
            query: 'test',
            state_group: 'ALL',
        })
        .reply(200, {
            query: 'test',
            suggests: [
                {
                    params: {
                        catalog_filter: [ { mark: 'OPEL', model: 'ASTRA' } ],
                    },
                },
            ],
            status: 'SUCCESS',
        });

    if (req.router) {
        req.router.params = {
            category: 'cars',
            section: 'all',
            query: 'test',
            utm_from: 'auto',
            utm_test: 'test',
        };
    }

    return new Promise<void>((done) => {
        redirectSearchlineQuery(req, res, (error) => {
            expect(error).toMatchObject({
                code: RedirectError.CODES.QUERY_SEARCHLINE_TO_PAGE,
                data: {
                    location: 'https://autoru_frontend.base_domain/cars/opel/astra/all/?query=test&from=searchline&utm_from=auto&utm_test=test',
                    status: 301,
                },
            });
            done();
        });
    });
});

it('не делаем редирект, если в параметрах есть query и марка или модель', () => {
    publicApi
        .get('/1.0/searchline/suggest/cars')
        .query({
            query: 'test',
            state_group: 'ALL',
        })
        .reply(200, {});

    if (req.router) {
        req.router.params = {
            category: 'cars',
            section: 'all',
            catalog_filter: [
                {
                    mark: 'HONDA',
                },
            ],
            query: 'test',
        };
    }

    return new Promise<void>((done) => {
        redirectSearchlineQuery(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});
