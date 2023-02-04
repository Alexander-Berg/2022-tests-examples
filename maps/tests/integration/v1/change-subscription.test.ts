import {expect} from 'chai';
import * as fs from 'fs';
import nock from 'nock';
import {URL} from 'url';
import {Hosts} from '@yandex-int/maps-host-configs';

import {app} from 'app/app';
import {dbClient} from 'app/lib/db-client';
import {intHostsLoader} from 'app/lib/hosts';
import {getAvatarUrl} from 'app/v1/helpers/avatar';
import {encodePublicId, PUBLIC_ID_PATTERN} from 'app/v1/helpers/public-id';

import {client, TestServer} from 'tests/integration/test-server';
import {TvmDaemon} from 'tests/integration/tvm-daemon';
import {clearDb} from 'tests/helpers/clear-db';
import {random} from 'tests/helpers/generation';
import {StoredSeed} from 'tests/helpers/db';

const USER1_TICKET = fs.readFileSync('src/tests/fixtures/tvm-tickets/user/uid-1').toString().trim();
const SERVICE_TICKET_MAPS = fs.readFileSync('src/tests/fixtures/tvm-tickets/service/maps').toString().trim();
const UNKNOWN_SERVICE_TICKET = fs.readFileSync('src/tests/fixtures/tvm-tickets/service/unknown').toString().trim();

