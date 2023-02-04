import * as http from 'http';
import * as nock from 'nock';
import {URLSearchParams} from 'url';
import {readFileSync} from 'fs';
import * as path from 'path';
import * as got from 'got';
import {app} from 'app/app';
import {startServer, stopServer} from 'tests/integration/test-server';
import {intHostConfigLoader} from 'app/lib/host-loader';

const TIMESTAMP_REGEXP = /^\d+$/;
const XML_JAMS_TIMESTAMP_REGEXP = /<r:jamsTimestamp>(.*)<\/r:jamsTimestamp>/;

const plainRouterUrl = '/v2/summary?rll=37.286151346057665%2C55.665010141044505~37.41175%2C55.767615&mode=nojams';
const jamsRouterUrl = '/v2/summary?rll=37.286151346057665%2C55.665010141044505~37.41175%2C55.767615&mode=approx';
const threePointsUrl = '/v2/summary?rll=37.286151346057665%2C55.665010141044505' +
    '~37.41175%2C55.767615~37.01%2C55.02&mode=nojams';
const defaultQuery: {rll?: string, format: 'json'} = {
    rll: '37.286151346057665,55.665010141044505~37.41175,55.767615',
    format: 'json'
};
const fixturesPath = './src/tests/integration/v1/fixtures/router';

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json'
});

