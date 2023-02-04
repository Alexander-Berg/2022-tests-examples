const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const getOfferCardFixtures = require('auto-core/server/resources/publicApiCard/methods/getOfferCard.fixtures');
const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');

const controller = require('./offer-photos-add');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен сделать редирект в ЛК, если нет прав на редактирование', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.client_auth());

    publicApi
        .get('/1.0/offer/cars/123-abc')
        .query(() => true)
        .reply(200, getOfferCardFixtures.privateCarsUsed());

    const params = {
        parent_category: 'cars',
        sale_id: '123-abc',
    };

    return de.run(controller, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'OFFER_PHOTO_ADD_TO_LK',
                    id: 'REDIRECTED',
                    location: 'https://autoru_frontend.base_domain/my/cars/',
                    status_code: 302,
                },
            });
        });
});

it('должен вернуть ответ, если есть права на редактирование', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.client_auth());

    publicApi
        .get('/1.0/offer/cars/123-abc')
        .query(() => true)
        .reply(200, getOfferCardFixtures.privateCarsUsedOwner());

    const params = {
        parent_category: 'cars',
        sale_id: '123-abc',
    };

    return de.run(controller, { context, params }).then(
        (result) => {
            expect(result).toMatchObject({
                session: {
                    id: 'TEST_USER_ID',
                },
                offer: {
                    offer: { id: '123-abc' },
                },
            });
        });
});
