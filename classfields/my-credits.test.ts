import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import getBunkerDict from 'auto-core/lib/util/getBunkerDict';
import RedirectError from 'auto-core/lib/handledErrors/RedirectError';

import creditApplicationMock from 'auto-core/react/dataDomain/credit/mocks/creditApplication.mockchain';

import createContext from 'auto-core/server/descript/createContext';
import publicApi from 'auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures';
import sessionFixtures from 'auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures';

import { CreditApplicationState } from 'auto-core/types/TCreditBroker';
import type { THttpRequest, THttpResponse } from 'auto-core/http';

import controller from './my-credits';

jest.mock('auto-core/lib/util/getBunkerDict', () => jest.fn());

const getBunkerDictMock = getBunkerDict as jest.MockedFunction<typeof getBunkerDict>;
const creditApplicationDraft = creditApplicationMock().withState(CreditApplicationState.DRAFT).value();
const creditApplicationActive = creditApplicationMock().withState(CreditApplicationState.ACTIVE).value();

describe('контроллер my-credits', () => {
    let context: ReturnType<typeof createContext>;
    let req: THttpRequest;
    let res: THttpResponse;

    beforeEach(() => {
        req = createHttpReq();
        res = createHttpRes();
        context = createContext({ req, res });
    });

    it('редирект на обычную промку для незалогина', function() {

        publicApi
            .get(`/1.0/session/`)
            .reply(200, sessionFixtures.no_auth());

        return de.run(controller, { context })
            .then(
                () => Promise.reject('UNEXPECTED_RESOLVE'),
                (error) => {
                    expect(error).toMatchObject({
                        error: {
                            code: RedirectError.CODES.CREDITS_LK_ROOT_TO_PROMO,
                        },
                    });
                },
            );
    });

    it('редирект на специальную промку незалогина с включенной акцией', function() {
        getBunkerDictMock.mockImplementation((node) => {
            return node === 'finance/flags' ? { promo: true } : undefined;
        });

        publicApi
            .get(`/1.0/session/`)
            .reply(200, sessionFixtures.no_auth());

        return de.run(controller, { context })
            .then(
                () => Promise.reject('UNEXPECTED_RESOLVE'),
                (error) => {
                    expect(error).toMatchObject({
                        error: {
                            code: RedirectError.CODES.CREDITS_LK_ROOT_TO_SPECIAL_PROMO,
                        },
                    });
                },
            );
    });

    it('анкету в драфте редиректит на my-credits-draft', function() {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.user_auth())
            .get('/1.0/shark/credit-application/active?with_offers=true&with_person_profiles=true')
            .reply(200, { credit_application: creditApplicationDraft });

        return de.run(controller, { context })
            .then(
                () => Promise.reject('UNEXPECTED_RESOLVE'),
                (error) => {
                    expect(error).toMatchObject({
                        error: {
                            code: RedirectError.CODES.CREDITS_LK_ROOT_TO_DRAFT,
                        },
                    });
                },
            );
    });

    it('активную анкету редиректит на my-credits-active', function() {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.user_auth())
            .get('/1.0/shark/credit-application/active?with_offers=true&with_person_profiles=true')
            .reply(200, { credit_application: creditApplicationActive });

        return de.run(controller, { context })
            .then(
                () => Promise.reject('UNEXPECTED_RESOLVE'),
                (error) => {
                    expect(error).toMatchObject({
                        error: {
                            code: RedirectError.CODES.CREDITS_LK_ROOT_TO_ACTIVE,
                        },
                    });
                },
            );
    });
});
