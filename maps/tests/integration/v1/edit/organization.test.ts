import * as http from 'http';
import * as got from 'got';
import nock from 'nock';
import {promises} from 'fs';
import {app} from 'app/app';
import {intHostConfigLoader} from 'app/lib/host-loader';
import {startServer, stopServer} from 'tests/test-server';
import organization from 'tests/fixtures/integration/v1/organization/1094008369.json';

const ORG_ID = 1094008369;

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json'
});

describe('/v1/edit', () => {
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

    beforeEach(async () => {
        const {search} = await intHostConfigLoader.get();
        nock(search)
            .get('/yandsearch')
            .query({
                origin: 'discovery',
                lang: 'ru_RU',
                type: 'biz',
                format: 'json',
                snippets: [
                    'businessrating/1.x',
                    'masstransit/1.x',
                    'photos/2.x',
                    'cluster_permalinks',
                    'promo_mastercard/1.x:mastercardoffers'
                ].join(','),
                ms: 'pb',
                business_oid: ORG_ID
            })
            .reply(200, await promises.readFile(`src/tests/fixtures/backend/organizations/${ORG_ID}.protobuf`));
    });

    afterEach(() => {
        nock.cleanAll();
    });

    describe('/v1/edit/organization', () => {
        it('should return organization', async () => {
            const {statusCode, body} = await client.get(`${url}/v1/edit/organization?id=${ORG_ID}`);
            expect(statusCode).toEqual(200);
            expect(body).toEqual(organization);
        });
    });
});
