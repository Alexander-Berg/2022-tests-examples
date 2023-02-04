/* eslint-disable camelcase */
import * as nock from 'nock';
import {withRecording} from 'tests/integration/nock-utils';
import {expect} from 'chai';
import {app} from 'app/app';
import {TestServer} from 'tests/integration/test-server';
import {config} from 'app/config';
import {TvmDaemon} from 'tests/integration/tvm-daemon';

describe('GET /v1/staticmap/route', () => {
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

    it('returns 200 if all required params are present', async () => {
        const res = await withRecording('staticmap/route', () => {
            return testServer.request('/v1/staticmap/route', {
                query: {
                    rll: '37.516047,55.792923~37.730281,55.729428',
                    lang: 'ru_RU',
                    origin: 'test',
                    routing_mode: 'masstransit',
                    key: config['staticmap.key']
                }
            });
        });

        expect(res.statusCode).to.equal(200);
    });

    it('should return 400 if key is not provided', async () => {
        const res = await testServer.request('/v1/staticmap/route', {
            query: {
                rll: '37.516047,55.792923~37.730281,55.729428',
                lang: 'ru_RU',
                origin: 'test',
                routing_mode: 'route'
            }
        });

        expect(res.statusCode).to.equal(400);
    });

    it('should return 401 if key is wrong', async () => {
        const res = await testServer.request('/v1/staticmap/route', {
            query: {
                rll: '37.516047,55.792923~37.730281,55.729428',
                lang: 'ru_RU',
                origin: 'test',
                routing_mode: 'route',
                key: config['staticmap.key'] + 'wrong'
            }
        });

        expect(res.statusCode).to.equal(401);
    });

    it('handles not set `rll` param', async () => {
        const res = await testServer.request('/v1/staticmap/route', {
            query: {
                lang: 'ru_RU',
                origin: 'test',
                routing_mode: 'route',
                key: config['staticmap.key']
            }
        });

        expect(res.statusCode).to.equal(400);
    });

    it('handles not set `lang` param', async () => {
        const res = await testServer.request('/v1/staticmap/route', {
            query: {
                rll: '37.516047,55.792923~37.730281,55.729428',
                origin: 'test',
                routing_mode: 'route',
                key: config['staticmap.key']
            }
        });

        expect(res.statusCode).to.equal(400);
    });

    it('handles not set `origin` param', async () => {
        const res = await testServer.request('/v1/staticmap/route', {
            query: {
                rll: '37.516047,55.792923~37.730281,55.729428',
                lang: 'ru_RU',
                routing_mode: 'route',
                key: config['staticmap.key']
            }
        });

        expect(res.statusCode).to.equal(400);
    });

    it('handles not set `routing_mode` param', async () => {
        const res = await testServer.request('/v1/staticmap/route', {
            query: {
                rll: '37.516047,55.792923~37.730281,55.729428',
                lang: 'ru_RU',
                origin: 'test',
                key: config['staticmap.key']
            }
        });

        expect(res.statusCode).to.equal(400);
    });

    it('handles wrong `map_size` param', async () => {
        const res = await testServer.request('/v1/staticmap/route', {
            query: {
                rll: '37.516047,55.792923~37.730281,55.729428',
                lang: 'ru_RU',
                origin: 'test',
                routing_mode: 'route',
                map_size: '2001,1080',
                key: config['staticmap.key']
            }
        });

        expect(res.statusCode).to.equal(400);
    });

    it('handles wrong `map_scale` param', async () => {
        const res = await testServer.request('/v1/staticmap/route', {
            query: {
                rll: '37.516047,55.792923~37.730281,55.729428',
                lang: 'ru_RU',
                origin: 'test',
                routing_mode: 'route',
                map_scale: '10',
                key: config['staticmap.key']
            }
        });

        expect(res.statusCode).to.equal(400);
    });

    it('handles wrong `line_color` param', async () => {
        const res = await testServer.request('/v1/staticmap/route', {
            query: {
                rll: '37.516047,55.792923~37.730281,55.729428',
                lang: 'ru_RU',
                origin: 'test',
                routing_mode: 'route',
                line_color: 'fff',
                key: config['staticmap.key']
            }
        });

        expect(res.statusCode).to.equal(400);
    });

    it('handles wrong `show_jams` param', async () => {
        const res = await testServer.request('/v1/staticmap/route', {
            query: {
                rll: '37.516047,55.792923~37.730281,55.729428',
                lang: 'ru_RU',
                origin: 'test',
                routing_mode: 'route',
                show_jams: 'wrong',
                key: config['staticmap.key']
            }
        });

        expect(res.statusCode).to.equal(400);
    });

    it('handles wrong `markers` param', async () => {
        const res = await testServer.request('/v1/staticmap/route', {
            query: {
                rll: '37.516047,55.792923~37.730281,55.729428',
                lang: 'ru_RU',
                origin: 'test',
                routing_mode: 'route',
                markers: 'home,wrong',
                key: config['staticmap.key']
            }
        });

        expect(res.statusCode).to.equal(400);
    });
});
