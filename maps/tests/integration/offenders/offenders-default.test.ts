import * as http from 'http';
import * as nock from 'nock';
import {app} from 'server/app';
import {config} from 'server/config';
import {startServer, stopServer, client} from 'server/tests/integration/test-server';
import {
    blackboxSessionidResponse,
    tvmIntResponse
} from 'server/tests/integration/fixtures/common-responses';

describe('not found offenders page', () => {
    let url: string;
    let server: http.Server;
    let tvm: nock.Interceptor;
    let blackboxIntSessionid: nock.Interceptor;

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);

        blackboxIntSessionid = nock(`http://${config['blackbox.internalHost']}`)
            .get(/blackbox\?method=sessionid/);

        tvm = nock(config['tvm.url'])
            .get(`/tvm/tickets`)
            .query(true);
    });

    beforeEach(() => {
        tvm.reply(200, tvmIntResponse);
        blackboxIntSessionid.reply(200, blackboxSessionidResponse);
    });

    afterAll(async () => {
        await stopServer(server);
        nock.restore();
        nock.enableNetConnect();
    });

    afterEach(() => {
        nock.cleanAll();
    });

    it('should return 404 of wrong path', async () => {
        const response = await client.get(`${url}/api/offenders/some_wrong_uri`);
        expect(response.statusCode).toEqual(404);
    });
});
