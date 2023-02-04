const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const getOfferCardFixtures = require('auto-core/server/resources/publicApiCard/methods/getOfferCard.fixtures');
const getRequisitesFixtures = require('auto-core/server/resources/publicApiUserOffers/methods/getRequisites.fixtures');
const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');

const controller = require('./form-edit-contacts');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен ответить 404, если /user/offers/requisites не ответила SUCCESS', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.client_auth());

    publicApi
        .get('/1.0/offer/cars/123-abc')
        .query(() => true)
        .reply(200, getOfferCardFixtures.dealerCarsUsedOwner());

    publicApi
        .get('/1.0/user/offers/cars/123-abc/requisites')
        .reply(404, getRequisitesFixtures.response404());

    const params = {
        parent_category: 'cars',
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

it('должен вернуть ответ, если /user/offers/requisites ответила SUCCESS', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.client_auth());

    publicApi
        .get('/1.0/offer/cars/123-abc')
        .query(() => true)
        .reply(200, getOfferCardFixtures.dealerCarsUsedOwner());

    publicApi
        .get('/1.0/user/offers/cars/123-abc/requisites')
        .reply(200, getRequisitesFixtures.response200());

    const params = {
        parent_category: 'cars',
        sale_id: '123-abc',
    };

    return de.run(controller, { context, params }).then(
        (result) => {
            expect(result).toMatchObject({
                user: {
                    id: 'TEST_USER_ID',
                },
                offer: {
                    offer: { id: '123-abc' },
                },
                requisites: {
                    status: 'SUCCESS',
                    phones: [
                        { phone: '79876543210' },
                    ],
                },
            });
        });
});
