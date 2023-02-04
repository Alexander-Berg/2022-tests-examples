const de = require('descript');

const session = require('./session');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');

let context;
let req;
let res;
let validResult;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });

    validResult = {
        auth: true,
        emails: [
            {
                confirmed: true,
                email: 'test@yandex.ru',
            },
        ],
        hashEmail: 'c0bc9fd0c655f9f70848e392011406370a25e7e30ed13df4b2d5287a1f194d64',
        id: 'TEST_USER_ID',
        name: 'id TEST_USER_ID',
        profile: {
            autoru: {
                alias: 'id TEST_USER_ID',
            },
        },
    };
});

it('должен отдать параметры сессии при успешной авторизации пользователя', () => {
    publicApi.get('/1.0/session/').reply(200, sessionFixtures.user_auth());

    return de.run(session, { context }).then(result => {
        expect(result).toEqual(validResult);
    });
});

it('должен отдать 200 без параметров пользователя, если пользователь не авторизован', () => {
    publicApi.get('/1.0/session/').reply(200, sessionFixtures.no_auth());

    return de.run(session, { context }).then(result => {
        expect(result).toMatchSnapshot();
    });
});

it('должен отдать 500, если ручка не ответила', () => {
    publicApi
        .get('/1.0/session/')
        .times(3) // retry
        .reply(500);

    return de.run(session, { context }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchSnapshot();
        });
});

describe('кеш', () => {
    it('должен вернуть закешированную, если она есть в context, и не делать второй запрос (sequential)', () => {
        publicApi.get('/1.0/session/').reply(200, sessionFixtures.user_auth());

        return Promise.resolve()
            .then(() => {
                return de.run(session, { context });
            })
            .then(() => {
                return de.run(session, { context });
            })
            .then((result) => {
                expect(result).toEqual(validResult);
            });
    });

    it('должен вернуть закешированную, если она есть в context, и не делать второй запрос (parallel without delay)', () => {
        publicApi
            .get('/1.0/session/')
            .times(2)
            .reply(200, sessionFixtures.user_auth());

        return Promise.all([
            de.run(session, { context }),
            de.run(session, { context }),
        ])
            .then((results) => {
                expect(results[0]).toEqual(validResult);
                expect(results[1]).toEqual(validResult);
            });
    });

    it('должен вернуть закешированную, если она есть в context, и не делать второй запрос (parallel with delay)', () => {
        publicApi
            .get('/1.0/session/')
            .delay(100)
            .reply(200, sessionFixtures.user_auth());

        publicApi
            .get('/1.0/session/')
            .delay(200)
            .reply(200, sessionFixtures.user_auth());

        return Promise.all([
            de.run(session, { context }),
            de.run(session, { context }),
        ])
            .then((results) => {
                expect(results[0]).toEqual(validResult);
                expect(results[1]).toEqual(validResult);
            });
    });
});
