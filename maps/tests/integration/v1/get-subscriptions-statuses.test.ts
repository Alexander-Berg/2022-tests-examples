import {Hosts} from '@yandex-int/maps-host-configs';
import {expect} from 'chai';
import * as fs from 'fs';
import nock from 'nock';
import {URL} from 'url';

import {app} from 'app/app';
import {SharedBookmark} from 'app/lib/api-types';
import {dbClient} from 'app/lib/db-client';
import {intHostsLoader} from 'app/lib/hosts';
import {getAvatarUrl} from 'app/v1/helpers/avatar';
import {encodePublicId} from 'app/v1/helpers/public-id';

import {client, TestServer} from 'tests/integration/test-server';
import {TvmDaemon} from 'tests/integration/tvm-daemon';
import {clearDb} from 'tests/helpers/clear-db';
import {sortByPublicId} from 'tests/helpers/validation';
import {random} from 'tests/helpers/generation';

const USER1_TICKET = fs.readFileSync('src/tests/fixtures/tvm-tickets/user/uid-1').toString().trim();
const SERVICE_TICKET_MAPS = fs.readFileSync('src/tests/fixtures/tvm-tickets/service/maps').toString().trim();
const UNKNOWN_SERVICE_TICKET = fs.readFileSync('src/tests/fixtures/tvm-tickets/service/unknown').toString().trim();

