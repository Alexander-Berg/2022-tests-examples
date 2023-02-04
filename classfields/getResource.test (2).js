const de = require('descript');

const hydraNock = require('./getResource.nock.fixtures');

const createContext = require('auto-core/server/descript/createContext');

const hydra = require('./getResource');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let deContext;
let req;
let res;
let hydraMethod;
beforeEach(() => {
    hydraMethod = hydra({
        block: {
            pathname: '/testpath',
        },
    });

    req = createHttpReq();
    res = createHttpRes();
    deContext = createContext({ req, res });
});

describe('processError', () => {
    it('должен вернуть 0, если бек ответил 429', () => {
        hydraNock.get('/testpath').reply(429);

        return de.run(hydraMethod, { context: deContext }).then(result => {
            expect(result).toMatchObject({
                result: 0,
            });
        });
    });

    it('должен вернуть 1001, если бек ответил 500', () => {
        hydraNock.get('/testpath').reply(500);

        return de.run(hydraMethod, { context: deContext }).then(result => {
            expect(result).toMatchObject({
                result: 1001,
            });
        });
    });
});
