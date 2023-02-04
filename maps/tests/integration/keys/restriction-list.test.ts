import * as http from 'http';
import * as nock from 'nock';
import {app} from 'server/app';
import {config} from 'server/config';
import {
    restrictionsListXML,
    restrictionsListExpectedResult
} from 'server/tests/integration/fixtures/find-keys';
import {intHostConfigLoader} from 'server/libs/host-loader';
import {KeyservSearchType} from 'server/routers/keys/keyserv-types';
import {startServer, stopServer, client} from 'server/tests/integration/test-server';
import {
    blackboxSessionidResponse,
    tvmIntResponse
} from 'server/tests/integration/fixtures/common-responses';

describe('/keyserv/settings', () => {
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
                action: KeyservSearchType.RESTRICTION_LIST
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
        keyserv.reply(200, restrictionsListXML, {
            'Content-Type': 'text/xml; charset="utf-8"'
        });

        const response = await client.get(`${url}/api/keys/restrictions_list`);

        expect(response.statusCode).toEqual(200);
        expect(response.body).toEqual(restrictionsListExpectedResult);
    });
});
