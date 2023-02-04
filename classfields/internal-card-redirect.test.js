const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const getOfferCardFixtures = require('auto-core/server/resources/publicApiCard/methods/getOfferCard.fixtures');

const controller = require('./internal-card-redirect');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен ответить 404, если все категории ответили 404', () => {
    publicApi
        .get('/1.0/offer/cars/123-abc')
        .query(() => true)
        .reply(404, getOfferCardFixtures.response404());

    publicApi
        .get('/1.0/offer/moto/123-abc')
        .query(() => true)
        .reply(404, getOfferCardFixtures.response404());

    publicApi
        .get('/1.0/offer/trucks/123-abc')
        .query(() => true)
        .reply(404, getOfferCardFixtures.response404());

    const params = {
        sale_id: '123-abc',
    };

    return de.run(controller, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'OFFER_NOT_FOUND',
                    status_code: 404,
                },
            });
        });
});

it('должен ответить 404, если все категории не ответили', () => {
    const params = {
        sale_id: '123-abc',
    };

    return de.run(controller, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'OFFER_NOT_FOUND',
                    status_code: 404,
                },
            });
        });
});

it('должен сделать редирект на карточку CARS, если она ответила', () => {
    publicApi
        .get('/1.0/offer/cars/123-abc')
        .query(() => true)
        .reply(200, getOfferCardFixtures.privateCarsUsed());

    const params = {
        sale_id: '123-abc',
    };

    return de.run(controller, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'RCARD',
                    id: 'REDIRECTED',
                    location: 'https://autoru_frontend.base_domain/cars/used/sale/mercedes/gl_klasse/123-abc/',
                    status_code: 302,
                },
            });
        });
});
