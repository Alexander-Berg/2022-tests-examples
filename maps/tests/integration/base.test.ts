import {expect} from 'chai';
import {app} from 'app/app';
import {client, withTestServer} from 'tests/integration/test-server';

describe('the basic interface of the HTTP API', () => {
    it('should respond with 200 on /ping requests', async () => {
        await withTestServer(app, async (server) => {
            const res = await client.get(`${server.url}/ping`);
            expect(res.statusCode).to.equal(200);
        });
    });

    it('should respond with 404 on unsupported paths', async () => {
        await withTestServer(app, async (server) => {
            const res = await client.get(`${server.url}/cat_pictures`);
            expect(res.statusCode).to.equal(404);
        });
    });

    it('should be free of X-Powered-By headers', async () => {
        await withTestServer(app, async (server) => {
            const res = await client.get(`${server.url}/ping`);
            expect(res.headers['x-powered-by'], 'X-Powered-By header should be disabled').to.not.exist;
        });
    });
});
