import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import createContext from 'auto-core/server/descript/createContext';
import publicApi from 'auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures';
import sessionFixtures from 'auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures';
import userFixtures from 'auto-core/server/resources/publicApiAuth/methods/user.nock.fixtures';

import type { THttpResponse, THttpRequest } from 'auto-core/http';

import controller from './c2b-auction-add';

describe('контроллер c2b-auction-add', () => {
    let context: ReturnType<typeof createContext>;
    let req: THttpRequest;
    let res: THttpResponse;
    beforeEach(() => {
        req = createHttpReq();
        res = createHttpRes();
        context = createContext({ req, res });
    });

    it('показывает 404 для незалогинов', () => {
        context.req.experimentsData.has = () => true;

        publicApi
            .get(`/1.0/user/`)
            .reply(200, userFixtures.no_auth())
            .get(`/1.0/session/`)
            .reply(200, sessionFixtures.no_auth())
            .get('/1.0/search/cars/breadcrumbs')
            .query(true)
            .reply(200, {})
            .get('/1.0/reference/catalog/cars/suggest')
            .reply(200, {
                car_suggest: {
                    body_types: [],
                },
            });

        return de.run(controller, { context }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (error) => {
                expect(error).toEqual({
                    error: {
                        id: 'NOT_FOUND',
                        status_code: 404,
                    },
                });
            },
        );
    });

    it('показывает 404 для не сотрудников Яндекса', () => {
        context.req.experimentsData.has = () => true;

        publicApi
            .get(`/1.0/user/`)
            .reply(200, userFixtures.client_auth())
            .get(`/1.0/session/`)
            .reply(200, sessionFixtures.client_auth())
            .get('/1.0/search/cars/breadcrumbs')
            .query(true)
            .reply(200, {})
            .get('/1.0/reference/catalog/cars/suggest')
            .reply(200, {
                car_suggest: {
                    body_types: [],
                },
            });

        return de.run(controller, { context }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (error) => {
                expect(error).toEqual({
                    error: {
                        id: 'NOT_FOUND',
                        status_code: 404,
                    },
                });
            },
        );
    });
});
