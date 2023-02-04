const de = require('descript');
const nock = require('nock');
const createContext = require('auto-core/server/descript/createContext');

const getResource = require('./getResource');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let context;
let method;
let req;
let res;
beforeEach(() => {
    method = getResource({
        block: {
            pathname: '/testpath',
        },
    });

    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен добавить заголовок x-autoru-operator-uid с id авторизованного пользователя', () => {
    nock('http://autoru-cabinet-api-http.vrts-slb.test.vertis.yandex.net', {
        reqheaders: {
            'x-autoru-operator-uid': '123',
        },
    })
        .get('/testpath')
        .reply(200, { status: 'OK' }, {
            'content-type': 'application/json',
        });

    req.session = {
        id: '123',
    };

    return de.run(method, { context, params: {} })
        .then(() => {
            expect(nock.isDone()).toEqual(true);
        });
});
