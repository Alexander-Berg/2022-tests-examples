import * as http from 'http';
import * as nock from 'nock';
import {app} from 'server/app';
import {URLSearchParams} from 'url';
import {
    offendersHistoryXML,
    noOffendersHistoryXML,
    offendersHistoryExpectedResult
} from 'server/tests/integration/fixtures/find-offenders';
import {intHostConfigLoader} from 'server/libs/host-loader';
import {startServer, stopServer, client} from 'server/tests/integration/test-server';
import {OffenderActions, OffenderType} from 'server/routers/offenders/offender-types';
import {config} from 'server/config';
import {
    tvmIntResponse,
    blackboxSessionidResponse
} from 'server/tests/integration/fixtures/common-responses';

describe('/keyserv/ settings', () => {
    let url: string;
    let server: http.Server;

    let offendersHistoryByIP: nock.Interceptor;
    let offendersHistoryByURI: nock.Interceptor;
    let offendersHistoryByAPP: nock.Interceptor;
    let tvmInt: nock.Interceptor;
    let blackboxIntUserinfo: nock.Interceptor;
    let blackboxIntSessionid: nock.Interceptor;

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);

        const {keyserv: keyservHost} = await intHostConfigLoader.get();
        offendersHistoryByAPP = nock(keyservHost)
            .get(`/2.x/`)
            .query({
                action: OffenderActions.OFFENDERS_HISTORY,
                type: OffenderType.APP,
                id: 'sample_appid'
            });

        offendersHistoryByURI = nock(keyservHost)
            .get(`/2.x/`)
            .query({
                action: OffenderActions.OFFENDERS_HISTORY,
                type: OffenderType.URI,
                id: 'sample_uri'
            });

        offendersHistoryByIP = nock(keyservHost)
            .get(`/2.x/`)
            .query({
                action: OffenderActions.OFFENDERS_HISTORY,
                type: OffenderType.IP,
                id: 'sample_ip'
            });

        blackboxIntUserinfo = nock(`http://${config['blackbox.internalHost']}`)
            .get(/blackbox\?method=userinfo/);

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
        blackboxIntUserinfo.reply(200, {
            users: [
                {
                    uid: {value: '728119531574847900'},
                    login: 'user1'
                },
                {
                    uid: {value: '4475619391450774500'},
                    login: 'user2'
                }
            ]
        });
        blackboxIntSessionid.reply(200, blackboxSessionidResponse);
        tvmInt.reply(200, tvmIntResponse);
    });

    afterEach(() => {
        nock.cleanAll();
    });

    describe('request to each searchType', () => {
        it('should return 200 for request to get list of offenders by uri', async () => {
            offendersHistoryByURI.reply(200, offendersHistoryXML, {
                'Content-Type': 'text/xml; charset="utf-8"'
            });

            const response = await client.get(`${url}/api/offenders/offenders_history`, {
                searchParams: new URLSearchParams({
                    searchType: OffenderType.URI,
                    searchedValue: 'sample_uri'
                })
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body).toEqual(offendersHistoryExpectedResult);
        });

        it('should return 200 for request to get list of offenders by app', async () => {
            offendersHistoryByAPP.reply(200, offendersHistoryXML, {
                'Content-Type': 'text/xml; charset="utf-8"'
            });

            const response = await client.get(`${url}/api/offenders/offenders_history`, {
                searchParams: new URLSearchParams({
                    searchType: OffenderType.APP,
                    searchedValue: 'sample_appid'
                })
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body).toEqual(offendersHistoryExpectedResult);
        });

        it('should return 200 for request to get list of offenders by ip', async () => {
            offendersHistoryByIP.reply(200, offendersHistoryXML, {
                'Content-Type': 'text/xml; charset="utf-8"'
            });

            const response = await client.get(`${url}/api/offenders/offenders_history`, {
                searchParams: new URLSearchParams({
                    searchType: OffenderType.IP,
                    searchedValue: 'sample_ip'
                })
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body).toEqual(offendersHistoryExpectedResult);
        });

        it('should return 200 and empty array for request', async () => {
            offendersHistoryByIP.reply(200, noOffendersHistoryXML, {
                'Content-Type': 'text/xml; charset="utf-8"'
            });

            const response = await client.get(`${url}/api/offenders/offenders_history`, {
                searchParams: new URLSearchParams({
                    searchType: OffenderType.IP,
                    searchedValue: 'sample_ip'
                })
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body).toEqual([]);
        });
    });

    describe('error validation && parsing', () => {
        it('should return 400 after validation', async () => {
            const response = await client.get(`${url}/api/offenders/offenders_history`, {
                searchParams: new URLSearchParams({
                    searchType: OffenderType.IP
                })
            });

            expect(response.statusCode).toEqual(400);
        });

        it('should return 400 after validation', async () => {
            const response = await client.get(`${url}/api/offenders/offenders_history`, {
                searchParams: new URLSearchParams({
                    searchType: OffenderType.IP,
                    searchedValue: ''
                })
            });

            expect(response.statusCode).toEqual(400);
        });

        it('should return 500 during XML parsing', async () => {
            offendersHistoryByURI.reply(200, '');

            const response = await client.get(`${url}/api/offenders/offenders_history`, {
                searchParams: new URLSearchParams({
                    searchType: OffenderType.URI,
                    searchedValue: 'sample_uri'
                })
            });

            expect(response.statusCode).toEqual(500);
        });

        it('should return 500 after got request', async () => {
            offendersHistoryByURI.reply(500, 'error');

            const response = await client.get(`${url}/api/offenders/offenders_history`, {
                searchParams: new URLSearchParams({
                    searchType: OffenderType.URI,
                    // For this reason, got will turn 500
                    searchedValue: 'wrong_uri'
                })
            });

            expect(response.statusCode).toEqual(500);
        });

        it('should return 400 after validation', async () => {
            const response = await client.get(`${url}/api/offenders/offenders_history`);

            expect(response.statusCode).toEqual(400);
        });
    });
});
