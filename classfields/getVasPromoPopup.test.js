const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const getVasPromoPopup = require('./getVasPromoPopup');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');
const getServiceDiscountFixtures = require('auto-core/server/resources/publicApiBilling/methods/getServiceDiscount.nock.fixture');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let context;
let req;
let res;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('вернет null если пользователь не авторизован', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.no_auth());

    publicApi
        .get('/1.0/billing/services/discount/all')
        .query({ category: 'all' })
        .reply(200, getServiceDiscountFixtures.with_active_discount());

    return de.run(getVasPromoPopup, { params: {}, context })
        .then((result) => {
            expect(result).toBeNull();
        });
});

it('вернет null если пользователь это дилер', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.client_auth());

    publicApi
        .get('/1.0/billing/services/discount/all')
        .query({ category: 'all' })
        .reply(200, getServiceDiscountFixtures.with_active_discount());

    return de.run(getVasPromoPopup, { params: {}, context })
        .then((result) => {
            expect(result).toBeNull();
        });
});

it('вернет скидку для авторизованного пользователя', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.user_auth());

    publicApi
        .get('/1.0/billing/services/discount/all')
        .reply(200, getServiceDiscountFixtures.with_active_discount());

    return de.run(getVasPromoPopup, { params: {}, context })
        .then((result) => {
            expect(result).not.toBeNull();
        });
});
