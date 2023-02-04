import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import type { TDescriptContext } from 'auto-core/server/descript/createContext';
import createContext from 'auto-core/server/descript/createContext';
import publicApi from 'auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures';
import sessionFixtures from 'auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures';
import userFixtures from 'auto-core/server/resources/publicApiAuth/methods/user.nock.fixtures';

import type { THttpRequest, THttpResponse } from 'auto-core/http';

import controller from './gus-required';

let context: TDescriptContext;
let req: THttpRequest;
let res: THttpResponse;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('возвращает ответ, если пользователь не авторизован', async() => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.no_auth());

    publicApi
        .get('/1.0/user/')
        .reply(200, userFixtures.no_auth());

    await expect(
        de.run(controller, { context }),
    ).resolves.toMatchObject({
        session: {
            auth: false,
        },
    });
});

it('возвращает ответ, если пользователь авторизован без ГосУслуг', async() => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.user_auth());

    publicApi
        .get('/1.0/user/')
        .reply(200, userFixtures.user_auth());

    await expect(
        de.run(controller, { context }),
    ).resolves.toMatchObject({
        session: {
            auth: true,
            id: 'TEST_USER_ID',
        },
    });
});

it('возвращает ответ, если пользователь авторизован с неподтвержденными ГосУслугами', async() => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.user_auth_with_gosusligi_not_trusted());

    publicApi
        .get('/1.0/user/')
        .reply(200, userFixtures.user_auth_with_gosusligi_not_trusted());

    await expect(
        de.run(controller, { context }),
    ).resolves.toMatchObject({
        session: {
            auth: true,
            id: 'TEST_USER_ID',
        },
    });
});

it('возвращает редирект, если пользователь авторизован с подтвержденным ГосУслугами', async() => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.user_auth_with_gosusligi_trusted());

    publicApi
        .get('/1.0/user/')
        .reply(200, userFixtures.user_auth_with_gosusligi_trusted());

    await expect(
        de.run(controller, { context }),
    ).rejects.toMatchObject({
        error: {
            code: 'AUTH_GUS_SUCCESS',
            id: 'REDIRECTED',
            location: 'https://autoru_frontend.base_domain/info/gus-success/',
            status_code: 302,
        },
    });
});
