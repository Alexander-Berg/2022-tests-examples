import {config} from 'app/config';
import {getAvatarUrl} from 'app/v1/helpers/avatar';
import {expect} from 'chai';
import * as fs from 'fs';
import {URL} from 'url';
import {app} from 'app/app';
import {client, TestServer} from 'tests/integration/test-server';
import {TvmDaemon} from 'tests/integration/tvm-daemon';
import {clearDb} from 'tests/helpers/clear-db';
import {sleep} from 'tests/helpers/sleep';
import {expectArraysDeepEqual} from 'tests/helpers/validation';
import {dbClient} from 'app/lib/db-client';
import nock from 'nock';
import {intHostsLoader} from 'app/lib/hosts';
import {Hosts} from '@yandex-int/maps-host-configs';
import {encodePublicId, PUBLIC_ID_PATTERN} from 'app/v1/helpers/public-id';
import {random} from 'tests/helpers/generation';
import {StoredSeed} from 'tests/helpers/db';

// TODO: Implement ticket auto-generation
// TODO: Add tests to check author's names
const USER_111_TICKET = fs.readFileSync('src/tests/fixtures/tvm-tickets/user/uid-111').toString().trim();
const SERVICE_TICKET_MAPS = fs.readFileSync('src/tests/fixtures/tvm-tickets/service/maps').toString().trim();
const UNKNOWN_SERVICE_TICKET = fs.readFileSync('src/tests/fixtures/tvm-tickets/service/unknown').toString().trim();

const uid111AuthHeaders = {
    'X-Ya-Service-Ticket': SERVICE_TICKET_MAPS,
    'X-Ya-User-Ticket': USER_111_TICKET
};

