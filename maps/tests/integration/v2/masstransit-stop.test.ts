import * as nock from 'nock';
import {expect} from 'chai';
import {app} from 'app/app';
import {TestServer} from 'tests/integration/test-server';
import {TvmDaemon} from 'tests/integration/tvm-daemon';
import {withRecording} from 'tests/integration/nock-utils';
import {createResponseSchemaValidator} from 'tests/integration/response-schema-validator';

describe('GET /v2/masstransit/stop', () => {
    const validateResponseSchema = createResponseSchemaValidator({
        path: '/v2/masstransit/stop'
    });

    let testServer: TestServer;
    let tvmDaemon: TvmDaemon;

    before(async () => {
        nock.disableNetConnect();
        nock.enableNetConnect((host) => host.startsWith('127.0.0.1:') || host.startsWith('localhost'));

        [testServer, tvmDaemon] = await Promise.all([
            TestServer.start(app),
            TvmDaemon.start()
        ]);
    });

    after(async () => {
        await Promise.all([
            testServer.stop(),
            tvmDaemon.stop()
        ]);

        nock.enableNetConnect();
    });

    afterEach(() => {
        nock.cleanAll();
    });

    it('should return 400 for invalid "uri" parameter', async () => {
        const res = await withRecording('masstransit/stop/invalid-uri', () => {
            return testServer.request('/v2/masstransit/stop', {
                query: {
                    uri: 'http://example.com',
                    lang: 'ru_RU'
                },
                json: true
            });
        });
        expect(res.statusCode).to.equal(400);
    });

    it('should return 404 if stop not found', async () => {
        const res = await withRecording('masstransit/stop/unknown-stop', () => {
            return testServer.request('/v2/masstransit/stop', {
                query: {
                    uri: 'ymapsbm1://transit/stop?id=foobar',
                    lang: 'ru_RU'
                },
                json: true
            });
        });

        expect(res.statusCode).to.equal(404);
    });

    it('should return stop info for underground exit', async () => {
        const res = await withRecording('masstransit/stop/underground-exit', () => {
            return testServer.request('/v2/masstransit/stop', {
                query: {
                    uri: 'ymapsbm1://transit/stop?id=exit__22925',
                    lang: 'ru_RU'
                },
                json: true
            });
        });

        expect(res.statusCode).to.equal(200);
        validateResponseSchema(res);
        expect(res.body[0].properties.StopMetaData.Transport[0].Style.color).to.be.a('string');
    });

    it('should return stop info for urban stop', async () => {
        const res = await withRecording('masstransit/stop/urban-stop', () => {
            return testServer.request('/v2/masstransit/stop', {
                query: {
                    uri: 'ymapsbm1://transit/stop?id=stop__9646618',
                    lang: 'ru_RU'
                },
                json: true
            });
        });

        expect(res.statusCode).to.equal(200);
        validateResponseSchema(res);
    });

    // https://st.yandex-team.ru/MAPSHTTPAPI-1206
    it('should return timetable link for railway stop', async () => {
        const res = await withRecording('masstransit/stop/railway-stop', () => {
            return testServer.request('/v2/masstransit/stop', {
                query: {
                    uri: 'ymapsbm1://transit/stop?id=station__lh_9602258',
                    lang: 'ru_RU'
                },
                json: true
            });
        });

        expect(res.statusCode).to.equal(200);
        validateResponseSchema(res);
        const links = res.body[0].properties.StopMetaData.Links;
        expect(links).to.be.an('array');
        expect(links[0].type).to.equal('timetable');
        expect(links[0].href).to.be.a('string');
    });
});
