const de = require('descript');

const contoller = require('./reseller-public-page');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const CORRECT_PUBLIC_USER_INFO_RESPONSE = {
    alias: 'name',
    registration_date: '01-13-2021',
    offers_stats_by_category: {
        CARS: {
            active_offers_count: 0,
            inactive_offers_count: 0,
        },
        MOTO: {
            active_offers_count: 0,
            inactive_offers_count: 0,
        },
        TRUCKS: {
            active_offers_count: 0,
            inactive_offers_count: 0,
        },
    },
};
const CORRECT_PUBLIC_USER_OFFERS_RESPONSE = {
    offers: [],
};

const CORRECT_RESULT = {
    listing: {
        offers: [],
    },
    publicUserInfo: {
        ...CORRECT_PUBLIC_USER_INFO_RESPONSE,
        offers_stats_by_category: {
            ...CORRECT_PUBLIC_USER_INFO_RESPONSE.offers_stats_by_category,
            ALL: {
                active_offers_count: 0,
                inactive_offers_count: 0,
            },
        },
    },
};

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

describe('ручка получения офферов', () => {
    beforeEach(() => {
        publicApi
            .get('/1.0/user/123/info')
            .query(true)
            .reply(200, CORRECT_PUBLIC_USER_INFO_RESPONSE);
    });

    it('должен ответить 200, если ручка ответила 200', () => {
        publicApi
            .get('/1.0/user/123/offers/all')
            .query(true)
            .reply(200, CORRECT_PUBLIC_USER_OFFERS_RESPONSE);

        const params = {
            category: 'all',
            encrypted_user_id: '123',
        };

        return de.run(contoller, { context, params }).then(
            (result) => {
                expect(result).toMatchObject(CORRECT_RESULT);
            });
    });

    it('должен ответить 404, если  ручка с офферами ответила 404', () => {
        publicApi
            .get('/1.0/user/123/offers/all')
            .query(true)
            .reply(404);

        const params = {
            category: 'all',
            encrypted_user_id: '123',
        };

        return de.run(contoller, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (error) => {
                expect(error).toMatchObject({
                    error: {
                        id: 'RESELLER_NOT_FOUND',
                        status_code: 404,
                    },
                });
            });
    });

    it('должен сделать редирект на листинг, если ручка с офферами ответила 403', () => {
        publicApi
            .get('/1.0/user/123/offers/all')
            .query(true)
            .reply(403);

        const params = {
            category: 'all',
            encrypted_user_id: '123',
        };

        return de.run(contoller, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (error) => {
                expect(error).toMatchObject({
                    error: {
                        id: 'REDIRECTED',
                        status_code: 302,
                        location: 'https://autoru_frontend.base_domain/cars/all/',
                    },
                });
            });
    });
});

describe('ручка получения информации о перекупе', () => {
    beforeEach(() => {
        publicApi
            .get('/1.0/user/123/offers/all')
            .query(true)
            .reply(200, CORRECT_PUBLIC_USER_OFFERS_RESPONSE);
    });

    it('должен ответить 200, если ручка ответила 200', () => {
        publicApi
            .get('/1.0/user/123/info')
            .query(true)
            .reply(200, CORRECT_PUBLIC_USER_INFO_RESPONSE);

        const params = {
            category: 'all',
            encrypted_user_id: '123',
        };

        return de.run(contoller, { context, params }).then(
            (result) => {
                expect(result).toMatchObject(CORRECT_RESULT);
            });
    });

    it('должен ответить 404, если  ручка с офферами ответила 404', () => {
        publicApi
            .get('/1.0/user/123/info')
            .query(true)
            .reply(404);

        const params = {
            category: 'all',
            encrypted_user_id: '123',
        };

        return de.run(contoller, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (error) => {
                expect(error).toMatchObject({
                    error: {
                        id: 'RESELLER_NOT_FOUND',
                        status_code: 404,
                    },
                });
            });
    });

    it('должен сделать редирект на листинг, если ручка с офферами ответила 403', () => {
        publicApi
            .get('/1.0/user/123/info')
            .query(true)
            .reply(403);

        const params = {
            category: 'all',
            encrypted_user_id: '123',
        };

        return de.run(contoller, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (error) => {
                expect(error).toMatchObject({
                    error: {
                        id: 'REDIRECTED',
                        status_code: 302,
                        location: 'https://autoru_frontend.base_domain/cars/all/',
                    },
                });
            });
    });
});
