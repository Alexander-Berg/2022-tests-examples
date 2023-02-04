const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const method = require('./getUserReview');
const methodFixtures = require('./getUserReview.fixtures');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

describe('обработка ошибок', () => {
    let params;
    beforeEach(() => {
        params = { review_id: 123 };
    });

    it('должен считать 200 за успешный ответ', async() => {
        publicApi
            .get('/1.0/user/reviews/123')
            .reply(200, methodFixtures.response200());

        await expect(
            de.run(method, { context, params }),
        ).resolves.toMatchObject({
            review: {
                id: '123',
            },
        });
    });

    it('должен считать 401 за успешный ответ', async() => {
        publicApi
            .get('/1.0/user/reviews/123')
            .reply(401, methodFixtures.response401());

        await expect(
            de.run(method, { context, params }),
        ).resolves.toMatchObject({
            error: 'NO_AUTH',
            status: 'ERROR',
        });
    });

    it('должен считать 404 за успешный ответ', async() => {
        publicApi
            .get('/1.0/user/reviews/123')
            .reply(404, methodFixtures.response404());

        await expect(
            de.run(method, { context, params }),
        ).resolves.toMatchObject({
            error: 'REVIEW_NOT_FOUND',
            status: 'ERROR',
        });
    });

    it('должен считать 500 за неуспешный ответ', async() => {
        publicApi
            .get('/1.0/user/reviews/123')
            .times(2)
            .reply(500, methodFixtures.response500());

        await expect(
            de.run(method, { context, params }),
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