describe('GET /v1/get_subscriptions_statuses', () => {
    let tvmDaemon: TvmDaemon;
    let server: TestServer;
    let intHosts: Hosts;
    let url: URL;

    before(async () => {
        server = await TestServer.start(app);
        tvmDaemon = await TvmDaemon.start();
        intHosts = await intHostsLoader.get();
        url = new URL(`${server.url}/v1/get_subscriptions_statuses`);
    });

    after(async () => {
        await tvmDaemon.stop();
        await server.stop();
    });

    const user1Headers = {
        'X-Ya-Service-Ticket': SERVICE_TICKET_MAPS,
        'X-Ya-User-Ticket': USER1_TICKET
    };

    describe('check headers', () => {
        it('should throw 401 when service ticket is missing', async () => {
            const res = await client.get(url, {
                headers: {
                    'X-Ya-User-Ticket': USER1_TICKET
                }
            });

            expect(res.statusCode).to.equal(401);
            expect(JSON.parse(res.body).message).to.equal('Invalid authentication data provided');
        });

        it('should throw 401 when service ticket is invalid', async () => {
            const res = await client.get(url, {
                headers: {
                    'X-Ya-Service-Ticket': 'shto-to-ne-to',
                    'X-Ya-User-Ticket': USER1_TICKET
                }
            });

            expect(res.statusCode).to.equal(401);
            expect(JSON.parse(res.body).message).to.equal('Invalid TVM service ticket');
        });

        it('should throw 401 when user ticket is missing', async () => {
            const res = await client.get(url, {
                headers: {
                    'X-Ya-Service-Ticket': SERVICE_TICKET_MAPS
                }
            });

            expect(res.statusCode).to.equal(401);
            expect(JSON.parse(res.body).message).to.equal('Invalid authentication data provided');
        });

        it('should throw 401 when user ticket is invalid', async () => {
            const res = await client.get(url, {
                headers: {
                    'X-Ya-Service-Ticket': SERVICE_TICKET_MAPS,
                    'X-Ya-User-Ticket': 'shto-to-ne-to'
                }
            });

            expect(res.statusCode).to.equal(401);
            expect(JSON.parse(res.body).message).to.equal('Invalid TVM user ticket');
        });

        it('should throw 403 when unknown service ticket', async () => {
            const res = await client.get(url, {
                headers: {
                    'X-Ya-Service-Ticket': UNKNOWN_SERVICE_TICKET,
                    'X-Ya-User-Ticket': USER1_TICKET
                }
            });

            expect(res.statusCode).to.equal(403);
            expect(JSON.parse(res.body).message).to.equal('TVM service does not have enough access rights');
        });
    });

    describe('check get data', () => {
        before(() => {
            nock.disableNetConnect();
            nock.enableNetConnect(/(127.0.0.1|localhost)/);
        });

        after(() => {
            nock.enableNetConnect();
        });

        beforeEach(clearDb);

        afterEach(nock.cleanAll);

        it('should return empty array when there are no subscriptions', async () => {
            const res = await client.get(url, {
                headers: user1Headers
            });

            expect(res.statusCode).to.equal(200);
            expect(JSON.parse(res.body)).to.deep.equal([]);
        });

        it('should not return the lists authored by the user', async () => {
            const internalId = random(1, 2000);

            await dbClient.executeWriteCallback(async (pgClient) => {
                // create list
                const {rows: [{id: listId}]} = await pgClient.query(`INSERT INTO lists
                    (uid, id, record_id, revision, timestamp, status, bookmarks) VALUES
                    (1, $1, 'record-1', 1, '2021-01-01 12:00:00+00', 'shared', '[]')
                    RETURNING id`,
                    [internalId]
                );

                // subscribe users on list
                await pgClient.query(`INSERT INTO subscriptions
                    (list_id, uid) VALUES
                    ($1, 2),
                    ($1, 3)`,
                    [listId]
                );
            });

            const res = await client.get(url, {
                headers: user1Headers
            });

            expect(res.statusCode).to.equal(200);
            expect(JSON.parse(res.body)).to.deep.equal([]);
        });

        it('should not return another user`s subscriptions', async () => {
            const internalId = random(1, 2000);

            await dbClient.executeWriteCallback(async (pgClient) => {
                // create list
                const {rows: [{id: listId}]} = await pgClient.query(`INSERT INTO lists
                    (uid, id, record_id, revision, timestamp, status, bookmarks) VALUES
                    (123, $1, 'record-1', 1, '2021-01-01 12:00:00+00', 'shared', '[]')
                    RETURNING id`,
                    [internalId]
                );

                // subscribe users on list
                await pgClient.query(`INSERT INTO subscriptions
                    (list_id, uid) VALUES
                    ($1, 2),
                    ($1, 3)`,
                    [listId]
                );
            });

            const res = await client.get(url, {
                headers: user1Headers
            });

            expect(res.statusCode).to.equal(200);
            expect(JSON.parse(res.body)).to.deep.equal([]);
        });

        it('should return fields data', async () => {
            const firstInternalId = random(1, 2000);
            const secondInternalId = firstInternalId + 1;
            const thirdInternalId = secondInternalId + 1;
            const firstAuthorUid = 123;
            const firstAvatarId = 'golden_autumn';
            const firstAuthorName = 'Wow Wow';
            const secondAuthorUid = 456;
            const secondAvatarId = 'green_summer';
            const secondAuthorName = 'Hip Hop';
            const firstBookmarks: SharedBookmark[] = [
                {
                    record_id: 'record-b-1',
                    title: '1',
                    uri: 'uri-1',
                    description: ''
                }
            ];

            const {data: [firstSeed, thirdSeed]} = await dbClient.executeWriteCallback(async (pgClient) => {
                // create lists
                // tslint:disable:no-unused-variable
                const {rows: [{seed: firstSeed}, _, {seed: thirdSeed}]} = await pgClient.query(`INSERT INTO lists
                    (uid, id, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                    ($1, $3, 'record-1', 1, '2021-01-01 12:00:00+00', 'shared', $6, $8),
                    ($1, $4, 'record-2', 2, '2021-01-02', 'closed', '[]', '{}'),
                    ($2, $5, 'record-3', 3, '2021-01-03 12:00:00+00', 'deleted', $7, $9)
                    RETURNING seed`,
                    [
                        firstAuthorUid,
                        secondAuthorUid,
                        firstInternalId,
                        secondInternalId,
                        thirdInternalId,
                        JSON.stringify(firstBookmarks),
                        JSON.stringify([]),
                        {title: 'First list', bookmarks_count: firstBookmarks.length, icon: 'rubric:color'},
                        {title: 'Third list', bookmarks_count: 0}
                    ]
                );

                const listIds = (await pgClient.query('SELECT id FROM lists ORDER BY record_id'))
                    .rows.map((row) => row.id);

                // subscribe users on list
                await pgClient.query(`INSERT INTO subscriptions
                    (list_id, uid) VALUES
                    ($1, 1),
                    ($3, 1),
                    ($1, 2),
                    ($2, 2),
                    ($3, 3)`,
                    [
                        listIds[0],
                        listIds[1],
                        listIds[2]
                    ]
                );

                return [firstSeed, thirdSeed];
            });

            const firstPublicId = encodePublicId(`${firstInternalId}`, firstSeed);
            const thirdPublicId = encodePublicId(`${thirdInternalId}`, thirdSeed);

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
                            uid: {value: firstAuthorUid}
                        },
                        {
                            display_name: {
                                public_name: secondAuthorName,
                                avatar: {default: secondAvatarId}
                            },
                            uid: {value: secondAuthorUid}
                        }
                    ]
                });

            const res = await client.get(url, {
                headers: user1Headers
            });

            const expected = [
                {
                    public_id: firstPublicId,
                    revision: 1,
                    timestamp: new Date('2021-01-01T12:00:00.000Z').valueOf(),
                    status: 'shared',
                    title: 'First list',
                    author: firstAuthorName,
                    avatar_url: getAvatarUrl(firstAvatarId),
                    bookmarks_count: 1,
                    icon: 'rubric:color'
                },
                {
                    public_id: thirdPublicId,
                    revision: 3,
                    timestamp: new Date('2021-01-03T12:00:00.000Z').valueOf(),
                    status: 'deleted',
                    title: 'Third list',
                    bookmarks_count: 0
                }
            ];

            const actual = JSON.parse(res.body);

            sortByPublicId(actual);
            sortByPublicId(expected);

            expect(nockBlackbox.isDone()).to.be.true;
            expect(res.statusCode).to.equal(200);
            expect(actual).to.deep.equal(expected);
        });

        it('should return lists with all statuses', async () => {
            const firstInternalId = random(1, 2000);
            const secondInternalId = firstInternalId + 1;
            const thirdInternalId = secondInternalId + 1;
            const authorName = 'Bookmark Bookmarkovich';
            const avatarId = 'blue_spring';
            const authorUid = 123;
            const icon = 'rubric:color';

            const {data: storedSeeds} = await dbClient.executeWriteCallback(async (pgClient) => {
                // create lists
                const {rows: [{seed: firstSeed}, {seed: secondSeed}, {seed: thirdSeed}]}
                    = await pgClient.query(`INSERT INTO lists
                        (uid, id, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                        ($1, $2, 'record-1', 1,  '2021-01-01 12:00:00+00', 'shared', '[]', $5),
                        ($1, $3, 'record-2', 2,  '2021-01-02 12:00:00+00', 'closed', '[]', $6),
                        ($1, $4, 'record-3', 3, '2021-01-03 12:00:00+00', 'deleted', '[]', $7)
                        RETURNING seed`,
                        [
                            authorUid,
                            firstInternalId,
                            secondInternalId,
                            thirdInternalId,
                            {title: 'First list', icon, bookmarks_count: 0},
                            {title: 'Second list', bookmarks_count: 0},
                            {title: 'Third list', bookmarks_count: 0}
                        ]
                    );

                const listIds = (await pgClient.query('SELECT id FROM lists ORDER BY record_id'))
                    .rows.map((row) => row.id);

                // subscribe users on list
                await pgClient.query(`INSERT INTO subscriptions
                    (list_id, uid) VALUES
                    ($1, 1),
                    ($2, 1),
                    ($3, 1)`,
                    [...listIds]
                );

                return [firstSeed, secondSeed, thirdSeed];
            });

            const firstPublicId = encodePublicId(`${firstInternalId}`, storedSeeds[0]);
            const secondPublicId = encodePublicId(`${secondInternalId}`, storedSeeds[1]);
            const thirdPublicId = encodePublicId(`${thirdInternalId}`, storedSeeds[2]);

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
                            uid: {value: authorUid}
                        }
                    ]
                });

            const res = await client.get(url, {
                headers: user1Headers
            });

            const avatarUrl = getAvatarUrl(avatarId);

            const actual = JSON.parse(res.body);
            const expected = [
                {
                    public_id: firstPublicId,
                    revision: 1,
                    timestamp: new Date('2021-01-01T12:00:00.000Z').valueOf(),
                    status: 'shared',
                    title: 'First list',
                    author: authorName,
                    avatar_url: avatarUrl,
                    bookmarks_count: 0,
                    icon
                },
                {
                    public_id: secondPublicId,
                    revision: 2,
                    timestamp: new Date('2021-01-02T12:00:00.000Z').valueOf(),
                    status: 'closed',
                    title: 'Second list',
                    bookmarks_count: 0
                },
                {
                    public_id: thirdPublicId,
                    revision: 3,
                    timestamp: new Date('2021-01-03T12:00:00.000Z').valueOf(),
                    status: 'deleted',
                    title: 'Third list',
                    bookmarks_count: 0
                }
            ];

            sortByPublicId(actual);
            sortByPublicId(expected);

            expect(nockBlackbox.isDone()).to.be.true;
            expect(res.statusCode).to.equal(200);
            expect(actual).to.deep.equal(expected);
        });

        it('should return the default values in title and bookmarks_count ' +
            'when these fields are missing in the attributes', async () => {
            const internalId = random(1, 2000);
            const authorUid = 123;

            const {data: [firstSeed]} = await dbClient.executeWriteCallback(async (pgClient) => {
                const {rows: [{seed: firstSeed}]} = await pgClient.query(`INSERT INTO lists
                    (uid, id, record_id, revision, timestamp, status, bookmarks, attributes) VALUES
                    ($1, $2, 'record-1', 1, '2021-01-01 12:00:00+00', 'shared', '[]', $3)
                    RETURNING seed`,
                    [
                        authorUid,
                        internalId,
                        {}
                    ]
                );

                const listIds = (await pgClient.query('SELECT id FROM lists ORDER BY record_id'))
                    .rows.map((row) => row.id);

                // subscribe users on list
                await pgClient.query(`INSERT INTO subscriptions
                    (list_id, uid) VALUES
                    ($1, 1)`,
                    [listIds[0]]
                );

                return [firstSeed];
            });

            const publicId = encodePublicId(`${internalId}`, firstSeed);

            const nockBlackbox = nock(intHosts.blackbox)
                .get('/')
                .query(true)
                .times(1)
                .reply(200, {
                    users: [
                        {
                            display_name: {
                                public_name: 'Vasya',
                                avatar: {default: '0/0-0'}
                            },
                            uid: {value: authorUid}
                        }
                    ]
                });

            const res = await client.get(url, {
                headers: user1Headers
            });

            const expected = [
                {
                    public_id: publicId,
                    revision: 1,
                    timestamp: new Date('2021-01-01T12:00:00.000Z').valueOf(),
                    status: 'shared',
                    title: '',
                    author: 'Vasya',
                    avatar_url: getAvatarUrl('0/0-0'),
                    bookmarks_count: 0
                }
            ];

            const actual = JSON.parse(res.body);

            expect(nockBlackbox.isDone()).to.be.true;
            expect(res.statusCode).to.equal(200);
            expect(actual).to.deep.equal(expected);
        });
    });
});
