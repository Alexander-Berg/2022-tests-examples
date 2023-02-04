const de = require('descript');
const nock = require('nock');
const os = require('os');

const HOSTNAME = os.hostname();

const createContext = require('auto-core/server/descript/createContext');
const baseHttpBlock = require('./baseHttpBlock');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let context;
let method;
let req;
let res;
beforeEach(() => {
    method = baseHttpBlock({
        block: {
            hostname: 'auto.ru',
            pathname: '/testpath',
        },
    });

    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен добавить базовые заголовки', () => {
    const scope = nock('http://auto.ru', {
        reqheaders: {
            'x-application-id': 'af-jest',
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

it('должен добавить заголовок с таймаутом, если он есть в декларации блока', () => {
    const scope = nock('http://auto.ru', {
        reqheaders: {
            'x-client-timeout-ms': '1000',
        },
    })
        .get('/testpath')
        .reply(200, { status: 'OK' }, {
            'content-type': 'application/json',
        });

    const blockWithTimeout = method({
        block: {
            timeout: 1000,
        },
    });

    return de.run(blockWithTimeout, { context }).then(() => {
        expect(scope.isDone()).toEqual(true);
    });
});

it('должен бросить ошибку, если в path есть ".."', () => {
    const blockWithParamsInPath = method({
        block: {
            pathname: ({ params }) => `/testpath/${ params.id }`,
        },
    });

    return de.run(blockWithParamsInPath, {
        context,
        params: { id: '../iamhacker' },
    }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toEqual({
                error: {
                    id: 'UNEXPECTED_DIRECTORY_TRAVERSAL',
                },
            });
        });
});

describe('обработка пустых сообщений', () => {
    it('должен декодировать пустое body для application/json в null', () => {
        const scope = nock('http://auto.ru')
            .get('/testpath')
            .reply(200, Buffer.from([]), {
                'content-type': 'application/json',
                'x-proto-name': 'auto.api.shark.RichCreditApplicationResponse',
            });

        return de.run(method, { context, params: {} })
            .then((result) => {
                expect(scope.isDone()).toEqual(true);
                expect(result.result).toBeNull();
            });
    });

    it('должен декодировать пустое protobuf-сообщение в {}', () => {
        const scope = nock('http://auto.ru')
            .get('/testpath')
            .reply(200, Buffer.from([]), {
                'content-type': 'application/protobuf',
                'x-proto-name': 'auto.api.shark.RichCreditApplicationResponse',
            });

        return de.run(method, { context, params: {} })
            .then((result) => {
                expect(scope.isDone()).toEqual(true);
                expect(result.result).toEqual({});
            });
    });

    it('должен декодировать пустое body для text/plain в пустую строку', () => {
        const scope = nock('http://auto.ru')
            .get('/testpath')
            .reply(200, Buffer.from([]), {
                'content-type': 'text/plain',
            });

        return de.run(method, { context, params: {} })
            .then((result) => {
                expect(scope.isDone()).toEqual(true);
                expect(result.result).toEqual('');
            });
    });
});
