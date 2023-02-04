import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import type { THttpRequest, THttpResponse } from 'auto-core/http';

import middleware from './redirect-old-index-pages';

let req: THttpRequest;
let res: THttpResponse;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
});

it('не должен ничего сделать, если запрос не GET', () => {
    req.method = 'POST';

    middleware(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});

describe('moto', () => {
    it('должен средиректить /moto/ в /motorcycle/all/', () => {
        req.urlWithoutRegion = '/moto/';

        middleware(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'OLD_INDEX_PAGE',
                data: {
                    location: 'https://autoru_frontend.base_domain/motorcycle/all/',
                    status: 301,
                },
            });
        });
    });

    it('должен средиректить /motorcycle/ в /motorcycle/all/', () => {
        req.urlWithoutRegion = '/motorcycle/';

        middleware(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'OLD_INDEX_PAGE',
                data: {
                    location: 'https://autoru_frontend.base_domain/motorcycle/all/',
                    status: 301,
                },
            });
        });
    });

    it('должен средиректить /snowmobile/ в /snowmobile/all/', () => {
        req.urlWithoutRegion = '/snowmobile/';

        middleware(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'OLD_INDEX_PAGE',
                data: {
                    location: 'https://autoru_frontend.base_domain/snowmobile/all/',
                    status: 301,
                },
            });
        });
    });
});

describe('trucks', () => {

    it('должен средиректить /commercial/ в /lcv/all/', () => {
        req.urlWithoutRegion = '/commercial/';

        middleware(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'OLD_INDEX_PAGE',
                data: {
                    location: 'https://autoru_frontend.base_domain/lcv/all/',
                    status: 301,
                },
            });
        });
    });

    it('должен средиректить /lcv/ в /lcv/all/', () => {
        req.urlWithoutRegion = '/lcv/';

        middleware(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'OLD_INDEX_PAGE',
                data: {
                    location: 'https://autoru_frontend.base_domain/lcv/all/',
                    status: 301,
                },
            });
        });
    });

    it('должен средиректить /bulldozers/ в /bulldozers/all/', () => {
        req.urlWithoutRegion = '/bulldozers/';

        middleware(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'OLD_INDEX_PAGE',
                data: {
                    location: 'https://autoru_frontend.base_domain/bulldozers/all/',
                    status: 301,
                },
            });
        });
    });
});
