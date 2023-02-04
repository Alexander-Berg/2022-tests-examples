import {expect} from 'chai';
import * as fs from 'fs';
import {URL} from 'url';

import {app} from 'app/app';
import {dbClient} from 'app/lib/db-client';
import {ListStatus, TAKEOUT_DELETE_ID} from 'app/lib/api-types';
import {clearDb} from 'tests/helpers/clear-db';
import {client, TestServer} from 'tests/integration/test-server';
import {TvmDaemon} from 'tests/integration/tvm-daemon';

const USER1_TICKET = fs.readFileSync('src/tests/fixtures/tvm-tickets/user/uid-1').toString().trim();
const SERVICE_TICKET_TAKEOUT = fs.readFileSync('src/tests/fixtures/tvm-tickets/service/takeout').toString().trim();
const UNKNOWN_SERVICE_TICKET = fs.readFileSync('src/tests/fixtures/tvm-tickets/service/unknown').toString().trim();

interface ResponseError {
    message: string;
}

describe('GET /v1/takeout/status', async () => {
    let tvmDaemon: TvmDaemon;
    let server: TestServer;
    let url: URL;

    before(async () => {
        server = await TestServer.start(app);
        tvmDaemon = await TvmDaemon.start();
        url = new URL(`${server.url}/v1/takeout/status`);
    });

    after(async () => {
        await tvmDaemon.stop();
        await server.stop();
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

            expect(res.statusCode).to.equal(401);
            expect(res.body.message).to.equal('Invalid authentication data provided');
        });

        it('should return error when headers does not contain user ticket', async () => {
            const res = await client.get<ResponseError>(url, {
                searchParams: {request_id: requestId},
                headers: {
                    'X-Ya-Service-Ticket': SERVICE_TICKET_TAKEOUT
                },
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(401);
            expect(res.body.message).to.equal('Invalid authentication data provided');
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

    describe('get status', () => {
        beforeEach(clearDb);

        async function insertList(uid: string, recordId: string, status: ListStatus) {
            return dbClient.executeWriteQuery<{id: string}>({
                text: `
                    INSERT INTO lists
                    (uid, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                    ($1, $2, 1, '2022-03-22', $3, '[]', '{}')
                    RETURNING id`,
                values: [uid, recordId, status]
            });
        }

        async function insertSubscription(uid: string, listId: string) {
            await dbClient.executeWriteQuery({
                text: `
                    INSERT INTO subscriptions
                    (uid, list_id) VALUES
                    ($1, $2)`,
                values: [uid, listId]
            });
        }

        it('should return empty statuses when no data', async () => {
            const res = await client.get(url, {
                searchParams: {request_id: requestId},
                headers: user1Headers,
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({
                status: 'ok',
                data: [{id: TAKEOUT_DELETE_ID, slug: 'bookmarks', state: 'empty'}]
            });
        });

        it('should return empty statuses when list is `deleted`', async () => {
            await insertList('1', 'list_record_id', 'deleted');
            const res = await client.get(url, {
                searchParams: {request_id: requestId},
                headers: user1Headers,
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({
                status: 'ok',
                data: [{id: TAKEOUT_DELETE_ID, slug: 'bookmarks', state: 'empty'}]
            });
        });

        it('should return statuses when user have list and status is not `deleted`', async () => {
            await insertList('1', 'list_record_id', 'shared');

            const res = await client.get(url, {
                searchParams: {request_id: requestId},
                headers: user1Headers,
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({
                status: 'ok',
                data: [{id: TAKEOUT_DELETE_ID, slug: 'bookmarks', state: 'ready_to_delete'}]
            });
        });

        it('should return statuses when user have subscription', async () => {
            const {data: {rows: [{id: listId}]}} = await insertList('2', 'list_record_id', 'shared');
            await insertSubscription('1', listId);

            const res = await client.get(url, {
                searchParams: {request_id: requestId},
                headers: user1Headers,
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({
                status: 'ok',
                data: [{id: TAKEOUT_DELETE_ID, slug: 'bookmarks', state: 'ready_to_delete'}]
            });
        });

        it('should return statuses when user have list and subscription', async () => {
            await insertList('1', 'list_record_id', 'shared');
            const {data: {rows: [{id: listId}]}} = await insertList('2', 'list_record_id', 'shared');
            await insertSubscription('1', listId);

            const res = await client.get(url, {
                searchParams: {request_id: requestId},
                headers: user1Headers,
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({
                status: 'ok',
                data: [{id: TAKEOUT_DELETE_ID, slug: 'bookmarks', state: 'ready_to_delete'}]
            });
        });
    });
});
