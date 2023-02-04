import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import RedirectError from 'auto-core/lib/handledErrors/RedirectError';

import createContext from 'auto-core/server/descript/createContext';
import publicApi from 'auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures';
import sessionFixtures from 'auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures';

import type { THttpRequest, THttpResponse } from 'auto-core/http';

import controller from './my-credits-active';

describe('контроллер my-credits-active', () => {
    let context: ReturnType<typeof createContext>;
    let req: THttpRequest;
    let res: THttpResponse;

    beforeEach(() => {
        req = createHttpReq();
        res = createHttpRes();
        context = createContext({ req, res });
    });

    it('если нет активной анкеты, редиректит в my-credits', function() {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.user_auth())
            .get('/1.0/shark/credit-application/active?with_offers=true&with_person_profiles=true')
            .reply(200, {});

        return de.run(controller, { context })
            .then(
                () => Promise.reject('UNEXPECTED_RESOLVE'),
                (error) => {
                    expect(error).toMatchObject({
                        error: {
                            code: RedirectError.CODES.CREDITS_LK_ACTIVE_TO_ROOT,
                        },
                    });
                },
            );
    });
});