describe('/v1/router', () => {
    let server: http.Server;
    let url: string;
    let routerHost: string;

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);

        const hosts = await intHostConfigLoader.get();
        routerHost = hosts.router;
    });

    afterAll(async () => {
        await stopServer(server);
        nock.enableNetConnect();
    });

    beforeEach(() => {
        nock(routerHost)
            .get(plainRouterUrl)
            .reply(200, readFileSync(path.resolve(`${fixturesPath}/route.protobuf`)));
    });

    afterEach(() => {
        nock.cleanAll();
    });

    it('should return right response in json format', async () => {
        const response = await client.get(`${url}/v1/router`, {
            responseType: 'json',
            searchParams: new URLSearchParams(defaultQuery)
        });
        expect(response.statusCode).toEqual(200);
        expect(getValidateJson(
            response.body,
            path.resolve(`${fixturesPath}/route.json`)
        )).toEqual(response.body);
        expect(response.headers['content-type']).toEqual('application/json; charset=utf-8');
    });

    it('should return right response in xml format', async () => {
        const response = await client.get(`${url}/v1/router`, {
            responseType: 'text',
            searchParams: new URLSearchParams({
                ...defaultQuery,
                format: 'xml'
            })
        });
        expect(response.statusCode).toEqual(200);
        expect(getValidateXml(
            response.body,
            path.resolve(`${fixturesPath}/route.xml`)
        )).toEqual(response.body);
        expect(response.headers['content-type']).toEqual('text/xml; charset=utf-8');
    });

    it('should also return right response in xml with upper-case format', async () => {
        const response = await client.get(`${url}/v1/router`, {
            responseType: 'text',
            searchParams: new URLSearchParams({
                ...defaultQuery,
                format: 'JSON'
            })
        });
        expect(response.statusCode).toEqual(200);
        expect(getValidateXml(
            response.body,
            path.resolve(`${fixturesPath}/route.xml`)
        )).toEqual(response.body);
        expect(response.headers['content-type']).toEqual('text/xml; charset=utf-8');
    });

    it('should also return right response in xml with unknown format', async () => {
        const response = await client.get(`${url}/v1/router`, {
            responseType: 'text',
            searchParams: new URLSearchParams({
                ...defaultQuery,
                format: 'unknown'
            })
        });
        expect(response.statusCode).toEqual(200);
        expect(getValidateXml(
            response.body,
            path.resolve(`${fixturesPath}/route.xml`)
        )).toEqual(response.body);
        expect(response.headers['content-type']).toEqual('text/xml; charset=utf-8');
    });

    it('should return right response with unknown parameters', async () => {
        const response = await client.get(`${url}/v1/router`, {
            responseType: 'json',
            searchParams: new URLSearchParams({
                ...defaultQuery,
                something: 'nothing',
                something_else: 'nothing_else'
            })
        });
        expect(response.statusCode).toEqual(200);
        expect(getValidateJson(
            response.body,
            path.resolve(`${fixturesPath}/route.json`)
        )).toEqual(response.body);
        expect(response.headers['content-type']).toEqual('application/json; charset=utf-8');
    });

    it('should return right response without output parameter', async () => {
        const response = await client.get(`${url}/v1/router`, {
            responseType: 'json',
            searchParams: new URLSearchParams(defaultQuery)
        });
        expect(response.statusCode).toEqual(200);
        expect(getValidateJson(
            response.body,
            path.resolve(`${fixturesPath}/route.json`)
        )).toEqual(response.body);
        expect(response.headers['content-type']).toEqual('application/json; charset=utf-8');
    });

    it('should return right response with unknown output parameter', async () => {
        const response = await client.get(`${url}/v1/router`, {
            responseType: 'json',
            searchParams: new URLSearchParams({
                ...defaultQuery,
                output: 'unknown'
            })
        });
        expect(response.statusCode).toEqual(200);
        expect(getValidateJson(
            response.body,
            path.resolve(`${fixturesPath}/route.json`)
        )).toEqual(response.body);
        expect(response.headers['content-type']).toEqual('application/json; charset=utf-8');
    });

    it('should return right response jams mode parameter', async () => {
        nock.cleanAll();
        nock(routerHost)
            .get(jamsRouterUrl)
            .reply(200, readFileSync(path.resolve(`${fixturesPath}/jams.protobuf`)));

        const response = await client.get(`${url}/v1/router`, {
            responseType: 'json',
            searchParams: new URLSearchParams({
                ...defaultQuery,
                mode: 'jams'
            })
        });
        expect(response.statusCode).toEqual(200);
        expect(getValidateJson(
            response.body,
            path.resolve(`${fixturesPath}/jams.json`)
        )).toEqual(response.body);
        expect(response.headers['content-type']).toEqual('application/json; charset=utf-8');
    });

    it('should return right response with unknown mode parameter', async () => {
        const response = await client.get(`${url}/v1/router`, {
            responseType: 'json',
            searchParams: new URLSearchParams({
                ...defaultQuery,
                mode: 'unknown'
            })
        });
        expect(response.statusCode).toEqual(200);
        expect(getValidateJson(
            response.body,
            path.resolve(`${fixturesPath}/route.json`)
        )).toEqual(response.body);
        expect(response.headers['content-type']).toEqual('application/json; charset=utf-8');
    });

    it('should return right response with more than two point in rll', async () => {
        nock.cleanAll();
        nock(routerHost)
            .get(threePointsUrl)
            .reply(200, readFileSync(path.resolve(`${fixturesPath}/three_points.protobuf`)));

        const response = await client.get(`${url}/v1/router`, {
            responseType: 'json',
            searchParams: new URLSearchParams({
                ...defaultQuery,
                rll: '37.286151346057665,55.665010141044505~37.41175,55.767615~37.01,55.02'
            })
        });
        expect(response.statusCode).toEqual(200);
        expect(getValidateJson(
            response.body,
            path.resolve(`${fixturesPath}/three_points.json`)
        )).toEqual(response.body);
        expect(response.headers['content-type']).toEqual('application/json; charset=utf-8');
    });

    it('should throw error with less than two point in rll', async () => {
        const response = await client.get(`${url}/v1/router`, {
            responseType: 'json',
            searchParams: new URLSearchParams({
                ...defaultQuery,
                rll: '37.286151346057665,55.665010141044505'
            })
        });
        expect(response.statusCode).toEqual(400);
    });

    it('should throw error without rll', async () => {
        const query = {...defaultQuery};
        delete query.rll;

        const response = await client.get(`${url}/v1/router`, {
            responseType: 'json',
            searchParams: new URLSearchParams(query)
        });
        expect(response.statusCode).toEqual(400);
    });
});

function getValidateJson(body: any, filePath: string) {
    const ts = body.properties.RouterRouteMetaData.jamsTimestamp;

    if (!ts.match(TIMESTAMP_REGEXP)) {
        return new Error('Incorrect timestamp');
    }

    return JSON.parse(readFileSync(filePath, 'utf8').replace('{{CurrentTimestamp}}', ts));
}

function getValidateXml(body: any, filePath: string) {
    const ts = body.match(XML_JAMS_TIMESTAMP_REGEXP)[1];

    if (!ts.match(TIMESTAMP_REGEXP)) {
        return new Error('Incorrect timestamp');
    }

    return readFileSync(filePath, 'utf8').replace('{{CurrentTimestamp}}', ts);
}
