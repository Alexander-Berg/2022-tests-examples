import * as http from 'http';
import * as nock from 'nock';
import {app} from 'server/app';
import {config} from 'server/config';
import {
    findKeysByCountXML,
    wrongFindKeysByCountXML,
    findKeysByCountExpectedResult
} from 'server/tests/integration/fixtures/find-keys';
import {intHostConfigLoader} from 'server/libs/host-loader';
import {KeyservSearchType} from 'server/routers/keys/keyserv-types';
import {startServer, stopServer, client} from 'server/tests/integration/test-server';
import {
    blackboxSessionidResponse,
    tvmIntResponse
} from 'server/tests/integration/fixtures/common-responses';

describe('/keys/find_keys_count_by_host', () => {
    let url: string;
    let server: http.Server;
    let keyserv: nock.Interceptor;
    let tvmInt: nock.Interceptor;
    let blackboxIntSessionid: nock.Interceptor;

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);

        const {keyserv: keyservHost} = await intHostConfigLoader.get();
        keyserv = nock(keyservHost)
            .get(`/2.x/`)
            .query({
                action: KeyservSearchType.HOST_COUNT,
                host: 'sample_host'
            });

        blackboxIntSessionid = nock(`http://${config['blackbox.internalHost']}`)
            .get(/blackbox\?method=sessionid/);

        tvmInt = nock(config['tvm.url'])
            .get(`/tvm/tickets`)
            .query(true);
    });

    afterAll(async () => {
        await stopServer(server);
        nock.restore();
        nock.enableNetConnect();
    });

    beforeEach(() => {
        blackboxIntSessionid.reply(200, blackboxSessionidResponse);
        tvmInt.reply(200, tvmIntResponse);
    });

    afterEach(() => {
        nock.cleanAll();
    });

    it('should return 200 for request to keyserv', async () => {
        keyserv.reply(200, findKeysByCountXML, {
            'Content-Type': 'text/xml; charset="utf-8"'
        });

        const response = await client.get(`${url}/api/keys/find_keys_count_by_host`, {
            searchParams: {
                host: 'sample_host'
            }
        });

        expect(response.statusCode).toEqual(200);
        expect(response.body).toEqual(findKeysByCountExpectedResult);
    });

    it('should return 500 during XML parsing', async () => {
        keyserv.reply(200, '');

        const response = await client.get(`${url}/api/keys/find_keys_count_by_host`, {
            searchParams: {
                host: 'sample_host'
            }
        });

        expect(response.statusCode).toEqual(500);
    });

    it('should return 500 during XML parsing', async () => {
        keyserv.reply(200, wrongFindKeysByCountXML);

        const response = await client.get(`${url}/api/keys/find_keys_count_by_host`, {
            searchParams: {
                host: 'sample_host'
            }
        });

        expect(response.statusCode).toEqual(500);
    });

    it('should return 400 after validation', async () => {
        const response = await client.get(`${url}/api/keys/find_keys_count_by_host`);
        expect(response.statusCode).toEqual(400);
    });
});
