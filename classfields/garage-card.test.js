const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const getCardFixtures = require('auto-core/server/resources/publicApiGarage/methods/getCard.fixtures');
const getCardsListingFixtures = require('auto-core/server/resources/publicApiGarage/methods/getCardsListing.fixtures');

const controller = require('./garage-card');

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

it('должен отдать карточку, если ответили листинг и карточка', () => {
    publicApi
        .post('/1.0/garage/user/cards', PROPS)
        .reply(200, getCardsListingFixtures.response200());

    publicApi
        .get('/1.0/garage/user/card/58133405')
        .reply(200, getCardFixtures.response200());

    const params = { card_id: '58133405' };

    return de.run(controller, { context, params }).then(
        (result) => {
            expect(result).toMatchObject({
                garage: {
                    status: 'SUCCESS',
                },
                garageCard: {
                    card: { id: '58133405' },
                },
            });
        });
});

it('должен сделать редирект на морду гаража, если карточка ответила 401', () => {
    publicApi
        .post('/1.0/garage/user/cards', PROPS)
        .reply(200, getCardsListingFixtures.response200());

    publicApi
        .get('/1.0/garage/user/card/58133405')
        .reply(401, getCardFixtures.response401());

    const params = { card_id: '58133405' };

    return de.run(controller, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'GARAGE_CARD_TO_SHARED',
                    id: 'REDIRECTED',
                    location: 'https://autoru_frontend.base_domain/garage/share/58133405/',
                },
            });
        });
});

it('должен сделать редирект на морду гаража, если карточка ответила 404', () => {
    publicApi
        .post('/1.0/garage/user/cards', PROPS)
        .reply(200, getCardsListingFixtures.response200());

    publicApi
        .get('/1.0/garage/user/card/58133405')
        .reply(404, getCardFixtures.response404);

    const params = { card_id: '58133405' };

    return de.run(controller, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'GARAGE_CARD_TO_SHARED',
                    id: 'REDIRECTED',
                    location: 'https://autoru_frontend.base_domain/garage/share/58133405/',
                },
            });
        });
});