describe('POST /v1/get_lists', () => {
    let tvmDaemon: TvmDaemon;
    let server: TestServer;
    let url: URL;
    let intHosts: Hosts;

    before(async () => {
        server = await TestServer.start(app);
        tvmDaemon = await TvmDaemon.start();
        url = new URL(`${server.url}/v1/get_lists`);
        intHosts = await intHostsLoader.get();
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
                message: 'Error validating body: \"only\" is required'
            };

            const res = await client.post(url, {
                headers: uid111AuthHeaders,
                json: body
            });

            expect(res.statusCode).to.be.equal(400);
            const actual = JSON.parse(res.body);
            expect(actual).to.deep.equal(expected);
        });

        it('should fail if "only" property have incorrect value (custom string)', async () => {
            const body = {only: 'custom string'};
            const expected = {
                statusCode: 400,
                error: 'Bad Request',
                message: 'Error validating body: \"only\" must be one of [my_shared, my_subscriptions, array]'
            };

            const res = await client.post(url, {
                headers: uid111AuthHeaders,
                json: body
            });

            expect(res.statusCode).to.be.equal(400);
            const actual = JSON.parse(res.body);
            expect(actual).to.deep.equal(expected);
        });

        it('should fail if "only" property have incorrect value (object)', async () => {
            const body = {only: {}};
            const expected = {
                statusCode: 400,
                error: 'Bad Request',
                message: 'Error validating body: \"only\" must be one of [my_shared, my_subscriptions, array]'
            };

            const res = await client.post(url, {
                headers: uid111AuthHeaders,
                json: body
            });

            expect(res.statusCode).to.be.equal(400);
            const actual = JSON.parse(res.body);
            expect(actual).to.deep.equal(expected);
        });

        it('should fail if "only" property have incorrect value (incorrect public_id format)', async () => {
            const incorrectPublicIds = [
                'small',
                'medium_id',
                'long_long_long_id',
                'incorrect.'
            ];

            for (const publicId of incorrectPublicIds) {
                const body = {only: [publicId]};
                const expected = {
                    statusCode: 400,
                    error: 'Bad Request',
                    message: `Error validating body: \"only[0]\" with value \"${publicId}\" ` +
                        `fails to match the required pattern: ${PUBLIC_ID_PATTERN}`
                };

                const res = await client.post(url, {
                    headers: uid111AuthHeaders,
                    json: body
                });

                expect(res.statusCode).to.be.equal(400);
                const actual = JSON.parse(res.body);
                expect(actual).to.deep.equal(expected);
            }
        });

        it('should throw 401 when service ticket is missing', async () => {
            const res = await client.post(url, {
                headers: {
                    'X-Ya-User-Ticket': USER_111_TICKET
                },
                json: {only: 'my_shared'}
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
                json: {only: 'my_shared'}
            });

            expect(res.statusCode).to.equal(401);
            expect(JSON.parse(res.body).message).to.equal('Invalid TVM service ticket');
        });

        it('should throw 401 when user ticket is invalid', async () => {
            const res = await client.post(url, {
                headers: {
                    'X-Ya-Service-Ticket': SERVICE_TICKET_MAPS,
                    'X-Ya-User-Ticket': 'shto-to-ne-to'
                },
                json: {only: 'my_shared'}
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
                json: {only: 'my_shared'}
            });

            expect(res.statusCode).to.equal(403);
            expect(JSON.parse(res.body).message).to.equal('TVM service does not have enough access rights');
        });
    });

    describe('with "only" = "my_shared"', () => {
        beforeEach(clearDb);

        it('should return empty array if user didn\'t shared anything', async () => {
            const uid = '555';
            const timestamp = new Date();
            await dbClient.executeWriteQuery({
                text: `INSERT INTO lists
                    (uid, record_id, revision, timestamp, status, bookmarks) VALUES
                    ($1, 'record-l-1', 1, $2, 'shared', '[]'),
                    ($1, 'record-l-2', 2, $2, 'shared', '[]')`,
                values: [uid, timestamp]
            });

            const body = {only: 'my_shared'};
            const expected: any[] = [];

            const res = await client.post(url, {
                headers: uid111AuthHeaders,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual).to.deep.equal(expected);
        });

        it('should return the lists shared by user', async () => {
            const uid = '111';
            const timestamp = new Date();

            const firstInternalId = random(1, 2000);
            const secondInternalId = firstInternalId + 1;
            const thirdInternalId = secondInternalId + 1;
            const fourthInternalId = thirdInternalId + 1;

            const {data: {rows}} = await dbClient.executeWriteQuery<StoredSeed>({
                text: `INSERT INTO lists
                    (uid, id, record_id, revision, timestamp, status, bookmarks) VALUES
                    ($1, $3, 'record-l-1', 1, $2, 'shared', '[]'),
                    ($1, $4, 'record-l-2', 2, $2, 'shared', '[]'),
                    ('666', $5, 'record-l-3', 3, $2, 'shared', '[]'),
                    ($1, $6, 'record-l-4', 4, $2, 'shared', '[]')
                    RETURNING seed`,
                values: [
                    uid,
                    timestamp,
                    firstInternalId,
                    secondInternalId,
                    thirdInternalId,
                    fourthInternalId
                ]
            });

            const firstPublicId = encodePublicId(`${firstInternalId}`, rows[0].seed);
            const secondPublicId = encodePublicId(`${secondInternalId}`, rows[1].seed);
            const fourthPublicId = encodePublicId(`${fourthInternalId}`, rows[3].seed);

            const body = {only: 'my_shared'};
            const expected = [
                {
                    public_id: firstPublicId,
                    record_id: 'record-l-1',
                    revision: 1,
                    timestamp: timestamp.getTime(),
                    status: 'shared'
                },
                {
                    public_id: secondPublicId,
                    record_id: 'record-l-2',
                    revision: 2,
                    timestamp: timestamp.getTime(),
                    status: 'shared'
                },
                {
                    public_id: fourthPublicId,
                    record_id: 'record-l-4',
                    revision: 4,
                    timestamp: timestamp.getTime(),
                    status: 'shared'
                }
            ];

            const res = await client.post(url, {
                headers: uid111AuthHeaders,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expectArraysDeepEqual(actual, expected);
        });

        it('should return closed but not deleted lists', async () => {
            const uid = '111';
            const timestamp = new Date();

            const firstInternalId = random(1, 2000);
            const secondInternalId = firstInternalId + 1;
            const thirdInternalId = secondInternalId + 1;

            const {data: {rows: [{seed: firstSeed}, {seed: secondSeed}]}} =
                await dbClient.executeWriteQuery<StoredSeed>({
                    text: `INSERT INTO lists
                        (uid, id, record_id, revision, timestamp, status, bookmarks) VALUES
                        ($1, $3, 'record-l-1', 1, $2, 'shared', '[]'),
                        ($1, $4, 'record-l-2', 2, $2, 'closed', '[]'),
                        ($1, $5, 'record-l-3', 3, $2, 'deleted', '[]')
                        RETURNING seed`,
                    values: [
                        uid,
                        timestamp,
                        firstInternalId,
                        secondInternalId,
                        thirdInternalId
                    ]
                });

            const firstPublicId = encodePublicId(`${firstInternalId}`, firstSeed);
            const secondPublicId = encodePublicId(`${secondInternalId}`, secondSeed);

            const body = {only: 'my_shared'};
            const expected = [
                {
                    public_id: firstPublicId,
                    record_id: 'record-l-1',
                    revision: 1,
                    timestamp: timestamp.getTime(),
                    status: 'shared'
                },
                {
                    public_id: secondPublicId,
                    record_id: 'record-l-2',
                    revision: 2,
                    timestamp: timestamp.getTime(),
                    status: 'closed'
                }
            ];

            const res = await client.post(url, {
                headers: uid111AuthHeaders,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expectArraysDeepEqual(actual, expected);
        });

        it('shouldn\'t return the lists user subscribed to', async () => {
            const authorUid = '999';
            const subscriberUid = '111';
            const timestamp = new Date();

            const firstInternalId = random(1, 2000);
            const secondInternalId = firstInternalId + 1;

            await dbClient.executeWriteCallback(async (client) => {
                await client.query(
                    `INSERT INTO lists
                    (uid, id, record_id, revision, timestamp, status, bookmarks) VALUES
                    ($1, $3, 'record-l-1', 1, $2, 'shared', '[]'),
                    ($1, $4, 'record-l-2', 2, $2, 'shared', '[]')`,
                    [authorUid, timestamp, firstInternalId, secondInternalId]
                );
                await client.query(
                    `INSERT INTO subscriptions
                    (list_id, uid) VALUES
                    ((SELECT id FROM lists WHERE id = $2), $1),
                    ((SELECT id FROM lists WHERE id = $3), $1)`,
                    [subscriberUid, firstInternalId, secondInternalId]
                );
            });

            const body = {only: 'my_shared'};
            const expected: any[] = [];

            const res = await client.post(url, {
                headers: uid111AuthHeaders,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual).to.deep.equal(expected);
        });

        it('should return `is_banned` field when user was banned', async () => {
            const uid = '111';
            const timestamp = new Date();

            const firstInternalId = random(1, 2000);
            const secondInternalId = firstInternalId + 1;

            const {data: {rows: [{seed: firstSeed}, {seed: secondSeed}]}} =
                await dbClient.executeWriteQuery<StoredSeed>({
                    text: `INSERT INTO lists
                        (uid, id, record_id, revision, timestamp, status, bookmarks) VALUES
                        ($1, $3, 'record-l-1', 1, $2, 'closed', '[]'),
                        ($1, $4, 'record-l-2', 2, $2, 'closed', '[]')
                        RETURNING seed`,
                    values: [
                        uid,
                        timestamp,
                        firstInternalId,
                        secondInternalId
                    ]
                });

            await dbClient.executeWriteQuery({
                text: 'INSERT INTO banned_violators (uid, status, reviewed) VALUES ($1, $2, $3)',
                values: [uid, 'banned', true]
            });

            // Wait a bit to make sure the ban records were updated in cache
            await sleep(config['app.banCacheRenewTimeoutMs']);

            const firstPublicId = encodePublicId(`${firstInternalId}`, firstSeed);
            const secondPublicId = encodePublicId(`${secondInternalId}`, secondSeed);

            const body = {only: 'my_shared'};
            const expected = [
                {
                    public_id: firstPublicId,
                    record_id: 'record-l-1',
                    revision: 1,
                    timestamp: timestamp.getTime(),
                    status: 'closed',
                    is_banned: true
                },
                {
                    public_id: secondPublicId,
                    record_id: 'record-l-2',
                    revision: 2,
                    timestamp: timestamp.getTime(),
                    status: 'closed',
                    is_banned: true
                }
            ];

            const res = await client.post(url, {
                headers: uid111AuthHeaders,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expectArraysDeepEqual(actual, expected);
        });

        it('should not return `is_banned` field when ban status is whitelisted', async () => {
            const uid = '111';
            const timestamp = new Date();

            const firstInternalId = random(1, 2000);
            const secondInternalId = firstInternalId + 1;

            const {data: {rows: [{seed: firstSeed}, {seed: secondSeed}]}} =
                await dbClient.executeWriteQuery<StoredSeed>({
                    text: `INSERT INTO lists
                        (uid, id, record_id, revision, timestamp, status, bookmarks) VALUES
                        ($1, $3, 'record-l-1', 1, $2, 'closed', '[]'),
                        ($1, $4, 'record-l-2', 2, $2, 'closed', '[]')
                        RETURNING seed`,
                    values: [
                        uid,
                        timestamp,
                        firstInternalId,
                        secondInternalId
                    ]
                });

            await dbClient.executeWriteQuery({
                text: 'INSERT INTO banned_violators (uid, status, reviewed) VALUES ($1, $2, $3)',
                values: [uid, 'whitelisted', true]
            });

            // Wait a bit to make sure the ban records were updated in cache
            await sleep(config['app.banCacheRenewTimeoutMs']);

            const firstPublicId = encodePublicId(`${firstInternalId}`, firstSeed);
            const secondPublicId = encodePublicId(`${secondInternalId}`, secondSeed);

            const body = {only: 'my_shared'};
            const expected = [
                {
                    public_id: firstPublicId,
                    record_id: 'record-l-1',
                    revision: 1,
                    timestamp: timestamp.getTime(),
                    status: 'closed'
                },
                {
                    public_id: secondPublicId,
                    record_id: 'record-l-2',
                    revision: 2,
                    timestamp: timestamp.getTime(),
                    status: 'closed'
                }
            ];

            const res = await client.post(url, {
                headers: uid111AuthHeaders,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expectArraysDeepEqual(actual, expected);
        });
    });

    describe('with "only" = "my_subscriptions"', () => {
        before(() => {
            nock.disableNetConnect();
            nock.enableNetConnect(/(127.0.0.1|localhost)/);
        });

        after(() => {
            nock.enableNetConnect();
        });

        beforeEach(clearDb);

        afterEach(nock.cleanAll);

        it('should return empty array if user didn\'t subscribed to anything', async () => {
            const authorUid = '555';
            const subscriberUid = '999';
            const timestamp = new Date();

            const firstInternalId = random(1, 2000);
            const secondInternalId = firstInternalId + 1;

            await dbClient.executeWriteCallback(async (client) => {
                await client.query(
                    `INSERT INTO lists
                    (uid, id, record_id, revision, timestamp, status, bookmarks) VALUES
                    ($1, $3, 'record-l-1', 1, $2, 'shared', '[]'),
                    ($1, $4, 'record-l-2', 2, $2, 'shared', '[]')`,
                    [authorUid, timestamp, firstInternalId, secondInternalId]
                );
                await client.query(
                    `INSERT INTO subscriptions
                    (list_id, uid) VALUES
                    ((SELECT id FROM lists WHERE id = $2), $1),
                    ((SELECT id FROM lists WHERE id = $3), $1)`,
                    [subscriberUid, firstInternalId, secondInternalId]
                );
            });

            const body = {only: 'my_subscriptions'};
            const expected: any[] = [];

            const res = await client.post(url, {
                headers: uid111AuthHeaders,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual).to.deep.equal(expected);
        });

        it('should return the lists user subscribed to (along with all the bookmarks)', async () => {
            const authorUid = '999';
            const subscriberUid = '111';
            const authorName = 'Vasya Pupkin';
            const avatarId = '0/0-0';
            const timestamp = new Date();
            const icon = 'rubric:color';
            const description = 'Stub description';

            const firstInternalId = random(1, 2000);
            const secondInternalId = firstInternalId + 1;
            const thirdInternalId = secondInternalId + 1;

            const {data: [firstSeed, secondSeed]} = await dbClient.executeWriteCallback(async (client) => {
                const {rows: [{seed: firstSeed}, {seed: secondSeed}]} = await client.query(
                    `INSERT INTO lists
                    (uid, id, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                    ($1, $3, 'record-l-1', 1, $2, 'shared', $6, $9),
                    ($1, $4, 'record-l-2', 2, $2, 'shared', $7, $10),
                    ($1, $5, 'record-l-3', 3, $2, 'shared', $8, $11)
                    RETURNING seed`,
                    [
                        authorUid,
                        timestamp,
                        firstInternalId,
                        secondInternalId,
                        thirdInternalId,
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
                                uri: `https://stub_1_2`
                            }
                        ]),
                        JSON.stringify([
                            {
                                record_id: 'record-b-2-1',
                                title: `Stub bookmark #2-1`,
                                uri: `https://stub_2_1`,
                                description: `Stub bookmark #2-1`
                            }
                        ]),
                        JSON.stringify([
                            {
                                record_id: 'record-b-3-1',
                                title: `Stub bookmark #3-1`,
                                uri: `https://stub_3_1`,
                                description: `Stub bookmark #3-1`
                            },
                            {
                                record_id: 'record-b-3-2',
                                title: `Stub bookmark #3-2`,
                                uri: `https://stub_3_2`,
                                description: `Stub bookmark #3-2`
                            },
                            {
                                record_id: 'record-b-3-3',
                                title: `Stub bookmark #3-3`,
                                uri: `https://stub_3_3`,
                                description: `Stub bookmark #3-3`
                            }
                        ]),
                        {title: 'First list', description},
                        {title: 'Second list', icon},
                        {title: 'Third list', icon, description}
                    ]
                );
                await client.query(
                    `INSERT INTO subscriptions
                    (list_id, uid) VALUES
                    ((SELECT id FROM lists WHERE id = $2), $1),
                    ((SELECT id FROM lists WHERE id = $3), $1)`,
                    [subscriberUid, firstInternalId, secondInternalId]
                );

                return [firstSeed, secondSeed];
            });

            const firstPublicId = encodePublicId(`${firstInternalId}`, firstSeed);
            const secondPublicId = encodePublicId(`${secondInternalId}`, secondSeed);

            const avatarUrl = getAvatarUrl(avatarId);

            const body = {only: 'my_subscriptions'};
            const expected = [
                {
                    public_id: firstPublicId,
                    record_id: 'record-l-1',
                    revision: 1,
                    timestamp: timestamp.getTime(),
                    title: `First list`,
                    description,
                    author: authorName,
                    avatar_url: avatarUrl,
                    status: 'shared',
                    meta: {
                        is_current_user_author: false,
                        is_current_user_subscribed: true
                    },
                    bookmarks: [
                        {
                            record_id: 'record-b-1-1',
                            title: `Stub bookmark #1-1`,
                            uri: `https://stub_1_1`,
                            description: `Stub bookmark #1-1`
                        },
                        {
                            record_id: 'record-b-1-2',
                            title: `Stub bookmark #1-2`,
                            uri: `https://stub_1_2`
                        }
                    ]
                },
                {
                    public_id: secondPublicId,
                    record_id: 'record-l-2',
                    revision: 2,
                    timestamp: timestamp.getTime(),
                    title: `Second list`,
                    icon,
                    author: authorName,
                    avatar_url: avatarUrl,
                    status: 'shared',
                    meta: {
                        is_current_user_author: false,
                        is_current_user_subscribed: true
                    },
                    bookmarks: [
                        {
                            record_id: 'record-b-2-1',
                            title: `Stub bookmark #2-1`,
                            uri: `https://stub_2_1`,
                            description: `Stub bookmark #2-1`
                        }
                    ]
                }
            ];

            const nockBlackbox = nock(intHosts.blackbox)
                .get('/')
                .query(true)
                .times(1)
                .reply(200, {
                    users: [
                        {
                            display_name: {
                                public_name: 'Vasya Pupkin',
                                avatar: {default: '0/0-0'}
                            },
                            uid: {
                                value: authorUid
                            }
                        }
                    ]
                });

            const res = await client.post(url, {
                headers: uid111AuthHeaders,
                json: body
            });

            expect(nockBlackbox.isDone()).to.be.true;
            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expectArraysDeepEqual(actual, expected);
        });

        it('should return closed or deleted lists (and hide author\'s data)', async () => {
            const authorUid = '999';
            const subscriberUid = '111';
            const authorName = 'Vasya Pupkin';
            const avatarId = '0/0-0';
            const timestamp = new Date();
            const icon = 'rubric:color';
            const description = 'Stub description';

            const firstInternalId = random(1, 2000);
            const secondInternalId = firstInternalId + 1;
            const thirdInternalId = secondInternalId + 1;

            const {data: [firstSeed, secondSeed, thirdSeed]} =
                await dbClient.executeWriteCallback(async (client) => {
                    const {rows: [{seed: firstSeed}, {seed: secondSeed}, {seed: thirdSeed}]} =
                        await client.query(
                            `INSERT INTO lists
                            (uid, id, record_id, revision, timestamp, status, bookmarks, attributes)
                            VALUES
                            ($1, $3, 'record-l-1', 1, $2, 'shared', '[]', $6),
                            ($1, $4, 'record-l-2', 2, $2, 'closed', '[]', $7),
                            ($1, $5, 'record-l-3', 3, $2, 'deleted', '[]', $8)
                            RETURNING seed`,
                            [
                                authorUid,
                                timestamp,
                                firstInternalId,
                                secondInternalId,
                                thirdInternalId,
                                {title: 'Shared list', icon, description},
                                {title: 'Closed list'},
                                {title: 'Deleted list'}
                            ]
                        );
                    await client.query(
                        `INSERT INTO subscriptions
                        (list_id, uid) VALUES
                        ((SELECT id FROM lists WHERE id = $2), $1),
                        ((SELECT id FROM lists WHERE id = $3), $1),
                        ((SELECT id FROM lists WHERE id = $4), $1)`,
                        [
                            subscriberUid,
                            firstInternalId,
                            secondInternalId,
                            thirdInternalId
                        ]
                    );

                    return [firstSeed, secondSeed, thirdSeed];
                });

            const firstPublicId = encodePublicId(`${firstInternalId}`, firstSeed);
            const secondPublicId = encodePublicId(`${secondInternalId}`, secondSeed);
            const thirdPublicId = encodePublicId(`${thirdInternalId}`, thirdSeed);

            const avatarUrl = getAvatarUrl(avatarId);

            const body = {only: 'my_subscriptions'};
            const expected = [
                {
                    public_id: firstPublicId,
                    record_id: 'record-l-1',
                    revision: 1,
                    timestamp: timestamp.getTime(),
                    title: `Shared list`,
                    icon,
                    description,
                    author: authorName,
                    avatar_url: avatarUrl,
                    status: 'shared',
                    meta: {
                        is_current_user_author: false,
                        is_current_user_subscribed: true
                    },
                    bookmarks: []
                },
                {
                    public_id: secondPublicId,
                    record_id: 'record-l-2',
                    revision: 2,
                    timestamp: timestamp.getTime(),
                    title: `Closed list`,
                    status: 'closed',
                    meta: {
                        is_current_user_author: false,
                        is_current_user_subscribed: true
                    },
                    bookmarks: []
                },
                {
                    public_id: thirdPublicId,
                    record_id: 'record-l-3',
                    revision: 3,
                    timestamp: timestamp.getTime(),
                    title: `Deleted list`,
                    status: 'deleted',
                    meta: {
                        is_current_user_author: false,
                        is_current_user_subscribed: true
                    },
                    bookmarks: []
                }
            ];

            const nockBlackbox = nock(intHosts.blackbox)
                .get('/')
                .query(true)
                .times(1)
                .reply(200, {
                    users: [
                        {
                            display_name: {
                                public_name: authorName,
                                avatar: {default: avatarId}
                            },
                            uid: {
                                value: authorUid
                            }
                        }
                    ]
                });

            const res = await client.post(url, {
                headers: uid111AuthHeaders,
                json: body
            });

            expect(nockBlackbox.isDone()).to.be.true;
            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expectArraysDeepEqual(actual, expected);
        });

        it('shouldn\'t return the lists user shared', async () => {
            const uid = '111';
            const timestamp = new Date();

            const firstInternalId = random(1, 2000);
            const secondInternalId = firstInternalId + 1;

            await dbClient.executeWriteQuery({
                text: `INSERT INTO lists
                    (uid, id, record_id, revision, timestamp, status, bookmarks) VALUES
                    ($1, $3, 'record-l-1', 1, $2, 'shared', '[]'),
                    ($1, $4, 'record-l-2', 2, $2, 'shared', '[]')`,
                values: [uid, timestamp, firstInternalId, secondInternalId]
            });

            const body = {only: 'my_subscriptions'};
            const expected: any[] = [];

            const res = await client.post(url, {
                headers: uid111AuthHeaders,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual).to.deep.equal(expected);
        });

        // MAPSHTTPAPI-2416
        it('should return comment in bookmark if it exists', async () => {
            const authorUid = '999';
            const subscriberUid = '111';
            const authorName = 'Vasya Pupkin';
            const avatarId = '0/0-0';
            const timestamp = new Date();
            const listTitle = 'List';

            const internalId = random(1, 2000);

            const {data: seed} = await dbClient.executeWriteCallback(async (client) => {
                const {rows: [{seed: listSeed}]} = await client.query(
                    `INSERT INTO lists
                (uid, id, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                ($1, $2, 'record-l-1', 1, $3, 'shared', $4, $5)
                RETURNING seed`,
                    [
                        authorUid,
                        internalId,
                        timestamp,
                        JSON.stringify([
                            {
                                record_id: 'record-b-1-1',
                                title: `Stub bookmark #1-1`,
                                uri: `https://stub_1_1`,
                                description: `Stub bookmark #1-1`,
                                comment: 'Wow!'
                            }
                        ]),
                        {title: listTitle}
                    ]
                );
                await client.query(
                    `INSERT INTO subscriptions
                    (uid, list_id) VALUES
                    ($1, (SELECT id FROM lists WHERE id = $2))`,
                    [subscriberUid, internalId]
                );

                return listSeed;
            });

            const publicId = encodePublicId(`${internalId}`, seed);
            const avatarUrl = getAvatarUrl(avatarId);

            const body = {only: 'my_subscriptions'};
            const expected = [
                {
                    public_id: publicId,
                    record_id: 'record-l-1',
                    revision: 1,
                    timestamp: timestamp.getTime(),
                    title: listTitle,
                    author: authorName,
                    avatar_url: avatarUrl,
                    status: 'shared',
                    meta: {
                        is_current_user_author: false,
                        is_current_user_subscribed: true
                    },
                    bookmarks: [
                        {
                            record_id: 'record-b-1-1',
                            title: `Stub bookmark #1-1`,
                            uri: `https://stub_1_1`,
                            description: `Stub bookmark #1-1`,
                            comment: 'Wow!'
                        }
                    ]
                }
            ];

            const nockBlackbox = nock(intHosts.blackbox)
                .get('/')
                .query(true)
                .times(1)
                .reply(200, {
                    users: [
                        {
                            display_name: {
                                public_name: 'Vasya Pupkin',
                                avatar: {default: '0/0-0'}
                            },
                            uid: {
                                value: authorUid
                            }
                        }
                    ]
                });

            const res = await client.post(url, {
                headers: uid111AuthHeaders,
                json: body
            });

            expect(nockBlackbox.isDone()).to.be.true;
            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expectArraysDeepEqual(actual, expected);
        });
    });

    describe('with "only" = "<array of public_ids>"', () => {
        beforeEach(clearDb);

        it('should return empty array if no public_ids passed', async () => {
            const authorUid = '555';
            const timestamp = new Date();

            const firstInternalId = random(1, 2000);
            const secondInternalId = firstInternalId + 1;

            await dbClient.executeWriteQuery({
                text: `INSERT INTO lists
                    (uid, id, record_id, revision, timestamp, status, bookmarks) VALUES
                    ($1, $3, 'record-l-1', 1, $2, 'shared', '[]'),
                    ($1, $4, 'record-l-2', 2, $2, 'shared', '[]')`,
                values: [authorUid, timestamp, firstInternalId, secondInternalId]
            });

            const body = {only: []};
            const expected: any[] = [];

            const res = await client.post(url, {
                headers: uid111AuthHeaders,
                json: body
            });

            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expect(actual).to.deep.equal(expected);
        });

        it('should skip lists with non-existed public_id', async () => {
            const uid = '111';
            const timestamp = new Date();
            const icon = 'rubric:color';
            const description = 'Stub description';

            const firstInternalId = random(1, 2000);
            const secondInternalId = firstInternalId + 1;
            const thirdInternalId = secondInternalId + 1;
            const nonExistedInternalId = thirdInternalId + 1;

            const {data: {rows: [{seed: firstSeed}, {seed: secondSeed}, {seed: thirdSeed}]}} =
                await dbClient.executeWriteQuery<StoredSeed>({
                    text: `INSERT INTO lists
                        (uid, id, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                        ($1, $3, 'record-l-1', 1, $2, 'shared', $6, $9),
                        ($1, $4, 'record-l-2', 2, $2, 'shared', $7, $10),
                        ($1, $5, 'record-l-3', 3, $2, 'shared', $8, '{}')
                        RETURNING seed`,
                    values: [
                        uid,
                        timestamp,
                        firstInternalId,
                        secondInternalId,
                        thirdInternalId,
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
                            }
                        ]),
                        JSON.stringify([
                            {
                                record_id: 'record-b-3-1',
                                title: `Stub bookmark #3-1`,
                                uri: `https://stub_3_1`,
                                description: `Stub bookmark #3-1`
                            },
                            {
                                record_id: 'record-b-3-2',
                                title: `Stub bookmark #3-2`,
                                uri: `https://stub_3_2`,
                                description: `Stub bookmark #3-2`
                            },
                            {
                                record_id: 'record-b-3-3',
                                title: `Stub bookmark #3-3`,
                                uri: `https://stub_3_3`,
                                description: `Stub bookmark #3-3`
                            }
                        ]),
                        {title: 'First list', icon, description},
                        {title: 'Second list', icon, description}
                    ]
                });

            const firstPublicId = encodePublicId(`${firstInternalId}`, firstSeed);
            const secondPublicId = encodePublicId(`${secondInternalId}`, secondSeed);
            const nonExistedPublicId = encodePublicId(`${nonExistedInternalId}`, thirdSeed);

            const nockBlackbox = nock(intHosts.blackbox)
                .get('/')
                .query(true)
                .times(1)
                .reply(200, {
                    users: [
                        {
                            display_name: {
                                public_name: 'Vasya Pupkin',
                                avatar: {default: '1/2-3'}
                            },
                            uid: {value: uid}
                        }
                    ]
                });

            const avatarUrl = getAvatarUrl('1/2-3');

            const body = {only: [firstPublicId, secondPublicId, nonExistedPublicId]};
            const expected = [
                {
                    public_id: firstPublicId,
                    record_id: 'record-l-1',
                    revision: 1,
                    timestamp: timestamp.getTime(),
                    title: `First list`,
                    icon,
                    description,
                    status: 'shared',
                    author: 'Vasya Pupkin',
                    avatar_url: avatarUrl,
                    meta: {
                        is_current_user_author: true,
                        is_current_user_subscribed: false
                    },
                    bookmarks: [
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
                    ]
                },
                {
                    public_id: secondPublicId,
                    record_id: 'record-l-2',
                    revision: 2,
                    timestamp: timestamp.getTime(),
                    title: `Second list`,
                    icon,
                    description,
                    status: 'shared',
                    author: 'Vasya Pupkin',
                    avatar_url: avatarUrl,
                    meta: {
                        is_current_user_author: true,
                        is_current_user_subscribed: false
                    },
                    bookmarks: [
                        {
                            record_id: 'record-b-2-1',
                            title: `Stub bookmark #2-1`,
                            uri: `https://stub_2_1`,
                            description: `Stub bookmark #2-1`
                        }
                    ]
                }
            ];

            const res = await client.post(url, {
                headers: uid111AuthHeaders,
                json: body
            });

            expect(nockBlackbox.isDone()).to.be.true;
            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);

            expectArraysDeepEqual(actual, expected);
        });

        it('should skip lists with invalid seeds encoded in public_id', async () => {
            const uid = '111';
            const timestamp = new Date();
            const icon = 'rubric:color';
            const description = 'Stub description';

            const firstInternalId = random(1, 2000);
            const secondInternalId = firstInternalId + 1;
            const thirdInternalId = secondInternalId + 1;
            const seed = random(1, 2000);
            const nonExistedSeed = seed + 1;

            await dbClient.executeWriteQuery<StoredSeed>({
                text: `INSERT INTO lists
                    (uid, id, seed, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                    ($1, $4, $2, 'record-l-1', 1, $3, 'shared', '[]', $7),
                    ($1, $5, $2, 'record-l-2', 2, $3, 'shared', '[]', '{}'),
                    ($1, $6, $2, 'record-l-3', 3, $3, 'shared', '[]', '{}')`,
                values: [
                    uid,
                    seed,
                    timestamp,
                    firstInternalId,
                    secondInternalId,
                    thirdInternalId,
                    {title: 'First list', icon, description}
                ]
            });

            const firstPublicId = encodePublicId(`${firstInternalId}`, seed);
            const invalidSecondPublicId = encodePublicId(`${secondInternalId}`, nonExistedSeed);
            const invalidThirdPublicId = encodePublicId(`${thirdInternalId}`, nonExistedSeed);

            const nockBlackbox = nock(intHosts.blackbox)
                .get('/')
                .query(true)
                .times(1)
                .reply(200, {
                    users: [
                        {
                            display_name: {
                                public_name: 'Vasya Pupkin',
                                avatar: {default: '0/0-0'}
                            },
                            uid: {value: uid}
                        }
                    ]
                });

            const body = {only: [firstPublicId, invalidSecondPublicId, invalidThirdPublicId]};
            const expected = [
                {
                    public_id: firstPublicId,
                    record_id: 'record-l-1',
                    revision: 1,
                    timestamp: timestamp.getTime(),
                    title: `First list`,
                    icon,
                    description,
                    status: 'shared',
                    author: 'Vasya Pupkin',
                    avatar_url: getAvatarUrl('0/0-0'),
                    meta: {
                        is_current_user_author: true,
                        is_current_user_subscribed: false
                    },
                    bookmarks: []
                }
            ];

            const res = await client.post(url, {
                headers: uid111AuthHeaders,
                json: body
            });

            expect(nockBlackbox.isDone()).to.be.true;
            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);

            expectArraysDeepEqual(actual, expected);
        });

        it('should return the lists of any statuses to its subscriber', async () => {
            const authorUid = '999';
            const subscriberUid = '111';
            const authorName = 'Vasya Pupkin';
            const avatarId = '123/456-789';
            const timestamp = new Date();
            const icon = 'rubric:color';
            const description = 'Stub description';

            const firstSharedInternalId = random(1, 2000);
            const secondSharedInternalId = firstSharedInternalId + 1;
            const firstClosedInternalId = secondSharedInternalId + 1;
            const secondClosedInternalId = firstClosedInternalId + 1;
            const firstDeletedInternalId = secondClosedInternalId + 1;
            const secondDeletedInternalId = firstDeletedInternalId + 1;

            const {data: storedSeeds} =
                await dbClient.executeWriteCallback(async (client) => {
                    const {rows: storedSeedObjects} =
                        await client.query(
                            `INSERT INTO lists
                            (uid, id, record_id, revision, timestamp, status, bookmarks, attributes)
                            VALUES
                            ($1, $3, 'record-l-1', 1, $2, 'shared', '[]', $9),
                            ($1, $4, 'record-l-2', 2, $2, 'shared', '[]', $10),
                            ($1, $5, 'record-l-3', 3, $2, 'closed', '[]', $11),
                            ($1, $6, 'record-l-4', 4, $2, 'closed', '[]', '{}'),
                            ($1, $7, 'record-l-5', 5, $2, 'deleted', '[]', $12),
                            ($1, $8, 'record-l-6', 6, $2, 'deleted', '[]', '{}')
                            RETURNING seed`,
                            [
                                authorUid,
                                timestamp,
                                firstSharedInternalId,
                                secondSharedInternalId,
                                firstClosedInternalId,
                                secondClosedInternalId,
                                firstDeletedInternalId,
                                secondDeletedInternalId,
                                {title: 'First shared list', icon, description},
                                {title: 'Second shared list', icon, description},
                                {title: 'First closed list'},
                                {title: 'First deleted list'}
                            ]
                        );
                    await client.query(
                        `INSERT INTO subscriptions
                        (list_id, uid) VALUES
                        ((SELECT id FROM lists WHERE id = $2), $1),
                        ((SELECT id FROM lists WHERE id = $3), $1),
                        ((SELECT id FROM lists WHERE id = $4), $1)`,
                        [
                            subscriberUid,
                            firstSharedInternalId,
                            firstClosedInternalId,
                            firstDeletedInternalId
                        ]
                    );

                    return storedSeedObjects.map((item) => item.seed);
                });

            const firstSharedPublicId = encodePublicId(`${firstSharedInternalId}`, storedSeeds[0]);
            const secondSharedPublicId = encodePublicId(`${secondSharedInternalId}`, storedSeeds[1]);
            const firstClosedPublicId = encodePublicId(`${firstClosedInternalId}`, storedSeeds[2]);
            const secondClosedPublicId = encodePublicId(`${secondClosedInternalId}`, storedSeeds[3]);
            const firstDeletedPublicId = encodePublicId(`${firstDeletedInternalId}`, storedSeeds[4]);
            const secondDeletedPublicId = encodePublicId(`${secondDeletedInternalId}`, storedSeeds[5]);

            const body = {only: [
                firstSharedPublicId,
                secondSharedPublicId,
                firstClosedPublicId,
                secondClosedPublicId,
                firstDeletedPublicId,
                secondDeletedPublicId
            ]};
            const avatarUrl = getAvatarUrl(avatarId);
            const expected = [
                {
                    public_id: firstSharedPublicId,
                    record_id: 'record-l-1',
                    revision: 1,
                    timestamp: timestamp.getTime(),
                    title: `First shared list`,
                    icon,
                    description,
                    author: authorName,
                    avatar_url: avatarUrl,
                    status: 'shared',
                    meta: {
                        is_current_user_author: false,
                        is_current_user_subscribed: true
                    },
                    bookmarks: []
                },
                {
                    public_id: secondSharedPublicId,
                    record_id: 'record-l-2',
                    revision: 2,
                    timestamp: timestamp.getTime(),
                    title: `Second shared list`,
                    icon,
                    description,
                    author: authorName,
                    avatar_url: avatarUrl,
                    status: 'shared',
                    meta: {
                        is_current_user_author: false,
                        is_current_user_subscribed: false
                    },
                    bookmarks: []
                },
                {
                    public_id: firstClosedPublicId,
                    record_id: 'record-l-3',
                    revision: 3,
                    timestamp: timestamp.getTime(),
                    title: `First closed list`,
                    status: 'closed',
                    meta: {
                        is_current_user_author: false,
                        is_current_user_subscribed: true
                    },
                    bookmarks: []
                },
                {
                    public_id: firstDeletedPublicId,
                    record_id: 'record-l-5',
                    revision: 5,
                    timestamp: timestamp.getTime(),
                    title: `First deleted list`,
                    status: 'deleted',
                    meta: {
                        is_current_user_author: false,
                        is_current_user_subscribed: true
                    },
                    bookmarks: []
                }
            ];

            const nockBlackbox = nock(intHosts.blackbox)
                .get('/')
                .query(true)
                .times(1)
                .reply(200, {
                    users: [
                        {
                            display_name: {
                                public_name: 'Vasya Pupkin',
                                avatar: {default: avatarId}
                            },
                            uid: {value: authorUid}
                        }
                    ]
                });

            const res = await client.post(url, {
                headers: uid111AuthHeaders,
                json: body
            });

            expect(nockBlackbox.isDone()).to.be.true;
            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);

            expectArraysDeepEqual(actual, expected);
        });

        it('should skip closed and deleted lists for non-author\'s requests', async () => {
            const uid = '999';
            const authorName = 'Vasya Pupkin';
            const avatarId = '111/222-33';
            const timestamp = new Date();
            const icon = 'rubric:color';
            const description = 'Stub description';

            const firstInternalId = random(1, 2000);
            const secondInternalId = firstInternalId + 1;
            const thirdInternalId = secondInternalId + 1;

            const {data: {rows: [{seed: firstSeed}, {seed: secondSeed}, {seed: thirdSeed}]}} =
                await dbClient.executeWriteQuery<StoredSeed>({
                    text: `INSERT INTO lists
                        (uid, id, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                        ($1, $3, 'record-l-1', 1, $2, 'shared', '[]', $6),
                        ($1, $4, 'record-l-2', 2, $2, 'closed', '[]', '{}'),
                        ($1, $5, 'record-l-3', 3, $2, 'deleted', '[]', '{}')
                        RETURNING seed`,
                    values: [
                        uid,
                        timestamp,
                        firstInternalId,
                        secondInternalId,
                        thirdInternalId,
                        {title: 'Shared list', icon, description}
                    ]
                });

            const firstPublicId = encodePublicId(`${firstInternalId}`, firstSeed);
            const secondPublicId = encodePublicId(`${secondInternalId}`, secondSeed);
            const thirdPublicId = encodePublicId(`${thirdInternalId}`, thirdSeed);

            const body = {only: [
                firstPublicId,
                secondPublicId,
                thirdPublicId
            ]};
            const expected = [
                {
                    public_id: firstPublicId,
                    record_id: 'record-l-1',
                    revision: 1,
                    timestamp: timestamp.getTime(),
                    title: `Shared list`,
                    icon,
                    description,
                    author: authorName,
                    avatar_url: getAvatarUrl(avatarId),
                    status: 'shared',
                    meta: {
                        is_current_user_author: false,
                        is_current_user_subscribed: false
                    },
                    bookmarks: []
                }
            ];

            const nockBlackbox = nock(intHosts.blackbox)
                .get('/')
                .query(true)
                .times(1)
                .reply(200, {
                    users: [
                        {
                            display_name: {
                                public_name: 'Vasya Pupkin',
                                avatar: {default: avatarId}
                            },
                            uid: {
                                value: uid
                            }
                        }
                    ]
                });

            const res = await client.post(url, {
                headers: uid111AuthHeaders,
                json: body
            });

            expect(nockBlackbox.isDone()).to.be.true;
            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expectArraysDeepEqual(actual, expected);
        });

        it('should return shared and closed lists for author\'s requests', async () => {
            const uid = '111';
            const timestamp = new Date();
            const icon = 'rubric:color';
            const description = 'Stub description';

            const firstInternalId = random(1, 2000);
            const secondInternalId = firstInternalId + 1;
            const thirdInternalId = secondInternalId + 1;

            const {data: {rows: [{seed: firstSeed}, {seed: secondSeed}, {seed: thirdSeed}]}} =
                await dbClient.executeWriteQuery<StoredSeed>({
                    text: `INSERT INTO lists
                        (uid, id, record_id, revision, timestamp, status, attributes, bookmarks) VALUES
                        ($1, $3, 'record-l-1', 1, $2, 'shared', $6, $8),
                        ($1, $4, 'record-l-2', 2, $2, 'closed', $7, '[]'),
                        ($1, $5, 'record-l-3', 3, $2, 'deleted', '{}', '[]')
                        RETURNING seed`,
                    values: [
                        uid,
                        timestamp,
                        firstInternalId,
                        secondInternalId,
                        thirdInternalId,
                        {title: 'Shared list', icon, description},
                        {title: 'Closed list'},
                        JSON.stringify([
                            {
                                record_id: 'record-b-1-1',
                                title: `Stub bookmark #1-1`,
                                uri: `https://stub_1_1`,
                                description: `Stub bookmark #1-1`
                            }
                        ])
                    ]
                });

            const firstPublicId = encodePublicId(`${firstInternalId}`, firstSeed);
            const secondPublicId = encodePublicId(`${secondInternalId}`, secondSeed);
            const thirdPublicId = encodePublicId(`${thirdInternalId}`, thirdSeed);

            const body = {only: [
                firstPublicId,
                secondPublicId,
                thirdPublicId
            ]};
            const avatarUrl = getAvatarUrl('0/000');
            const expected = [
                {
                    public_id: firstPublicId,
                    record_id: 'record-l-1',
                    revision: 1,
                    timestamp: timestamp.getTime(),
                    title: `Shared list`,
                    icon,
                    description,
                    status: 'shared',
                    author: 'Vasya Pupkin',
                    avatar_url: avatarUrl,
                    meta: {
                        is_current_user_author: true,
                        is_current_user_subscribed: false
                    },
                    bookmarks: [
                        {
                            record_id: 'record-b-1-1',
                            title: `Stub bookmark #1-1`,
                            uri: `https://stub_1_1`,
                            description: `Stub bookmark #1-1`
                        }
                    ]
                },
                {
                    public_id: secondPublicId,
                    record_id: 'record-l-2',
                    revision: 2,
                    timestamp: timestamp.getTime(),
                    title: `Closed list`,
                    status: 'closed',
                    meta: {
                        is_current_user_author: true,
                        is_current_user_subscribed: false
                    },
                    bookmarks: []
                }
            ];

            const nockBlackbox = nock(intHosts.blackbox)
                .get('/')
                .query(true)
                .times(1)
                .reply(200, {
                    users: [
                        {
                            display_name: {
                                public_name: 'Vasya Pupkin',
                                avatar: {default: '0/000'}
                            },
                            uid: {value: uid}
                        }
                    ]
                });

            const res = await client.post(url, {
                headers: uid111AuthHeaders,
                json: body
            });

            expect(nockBlackbox.isDone()).to.be.true;
            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expectArraysDeepEqual(actual, expected);
        });

        it('should return only shared lists for an unauthorized user', async () => {
            const firstUid = '333';
            const secondUid = '999';
            const firstAuthorName = 'Vasya Pupkin';
            const secondAuthorName = 'Ivan Ivanov';
            const firstAvatarId = '0/123';
            const secondAvatarId = '4/567';
            const timestamp = new Date();
            const icon = 'rubric:color';
            const description = 'Stub description';

            const firstInternalId = random(1, 2000);
            const secondInternalId = firstInternalId + 1;
            const thirdInternalId = secondInternalId + 1;
            const fourthInternalId = thirdInternalId + 1;

            const {data: {
                rows: [
                    {seed: firstSeed},
                    {seed: secondSeed},
                    {seed: thirdSeed},
                    {seed: fourthSeed}
                ]
            }} = await dbClient.executeWriteQuery<StoredSeed>({
                text: `INSERT INTO lists
                    (uid, id, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                    ($1, $4, 'record-l-1', 1, $3, 'shared', '[]', $8),
                    ($1, $5, 'record-l-2', 2, $3, 'closed', '[]', '{}'),
                    ($1, $6, 'record-l-3', 3, $3, 'deleted', '[]', '{}'),
                    ($2, $7, 'record-l-5', 4, $3, 'shared', '[]', $9)
                    RETURNING seed`,
                values: [
                    firstUid,
                    secondUid,
                    timestamp,
                    firstInternalId,
                    secondInternalId,
                    thirdInternalId,
                    fourthInternalId,
                    {title: 'Shared list 1', icon, description},
                    {title: 'Shared list 2', icon, description}
                ]
            });

            const firstPublicId = encodePublicId(`${firstInternalId}`, firstSeed);
            const secondPublicId = encodePublicId(`${secondInternalId}`, secondSeed);
            const thirdPublicId = encodePublicId(`${thirdInternalId}`, thirdSeed);
            const fourthPublicId = encodePublicId(`${fourthInternalId}`, fourthSeed);

            const body = {only: [
                firstPublicId,
                secondPublicId,
                thirdPublicId,
                fourthPublicId
            ]};
            const expected = [
                {
                    public_id: firstPublicId,
                    record_id: 'record-l-1',
                    revision: 1,
                    timestamp: timestamp.getTime(),
                    title: `Shared list 1`,
                    icon,
                    description,
                    author: firstAuthorName,
                    avatar_url: getAvatarUrl(firstAvatarId),
                    status: 'shared',
                    meta: {
                        is_current_user_author: false,
                        is_current_user_subscribed: false
                    },
                    bookmarks: []
                },
                {
                    public_id: fourthPublicId,
                    record_id: 'record-l-5',
                    revision: 4,
                    timestamp: timestamp.getTime(),
                    title: `Shared list 2`,
                    icon,
                    description,
                    author: secondAuthorName,
                    avatar_url: getAvatarUrl(secondAvatarId),
                    status: 'shared',
                    meta: {
                        is_current_user_author: false,
                        is_current_user_subscribed: false
                    },
                    bookmarks: []
                }
            ];

            const nockBlackbox = nock(intHosts.blackbox)
                .get('/')
                .query(true)
                .times(1)
                .reply(200, {
                    users: [
                        {
                            display_name: {
                                public_name: firstAuthorName,
                                avatar: {default: firstAvatarId}
                            },
                            uid: {
                                value: firstUid
                            }
                        },
                        {
                            display_name: {
                                public_name: secondAuthorName,
                                avatar: {default: secondAvatarId}
                            },
                            uid: {
                                value: secondUid
                            }
                        }
                    ]
                });

            const res = await client.post(url, {
                headers: {
                    'X-Ya-Service-Ticket': SERVICE_TICKET_MAPS
                },
                json: body
            });

            expect(nockBlackbox.isDone()).to.be.true;
            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expectArraysDeepEqual(actual, expected);
        });

        it('should return shared and closed lists for author', async () => {
            const uid = '111';
            const authorName = 'Nyan Cat';
            const avatarId = 'nyan/cat-id';
            const timestamp = new Date();
            const icon = 'rubric:color';
            const description = 'Stub description';

            const firstInternalId = random(1, 2000);
            const secondInternalId = firstInternalId + 1;
            const thirdInternalId = secondInternalId + 1;

            const {data: {rows: [{seed: firstSeed}, {seed: secondSeed}, {seed: thirdSeed}]}} =
                await dbClient.executeWriteQuery<StoredSeed>({
                    text: `INSERT INTO lists
                        (uid, id, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                        ($1, $3, 'record-l-1', 1, $2, 'shared', '[]', $6),
                        ($1, $4, 'record-l-2', 2, $2, 'closed', '[]', $7),
                        ($1, $5, 'record-l-3', 3, $2, 'deleted', '[]', '{}')
                        RETURNING seed`,
                    values: [
                        uid,
                        timestamp,
                        firstInternalId,
                        secondInternalId,
                        thirdInternalId,
                        {title: 'Shared list', icon, description},
                        {title: 'Closed list'}
                    ]
                });

            const firstPublicId = encodePublicId(`${firstInternalId}`, firstSeed);
            const secondPublicId = encodePublicId(`${secondInternalId}`, secondSeed);
            const thirdPublicId = encodePublicId(`${thirdInternalId}`, thirdSeed);

            const body = {only: [
                firstPublicId,
                secondPublicId,
                thirdPublicId
            ]};
            const avatarUrl = getAvatarUrl(avatarId);
            const expected = [
                {
                    public_id: firstPublicId,
                    record_id: 'record-l-1',
                    revision: 1,
                    timestamp: timestamp.getTime(),
                    title: `Shared list`,
                    icon,
                    description,
                    author: authorName,
                    avatar_url: avatarUrl,
                    status: 'shared',
                    meta: {
                        is_current_user_author: true,
                        is_current_user_subscribed: false
                    },
                    bookmarks: []
                },
                {
                    public_id: secondPublicId,
                    record_id: 'record-l-2',
                    revision: 2,
                    timestamp: timestamp.getTime(),
                    title: `Closed list`,
                    status: 'closed',
                    meta: {
                        is_current_user_author: true,
                        is_current_user_subscribed: false
                    },
                    bookmarks: []
                }
            ];

            const nockBlackbox = nock(intHosts.blackbox)
                .get('/')
                .query(true)
                .times(1)
                .reply(200, {
                    users: [
                        {
                            display_name: {
                                public_name: authorName,
                                avatar: {default: avatarId}
                            },
                            uid: {value: uid}
                        }
                    ]
                });

            const res = await client.post(url, {
                headers: uid111AuthHeaders,
                json: body
            });

            expect(nockBlackbox.isDone()).to.be.true;
            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expectArraysDeepEqual(actual, expected);
        });

        // MAPSHTTPAPI-2416
        it('should return the bookmark comment if it exists', async () => {
            const uid = '111';
            const authorName = 'Nyan Cat';
            const avatarId = 'nyan/cat-id';
            const timestamp = new Date();

            const internalId = random(1, 2000);

            const {data: {rows: [{seed: listSeed}]}} =
                await dbClient.executeWriteQuery<StoredSeed>({
                    text: `INSERT INTO lists
                        (uid, id, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                        ($1, $2, 'record-l-1', 1, $3, 'shared', $4, $5)
                        RETURNING seed`,
                    values: [
                        uid,
                        internalId,
                        timestamp,
                        JSON.stringify([{
                            record_id: 'record-b-1-1',
                            title: `Stub bookmark #1-1`,
                            uri: `https://stub_1_1`,
                            description: `Stub bookmark #1-1`,
                            comment: 'YEY!'
                        }]),
                        {title: 'Shared list'}
                    ]
                });

            const publicId = encodePublicId(`${internalId}`, listSeed);

            const body = {only: [publicId]};
            const avatarUrl = getAvatarUrl(avatarId);
            const expected = [
                {
                    public_id: publicId,
                    record_id: 'record-l-1',
                    revision: 1,
                    timestamp: timestamp.getTime(),
                    title: `Shared list`,
                    author: authorName,
                    avatar_url: avatarUrl,
                    status: 'shared',
                    meta: {
                        is_current_user_author: true,
                        is_current_user_subscribed: false
                    },
                    bookmarks: [{
                        record_id: 'record-b-1-1',
                        title: `Stub bookmark #1-1`,
                        uri: `https://stub_1_1`,
                        description: `Stub bookmark #1-1`,
                        comment: 'YEY!'
                    }]
                }
            ];

            const nockBlackbox = nock(intHosts.blackbox)
                .get('/')
                .query(true)
                .times(1)
                .reply(200, {
                    users: [
                        {
                            display_name: {
                                public_name: authorName,
                                avatar: {default: avatarId}
                            },
                            uid: {value: uid}
                        }
                    ]
                });

            const res = await client.post(url, {
                headers: uid111AuthHeaders,
                json: body
            });

            expect(nockBlackbox.isDone()).to.be.true;
            expect(res.statusCode).to.be.equal(200);
            const actual = JSON.parse(res.body);
            expectArraysDeepEqual(actual, expected);
        });
    });
});
