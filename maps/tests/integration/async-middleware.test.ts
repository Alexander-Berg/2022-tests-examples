import * as express from 'express';
import * as got from 'got';
import {expect} from 'chai';
import {asyncMiddleware} from '../../lib';
import {startServer, stopServer} from './test-server';

async function withTestServer(
    app: express.Application,
    testBody: (baseUrl: string) => Promise<void>
): Promise<void> {
    const [server, baseUrl] = await startServer(app);
    try {
        await testBody(baseUrl);
    } finally {
        await stopServer(server);
    }
}

const errorHandler: express.ErrorRequestHandler = (err, req, res, next) => {
    res.sendStatus(500);
};

const client = got.extend({
    retry: 0,
    timeout: 2000,
    throwHttpErrors: false
});

describe('asyncMiddleware', () => {
    it('should call error handler if middleware returns rejected promise', async () => {
        const app = express()
            .use(asyncMiddleware(() => Promise.reject('some error')))
            .use((req, res) => res.sendStatus(200))
            .use(errorHandler);

        await withTestServer(app, async (url) => {
            const res = await client(url);
            expect(res.statusCode).to.equal(500);
        });
    });

    it('should call next middleware if middleware returns fulfilled promise', async () => {
        const app = express()
            .use(asyncMiddleware(() => Promise.resolve()))
            .use((req, res) => res.sendStatus(200))
            .use(errorHandler);

        await withTestServer(app, async (url) => {
            const res = await client(url);
            expect(res.statusCode).to.equal(200);
        });
    });

    it('should not call next middleware if response was already sent', async () => {
        const app = express()
            .use(asyncMiddleware(async (req, res) => {
                res.sendStatus(400);
            }))
            .use((req, res) => res.sendStatus(200))
            .use(errorHandler);

        await withTestServer(app, async (url) => {
            const res = await client(url);
            expect(res.statusCode).to.equal(400);
        });
    });
});
