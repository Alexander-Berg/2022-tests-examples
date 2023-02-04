import nock from 'nock';
import {knex, executeReadQuery} from 'app/lib/db';
import {TestDb} from 'tests/test-db';
import {fixtures} from 'tests/fixtures/db';
import {executeScript} from 'schedulers/scripts/refresh-tracker-info';
import {pagesTracker} from 'tests/fixtures/db/pages-tracker';
import {Branch, Table} from 'app/types/consts';
import {config} from 'app/config';

const SCHEDULER = 'refresh-tracker-info';
const ST_API = 'https://st-api.yandex-team.ru/v2';
const TEST_ISSUE_ID = 'test_issue_id';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
async function getPageTracker(pageId: number): Promise<any> {
    const query = knex('*').from(Table.PAGES_TRACKER).where({
        page_id: pageId
    });

    const {rows} = await executeReadQuery(query.toString());
    return rows[0];
}

function nockCreateIssue(): void {
    nock(ST_API)
        .post('/issues', {
            queue: config['startrek.queue'],
            components: [config['startrek.component']],
            parent: config['startrek.parentId'],
            summary: 'Проверить подборку: https://discovery-admin.tst.c.maps.yandex-team.ru/pages/edit?id=1',
            description: 'Проверить подборку: https://discovery-admin.tst.c.maps.yandex-team.ru/pages/edit?id=1'
        })
        .reply(200, {
            id: TEST_ISSUE_ID
        });

    nock(ST_API)
        .post(`/issues/${TEST_ISSUE_ID}/comments`, {
            text: 'Пермалинки для проверки: 1197453631'
        })
        .query({
            isAddToFollowers: false
        })
        .reply(200);
}

function nockReopenIssue(issueId: string): void {
    nock(ST_API)
        .get(`/issues/${issueId}`)
        .reply(200, {
            status: {
                key: 'closed'
            }
        });

    nock(ST_API)
        .post(`/issues/${issueId}/comments`, {
            text: 'Пермалинки для проверки: 1393234578'
        })
        .query({
            isAddToFollowers: false
        })
        .reply(200);

    nock(ST_API).post(`/issues/${issueId}/transitions/reopen/_execute`).reply(200);
}

function nockCloseIssue(issueId: string): void {
    nock(ST_API)
        .get(`/issues/${issueId}`)
        .reply(200, {
            status: {
                key: 'open'
            }
        });

    nock(ST_API).post(`/issues/${issueId}/transitions/close/_execute`).reply(200);
}

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

        nockCreateIssue();

        pagesTracker.rows
            .filter((tracker) => tracker.branch === Branch.PUBLIC)
            .forEach((tracker) => {
                const issueId = `tracker_${tracker.page_id}_public`;

                if (tracker.page_id === 3) {
                    return nockReopenIssue(issueId);
                }

                nockCloseIssue(issueId);
            });
    });

    afterEach(() => {
        nock.cleanAll();
    });

    it('should make all requests to st-api', async () => {
        const pageTrackerBefore = await getPageTracker(1);
        expect(pageTrackerBefore).toBeUndefined();

        await executeScript();
        expect(nock.pendingMocks().length).toBe(0);

        const pageTrackerAfter = await getPageTracker(1);
        expect(pageTrackerAfter).toEqual({
            page_id: 1,
            branch: Branch.PUBLIC,
            info: {
                id: TEST_ISSUE_ID
            }
        });
    });
});
