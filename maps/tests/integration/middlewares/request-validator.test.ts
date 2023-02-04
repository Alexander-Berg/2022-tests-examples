import express from 'express';
import * as joi from 'joi';
import * as bodyParser from 'body-parser';
import {expect} from 'chai';
import {client, withTestServer} from 'tests/integration/test-server';
import * as Boom from '@hapi/boom';
import {requestValidatorMiddleware} from 'app/middlewares/request-validator';

describe('Request validator middleware', () => {
    const errorHandler: express.ErrorRequestHandler = (err, req, res, next) => {
        if (Boom.isBoom(err)) {
            res.status(err.output.statusCode).end(err.message);
        } else {
            next(err);
        }
    };

    describe('headers validation', () => {
        const app = express()
            .get(
                '/',
                requestValidatorMiddleware({headers: {h1: joi.number().required()}}),
                (req, res) => {
                    expect(req.headers.h1).to.equal(123);
                    res.end();
                }
            )
            .use(errorHandler);

        it('should check header on existence', async () => {
            await withTestServer(app, async (server) => {
                const res = await client.get(`${server.url}/`);
                expect(res.statusCode).to.equal(400);
            });
        });

        it('should check header type', async () => {
            await withTestServer(app, async (server) => {
                const res = await client.get(`${server.url}/`, {headers: {h1: '1x'}});
                expect(res.statusCode).to.equal(400);
            });
        });

        it('should convert value', async () => {
            await withTestServer(app, async (server) => {
                const res = await client.get(`${server.url}/`, {headers: {h1: '123'}});
                expect(res.statusCode).to.equal(200);
            });
        });

        it('should allow unknown headers', async () => {
            await withTestServer(app, async (server) => {
                const res = await client.get(`${server.url}/`, {headers: {h1: '123', h2: 'h2 value'}});
                expect(res.statusCode).to.equal(200);
            });
        });
    });

    describe('path parameters validation', () => {
        const app = express()
            .get(
                '/:id',
                requestValidatorMiddleware({params: {id: joi.number().required()}}),
                (req, res) => {
                    expect(req.params.id).to.equal(123);
                    res.end();
                }
            )
            .use(errorHandler);

        it('should check parameter type', async () => {
            await withTestServer(app, async (server) => {
                const res = await client.get(`${server.url}/xxx`);
                expect(res.statusCode).to.equal(400);
            });
        });

        it('should convert value', async () => {
            await withTestServer(app, async (server) => {
                const res = await client.get(`${server.url}/123`);
                expect(res.statusCode).to.equal(200);
            });
        });
    });

    describe('query parameters validation', () => {
        const app = express()
            .get(
                '/',
                requestValidatorMiddleware({query: {q1: joi.number().required()}}),
                (req, res) => {
                    expect(req.query.q1).to.equal(123);
                    res.end();
                }
            )
            .use(errorHandler);

        it('should check parameter existence', async () => {
            await withTestServer(app, async (server) => {
                const res = await client.get(`${server.url}/`);
                expect(res.statusCode).to.equal(400);
            });
        });

        it('should check parameter type', async () => {
            await withTestServer(app, async (server) => {
                const res = await client.get(`${server.url}/`, {searchParams: {q1: '1x'}});
                expect(res.statusCode).to.equal(400);
            });
        });

        it('should convert value', async () => {
            await withTestServer(app, async (server) => {
                const res = await client.get(`${server.url}/`, {searchParams: {q1: '123'}});
                expect(res.statusCode).to.equal(200);
            });
        });

        it('should allow unknown parameters', async () => {
            await withTestServer(app, async (server) => {
                const res = await client.get(`${server.url}/`, {searchParams: {q1: '123', q2: 'xxx'}});
                expect(res.statusCode).to.equal(200);
            });
        });
    });

    describe('body validation', () => {
        function configureApp(app: express.Application): void {
            app.post(
                '/',
                requestValidatorMiddleware({body: joi.object({key: joi.number().required()}).required()}),
                (req, res) => {
                    expect(req.body).to.deep.equal({key: 123});
                    res.end();
                }
            );
            app.use(errorHandler);
        }

        describe('without body parser', () => {
            const app = express();
            configureApp(app);

            it('should return bad request for missing body', async () => {
                await withTestServer(app, async (server) => {
                    const res = await client.post(`${server.url}/`, {
                        headers: {
                            'content-type': 'application/json'
                        },
                        body: ''
                    });
                    expect(res.statusCode).to.equal(400);
                });
            });
        });

        describe('with body parser', () => {
            const bodyParserMiddleware = bodyParser.json({strict: false});
            const app = express().use(bodyParserMiddleware);
            configureApp(app);

            it('should check body type', async () => {
                await withTestServer(app, async (server) => {
                    const res = await client.post(`${server.url}/`, {
                        // https://github.com/sindresorhus/got/issues/511
                        headers: {
                            'content-type': 'application/json'
                        },
                        body: JSON.stringify(1)
                    });
                    expect(res.statusCode).to.equal(400);
                });
            });

            it('should check body keys existence', async () => {
                await withTestServer(app, async (server) => {
                    const res = await client.post(`${server.url}/`, {json: {}});
                    expect(res.statusCode).to.equal(400);
                });
            });

            it('should not convert values', async () => {
                await withTestServer(app, async (server) => {
                    const res = await client.post(`${server.url}/`, {json: {ket: '123'}});
                    expect(res.statusCode).to.equal(400);
                });
            });

            it('should pass correct body', async () => {
                await withTestServer(app, async (server) => {
                    const res = await client.post(`${server.url}/`, {json: {key: 123}});
                    expect(res.statusCode).to.equal(200);
                });
            });

            it('should not allow unknown values', async () => {
                await withTestServer(app, async (server) => {
                    const res = await client.post(`${server.url}/`, {json: {key: 123, key2: 4}});
                    expect(res.statusCode).to.equal(400);
                });
            });
        });
    });
});
