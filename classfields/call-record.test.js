/**
 * @jest-environment node
 */
const callRecord = require('./call-record');

const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');
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

it('должен вызвать context.res.write с корректными параметрами', () => {
    const https = require('https');
    context.res.write = jest.fn();
    const buffer = Buffer.from([ 1, 2, 3 ]);
    https.request = jest.fn((options, callback) => callback({
        headers: {},
        on: jest.fn((id, callback) => {
            if (id === 'end') {
                callback();
            }
            if (id === 'data') {
                callback(buffer);
            }
        }),
    }));
    publicApi
        .post('/1.0/calltracking/call/record')
        .query({})
        .reply(200, { url: 'https://yandex.ru' });

    return de.run(callRecord(), { context })
        .then(() => {
            expect(context.res.write).toHaveBeenCalledWith(buffer);
        });
});

it('должен вызвать context.res.write с корректными параметрами, если передан заголовок range', () => {
    const https = require('https');
    context.res.write = jest.fn();
    context.res.setHeader = jest.fn();
    context.req.headers = { range: '0-1' };
    const buffer = Buffer.from([ 1, 2, 3 ]);
    https.request = jest.fn((options, callback) => callback({
        headers: {},
        on: jest.fn((id, callback) => {
            if (id === 'end') {
                callback();
            }
            if (id === 'data') {
                callback(buffer);
            }
        }),
    }));
    publicApi
        .post('/1.0/calltracking/call/record')
        .query({})
        .reply(200, { url: 'https://yandex.ru' });

    return de.run(callRecord(), { context })
        .then(() => {
            expect(context.res.write).toHaveBeenCalledWith(Buffer.from([ 1, 2 ]));
        });
});
