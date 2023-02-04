const de = require('descript');
const nock = require('nock');

const createContext = require('auto-core/server/descript/createContext');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const fixtures = require('./getCardsListing.fixtures');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const method = require('./getCardsListing');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен отдать нормальный ответ', async() => {
    publicApi
        .post('/1.0/garage/user/cards')
        .reply(200, fixtures.response200());

    await expect(
        de.run(method, { context }),
    ).resolves.toEqual(fixtures.response200());
});

it('должен отдать пустой листинг в случае 401 ответа', async() => {
    publicApi
        .post('/1.0/garage/user/cards')
        .reply(401, fixtures.response401());

    await expect(
        de.run(method, { context }),
    ).resolves.toEqual({ listing: [], status: 'SUCCESS' });
});

describe('перезапросы из-за ошибок', () => {
    it('нормальный ответ: 500 - 200', async() => {
        publicApi
            .post('/1.0/garage/user/cards')
            .times(1)
            .reply(500, fixtures.response500());

        publicApi
            .post('/1.0/garage/user/cards')
            .times(1)
            .reply(200, fixtures.response200());

        await expect(
            de.run(method, { context }),
        ).resolves.toEqual(fixtures.response200());
        expect(nock.isDone()).toEqual(true);
    });

    it('ошибка: 500 - 500', async() => {
        publicApi
            .post('/1.0/garage/user/cards')
            .times(2)
            .reply(500, fixtures.response500());

        await expect(
            de.run(method, { context }),
        ).rejects.toMatchObject({
            error: {
                body: { error: 'UNKNOWN_ERROR', status: 'ERROR' },
            },
        });
        expect(nock.isDone()).toEqual(true);
    });

    it('ошибка: TIMEOUT - TIMEOUT', async() => {
        publicApi
            .post('/1.0/garage/user/cards')
            .delay(2050)
            .times(2)
            .reply(200, fixtures.response200Empty());

        await expect(
            de.run(method, { context }),
        ).rejects.toMatchObject({
            error: { id: 'REQUEST_TIMEOUT' },
        });
        expect(nock.isDone()).toEqual(true);
    });

    it('ошибка: TIMEOUT - 200', async() => {
        publicApi
            .post('/1.0/garage/user/cards')
            .delay(2050)
            .times(1)
            .reply(200, fixtures.response200());

        publicApi
            .post('/1.0/garage/user/cards')
            .times(1)
            .reply(200, fixtures.response200());

        await expect(
            de.run(method, { context }),
        ).resolves.toEqual(fixtures.response200());
        expect(nock.isDone()).toEqual(true);
    });
});
