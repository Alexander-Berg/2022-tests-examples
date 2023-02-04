import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import createContext from 'auto-core/server/descript/createContext';
import publicApi from 'auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures';
import sessionFixtures from 'auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures';

import getSafeDealPromo from './getSafeDealPromo';

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
    it('если нет кнопочки сделочки', () => {
        return de.run(getSafeDealPromo, { params: { routeName: 'card', hasSafeDealButton: false }, context })
            .then((result) => {
                expect(result).toBeNull();
            });
    });

    it('если страница не card', () => {
        return de.run(getSafeDealPromo, { params: { routeName: 'listing', hasSafeDealButton: true }, context })
            .then((result) => {
                expect(result).toBeNull();
            });
    });

    it('если есть сделки', () => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.client_auth());

        publicApi
            .get('/1.0/safe-deal/deal/list')
            .reply(200, { dealList: [ {} ] });

        return de.run(getSafeDealPromo, { params: { routeName: 'card', hasSafeDealButton: true }, context })
            .then((result) => {
                expect(result).toBeNull();
            });
    });

    it('и ставит куку 0, если куки не было', () => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.client_auth());

        publicApi
            .get('/1.0/safe-deal/deal/list')
            .reply(200, { dealList: [] });

        return de.run(getSafeDealPromo, { params: { routeName: 'card', hasSafeDealButton: true }, context })
            .then((result) => {
                expect(context.res.cookie).toHaveBeenCalledWith('safe_deal_promo', 0);
                expect(result).toBeNull();
            });
    });

    it('и инкрементирует куку, если в куке 0-1', () => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.client_auth());

        publicApi
            .get('/1.0/safe-deal/deal/list')
            .reply(200, { dealList: [] });

        context.req.cookies = { safe_deal_promo: 1 };

        return de.run(getSafeDealPromo, { params: { routeName: 'card', hasSafeDealButton: true }, context })
            .then((result) => {
                expect(context.res.cookie).toHaveBeenCalledWith('safe_deal_promo', 2);
                expect(result).toBeNull();
            });
    });
});

it('возвращает пустой объект если в куке 2, нет сделок и есть кнопка сделок', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.client_auth());

    publicApi
        .get('/1.0/safe-deal/deal/list')
        .reply(200, { dealList: [ ] });

    context.req.cookies = { safe_deal_promo: 2 };

    return de.run(getSafeDealPromo, { params: { routeName: 'card', hasSafeDealButton: true }, context })
        .then((result) => {
            expect(result).toEqual({});
        });
});
