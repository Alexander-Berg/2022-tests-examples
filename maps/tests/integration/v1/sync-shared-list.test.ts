import {expect} from 'chai';
import * as fs from 'fs';
import {URL} from 'url';
import {app} from 'app/app';
import {dbClient} from 'app/lib/db-client';
import {decodePublicId, encodePublicId} from 'app/v1/helpers/public-id';
import {client, TestServer} from 'tests/integration/test-server';
import {TvmDaemon} from 'tests/integration/tvm-daemon';
import {SharedList, ListResult, ListStatus, SharedBookmark, ListAttributes} from 'app/lib/api-types';
import {selectList, StoredSeed} from 'tests/helpers/db';
import {clearDb} from 'tests/helpers/clear-db';
import {sleep} from 'tests/helpers/sleep';
import {expectArraysDeepEqual} from 'tests/helpers/validation';
import {random} from 'tests/helpers/generation';
import {config} from 'app/config';
import {BanStatus} from 'app/lib/banned-cache';
import {randomBytes} from 'crypto';

// TODO: Implement ticket auto-generation
const USER_111_TICKET = fs.readFileSync('src/tests/fixtures/tvm-tickets/user/uid-111').toString().trim();
const USER_999_TICKET = fs.readFileSync('src/tests/fixtures/tvm-tickets/user/uid-999').toString().trim();
const SERVICE_TICKET_MAPS = fs.readFileSync('src/tests/fixtures/tvm-tickets/service/maps').toString().trim();
const UNKNOWN_SERVICE_TICKET = fs.readFileSync('src/tests/fixtures/tvm-tickets/service/unknown').toString().trim();

const minute = 60 * 1000;

interface BanRecord {
    uid: string;
    status: BanStatus;
    previous_status: BanStatus | null;
    exclusive_limit: number;
    reviewed: boolean;
}

interface ListInfo {
    public_id: string;
    record_id: string;
    revision: number;
    status: ListStatus;
    timestamp: number | Date;
}

