import Ajv from 'ajv';
import * as http from 'http';
import * as got from 'got';
import nock from 'nock';
import {app} from 'app/app';
import {TestDb} from 'tests/test-db';
import {fixtures} from 'tests/fixtures/db';
import {startServer, stopServer} from 'tests/test-server';
import {logger} from 'app/lib/logger';
import SEGMENT_LIST_SCHEMA from 'tests/integration/v3/ajv-schemas/get-segment-list.json';
import {ResponseBody} from 'tests/types';

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'json'
});

const ajv = new Ajv({
    nullable: true
});

describe('/v3/edit', () => {
    const testDb = new TestDb();
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
        await testDb.clean();
        await testDb.loadFixtures(fixtures);
    });

    afterEach(() => {
        nock.cleanAll();
    });

    describe('/v3/edit/segment/list', () => {
        it('should return segments list', async () => {
            const {statusCode, body} = await client.get<ResponseBody>(`${url}/v3/edit/segment/list`, {
                searchParams: {
                    limit: 2
                }
            });

            expect(statusCode).toEqual(200);
            expect(body.data.length).toBeGreaterThan(0);

            const valid = ajv.validate(SEGMENT_LIST_SCHEMA, body.data);
            if (!valid) {
                logger.error(ajv.errorsText());
            }

            expect(valid).toBeTruthy();
        });
    });
});
