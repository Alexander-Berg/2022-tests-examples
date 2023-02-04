import {expect} from 'chai';
import * as fs from 'fs';
import {URL} from 'url';
import {app} from 'app/app';
import {encodePublicId} from 'app/v1/helpers/public-id';
import {ListAttributes, SharedBookmark} from 'app/lib/api-types';
import {dbClient} from 'app/lib/db-client';
import {clearDb} from 'tests/helpers/clear-db';
import {client, TestServer} from 'tests/integration/test-server';
import {TvmDaemon} from 'tests/integration/tvm-daemon';

const SERVICE_TICKET_TAKEOUT = fs.readFileSync('src/tests/fixtures/tvm-tickets/service/takeout').toString().trim();
const UNKNOWN_SERVICE_TICKET = fs.readFileSync('src/tests/fixtures/tvm-tickets/service/unknown').toString().trim();

interface ResponseError {
    message: string;
}

describe('GET /v1/takeout/lists/:id', async () => {
    let tvmDaemon: TvmDaemon;
    let server: TestServer;

    before(async () => {
        server = await TestServer.start(app);
        tvmDaemon = await TvmDaemon.start();
    });

    after(async () => {
        await tvmDaemon.stop();
        await server.stop();
    });

    describe('check params and headers', () => {
        it('should throw 401 when headers does not contain service ticket', async () => {
            const url = new URL(`${server.url}/v1/takeout/lists/gKZ-xkEC`);
            const res = await client.get<ResponseError>(url, {
                headers: {},
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(401);
            expect(res.body.message).to.equal('Invalid authentication data provided');
        });

        it('should throw 401 when invalid service ticket', async () => {
            const url = new URL(`${server.url}/v1/takeout/lists/gKZ-xkEC`);
            const res = await client.get<ResponseError>(url, {
                headers: {
                    'X-Ya-Service-Ticket': 'shto-to-ne-to'
                },
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(401);
            expect(res.body.message).to.equal('Invalid TVM service ticket');
        });

        it('should throw 403 when unknown service ticket', async () => {
            const url = new URL(`${server.url}/v1/takeout/lists/gKZ-xkEC`);
            const res = await client.get<ResponseError>(url, {
                headers: {
                    'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET
                },
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(403);
            expect(res.body.message).to.equal('TVM service does not have enough access rights');
        });

        it('should throw 400 when id has an incorrect format', async () => {
            const url = new URL(`${server.url}/v1/takeout/lists/123`);
            const res = await client.get<ResponseError>(url, {
                headers: {
                    'X-Ya-Service-Ticket': SERVICE_TICKET_TAKEOUT
                },
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(400);
            expect(res.body.message).to.equal('Error validating path parameters: "id" with value "123" ' +
                'fails to match the required pattern: /^[-_a-zA-Z0-9]{8}$|^[-_a-zA-Z0-9]{14}$/');
        });
    });

    describe('get list data', () => {
        beforeEach(clearDb);

        async function insertList(args: {
            uid: string,
            id: string,
            seed: number,
            recordId: string,
            bookmarks: SharedBookmark[],
            attributes: ListAttributes
        }) {
            return dbClient.executeWriteQuery<{id: string, seed: number}>({
                text: `
                    INSERT INTO lists
                    (id, seed, uid, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                    ($1, $2, $3, $4, 1, '2022-03-22', 'shared', $5, $6)
                    RETURNING id, seed`,
                values: [
                    args.id,
                    args.seed,
                    args.uid,
                    args.recordId,
                    JSON.stringify(args.bookmarks),
                    args.attributes
                ]
            });
        }

        it('should throw 404 when list not found', async () => {
            await insertList({
                uid: '1',
                recordId: 'record_id',
                id: '1',
                seed: 2,
                bookmarks: [],
                attributes: {}
            }); // another list
            const publicId = encodePublicId('3', 4);
            const url = new URL(`${server.url}/v1/takeout/lists/${publicId}`);
            const res = await client.get<ResponseError>(url, {
                headers: {
                    'X-Ya-Service-Ticket': SERVICE_TICKET_TAKEOUT
                },
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(404);
            expect(res.body.message).to.equal(`List ${publicId} not found`);
        });

        it('should return list\'s data', async () => {
            const {data: {rows: [storedList]}} = await insertList({
                uid: '1',
                recordId: 'record_id',
                id: '1',
                seed: 2,
                bookmarks: [
                    {
                        record_id: 'b_1',
                        title: 'First bookmark',
                        uri: 'ymapsbm1://org?oid=12345',
                        description: 'Orange color',
                        comment: 'You have never seen this'
                    },
                    {
                        record_id: 'b_1',
                        title: 'Second bookmark',
                        uri: 'ymapsbm1://org?oid=67890',
                        description: 'Black color'
                    }
                ],
                attributes: {
                    title: 'My favorite list',
                    description: 'The best list ever',
                    icon: 'mountain:#3cb200',
                    bookmarks_count: 2
                }
            });
            const url = new URL(`${server.url}/v1/takeout/lists/${encodePublicId(storedList.id, storedList.seed)}`);

            const res = await client.get(url, {
                headers: {
                    'X-Ya-Service-Ticket': SERVICE_TICKET_TAKEOUT
                },
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({
                record_id: 'record_id',
                title: 'My favorite list',
                description: 'The best list ever',
                icon: 'mountain:#3cb200',
                bookmarks: [
                    {
                        record_id: 'b_1',
                        title: 'First bookmark',
                        uri: 'ymapsbm1://org?oid=12345',
                        description: 'Orange color',
                        comment: 'You have never seen this'
                    },
                    {
                        record_id: 'b_1',
                        title: 'Second bookmark',
                        uri: 'ymapsbm1://org?oid=67890',
                        description: 'Black color'
                    }
                ]
            });
        });

        it('should return default data in fields when list is empty', async () => {
            const {data: {rows: [storedList]}} = await insertList({
                uid: '1',
                recordId: 'record_id',
                id: '1',
                seed: 2,
                bookmarks: [],
                attributes: {}
            });
            const url = new URL(`${server.url}/v1/takeout/lists/${encodePublicId(storedList.id, storedList.seed)}`);

            const res = await client.get(url, {
                headers: {
                    'X-Ya-Service-Ticket': SERVICE_TICKET_TAKEOUT
                },
                responseType: 'json'
            });

            expect(res.statusCode).to.equal(200);
            expect(res.body).to.deep.equal({
                record_id: 'record_id',
                title: '',
                description: '',
                icon: '',
                bookmarks: []
            });
        });
    });
});
