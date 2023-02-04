import {knex, executeReadQuery} from 'app/lib/db';
import nock from 'nock';
import {promises} from 'fs';
import {intHostConfigLoader} from 'app/lib/host-loader';
import {executeScript} from 'schedulers/scripts/refresh-pages-content';
import {Table, Branch, PageBlockType} from 'app/types/consts';
import {TestDb} from 'tests/test-db';
import {fixtures} from 'tests/fixtures/db';
import {Organization, Promo, Schema} from 'app/types/db/pages-content';

const SCHEDULER = 'refresh-pages-content';
const NOT_FOUND_OID = 'not-found-organization';
const SEARCH_DEFAULT_QUERY = {
    origin: 'discovery',
    lang: 'ru_RU',
    type: 'biz',
    format: 'json',
    snippets: 'businessrating/1.x,masstransit/1.x,photos/2.x,cluster_permalinks,promo_mastercard/1.x:mastercardoffers',
    ms: 'pb',
    ull: '37.52737,55.598274'
};

async function getPageOrganizationBlocks(pageId: number): Promise<Organization[]> {
    const query = knex('*').from(Table.PAGES_CONTENT).where({
        page_id: pageId,
        branch: Branch.PUBLIC
    });

    const {rows} = await executeReadQuery<Schema>(query.toString());
    const blocks = rows[0].blocks;

    return blocks.filter((block): block is Organization => block.type === PageBlockType.ORGANIZATION);
}

async function getPagePromoBlocks(pageId: number): Promise<Promo[]> {
    const query = knex('*').from(Table.PAGES_CONTENT).where({
        page_id: pageId,
        branch: Branch.PUBLIC
    });

    const {rows} = await executeReadQuery<Schema>(query.toString());
    const blocks = rows[0].blocks;

    return blocks.filter((block): block is Promo => block.type === PageBlockType.PROMO);
}

async function getPageArchivedBlocks(pageId: number): Promise<Organization[]> {
    const query = knex
        .select({
            archivedBlock: 'archived_blocks'
        })
        .from(Table.PAGES_CONTENT)
        .where({
            page_id: pageId,
            branch: Branch.PUBLIC
        });

    const {rows} = await executeReadQuery<{archivedBlock: Organization[]}>(query.toString());
    return rows[0].archivedBlock;
}

describe(`scripts/${SCHEDULER}`, () => {
    const testDb = new TestDb();
    let searchHost: string;

    beforeAll(async () => {
        const hosts = await intHostConfigLoader.get();
        searchHost = hosts.search;

        nock.disableNetConnect();
    });

    afterAll(() => {
        nock.enableNetConnect();
    });

    beforeEach(async () => {
        await testDb.clean();
        await testDb.loadFixtures(fixtures);

        const oids = ['1197453631', NOT_FOUND_OID, '18354308579', '1393234578', '1106890691', '1123659643'];

        for (let i = 0; i < oids.length; i++) {
            const oid = oids[i];

            nock(searchHost)
                .get('/yandsearch')
                .query({
                    ...SEARCH_DEFAULT_QUERY,
                    business_oid: oid
                })
                // eslint-disable-next-line no-await-in-loop
                .reply(200, await promises.readFile(`src/tests/fixtures/backend/organizations/${oid}.protobuf`));
        }
    });

    afterEach(() => {
        nock.cleanAll();
    });

    it('should update oganization blocks if it was changed in Яндекс.Справочник', async () => {
        const blocksBefore = await getPageOrganizationBlocks(1);
        await executeScript();
        const blocksAfter = await getPageOrganizationBlocks(1);

        expect(blocksBefore.length).toEqual(blocksAfter.length);

        blocksBefore.forEach((block, i) => {
            const blockAfter = blocksAfter[i];

            // Check on "rating" field
            expect(block.rating.ratings).not.toEqual(blockAfter.rating.ratings);
            expect(block.rating.reviews).not.toEqual(blockAfter.rating.reviews);

            // Check on whole object
            expect(block).not.toEqual(blockAfter);
        });
    });

    it('should update archived blocks if it was changed in Яндекс.Справочник', async () => {
        const blocksBefore = await getPageArchivedBlocks(3);
        await executeScript();
        const blocksAfter = await getPageArchivedBlocks(3);

        expect(blocksBefore.length).toEqual(blocksAfter.length);

        blocksBefore.forEach((block, i) => {
            const blockAfter = blocksAfter[i];

            // Check on "rating" field
            expect(block.rating.ratings).not.toEqual(blockAfter.rating.ratings);
            expect(block.rating.reviews).not.toEqual(blockAfter.rating.reviews);

            // Check on whole object
            expect(block).not.toEqual(blockAfter);
        });
    });

    it('should update promo blocks', async () => {
        const blocksBefore = await getPagePromoBlocks(4);
        await executeScript();
        const blocksAfter = await getPagePromoBlocks(4);

        expect(blocksBefore.length).toEqual(blocksAfter.length);

        blocksBefore.forEach((block, i) => {
            const blockAfter = blocksAfter[i];

            expect(block.title.split(' ')[0]).toEqual('Возьми');
            expect(blockAfter.title.split(' ')[0]).toEqual('Возьмите');

            expect(block.title).not.toEqual(blockAfter.title);
        });
    });
});
