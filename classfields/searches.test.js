const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const block = require('./searches');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен ответить 500, если избранные ответили 500', () => {
    publicApi
        .get('/1.0/user/favorites/all/subscriptions')
        .query(() => true)
        .times(2)
        .reply(500);

    const params = {
        category: 'cars',
    };

    return de.run(block, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (error) => {
            expect(error).toMatchObject({
                error: {
                    id: 'REQUIRED_BLOCK_FAILED',
                },
            });
        });
});

it('должен ответить 200, если избранные ответили 200 и я не робот', () => {
    publicApi
        .get('/1.0/user/favorites/all/subscriptions')
        .reply(200, {
            saved_searches: [
                {
                    title: 'foo',
                    params: { },
                },
            ],
        });

    const params = {
        category: 'cars',
    };

    return de.run(block, { context, params }).then(
        (result) => {
            expect(result).toMatchObject({
                subscriptions: [
                    { title: 'foo' },
                ],
            });
        });
});

it('должен ответить 200, если избранные я робот и избранные заблокированы', () => {
    req.isRobot = true;
    const params = {
        category: 'cars',
    };

    return de.run(block, { context, params }).then(
        (result) => {
            expect(result).toMatchObject({
                subscriptions: [],
            });
        });
});
