import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import createContext from 'auto-core/server/descript/createContext';
import publicApi from 'auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures';

import type { THttpResponse, THttpRequest } from 'auto-core/http';

import controller from './c2b-auction-application';

const offerId = '9040931609094442784-3291eb2b';

describe('контроллер c2b-auction-application', () => {
    let context: ReturnType<typeof createContext>;
    let req: THttpRequest;
    let res: THttpResponse;
    beforeEach(() => {
        req = createHttpReq();
        res = createHttpRes();
        context = createContext({ req, res });
    });

    it('возвращает ответ с данным офера и инфой для аукциона', () => {
        context.req.experimentsData.has = () => true;

        publicApi
            .get(`/1.0/user/draft/cars/${ offerId }/c2b_application_info`)
            .reply(200, {
                canApply: true,
            });

        return de.run(controller, { context, params: { draft_id: offerId } }).then(
            (result) => {
                expect(result).toEqual(
                    expect.objectContaining({
                        applicationInfo: {
                            canApply: true,
                        },
                    }),
                );
            },
            () => Promise.reject('UNEXPECTED_RESOLVE'),
        );
    });

    it('Не возвращает ничего, если заявка не найдена', () => {
        context.req.experimentsData.has = () => true;

        publicApi
            .get(`/1.0/user/draft/cars/${ offerId }/c2b_application_info`)
            .reply(404, {});

        return de.run(controller, { context, params: { draft_id: offerId } }).then(
            (result) => {
                expect(result).toEqual(
                    expect.objectContaining({
                        applicationInfo: {},
                    }),
                );
            },
            () => Promise.reject('UNEXPECTED_RESOLVE'),
        );
    });

    it('Не возвращает ничего, если нет сессии', () => {
        context.req.experimentsData.has = () => true;

        publicApi
            .get(`/1.0/user/draft/cars/${ offerId }/c2b_application_info`)
            .reply(401, {});

        return de.run(controller, { context, params: { draft_id: offerId } }).then(
            (result) => {
                expect(result).toEqual(
                    expect.objectContaining({
                        applicationInfo: {},
                    }),
                );
            },
            () => Promise.reject('UNEXPECTED_RESOLVE'),
        );
    });
});
