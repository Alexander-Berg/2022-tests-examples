const de = require('descript');
const nock = require('nock');

const createContext = require('auto-core/server/descript/createContext');
const block = require('./baseHttpBlockWithAuth');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let blockWithCheckAuth;
let context;
let req;
let res;
beforeEach(() => {
    blockWithCheckAuth = block({
        block: {
            hostname: 'my-backend',
            pathname: '/mymethod',
            method: 'GET',
        },
        options: {
            after: ({ result }) => result.result,
        },
    });

    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен ответить 401, если пользователь не авторизован', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.no_auth());

    return de.run(blockWithCheckAuth, {
        context,
        params: { client_id: 1 },
    }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'NO_AUTH',
                    status_code: 401,
                },
            });
        });
});

it('должен ответить 401, если сессия 500ит', () => {
    publicApi
        .get('/1.0/session/')
        .times(2)
        .reply(500, sessionFixtures.no_auth());

    return de.run(blockWithCheckAuth, {
        context,
        params: { client_id: 1 },
    }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'NO_AUTH',
                    status_code: 401,
                },
            });
        });
});

it('должен вылонить блок, если пользователь авторизован', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.user_auth());

    nock('http://my-backend')
        .defaultReplyHeaders({
            'content-type': 'application/json',
        })
        .get('/mymethod')
        .reply(200, { foo: 'bar' });

    return de.run(blockWithCheckAuth, {
        context,
        params: { },
    })
        .then((result) => {
            expect(result).toEqual({
                foo: 'bar',
            });
        });
});
