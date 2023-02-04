const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const createContext = require('auto-core/server/descript/createContext');

const block = require('./deal');
const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');
const de = require('descript');
const RedirectError = require('auto-core/lib/handledErrors/RedirectError');
const getOfferCardFixtures = require('auto-core/server/resources/publicApiCard/methods/getOfferCard.fixtures');
const dealMock = require('auto-core/react/dataDomain/safeDeal/mocks/safeDeal.mock').default;

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('без авторизации должен редиректить на промо', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.no_auth());

    return de.run(block, { context }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: RedirectError.CODES.DEAL_LK_NOAUTH_TO_PROMO,
                    id: 'REDIRECTED',
                    location: '/promo/safe-deal/',
                    status_code: 302,
                },
            });
        });
});

it('должен вернуть правильную ошибку если сделки нет', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.client_auth());
    publicApi
        .get('/1.0/safe-deal/deal/get/123')
        .query(true)
        .reply(404, {});

    return de.run(block, { context, params: { deal_id: '123' } })
        .then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toEqual({ error: { id: 'SAFE_DEAL_NOT_FOUND', status_code: 404 } });
            },
        );
});

it('должен вернуть правильную ошибку если нет доступа к сделке', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.client_auth());
    publicApi
        .get('/1.0/safe-deal/deal/get/123')
        .query(true)
        .reply(403, {});

    return de.run(block, { context, params: { deal_id: '123' } })
        .then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toEqual({ error: { id: 'SAFE_DEAL_NOT_ALLOWED', status_code: 403 } });
            },
        );
});

describe('должен редиректить на страницу сделок если', () => {
    const expected = {
        error: {
            code: RedirectError.CODES.DEAL_IS_NOT_ACTIVE,
            id: 'REDIRECTED',
            location: 'https://autoru_frontend.base_domain/my/deals/',
            status_code: 302,
        },
    };

    it('не пришла сделка в ответе', () => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.client_auth());
        publicApi
            .get('/1.0/safe-deal/deal/get/123')
            .query(true)
            .reply(200, {
                offer: getOfferCardFixtures.privateCarsUsed().offer,
                deal: {},
            });

        return de.run(block, { context, params: { deal_id: '123' } })
            .then(
                () => Promise.reject('UNEXPECTED_RESOLVE'),
                (result) => {
                    expect(result).toMatchObject(expected);
                },
            );
    });

    it('в сделке шаг не DEAL_CONFIRMED/DEAL_COMPLETED/DEAL_INVITE_ACCEPTED', () => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.client_auth());
        publicApi
            .get('/1.0/safe-deal/deal/get/123')
            .query(true)
            .reply(200, {
                offer: getOfferCardFixtures.privateCarsUsed().offer,
                deal: {
                    ...dealMock.deal,
                    step: 'DEAL_CREATED',
                },
            });

        return de.run(block, { context, params: { deal_id: '123' } })
            .then(
                () => Promise.reject('UNEXPECTED_RESOLVE'),
                (result) => {
                    expect(result).toMatchObject(expected);
                },
            );
    });

    it('сделка заблокирована', () => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.client_auth());
        publicApi
            .get('/1.0/safe-deal/deal/get/123')
            .query(true)
            .reply(200, {
                offer: getOfferCardFixtures.privateCarsUsed().offer,
                deal: {
                    ...dealMock.deal,
                    is_locked: true,
                },
            });

        return de.run(block, { context, params: { deal_id: '123' } })
            .then(
                () => Promise.reject('UNEXPECTED_RESOLVE'),
                (result) => {
                    expect(result).toMatchObject(expected);
                },
            );
    });
});

it('должен вернуть блок safeDeal', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.client_auth());
    publicApi
        .get('/1.0/safe-deal/deal/get/123')
        .query(true)
        .reply(200, {
            offer: getOfferCardFixtures.privateCarsUsed().offer,
            deal: dealMock.deal,
        });

    return de.run(block, { context, params: { deal_id: '123' } })
        .then(
            (result) => {
                expect(result.safeDeal).toMatchSnapshot();
            },
        );
});
