const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');
const getCardsListingFixtures = require('auto-core/server/resources/publicApiGarage/methods/getCardsListing.fixtures');

const controller = require('./garage');

const TEST_VIN = 'TEST_VIN';
let context;
let req;
let res;

const PARAMS = {
    filters: { status: [ 'ACTIVE' ], card_type: [ 'CURRENT_CAR', 'DREAM_CAR', 'EX_CAR' ] },
    sorting: 'CREATION_DATE',
    pagination: { page: 1, page_size: 20 },
};

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен отдать лендинг, если листинг ответил 200 и список пустой', () => {
    publicApi
        .post('/1.0/garage/user/cards', PARAMS)
        .reply(200, getCardsListingFixtures.response200Empty());

    const params = { card_id: '58133405' };

    return de.run(controller, { context, params }).then(
        (result) => {
            expect(result).toMatchObject({
                garage: {
                    listing: [],
                    status: 'SUCCESS',
                },
            });
        });
});

it('должен отдать лендинг, если листинг ответил 401', () => {
    publicApi
        .post('/1.0/garage/user/cards', PARAMS)
        .reply(401, getCardsListingFixtures.response401());

    const params = { card_id: '58133405' };

    return de.run(controller, { context, params }).then(
        (result) => {
            expect(result).toMatchObject({
                garage: {
                    listing: [],
                    status: 'SUCCESS',
                },
            });
        });
});

it('должен сделать редирект в карточку, если листинг непустой', () => {
    publicApi
        .post('/1.0/garage/user/cards', PARAMS)
        .reply(200, getCardsListingFixtures.response200());

    const params = { card_id: '58133405' };

    return de.run(controller, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'GARAGE_LISTING_TO_CARD',
                    id: 'REDIRECTED',
                    location: '/garage/58133405/?metrika=true',
                },
            });
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
                    code: 'GARAGE_TO_CABINET',
                    id: 'REDIRECTED',
                    location: 'https://cabinet.autoru_frontend.base_domain',
                },
            });
        });
});

it('должен сделать редирект на промку', () => {
    publicApi
        .post(`/1.0/garage/user/card/identifier/${ TEST_VIN }`)
        .reply(200, {});

    const params = { promo: 'fitservice', vin_or_licence_plate: TEST_VIN };

    return de.run(controller, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'GARAGE_LANDING_TO_PROMO',
                    id: 'REDIRECTED',
                    location: 'https://autoru_frontend.base_domain/promo/garage-fitservice/?redirect=false',
                },
            });
        });
});
