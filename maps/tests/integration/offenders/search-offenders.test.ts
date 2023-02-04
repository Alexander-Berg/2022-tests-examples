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
import {
    blackboxSessionidResponse,
    tvmIntResponse
} from 'server/tests/integration/fixtures/common-responses';
import {config} from 'server/config';

describe('/keyserv/ settings', () => {
    let url: string;
    let server: http.Server;

    let searchOffendersByIp: nock.Interceptor;
    let searchOffendersByApp: nock.Interceptor;
    let searchOffendersByUri: nock.Interceptor;

    let tvmInt: nock.Interceptor;
    let blackboxIntSessionid: nock.Interceptor;

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);

        const {keyserv: keyservHost} = await intHostConfigLoader.get();

        searchOffendersByIp = nock(keyservHost)
            .get(`/2.x/`)
            .query({
                action: OffenderActions.SEARCH_OFFENDERS,
                type: OffenderType.IP,
                query: '1.1.1.1'
            });

        searchOffendersByApp = nock(keyservHost)
            .get(`/2.x/`)
            .query({
                action: OffenderActions.SEARCH_OFFENDERS,
                type: OffenderType.APP,
                query: 't2'
            });

        searchOffendersByUri = nock(keyservHost)
            .get(`/2.x/`)
            .query({
                action: OffenderActions.SEARCH_OFFENDERS,
                type: OffenderType.URI,
                query: 'alyniekka.com'
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
            searchOffendersByUri.reply(200, foundOffendersByUriXML, {
                'Content-Type': 'text/xml; charset="utf-8"'
            });

            const response = await client.get(`${url}/api/offenders/search_offenders`, {
                searchParams: new URLSearchParams({
                    searchType: OffenderType.URI,
                    query: 'alyniekka.com'
                })
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body).toEqual(foundOffendersByUriExpectedResult);
        });

        it('should return 200 for request to keyserv by APP', async () => {
            searchOffendersByApp.reply(200, foundOffendersByAppXML, {
                'Content-Type': 'text/xml; charset="utf-8"'
            });

            const response = await client.get(`${url}/api/offenders/search_offenders`, {
                searchParams: new URLSearchParams({
                    searchType: OffenderType.APP,
                    query: 't2'
                })
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body).toEqual(foundOffendersByAppExpectedResult);
        });

        it('should return 200 for request to keyserv by IP', async () => {
            searchOffendersByIp.reply(200, foundOffendersByIpXML, {
                'Content-Type': 'text/xml; charset="utf-8"'
            });

            const response = await client.get(`${url}/api/offenders/search_offenders`, {
                searchParams: new URLSearchParams({
                    searchType: OffenderType.IP,
                    query: '1.1.1.1'
                })
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body).toEqual(foundOffendersByIpExpectedResult);
        });

        it('should return 200 and empty array for request to keyserv by IP', async () => {
            searchOffendersByIp.reply(200, notFoundOffendersByIpXML);

            const response = await client.get(`${url}/api/offenders/search_offenders`, {
                searchParams: new URLSearchParams({
                    searchType: OffenderType.IP,
                    query: '1.1.1.1'
                })
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body).toEqual([]);
        });
    });

    it('should return 400 after validation', async () => {
        const response = await client.get(`${url}/api/offenders/search_offenders`);
        expect(response.statusCode).toEqual(400);
    });
});
