import {expect} from 'chai';
import * as fs from 'fs';
import nock from 'nock';
import {URL} from 'url';

import {app} from 'app/app';
import {intHostsLoader} from 'app/lib/hosts';
import {Hosts} from '@yandex-int/maps-host-configs';

import {client, TestServer} from 'tests/integration/test-server';
import {TvmDaemon} from 'tests/integration/tvm-daemon';

const USER1_TICKET = fs.readFileSync('src/tests/fixtures/tvm-tickets/user/uid-1').toString().trim();
const SERVICE_TICKET_TAKEOUT = fs.readFileSync('src/tests/fixtures/tvm-tickets/service/takeout').toString().trim();
const UNKNOWN_SERVICE_TICKET = fs.readFileSync('src/tests/fixtures/tvm-tickets/service/unknown').toString().trim();

interface ResponseError {
    message: string;
}

describe('GET /datasync/v1/takeout/status', async () => {
    let intHosts: Hosts;
    let tvmDaemon: TvmDaemon;
    let server: TestServer;
    let url: URL;

    before(async () => {
        intHosts = await intHostsLoader.get();
        server = await TestServer.start(app);
        tvmDaemon = await TvmDaemon.start();
        url = new URL(`${server.url}/datasync/v1/takeout/status`);
        nock.disableNetConnect();
        nock.enableNetConnect(/(127.0.0.1|localhost)/);
    });

    after(async () => {
        await tvmDaemon.stop();
        await server.stop();
        nock.enableNetConnect();
    });

    const requestId = 'test';
    const user1Headers = {
        'X-Ya-Service-Ticket': SERVICE_TICKET_TAKEOUT,
        'X-Ya-User-Ticket': USER1_TICKET
    };

    describe('check query and headers', () => {
        it('should return error when query does not contain request id', async () => {
            const res = await client.get<ResponseError>(url, {
                headers: user1Headers,
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(400);
            expect(res.body.message).to.equal('Error validating query parameters: "request_id" is required');
        });

        it('should return error when headers does not contain service ticket', async () => {
            const res = await client.get<ResponseError>(url, {
                searchParams: {request_id: requestId},
                headers: {
                    'X-Ya-User-Ticket': USER1_TICKET
                },
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(400);
            expect(res.body.message).to.equal('Error validating headers: "x-ya-service-ticket" is required');
        });

        it('should return error when headers does not contain user ticket', async () => {
            const res = await client.get<ResponseError>(url, {
                searchParams: {request_id: requestId},
                headers: {
                    'X-Ya-Service-Ticket': SERVICE_TICKET_TAKEOUT
                },
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(400);
            expect(res.body.message).to.equal('Error validating headers: "x-ya-user-ticket" is required');
        });

        it('should return error when invalid service ticket', async () => {
            const res = await client.get<ResponseError>(url, {
                searchParams: {request_id: requestId},
                headers: {
                    'X-Ya-Service-Ticket': 'shto-to-ne-to',
                    'X-Ya-User-Ticket': USER1_TICKET
                },
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(401);
            expect(res.body.message).to.equal('Invalid TVM service ticket');
        });

        it('should return error when invalid user ticket', async () => {
            const res = await client.get<ResponseError>(url, {
                searchParams: {request_id: requestId},
                headers: {
                    'X-Ya-Service-Ticket': SERVICE_TICKET_TAKEOUT,
                    'X-Ya-User-Ticket': 'shto-to-ne-to'
                },
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(401);
            expect(res.body.message).to.equal('Invalid TVM user ticket');
        });

        it('should return error when unknown service ticket', async () => {
            const res = await client.get<ResponseError>(url, {
                searchParams: {request_id: requestId},
                headers: {
                    'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET,
                    'X-Ya-User-Ticket': USER1_TICKET
                },
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(403);
            expect(res.body.message).to.equal('TVM service does not have enough access rights');
        });
    });

    describe('get datasync status', () => {
        afterEach(nock.cleanAll);

        it('should return error when datasync error occurs', async () => {
            nock(intHosts.cloudInt)
                .persist()
                .get(/.*/g)
                .reply(400);

            const res = await client.get(url, {
                searchParams: {request_id: requestId},
                headers: user1Headers,
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({
                status: 'error',
                errors: [
                    {
                        code: 'internal',
                        message: 'Internal server error'
                    }
                ]
            });
        });

        it('should return empty status when database does not exist', async () => {
            nock(intHosts.cloudInt)
                .persist()
                .get(/.*/g)
                .reply(404);

            const res = await client.get(url, {
                searchParams: {request_id: requestId},
                headers: user1Headers,
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({
                status: 'ok',
                data: [
                    {id: 'bookmarks', slug: 'bookmarks', state: 'empty'},
                    {id: 'search-history', slug: 'search-history', state: 'empty'},
                    {id: 'points-history', slug: 'points-history', state: 'empty'},
                    {id: 'addresses', slug: 'addresses', state: 'empty'},
                    {id: 'ridehistory', slug: 'ridehistory', state: 'empty'},
                    {id: 'ynavisync', slug: 'ynavisync', state: 'empty'},
                    {id: 'ynavicarinfo', slug: 'ynavicarinfo', state: 'empty'}
                ]
            });
        });

        it('should return empty statuses when no data', async () => {
            nock(intHosts.cloudInt)
                .persist()
                .get(/.*/g)
                .reply(200, {records_count: 0});

            const res = await client.get(url, {
                searchParams: {request_id: requestId},
                headers: user1Headers,
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({
                status: 'ok',
                data: [
                    {id: 'bookmarks', slug: 'bookmarks', state: 'empty'},
                    {id: 'search-history', slug: 'search-history', state: 'empty'},
                    {id: 'points-history', slug: 'points-history', state: 'empty'},
                    {id: 'addresses', slug: 'addresses', state: 'empty'},
                    {id: 'ridehistory', slug: 'ridehistory', state: 'empty'},
                    {id: 'ynavisync', slug: 'ynavisync', state: 'empty'},
                    {id: 'ynavicarinfo', slug: 'ynavicarinfo', state: 'empty'}
                ]
            });
        });

        it('should return statuses when data exist', async () => {
            nock(intHosts.cloudInt)
                .persist()
                .get(/.*/g)
                .reply(200, {records_count: 10, modified: '2022-01-18T12:53:31Z'});

            const res = await client.get(url, {
                searchParams: {request_id: requestId},
                headers: user1Headers,
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({
                status: 'ok',
                data: [
                    {
                        id: 'bookmarks',
                        slug: 'bookmarks',
                        state: 'ready_to_delete',
                        update_date: '2022-01-18T12:53:31Z'
                    },
                    {
                        id: 'search-history',
                        slug: 'search-history',
                        state: 'ready_to_delete',
                        update_date: '2022-01-18T12:53:31Z'
                    },
                    {
                        id: 'points-history',
                        slug: 'points-history',
                        state: 'ready_to_delete',
                        update_date: '2022-01-18T12:53:31Z'
                    },
                    {
                        id: 'addresses',
                        slug: 'addresses',
                        state: 'ready_to_delete',
                        update_date: '2022-01-18T12:53:31Z'
                    },
                    {
                        id: 'ridehistory',
                        slug: 'ridehistory',
                        state: 'ready_to_delete',
                        update_date: '2022-01-18T12:53:31Z'
                    },
                    {
                        id: 'ynavisync',
                        slug: 'ynavisync',
                        state: 'ready_to_delete',
                        update_date: '2022-01-18T12:53:31Z'
                    },
                    {
                        id: 'ynavicarinfo',
                        slug: 'ynavicarinfo',
                        state: 'ready_to_delete',
                        update_date: '2022-01-18T12:53:31Z'
                    }
                ]
            });
        });
    });
});
