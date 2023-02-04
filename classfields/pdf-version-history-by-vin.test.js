const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const block = require('./pdf-version-history-by-vin');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

let context;
let req;
let res;

const HISTORY_ENTITY_ID = 'SOME_ID';
const SERVER_RESULT = {
    order: {
        status: 'SUCCESS',
        report_type: 'FULL_REPORT',
    },
    full_report: {
        status: 'UNTRUSTED',
    },
};

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен выбросить ошибку, если нет подходящего history entity id', async() => {
    await expect(
        de.run(block, { context, params: { haha: HISTORY_ENTITY_ID } }),
    ).rejects.toMatchObject({
        error: {
            id: 'NOT_FOUND',
            status_code: 404,
        },
    });
});

it('должен выбросить 403, подходящий history entity id, кривые данные', async() => {
    publicApi
        .get(`/1.0/carfax/orders/result?order_id=${ HISTORY_ENTITY_ID }`)
        .reply(200, SERVER_RESULT);

    await expect(
        de.run(block, { context, params: { order_id: HISTORY_ENTITY_ID } }),
    ).rejects.toMatchObject({
        error: {
            id: 'HTTP_403',
            status_code: 403,
        },
    });
});

it('отдельно обрабатываем 401 ошибку', async() => {
    publicApi
        .get(`/1.0/carfax/orders/result?order_id=${ HISTORY_ENTITY_ID }`)
        .reply(401, { error: 'HTTP_401' });

    await expect(
        de.run(block, { context, params: { order_id: HISTORY_ENTITY_ID } }),
    ).rejects.toMatchObject({
        error: {
            id: 'HTTP_401',
            status_code: 401,
        },
    });
});

it('отдельно обрабатываем 403 ошибку', async() => {
    publicApi
        .get(`/1.0/carfax/orders/result?order_id=${ HISTORY_ENTITY_ID }`)
        .reply(403, { error: 'HTTP_403' });

    await expect(
        de.run(block, { context, params: { order_id: HISTORY_ENTITY_ID } }),
    ).rejects.toMatchObject({
        error: {
            id: 'HTTP_403',
            status_code: 403,
        },
    });
});
