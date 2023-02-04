const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const method = require('./getAnonDraft');
const methodFixtures = require('./getAnonDraft.fixtures');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

describe('обработка ошибок', () => {
    it('должен считать 200 за успешный ответ', async() => {
        publicApi
            .get('/1.0/reviews/auto/anon-draft')
            .reply(200, methodFixtures.response200());

        await expect(
            de.run(method, { context }),
        ).resolves.toMatchObject({
            review: {
                id: '123',
            },
        });
    });

    it('должен считать 404 за успешный ответ', async() => {
        publicApi
            .get('/1.0/reviews/auto/anon-draft')
            .reply(404, methodFixtures.response404());

        await expect(
            de.run(method, { context }),
        ).resolves.toMatchObject({
            error: 'REVIEW_NOT_FOUND',
            status: 'ERROR',
        });
    });

    it('должен считать 500 за неуспешный ответ', async() => {
        publicApi
            .get('/1.0/reviews/auto/anon-draft')
            .times(2)
            .reply(500, methodFixtures.response500());

        await expect(
            de.run(method, { context }),
        ).rejects.toMatchObject({
            error: {
                body: {
                    error: 'UNKNOWN_ERROR',
                    status: 'ERROR',
                },
                id: 'HTTP_500',
                status_code: 500,
            },
        });
    });
});
