import {expect} from 'chai';
import * as fs from 'fs';
import {URL} from 'url';

import {app} from 'app/app';
import {dbClient} from 'app/lib/db-client';
import {ListStatus, TAKEOUT_DELETE_ID} from 'app/lib/api-types';
import {encodePublicId} from 'app/v1/helpers/public-id';
import {clearDb} from 'tests/helpers/clear-db';
import {client, TestServer} from 'tests/integration/test-server';
import {TvmDaemon} from 'tests/integration/tvm-daemon';

const USER1_TICKET = fs.readFileSync('src/tests/fixtures/tvm-tickets/user/uid-1').toString().trim();
const SERVICE_TICKET_TAKEOUT = fs.readFileSync('src/tests/fixtures/tvm-tickets/service/takeout').toString().trim();
const UNKNOWN_SERVICE_TICKET = fs.readFileSync('src/tests/fixtures/tvm-tickets/service/unknown').toString().trim();

interface ResponseError {
    message: string;
}

describe('POST /v1/takeout/delete', async () => {
    let tvmDaemon: TvmDaemon;
    let server: TestServer;
    let url: URL;

    before(async () => {
        server = await TestServer.start(app);
        tvmDaemon = await TvmDaemon.start();
        url = new URL(`${server.url}/v1/takeout/delete`);
    });

    after(async () => {
        await tvmDaemon.stop();
        await server.stop();
    });

    const requestId = '86da91117e3ff8c16ec8546daa5323260a';
    const user1Headers = {
        'X-Ya-Service-Ticket': SERVICE_TICKET_TAKEOUT,
        'X-Ya-User-Ticket': USER1_TICKET
    };

    describe('check query and headers', () => {
        it('should throw 400 when query does not contain request id', async () => {
            const res = await client.post<ResponseError>(url, {
                headers: user1Headers,
                responseType: 'json',
                json: {id: [TAKEOUT_DELETE_ID]}
            });

            expect(res.statusCode).to.equal(400);
            expect(res.body.message).to.equal('Error validating query parameters: "request_id" is required');
        });

        it('should throw 400 when body contain incorrect fields', async () => {
            const res = await client.post<ResponseError>(url, {
                searchParams: {request_id: requestId},
                headers: {
                    'X-Ya-Service-Ticket': SERVICE_TICKET_TAKEOUT,
                    'X-Ya-User-Ticket': USER1_TICKET
                },
                json: {deleteIDs: [TAKEOUT_DELETE_ID]},
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(400);
            expect(res.body.message).to.equal('Error validating body: "id" is required');
        });

        it('should return error when id field contains incorrect values', async () => {
            const res = await client.post<ResponseError>(url, {
                searchParams: {request_id: requestId},
                headers: {
                    'X-Ya-Service-Ticket': SERVICE_TICKET_TAKEOUT,
                    'X-Ya-User-Ticket': USER1_TICKET
                },
                json: {id: ['2']},
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({
                status: 'error',
                errors: [{code: 'bad_id', message: `bookmarks-int accepts only {id: [${TAKEOUT_DELETE_ID}]}`}]
            });
        });

        it('should throw 401 when headers does not contain service ticket', async () => {
            const res = await client.post<ResponseError>(url, {
                searchParams: {request_id: requestId},
                headers: {
                    'X-Ya-User-Ticket': USER1_TICKET
                },
                json: {id: [TAKEOUT_DELETE_ID]},
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(401);
            expect(res.body.message).to.equal('Invalid authentication data provided');
        });

        it('should throw 401 when headers does not contain user ticket', async () => {
            const res = await client.post<ResponseError>(url, {
                searchParams: {request_id: requestId},
                headers: {
                    'X-Ya-Service-Ticket': SERVICE_TICKET_TAKEOUT
                },
                json: {id: [TAKEOUT_DELETE_ID]},
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(401);
            expect(res.body.message).to.equal('Invalid authentication data provided');
        });

        it('should throw 401 when invalid service ticket', async () => {
            const res = await client.post<ResponseError>(url, {
                searchParams: {request_id: requestId},
                headers: {
                    'X-Ya-Service-Ticket': 'shto-to-ne-to',
                    'X-Ya-User-Ticket': USER1_TICKET
                },
                json: {id: [TAKEOUT_DELETE_ID]},
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(401);
            expect(res.body.message).to.equal('Invalid TVM service ticket');
        });

        it('should throw 401 when invalid user ticket', async () => {
            const res = await client.post<ResponseError>(url, {
                searchParams: {request_id: requestId},
                headers: {
                    'X-Ya-Service-Ticket': SERVICE_TICKET_TAKEOUT,
                    'X-Ya-User-Ticket': 'shto-to-ne-to'
                },
                json: {id: [TAKEOUT_DELETE_ID]},
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(401);
            expect(res.body.message).to.equal('Invalid TVM user ticket');
        });

        it('should throw 403 when unknown service ticket', async () => {
            const res = await client.post<ResponseError>(url, {
                searchParams: {request_id: requestId},
                headers: {
                    'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET,
                    'X-Ya-User-Ticket': USER1_TICKET
                },
                json: {id: [TAKEOUT_DELETE_ID]},
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(403);
            expect(res.body.message).to.equal('TVM service does not have enough access rights');
        });
    });

    describe('delete data', () => {
        beforeEach(clearDb);

        async function insertList(uid: string, recordId: string, status: ListStatus = 'shared') {
            return dbClient.executeWriteQuery<{id: string, seed: number}>({
                text: `
                    INSERT INTO lists
                    (uid, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                    ($1, $2, 1, '2022-03-22', $3, '[]', '{}')
                    RETURNING id, seed`,
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

        it('should set status = `deleted` for user\'s lists', async () => {
            const {data: {rows: [{id: firstListId, seed: firstSeed}]}} = await insertList('1', '1_record_id');
            const {data: {rows: [{id: secondListId, seed: secondSeed}]}} = await insertList('1', '2_record_id');
            await insertList('2', '3_record_id');

            // save previous deletion
            const {data: {rows: [{
                id: deletedListId,
                seed: deletedListSeed
            }]}} = await insertList('1', '4_record_id', 'deleted');

            const oldRequestId = 'old_request_id';
            await dbClient.executeWriteQuery({
                text: `
                    INSERT INTO deleted_users (uid, attributes, deleted_at) VALUES ($1, $2, $3);`,
                values: [
                    '1',
                    {
                        request_id: oldRequestId,
                        source: 'takeout',
                        lists: [
                            {id: deletedListId, public_id: encodePublicId(deletedListId, deletedListSeed)}
                        ]
                    },
                    new Date(Date.now() - 60 * 1000) // 1 minute ago
                ]
            });

            const res = await client.post(url, {
                searchParams: {request_id: requestId},
                headers: user1Headers,
                json: {id: [TAKEOUT_DELETE_ID]},
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({status: 'ok'});

            const {data: {rows: lists}} = await dbClient.executeReadQuery(
                `SELECT uid, record_id, status FROM lists ORDER BY record_id`
            );
            expect(lists).to.deep.equal([
                {uid: '1', record_id: '1_record_id', status: 'deleted'},
                {uid: '1', record_id: '2_record_id', status: 'deleted'},
                {uid: '2', record_id: '3_record_id', status: 'shared'},
                {uid: '1', record_id: '4_record_id', status: 'deleted'}
            ]);

            const {data: {rows: deletedUsers}} = await dbClient.executeReadQuery(
                `SELECT uid, attributes FROM deleted_users ORDER BY deleted_at`
            );
            expect(deletedUsers).to.deep.equal([
                {
                    uid: '1',
                    attributes: {
                        request_id: oldRequestId,
                        source: 'takeout',
                        lists: [
                            {id: deletedListId, public_id: encodePublicId(deletedListId, deletedListSeed)}
                        ]
                    }
                },
                {
                    uid: '1',
                    attributes: {
                        request_id: requestId,
                        source: 'takeout',
                        lists: [
                            {id: firstListId, public_id: encodePublicId(firstListId, firstSeed)},
                            {id: secondListId, public_id: encodePublicId(secondListId, secondSeed)}
                        ]
                    }
                }
            ]);
        });

        it('should delete subscriptions', async () => {
            const {data: {rows: [{id: firstListId}]}} = await insertList('2', '1_record_id');
            const {data: {rows: [{id: secondListId}]}} = await insertList('3', '2_record_id');
            await insertSubscription('1', firstListId);
            await insertSubscription('1', secondListId);
            await insertSubscription('2', secondListId);

            const res = await client.post(url, {
                searchParams: {request_id: requestId},
                headers: user1Headers,
                json: {id: [TAKEOUT_DELETE_ID]},
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({status: 'ok'});

            const {data: {rows: lists}} = await dbClient.executeReadQuery(
                `SELECT uid, record_id, status FROM lists ORDER BY record_id`
            );
            expect(lists).to.deep.equal([
                {uid: '2', record_id: '1_record_id', status: 'shared'},
                {uid: '3', record_id: '2_record_id', status: 'shared'}
            ]);

            const {data: {rows: subscriptions}} = await dbClient.executeReadQuery(
                `SELECT uid, list_id FROM subscriptions`
            );
            expect(subscriptions).to.deep.equal([
                {uid: '2', list_id: secondListId}
            ]);

            const {data: {rows: deletedUsers}} = await dbClient.executeReadQuery(
                `SELECT uid, attributes FROM deleted_users`
            );
            expect(deletedUsers).to.deep.equal([{
                uid: '1',
                attributes: {
                    request_id: requestId,
                    source: 'takeout',
                    lists: []
                }
            }]);
        });

        it('should update user\'s lists statuses and delete subscriptions', async () => {
            const {data: {rows: [{id: firstListId, seed: firstSeed}]}} = await insertList('1', '1_record_id');
            const {data: {rows: [{id: secondListId}]}} = await insertList('2', '2_record_id');
            await insertSubscription('1', secondListId);
            await insertSubscription('2', firstListId);

            const res = await client.post(url, {
                searchParams: {request_id: requestId},
                headers: user1Headers,
                json: {id: [TAKEOUT_DELETE_ID]},
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({status: 'ok'});

            const {data: {rows: lists}} = await dbClient.executeReadQuery(
                `SELECT uid, record_id, status FROM lists ORDER BY record_id`
            );
            expect(lists).to.deep.equal([
                {uid: '1', record_id: '1_record_id', status: 'deleted'},
                {uid: '2', record_id: '2_record_id', status: 'shared'}
            ]);

            const {data: {rows: subscriptions}} = await dbClient.executeReadQuery(
                `SELECT uid, list_id FROM subscriptions`
            );
            expect(subscriptions).to.deep.equal([
                {uid: '2', list_id: firstListId}
            ]);

            const {data: {rows: deletedUsers}} = await dbClient.executeReadQuery(
                `SELECT uid, attributes FROM deleted_users`
            );
            expect(deletedUsers).to.deep.equal([{
                uid: '1',
                attributes: {
                    request_id: requestId,
                    source: 'takeout',
                    lists: [{id: firstListId, public_id: encodePublicId(firstListId, firstSeed)}]
                }
            }]);
        });
    });
});
