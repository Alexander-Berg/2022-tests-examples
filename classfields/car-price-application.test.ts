import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import createContext from 'auto-core/server/descript/createContext';
import publicApi from 'auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures';
import sessionFixtures from 'auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures';

import type { THttpResponse, THttpRequest } from 'auto-core/http';

import controller from './car-price-application';

const offerId = '9040931609094442784-3291eb2b';

describe('контроллер car-price-application', () => {
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
            .get('/1.0/user/draft/cars')
            .reply(200, {
                offer: {
                    category: 'CARS',
                    id: offerId,
                },
                offer_id: offerId,
            })
            .post(`/1.0/user/draft/cars/${ offerId }/c2b_application_validate`)
            .reply(200, {})
            .get(`/1.0/user/draft/cars/${ offerId }/carp_auction_application_info`)
            .reply(200, {
                canApply: true,
            })
            .get(`/1.0/user/`)
            .reply(200, {})
            .get(`/1.0/session/`)
            .reply(200, sessionFixtures.user_auth());

        return de.run(controller, { context, params: { draft_id: offerId } }).then(
            (result) => {
                expect(result).toEqual(
                    expect.objectContaining({
                        offerDraft: {
                            offerId: '9040931609094442784-3291eb2b',
                            images: undefined,
                            offer: {
                                vehicle_info: undefined,
                                state: undefined,
                                documents: undefined,
                                category: 'cars',
                                place: undefined,
                            },
                        },
                        applicationInfo: {
                            canApply: true,
                        },
                    }),
                );
            },
            () => Promise.reject('UNEXPECTED_RESOLVE'),
        );
    });

    it('редирект в лк, если не в экспе', () => {
        context.req.experimentsData.has = () => false;

        return de.run(controller, { context, params: { draft_id: offerId } }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (error) => {
                expect(error).toEqual({
                    error: {
                        code: 'UNVALIDATE_AUCTION_APPLICATION',
                        id: 'REDIRECTED',
                        location: '/my/all',
                        status_code: 302,
                    },
                });
            },

        );
    });

    it('редирект в лк, если не нашли черновик', () => {
        context.req.experimentsData.has = () => true;

        publicApi
            .get('/1.0/user/draft/cars')
            .reply(404)
            .post(`/1.0/user/draft/cars/${ offerId }/c2b_application_validate`)
            .reply(404)
            .get(`/1.0/user/draft/cars/${ offerId }/carp_auction_application_info`)
            .reply(404)
            .get(`/1.0/user/`)
            .reply(200, {})
            .get(`/1.0/session/`)
            .reply(200, sessionFixtures.user_auth());

        return de.run(controller, { context, params: { draft_id: offerId } }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (error) => {
                expect(error).toEqual({
                    error: {
                        code: 'UNVALIDATE_AUCTION_APPLICATION',
                        id: 'REDIRECTED',
                        location: '/my/all',
                        status_code: 302,
                    },
                });
            },
        );
    });

    it('редирект в лк, если не прошла валидация черновика на возможность создать заявку', () => {
        context.req.experimentsData.has = () => true;

        publicApi
            .get('/1.0/user/draft/cars')
            .reply(200, {
                offer: {
                    category: 'CARS',
                    id: offerId,
                },
                offer_id: offerId,
            })
            .post(`/1.0/user/draft/cars/${ offerId }/c2b_application_validate`)
            .reply(200, {
                validation_errors: [
                    { message: 'error' },
                ],
            })
            .get(`/1.0/user/draft/cars/${ offerId }/carp_auction_application_info`)
            .reply(200, {
                canApply: true,
            })
            .get(`/1.0/user/`)
            .reply(200, {})
            .get(`/1.0/session/`)
            .reply(200, sessionFixtures.user_auth());

        return de.run(controller, { context, params: { draft_id: offerId } }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (error) => {
                expect(error).toEqual({
                    error: {
                        code: 'UNVALIDATE_AUCTION_APPLICATION',
                        id: 'REDIRECTED',
                        location: '/my/all',
                        status_code: 302,
                    },
                });
            },
        );
    });

});
