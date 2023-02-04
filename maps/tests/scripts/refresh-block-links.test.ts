import {knex, executeReadQuery} from 'app/lib/db';
import {TestDb} from 'tests/test-db';
import {fixtures} from 'tests/fixtures/db';
import {Table, Branch, PageBlockType} from 'app/types/consts';
import {Link, Schema} from 'app/types/db/pages-content';
import {executeScript} from 'schedulers/scripts/refresh-block-links';

const SCHEDULER = 'refresh-block-links';

interface LinkedPagesIds {
    publicPageIds: number[];
    draftPageIds: number[];
}

async function getLinkedPagesIds(pageId: number): Promise<LinkedPagesIds> {
    const query = knex('*').from(Table.PAGES_CONTENT).where({
        page_id: pageId
    });

    const {rows} = await executeReadQuery<Schema>(query.toString());

    const publicPage = rows.find((row) => row.branch === Branch.PUBLIC);
    const draftPage = rows.find((row) => row.branch === Branch.DRAFT);

    const publicBlockLink = publicPage?.blocks.find((block) => block.type === PageBlockType.LINKS) as Link;
    const draftBlockLink = draftPage?.blocks.find((block) => block.type === PageBlockType.LINKS) as Link;

    expect(publicBlockLink).not.toBeUndefined();
    expect(draftBlockLink).not.toBeUndefined();

    return {
        publicPageIds: publicBlockLink.pages.map((page) => page.id),
        draftPageIds: draftBlockLink.pages.map((page) => page.id)
    };
}

describe(`scripts/${SCHEDULER}`, () => {
    const testDb = new TestDb();

    beforeEach(async () => {
        await testDb.clean();
        await testDb.loadFixtures(fixtures);
    });

    it('should update block links in page', async () => {
        const idsBeforeUpdate = await getLinkedPagesIds(2);

        await executeScript();

        const idsAfterUpdate = await getLinkedPagesIds(2);

        expect(idsAfterUpdate.draftPageIds.length).not.toBe(0);
        expect(idsAfterUpdate.publicPageIds.length).not.toBe(0);

        expect(idsBeforeUpdate.draftPageIds.join(',')).not.toEqual(idsAfterUpdate.draftPageIds.join(','));
        expect(idsBeforeUpdate.publicPageIds.join(',')).not.toEqual(idsAfterUpdate.publicPageIds.join(','));
    });
});
