const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const getPromoAllFixtures = require('auto-core/server/resources/publicApiGarage/methods/getPromoAll.fixtures.js');
const getCardsListingFixtures = require('auto-core/server/resources/publicApiGarage/methods/getCardsListing.fixtures');
const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');

const controller = require('./garage-promo-all');

let context;
let req;
let res;

const PROPS = {
    filters: { status: [ 'ACTIVE' ], card_type: [ 'CURRENT_CAR', 'DREAM_CAR', 'EX_CAR' ] },
    sorting: 'CREATION_DATE',
    pagination: { page: 1, page_size: 20 },
};

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен отдать промки, если ответил листинг', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.user_auth());

    publicApi
        .post('/1.0/garage/user/cards', PROPS)
        .reply(200, getCardsListingFixtures.response200());

    publicApi
        .get('/1.0/garage/user/promos')
        .reply(200, getPromoAllFixtures.response200());

    return de.run(controller, { context }).then(
        (result) => {
            expect(result.garagePromoAll).toMatchObject(getPromoAllFixtures.response200());
        });
});

it('должен сделать редирект в кабинет дилера, если пришел дилер', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.client_auth());

    return de.run(controller, { context }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'GARAGE_PROMO_ALL_TO_CABINET',
                    id: 'REDIRECTED',
                    location: 'https://cabinet.autoru_frontend.base_domain',
                },
            });
        });
});
