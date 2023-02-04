const de = require('descript');
const nock = require('nock');

const createContext = require('auto-core/server/descript/createContext');
const baseHttpBlockWithCheckAccess = require('./baseHttpBlockWithCheckAccess');

const apiCabinetNext = require('auto-core/server/resources/apiCabinetNext/apiCabinetNext.nock.fixtures');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let blockWithCheckAccess;
let context;
let req;
let res;
beforeEach(() => {
    blockWithCheckAccess = baseHttpBlockWithCheckAccess({
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
    publicApi.get('/1.0/session/').reply(200, sessionFixtures.no_auth());

    return de.run(blockWithCheckAccess, {
        context,
        params: { client_id: 1 },
    }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (error) => {
            expect(error).toMatchObject({
                error: {
                    id: 'NO_AUTH',
                    status_code: 401,
                },
            });
        });
});

it('должен ответить 401, если авторизации 500ит', () => {
    publicApi
        .get('/1.0/session/')
        .times(2)
        .reply(500);

    return de.run(blockWithCheckAccess, {
        context,
        params: { client_id: 1 },
    }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (error) => {
            expect(error).toMatchObject({
                error: {
                    id: 'NO_AUTH',
                    status_code: 401,
                },
            });
        });
});

it('не должен запрашивать checkAccess, если пользователь авторизован, но нет params.client_id', () => {
    publicApi.get('/1.0/session/').reply(200, sessionFixtures.client_auth());

    nock('http://my-backend')
        .defaultReplyHeaders({
            'content-type': 'application/json',
        })
        .get('/mymethod')
        .reply(200, { foo: 'bar' });

    return de.run(blockWithCheckAccess, {
        context,
        params: {},
    }).then(result => {
        expect(result).toEqual({
            foo: 'bar',
        });
    });
});

it('должен ответиь 403, если клиент авторизован и checkAccess ответил ошибкой', () => {
    publicApi.get('/1.0/session/').reply(200, sessionFixtures.client_auth());

    apiCabinetNext
        .get('/api/1.x/access/client/1')
        .times(2)
        .reply(500, 'oops');

    return de.run(blockWithCheckAccess, {
        context,
        params: { client_id: 1 },
    }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (error) => {
            expect(error).toMatchObject({
                error: {
                    id: 'ACCESS_DENIED',
                    status_code: 403,
                },
            });
        });
});
