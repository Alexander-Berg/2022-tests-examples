/**
 * @jest-environment node
 */
const getCsvReport = require('./getCsvReport');

const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const telepony = require('auto-core/server/resources/telepony/getResource.nock.fixtures');

let context;
let req;
let res;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

const expectedReport = 'Date;Source;Proxy;Target;Wait;Talk\n' + '2020-01-13T14:23:22.679+03:00;+79696567184;+79167637557;+74951350605;00:03;00:07\n';

it('должен сформировать отчёт из ответа', () => {
    telepony
        .get('/api/2.x/auto-dealers/call/full')
        .reply(200, [
            {
                callResult: 'success',
                createTime: '2020-01-13T14:24:33.977+03:00',
                duration: 10,
                externalId: '46226287009',
                id: 'jYoMmaN8NZo',
                objectId: 'dealer-10767',
                proxy: '+79167637557',
                recordId: 'jYoMmaN8NZo',
                redirectId: 'Z6_H4glw9rM',
                source: '+79696567184',
                tag: 'category=TRUCKS',
                talkDuration: 7,
                target: '+74951350605',
                time: '2020-01-13T14:23:22.679+03:00',
            },
        ]);

    const params = {
        __method: 'getTeleponyCallsListFull',
        __headers: [ 'Date', 'Source', 'Proxy', 'Target', 'Wait', 'Talk' ],
        __fields: [ 'time', 'source', 'proxy', 'target', 'waitDuration', 'talkDuration' ],
    };

    return de.run(getCsvReport, { context, params })
        .then((result) => {
            expect(result).toBe(expectedReport);
        });
});

it('должен вернуть ошибку, если нет данных', () => {
    telepony
        .get('/api/2.x/auto-dealers/call/full')
        .reply(200, []);

    const params = {
        __method: 'getTeleponyCallsListFull',
        __headers: [ 'Date', 'Source', 'Proxy', 'Target', 'Wait', 'Talk' ],
        __fields: [ 'time', 'source', 'proxy', 'target', 'waitDuration', 'talkDuration' ],
    };

    return de.run(getCsvReport, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'NO_INPUT',
                    status_code: 204,
                    message: 'Нет данных для экспорта',
                },
            });
        });
});