describe('POST /v1/sync_shared_lists', () => {
    let tvmDaemon: TvmDaemon;
    let server: TestServer;
    let url: URL;

    const auth111Headers = {
        'X-Ya-Service-Ticket': SERVICE_TICKET_MAPS,
        'X-Ya-User-Ticket': USER_111_TICKET
    };

    before(async () => {
        server = await TestServer.start(app);
        tvmDaemon = await TvmDaemon.start();
        url = new URL(`${server.url}/v1/sync_shared_lists`);
    });

    after(async () => {
        await tvmDaemon.stop();
        await server.stop();
    });

    describe('schema and headers validation', () => {
        it('should fail if body have invalid format (empty object)', async () => {
            const body = {};
            const expected = {
                statusCode: 400,
                error: 'Bad Request',
                message: 'Error validating body: \"value\" must be an array'
            };

            const res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(400);
            const actual = JSON.parse(res.body);
            expect(actual).to.deep.equal(expected);
        });

        it('should fail if lists contain no properties', async () => {
            const body = [{}];
            const expected = {
                statusCode: 400,
                error: 'Bad Request',
                message: 'Error validating body: \"[0].record_id\" is required'
            };

            const res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(400);
            const actual = JSON.parse(res.body);
            expect(actual).to.deep.equal(expected);
        });

        it('should fail if bookmarks contain no properties', async () => {
            const body = [
                {
                    record_id: 'record-l-1',
                    title: 'Stub title',
                    icon: 'rubric:color',
                    description: 'Stub description',
                    revision: 1,
                    status: 'shared',
                    bookmarks: [{}]
                }
            ];
            const expected = {
                statusCode: 400,
                error: 'Bad Request',
                message: 'Error validating body: \"[0].bookmarks[0].record_id\" is required'
            };

            const res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(400);
            const actual = JSON.parse(res.body);
            expect(actual).to.deep.equal(expected);
        });

        it('should fail if the "icon" property length is more than 128', async () => {
            const icon = randomBytes(65).toString('hex');
            expect(icon.length).to.be.equal(130);
            const body = [
                {
                    record_id: 'record-l-1',
                    title: 'Stub title',
                    icon,
                    description: 'Stub description',
                    revision: 1,
                    status: 'shared',
                    bookmarks: []
                }
            ];
            const expected = {
                statusCode: 400,
                error: 'Bad Request',
                message: 'Error validating body: \"[0].icon\" length must be less than or equal to 128 characters long'
            };

            const res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(400);
            const actual = JSON.parse(res.body);
            expect(actual).to.deep.equal(expected);
        });

        it('should throw 401 when service ticket is missing', async () => {
            const res = await client.post(url, {
                headers: {
                    'X-Ya-User-Ticket': USER_111_TICKET
                },
                json: {}
            });

            expect(res.statusCode).to.equal(401);
            expect(JSON.parse(res.body).message).to.equal('Invalid authentication data provided');
        });

        it('should throw 401 when service ticket is invalid', async () => {
            const res = await client.post(url, {
                headers: {
                    'X-Ya-Service-Ticket': 'shto-to-ne-to',
                    'X-Ya-User-Ticket': USER_111_TICKET
                },
                json: {}
            });

            expect(res.statusCode).to.equal(401);
            expect(JSON.parse(res.body).message).to.equal('Invalid TVM service ticket');
        });

        it('should throw 401 when user ticket is missing', async () => {
            const res = await client.post(url, {
                headers: {
                    'X-Ya-Service-Ticket': SERVICE_TICKET_MAPS
                },
                json: {}
            });

            expect(res.statusCode).to.equal(401);
            expect(JSON.parse(res.body).message).to.equal('Invalid authentication data provided');
        });

        it('should throw 401 when user ticket is invalid', async () => {
            const res = await client.post(url, {
                headers: {
                    'X-Ya-Service-Ticket': SERVICE_TICKET_MAPS,
                    'X-Ya-User-Ticket': 'shto-to-ne-to'
                },
                json: {}
            });

            expect(res.statusCode).to.equal(401);
            expect(JSON.parse(res.body).message).to.equal('Invalid TVM user ticket');
        });

        it('should throw 403 when unknown service ticket', async () => {
            const res = await client.post(url, {
                headers: {
                    'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET,
                    'X-Ya-User-Ticket': USER_111_TICKET
                },
                json: {}
            });

            expect(res.statusCode).to.equal(403);
            expect(JSON.parse(res.body).message).to.equal('TVM service does not have enough access rights');
        });
    });

    describe('violator\'s ban logic', () => {
        beforeEach(clearDb);

        interface GeneratedList {
            record_id: string;
            revision: number;
            status: ListStatus;
            timestamp?: number;
            title: string;
            icon?: string;
            description?: string;
            bookmarks: SharedBookmark[];
        }

        interface ListToSave {
            record_id: string;
            revision: number;
            status: ListStatus;
            timestamp?: number;
            attributes: ListAttributes;
            bookmarks: SharedBookmark[];
        }

        async function getSavedListsCount(uid: string): Promise<number> {
            const {data: {rows: [{count: result}]}} = await dbClient.executeReadQuery<{count: number}>({
                text: 'SELECT COUNT(*) FROM lists WHERE uid = $1',
                values: [uid]
            });

            return Number(result);
        }

        async function getBanRecord(uid: string): Promise<BanRecord> {
            const {data: {rows: [banRecord]}} = await dbClient.executeReadQuery<BanRecord>({
                text: 'SELECT * FROM banned_violators WHERE uid = $1',
                values: [uid]
            });

            return banRecord;
        }

        async function createBanRecord(banRecord: BanRecord) {
            await dbClient.executeWriteQuery({
                text: `INSERT INTO banned_violators (uid, status, previous_status, exclusive_limit, reviewed) VALUES
                ($1, $2, $3, $4, $5)`,
                values: [
                    banRecord.uid,
                    banRecord.status,
                    banRecord.previous_status,
                    banRecord.exclusive_limit,
                    banRecord.reviewed
                ]
            });
        }

        async function insertLists(uid: string, lists: GeneratedList[]) {
            const listsToSave: ListToSave[] = lists.map((list) => ({
                record_id: list.record_id,
                revision: list.revision,
                status: list.status,
                timestamp: list.timestamp,
                attributes: {
                    title: list.title || '',
                    icon: list.icon || '',
                    description: list.description || '',
                    bookmarks_count: list.bookmarks.length
                },
                bookmarks: list.bookmarks
            }));

            await dbClient.executeWriteQuery({
                text: `
                    INSERT INTO lists (uid, record_id, revision, timestamp, status, attributes, bookmarks)
                    SELECT
                        $1 AS uid,
                        got.record_id,
                        got.revision,
                        to_timestamp(got.timestamp) AS timestamp,
                        got.status,
                        got.attributes,
                        got.bookmarks
                    FROM json_to_recordset($2) AS got (
                        record_id varchar(128),
                        revision integer,
                        "timestamp" bigint,
                        status list_status,
                        attributes jsonb,
                        bookmarks text
                    )`,
                values: [uid, JSON.stringify(listsToSave)]
            });
        }

        it('should not pass the users with reviewed ban records only', async () => {
            const uid111 = '111';
            const uid999 = '999';
            const uid999Headers = {
                'X-Ya-Service-Ticket': SERVICE_TICKET_MAPS,
                'X-Ya-User-Ticket': USER_999_TICKET
            };

            await createBanRecord({
                uid: uid111,
                status: 'banned',
                previous_status: null,
                exclusive_limit: 0,
                reviewed: false
            });
            await createBanRecord({
                uid: uid999,
                status: 'banned',
                previous_status: null,
                exclusive_limit: 0,
                reviewed: true
            });

            // Delay the test a bit to allow the server to renewe its ban cache
            await sleep(config['app.banCacheRenewTimeoutMs']);

            const body = [
                {
                    record_id: 'record-l-1',
                    revision: 1,
                    title: 'Shared list',
                    status: 'shared',
                    bookmarks: []
                }
            ];

            // We should allow the 111 user to sync his lists
            let res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            let actual = JSON.parse(res.body);
            expect(actual.length).to.be.equal(1);

            const uid111StoredListsCount = await getSavedListsCount(uid111);
            expect(uid111StoredListsCount).to.be.equal(1);

            // We should reject the request of the 999 user
            res = await client.post(url, {
                headers: uid999Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(403);
            actual = JSON.parse(res.body);
            expect(actual).to.be.deep.equal({
                statusCode: 403,
                error: 'Forbidden',
                message: 'The user does not have rights to make this operation'
            });

            const uid999StoredListsCount = await getSavedListsCount(uid999);
            expect(uid999StoredListsCount).to.be.equal(0);

            // Make sure ban records didn't changed
            const uid111BanRecord = await getBanRecord(uid111);
            expect(uid111BanRecord).to.be.deep.equal({
                uid: uid111,
                status: 'banned',
                previous_status: null,
                exclusive_limit: 0,
                reviewed: false
            });

            const uid999BanRecord = await getBanRecord(uid999);
            expect(uid999BanRecord).to.be.deep.equal({
                uid: uid999,
                status: 'banned',
                previous_status: null,
                exclusive_limit: 0,
                reviewed: true
            });
        });

        it('should process request if user did not exceeded the limit', async () => {
            const uid = '111';
            const listsCount = config['app.writeListsBanLimit'];
            const lists = [];
            const now = Date.now();
            for (let i = 0; i < listsCount; i++) {
                lists.push({
                    record_id: `record-l-${i}`,
                    revision: i,
                    title: `List #${i}`,
                    status: 'shared',
                    bookmarks: []
                });
            }

            const res = await client.post(url, {
                headers: auth111Headers,
                json: lists
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual.length).to.be.equal(listsCount);

            for (const actualList of actual) {
                const sourceList = lists.find((list) => list.record_id === actualList.record_id);
                expect(sourceList).not.to.be.undefined;
                expect(actualList).to.include({
                    record_id: sourceList!.record_id,
                    revision: sourceList!.revision,
                    status: sourceList!.status
                });
                expect(actualList.public_id).not.to.be.undefined;
                expect(actualList.timestamp).to.be.greaterThan(now);
            }

            const storedListsCount = await getSavedListsCount(uid);
            expect(storedListsCount).to.be.equal(listsCount);

            const banRecord = await getBanRecord(uid);
            expect(banRecord).to.be.undefined;
        });

        it('should process request and ban the user if has exceeded the limit', async () => {
            const uid = '111';
            const timestamp = Date.now();
            const icon = 'rubric:color';
            const description = 'Stub description';

            const presavedListsCount = config['app.writeListsBanLimit'];
            const presavedLists: GeneratedList[] = [];
            for (let i = 0; i < presavedListsCount; i++) {
                presavedLists.push({
                    record_id: `record-presaved-l-${i}`,
                    revision: i,
                    title: `List #${i}`,
                    icon,
                    description,
                    timestamp,
                    status: 'shared',
                    bookmarks: []
                });
            }

            await insertLists(uid, presavedLists);

            const listsCount = config['app.writeListsBanLimit'];
            const lists: GeneratedList[] = [];
            for (let i = 0; i < listsCount; i++) {
                lists.push({
                    record_id: `record-l-${i}`,
                    revision: i,
                    title: `List #${i}`,
                    status: 'shared',
                    bookmarks: []
                });
            }

            const res = await client.post(url, {
                headers: auth111Headers,
                json: lists
            });

            // Wait a bit to make sure the ban records were updated in DB
            await sleep(config['app.banCacheRenewTimeoutMs']);

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            const totalLists = lists.concat(presavedLists.map((list) => {
                list.status = 'closed';
                return list;
            }));

            expect(actual.length).to.be.equal(totalLists.length);

            for (const actualList of actual) {
                const sourceList = totalLists.find((list) => list.record_id === actualList.record_id);
                expect(sourceList).not.to.be.undefined;
                expect(actualList).to.include({
                    record_id: sourceList!.record_id,
                    revision: sourceList!.revision,
                    status: sourceList!.status
                });
                expect(actualList.public_id).not.to.be.undefined;
                expect(actualList.timestamp).not.to.be.undefined;
            }

            const storedListsCount = await getSavedListsCount(uid);
            expect(storedListsCount).to.be.equal(totalLists.length);

            const banRecord = await getBanRecord(uid);
            expect(banRecord).to.be.deep.equal({
                uid,
                status: 'banned',
                previous_status: null,
                exclusive_limit: 0,
                reviewed: false
            });
        });

        it('should process request and ban the user if it contains more lists than the limit', async () => {
            const uid = '111';
            const listsCount = config['app.writeListsBanLimit'] + 1;
            const lists = [];
            for (let i = 0; i < listsCount; i++) {
                lists.push({
                    record_id: `record-l-${i}`,
                    revision: i,
                    title: `List #${i}`,
                    status: 'shared',
                    bookmarks: []
                });
            }

            const res = await client.post(url, {
                headers: auth111Headers,
                json: lists
            });

            // Wait a bit to make sure the ban records were updated in DB
            await sleep(config['app.banCacheRenewTimeoutMs']);

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual.length).to.be.equal(listsCount);

            for (const actualList of actual) {
                const sourceList = lists.find((list) => list.record_id === actualList.record_id);
                expect(sourceList).not.to.be.undefined;
                expect(actualList).to.include({
                    record_id: sourceList!.record_id,
                    revision: sourceList!.revision,
                    status: sourceList!.status
                });
                expect(actualList.public_id).not.to.be.undefined;
            }

            const storedListsCount = await getSavedListsCount(uid);
            expect(storedListsCount).to.be.equal(listsCount);

            const banRecord = await getBanRecord(uid);
            expect(banRecord).to.be.deep.equal({
                uid,
                status: 'banned',
                previous_status: null,
                exclusive_limit: 0,
                reviewed: false
            });
        });

        it('should process request of the whitelisted user if he did not exceeded exclusive limit', async () => {
            const uid = '111';
            const limit = config['app.writeListsBanLimit'];

            const listsCounts = [0, limit - 1, limit, limit + 1];
            for (const listsCount of listsCounts) {
                await clearDb();

                const exclusiveLimit = limit + 2;
                await createBanRecord({
                    uid,
                    status: 'whitelisted',
                    previous_status: 'banned',
                    exclusive_limit: exclusiveLimit,
                    reviewed: true
                });

                // Delay the test a bit to allow the server to renew its ban cache
                await sleep(config['app.banCacheRenewTimeoutMs']);

                const lists = [];
                for (let i = 0; i < listsCount; i++) {
                    lists.push({
                        record_id: `record-l-${i}`,
                        revision: i,
                        title: `List #${i}`,
                        status: 'shared',
                        bookmarks: []
                    });
                }

                const res = await client.post(url, {
                    headers: auth111Headers,
                    json: lists
                });

                expect(res.statusCode).to.be.equal(200);
                const actual = JSON.parse(res.body);
                expect(actual.length).to.be.equal(listsCount);

                for (const actualList of actual) {
                    const sourceList = lists.find((list) => list.record_id === actualList.record_id);
                    expect(sourceList).not.to.be.undefined;
                    expect(actualList).to.include({
                        record_id: sourceList!.record_id,
                        revision: sourceList!.revision,
                        status: sourceList!.status
                    });
                    expect(actualList.public_id).not.to.be.undefined;
                }

                const storedListsCount = await getSavedListsCount(uid);
                expect(storedListsCount).to.be.equal(listsCount);

                const banRecord = await getBanRecord(uid);
                expect(banRecord).to.be.deep.equal({
                    uid,
                    status: 'whitelisted',
                    previous_status: 'banned',
                    exclusive_limit: exclusiveLimit,
                    reviewed: true
                });
            }
        });

        it('should process request and ban the user if he has exceeded the whitelist exclusive limit', async () => {
            const uid = '111';
            const icon = 'rubric:color';
            const description = 'Stub description';

            const exclusiveLimit = config['app.writeListsBanLimit'] + 1;
            await createBanRecord({
                uid,
                status: 'whitelisted',
                previous_status: 'banned',
                exclusive_limit: exclusiveLimit,
                reviewed: true
            });

            // Delay the test a bit to allow the server to renewe its ban cache
            await sleep(config['app.banCacheRenewTimeoutMs']);

            const timestamp = new Date('2021-09-15').getTime();
            const presavedListsCount = exclusiveLimit;
            const presavedLists: GeneratedList[] = [];
            for (let i = 0; i < presavedListsCount; i++) {
                presavedLists.push({
                    record_id: `record-presaved-l-${i}`,
                    revision: i,
                    title: `List #${i}`,
                    icon,
                    description,
                    timestamp,
                    status: 'shared',
                    bookmarks: []
                });
            }

            await insertLists(uid, presavedLists);

            const listsCount = exclusiveLimit;
            const lists: GeneratedList[] = [];
            for (let i = 0; i < listsCount; i++) {
                lists.push({
                    record_id: `record-l-${i}`,
                    revision: i,
                    title: `List #${i}`,
                    icon,
                    description,
                    status: 'shared',
                    bookmarks: []
                });
            }

            const res = await client.post(url, {
                headers: auth111Headers,
                json: lists
            });

            // Wait a bit to make sure the ban records were updated in DB
            await sleep(config['app.banCacheRenewTimeoutMs']);

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            const totalLists = lists.concat(presavedLists.map((list) => {
                list.status = 'closed';
                return list;
            }));

            expect(actual.length).to.be.equal(totalLists.length);

            for (const actualList of actual) {
                const sourceList = totalLists.find((list) => list.record_id === actualList.record_id);
                expect(sourceList).not.to.be.undefined;
                expect(actualList).to.include({
                    record_id: sourceList!.record_id,
                    revision: sourceList!.revision,
                    status: sourceList!.status
                });
                expect(actualList.public_id).not.to.be.undefined;
                expect(actualList.timestamp).not.to.be.undefined;
            }

            const storedListsCount = await getSavedListsCount(uid);
            expect(storedListsCount).to.be.equal(totalLists.length);

            const banRecord = await getBanRecord(uid);
            expect(banRecord).to.be.deep.equal({
                uid,
                status: 'banned',
                previous_status: 'whitelisted',
                exclusive_limit: exclusiveLimit,
                reviewed: false
            });
        });

        it('should process request and ban the user if it contains more lists than the exclusive limit', async () => {
            const uid = '111';

            const exclusiveLimit = config['app.writeListsBanLimit'] + 1;
            await createBanRecord({
                uid,
                status: 'whitelisted',
                previous_status: 'banned',
                exclusive_limit: exclusiveLimit,
                reviewed: true
            });

            // Delay the test a bit to allow the server to renewe its ban cache
            await sleep(config['app.banCacheRenewTimeoutMs']);

            const listsCount = exclusiveLimit + 1;
            const lists = [];
            for (let i = 0; i < listsCount; i++) {
                lists.push({
                    record_id: `record-l-${i}`,
                    revision: i,
                    title: `List #${i}`,
                    status: 'shared',
                    bookmarks: []
                });
            }

            const res = await client.post(url, {
                headers: auth111Headers,
                json: lists
            });

            // Wait a bit to make sure the ban records were updated in DB
            await sleep(config['app.banCacheRenewTimeoutMs']);

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual.length).to.be.equal(listsCount);

            for (const actualList of actual) {
                const sourceList = lists.find((list) => list.record_id === actualList.record_id);
                expect(sourceList).not.to.be.undefined;
                expect(actualList).to.include({
                    record_id: sourceList!.record_id,
                    revision: sourceList!.revision,
                    status: sourceList!.status
                });
                expect(actualList.public_id).not.to.be.undefined;
            }

            const storedListsCount = await getSavedListsCount(uid);
            expect(storedListsCount).to.be.equal(listsCount);

            const banRecord = await getBanRecord(uid);
            expect(banRecord).to.be.deep.equal({
                uid,
                status: 'banned',
                previous_status: 'whitelisted',
                exclusive_limit: exclusiveLimit,
                reviewed: false
            });
        });
    });

    describe('for one user', () => {
        beforeEach(clearDb);

        it('should save list without a title in attributes if the "title" property is missed', async () => {
            const uid = '111';
            const now = Date.now();
            const recordId = 'record-l-1';
            const revision = 1;
            const status = 'shared';
            const icon = 'rubric:color';
            const description = 'Stub description';
            const body = [
                {
                    record_id: recordId,
                    revision: 1,
                    icon,
                    description,
                    status,
                    bookmarks: []
                }
            ];

            const res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);

            expect(actual.length).to.be.equal(1);

            expect(actual[0]).to.include({record_id: recordId, revision, status});
            expect(actual[0].public_id).not.to.be.undefined;

            const storedList = await selectList(actual[0].public_id);
            const {id, seed} = decodePublicId(actual[0].public_id)!;
            expect(storedList).to.deep.include({
                id,
                seed,
                uid,
                record_id: recordId,
                revision,
                attributes: {
                    icon,
                    description,
                    bookmarks_count: 0
                },
                status,
                bookmarks: []
            });
            expect(storedList.timestamp.getTime()).to.be.greaterThan(now);
        });

        it('should save list with empty bookmarks if the "bookmarks" property is missed', async () => {
            const uid = '111';
            const now = Date.now();
            const recordId = 'record-l-1';
            const revision = 5;
            const status = 'shared';
            const title = 'Sample list';
            const icon = 'rubric:color';
            const description = 'Stub description';
            const body = [{
                record_id: recordId,
                revision,
                title,
                icon,
                description,
                status
            }];

            const res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual.length).to.be.equal(1);

            expect(actual[0]).to.include({record_id: recordId, revision, status});
            expect(actual[0].public_id).not.to.be.undefined;

            const storedList = await selectList(actual[0].public_id);
            const {id, seed} = decodePublicId(actual[0].public_id)!;
            expect(storedList).to.deep.include({
                id,
                seed,
                uid,
                record_id: recordId,
                revision,
                attributes: {
                    title,
                    icon,
                    description
                },
                status,
                bookmarks: []
            });
            expect(storedList.timestamp.getTime()).to.be.greaterThan(now);
        });

        it('should save list without an icon in attributes if the "icon" property is missed', async () => {
            const uid = '111';
            const now = Date.now();
            const recordId = 'record-l-1';
            const revision = 5;
            const status = 'shared';
            const title = 'Sample list';
            const description = 'Stub description';
            const body = [{
                record_id: recordId,
                revision,
                title,
                description,
                status,
                bookmarks: []
            }];

            const res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual.length).to.be.equal(1);

            expect(actual[0]).to.include({record_id: recordId, revision, status});
            expect(actual[0].public_id).not.to.be.undefined;

            const storedList = await selectList(actual[0].public_id);
            const {id, seed} = decodePublicId(actual[0].public_id)!;
            expect(storedList).to.deep.include({
                id,
                seed,
                uid,
                record_id: recordId,
                revision,
                attributes: {
                    title,
                    description,
                    bookmarks_count: 0
                },
                status,
                bookmarks: []
            });
            expect(storedList.timestamp.getTime()).to.be.greaterThan(now);
        });

        it('should save list with empty icon if the "icon" property is empty string', async () => {
            const uid = '111';
            const now = Date.now();
            const recordId = 'record-l-1';
            const revision = 7;
            const status = 'shared';
            const title = 'Sample list';
            const icon = '';
            const description = 'Stub description';
            const body = [{
                record_id: recordId,
                revision,
                title,
                icon,
                description,
                status,
                bookmarks: []
            }];

            const res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual.length).to.be.equal(1);

            expect(actual[0]).to.include({record_id: recordId, revision, status});
            expect(actual[0].public_id).not.to.be.undefined;

            const storedList = await selectList(actual[0].public_id);
            const {id, seed} = decodePublicId(actual[0].public_id)!;
            expect(storedList).to.deep.include({
                id,
                seed,
                uid,
                record_id: recordId,
                revision,
                attributes: {
                    title,
                    icon,
                    description,
                    bookmarks_count: 0
                },
                status,
                bookmarks: []
            });
            expect(storedList.timestamp.getTime()).to.be.greaterThan(now);
        });

        it('should save list without a description if the "description" property is missed', async () => {
            const uid = '111';
            const now = Date.now();
            const recordId = 'record-l-1';
            const revision = 3;
            const status = 'shared';
            const title = 'Sample list';
            const icon = 'rubric:color';
            const body = [{
                record_id: recordId,
                revision,
                title,
                icon,
                status,
                bookmarks: []
            }];

            const res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual.length).to.be.equal(1);

            expect(actual[0]).to.include({record_id: recordId, revision, status});
            expect(actual[0].public_id).not.to.be.undefined;

            const storedList = await selectList(actual[0].public_id);
            const {id, seed} = decodePublicId(actual[0].public_id)!;
            expect(storedList).to.deep.include({
                id,
                seed,
                uid,
                record_id: recordId,
                revision,
                attributes: {
                    title,
                    icon,
                    bookmarks_count: 0
                },
                status,
                bookmarks: []
            });
            expect(storedList.timestamp.getTime()).to.be.greaterThan(now);
        });

        it('should save list with empty description if the "description" property is empty string', async () => {
            const uid = '111';
            const now = Date.now();
            const recordId = 'record-l-1';
            const revision = 1;
            const status = 'shared';
            const title = 'Sample list';
            const icon = 'rubric:color';
            const description = '';
            const body = [{
                record_id: recordId,
                revision,
                title,
                icon,
                description,
                status,
                bookmarks: []
            }];

            const res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual.length).to.be.equal(1);

            expect(actual[0]).to.include({record_id: recordId, revision, status});
            expect(actual[0].public_id).not.to.be.undefined;

            const storedList = await selectList(actual[0].public_id);
            const {id, seed} = decodePublicId(actual[0].public_id)!;
            expect(storedList).to.deep.include({
                id,
                seed,
                uid,
                record_id: recordId,
                revision,
                attributes: {
                    title,
                    icon,
                    description,
                    bookmarks_count: 0
                },
                status,
                bookmarks: []
            });
            expect(storedList.timestamp.getTime()).to.be.greaterThan(now);
        });

        it('should save empty list of bookmarks', async () => {
            const uid = '111';
            const now = Date.now();
            const recordId = 'record-l-1';
            const revision = 2;
            const title = 'Shared list';
            const status = 'shared';
            const icon = 'rubric:color';
            const description = 'Stub description';
            const body = [{
                record_id: recordId,
                title,
                revision,
                status,
                icon,
                description,
                bookmarks: []
            }];

            const res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual.length).to.be.equal(1);

            expect(actual[0]).to.include({record_id: recordId, revision, status});
            expect(actual[0].public_id).not.to.be.undefined;

            const storedList = await selectList(actual[0].public_id);
            const {id, seed} = decodePublicId(actual[0].public_id)!;
            expect(storedList).to.deep.include({
                id,
                seed,
                uid,
                record_id: recordId,
                revision,
                attributes: {
                    title,
                    icon,
                    description,
                    bookmarks_count: 0
                },
                status,
                bookmarks: []
            });
            expect(storedList.timestamp.getTime()).to.be.greaterThan(now);
        });

        it('should update list own properties (title)', async () => {
            const uid = '111';
            const internalId = random(1, 2000);
            const recordId = 'record-l-1';
            const revision = 3;
            const newRevision = 5;
            const now = Date.now();
            const status = 'shared';
            const title = 'Shared list';
            const icon = 'rubric:color';
            const description = 'Stub description';

            const {data: {rows: [{seed: seed}]}} = await dbClient.executeWriteQuery<StoredSeed>({
                text: `INSERT INTO lists
                    (uid, id, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                    ($1, $2, $3, $4, $5, $6, '[]', $7)
                    RETURNING seed`,
                values: [uid, internalId, recordId, revision, new Date(now - minute), status, {title}]
            });
            const publicId = encodePublicId(`${internalId}`, seed);

            const newTitle = 'New title';
            const body = [{
                record_id: recordId,
                title: newTitle,
                icon,
                description,
                revision: newRevision,
                status,
                bookmarks: []
            }];
            const expected = {
                public_id: publicId,
                record_id: recordId,
                revision: newRevision,
                status
            };

            const res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual.length).to.be.equal(1);
            expect(actual[0]).to.deep.include(expected);
            expect(actual[0].timestamp).to.be.greaterThan(now);

            const storedList = await selectList(publicId);
            expect(storedList).to.deep.include({
                uid,
                id: internalId,
                seed,
                record_id: recordId,
                revision: newRevision,
                attributes: {
                    title: newTitle,
                    icon,
                    description,
                    bookmarks_count: 0
                },
                status,
                bookmarks: []
            });
            expect(storedList.timestamp.getTime()).to.be.greaterThan(now);
        });

        it('should remove bookmarks, icon and description on closing (status = "closed")', async () => {
            const uid = '111';
            const internalId = random(1, 2000);
            const recordId = 'record-l-1';
            const revision = 3;
            const newRevision = 10;
            const now = Date.now();
            const title = 'Shared list';
            const status = 'shared';
            const icon = 'rubric:color';
            const description = 'Stub description';

            const {data: {rows: [{seed: seed}]}} = await dbClient.executeWriteQuery<StoredSeed>({
                text: `INSERT INTO lists
                    (uid, id, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                    ($1, $2, $3, $4, $5, $6, $7, $8)
                    RETURNING seed`,
                values: [
                    uid,
                    internalId,
                    recordId,
                    revision,
                    new Date(now - minute),
                    status,
                    JSON.stringify([
                        {
                            record_id: 'record-b-1-1',
                            title: `Stub bookmark #1-1`,
                            uri: `https://stub_1_1`,
                            description: ''
                        }
                    ]),
                    {title, icon, description, bookmarks_count: 1}
                ]
            });
            const publicId = encodePublicId(`${internalId}`, seed);

            const body = [{
                title: 'New title',
                record_id: recordId,
                revision: newRevision,
                status: 'closed',
                bookmarks: []
            }];
            const expected = {
                public_id: publicId,
                record_id: recordId,
                revision,
                status: 'closed'
            };

            const res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual[0]).to.deep.include(expected);

            const storedList = await selectList(publicId);
            expect(storedList).to.deep.include({
                uid,
                id: internalId,
                seed,
                record_id: recordId,
                revision,
                attributes: {
                    title,
                    bookmarks_count: 0
                },
                status: 'closed',
                bookmarks: []
            });
            expect(storedList.timestamp.getTime()).to.be.greaterThan(now);
        });

        it('should close empty list (empty body)', async () => {
            const uid = '111';
            const internalId = random(1, 2000);
            const recordId = 'record-l-1';
            const revision = 3;
            const now = Date.now();
            const storedTimestamp = new Date(now - minute);
            const title = 'Shared list';
            const status = 'shared';
            const anotherUid = '555';
            const anotherInternalId = internalId + 1;

            const {data: {rows: [{seed: seed}, {seed: anotherSeed}]}} = await dbClient.executeWriteQuery<StoredSeed>({
                text: `INSERT INTO lists
                    (uid, id, record_id, revision, timestamp, attributes, status, bookmarks) VALUES
                    ($1, $2, $3, $4, $5, $6, $7, $8),
                    ($9, $10, $3, $4, $5, $6, $7, $11)
                    RETURNING seed`,
                values: [
                    uid,
                    internalId,
                    recordId,
                    revision,
                    storedTimestamp,
                    {title, icon: 'a', description: 'b', bookmarks_count: 1},
                    status,
                    JSON.stringify([
                        {
                            record_id: 'record-b-1-1',
                            title: `Stub bookmark #1-1`,
                            uri: `https://stub_1_1`
                        }
                    ]),
                    anotherUid,
                    anotherInternalId,
                    JSON.stringify([
                        {
                            record_id: 'record-b-1-2',
                            title: `Stub bookmark #1-2`,
                            uri: `https://stub_1_2`
                        }
                    ])
                ]
            });

            const publicId = encodePublicId(`${internalId}`, seed);
            const anotherPublicId = encodePublicId(`${anotherInternalId}`, anotherSeed);

            const body: SharedList[] = [];

            const res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual.length).to.be.equal(1);

            expect(actual[0]).to.include({
                record_id: recordId,
                revision,
                public_id: publicId,
                status: 'closed'
            });
            expect(actual[0].timestamp).to.be.greaterThan(now);

            const storedList = await selectList(publicId);
            expect(storedList).to.deep.include({
                uid,
                id: internalId,
                seed,
                record_id: recordId,
                revision,
                attributes: {
                    title,
                    bookmarks_count: 0
                },
                status: 'closed',
                bookmarks: []
            });
            expect(storedList.timestamp.getTime()).to.be.greaterThan(now);

            // Should not change list of another author
            const anotherStoredList = await selectList(anotherPublicId);
            expect(anotherStoredList).to.be.deep.equal({
                uid: anotherUid,
                id: anotherInternalId,
                seed: anotherSeed,
                record_id: recordId,
                revision,
                timestamp: storedTimestamp,
                attributes: {
                    title,
                    icon: 'a',
                    description: 'b',
                    bookmarks_count: 1
                },
                status: 'shared',
                bookmarks: [
                    {
                        record_id: 'record-b-1-2',
                        title: `Stub bookmark #1-2`,
                        uri: `https://stub_1_2`
                    }
                ]
            });
        });

        it('should delete list (status = "deleted") and update it if the the same record_id received', async () => {
            const uid = '111';
            const internalId = random(1, 2000);
            const recordId = 'record-l-1';
            const revision = 2;
            const deletedRevision = 3;
            const createdRevision = 4;
            const now = Date.now();
            const title = 'Shared list';
            const icon = 'rubric:color';
            const description = 'Stub description';
            const status = 'shared';

            const {data: {rows: [{seed: listSeed}]}} = await dbClient.executeWriteQuery<StoredSeed>({
                text: `INSERT INTO lists
                    (uid, id, record_id, revision, timestamp, attributes, status, bookmarks) VALUES
                    ($1, $2, $3, $4, $5, $6, $7, $8)
                    RETURNING seed`,
                values: [
                    uid,
                    internalId,
                    recordId,
                    revision,
                    new Date(now - 2 * minute),
                    {title, icon: 'i', description: 'd', bookmarks_count: 1},
                    status,
                    JSON.stringify([
                        {
                            record_id: 'b-record',
                            title: 'b title',
                            uri: 'https://b_uri',
                            description: 'b-description'
                        }
                    ])
                ]
            });

            const deleteBody = [{
                record_id: recordId,
                revision: deletedRevision,
                title,
                icon,
                description,
                status: 'deleted',
                bookmarks: [
                    {
                        record_id: 'b-record',
                        title: 'b title',
                        uri: 'https://stub',
                        description: 'b-description'
                    }
                ]
            }];
            const deleteExpected: ListInfo[] = [];

            const deleteRes = await client.post(url, {
                headers: auth111Headers,
                json: deleteBody
            });

            expect(deleteRes.statusCode).to.be.equal(200);
            const deleteActual = JSON.parse(deleteRes.body);
            expect(deleteActual).to.be.deep.equal(deleteExpected);

            // Should save empty bookmarks
            const deletedList = await selectList(encodePublicId(`${internalId}`, listSeed));
            expect(deletedList.bookmarks).to.be.deep.equal([]);
            expect(deletedList.attributes).to.deep.equal({
                bookmarks_count: 0,
                title
            });

            const createBody = [{
                record_id: recordId,
                revision: createdRevision,
                title,
                icon: 'new i',
                status: 'shared',
                bookmarks: []
            }];

            const createRes = await client.post(url, {
                headers: auth111Headers,
                json: createBody
            });

            expect(createRes.statusCode).to.be.equal(200);
            const createActual = JSON.parse(createRes.body);
            expect(createActual.length).to.be.equal(1);

            expect(createActual[0]).to.include({record_id: recordId, revision: createdRevision, status});
            expect(createActual[0].public_id).not.to.be.undefined;

            const {id, seed} = decodePublicId(createActual[0].public_id)!;
            const {data: {rows: storedLists}} = await dbClient.executeReadQuery({
                text: `SELECT * FROM lists
                    WHERE uid = $1
                    ORDER BY timestamp`,
                values: [uid]
            });
            expect(storedLists.length).to.equal(1);
            expect(storedLists[0]).to.deep.include({
                uid,
                id: `${id}`,
                seed,
                record_id: recordId,
                revision: createdRevision,
                attributes: {
                    title,
                    icon: 'new i',
                    bookmarks_count: 0
                },
                status: 'shared',
                bookmarks: `[]`
            });
            expect((storedLists[0] as any).timestamp.getTime()).to.be.greaterThan(now);
        });

        it('should save list with few bookmarks', async () => {
            const uid = '111';
            const recordId = 'record-l-1';
            const revision = 3;
            const now = Date.now();
            const title = 'Shared list';
            const icon = 'rubric:color';
            const description = 'Stub description';
            const status = 'shared';

            const body = [{
                record_id: recordId,
                revision,
                title,
                icon,
                description,
                status: 'shared',
                bookmarks: [
                    {
                        record_id: 'record-b-1-1',
                        title: `Stub bookmark #1-1`,
                        uri: `https://stub_1_1`,
                        description: '' // Schema validation should allow empty description
                    },
                    {
                        record_id: 'record-b-1-2',
                        title: `Stub bookmark #1-2`,
                        uri: `https://stub_1_2`,
                        description: `Stub bookmark #1-2`
                    },
                    {
                        record_id: 'record-b-1-3',
                        title: `Stub bookmark #1-3`,
                        uri: `https://stub_1_3`
                    }
                ]
            }];

            const res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual.length).to.be.equal(1);

            expect(actual[0]).to.include({record_id: recordId, revision, status});
            expect(actual[0].public_id).not.to.be.undefined;

            const {id, seed} = decodePublicId(actual[0].public_id)!;
            const storedList = await selectList(actual[0].public_id);
            expect(storedList).to.deep.include({
                uid,
                id,
                seed,
                record_id: actual[0].record_id,
                revision,
                attributes: {
                    title,
                    icon,
                    description,
                    bookmarks_count: 3
                },
                status,
                bookmarks: body[0].bookmarks
            });
            expect(storedList.timestamp.getTime()).to.be.greaterThan(now);
        });

        it('should not update the lists\'s properties ' +
            'which were omitted in the query (title, icon, description)', async () => {
            const uid = '111';
            const internalId = random(1, 2000);
            const recordId = 'record-l-1';
            const revision = 3;
            const newRevision = 5;
            const now = Date.now();
            const title = 'Shared list';
            const icon = 'rubric:color';
            const description = 'Stub description';
            const status = 'shared';
            const bookmarks = [
                {
                    record_id: 'record-b-1-1',
                    title: `Stub bookmark #1-1`,
                    uri: `https://stub_1_1`,
                    description: '' // Schema validation should allow empty description
                },
                {
                    record_id: 'record-b-1-2',
                    title: `Stub bookmark #1-2`,
                    uri: `https://stub_1_2`,
                    description: `Stub bookmark #1-2`
                }
            ];

            const {data: {rows: [{seed: seed}]}} = await dbClient.executeWriteQuery<StoredSeed>({
                text: `INSERT INTO lists
                    (uid, id, record_id, revision, timestamp, attributes, status, bookmarks) VALUES
                    ($1, $2, $3, $4, $5, $6, $7, $8)
                    RETURNING seed`,
                values: [
                    uid,
                    internalId,
                    recordId,
                    revision,
                    new Date(now - minute),
                    {title, icon, description, bookmarks_count: bookmarks.length},
                    status,
                    JSON.stringify(bookmarks)
                ]
            });

            const publicId = encodePublicId(`${internalId}`, seed);

            const body = [{
                record_id: recordId,
                revision: newRevision,
                status: 'shared'
            }];
            const expected = {
                record_id: recordId,
                revision: newRevision,
                status: 'shared',
                public_id: publicId
            };

            const res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual.length).to.equal(1);
            expect(actual[0]).to.deep.include(expected);
            expect(actual[0].timestamp).to.be.greaterThan(now);

            const storedList = await selectList(publicId);
            expect(storedList).to.deep.include({
                uid,
                id: internalId,
                seed,
                record_id: recordId,
                revision: newRevision,
                attributes: {
                    title,
                    icon,
                    description,
                    bookmarks_count: bookmarks.length
                },
                status,
                bookmarks
            });
            expect(storedList.timestamp.getTime()).to.be.greaterThan(now);
        });

        it('should add new lists and update existed ones', async () => {
            const uid = '111';
            const now = Date.now();
            const firstInternalId = random(1, 2000);
            const secondInternalId = firstInternalId + 1;
            const icon = 'rubric:color';
            const description = 'Stub description';
            await dbClient.executeWriteQuery({
                text: `INSERT INTO lists
                    (uid, id, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                    ($1, $3, 'record-l-1', 3, $2, 'shared', '[]', $5),
                    ($1, $4, 'record-l-2', 2, $2, 'shared', '[]', $6)`,
                values: [
                    uid,
                    new Date(now - minute),
                    firstInternalId,
                    secondInternalId,
                    {title: 'First list'},
                    {title: 'Second list'}
                ]
            });

            const body = [
                {
                    record_id: 'record-l-1',
                    title: 'First list',
                    icon: 'new-icon',
                    description,
                    revision: 5,
                    status: 'shared',
                    bookmarks: []
                },
                {
                    record_id: 'record-l-2',
                    title: 'Second list: new title',
                    icon,
                    description: 'New description',
                    revision: 7,
                    status: 'shared',
                    bookmarks: []
                },
                {
                    record_id: 'record-l-3',
                    title: 'Third list',
                    icon: 'new-icon',
                    description,
                    revision: 3,
                    status: 'shared',
                    bookmarks: []
                },
                {
                    record_id: 'record-l-4',
                    title: 'Fourth list',
                    icon,
                    description,
                    revision: 4,
                    status: 'shared',
                    bookmarks: []
                }
            ];
            const expected = [
                {record_id: 'record-l-1', revision: 5, status: 'shared'},
                {record_id: 'record-l-2', revision: 7, status: 'shared'},
                {record_id: 'record-l-3', revision: 3, status: 'shared'},
                {record_id: 'record-l-4', revision: 4, status: 'shared'}
            ];

            const res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual.length).to.be.equal(expected.length);

            for (let i = 0; i < expected.length; i++) {
                const list = actual.find((list: ListResult) => list.record_id === expected[i].record_id);

                expect(list).to.include(expected[i]);
                expect(list.public_id).not.to.be.undefined;
                expect(list.timestamp).to.be.greaterThan(now);

                const storedList = await selectList(list.public_id);
                const decodedPublicId = decodePublicId(list.public_id)!;
                expect(storedList).to.deep.include({
                    uid,
                    id: decodedPublicId.id,
                    seed: decodedPublicId.seed,
                    record_id: body[i].record_id,
                    revision: body[i].revision,
                    attributes: {
                        title: body[i].title,
                        icon: body[i].icon,
                        description: body[i].description,
                        bookmarks_count: 0
                    },
                    status: 'shared',
                    bookmarks: []
                });
                expect(storedList.timestamp.getTime()).to.be.greaterThan(now);
            }
        });

        it('should edit many different bookmarks in many lists', async () => {
            const uid = '111';
            const now = Date.now();
            const firstListRevision = 1;
            const secondListRevision = 2;
            const icon = 'rubric:color';
            const description = 'Stub description';
            const firstInternalId = random(1, 2000);
            const secondInternalId = firstInternalId + 1;

            const {data: {rows: [{seed: firstSeed}, {seed: secondSeed}]}} =
                await dbClient.executeWriteQuery<StoredSeed>({
                    text: `INSERT INTO lists
                        (uid, id, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                        ($1, $3, 'record-l-1', $5, $2, 'shared', $7, $9),
                        ($1, $4, 'record-l-2', $6, $2, 'shared', $8, $10)
                        RETURNING seed`,
                    values: [
                        uid,
                        new Date(now - minute),
                        firstInternalId,
                        secondInternalId,
                        firstListRevision,
                        secondListRevision,
                        JSON.stringify([
                            {
                                record_id: 'record-b-1-1',
                                title: `Stub bookmark #1-1`,
                                uri: `https://stub_1_1`,
                                description: `Stub bookmark #1-1`
                            },
                            {
                                record_id: 'record-b-1-2',
                                title: `Stub bookmark #1-2`,
                                uri: `https://stub_1_2`,
                                description: `Stub bookmark #1-2`
                            }
                        ]),
                        JSON.stringify([
                            {
                                record_id: 'record-b-2-1',
                                title: `Stub bookmark #2-1`,
                                uri: `https://stub_2_1`,
                                description: `Stub bookmark #2-1`
                            },
                            {
                                record_id: 'record-b-2-2',
                                title: `Stub bookmark #2-2`,
                                uri: `https://stub_2_2`,
                                description: `Stub bookmark #2-2`
                            },
                            {
                                record_id: 'record-b-2-3',
                                title: `Stub bookmark #2-3`,
                                uri: `https://stub_2_3`,
                                description: `Stub bookmark #2-3`
                            }
                        ]),
                        {title: 'First list'},
                        {title: 'Second list'}
                    ]
                });

            const firstPublicId = encodePublicId(`${firstInternalId}`, firstSeed);
            const secondPublicId = encodePublicId(`${secondInternalId}`, secondSeed);

            const body = [
                {
                    record_id: 'record-l-1',
                    title: 'First list',
                    icon,
                    description,
                    revision: firstListRevision + 1,
                    status: 'shared',
                    bookmarks: [
                        {
                            record_id: 'record-b-1-1',
                            title: `Stub bookmark #1-1 - updated`,
                            uri: `https://stub_1_1_updated`,
                            description: `Stub bookmark #1-1 - updated`
                        },
                        {
                            record_id: 'record-b-1-2',
                            title: `Stub bookmark #1-2 - updated`,
                            uri: `https://stub_1_2_updated`,
                            description: `Stub bookmark #1-2 - updated`
                        }
                    ]
                },
                {
                    record_id: 'record-l-2',
                    title: 'Second list',
                    icon,
                    description,
                    revision: secondListRevision + 1,
                    status: 'shared',
                    bookmarks: [
                        {
                            record_id: 'record-b-2-1',
                            title: `Stub bookmark #2-1 - updated`,
                            uri: `https://stub_2_1_updated`,
                            description: `Stub bookmark #2-1 - updated`
                        },
                        {
                            record_id: 'record-b-2-2',
                            title: `Stub bookmark #2-2 - updated`,
                            uri: `https://stub_2_2_updated`,
                            description: `Stub bookmark #2-2 - updated`
                        },
                        {
                            record_id: 'record-b-2-3',
                            title: `Stub bookmark #2-3 - updated`,
                            uri: `https://stub_2_3_updated`,
                            description: `Stub bookmark #2-3 - updated`
                        }
                    ]
                }
            ];
            const expected = [
                {
                    public_id: firstPublicId,
                    record_id: 'record-l-1',
                    revision: firstListRevision + 1,
                    status: 'shared'
                },
                {
                    public_id: secondPublicId,
                    record_id: 'record-l-2',
                    revision: secondListRevision + 1,
                    status: 'shared'
                }
            ];

            const res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual.length).to.equal(2);
            for (let i = 0; i < expected.length; i++) {
                const list = actual.find((list: ListResult) => list.record_id === expected[i].record_id);

                expect(list).to.include(expected[i]);
                expect(list.timestamp).to.be.greaterThan(now);
            }

            const {data: {rows: storedInDb}} = await dbClient.executeReadQuery({
                text: `SELECT * FROM lists
                    WHERE uid = $1
                    ORDER BY record_id`,
                values: [uid]
            });
            expect(storedInDb.length).to.equal(2);
            expect(storedInDb[0]).to.deep.include({
                id: `${firstInternalId}`,
                seed: firstSeed,
                record_id: 'record-l-1',
                revision: firstListRevision + 1,
                status: 'shared',
                attributes: {
                    title: 'First list',
                    icon,
                    description,
                    bookmarks_count: 2
                },
                bookmarks: JSON.stringify([
                    {
                        record_id: 'record-b-1-1',
                        title: `Stub bookmark #1-1 - updated`,
                        uri: `https://stub_1_1_updated`,
                        description: `Stub bookmark #1-1 - updated`
                    },
                    {
                        record_id: 'record-b-1-2',
                        title: `Stub bookmark #1-2 - updated`,
                        uri: `https://stub_1_2_updated`,
                        description: `Stub bookmark #1-2 - updated`
                    }
                ])
            });
            expect((storedInDb[0] as any).timestamp.getTime()).to.be.greaterThan(now);
            expect(storedInDb[1]).to.deep.include({
                id: `${secondInternalId}`,
                seed: secondSeed,
                record_id: 'record-l-2',
                revision: secondListRevision + 1,
                status: 'shared',
                attributes: {
                    title: 'Second list',
                    icon,
                    description,
                    bookmarks_count: 3
                },
                bookmarks: JSON.stringify([
                    {
                        record_id: 'record-b-2-1',
                        title: `Stub bookmark #2-1 - updated`,
                        uri: `https://stub_2_1_updated`,
                        description: `Stub bookmark #2-1 - updated`
                    },
                    {
                        record_id: 'record-b-2-2',
                        title: `Stub bookmark #2-2 - updated`,
                        uri: `https://stub_2_2_updated`,
                        description: `Stub bookmark #2-2 - updated`
                    },
                    {
                        record_id: 'record-b-2-3',
                        title: `Stub bookmark #2-3 - updated`,
                        uri: `https://stub_2_3_updated`,
                        description: `Stub bookmark #2-3 - updated`
                    }
                ])
            });
            expect((storedInDb[1] as any).timestamp.getTime()).to.be.greaterThan(now);
        });

        it('should remove, insert and update bookmarks successfully', async () => {
            const uid = '111';
            const now = Date.now();
            const revision = 4;
            const internalId = random(1, 2000);
            const {data: {rows: [{seed: seed}]}} = await dbClient.executeWriteQuery<StoredSeed>({
                text: `INSERT INTO lists
                    (uid, id, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                    ($1, $2, 'record-l-1', $3, $4, 'shared', $5, $6)
                    RETURNING seed`,
                values: [
                    uid,
                    internalId,
                    revision,
                    new Date(now - minute),
                    JSON.stringify([
                        {
                            record_id: 'record-b-1-1',
                            title: `Stub bookmark #1-1`,
                            uri: `https://stub_1_1`,
                            description: `Stub bookmark #1-1`
                        },
                        {
                            record_id: 'record-b-1-2',
                            title: `Stub bookmark #1-2`,
                            uri: `https://stub_1_2`,
                            description: `Stub bookmark #1-2`
                        },
                        {
                            record_id: 'record-b-1-3',
                            title: `Stub bookmark #1-3`,
                            uri: `https://stub_1_3`,
                            description: `Stub bookmark #1-3`
                        }
                    ]),
                    {title: 'First list', bookmarks_count: 3}
                ]
            });
            const publicId = encodePublicId(`${internalId}`, seed);

            const body = [
                {
                    record_id: 'record-l-1',
                    title: 'First list',
                    icon: '',
                    description: '',
                    revision: revision + 1,
                    status: 'shared',
                    bookmarks: [
                        {
                            record_id: 'record-b-1-2',
                            title: `Stub bookmark #1-2`,
                            uri: `https://stub_1_2`,
                            description: `Stub bookmark #1-2`
                        },
                        {
                            record_id: 'record-b-1-3',
                            title: `Stub bookmark #1-3 - updated`,
                            uri: `https://stub_1_3_updated`,
                            description: `Stub bookmark #1-3 - updated`
                        },
                        {
                            record_id: 'record-b-1-4',
                            title: `Stub bookmark #1-4`,
                            uri: `https://stub_1_4`,
                            description: `Stub bookmark #1-4`
                        }
                    ]
                }
            ];
            const expected = {
                public_id: publicId,
                record_id: 'record-l-1',
                revision: revision + 1,
                status: 'shared'
            };

            const res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual.length).to.equal(1);
            expect(actual[0]).to.deep.include(expected);

            const {data: {rows: storedInDb}} = await dbClient.executeReadQuery({
                text: `SELECT * FROM lists
                    WHERE uid = $1`,
                values: [uid]
            });
            expect(storedInDb.length).to.equal(1);
            expect(storedInDb[0]).to.deep.include({
                id: `${internalId}`,
                seed,
                record_id: 'record-l-1',
                status: 'shared',
                revision: revision + 1,
                attributes: {
                    title: 'First list',
                    icon: '',
                    description: '',
                    bookmarks_count: 3
                },
                bookmarks: JSON.stringify([
                    {
                        record_id: 'record-b-1-2',
                        title: `Stub bookmark #1-2`,
                        uri: `https://stub_1_2`,
                        description: `Stub bookmark #1-2`
                    },
                    {
                        record_id: 'record-b-1-3',
                        title: `Stub bookmark #1-3 - updated`,
                        uri: `https://stub_1_3_updated`,
                        description: `Stub bookmark #1-3 - updated`
                    },
                    {
                        record_id: 'record-b-1-4',
                        title: `Stub bookmark #1-4`,
                        uri: `https://stub_1_4`,
                        description: `Stub bookmark #1-4`
                    }
                ])
            });
            expect((storedInDb[0] as any).timestamp.getTime()).to.be.greaterThan(now);
        });

        it('should remove, insert and update "shared" bookmarks independently', async () => {
            const uid = '111';
            const now = Date.now();
            const firstInternalId = random(1, 2000);
            const secondInternalId = firstInternalId + 1;
            const firstRevision = 1;
            const secondRevision = 2;
            const {data: {rows: [{seed: firstSeed}, {seed: secondSeed}]}} =
                await dbClient.executeWriteQuery<StoredSeed>({
                    text: `INSERT INTO lists
                        (uid, id, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                        ($1, $3, 'record-l-1', $5, $2, 'shared', $7, $8),
                        ($1, $4, 'record-l-2', $6, $2, 'shared', $7, $9)
                        RETURNING seed`,
                    values: [
                        uid,
                        new Date(now - minute),
                        firstInternalId,
                        secondInternalId,
                        firstRevision,
                        secondRevision,
                        JSON.stringify([
                            {
                                record_id: 'shared-bookmarks-record-id',
                                title: `Shared bookmark title`,
                                uri: `https://shared_bookmark`,
                                description: `Shared bookmark description`
                            }
                        ]),
                        {title: 'First list', bookmarks_count: 1},
                        {title: 'Second list', bookmarks_count: 1}
                    ]
                });

            const firstPublicId = encodePublicId(`${firstInternalId}`, firstSeed);
            const secondPublicId = encodePublicId(`${secondInternalId}`, secondSeed);

            const body = [
                {
                    record_id: 'record-l-1',
                    title: 'First list',
                    icon: '',
                    description: '',
                    revision: firstRevision + 1,
                    status: 'shared',
                    bookmarks: [
                        {
                            record_id: 'shared-bookmarks-record-id',
                            title: `Shared bookmark title - first list`,
                            uri: `https://shared_bookmark-first-list`,
                            description: `Shared bookmark description - first list`
                        }
                    ]
                },
                {
                    record_id: 'record-l-2',
                    title: 'Second list',
                    icon: '',
                    description: '',
                    revision: secondRevision + 1,
                    status: 'shared',
                    bookmarks: [
                        {
                            record_id: 'shared-bookmarks-record-id',
                            title: `Shared bookmark title - second list`,
                            uri: `https://shared_bookmark-second-list`,
                            description: `Shared bookmark description - second list`
                        }
                    ]
                }
            ];
            const expected = [
                {
                    public_id: firstPublicId,
                    record_id: 'record-l-1',
                    revision: firstRevision + 1,
                    status: 'shared'
                },
                {
                    public_id: secondPublicId,
                    record_id: 'record-l-2',
                    revision: secondRevision + 1,
                    status: 'shared'
                }
            ];

            const res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual.length).to.equal(2);

            for (let i = 0; i < expected.length; i++) {
                const list = actual.find((list: ListResult) => list.record_id === expected[i].record_id);

                expect(list).to.include(expected[i]);
                expect(list.timestamp).to.be.greaterThan(now);
            }

            const {data: {rows: storedInDb}} = await dbClient.executeReadQuery({
                text: `SELECT id, seed, record_id, revision, status, attributes, bookmarks
                    FROM lists
                    WHERE uid = $1`,
                values: [uid]
            });
            expectArraysDeepEqual(storedInDb, [
                {
                    id: `${firstInternalId}`,
                    seed: firstSeed,
                    record_id: 'record-l-1',
                    status: 'shared',
                    revision: firstRevision + 1,
                    attributes: {
                        title: 'First list',
                        icon: '',
                        description: '',
                        bookmarks_count: 1
                    },
                    bookmarks: JSON.stringify([
                        {
                            record_id: 'shared-bookmarks-record-id',
                            title: `Shared bookmark title - first list`,
                            uri: `https://shared_bookmark-first-list`,
                            description: `Shared bookmark description - first list`
                        }
                    ])
                },
                {
                    id: `${secondInternalId}`,
                    seed: secondSeed,
                    record_id: 'record-l-2',
                    status: 'shared',
                    revision: secondRevision + 1,
                    attributes: {
                        title: 'Second list',
                        icon: '',
                        description: '',
                        bookmarks_count: 1
                    },
                    bookmarks: JSON.stringify([
                        {
                            record_id: 'shared-bookmarks-record-id',
                            title: `Shared bookmark title - second list`,
                            uri: `https://shared_bookmark-second-list`,
                            description: `Shared bookmark description - second list`
                        }
                    ])
                }
            ]);
        });

        it('should not apply updates with the outdated revision', async () => {
            const uid = '111';
            const internalId = random(1, 2000);
            const recordId = 'record-l-1';
            const revision = 7;
            const timestamp = new Date('2021-09-13');
            const status = 'shared';
            const title = 'Shared list';
            const icon = 'rubric:color';
            const description = 'Stub description';

            const {data: {rows: [{seed: seed}]}} = await dbClient.executeWriteQuery<StoredSeed>({
                text: `INSERT INTO lists
                    (uid, id, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                    ($1, $2, $3, $4, $5, $6, '[]', $7)
                    RETURNING seed`,
                values: [
                    uid,
                    internalId,
                    recordId,
                    revision,
                    timestamp,
                    status,
                    {title, icon, description, bookmarks_count: 0}
                ]
            });
            const publicId = encodePublicId(`${internalId}`, seed);

            const body = [{
                record_id: recordId,
                title: 'New title',
                icon: 'new icon',
                description: 'new description',
                revision: revision - 1, // outdated revision
                status,
                bookmarks: [{
                    record_id: 'record-b-1-1',
                    title: `Stub bookmark #1-1`,
                    uri: `https://stub_1_1`,
                    description: `Stub bookmark #1-1`
                }]
            }];
            const expected = {
                error: 'outdated',
                record_id: recordId,
                public_id: publicId,
                revision,
                timestamp: timestamp.getTime(),
                status
            };

            const res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual[0]).to.be.deep.equal(expected);

            const storedList = await selectList(publicId);
            expect(storedList).to.be.deep.equal({
                id: internalId,
                seed,
                uid,
                record_id: recordId,
                revision,
                timestamp,
                attributes: {
                    title,
                    icon,
                    description,
                    bookmarks_count: 0
                },
                status,
                bookmarks: []
            });
        });

        it('should not apply updates with the equal revision', async () => {
            const uid = '111';
            const internalId = random(1, 2000);
            const recordId = 'record-l-1';
            const revision = 7;
            const timestamp = new Date('2021-09-13');
            const status = 'shared';
            const title = 'Shared list';
            const icon = 'rubric:color';
            const description = 'Stub description';

            const {data: {rows: [{seed: seed}]}} = await dbClient.executeWriteQuery<StoredSeed>({
                text: `INSERT INTO lists
                    (uid, id, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                    ($1, $2, $3, $4, $5, $6, '[]', $7)
                    RETURNING seed`,
                values: [
                    uid,
                    internalId,
                    recordId,
                    revision,
                    timestamp,
                    status,
                    {title, icon, description, bookmarks_count: 0}
                ]
            });
            const publicId = encodePublicId(`${internalId}`, seed);

            const body = [{
                record_id: recordId,
                title: 'New title',
                icon: 'new icon',
                description: 'new description',
                revision, // the same revision
                status,
                bookmarks: [{
                    record_id: 'record-b-1-1',
                    title: `Stub bookmark #1-1`,
                    uri: `https://stub_1_1`,
                    description: `Stub bookmark #1-1`
                }]
            }];
            const expected = {
                record_id: recordId,
                public_id: publicId,
                revision,
                timestamp: timestamp.getTime(),
                status
            };

            const res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual[0]).to.be.deep.equal(expected);

            const storedList = await selectList(publicId);
            expect(storedList).to.be.deep.equal({
                id: internalId,
                seed,
                uid,
                record_id: recordId,
                revision,
                timestamp,
                attributes: {
                    title,
                    icon,
                    description,
                    bookmarks_count: 0
                },
                status,
                bookmarks: []
            });
        });

        it('should successfully save the bookmark comment', async () => {
            const recordId = 'record-l-1';
            const revision = 1;
            const status = 'shared';
            const body = [
                {
                    record_id: recordId,
                    revision,
                    status,
                    bookmarks: [
                        {
                            record_id: 'record-id-1',
                            title: `Shared bookmark title 1`,
                            uri: `https://shared_bookmark_1`,
                            description: `Description 1`
                        },
                        {
                            record_id: 'record-id-2',
                            title: `Shared bookmark title 2`,
                            uri: `https://shared_bookmark_2`,
                            description: `Description 2`,
                            comment: 'Some comment'
                        }
                    ]
                }
            ];

            const res = await client.post(url, {
                headers: auth111Headers,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);

            expect(actual.length).to.be.equal(1);
            expect(actual[0]).to.include({record_id: recordId, revision, status});

            const storedList = await selectList(actual[0].public_id);
            expect(storedList.bookmarks).to.deep.equal([
                {
                    record_id: 'record-id-1',
                    title: `Shared bookmark title 1`,
                    uri: `https://shared_bookmark_1`,
                    description: `Description 1`
                },
                {
                    record_id: 'record-id-2',
                    title: `Shared bookmark title 2`,
                    uri: `https://shared_bookmark_2`,
                    description: `Description 2`,
                    comment: 'Some comment'
                }
            ]);
        });
    });

    describe('for many users', () => {
        const uid111AuthHeaders = {
            'X-Ya-Service-Ticket': SERVICE_TICKET_MAPS,
            'X-Ya-User-Ticket': USER_111_TICKET
        };
        const uid999AuthHeaders = {
            'X-Ya-Service-Ticket': SERVICE_TICKET_MAPS,
            'X-Ya-User-Ticket': USER_999_TICKET
        };

        beforeEach(clearDb);

        it('should create lists of different users independently', async () => {
            const users = [{uid: '111', authHeaders: uid111AuthHeaders}, {uid: '999', authHeaders: uid999AuthHeaders}];
            const now = Date.now();
            for (const {uid, authHeaders} of users) {
                const recordId = 'record-l-1';
                const revision = 3;
                const title = 'Shared list';
                const status = 'shared';
                const icon = 'rubric:color';
                const description = 'Stub description';

                const body = [{
                    record_id: recordId,
                    revision,
                    title,
                    icon,
                    description,
                    status: 'shared',
                    bookmarks: [
                        {
                            record_id: 'record-b-1-1',
                            title: `Stub bookmark #1-1`,
                            uri: `https://stub_1_1`,
                            description: 'Stub bookmark #1-1'
                        },
                        {
                            record_id: 'record-b-1-2',
                            title: `Stub bookmark #1-2`,
                            uri: `https://stub_1_2`,
                            description: `Stub bookmark #1-2`
                        }
                    ]
                }];

                const res = await client.post(url, {
                    headers: authHeaders,
                    json: body
                });

                expect(res.statusCode).to.be.equal(200);
                const actual = JSON.parse(res.body);
                expect(actual.length).to.be.equal(1);

                expect(actual[0]).to.include({record_id: recordId, revision, status});
                expect(actual[0].public_id).not.to.be.undefined;

                const storedList = await selectList(actual[0].public_id);
                const {id, seed} = decodePublicId(actual[0].public_id)!;
                expect(storedList).to.deep.include({
                    uid,
                    id,
                    seed,
                    record_id: recordId,
                    revision,
                    attributes: {
                        title,
                        icon,
                        description,
                        bookmarks_count: 2
                    },
                    status,
                    bookmarks: [
                        {
                            record_id: 'record-b-1-1',
                            title: `Stub bookmark #1-1`,
                            uri: `https://stub_1_1`,
                            description: 'Stub bookmark #1-1'
                        },
                        {
                            record_id: 'record-b-1-2',
                            title: `Stub bookmark #1-2`,
                            uri: `https://stub_1_2`,
                            description: `Stub bookmark #1-2`
                        }
                    ]
                });
                expect(storedList.timestamp.getTime()).to.be.greaterThan(now);
            }
        });

        it('should update lists of different users independently', async () => {
            const users = [{uid: '111', authHeaders: uid111AuthHeaders}, {uid: '999', authHeaders: uid999AuthHeaders}];
            const now = Date.now();
            for (const {uid, authHeaders} of users) {
                const internalId = Number(uid) * 1000 + random(1, 1000);
                const revision = 3;
                const newRevision = 6;
                const recordId = 'record-l-1';
                const status = 'shared';
                const title = 'Shared list';
                const icon = 'rubric:color';
                const description = 'Stub description';

                const {data: {rows: [{seed: seed}]}} = await dbClient.executeWriteQuery<StoredSeed>({
                    text: `INSERT INTO lists
                        (uid, id, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                        ($1, $2, $3, $4, $5, $6, '[]', $7)
                        RETURNING seed`,
                    values: [
                        uid,
                        internalId,
                        recordId,
                        revision,
                        new Date('2021-09-15'),
                        status,
                        {title, icon, description, bookmarks_count: 0}
                    ]
                });
                const publicId = encodePublicId(`${internalId}`, seed);

                const newTitle = 'New title';
                const newIcon = 'new-icon';
                const newDescription = 'New description';
                const body = [{
                    record_id: recordId,
                    title: newTitle,
                    icon: newIcon,
                    description: newDescription,
                    revision: newRevision,
                    status,
                    bookmarks: [
                        {
                            record_id: `record-b`,
                            title: `Some bookmark`,
                            uri: `https://stub`,
                            description: 'Summer'
                        }
                    ]
                }];
                const expected = {
                    public_id: publicId,
                    record_id: recordId,
                    revision: newRevision,
                    status
                };

                const res = await client.post(url, {
                    headers: authHeaders,
                    json: body
                });

                expect(res.statusCode).to.be.equal(200);
                const actual = JSON.parse(res.body);
                expect(actual.length).to.equal(1);
                expect(actual[0]).to.deep.include(expected);

                const storedList = await selectList(publicId);
                expect(storedList).to.deep.include({
                    uid,
                    id: internalId,
                    seed,
                    record_id: recordId,
                    revision: newRevision,
                    attributes: {
                        title: newTitle,
                        icon: newIcon,
                        description: newDescription,
                        bookmarks_count: 1
                    },
                    status,
                    bookmarks: [
                        {
                            record_id: `record-b`,
                            title: `Some bookmark`,
                            uri: `https://stub`,
                            description: 'Summer'
                        }
                    ]
                });
                expect(storedList.timestamp.getTime()).to.be.greaterThan(now);
            }
        });
    });
});
