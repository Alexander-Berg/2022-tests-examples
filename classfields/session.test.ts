import de from 'descript';
import nock from 'nock';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import type { TDescriptContext } from 'auto-core/server/descript/createContext';
import createContext from 'auto-core/server/descript/createContext';
import publicApi from 'auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures';
import sessionFixtures from 'auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures';
import userFixtures from 'auto-core/server/resources/publicApiAuth/methods/user.nock.fixtures';

import type { THttpRequest, THttpResponse } from 'auto-core/http';

import block from './session';

let context: TDescriptContext;
let req: THttpRequest;
let res: THttpResponse;
const sessionId = Symbol('session');

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('для незалогина отдает результат сессии', async() => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.no_auth());

    publicApi
        .get('/1.0/user/')
        .reply(200, userFixtures.no_auth());

    const params = {};

    await expect(
        de.run(block(sessionId), { context, params }),
    ).resolves.toMatchObject({
        auth: false,
    });

    expect(nock.isDone()).toBe(true);
});

describe('дилеры', () => {
    it('без экспа редиректит в старую форму', async() => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.client_auth());

        publicApi
            .get('/1.0/user/')
            .reply(200, userFixtures.client_auth());

        const params = {};

        await expect(
            de.run(block(sessionId), { context, params }),
        ).rejects.toMatchObject({
            error: {
                code: 'POFFER_AWESOME_TO_AWFUL',
                id: 'REDIRECTED',
                location: 'https://autoru_frontend.base_domain/beta////',
                status_code: 302,
            },
        });

        expect(nock.isDone()).toBe(true);
    });

    describe('в экспе', () => {
        beforeEach(() => {
            (req.experimentsData.has as jest.MockedFunction<typeof req.experimentsData.has>).mockImplementationOnce(() => true);
            context = createContext({ req, res });
        });

        it('отдает сессию', async() => {
            publicApi
                .get('/1.0/session/')
                .reply(200, sessionFixtures.client_auth());

            publicApi
                .get('/1.0/user/')
                .reply(200, userFixtures.client_auth());

            const params = {};

            await expect(
                de.run(block(sessionId), { context, params }),
            ).resolves.toMatchObject({
                auth: true,
            });

            expect(nock.isDone()).toBe(true);
        });

        it('пользователю без доступа отдает 403', async() => {
            publicApi
                .get('/1.0/session/')
                .reply(200, sessionFixtures.client_auth_readonly());

            publicApi
                .get('/1.0/user/')
                .reply(200, userFixtures.client_auth_readonly());

            const params = {};

            await expect(
                de.run(block(sessionId), { context, params }),
            ).rejects.toMatchObject({
                error: {
                    id: 'Permission denied to OFFERS:Read for user:TEST_CLIENT_ID',
                    status_code: 403,
                },
            });

            expect(nock.isDone()).toBe(true);
        });
    });
});

describe('частники', () => {
    beforeEach(() => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.user_auth());

        publicApi
            .get('/1.0/user/')
            .reply(200, userFixtures.user_auth());
    });

    it('отдает результат сессии', async() => {

        const params = {};

        await expect(
            de.run(block(sessionId), { context, params }),
        ).resolves.toMatchObject({
            auth: true,
        });

        expect(nock.isDone()).toBe(true);
    });

    it('для новых тачек отдает 404', async() => {
        const params = {
            section: 'new',
        };

        await expect(
            de.run(block(sessionId), { context, params }),
        ).rejects.toMatchObject({
            error: {
                id: 'section "new" does not exist for private users',
                status_code: 404,
            },
        });

        expect(nock.isDone()).toBe(true);
    });
});
