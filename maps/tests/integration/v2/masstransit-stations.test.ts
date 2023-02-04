/* eslint-disable camelcase */
import * as nock from 'nock';
import * as got from 'got';
import {expect} from 'chai';
import {app} from 'app/app';
import {intHostsLoader} from 'app/middleware/hosts';
import {TestServer} from 'tests/integration/test-server';
import {loadFixture} from 'tests/integration/load-fixture';

describe('GET /v2/masstransit/stations', () => {
    let intHosts: Record<string, string>;
    let testServer: TestServer;

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    function getStations(options?: Partial<got.GotJSONOptions>): got.GotPromise<any> {
        return testServer.request('/v2/masstransit/stations', {
            ...options,
            json: true
        });
    }

    function mockScheduleApi(): nock.Interceptor {
        return nock(intHosts.raspApi).get('/v3/nearest_stations/');
        // reqheaders: {
        //     'authorization': process.env.SCHEDULE_API_KEY
        // }
        // });
    }

    before(async () => {
        nock.disableNetConnect();
        nock.enableNetConnect((host) => host.startsWith('127.0.0.1:') || host.startsWith('localhost'));

        intHosts = await intHostsLoader.get();
        testServer = await TestServer.start(app);
    });

    after(async () => {
        await testServer.stop();

        nock.enableNetConnect();
    });

    afterEach(() => {
        nock.cleanAll();
    });

    it('should return 500 if schedule api returns 500', async () => {
        const scope = mockScheduleApi()
            .query({
                lang: 'ru_RU',
                distance: '1',
                format: 'json',
                lng: '37.6577',
                lat: '55.776461',
                station_types: 'train_station,bus_stop'
            })
            .reply(500);

        const res = await getStations({
            query: {
                lang: 'ru_RU',
                ll: '37.657700,55.776461',
                st: 'train_station,bus_stop',
                distance: 1
            }
        });
        expect(res.statusCode).to.equal(500);

        scope.done();
    });

    it('should return 500 if schedule api returns 400', async () => {
        const scope = mockScheduleApi()
            .query({
                lang: 'ru_RU',
                distance: '1',
                format: 'json',
                lng: '37.6577',
                lat: '55.776461',
                station_types: 'train_station,bus_stop'
            })
            .reply(400);

        const res = await getStations({
            query: {
                lang: 'ru_RU',
                ll: '37.657700,55.776461',
                st: 'train_station,bus_stop',
                distance: 1
            }
        });
        expect(res.statusCode).to.equal(500);

        scope.done();
    });

    it('should not call schedule api if request validation failed', async () => {
        const scope = mockScheduleApi()
            .query(true)
            .reply(500);

        const res = await getStations({
            query: {
                lang: 'ru_RU',
                ll: '37.657700,55.776461',
                st: 'train_station,bus_stop',
                distance: 100
            }
        });
        expect(res.statusCode).to.equal(400);
        expect(scope.isDone()).to.equal(false, 'Schedule API must not be called');
    });

    it('should return nearest stations on success', async () => {
        const apiResponse = loadFixture('schedule-api-response.json');

        const scope = mockScheduleApi()
            .query({
                lang: 'ru_RU',
                distance: '1',
                format: 'json',
                lng: '37.6577',
                lat: '55.776461',
                station_types: 'train_station,bus_stop'
            })
            .reply(200, apiResponse);

        const res = await getStations({
            query: {
                lang: 'ru_RU',
                ll: '37.657700,55.776461',
                st: 'train_station,bus_stop',
                distance: 1
            }
        });

        expect(res.statusCode).to.equal(200);
        expect(res.body).to.deep.equal(apiResponse.stations);

        scope.done();
    });

    it('should check cors');
});
