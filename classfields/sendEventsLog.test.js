/* eslint-disable no-console */

const de = require('descript');
const promClient = require('prom-client');
const createContext = require('auto-core/server/descript/createContext');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const logFixtures = require('auto-core/server/resources/publicApiEvents/methods/log.nock.fixtures');
const eventParamsMock = require('./mock/eventParams.json');

const sendEventsLog = require('./sendEventsLog');

let context;
let mockParams;
let req;
let res;
beforeEach(() => {
    mockParams = {
        events: JSON.stringify(
            new Array(2).fill(eventParamsMock),
        ),
        geo_radius: 200,
        geo_id: [ 213 ],
    };
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
    console.error = jest.fn();
    promClient.register.resetMetrics();
});

// afterEach(() => {
//     console.log(promClient.register.getMetricsAsJSON());
// });

it('должен дернуть ручку events/log и вернуть ее ответ', () => {
    publicApi
        .post('/1.0/events/log')
        .reply(200, logFixtures.success());

    return de.run(sendEventsLog, {
        context,
        params: mockParams,
    }).then((result) => {
        expect(result).toMatchObject(logFixtures.success());
    });
});

it('должен посчитать отсутствие yandexuid', () => {
    publicApi
        .post('/1.0/events/log')
        .reply(200, logFixtures.success());

    req.uatraits.OSFamily = 'Jest OSFamily';
    req.uatraits.BrowserName = 'Jest BrowserName';

    return de.run(sendEventsLog, {
        context,
        params: mockParams,
    }).then(() => {
        return promClient.register.getSingleMetric('request_without_yandexuid').get();
    }).then((metrics) => {
        expect(metrics).toMatchObject({
            values: [ { value: 1, labels: { os: 'Jest OSFamily', browser: 'Jest BrowserName' } } ],
        });
    });
});

it('не должен посчитать отсутствие yandexuid, если он есть', () => {
    publicApi
        .post('/1.0/events/log')
        .reply(200, logFixtures.success());

    req.cookies.yandexuid = '123';
    req.uatraits.OSFamily = 'Jest OSFamily';
    req.uatraits.BrowserName = 'Jest BrowserName';

    return de.run(sendEventsLog, {
        context,
        params: mockParams,
    }).then(() => {
        return promClient.register.getSingleMetric('request_without_yandexuid').get();
    }).then((metrics) => {
        expect(metrics).toMatchObject({
            values: [],
        });
    });
});

it('если параметры не переданы, должен отработать guard и вывестиcь сообщение в консоль', () => {
    console.error = jest.fn();

    publicApi
        .post('/1.0/events/log')
        .reply(200, logFixtures.success());

    return de.run(sendEventsLog, {
        context,
        params: {},
    }).then(
        () => Promise.reject('ENEXPECTED_RESOLVE'),
        (error) => {
            expect(error).toMatchObject({
                error: {
                    id: 'BLOCK_GUARDED',
                },
            });
            expect(console.error).toHaveBeenCalledWith(
                // eslint-disable-next-line max-len
                '{"_request_id":"jest-request-id","_message":"EVENTS_LOG_FAILED","_context":"EVENTS_LOG_FAILED","error":{"error":{"id":"BLOCK_GUARDED"}},"request_params":{"body":{"events":null}}}',
            );
        },
    );
});

it('должен отработать guard, если запрос пришел от робота, ответ не является ошибкой и не логируется', () => {
    context.req.isRobot = true;

    return de.run(sendEventsLog, {
        context,
        params: mockParams,
    }).then((result) => {
        expect(result).toMatchObject({
            error: {
                id: 'BLOCK_GUARDED',
            },
        });
        expect(console.error).not.toHaveBeenCalled();

        return promClient.register.getSingleMetric('request_without_yandexuid').get();
    }).then((metrics) => {
        expect(metrics).toMatchObject({
            values: [],
        });
    });
});

it('если ручка ответила ошибкой (400), должен ответить ошибкой и залогировать ошибку', () => {
    publicApi
        .post('/1.0/events/log')
        .reply(400, logFixtures.error());

    return de.run(sendEventsLog, {
        context,
        params: mockParams,
    }).then(
        () => Promise.reject('ENEXPECTED_RESOLVE'),
        (error) => {
            expect(error.error.body).toMatchObject(logFixtures.error());
            expect(console.error).toHaveBeenCalledWith(
                // eslint-disable-next-line max-len
                '{"_request_id":"jest-request-id","_message":"EVENTS_LOG_FAILED","_context":"EVENTS_LOG_FAILED","error":{"error":{"id":"HTTP_400","status_code":400,"headers":{"content-type":"application/json"},"body":{"status":"ERROR","error":"BAD_REQUEST","detailed_error":"[502bff1555cc9c74] The request content was malformed."},"message":"Bad Request"}},"request_params":{"body":{"events":[{"timestamp":"2019-12-23T13:39:04.479Z","card_show_event":{"category":"CARS","card_id":"1092301084-9f60cbba","card_from":"MORDA","app_version":"development","testing_group":0},"web_referer":"https://auto.ru/","utm_content":"test"},{"timestamp":"2019-12-23T13:39:04.479Z","card_show_event":{"category":"CARS","card_id":"1092301084-9f60cbba","card_from":"MORDA","app_version":"development","testing_group":0},"web_referer":"https://auto.ru/","utm_content":"test"}]}}}',
            );
        },
    );
});

it('если ручка не ответила (таймаут), должен ответить ошибкой и залогировать ошибку', () => {
    publicApi
        .post('/1.0/events/log')
        .times(2)
        .delay(2000)
        .reply(200, logFixtures.success());

    return de.run(sendEventsLog, {
        context,
        params: mockParams,
    }).then(
        () => Promise.reject('ENEXPECTED_RESOLVE'),
        (error) => {
            expect(error).toMatchObject({
                error: {
                    id: 'REQUEST_TIMEOUT',
                },
            });
            expect(console.error).toHaveBeenCalledWith(
                // eslint-disable-next-line max-len
                '{"_request_id":"jest-request-id","_message":"EVENTS_LOG_FAILED","_context":"EVENTS_LOG_FAILED","error":{"error":{"id":"REQUEST_TIMEOUT"}},"request_params":{"body":{"events":[{"timestamp":"2019-12-23T13:39:04.479Z","card_show_event":{"category":"CARS","card_id":"1092301084-9f60cbba","card_from":"MORDA","app_version":"development","testing_group":0},"web_referer":"https://auto.ru/","utm_content":"test"},{"timestamp":"2019-12-23T13:39:04.479Z","card_show_event":{"category":"CARS","card_id":"1092301084-9f60cbba","card_from":"MORDA","app_version":"development","testing_group":0},"web_referer":"https://auto.ru/","utm_content":"test"}]}}}',
            );
        },
    );
});

it('если ручка ответила ошибкой (200), должен вернуть её ответ', () => {
    publicApi
        .post('/1.0/events/log')
        .reply(200, logFixtures.error());

    return de.run(sendEventsLog, {
        context,
        params: mockParams,
    }).then((result) => {
        expect(result).toMatchObject(logFixtures.error());
    });
});
