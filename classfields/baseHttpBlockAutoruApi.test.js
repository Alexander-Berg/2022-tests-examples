/**
 * @jest-environment node
 */

const de = require('descript');
const nock = require('nock');
const os = require('os');

const HOSTNAME = os.hostname();

const appConfig = require('../../appConfig');
const createContext = require('../../descript/createContext');
const baseHttpBlockAutoruApi = require('./baseHttpBlockAutoruApi');

const createHttpReq = require('../../../mocks/createHttpReq');
const createHttpRes = require('../../../mocks/createHttpRes');

let context;
let method;
let req;
let res;
beforeEach(() => {
    method = baseHttpBlockAutoruApi({
        block: {
            pathname: '/testpath',
        },
    });

    req = createHttpReq();
    res = createHttpRes();
    context = createContext({
        req: req,
        res: res,
    });
});

it('должен добавить заголовки пабликапи и не перетереть базовые', () => {
    const scope = nock(`${ appConfig.autoruApi.protocol }//${ appConfig.autoruApi.hostname }`, {
        reqheaders: {
            'x-application-id': 'vertis-front-chat',
            'x-application-hostname': HOSTNAME,
            'x-descript2-controller': 'true',
            'x-request-id': 'jest-request-id',
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

it('должен добавить заголовки uaas, если они есть', () => {
    const autoruApi = nock(`${ appConfig.autoruApi.protocol }//${ appConfig.autoruApi.hostname }`, {
        badheaders: [ 'x-badheader' ],
        reqheaders: {
            'x-application-id': 'vertis-front-chat',
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
            expect(autoruApi.isDone()).toEqual(true);
        });
});

it('не должен добавить заголовки uaas, если их нет', () => {
    const autoruApi = nock(`${ appConfig.autoruApi.protocol }//${ appConfig.autoruApi.hostname }`, {
        badheaders: [ 'x-badheader', 'x-yandex-header1' ],
        reqheaders: {
            'x-application-id': 'vertis-front-chat',
        },
    })
        .get('/testpath')
        .reply(200, { status: 'OK' }, {
            'content-type': 'application/json',
        });

    return de.run(method, { context })
        .then(() => {
            expect(autoruApi.isDone()).toEqual(true);
        });
});


it('должен прокинуть заголовок "x-yandex-antirobot-degradation" для пабликапи', () => {
    const scope = nock(`${ appConfig.autoruApi.protocol }//${ appConfig.autoruApi.hostname }`, {
        reqheaders: {
            'x-yandex-antirobot-degradation': '1',
        },
    })
        .get('/testpath')
        .reply(200, { status: 'OK' }, {
            'content-type': 'application/json',
        });

    req.headers['x-yandex-antirobot-degradation'] = '1';

    return de.run(method, { context, params: {} })
        .then(() => {
            expect(scope.isDone()).toEqual(true);
        });
});
