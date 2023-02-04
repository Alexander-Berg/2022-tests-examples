const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');

const getPromoPopup = require('./getPromoPopup');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let deContext;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    deContext = createContext({ req, res });
});

it('должен вернуть пустой объект, если пользователь не авторизован', () => {
    publicApi.get('/1.0/session/').reply(200, sessionFixtures.no_auth());

    return de.run(getPromoPopup, { context: deContext }).then(result => {
        expect(result).toEqual({});
    });
});
