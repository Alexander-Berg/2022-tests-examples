/**
 * @jest-environment node
 */

const de = require('descript');

const session = require('./session');

const createContext = require('../../../../descript/createContext');
const createHttpReq = require('../../../../../mocks/createHttpReq');
const createHttpRes = require('../../../../../mocks/createHttpRes');

const autoruApi = require('../../baseHttpBlockAutoruApi.nock.fixtures');
const sessionFixtures = require('./session.nock.fixtures');

let context;
let req;
let res;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({
        req: req,
        res: res,
        config: {},
    });
});

// здесь нужен именно toMatchSnapshot - потому что нам важно проверять
// не только неизменность передаваеммых данных, но и то,
// что на клиент не просочились лишние данные

it('должен отдать параметры сессии при успешной авторизации пользователя', () => {
    autoruApi.get('/1.0/session/').reply(200, sessionFixtures.user_auth());

    return de.run(session, { context }).then(result => {
        expect(result).toMatchSnapshot();
    });
});

it('должен отдать 200 без параметров пользователя, если пользователь не авторизован', () => {
    autoruApi.get('/1.0/session/').reply(200, sessionFixtures.no_auth());

    return de.run(session, { context }).then(result => {
        expect(result).toMatchSnapshot();
    });
});

it('должен отдать 500, если ручка не ответила', () => {
    autoruApi
        .get('/1.0/session/')
        .times(2) // retry
        .reply(500);

    return de.run(session, { context }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchSnapshot();
        });
});
