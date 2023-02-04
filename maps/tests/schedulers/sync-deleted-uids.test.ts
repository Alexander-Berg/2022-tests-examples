import {execFileSync} from 'child_process';
import {expect} from 'chai';
import {v4 as uuid} from 'uuid';
import {TestDb} from 'tests/test-db';
import {masterConnectionOptions} from 'tests/test-config';
import * as yt from './yt';

function createMap(
    id: number,
    params: {date?: string; deleted?: boolean} = {}
) {
    const date = params.date ?? '2019-01-01';
    const deleted = params.deleted ?? false;
    return {
        id,
        sid: `sid-${id}`,
        time_created: date,
        time_updated: date,
        revision: '0',
        deleted: deleted,
        properties: {},
        options: {},
        state: {}
    };
}

describe('deleted uids synchronization', () => {
    const db = new TestDb();
    const ytTmpDir = `//tmp/constructor-int_test_${uuid()}`;
    const userDataDir = `${ytTmpDir}/userdata`;

    function syncDeletedUids(): void {
        const {host, port, database, user, password} = masterConnectionOptions;
        execFileSync(
            'schedulers/sync-deleted-uids/sync-deleted-uids',
            [
                '--yt-tmp-dir', ytTmpDir,
                '--user-data-dir', userDataDir
            ],
            {
                env: {
                    PGHOST: host,
                    PGPORT: port?.toString(),
                    PGDATABASE: database,
                    PGUSER: user,
                    PGPASSWORD: password,
                    PGSSLMODE: 'allow',
                    YT_TOKEN: process.env.YT_TOKEN,
                    MAX_ALLOWED_DELETED_USERS: '10'
                },
                stdio: 'pipe'
            }
        );
    }

    function createUserDataTable(tableName: string) {
        const tablePath = `${userDataDir}/${tableName}`;
        yt.createTable(
            tablePath,
            // Attributes must be in YSON format.
            '{schema=[{name=uid;type=string}];}'
        );
        return tablePath;
    }

    after(async () => {
        await db.clean();

        yt.remove(ytTmpDir);
    });

    beforeEach(async () => {
        await db.clean();

        yt.remove(userDataDir);
    });

    describe('when passport user data table is empty', () => {
        it('should throw an error if the number of users to be deleted is greater than a threshold', async () => {
            createUserDataTable('2019-01-05');

            const addMaps = [];
            const addUsers = [];

            const threshold = 10; // MAX_ALLOWED_DELETED_USERS value
            for (let i = 0; i < threshold + 1; ++i) {
                addMaps.push(createMap(i + 1));
                addUsers.push({
                    map_id: i + 1,
                    uid: i + 1,
                    role: 'administrator'
                });
            }

            await db.loadFixtures([
                {
                    table: 'maps',
                    rows: addMaps
                },
                {
                    table: 'maps_users',
                    rows: addUsers
                }
            ]);

            expect(syncDeletedUids).to.throw(/Got too many uids to delete/);

            const maps = await db.query('SELECT id, deleted FROM maps ORDER BY id');
            expect(maps.rowCount).to.equal(11, 'Invalid number of maps');
            const users = await db.query('SELECT uid, map_id FROM maps_users ORDER BY uid');
            expect(users.rowCount).to.equal(11, 'Invalid number of users');
        });
    });

    it('should remove maps for accounts that do not appear in user data table', async () => {
        const userDataTable = createUserDataTable('2019-01-05');

        // For more info https://wiki.yandex-team.ru/passport/deleted_accounts/#asinxronnyjjpodxod-yt
        yt.writeTable(userDataTable, [
            {uid: '1'},
            {uid: '2'},
            {uid: '3'},
            {uid: '5'}
        ]);

        await db.loadFixtures([
            {
                table: 'maps',
                rows: [
                    createMap(1),
                    createMap(2),
                    createMap(3),
                    createMap(4),
                    createMap(5)
                ]
            },
            {
                table: 'maps_users',
                rows: [
                    {
                        uid: 1,
                        map_id: 1,
                        role: 'administrator'
                    },
                    {
                        uid: 2,
                        map_id: 2,
                        role: 'administrator'
                    },
                    {
                        uid: 3,
                        map_id: 3,
                        role: 'administrator'
                    },
                    {
                        uid: 4,
                        map_id: 4,
                        role: 'administrator'
                    },
                    {
                        uid: 5,
                        map_id: 5,
                        role: 'administrator'
                    }
                ]
            }
        ]);

        syncDeletedUids();

        const maps = await db.query('SELECT id, deleted FROM maps ORDER BY id');
        expect(maps.rowCount).to.equal(4, 'Invalid number of maps');
        expect(maps.rows).to.deep.equal([
            {
                id: '1',
                deleted: false
            },
            {
                id: '2',
                deleted: false
            },
            {
                id: '3',
                deleted: false
            },
            {
                id: '5',
                deleted: false
            }
        ]);

        const users = await db.query('SELECT uid, map_id FROM maps_users ORDER BY uid');
        expect(users.rowCount).to.equal(4, 'Invalid number of users');
        expect(users.rows).to.deep.equal([
            {
                uid: '1',
                map_id: '1'
            },
            {
                uid: '2',
                map_id: '2'
            },
            {
                uid: '3',
                map_id: '3'
            },
            {
                uid: '5',
                map_id: '5'
            }
        ]);
    });

    it('should remove maps which were marked as deleted', async () => {
        const userDataTable = createUserDataTable('2019-01-05');

        yt.writeTable(userDataTable, [
            {uid: '999'}
        ]);

        await db.loadFixtures([
            {
                table: 'maps',
                rows: [
                    createMap(1),
                    createMap(2, {deleted: true})
                ]
            },
            {
                table: 'maps_users',
                rows: [
                    {
                        uid: 1,
                        map_id: 1,
                        role: 'administrator'
                    },
                    {
                        uid: 1,
                        map_id: 2,
                        role: 'administrator'
                    }
                ]
            }
        ]);

        syncDeletedUids();

        const maps = await db.query('SELECT * FROM maps');
        expect(maps.rowCount).to.equal(0, 'Invalid number of maps');

        const users = await db.query('SELECT * FROM maps_users');
        expect(users.rowCount).to.equal(0, 'Invalid number of users');
    });

    // In constructor database accounts with uid = 0 are considered as anonymous.
    it('should ignore anonymous accounts', async () => {
        const userDataTable = createUserDataTable('2019-01-05');

        yt.writeTable(userDataTable, [
            {uid: '999'}
        ]);

        await db.loadFixtures([
            {
                table: 'maps',
                rows: [
                    createMap(1)
                ]
            },
            {
                table: 'maps_users',
                rows: [
                    {
                        uid: 0,
                        map_id: 1,
                        role: 'administrator'
                    }
                ]
            }
        ]);

        syncDeletedUids();

        const maps = await db.query('SELECT * FROM maps');
        expect(maps.rowCount).to.equal(1, 'Invalid number of maps');

        const users = await db.query('SELECT * FROM maps_users');
        expect(users.rowCount).to.equal(1, 'Invalid number of users');
    });

    it('should write deleted maps to deleted_maps_log table', async () => {
        const userDataTable = createUserDataTable('2019-01-05');

        yt.writeTable(userDataTable, [
            {uid: '2'}
        ]);

        await db.loadFixtures([
            {
                table: 'maps',
                rows: [
                    createMap(1),
                    createMap(2),
                    createMap(3),
                    createMap(4)
                ]
            },
            {
                table: 'maps_users',
                rows: [
                    {
                        uid: 1,
                        map_id: 1,
                        role: 'administrator'
                    },
                    {
                        uid: 1,
                        map_id: 2,
                        role: 'administrator'
                    },
                    {
                        uid: 2,
                        map_id: 3,
                        role: 'administrator'
                    },
                    {
                        uid: 3,
                        map_id: 4,
                        role: 'administrator'
                    }
                ]
            }
        ]);

        syncDeletedUids();

        const deletedMaps = await db.query('SELECT sid, uid FROM deleted_maps_log ORDER BY sid');
        expect(deletedMaps.rowCount).to.equal(3, 'Invalid number of rows in deleted_maps_log table');
        expect(deletedMaps.rows).to.deep.equal([
            {
                sid: 'sid-1',
                uid: '1'
            },
            {
                sid: 'sid-2',
                uid: '1'
            },
            {
                sid: 'sid-4',
                uid: '3'
            }
        ]);
    });

    it('should merge with user data table for the latest available date', async () => {
        const tables = [
            createUserDataTable('2019-01-02'),
            createUserDataTable('2019-01-03'),
            createUserDataTable('2019-01-04'),
            createUserDataTable('2019-01-05'),
            createUserDataTable('tmp')
        ];
        const userDataTable = tables[3];

        yt.writeTable(userDataTable, [
            {uid: '2'}
        ]);

        await db.loadFixtures([
            {
                table: 'maps',
                rows: [
                    createMap(1),
                    createMap(2),
                    createMap(3)
                ]
            },
            {
                table: 'maps_users',
                rows: [
                    {
                        uid: 1,
                        map_id: 1,
                        role: 'administrator'
                    },
                    {
                        uid: 2,
                        map_id: 2,
                        role: 'administrator'
                    },
                    {
                        uid: 3,
                        map_id: 3,
                        role: 'administrator'
                    }
                ]
            }
        ]);

        syncDeletedUids();

        const maps = await db.query('SELECT id FROM maps ORDER BY id');
        expect(maps.rowCount).to.equal(1, 'Invalid number of maps');
        expect(maps.rows).to.deep.equal([
            {id: '2'}
        ]);

        const users = await db.query('SELECT uid, map_id FROM maps_users ORDER BY uid');
        expect(users.rowCount).to.equal(1, 'Invalid number of users');
        expect(users.rows).to.deep.equal([
            {
                uid: '2',
                map_id: '2'
            }
        ]);
    });

    describe('check that we do not delete new users', () => {
        it('should not delete users that created all of their maps recently', async () => {
            const userDataTable = createUserDataTable('2019-01-05');

            yt.writeTable(userDataTable, [
                {uid: '999'}
            ]);

            await db.loadFixtures([
                {
                    table: 'maps',
                    rows: [
                        // maps of user 1 should not be deleted
                        createMap(1, {date: '2019-01-04 00:00:00'}),
                        createMap(2, {date: '2019-01-05 00:00:00'}),
                        createMap(3, {date: '2019-01-06 00:00:00'}),

                        // maps of user 2 should be deleted
                        createMap(4, {date: '2019-01-01 00:00:00'}),
                        createMap(5, {date: '2019-01-06 00:00:00'}),

                        // maps of user 3 should be deleted
                        createMap(6, {date: '2019-01-03 23:59:59'})
                    ]
                },
                {
                    table: 'maps_users',
                    rows: [
                        {
                            uid: 1,
                            map_id: 1,
                            role: 'administrator'
                        },
                        {
                            uid: 1,
                            map_id: 2,
                            role: 'administrator'
                        },
                        {
                            uid: 1,
                            map_id: 3,
                            role: 'administrator'
                        },
                        {
                            uid: 2,
                            map_id: 4,
                            role: 'administrator'
                        },
                        {
                            uid: 2,
                            map_id: 5,
                            role: 'administrator'
                        },
                        {
                            uid: 3,
                            map_id: 6,
                            role: 'administrator'
                        }
                    ]
                }
            ]);

            syncDeletedUids();

            const maps = await db.query('SELECT id FROM maps ORDER BY id');
            expect(maps.rowCount).to.equal(3, 'Invalid number of maps');
            expect(maps.rows).to.deep.equal([
                {id: '1'},
                {id: '2'},
                {id: '3'}
            ]);

            const users = await db.query('SELECT uid, map_id FROM maps_users ORDER BY map_id');
            expect(users.rowCount).to.equal(3, 'Invalid number of users');
            expect(users.rows).to.deep.equal([
                {
                    uid: '1',
                    map_id: '1'
                },
                {
                    uid: '1',
                    map_id: '2'
                },
                {
                    uid: '1',
                    map_id: '3'
                }
            ]);
        });
    });
});
