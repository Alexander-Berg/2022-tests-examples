import * as express from 'express';
import {expect} from 'chai';
import {ApiError} from 'src/lib/api-errors';
import {jsonBodyParser} from 'src/middlewares/body-parser';
import {TestServer} from 'tests/integration/test-server';

async function withTestServer(
    app: express.Application,
    callback: (server: TestServer) => Promise<any>
): Promise<void> {
    const server = await TestServer.start(app);
    try {
        await callback(server);
    } finally {
        await server.stop();
    }
}

describe('jsonBodyParser middleware', () => {
    const errorHandler: express.ErrorRequestHandler = (err, req, res, next) => {
        if (err instanceof ApiError) {
            res.status(err.statusCode).end(err.message);
        } else {
            next(err);
        }
    };

    it('should return 400 for invalid body', async () => {
        const app = express()
            .post('/', jsonBodyParser, (req, res) => res.end())
            .use(errorHandler);

        await withTestServer(app, async (server) => {
            const res = await server.request('/', {
                method: 'POST',
                headers: {
                    'content-type': 'application/json'
                },
                body: '{'
            });
            expect(res.statusCode).to.equal(400);
            expect(res.body).to.match(/Failed to parse body: Unexpected end of JSON input/);
        });
    });

    describe('removing UTF special chars from parsed body', () => {
        it('for object', async () => {
            const app = express()
                .post('/', jsonBodyParser, (req, res) => {
                    expect(req.body).to.deep.equal({foo: ' bar '});
                    res.end();
                })
                .use(errorHandler);

            await withTestServer(app, async (server) => {
                const res = await server.request('/', {
                    method: 'POST',
                    json: true,
                    body: {foo: '\u0000 bar \u2028'}
                });
                expect(res.statusCode).to.equal(200);
            });
        });

        it('for string', async () => {
            const app = express()
                .post('/', jsonBodyParser, (req, res) => {
                    expect(req.body).to.equal('foobar');
                    res.end();
                })
                .use(errorHandler);

            await withTestServer(app, async (server) => {
                const res = await server.request('/', {
                    method: 'POST',
                    // https://github.com/sindresorhus/got/issues/511
                    headers: {
                        'content-type': 'application/json'
                    },
                    body: JSON.stringify('foo\u0000bar\u0000')
                });
                expect(res.statusCode).to.equal(200);
            });
        });
    });
});
