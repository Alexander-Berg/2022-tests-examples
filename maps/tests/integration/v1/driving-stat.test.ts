import * as nock from 'nock';
import {expect} from 'chai';
import {app} from 'app/app';
import {TestServer} from 'tests/integration/test-server';
import {TvmDaemon} from 'tests/integration/tvm-daemon';
import {withRecording} from 'tests/integration/nock-utils';
import {createResponseSchemaValidator} from 'tests/integration/response-schema-validator';

const API_KEY = 'b027f76e-cc66-f012-4f64-696c7961c395';

describe('GET /stat/v1', () => {
    const validateResponseSchema = createResponseSchemaValidator({
        path: '/stat/v1'
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

    it('should return route stat info', async () => {
        const res = await withRecording('driving-stat/stat', () => {
            return testServer.request('/stat/v1', {
                query: {
                    lang: 'tr_TR',
                    rll: '28.9925717,41.0643083~28.989452,41.06712339962514',
                    apikey: API_KEY
                },
                json: true
            });
        });

        expect(res.statusCode).to.equal(200);
        validateResponseSchema(res);
    });

    // Issue: https://st.yandex-team.ru/MAPSHTTPAPI-1323
    it('should return 200 when route not found', async () => {
        const res = await withRecording('driving-stat/not-found', () => {
            return testServer.request('/stat/v1', {
                query: {
                    lang: 'tr_TR',
                    rll: '28.9634018,41.0028165~28.976713180541992,41.00751876831055',
                    apikey: API_KEY
                },
                json: true
            });
        });

        expect(res.statusCode).to.equal(200);
        validateResponseSchema(res);
        expect(res.body.properties.RouteMetaData.trafficLevel).to.equal(0, 'Invalid traffic level');
        expect(res.body.properties.RouteMetaData.Duration.value).to.equal(0, 'Invalid duration');
        expect(res.body.properties.RouteMetaData.DurationInTraffic.value).to.equal(0, 'Invalid duration in traffic');
        expect(res.body.properties.RouteMetaData.Distance.value).to.equal(0, 'Invalid distance');
    });
});
