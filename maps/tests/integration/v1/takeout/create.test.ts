import {encodePublicId} from 'app/v1/helpers/public-id';
import {expect} from 'chai';
import * as fs from 'fs';
import {URL} from 'url';

import {app} from 'app/app';
import {dbClient} from 'app/lib/db-client';
import {ListStatus} from 'app/lib/api-types';
import {hostsLoader} from 'app/lib/hosts';
import {clearDb} from 'tests/helpers/clear-db';
import {client, TestServer} from 'tests/integration/test-server';
import {TvmDaemon} from 'tests/integration/tvm-daemon';
import {Hosts} from '@yandex-int/maps-host-configs';

const SERVICE_TICKET_TAKEOUT = fs.readFileSync('src/tests/fixtures/tvm-tickets/service/takeout').toString().trim();
const UNKNOWN_SERVICE_TICKET = fs.readFileSync('src/tests/fixtures/tvm-tickets/service/unknown').toString().trim();

interface ResponseError {
    message: string;
}

describe('POST /v1/takeout/', async () => {
    let tvmDaemon: TvmDaemon;
    let server: TestServer;
    let url: URL;
    let hosts: Hosts;

    before(async () => {
        server = await TestServer.start(app);
        tvmDaemon = await TvmDaemon.start();
        url = new URL(`${server.url}/v1/takeout/`);
        hosts = await hostsLoader.get();
    });

    after(async () => {
        await tvmDaemon.stop();
        await server.stop();
    });

    describe('check query and headers', () => {
        it('should throw 401 when headers does not contain service ticket', async () => {
            const res = await client.post<ResponseError>(url, {
                headers: {},
                json: {uid: '1'},
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(401);
            expect(res.body.message).to.equal('Invalid authentication data provided');
        });

        it('should throw 401 when invalid service ticket', async () => {
            const res = await client.post<ResponseError>(url, {
                headers: {
                    'X-Ya-Service-Ticket': 'shto-to-ne-to'
                },
                json: {uid: '1'},
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(401);
            expect(res.body.message).to.equal('Invalid TVM service ticket');
        });

        it('should throw 403 when unknown service ticket', async () => {
            const res = await client.post<ResponseError>(url, {
                headers: {
                    'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET
                },
                json: {uid: '1'},
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(403);
            expect(res.body.message).to.equal('TVM service does not have enough access rights');
        });
    });

    describe('get data', () => {
        beforeEach(clearDb);

        async function insertList(args: {
            uid: string,
            recordId: string,
            status: ListStatus
        }) {
            return dbClient.executeWriteQuery<{id: string, seed: number}>({
                text: `
                    INSERT INTO lists
                    (uid, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                    ($1, $2, 1, '2022-03-22', $3, '[]', '{}')
                    RETURNING id, seed`,
                values: [args.uid, args.recordId, args.status]
            });
        }

        async function insertSubscription(args: {uid: string, listId: string}) {
            await dbClient.executeWriteQuery({
                text: `
                    INSERT INTO subscriptions
                    (uid, list_id) VALUES
                    ($1, $2)`,
                values: [args.uid, args.listId]
            });
        }

        it('should return status = `no_data` when user does not have data', async () => {
            const res = await client.post(url, {
                headers: {
                    'X-Ya-Service-Ticket': SERVICE_TICKET_TAKEOUT
                },
                json: {uid: '1'},
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({status: 'no_data'});
        });

        it('should return status = `no_data` when user does not have public lists', async () => {
            await insertList({uid: '1', recordId: 'record_id', status: 'deleted'});

            const res = await client.post(url, {
                headers: {
                    'X-Ya-Service-Ticket': SERVICE_TICKET_TAKEOUT
                },
                json: {uid: '1'},
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({status: 'no_data'});
        });

        it('should return subscriptions as links in `data` field', async () => {
            const {data: {rows: [{id: firstListId, seed: firstSeed}]}} = await insertList({
                uid: '2',
                recordId: '1_record_id',
                status: 'shared'
            });
            const {data: {rows: [{id: secondListId, seed: secondSeed}]}} = await insertList({
                uid: '3',
                recordId: '2_record_id',
                status: 'shared'
            });
            const {data: {rows: [{id: thirdListId}]}} = await insertList({
                uid: '3',
                recordId: '3_record_id',
                status: 'shared'
            });
            await insertSubscription({uid: '1', listId: firstListId});
            await insertSubscription({uid: '1', listId: secondListId});
            await insertSubscription({uid: '2', listId: thirdListId});

            const res = await client.post(url, {
                headers: {
                    'X-Ya-Service-Ticket': SERVICE_TICKET_TAKEOUT
                },
                json: {uid: '1'},
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({
                status: 'ok',
                data: {
                    'subscriptions.json': JSON.stringify([
                        `${hosts.maps_RU}?bookmarks[publicId]=${encodePublicId(firstListId, firstSeed)}`,
                        `${hosts.maps_RU}?bookmarks[publicId]=${encodePublicId(secondListId, secondSeed)}`
                    ])
                }
            });
        });

        it('should return user\'s lists as `file_links` field', async () => {
            const {data: {rows: [{id: firstListId, seed: firstSeed}]}} = await insertList({
                uid: '1',
                recordId: '1_record_id',
                status: 'shared'
            });
            const {data: {rows: [{id: secondListId, seed: secondSeed}]}} = await insertList({
                uid: '1',
                recordId: '2_record_id',
                status: 'shared'
            });
            await insertList({
                uid: '2',
                recordId: '3_record_id',
                status: 'shared'
            });

            const res = await client.post(url, {
                headers: {
                    'X-Ya-Service-Ticket': SERVICE_TICKET_TAKEOUT
                },
                json: {uid: '1'},
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({
                status: 'ok',
                file_links: [
                    `${url.protocol}//${url.hostname}/v1/takeout/lists/${encodePublicId(firstListId, firstSeed)}`,
                    `${url.protocol}//${url.hostname}/v1/takeout/lists/${encodePublicId(secondListId, secondSeed)}`
                ]
            });
        });

        it('should return user\'s lists and subscriptions', async () => {
            const {data: {rows: [{id: firstListId, seed: firstSeed}]}} = await insertList({
                uid: '1',
                recordId: '1_record_id',
                status: 'shared'
            });
            const {data: {rows: [{id: secondListId, seed: secondSeed}]}} = await insertList({
                uid: '2',
                recordId: '2_record_id',
                status: 'shared'
            });
            await insertSubscription({uid: '1', listId: secondListId});
            await insertSubscription({uid: '2', listId: firstListId});

            const res = await client.post(url, {
                headers: {
                    'X-Ya-Service-Ticket': SERVICE_TICKET_TAKEOUT
                },
                json: {uid: '1'},
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({
                status: 'ok',
                data: {
                    'subscriptions.json': JSON.stringify([
                        `${hosts.maps_RU}?bookmarks[publicId]=${encodePublicId(secondListId, secondSeed)}`
                    ])
                },
                file_links: [
                    `${url.protocol}//${url.hostname}/v1/takeout/lists/${encodePublicId(firstListId, firstSeed)}`
                ]
            });
        });
    });
});
