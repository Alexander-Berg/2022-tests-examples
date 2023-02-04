import nock from 'nock';
import {knex, executeReadQuery} from 'app/lib/db';
import _ from 'lodash';
import {URL} from 'url';
import {config} from 'app/config';
import {TestDb} from 'tests/test-db';
import {fixtures} from 'tests/fixtures/db';
import {Table} from 'app/types/consts';
import {executeScript} from 'schedulers/scripts/import-device-segments';
import ytDeviceSegments from 'tests/fixtures/backend/yt/device-segments';
import {Schema as Segment} from 'app/types/db/segments';

const SCHEDULER = 'import-device-segments';
const YT_PATH_ORIGIN = 'yt-path';
const YT_PATH = `http://${YT_PATH_ORIGIN}/api/v3/read_table`;

interface Record {
    DeviceIDHash: string;
    segments: string[];
}

async function getDeviceSegments(): Promise<Record[]> {
    const query = knex.select().from(Table.DEVICES);
    const result = await executeReadQuery<Record>(query.toString());

    return result.rows;
}

async function getSegments(): Promise<Segment[]> {
    const query = knex.select().from(Table.SEGMENTS);
    const result = await executeReadQuery<Segment>(query.toString());

    return result.rows;
}

jest.mock('../../schedulers/scripts/import-device-segments/helpers/get-segments-from-nirvana');

describe(`scripts/${SCHEDULER}`, () => {
    const testDb = new TestDb();

    beforeAll(async () => {
        nock.disableNetConnect();
    });

    afterAll(() => {
        nock.enableNetConnect();
    });

    beforeEach(async () => {
        await testDb.clean();
        await testDb.loadFixtures(fixtures);

        const ytHostList = new URL(config['hosts.ytHostList']);
        nock(ytHostList.origin).get(ytHostList.pathname).reply(200, [YT_PATH_ORIGIN]);

        const ytPath = new URL(YT_PATH);
        nock(ytPath.origin)
            .get('/api/v3/read_table')
            .query({
                path: `${config['yt.path.homeDeviceSegments']}/devices`,
                encode_utf8: false
            })
            .reply(200, ytDeviceSegments);

        nock(ytPath.origin)
            .get('/api/v3/get')
            .query({
                path: `${config['yt.path.homeDeviceSegments']}/segments`
            })
            .reply(200, JSON.stringify({drivers: {segment: null}}));

        nock(ytPath.origin)
            .get('/api/v3/get')
            .query({
                path: `${config['yt.path.homeDeviceSegments']}/segments/drivers/segment`,
                'attributes[]': 'row_count'
            })
            .reply(200, JSON.stringify({$attributes: {row_count: 31337}}));
    });

    it('should add new and delete old device segments and update segments', async () => {
        const recordsBeforeUpdate = await getDeviceSegments();
        expect(recordsBeforeUpdate.length).toBe(2);
        expect(recordsBeforeUpdate.map((item) => _.omit(item, 'updated_time'))).toEqual([
            {
                DeviceIDHash: '7071321290416261339',
                segments: ['segment-1']
            },
            {
                DeviceIDHash: '2000457444287749023',
                segments: ['segment-1']
            }
        ]);

        await executeScript();

        const recordsAfterUpdate = await getDeviceSegments();
        expect(recordsAfterUpdate.length).toBe(3);
        expect(recordsAfterUpdate.map((item) => _.omit(item, 'updated_time'))).toEqual([
            {
                DeviceIDHash: '7071321290416261339',
                segments: ['test-1']
            },
            {
                DeviceIDHash: 'device-hash-3',
                segments: ['test-3']
            },
            {
                DeviceIDHash: 'device-hash-4',
                segments: ['test-4']
            }
        ]);

        const segments = await getSegments();
        expect(segments[2].title).toEqual('from nirvana');
        expect(segments[2].count).toEqual(31337);
    });
});
