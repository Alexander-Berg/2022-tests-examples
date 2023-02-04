const de = require('descript');

const myMain = require('./my-main');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const userFixtures = require('auto-core/server/resources/publicApiAuth/methods/user.nock.fixtures');

let context;
let req;
let res;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('дилера отправит в кабинет', async() => {
    publicApi.get('/1.0/user/').reply(200, userFixtures.client_auth());
    publicApi.get('/1.0/user/offers/count/').reply(200, {});

    await expect(de.run(myMain, { context })).rejects.toMatchSnapshot();
});

it('обычного частника отправит в категорию all', async() => {
    publicApi.get('/1.0/user/').reply(200, userFixtures.user_auth());
    publicApi.get('/1.0/user/offers/count').reply(200, { status: 'SUCCESS', moto: 42 });

    await expect(de.run(myMain, { context })).rejects.toMatchSnapshot();
});

it('клиента у которого есть объявления и по запчастям, и по транспорту - отправит в категорию all', async() => {
    publicApi.get('/1.0/user/').reply(200, userFixtures.user_auth());
    publicApi.get('/1.0/user/offers/count').reply(200, { status: 'SUCCESS', moto: 42, parts: 42 });

    await expect(de.run(myMain, { context })).rejects.toMatchSnapshot();
});
