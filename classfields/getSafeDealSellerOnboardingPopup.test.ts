import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import createContext from 'auto-core/server/descript/createContext';
import publicApi from 'auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures';
import sessionFixtures from 'auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures';

import getSafeDealSellerOnboardingPopup from './getSafeDealSellerOnboardingPopup';

let context: any;
let req;
let res;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
    context.res.cookie = jest.fn();
});

describe('возращает null', () => {
    it('если пользователь не авторизован', () => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.no_auth());

        return de.run(getSafeDealSellerOnboardingPopup, { params: {}, context })
            .then((result) => {
                expect(result).toBeNull();
            });
    });

    it('если пользователь - диллер', () => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.client_auth());

        return de.run(getSafeDealSellerOnboardingPopup, { params: {}, context })
            .then((result) => {
                expect(result).toBeNull();
            });
    });

    it('если нет куки', () => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.user_auth());

        return de.run(getSafeDealSellerOnboardingPopup, { params: {}, context })
            .then((result) => {
                expect(result).toBeNull();
            });
    });

    it('если в куке -1', () => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.user_auth());

        context.req.cookies = { safe_deal_seller_onboarding_promo: -1 };

        return de.run(getSafeDealSellerOnboardingPopup, { params: {}, context })
            .then((result) => {
                expect(result).toBeNull();
            });
    });

    it('и ставит куку, если в page_from = add-page', () => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.user_auth());

        return de.run(getSafeDealSellerOnboardingPopup, { params: { pageFrom: 'add-page' }, context })
            .then((result) => {
                expect(result).toBeNull();
                expect(context.res.cookie).toHaveBeenCalledWith('safe_deal_seller_onboarding_promo', 0);
            });
    });

    it('и НЕ ставит куку, если в page_from = add-page и есть кука', () => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.user_auth());

        context.req.cookies = { safe_deal_seller_onboarding_promo: -1 };

        return de.run(getSafeDealSellerOnboardingPopup, { params: { pageFrom: 'add-page' }, context })
            .then((result) => {
                expect(result).toBeNull();
                expect(context.res.cookie).not.toHaveBeenCalledWith('safe_deal_seller_onboarding_promo', 0);
            });
    });
});

it('возвращает пустой объект, если пользователь авторизон и в куке 0', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.user_auth());

    context.req.cookies = { safe_deal_seller_onboarding_promo: 0 };

    return de.run(getSafeDealSellerOnboardingPopup, { params: {}, context })
        .then((result) => {
            expect(result).toEqual({});
        });
});
