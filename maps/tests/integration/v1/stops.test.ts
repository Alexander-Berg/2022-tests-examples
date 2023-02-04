import * as http from 'http';
import * as nock from 'nock';
import * as got from 'got';
import * as path from 'path';
import {readFileSync} from 'fs';
import {URLSearchParams} from 'url';
import {app} from 'app/app';
import {intHostConfigLoader} from 'app/lib/host-loader';
import {startServer, stopServer} from 'tests/integration/test-server';

const defaultQuery: any = {
    lang: 'ru_RU',
    origin: 'maps',
    ll: '37.590507,55.734048',
    spn: '0.001,0.001'
};

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json'
});

describe('/v1/stops', () => {
    let server: http.Server;
    let url: string;

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);
    });

    afterAll(async () => {
        await stopServer(server);
        nock.enableNetConnect();
    });

    it('should make requests to geosearch/mtinfo/mtrouter and give right response', async () => {
        const hosts = await intHostConfigLoader.get();
        nock(hosts.search)
            .get(
                '/yandsearch?ms=pb&snippets=masstransit%2F2.x' +
                '&ll=37.590507%2C55.734048&spn=0.001%2C0.001&origin=maps&lang=ru_RU'
            )
            .reply(200, readFileSync(path.resolve('./src/tests/integration/v1/fixtures/stops/geosearch.protobuf')));

        nock(hosts.mtinfo)
            .get(
                '/v2/stop?id=station__9858874%2Cstation__9858873%2C100078876%2Cstop__9648341%2C' +
                'stop__9644594%2Cstop__9645484%2Cstop__9645154%2Cstop__9644280&lang=ru_RU&origin=maps'
            )
            .reply(200, readFileSync(path.resolve('./src/tests/integration/v1/fixtures/stops/mtinfo.protobuf')));

        [
            '/pedestrian/v2/summary?rll=37.590507%2C55.734048~37.593365143%2C55.735123467&lang=ru_RU&origin=maps',
            '/pedestrian/v2/summary?rll=37.590507%2C55.734048~37.593250009%2C55.735337754&lang=ru_RU&origin=maps',
            '/pedestrian/v2/summary?rll=37.590507%2C55.734048~37.580583447%2C55.727395255&lang=ru_RU&origin=maps',
            '/pedestrian/v2/summary?rll=37.590507%2C55.734048~37.59327714%2C55.734353039&lang=ru_RU&origin=maps',
            '/pedestrian/v2/summary?rll=37.590507%2C55.734048~37.591608614%2C55.731888622&lang=ru_RU&origin=maps',
            '/pedestrian/v2/summary?rll=37.590507%2C55.734048~37.593812401%2C55.730820566&lang=ru_RU&origin=maps',
            '/pedestrian/v2/summary?rll=37.590507%2C55.734048~37.588988934%2C55.737598878&lang=ru_RU&origin=maps',
            '/pedestrian/v2/summary?rll=37.590507%2C55.734048~37.587217402%2C55.739142139&lang=ru_RU&origin=maps'
        ].forEach((url, index) => {
            nock(hosts.masstransit)
                .get(url)
                .reply(
                    200,
                    readFileSync(
                        path.resolve(`./src/tests/integration/v1/fixtures/stops/pedestrian0${index + 1}.protobuf`)
                    )
                );
        });

        const response = await client.get(`${url}/v1/stops`, {
            searchParams: new URLSearchParams(defaultQuery)
        });
        expect(response.statusCode).toEqual(200);
        expect(response.body).toEqual(JSON.parse(readFileSync(
            path.resolve('./src/tests/integration/v1/fixtures/stops/response.json'),
            'utf-8'
        )));
    });

    it('should throw error when origin param is not valid', async () => {
        const query = {...defaultQuery};
        delete query.origin;

        const response = await client.get(`${url}/v1/stops`, {
            searchParams: new URLSearchParams(query)
        });
        expect(response.statusCode).toEqual(400);
    });

    it('should throw error when ll param is not valid', async () => {
        const response = await client.get(`${url}/v1/stops`, {
            searchParams: new URLSearchParams({
                ...defaultQuery,
                ll: 'test'
            })
        });
        expect(response.statusCode).toEqual(400);
    });

    it('should throw error when spn param is not valid', async () => {
        const response = await client.get(`${url}/v1/stops`, {
            searchParams: new URLSearchParams({
                ...defaultQuery,
                spn: 'test'
            })
        });
        expect(response.statusCode).toEqual(400);
    });

    it('should throw error when lang param is not valid', async () => {
        const response = await client.get(`${url}/v1/stops`, {
            searchParams: new URLSearchParams({
                ...defaultQuery,
                lang: 'test'
            })
        });
        expect(response.statusCode).toEqual(400);
    });
});
