import * as nock from 'nock';
import {expect} from 'chai';
import * as jspath from 'jspath';
import {app} from 'app/app';
import {TestServer} from 'tests/integration/test-server';
import {TvmDaemon} from 'tests/integration/tvm-daemon';
import {withRecording} from 'tests/integration/nock-utils';
import {createResponseSchemaValidator} from 'tests/integration/response-schema-validator';

describe('/v2/router', () => {
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

    describe('GET /v2/router/driving/route', () => {
        const validateResponseSchema = createResponseSchemaValidator({
            path: '/v2/router/driving/route'
        });

        it('should build router with two points', async () => {
            const res = await withRecording('router/driving/two-points', () => {
                return testServer.request('/v2/router/driving/route', {
                    query: {
                        lang: 'ru_RU',
                        rll: '37.70019877343748,55.86603295361316~37.604068402343735,55.74692664107045'
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
        });

        it('should build route with jams', async () => {
            const res = await withRecording('router/driving/jams', () => {
                return testServer.request('/v2/router/driving/route', {
                    query: {
                        lang: 'ru_RU',
                        rll: '37.70019877343748,55.86603295361316~37.604068402343735,55.74692664107045',
                        jams: '1'
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            for (const route of res.body.features) {
                for (const path of route.features) {
                    expect(path.properties).to.have.property('jams');
                }
            }
            validateResponseSchema(res);
        });

        it('should build router with three points', async () => {
            const res = await withRecording('router/driving/three-points', () => {
                return testServer.request('/v2/router/driving/route', {
                    query: {
                        lang: 'ru_RU',
                        rll: '37.70019877343748,55.86603295361316~37.604068402343735,55.74692664107045~' +
                            '37.7056919375,55.65852114087166'
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
        });

        // Issue: https://st.yandex-team.ru/MAPSHTTPAPI-1292
        it('should allow request with one waypoint', async () => {
            const res = await withRecording('router/driving/one-point', () => {
                return testServer.request('/v2/router/driving/route', {
                    query: {
                        lang: 'ru_RU',
                        rll: '37.565587,55.743588'
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
            expect(res.body.features).to.have.length(0, 'Invalid number of routers in response');
            expect(res.body.properties.RouterMetaData.Waypoints).to.have
                .length(1, 'Invalid number of waypoints in RouterMetaData');
        });

        it('should return number of routes corresponding to "results" parameter', async () => {
            const res = await withRecording('router/driving/results-limit', () => {
                return testServer.request('/v2/router/driving/route', {
                    query: {
                        lang: 'ru_RU',
                        rll: '37.70019877343748,55.86603295361316~37.604068402343735,55.74692664107045',
                        results: 1
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
            expect(res.body.features).to.have.length(1, 'Invalid number of routes in response');
        });

        it('should build router avoid traffic', async () => {
            const res = await withRecording('router/driving/no-traffic', () => {
                return testServer.request('/v2/router/driving/route', {
                    query: {
                        lang: 'ru_RU',
                        rll: '37.70019877343748,55.86603295361316~37.604068402343735,55.74692664107045',
                        rtm: 'dtr'
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
        });

        it('should build route with via points', async () => {
            const res = await withRecording('router/driving/via', () => {
                return testServer.request('/v2/router/driving/route', {
                    query: {
                        lang: 'ru_RU',
                        rll: '37.565587,55.743588~37.615524,55.756964~37.62272,55.756802~37.58667,55.733984',
                        via: '1'
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
            expect(res.body.properties.RouterMetaData.Waypoints).to.have
                .length(3, 'Invalid number of waypoints in RouterMetaData');
            expect(jspath.apply('.features[0].features.features.geometry.geometries{.type == "Point"}', res.body))
                .to.have.length(1, 'Invalid number of via points in segments');
        });

        it('should compute segment turn angle', async () => {
            const res = await withRecording('router/driving/segment-angle', () => {
                return testServer.request('/v2/router/driving/route', {
                    query: {
                        lang: 'ru_RU',
                        //  "метро Киевская", "Кутузовский проспект, 9к1"
                        rll: '37.565587,55.743588~37.561203,55.748478',
                        rtm: 'dtr'
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
            const path = res.body.features[0].features[0];
            expect(path.features[1].properties.SegmentMetaData.angle).to.be
                .within(-95, -85, 'Invalid angle between first and second segments');
            expect(path.features[2].properties.SegmentMetaData.angle).to.be
                .within(90, 100, 'Invalid angle between second and third segments');
        });
    });

    describe('GET /v2/router/masstransit/route', () => {
        const validateResponseSchema = createResponseSchemaValidator({
            path: '/v2/router/masstransit/route'
        });

        it('should build router with two points', async () => {
            const res = await withRecording('router/masstransit/two-points', () => {
                return testServer.request('/v2/router/masstransit/route', {
                    query: {
                        lang: 'ru_RU',
                        rll: '37.70019877343748,55.86603295361316~37.604068402343735,55.74692664107045'
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
        });

        it('should build router with three points', async () => {
            const res = await withRecording('router/masstransit/three-points', () => {
                return testServer.request('/v2/router/masstransit/route', {
                    query: {
                        lang: 'ru_RU',
                        rll: '37.70019877343748,55.86603295361316~37.604068402343735,55.74692664107045~' +
                            '37.7056919375,55.65852114087166'
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
        });

        it('should return number of routes corresponding to "results" parameter', async () => {
            const res = await withRecording('router/masstransit/results-limit', () => {
                return testServer.request('/v2/router/masstransit/route', {
                    query: {
                        lang: 'ru_RU',
                        rll: '37.70019877343748,55.86603295361316~37.604068402343735,55.74692664107045',
                        results: 1
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
            expect(res.body.features).to.have.length(1, 'Invalid number of routes in response');
        });

        it('should exclude via indexes from waypoints', async () => {
            const res = await withRecording('router/masstransit/via', () => {
                return testServer.request('/v2/router/masstransit/route', {
                    query: {
                        lang: 'ru_RU',
                        rll: '37.565587,55.743588~37.615524,55.756964~37.62272,55.756802~37.58667,55.733984',
                        via: '1',
                        results: 1
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
            expect(res.body.properties.RouterMetaData.Waypoints).to.have
                .length(3, 'Invalid number of waypoints in response');
            expect(res.body.features[0].features).to.have.length(2, 'Invalid number of paths in response');
        });
    });

    describe('GET /v2/router/pedestrian/route', () => {
        const validateResponseSchema = createResponseSchemaValidator({
            path: '/v2/router/pedestrian/route'
        });

        it('should build router with two points', async () => {
            const res = await withRecording('router/pedestrian/two-points', () => {
                return testServer.request('/v2/router/pedestrian/route', {
                    query: {
                        lang: 'ru_RU',
                        rll: '37.70019877343748,55.86603295361316~37.604068402343735,55.74692664107045'
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
        });

        it('should build router with three points', async () => {
            const res = await withRecording('router/pedestrian/three-points', () => {
                return testServer.request('/v2/router/pedestrian/route', {
                    query: {
                        lang: 'ru_RU',
                        rll: '37.70019877343748,55.86603295361316~37.604068402343735,55.74692664107045~' +
                            '37.7056919375,55.65852114087166'
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
        });

        it('should return number of routes corresponding to "results" parameter', async () => {
            const res = await withRecording('router/pedestrian/results-limit', () => {
                return testServer.request('/v2/router/pedestrian/route', {
                    query: {
                        lang: 'ru_RU',
                        rll: '37.70019877343748,55.86603295361316~37.604068402343735,55.74692664107045',
                        results: 1
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
            expect(res.body.features).to.have.length(1, 'Invalid number of routes in response');
        });

        it('should exclude via indexes from waypoints', async () => {
            const res = await withRecording('router/pedestrian/via', () => {
                return testServer.request('/v2/router/pedestrian/route', {
                    query: {
                        lang: 'ru_RU',
                        rll: '37.565587,55.743588~37.615524,55.756964~37.62272,55.756802~37.58667,55.733984',
                        via: '1',
                        results: 1
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
            expect(res.body.properties.RouterMetaData.Waypoints).to.have
                .length(3, 'Invalid number of waypoints in response');
            expect(res.body.features[0].features).to.have.length(2, 'Invalid number of paths in response');
        });
    });

    describe('GET /v2/router/bicycle/route', () => {
        const validateResponseSchema = createResponseSchemaValidator({
            path: '/v2/router/bicycle/route'
        });

        it('should build router with two points', async () => {
            const res = await withRecording('router/bicycle/two-points', () => {
                return testServer.request('/v2/router/bicycle/route', {
                    query: {
                        lang: 'ru_RU',
                        rll: '37.70019877343748,55.86603295361316~37.604068402343735,55.74692664107045'
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
        });

        it('should build router with three points', async () => {
            const res = await withRecording('router/bicycle/three-points', () => {
                return testServer.request('/v2/router/bicycle/route', {
                    query: {
                        lang: 'ru_RU',
                        rll: '37.70019877343748,55.86603295361316~37.604068402343735,55.74692664107045~' +
                            '37.7056919375,55.65852114087166'
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
        });

        it('should return number of routes corresponding to "results" parameter', async () => {
            const res = await withRecording('router/bicycle/results-limit', () => {
                return testServer.request('/v2/router/bicycle/route', {
                    query: {
                        lang: 'ru_RU',
                        rll: '37.70019877343748,55.86603295361316~37.604068402343735,55.74692664107045',
                        results: 1
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
            expect(res.body.features).to.have.length(1, 'Invalid number of routes in response');
        });

        it('should exclude via indexes from waypoints', async () => {
            const res = await withRecording('router/bicycle/via', () => {
                return testServer.request('/v2/router/bicycle/route', {
                    query: {
                        lang: 'ru_RU',
                        rll: '37.565587,55.743588~37.615524,55.756964~37.62272,55.756802~37.58667,55.733984',
                        via: '1',
                        results: 1
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);
            expect(res.body.properties.RouterMetaData.Waypoints).to.have
                .length(3, 'Invalid number of waypoints in response');
            expect(res.body.features[0].features).to.have.length(2, 'Invalid number of paths in response');
        });

        // Issue https://st.yandex-team.ru/MAPSHTTPAPI-1249
        it('should return path distance greater or equal than segments distance', async () => {
            const res = await withRecording('router/bicycle/path-distance', () => {
                return testServer.request('/v2/router/bicycle/route', {
                    query: {
                        lang: 'ru_RU',
                        rll: '37.565587,55.743588~37.62272,55.756802~37.58667,55.733984',
                        results: 1
                    },
                    json: true
                });
            });

            expect(res.statusCode).to.equal(200);
            validateResponseSchema(res);

            const paths = res.body.features[0].features;
            expect(paths[0].properties.PathMetaData.Distance.value).to.be.at.least(
                paths[0].features[0].properties.SegmentMetaData.Distance.value,
                'Invalid distance of first path'
            );
            expect(paths[1].properties.PathMetaData.Distance.value).to.be.at.least(
                paths[1].features[0].properties.SegmentMetaData.Distance.value,
                'Invalid distance of second path'
            );
        });
    });
});
