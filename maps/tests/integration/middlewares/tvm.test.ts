import {expect} from 'chai';
import express from 'express';
import * as fs from 'fs';
import * as Boom from '@hapi/boom';
import nock from 'nock';

import {config} from 'app/config';

import {authenticateByServiceTicket, authenticateByUserTicket} from 'app/middlewares/tvm';

import {client, TestServer, withTestServer} from 'tests/integration/test-server';
import {TvmDaemon} from 'tests/integration/tvm-daemon';

const USER1_TICKET = fs.readFileSync('src/tests/fixtures/tvm-tickets/user/uid-1').toString().trim();
const SERVICE_TICKET_MAPS = fs.readFileSync('src/tests/fixtures/tvm-tickets/service/maps').toString().trim();
const SERVICE_TICKET_TAKEOUT = fs.readFileSync('src/tests/fixtures/tvm-tickets/service/takeout').toString().trim();
const UNKNOWN_SERVICE_TICKET = fs.readFileSync('src/tests/fixtures/tvm-tickets/service/unknown').toString().trim();

describe('TVM middleware', () => {
    const errorHandler: express.ErrorRequestHandler = (err, req, res, next) => {
        if (Boom.isBoom(err)) {
            res.status(err.output.statusCode).end(err.message);
        } else {
            next(err);
        }
    };

    describe(`tvm availability`, () => {
        let tvmDaemon: TvmDaemon;

        before(async () => {
            tvmDaemon = await TvmDaemon.start();
        });

        after(async () => {
            await tvmDaemon!.stop();
        });

        describe('`authenticateByServiceTicket`', () => {
            const app = express()
                .get(
                    '/',
                    authenticateByServiceTicket('takeout'),
                    (req, res) => {
                        res.end();
                    }
                )
                .use(errorHandler);

            let server: TestServer;

            before(async () => {
                server = await TestServer.start(app);
            });

            after(async () => {
                await server.stop();
            });

            it('should throw 401 when service ticket is missing', async () => {
                const res = await client.get(`${server.url}/`);

                expect(res.statusCode).to.equal(401);
                expect(res.body).to.equal('Invalid authentication data provided');
            });

            it('should throw 401 when service ticket is empty string', async () => {
                const res = await client.get(`${server.url}/`, {
                    headers: {
                        'X-Ya-Service-Ticket': ''
                    }
                });

                expect(res.statusCode).to.equal(401);
                expect(res.body).to.equal('Invalid authentication data provided');
            });

            it('should throw 401 when service ticket is invalid', async () => {
                const res = await client.get(`${server.url}/`, {
                    headers: {
                        'X-Ya-Service-Ticket': '123'
                    }
                });

                expect(res.statusCode).to.equal(401);
                expect(res.body).to.equal('Invalid TVM service ticket');
            });

            it('should throw 403 when service does not have enough access rights', async () => {
                const res = await client.get(`${server.url}/`, {
                    headers: {
                        'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET
                    }
                });

                expect(res.statusCode).to.equal(403);
                expect(res.body).to.equal('TVM service does not have enough access rights');
            });

            it('should return 200 when service ticket is correct and service has access rights', async () => {
                const res = await client.get(`${server.url}/`, {
                    headers: {
                        'X-Ya-Service-Ticket': SERVICE_TICKET_TAKEOUT
                    }
                });

                expect(res.statusCode).to.equal(200);
            });
        });

        describe('`authenticateByUserTicket`', () => {
            const app = express()
                .get(
                    '/',
                    authenticateByUserTicket('maps', 'mobmaps-proxy-api'),
                    (req, res) => {
                        expect(req.tvm.uid).to.equal('1');
                        res.end();
                    }
                )
                .use(errorHandler);

            let server: TestServer;

            before(async () => {
                server = await TestServer.start(app);
            });

            after(async () => {
                await server.stop();
            });

            it('should throw 401 when service ticket is missing', async () => {
                const res = await client.get(`${server.url}/`, {
                    headers: {
                        'X-Ya-User-Ticket': USER1_TICKET
                    }
                });

                expect(res.statusCode).to.equal(401);
                expect(res.body).to.equal('Invalid authentication data provided');
            });

            it('should throw 401 when service ticket is empty string', async () => {
                const res = await client.get(`${server.url}/`, {
                    headers: {
                        'X-Ya-Service-Ticket': '',
                        'X-Ya-User-Ticket': USER1_TICKET
                    }
                });

                expect(res.statusCode).to.equal(401);
                expect(res.body).to.equal('Invalid authentication data provided');
            });

            it('should throw 401 when service ticket is invalid', async () => {
                const res = await client.get(`${server.url}/`, {
                    headers: {
                        'X-Ya-Service-Ticket': '123',
                        'X-Ya-User-Ticket': USER1_TICKET
                    }
                });

                expect(res.statusCode).to.equal(401);
                expect(res.body).to.equal('Invalid TVM service ticket');
            });

            it('should throw 401 when user ticket is missing', async () => {
                const res = await client.get(`${server.url}/`, {
                    headers: {
                        'X-Ya-Service-Ticket': SERVICE_TICKET_MAPS
                    }
                });

                expect(res.statusCode).to.equal(401);
                expect(res.body).to.equal('Invalid authentication data provided');
            });

            it('should throw 401 when user ticket is empty string', async () => {
                const res = await client.get(`${server.url}/`, {
                    headers: {
                        'X-Ya-Service-Ticket': SERVICE_TICKET_MAPS,
                        'X-Ya-User-Ticket': ''
                    }
                });

                expect(res.statusCode).to.equal(401);
                expect(res.body).to.equal('Invalid authentication data provided');
            });

            it('should throw 401 when user ticket is invalid', async () => {
                const res = await client.get(`${server.url}/`, {
                    headers: {
                        'X-Ya-Service-Ticket': SERVICE_TICKET_MAPS,
                        'X-Ya-User-Ticket': '123'
                    }
                });

                expect(res.statusCode).to.equal(401);
                expect(res.body).to.equal('Invalid TVM user ticket');
            });

            it('should throw 403 when service does not have enough access rights', async () => {
                const res = await client.get(`${server.url}/`, {
                    headers: {
                        'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET,
                        'X-Ya-User-Ticket': USER1_TICKET
                    }
                });

                expect(res.statusCode).to.equal(403);
                expect(res.body).to.equal('TVM service does not have enough access rights');
            });

            it('should return 200 when service and user tickets are correct and service has rights', async () => {
                const res = await client.get(`${server.url}/`, {
                    headers: {
                        'X-Ya-Service-Ticket': SERVICE_TICKET_MAPS,
                        'X-Ya-User-Ticket': USER1_TICKET
                    }
                });

                expect(res.statusCode).to.equal(200);
            });
        });
    });

    describe('check tvm unavailability', () => {
        before(() => {
            nock.disableNetConnect();
            nock.enableNetConnect(/(127.0.0.1|localhost)/);
        });

        after(() => {
            nock.enableNetConnect();
        });

        afterEach(nock.cleanAll);

        describe('`authenticateByServiceTicket`', () => {
            const app = express()
                .get(
                    '/',
                    authenticateByServiceTicket('takeout'),
                    (req, res) => {
                        res.end();
                    }
                )
                .use(errorHandler);

            it('should throw 500 when tvm is unavailable', async () => {
                const nockTvm = nock(config['tvm.url'])
                    .get('/tvm/checksrv')
                    .reply(500);

                await withTestServer(app, async (server) => {
                    const res = await client.get(`${server.url}/`, {
                        headers: {
                            'X-Ya-Service-Ticket': SERVICE_TICKET_TAKEOUT
                        },
                        retry: 0
                    });

                    expect(res.statusCode).to.equal(500);
                });

                expect(nockTvm.isDone()).to.be.true;
            });
        });

        describe('`authenticateByUserTicket`', () => {
            const app = express()
                .get(
                    '/',
                    authenticateByUserTicket('maps'),
                    (req, res) => {
                        res.end();
                    }
                )
                .use(errorHandler);

            it('should throw 500 when request to tvm was failed', async () => {
                const nockTvmService = nock(config['tvm.url'])
                    .get('/tvm/checksrv')
                    .reply(200);

                const nockTvmUser = nock(`${config['tvm.url']}/`)
                    .get('/tvm/checkusr')
                    .reply(500);

                await withTestServer(app, async (server) => {
                    const res = await client.get(`${server.url}/`, {
                        headers: {
                            'X-Ya-Service-Ticket': SERVICE_TICKET_MAPS,
                            'X-Ya-User-Ticket': USER1_TICKET
                        },
                        retry: 0
                    });

                    expect(res.statusCode).to.equal(500);
                });

                expect(nockTvmService.isDone()).to.be.true;
                expect(nockTvmUser.isDone()).to.be.true;
            });
        });
    });
});
