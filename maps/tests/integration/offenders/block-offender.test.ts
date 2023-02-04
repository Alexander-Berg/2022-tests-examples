import * as http from 'http';
import * as nock from 'nock';
import {app} from 'server/app';
import {config} from 'server/config';
import {
    tvmIntResponse,
    blackboxSessionidResponse
} from 'server/tests/integration/fixtures/common-responses';
import {intHostConfigLoader} from 'server/libs/host-loader';
import {correctSettedKeyXML} from 'server/tests/integration/fixtures/find-keys';
import {startServer, stopServer, client} from 'server/tests/integration/test-server';
import {OffenderActions, OffenderType} from 'server/routers/offenders/offender-types';

const blockOffenderByIPQuery = {
    action: OffenderActions.BLOCK,
    type: OffenderType.IP,
    aid: '1234',
    id: 'sample_ip',
    description: 'sample_description'
};

describe('/keyserv/settings', () => {
    let url: string;
    let keyservHost: string;
    let server: http.Server;

    let tvm: nock.Interceptor;
    let blackboxIntSessionid: nock.Interceptor;
    let blockOffenderByIP: nock.Interceptor;
    let blockOffenderByAPP: nock.Interceptor;
    let blockOffenderByURI: nock.Interceptor;

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);

        const {keyserv} = await intHostConfigLoader.get();
        keyservHost = keyserv;

        blackboxIntSessionid = nock(`http://${config['blackbox.internalHost']}`)
            .persist()
            .get('/blackbox')
            .query(true);

        tvm = nock(config['tvm.url'])
            .get(`/tvm/tickets`)
            .query(true);

        blockOffenderByIP = nock(keyservHost)
            .get(`/2.x/`)
            .query(blockOffenderByIPQuery);

        blockOffenderByURI = nock(keyservHost)
            .get(`/2.x/`)
            .query({
                ...blockOffenderByIPQuery,
                type: OffenderType.URI,
                id: 'sample_uri'
            });

        blockOffenderByAPP = nock(keyservHost)
            .get(`/2.x/`)
            .query({
                ...blockOffenderByIPQuery,
                type: OffenderType.APP,
                id: 'sample_appid'
            });
    });

    afterAll(async () => {
        await stopServer(server);
        nock.restore();
        nock.enableNetConnect();
    });

    beforeEach(() => {
        tvm.reply(200, tvmIntResponse);
        blackboxIntSessionid.reply(200, blackboxSessionidResponse);
    });

    afterEach(() => {
        nock.cleanAll();
    });

    describe('request to each searchType', () => {
        it('should return 200 for request to block offender by IP', async () => {
            blockOffenderByIP.reply(200, correctSettedKeyXML, {
                'Content-Type': 'text/xml; charset="utf-8"'
            });

            const response = await client.post(`${url}/api/offenders/block_offender`, {
                form: {
                    searchType: OffenderType.IP,
                    searchedTypeID: 'sample_ip',
                    description: 'sample_description'
                }
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body).toEqual({});
        });

        it('should return 200 for request to block offender by URI', async () => {
            blockOffenderByURI.reply(200, correctSettedKeyXML, {
                'Content-Type': 'text/xml; charset="utf-8"'
            });

            const response = await client.post(`${url}/api/offenders/block_offender`, {
                form: {
                    searchType: OffenderType.URI,
                    searchedTypeID: 'sample_uri',
                    description: 'sample_description'
                }
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body).toEqual({});
        });

        it('should return 200 for request to block offender by APP', async () => {
            blockOffenderByAPP.reply(200, correctSettedKeyXML, {
                'Content-Type': 'text/xml; charset="utf-8"'
            });

            const response = await client.post(`${url}/api/offenders/block_offender`, {
                form: {
                    searchType: OffenderType.APP,
                    searchedTypeID: 'sample_appid',
                    description: 'sample_description'
                }
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body).toEqual({});
        });
    });

    describe('error validation && parsing', () => {
        it('should return 500 during XML parsing', async () => {
            blockOffenderByAPP.reply(200, '', {
                'Content-Type': 'text/xml; charset="utf-8"'
            });

            const response = await client.post(`${url}/api/offenders/block_offender`, {
                form: {
                    searchType: OffenderType.APP,
                    searchedTypeID: 'sample_appid',
                    description: 'sample_description'
                }
            });

            expect(response.statusCode).toEqual(500);
        });

        it('should return 400 after validation', async () => {
            const response = await client.post(`${url}/api/offenders/block_offender`);
            expect(response.statusCode).toEqual(400);
        });
    });
});
