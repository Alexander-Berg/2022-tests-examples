const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const fixtures = require('./getCard.fixtures');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const method = require('./getCard');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен отдать нормальный ответ', () => {
    publicApi
        .get(`/1.0/garage/user/card/123`)
        .reply(200, fixtures.response200());

    const params = { card_id: 123 };
    return de.run(method, { context, params })
        .then((result) => {
            expect(result).toEqual(fixtures.response200());
        });
});

it('должен отдать ошибку { error: "CARD_NOT_FOUND" }, для удаленной карточки', () => {
    publicApi
        .get(`/1.0/garage/user/card/123`)
        .reply(200, fixtures.response200Deleted());

    const params = { card_id: 123 };
    return de.run(method, { context, params })
        .then((result) => {
            expect(result).toMatchObject({ error: 'CARD_NOT_FOUND', status: 'ERROR' });
        });
});

it('должен пробросить 401 ответ', () => {
    publicApi
        .get(`/1.0/garage/user/card/123`)
        .reply(401, fixtures.response401());

    const params = { card_id: 123 };
    return de.run(method, { context, params })
        .then((result) => {
            expect(result).toMatchObject({ error: 'NO_AUTH', status: 'ERROR' });
        });
});

it('должен пробросить 404 ответ', () => {
    publicApi
        .get(`/1.0/garage/user/card/123`)
        .reply(404, fixtures.response404());

    const params = { card_id: 123 };
    return de.run(method, { context, params })
        .then((result) => {
            expect(result).toMatchObject({ error: 'CARD_NOT_FOUND', status: 'ERROR' });
        });
});
