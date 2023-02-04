const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const loginByToken = require('./login-by-token');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен отредиректить на добавление фоток, если ручка ответила action=offer_photos_add', () => {
    publicApi.post('/1.0/auth/login-by-token', { token: 'OK-offer_photos_add' }).reply(200, {
        payload: {
            action: 'offer_photos_add',
            category: 'cars',
            offer_id: '123-abc',
        },
        status: 'SUCCESS',
    });

    return de.run(loginByToken, {
        context,
        params: { token: 'OK-offer_photos_add' },
    }).then(
        () => Promise.reject('ENEXPECTED_RESOLVE'),
        result => {
            expect(result).toMatchObject({
                error: {
                    code: 'LOGIN_BY_TOKEN',
                    id: 'REDIRECTED',
                    location: 'https://autoru_frontend.base_domain/sale-photos/cars/123-abc/',
                    status_code: 302,
                },
            });
        },
    );
});

it('должен отредиректить в ЛК, если ручка не ответила action=offer_photos_add, но авторизация прошла', () => {
    publicApi.post('/1.0/auth/login-by-token', { token: 'OK-common' }).reply(200, {
        status: 'SUCCESS',
    });

    return de.run(loginByToken, {
        context,
        params: { token: 'OK-common' },
    }).then(
        () => Promise.reject('ENEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'LOGIN_BY_TOKEN',
                    id: 'REDIRECTED',
                    location: '/my/',
                    status_code: 302,
                },
            });
        },
    );
});

it('должен отредиректить в ЛК, если авторизация не прошла', () => {
    publicApi.post('/1.0/auth/login-by-token', { token: 'BAD-401' }).reply(401, {
        error: 'AUTH_ERROR',
        status: 'ERROR',
        detailed_error: 'AUTH_ERROR',
    });

    return de.run(loginByToken, {
        context,
        params: { token: 'BAD-401' },
    }).then(
        () => Promise.reject('ENEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'LOGIN_BY_TOKEN',
                    id: 'REDIRECTED',
                    location: '/my/',
                    status_code: 302,
                },
            });
        },
    );
});

it('должен отдать 500, если ручка не ответил', () => {
    publicApi
        .post('/1.0/auth/login-by-token', { token: 'BAD-500' })
        .times(2)
        .reply(500);

    return de.run(loginByToken, {
        context,
        params: { token: 'BAD-500' },
    }).then(
        () => Promise.reject('ENEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'REQUIRED_BLOCK_FAILED',
                },
            });
        },
    );
});

it('должен отдать 204, если пришел робот', () => {
    req.isRobot = true;
    return de.run(loginByToken, {
        context,
        params: { token: 'NOT_EXIST' },
    }).then(
        () => Promise.reject('ENEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'ACCESS_DENIED',
                    status_code: 204,
                },
            });
        },
    );
});
