const de = require('descript');
const nock = require('nock');
const os = require('os');

const HOSTNAME = os.hostname();

const createContext = require('auto-core/server/descript/createContext');
const baseHttpBlockPublicApi = require('./baseHttpBlockPublicApi');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let context;
let method;
let req;
let res;
beforeEach(() => {
    method = baseHttpBlockPublicApi({
        block: {
            pathname: '/testpath',
        },
    });

    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен добавить заголовки пабликапи и не перетереть базовые', () => {
    const scope = nock(`http://${ process.env.PUBLICAPI_HOSTNAME }`, {
        reqheaders: {
            'x-application-id': 'af-jest',
            'x-application-hostname': HOSTNAME,
            'x-descript2-controller': 'true',
            'x-request-id': 'jest-request-id',
            'x-authorization': process.env.PUBLICAPI_KEY,
        },
    })
        .get('/testpath')
        .reply(200, { status: 'OK' }, {
            'content-type': 'application/json',
        });

    return de.run(method, { context, params: {} })
        .then(() => {
            expect(scope.isDone()).toEqual(true);
        });
});

/*
it('должен добавить заголовки uaas, если они есть', () => {
    const publicApi = nock(`http://${ process.env.PUBLICAPI_HOSTNAME }`, {
        badheaders: [ 'x-badheader' ],
        reqheaders: {
            'x-application-id': 'af-jest',
            'x-authorization': 'Vertis autoru_frontend.publicapi.key',
            'x-yandex-header1': 'value1',
            'x-yandex-header2': 'value2',
        },
    })
        .get('/testpath')
        .reply(200, { status: 'OK' }, {
            'content-type': 'application/json',
        });

    req.experimentsData = {
        uaas: {
            rawHeaders: {
                'x-yandex-header1': 'value1',
                'x-yandex-header2': 'value2',
                'x-badheader': 'badvalue',
            },
        },
    };

    return de.run(method, { context })
        .then(() => {
            expect(publicApi.isDone()).toEqual(true);
        });
});
*/

it('не должен добавить заголовки uaas, если их нет', () => {
    const publicApi = nock(`http://${ process.env.PUBLICAPI_HOSTNAME }`, {
        badheaders: [ 'x-badheader', 'x-yandex-header1' ],
        reqheaders: {
            'x-application-id': 'af-jest',
            'x-authorization': process.env.PUBLICAPI_KEY,
        },
    })
        .get('/testpath')
        .reply(200, { status: 'OK' }, {
            'content-type': 'application/json',
        });

    return de.run(method, { context })
        .then(() => {
            expect(publicApi.isDone()).toEqual(true);
        });
});
