const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const controller = require('./garage-promo');

let context;
let req;
let res;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен сделать редирект на лендинг гаража, если нет тачки в гараже', () => {
    publicApi
        .post('/1.0/garage/user/cards')
        .reply(200, {
            listing: [],
            status: 'SUCCESS',
        });

    const params = { type: 'fitservice' };

    return de.run(controller, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'GARAGE_IFRAME_PROMO_TO_LANDING',
                    id: 'REDIRECTED',
                    location: 'https://autoru_frontend.base_domain/garage/?promo=fitservice',
                },
            });
        });
});

it('отдаст 404, если неизвестная промка', () => {
    const params = { type: 'xxx' };

    return de.run(controller, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'NOT_FOUND',
                },
            });
        });
});
