import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import type { TDescriptContext } from 'auto-core/server/descript/createContext';
import createContext from 'auto-core/server/descript/createContext';
import publicApi from 'auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures';
import {
    response200LoginResult,
    response200MatchedCredentials,
    response401,
} from 'auto-core/server/resources/publicApiAuth/methods/loginOrRegisterYandex.fixtures';

import type { THttpRequest, THttpResponse } from 'auto-core/http';

import page from './index';

let req: THttpRequest;
let res: THttpResponse;
let context: TDescriptContext;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });

    req.query = { r: '/yandex-sync/?r=https://auto.ru' };
    req.url = '/yandex-sync/?r=https://auto.ru';
    req.cookies['Session_id'] = 'yandex-session-id';
});

describe('прошла авторизация', () => {
    it('должен проставить куки и сделать редирект', async() => {
        publicApi
            .post('/1.0/auth/login-or-register-yandex')
            .reply(200, response200LoginResult());

        await de.run(page, { context });

        expect(res.statusCode).toEqual(302);
        expect(res.setHeader).toHaveBeenCalledWith('location', 'https://autoru_frontend.base_domain/');
        expect(res.cookie).toHaveBeenCalledWith(
            'autoru_sid', '14212016|1637670340807.7776000.H2w', { httpOnly: true, maxAge: 15552000000, path: '/', sameSite: 'None', secure: true },
        );
        expect(res.cookie).toHaveBeenCalledWith(
            'autoruuid', 'g616e9b1d2bm7', { httpOnly: true, maxAge: 15552000000, path: '/', sameSite: 'None', secure: true },
        );
        expect(res.end).toHaveBeenCalledTimes(1);
    });
});

it('должен отдать ошибку, если нет куки Session_id', async() => {
    delete req.cookies['Session_id'];
    publicApi
        .post('/1.0/auth/login-or-register-yandex')
        .reply(200, response200LoginResult());

    await de.run(page, { context });

    expect(res.statusCode).toEqual(302);
    expect(res.setHeader).toHaveBeenCalledWith('location', 'https://autoru_frontend.auth_domain/login/?r=https://auto.ru');
    expect(res.cookie).not.toHaveBeenCalled();
    expect(res.end).toHaveBeenCalledTimes(1);
});

it('должен отдать отдать найденные аккаунт, если они пришли от бека', async() => {
    publicApi
        .post('/1.0/auth/login-or-register-yandex')
        .reply(200, response200MatchedCredentials());

    await expect(
        de.run(page, { context }),
    ).resolves.toEqual({
        matched_credentials: {
            identity: [
                { phone: '70*******66', identity_token: '' },
                { email: 'doo**********x.ru', identity_token: '' },
            ],
        },
    });

    expect(res.cookie).not.toHaveBeenCalled();
    expect(res.end).not.toHaveBeenCalled();
});

it('должен средиректить в автору, если пришел 401 ответ (конфликт)', async() => {
    publicApi
        .post('/1.0/auth/login-or-register-yandex')
        .reply(401, response401());

    await de.run(page, { context });

    expect(res.statusCode).toEqual(302);
    expect(res.setHeader).toHaveBeenCalledWith('location', 'https://autoru_frontend.base_domain/');
    expect(res.cookie).not.toHaveBeenCalled();
    expect(res.end).toHaveBeenCalledTimes(1);
});

it('должен сделать перезапрос и средиректить в паспорт, если бек не отвечает', async() => {
    publicApi
        .post('/1.0/auth/login-or-register-yandex')
        .times(2)
        .reply(500);

    await de.run(page, { context });

    expect(res.statusCode).toEqual(302);
    expect(res.setHeader).toHaveBeenCalledWith('location', 'https://autoru_frontend.auth_domain/login/?r=https://auto.ru');
    expect(res.cookie).not.toHaveBeenCalled();
    expect(res.end).toHaveBeenCalledTimes(1);
});
