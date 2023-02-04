'use strict';

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');

const redirectChat = require('./redirect-chat');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
});

it('не должен ничего сделать для обычой ссылки', () => {
    req.url = '/';
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.no_auth());
    redirectChat(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});

it('должен сделать редирект, если есть chat_id а юзер неавторизован', () => {
    return new Promise((done) => {
        req.router.params = Object.freeze({ chat_id: '123' });
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.no_auth());
        redirectChat(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'REQUIRED_REDIRECT',
                data: {
                    location: 'https://autoru_frontend.base_domain/chat-auth/?r2=',
                    reason: 'REDIRECT_FROM_UNAUTHORISED_CHAT',
                },
            });
            done();
        });
    });
});

it('не должен сделать редирект, если есть chat_id и юзер авторизован', () => {
    return new Promise((done) => {
        req.router.params = Object.freeze({ chat_id: '123' });
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.user_auth());
        redirectChat(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});

it('если авторизация не ответила то и фиг с ней', () => {
    return new Promise((done) => {
        req.router.params = Object.freeze({ chat_id: '123' });
        publicApi
            .get('/1.0/session/')
            .reply(500);
        redirectChat(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});
