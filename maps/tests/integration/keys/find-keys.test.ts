import * as http from 'http';
import * as nock from 'nock';
import {app} from 'server/app';
import {config} from 'server/config';
import {URLSearchParams} from 'url';
import {
    findKeysXML,
    notFoundKeysXML,
    findKeysExpectedResult
} from 'server/tests/integration/fixtures/find-keys';
import {intHostConfigLoader} from 'server/libs/host-loader';
import {startServer, stopServer, client} from 'server/tests/integration/test-server';
import {KeyservSearchType, SearchType} from 'server/routers/keys/keyserv-types';
import {
    blackboxSessionidResponse,
    tvmIntResponse
} from 'server/tests/integration/fixtures/common-responses';

describe('/keys/find_keys', () => {
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
                action: KeyservSearchType.HOST,
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
        keyserv.reply(200, findKeysXML, {
            'Content-Type': 'text/xml; charset="utf-8"'
        });

        const response = await client.get(`${url}/api/keys/find_keys`, {
            searchParams: new URLSearchParams({
                searchType: SearchType.HOST,
                value: 'sample_host'
            })
        });

        expect(response.statusCode).toEqual(200);
        expect(response.body).toEqual(findKeysExpectedResult);
    });

    it('should return 200 and empty keylist for request to keyserv', async () => {
        keyserv.reply(200, notFoundKeysXML, {
            'Content-Type': 'text/xml; charset="utf-8"'
        });

        const response = await client.get(`${url}/api/keys/find_keys`, {
            searchParams: new URLSearchParams({
                searchType: SearchType.HOST,
                value: 'sample_host'
            })
        });

        expect(response.statusCode).toEqual(200);
        expect(response.body).toEqual([]);
    });

    it('should return 500 during XML parsing', async () => {
        keyserv.reply(200, '');

        const response = await client.get(`${url}/api/keys/find_keys`, {
            searchParams: new URLSearchParams({
                searchType: SearchType.HOST,
                value: 'sample_host'
            })
        });

        expect(response.statusCode).toEqual(500);
    });

    it('should return 400 after validation', async () => {
        const response = await client.get(`${url}/api/keys/find_keys`);
        expect(response.statusCode).toEqual(400);
    });
});
