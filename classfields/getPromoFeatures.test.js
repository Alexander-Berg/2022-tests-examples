const de = require('descript');

const getPromoFeatures = require('./getPromoFeatures');
const createContext = require('auto-core/server/descript/createContext');

const promocoder = require('auto-core/server/resources/promocoder/promocoder.nock.fixtures');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен сделать запрос и подставить user_id из сессии', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.user_auth());

    promocoder
        .get('/api/1.x/service/autoru-users/feature/user/autoru_common_TEST_USER_ID')
        .reply(200, { message: 'OK' });

    return de.run(getPromoFeatures, {
        params: { },
        context,
    })
        .then((result) => {
            expect(result).toEqual({ message: 'OK' });
        });
});

it('должен сделать запрос и подставить user_id из params, если он есть', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.user_auth());

    promocoder
        .get('/api/1.x/service/autoru-users/feature/user/autoru_common_PARAMS_USER')
        .reply(200, { message: 'OK' });

    return de.run(getPromoFeatures, {
        params: { userId: 'PARAMS_USER' },
        context,
    })
        .then((result) => {
            expect(result).toEqual({ message: 'OK' });
        });
});