describe('POST /v1/change_subscription', () => {
    let tvmDaemon: TvmDaemon;
    let server: TestServer;
    let url: URL;

    before(async () => {
        server = await TestServer.start(app);
        tvmDaemon = await TvmDaemon.start();
        url = new URL(`${server.url}/v1/change_subscription`);
    });

    after(async () => {
        await tvmDaemon.stop();
        await server.stop();
    });

    const user1Headers = {
        'X-Ya-Service-Ticket': SERVICE_TICKET_MAPS,
        'X-Ya-User-Ticket': USER1_TICKET
    };

    describe('check query and headers', () => {
        it('should throw 400 when query does not contain `add` or `remove` field', async () => {
            const res = await client.post(url, {
                headers: user1Headers
            });

            expect(res.statusCode).to.equal(400);
            expect(JSON.parse(res.body).message).to.equal('Error validating body: ' +
                '"value" must contain at least one of [add, remove]');
        });

        it('should throw 400 when query contains both `add` and `remove` fields', async () => {
            const res = await client.post(url, {
                headers: user1Headers,
                json: {
                    add: 'KY9bHA45',
                    remove: 'KY9bHA45'
                }
            });

            expect(res.statusCode).to.equal(400);
            expect(JSON.parse(res.body).message).to.equal('Error validating body: ' +
                '"value" contains a conflict between exclusive peers [add, remove]');
        });

        it('should throw 400 when query contains incorrect `add` field', async () => {
            const res = await client.post(url, {
                headers: user1Headers,
                json: {
                    add: '@invalid_$'
                }
            });

            expect(res.statusCode).to.equal(400);
            expect(JSON.parse(res.body).message).to.equal('Error validating body: ' +
                '"add" with value "@invalid_$" fails to match the required pattern: ' +
                `${PUBLIC_ID_PATTERN}`);
        });

        it('should throw 400 when query contains incorrect `remove` field', async () => {
            const res = await client.post(url, {
                headers: user1Headers,
                json: {
                    remove: '@invalid_$'
                }
            });

            expect(res.statusCode).to.equal(400);
            expect(JSON.parse(res.body).message).to.equal('Error validating body: ' +
                '"remove" with value "@invalid_$" fails to match the required pattern: ' +
                `${PUBLIC_ID_PATTERN}`);
        });

        it('should throw 401 when service ticket is missing', async () => {
            const res = await client.post(url, {
                headers: {
                    'X-Ya-User-Ticket': USER1_TICKET
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
                    'X-Ya-User-Ticket': USER1_TICKET
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
                    'X-Ya-User-Ticket': USER1_TICKET
                },
                json: {}
            });

            expect(res.statusCode).to.equal(403);
            expect(JSON.parse(res.body).message).to.equal('TVM service does not have enough access rights');
        });
    });

    describe('change subscription', () => {
        let intHosts: Hosts;

        before(async () => {
            intHosts = await intHostsLoader.get();
            nock.disableNetConnect();
            nock.enableNetConnect(/(127.0.0.1|localhost)/);
        });

        after(() => {
            nock.enableNetConnect();
        });

        beforeEach(clearDb);

        afterEach(nock.cleanAll);

        describe('add', () => {
            it('should throw 410 when list does not exist', async () => {
                const searchInternalId = random(1, 2000);
                const otherInternalId = searchInternalId + 1;

                const {data: {rows: [{seed: storedSeed}]}} = await dbClient.executeWriteQuery<StoredSeed>({
                    text: `INSERT INTO lists
                        (uid, id, record_id, revision, timestamp, status, bookmarks) VALUES
                        (123, $1, 'record', 3, '2021-01-01', 'closed', '[]')
                        RETURNING seed`,
                    values: [otherInternalId]
                });

                const searchPublicId = encodePublicId(`${searchInternalId}`, storedSeed);

                const res = await client.post(url, {
                    headers: user1Headers,
                    json: {
                        add: searchPublicId
                    }
                });

                expect(res.statusCode).to.equal(410);
                expect(JSON.parse(res.body).message).to.equal(`List ${searchPublicId} does not exist`);
            });

            it('should return 410 when we add subscription and list is not shared', async () => {
                const internalId = random(1, 2000);

                const {data: {rows: [{seed: storedSeed}]}} = await dbClient.executeWriteQuery<StoredSeed>({
                    text: `INSERT INTO lists
                        (uid, id, record_id, revision, timestamp, status, bookmarks) VALUES
                        (123, $1, 'record', 2, '2021-01-01', 'closed', '[]')
                        RETURNING seed`,
                    values: [internalId]
                });

                const listPublicId = encodePublicId(`${internalId}`, storedSeed);

                const res = await client.post(url, {
                    headers: user1Headers,
                    json: {
                        add: listPublicId
                    }
                });

                expect(res.statusCode).to.equal(410);
                expect(JSON.parse(res.body).message).to.equal(`List ${listPublicId} is not shared`);
            });

            it('should return 400 when the user is trying to subscribe to his own list', async () => {
                const internalId = random(1, 2000);

                const {data: {rows: [{seed: storedSeed}]}} = await dbClient.executeWriteQuery<StoredSeed>({
                    text: `INSERT INTO lists
                        (uid, id, record_id, revision, timestamp, status, bookmarks) VALUES
                        (1, $1, 'record', 1, '2021-01-01', 'shared', '[]')
                        RETURNING seed`,
                    values: [internalId]
                });

                const listPublicId = encodePublicId(`${internalId}`, storedSeed);

                const res = await client.post(url, {
                    headers: user1Headers,
                    json: {
                        add: listPublicId
                    }
                });

                expect(res.statusCode).to.equal(400);
                expect(JSON.parse(res.body).message).to.equal(`List ${listPublicId} is belong to the user`);
            });

            it('should add subscription successfully', async () => {
                const internalId = random(1, 2000);
                const otherInternalId = internalId + 1;
                const authorUid = 123;

                const {data: {rows: [{seed: seed}]}} = await dbClient.executeWriteQuery<StoredSeed>({
                    text: `INSERT INTO lists
                        (uid, id, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                        ($1, $2, 'record-l-1', 1, '2021-01-01 12:00:00+00', 'shared', $4, $6),
                        (456, $3, 'record-l-2', 2, '2021-01-02', 'shared', $5, $7)
                        RETURNING seed`,
                    values: [
                        authorUid,
                        internalId,
                        otherInternalId,
                        JSON.stringify([
                            {record_id: 'record-b-1', title: 'First b', uri: 'uri-1', description: 'descr-1'},
                            {record_id: 'record-b-3', title: 'Third b', uri: 'uri-3', description: 'descr-3'}
                        ]),
                        JSON.stringify([
                            {record_id: 'record-b-2', title: 'Second b', uri: 'uri-2', description: 'descr-2'}
                        ]),
                        {title: 'First list', icon: 'Icon 1', description: 'Description 1'},
                        {title: 'Second list', icon: 'Icon 2', description: 'Description 2'}
                    ]
                });

                const listPublicId = encodePublicId(`${internalId}`, seed);

                const nockBlackbox = nock(intHosts.blackbox)
                    .get('/')
                    .query(true)
                    .reply(200, {
                        users: [
                            {
                                uid: {value: authorUid},
                                display_name: {
                                    public_name: 'Vasya Pupkin',
                                    avatar: {
                                        default: '0/0-0'
                                    }
                                }
                            }
                        ]
                    });

                const res = await client.post(url, {
                    headers: user1Headers,
                    json: {
                        add: listPublicId
                    }
                });

                expect(nockBlackbox.isDone()).to.be.true;
                expect(res.statusCode).to.equal(200);
                expect(JSON.parse(res.body)).to.deep.equal({
                    public_id: listPublicId,
                    record_id: 'record-l-1',
                    author: 'Vasya Pupkin',
                    avatar_url: getAvatarUrl('0/0-0'),
                    status: 'shared',
                    revision: 1,
                    timestamp: new Date('2021-01-01T12:00:00.000Z').valueOf(),
                    title: 'First list',
                    icon: 'Icon 1',
                    description: 'Description 1',
                    bookmarks: [
                        {
                            record_id: 'record-b-1',
                            title: 'First b',
                            uri: 'uri-1',
                            description: 'descr-1'
                        },
                        {
                            record_id: 'record-b-3',
                            title: 'Third b',
                            uri: 'uri-3',
                            description: 'descr-3'
                        }
                    ],
                    subscription_changed: true
                });

                await dbClient.executeReadCallback(async (pgClient) => {
                    const {rows} = await pgClient.query(
                        `SELECT
                            uid, list_id
                        FROM subscriptions`
                    );

                    expect(rows).to.deep.equal([{uid: '1', list_id: `${internalId}`}]);
                });
            });

            it('should not add subscription when user is already subscribed', async () => {
                const internalId = random(1, 2000);
                const authorUid = 123;

                const {data: storedSeed} = await dbClient.executeWriteCallback(async (pgClient) => {
                    const {rows: [{id: listId, seed: storedSeed}]} = await pgClient.query(`INSERT INTO lists
                        (uid, id, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                        ($1, $2, 'record-l-1', 1, '2021-01-01 12:00:00+00', 'shared', $3, $4)
                        RETURNING id, seed`,
                        [
                            authorUid,
                            internalId,
                            JSON.stringify([
                                {record_id: 'record-b-1', title: 'First b', uri: 'uri-1', description: 'descr-1'}
                            ]),
                            {title: 'First list', icon: 'I_1', description: 'D_2'}
                        ]
                    );

                    // subscribe user on list
                    await pgClient.query(`INSERT INTO subscriptions
                        (list_id, uid) VALUES
                        ($1, 1)`,
                        [listId]
                    );

                    return storedSeed;
                });
                const listPublicId = encodePublicId(`${internalId}`, storedSeed);

                const nockBlackbox = nock(intHosts.blackbox)
                    .get('/')
                    .query(true)
                    .reply(200, {
                        users: [
                            {
                                uid: {value: authorUid},
                                display_name: {
                                    public_name: 'Vasya Pupkin',
                                    avatar: {default: '123/456'}
                                }
                            }
                        ]
                    });

                const res = await client.post(url, {
                    headers: user1Headers,
                    json: {
                        add: listPublicId
                    }
                });

                expect(res.statusCode).to.equal(200);
                expect(nockBlackbox.isDone()).to.be.true;
                expect(JSON.parse(res.body)).to.deep.equal({
                    public_id: listPublicId,
                    record_id: 'record-l-1',
                    author: 'Vasya Pupkin',
                    avatar_url: getAvatarUrl('123/456'),
                    status: 'shared',
                    revision: 1,
                    timestamp: new Date('2021-01-01T12:00:00.000Z').valueOf(),
                    title: 'First list',
                    icon: 'I_1',
                    description: 'D_2',
                    bookmarks: [
                        {
                            record_id: 'record-b-1',
                            title: 'First b',
                            uri: 'uri-1',
                            description: 'descr-1'
                        }
                    ],
                    subscription_changed: false
                });

                await dbClient.executeReadCallback(async (pgClient) => {
                    const {rows} = await pgClient.query(
                        `SELECT
                        uid, list_id
                        FROM subscriptions`
                    );

                    expect(rows).to.deep.equal([{uid: '1', list_id: `${internalId}`}]);
                });
            });

            it('should return empty string in title, icon and description ' +
                'when these fields are missing in the attributes', async () => {
                const internalId = random(1, 2000);
                const authorUid = 123;

                const {data: {rows: [{seed: seed}]}} = await dbClient.executeWriteQuery<StoredSeed>({
                    text: `INSERT INTO lists
                        (uid, id, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                        ($1, $2, 'record-l-1', 1, '2021-01-01 12:00:00+00', 'shared', $3, $4)
                        RETURNING seed`,
                    values: [
                        authorUid,
                        internalId,
                        JSON.stringify([
                            {record_id: 'record-b-1', title: 'First b', uri: 'uri-1', description: 'descr-1'}
                        ]),
                        {} // <- empty attributes
                    ]
                });

                const publicId = encodePublicId(`${internalId}`, seed);

                const nockBlackbox = nock(intHosts.blackbox)
                    .get('/')
                    .query(true)
                    .reply(200, {
                        users: [
                            {
                                uid: {value: authorUid},
                                display_name: {
                                    public_name: 'Vasya Pupkin',
                                    avatar: {
                                        default: '0/0-0'
                                    }
                                }
                            }
                        ]
                    });

                const res = await client.post(url, {
                    headers: user1Headers,
                    json: {
                        add: publicId
                    }
                });

                expect(nockBlackbox.isDone()).to.be.true;
                expect(res.statusCode).to.equal(200);
                expect(JSON.parse(res.body)).to.deep.equal({
                    public_id: publicId,
                    record_id: 'record-l-1',
                    author: 'Vasya Pupkin',
                    avatar_url: getAvatarUrl('0/0-0'),
                    status: 'shared',
                    revision: 1,
                    timestamp: new Date('2021-01-01T12:00:00.000Z').valueOf(),
                    title: '',
                    icon: '',
                    description: '',
                    bookmarks: [
                        {
                            record_id: 'record-b-1',
                            title: 'First b',
                            uri: 'uri-1',
                            description: 'descr-1'
                        }
                    ],
                    subscription_changed: true
                });

                await dbClient.executeReadCallback(async (pgClient) => {
                    const {rows} = await pgClient.query(
                        `SELECT
                            uid, list_id
                        FROM subscriptions`
                    );

                    expect(rows).to.deep.equal([{uid: '1', list_id: `${internalId}`}]);
                });
            });
        });

        describe('remove', () => {
            it('should remove subscription successfully', async () => {
                const internalId = random(1, 2000);
                const otherInternalId = internalId + 1;

                let listIds: string[];

                const {data: seed} = await dbClient.executeWriteCallback(async (pgClient) => {
                    // create lists
                    // tslint:disable:no-unused-variable
                    const {rows: [{seed: seed}, _]} = await pgClient.query(`INSERT INTO lists
                        (uid, id, record_id, revision, timestamp, status, bookmarks) VALUES
                        (123, $1, 'record-l-1', 1, '2021-01-01', 'shared', '[]'),
                        (456, $2, 'record-l-2', 2, '2021-01-02', 'shared', '[]')
                        RETURNING seed`,
                        [
                            internalId,
                            otherInternalId
                        ]
                    );

                    listIds = (await pgClient.query('SELECT id FROM lists ORDER BY record_id'))
                        .rows.map((row) => row.id);

                    // subscribe users on list
                    await pgClient.query(`INSERT INTO subscriptions
                        (list_id, uid) VALUES
                        ($1, 1),
                        ($2, 1),
                        ($1, 2)`,
                        [
                            listIds[0],
                            listIds[1]
                        ]
                    );

                    return seed;
                });

                const listPublicId = encodePublicId(`${internalId}`, seed);

                const res = await client.post(url, {
                    headers: user1Headers,
                    json: {
                        remove: listPublicId
                    }
                });

                expect(res.statusCode).to.equal(200);
                expect(JSON.parse(res.body)).to.deep.equal({
                    subscription_changed: true
                });

                await dbClient.executeReadCallback(async (pgClient) => {
                    const {rows} = await pgClient.query(
                        `SELECT
                        subscriptions.uid, lists.id
                        FROM subscriptions
                        INNER JOIN lists ON lists.id = subscriptions.list_id
                        ORDER BY uid`
                    );

                    expect(rows).to.deep.equal([
                        {id: `${otherInternalId}`, uid: '1'},
                        {id: `${internalId}`, uid: '2'}
                    ]);
                });
            });

            it('should remove subscription when list is not shared', async () => {
                const closedInternalId = random(1, 2000);
                const deletedInternalId = closedInternalId + 1;

                const {data: storedSeeds} = await dbClient.executeWriteCallback(async (pgClient) => {
                    const {rows: [{seed: firstSeed}, {seed: secondSeed}]} =
                        await pgClient.query(`INSERT INTO lists
                            (uid, id, record_id, revision, timestamp, status, bookmarks) VALUES
                            (123, $1, 'record-l-1', 1, '2021-01-01', 'closed', '[]'),
                            (123, $2, 'record-l-2', 2, '2021-01-02', 'deleted', '[]')
                            RETURNING seed`,
                            [
                                closedInternalId,
                                deletedInternalId
                            ]
                        );

                    const listIds = (await pgClient.query('SELECT id FROM lists ORDER BY record_id'))
                        .rows.map((row) => row.id);

                    // subscribe users on list
                    await pgClient.query(`INSERT INTO subscriptions
                        (list_id, uid) VALUES
                        ($1, 1),
                        ($2, 1),
                        ($1, 2)`,
                        [
                            listIds[0],
                            listIds[1]
                        ]
                    );

                    return [firstSeed, secondSeed];
                });

                const closedListPublicId = encodePublicId(`${closedInternalId}`, storedSeeds[0]);
                const deletedListPublicId = encodePublicId(`${deletedInternalId}`, storedSeeds[1]);

                async function checkRemoveRequest(publicListId: string, expected: any) {
                    const res = await client.post(url, {
                        headers: user1Headers,
                        json: {
                            remove: publicListId
                        }
                    });

                    expect(res.statusCode).to.equal(200);
                    expect(JSON.parse(res.body)).to.deep.equal({
                        subscription_changed: true
                    });

                    await dbClient.executeReadCallback(async (pgClient) => {
                        const {rows} = await pgClient.query(
                            `SELECT
                                subscriptions.uid, lists.id
                            FROM subscriptions
                            INNER JOIN lists ON lists.id = subscriptions.list_id
                            ORDER BY lists.id`
                        );

                        expected.sort((a: any, b: any) => Number(a.id) - Number(b.id));

                        expect(rows).to.deep.equal(expected);
                    });
                }

                await checkRemoveRequest(closedListPublicId, [
                    {id: `${closedInternalId}`, uid: '2'},
                    {id: `${deletedInternalId}`, uid: '1'}
                ]);

                await checkRemoveRequest(deletedListPublicId, [
                    {id: `${closedInternalId}`, uid: '2'}
                ]);
            });

            it('should return 200 when subscription is already removed', async () => {
                const internalId = random(1, 2000);

                const {data: storedSeed} = await dbClient.executeWriteCallback(async (pgClient) => {
                    // create lists
                    const {rows: [{id: listId, seed: storedSeed}]} = await pgClient.query(`INSERT INTO lists
                        (uid, id, record_id, revision, timestamp, status, bookmarks) VALUES
                        (123, $1, 'record-l-1', 1, '2021-01-01', 'shared', '[]')
                        RETURNING id, seed`,
                        [internalId]
                    );

                    // subscribe users on list
                    await pgClient.query(`INSERT INTO subscriptions
                        (list_id, uid) VALUES
                        ($1, 2)`,
                        [listId]
                    );

                    return storedSeed;
                });

                const listPublicId = encodePublicId(`${internalId}`, storedSeed);

                const res = await client.post(url, {
                    headers: user1Headers,
                    json: {
                        remove: listPublicId
                    }
                });

                expect(res.statusCode).to.equal(200);
                expect(JSON.parse(res.body)).to.deep.equal({
                    subscription_changed: false
                });

                await dbClient.executeReadCallback(async (pgClient) => {
                    const {rows} = await pgClient.query(
                        `SELECT
                        uid, list_id
                        FROM subscriptions`
                    );

                    expect(rows).to.deep.equal([{list_id: `${internalId}`, uid: '2'}]);
                });
            });
        });
    });
});
