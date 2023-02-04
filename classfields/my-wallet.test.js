jest.mock('auto-core/lib/luster-bunker', () => {
    return {
        getNode(path) {
            if (path === '/auto_ru/common/vas') {
                return {};
            }
        },
    };
});

const de = require('descript');
const nock = require('nock');
const createContext = require('auto-core/server/descript/createContext');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const promocoder = require('auto-core/server/resources/promocoder/promocoder.nock.fixtures');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const getPromoFeaturesFixtures = require('auto-core/server/resources/promocoder/methods/getPromoFeatures.nock.fixtures');
const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');
const userFixtures = require('auto-core/server/resources/publicApiAuth/methods/user.nock.fixtures');

const page = require('./my-wallet');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });

    req.geoIds = [ 213 ];
    req.geoParents = [];
    req.geoIdsInfo = [];
});

describe('плохой ответ авторизации', () => {
    it('должен отправить в паспорт, если авторизация ответил 500', () => {
        // Это точно правильное поведение?

        publicApi
            .get('/1.0/session/')
            .times(2)
            .reply(500);

        publicApi
            .get('/1.0/user/')
            .times(2)
            .reply(500);

        return de.run(page, { context }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        id: 'REDIRECTED',
                        location: 'https://autoru_frontend.auth_domain/login/?r=https%3A%2F%2Fundefined',
                    },
                });
            });
    });

    it('должен отправить в паспорт, если пользователь не авторизован', () => {
        // Это точно правильное поведение?

        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.no_auth());

        publicApi
            .get('/1.0/user/')
            .reply(200, userFixtures.no_auth());

        return de.run(page, { context }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        id: 'REDIRECTED',
                        location: 'https://autoru_frontend.auth_domain/login/?r=https%3A%2F%2Fundefined',
                    },
                });
            });
    });

    it('должен отправить в кабинет, если пользователь - клиент', () => {
        // Это точно правильное поведение?

        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.client_auth());

        publicApi
            .get('/1.0/user/')
            .reply(200, userFixtures.client_auth());

        return de.run(page, { context }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        id: 'REDIRECTED',
                        location: 'https://cabinet.autoru_frontend.base_domain/wallet',
                    },
                });
            });
    });
});

describe('авторизация частником', () => {
    beforeEach(() => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.user_auth());

        publicApi
            .get('/1.0/user/')
            .reply(200, userFixtures.user_auth());
    });

    it('должен запросить транзакции пользователя', () => {
        publicApi
            .get('/1.0/billing/autoru/payment/history')
            .query({
                page: 1,
                page_size: 10,
            })
            .reply(200, {
                total: 100,
                payments: [],
            });

        return de.run(page, { context }).then(
            (result) => {
                expect(result).toMatchObject({
                    payments: {
                        total: 100,
                        payments: [],
                    },
                });

                expect(nock.isDone()).toEqual(true);
            });
    });

    it('должен запросить промокодер для пользователя', () => {
        promocoder
            .get('/api/1.x/service/autoru-users/feature/user/autoru_common_TEST_USER_ID')
            .reply(200, getPromoFeaturesFixtures.common());

        return de.run(page, { context }).then(
            (result) => {
                expect(result).toMatchObject({
                    promoFeatures: [
                        {
                            id: 'feature-id',
                            label: 'скидка 10%, осталось 10',
                            name: 'Поднятие в топ',
                            tag: 'top',
                        },
                    ],
                });

                expect(nock.isDone()).toEqual(true);
            });
    });
});
