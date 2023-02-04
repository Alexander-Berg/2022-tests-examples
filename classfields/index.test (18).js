const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const emailChange = require('./index');

let req;
let res;
let context;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });

    publicApi
        .get('/1.0/user/')
        .query((query) => query.with_auth_types === 'true')
        .reply(200, {
            user: {
                id: '777',
            },
        });

    publicApi
        .get('/1.0/user/')
        .query((query) => !query.with_auth_types)
        .reply(200, {});

    publicApi
        .post('/1.0/user/confirm')
        .reply(200, {
            status: 'SUCCESS',
        });

    publicApi
        .post('/1.0/user/email/change')
        .reply(200, {
            status: 'SUCCESS',
        });

});

it('должен обработать подтверждение адреса электронной почты', () => {
    const params = {
        new_email: 'test@test.ru',
        code: '7777',
    };
    return de.run(emailChange, { context, params }).then(
        (result) => expect(result).toMatchSnapshot(),
        () => Promise.reject('UNEXPECTED_REJECT'),
    );
});

it('должен обработать смену адреса электронной почты', () => {
    const params = {
        email: 'old_test@test.ru',
        new_email: 'test@test.ru',
        code: '7777',
    };
    return de.run(emailChange, { context, params }).then(
        (result) => expect(result).toMatchSnapshot(),
        () => Promise.reject('UNEXPECTED_REJECT'),
    );
});
