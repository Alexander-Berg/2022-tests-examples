const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const method = require('./addCardByIdentifier');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен заэнкодить pathname', async() => {
    publicApi
        .post('/1.0/garage/user/card/identifier/%D0%9A753%D0%A2%D0%9E150')
        .reply(200, {});

    await expect(
        de.run(method, { context, params: { identifier: 'К753ТО150' } }),
    ).resolves.toEqual({});
});
