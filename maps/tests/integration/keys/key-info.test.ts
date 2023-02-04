import * as http from 'http';
import * as nock from 'nock';
import {app} from 'server/app';
import {URLSearchParams} from 'url';
import {config} from 'server/config';
import {
    keyInfoXML,
    wrongKeyInfoXML,
    emptyKeyInfoXML,
    keyInfoExpectedResult,
    keyInfoWithoutStoplistXML,
    keyInfoRequiredParamsXML
} from 'server/tests/integration/fixtures/find-keys';
import {
    blackboxLoginValidResponse,
    blackboxSessionidResponse,
    tvmIntResponse,
    tvmResponse
} from 'server/tests/integration/fixtures/common-responses';
import {intHostConfigLoader} from 'server/libs/host-loader';
import {KeyservSearchType} from 'server/routers/keys/keyserv-types';
import {startServer, stopServer, client} from 'server/tests/integration/test-server';

describe('Get key info', () => {
    let url: string;
    let server: http.Server;

    let tvm: nock.Interceptor;
    let tvmInt: nock.Interceptor;
    let keyserv: nock.Interceptor;
    let blackboxUserinfo: nock.Interceptor;
    let blackboxIntSessionid: nock.Interceptor;

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);

        const {keyserv: keyservHost} = await intHostConfigLoader.get();

        blackboxUserinfo = nock(`http://${config['blackbox.host']}`)
            .get('/blackbox')
            .query(true);

        blackboxIntSessionid = nock(`http://${config['blackbox.internalHost']}`)
            .get(/blackbox\?method=sessionid/);

        tvm = nock(config['tvm.url'])
            .get(`/tvm/tickets?dsts=blackbox`);

        tvmInt = nock(config['tvm.url'])
            .get(`/tvm/tickets?dsts=blackboxInternal`);

        keyserv = nock(keyservHost)
            .get(`/2.x/`)
            .query({
                action: KeyservSearchType.KEY,
                key: 'sample_key'
            });
    });

    afterAll(async () => {
        await stopServer(server);
        nock.restore();
        nock.enableNetConnect();
    });

    beforeEach(() => {
        tvm.reply(200, tvmResponse);
        tvmInt.reply(200, tvmIntResponse);
        blackboxUserinfo.reply(200, blackboxLoginValidResponse);
        blackboxIntSessionid.reply(200, blackboxSessionidResponse);
    });

    afterEach(() => {
        nock.cleanAll();
    });

    it('should return 200 for request to keyserv', async () => {
        keyserv.reply(200, keyInfoXML, {
            'Content-Type': 'text/xml; charset="utf-8"'
        });

        const response = await client.get(`${url}/api/keys/key_info`, {
            searchParams: new URLSearchParams({
                key: 'sample_key'
            })
        });

        expect(response.statusCode).toEqual(200);
        expect(response.body).toEqual(keyInfoExpectedResult);
    });

    it('should return 200 for request without stoplist in XML', async () => {
        keyserv.reply(200, keyInfoWithoutStoplistXML, {
            'Content-Type': 'text/xml; charset="utf-8"'
        });

        const response = await client.get(`${url}/api/keys/key_info`, {
            searchParams: new URLSearchParams({
                key: 'sample_key'
            })
        });

        expect(response.statusCode).toEqual(200);
        expect(keyInfoExpectedResult).toMatchObject(response.body);
    });

    it('should return 200 for request without stoplist && restrictions in XML', async () => {
        keyserv.reply(200, keyInfoRequiredParamsXML, {
            'Content-Type': 'text/xml; charset="utf-8"'
        });

        const response = await client.get(`${url}/api/keys/key_info`, {
            searchParams: new URLSearchParams({
                key: 'sample_key'
            })
        });

        expect(response.statusCode).toEqual(200);
        expect(keyInfoExpectedResult).toMatchObject(response.body);
    });

    describe('error validation && parsing', () => {
        it('should return 404 when xml contains wrong fields', async () => {
            keyserv.reply(200, wrongKeyInfoXML, {
                'Content-Type': 'text/xml; charset="utf-8"'
            });

            const response = await client.get(`${url}/api/keys/key_info`, {
                searchParams: new URLSearchParams({
                    key: 'sample_key'
                })
            });

            expect(response.statusCode).toEqual(404);
        });

        it('should return 404 when key info is empty', async () => {
            keyserv.reply(200, emptyKeyInfoXML, {
                'Content-Type': 'text/xml; charset="utf-8"'
            });

            const response = await client.get(`${url}/api/keys/key_info`, {
                searchParams: new URLSearchParams({
                    key: 'sample_key'
                })
            });

            expect(response.statusCode).toEqual(404);
        });

        it('should return 404 when keyserv send not xml', async () => {
            keyserv.reply(200, '');

            const response = await client.get(`${url}/api/keys/key_info`, {
                searchParams: new URLSearchParams({
                    key: 'sample_key'
                })
            });

            expect(response.statusCode).toEqual(404);
        });

        it('should return 400 after validation', async () => {
            const response = await client.get(`${url}/api/keys/key_info`);
            expect(response.statusCode).toEqual(400);
        });
    });
});
