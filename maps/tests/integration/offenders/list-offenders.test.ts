import * as http from 'http';
import * as nock from 'nock';
import {app} from 'server/app';
import {URLSearchParams} from 'url';
import {
    foundOffendersByIpXML,
    foundOffendersByUriXML,
    foundOffendersByAppXML,
    notFoundOffendersByIpXML,
    foundOffendersByIpExpectedResult,
    foundOffendersByUriExpectedResult,
    foundOffendersByAppExpectedResult
} from 'server/tests/integration/fixtures/find-offenders';
import {intHostConfigLoader} from 'server/libs/host-loader';
import {startServer, stopServer, client} from 'server/tests/integration/test-server';
import {OffenderActions, OffenderType} from 'server/routers/offenders/offender-types';
import {config} from 'server/config';
import {
    blackboxSessionidResponse,
    tvmIntResponse
} from 'server/tests/integration/fixtures/common-responses';

describe('/keyserv/ settings', () => {
    let url: string;
    let server: http.Server;

    let listOffendersByIp: nock.Interceptor;
    let listOffendersByApp: nock.Interceptor;
    let listOffendersByUri: nock.Interceptor;

    let tvmInt: nock.Interceptor;
    let blackboxIntSessionid: nock.Interceptor;

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);

        const {keyserv: keyservHost} = await intHostConfigLoader.get();
        listOffendersByIp = nock(keyservHost)
            .get(`/2.x/`)
            .query({
                action: OffenderActions.LIST_OFFENDERS,
                type: OffenderType.IP
            });

        listOffendersByApp = nock(keyservHost)
            .get(`/2.x/`)
            .query({
                action: OffenderActions.LIST_OFFENDERS,
                type: OffenderType.APP
            });

        listOffendersByUri = nock(keyservHost)
            .get(`/2.x/`)
            .query({
                action: OffenderActions.LIST_OFFENDERS,
                type: OffenderType.URI
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

    describe('request to each searchType', () => {
        it('should return 200 for request to keyserv by URI', async () => {
            listOffendersByUri.reply(200, foundOffendersByUriXML, {
                'Content-Type': 'text/xml; charset="utf-8"'
            });

            const response = await client.get(`${url}/api/offenders/list_offenders`, {
                searchParams: new URLSearchParams({
                    searchType: OffenderType.URI
                })
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body).toEqual(foundOffendersByUriExpectedResult);
        });

        it('should return 200 for request to keyserv by APP', async () => {
            listOffendersByApp.reply(200, foundOffendersByAppXML, {
                'Content-Type': 'text/xml; charset="utf-8"'
            });

            const response = await client.get(`${url}/api/offenders/list_offenders`, {
                searchParams: new URLSearchParams({
                    searchType: OffenderType.APP
                })
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body).toEqual(foundOffendersByAppExpectedResult);
        });

        it('should return 200 for request to keyserv by IP', async () => {
            listOffendersByIp.reply(200, foundOffendersByIpXML, {
                'Content-Type': 'text/xml; charset="utf-8"'
            });

            const response = await client.get(`${url}/api/offenders/list_offenders`, {
                searchParams: new URLSearchParams({
                    searchType: OffenderType.IP
                })
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body).toEqual(foundOffendersByIpExpectedResult);
        });

        it('should return 200 and empty array for request to keyserv by IP', async () => {
            listOffendersByIp.reply(200, notFoundOffendersByIpXML);

            const response = await client.get(`${url}/api/offenders/list_offenders`, {
                searchParams: new URLSearchParams({
                    searchType: OffenderType.IP
                })
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body).toEqual([]);
        });
    });

    it('should return 400 after validation', async () => {
        const response = await client.get(`${url}/api/offenders/list_offenders`);
        expect(response.statusCode).toEqual(400);
    });
});
