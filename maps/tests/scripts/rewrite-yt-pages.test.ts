import nock from 'nock';
import {knex, executeReadQuery} from 'app/lib/db';
import Ajv from 'ajv';
import * as dateFns from 'date-fns';
import {URL} from 'url';
import {TestDb} from 'tests/test-db';
import {fixtures} from 'tests/fixtures/db';
import {config} from 'app/config';
import {executeScript, getYtData} from 'schedulers/scripts/rewrite-yt-pages';
import {prepareYtData} from 'schedulers/scripts/rewrite-yt-pages/helpers/prepare-yt-data';
import {rows as pagesContent} from 'tests/fixtures/db/pages-content';
import {rows as partners} from 'tests/fixtures/db/partnerts';
import {rubricsPages} from 'tests/fixtures/db/rubrics-pages';
import {rubrics} from 'tests/fixtures/db/rubrics';
import {Page} from 'schedulers/scripts/rewrite-yt-pages/helpers/get-pages';
import {Branch, Table} from 'app/types/consts';
import {logger} from 'app/lib/logger';
import RESPONSE_SCHEMA from 'tests/scripts/ajv-schemas/rewrite-yt-pages.json';

const SCHEDULER = 'rewrite-yt-pages';
const YT_PATH_ORIGIN = 'yt-path';
const YT_PATH = `http://${YT_PATH_ORIGIN}/api/v3/write_table`;

const ajv = new Ajv();

async function getPageUpdateTime(id: number): Promise<string> {
    const query = knex
        .select({
            updatedTime: 'updated_time'
        })
        .from(Table.PAGES_CONTENT)
        .where({
            page_id: id,
            branch: Branch.PUBLIC
        });

    const {rows} = await executeReadQuery<{updatedTime: string}>(query.toString());
    return rows[0].updatedTime;
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

        const ytHostList = new URL(config['hosts.ytHostList']);
        nock(ytHostList.origin).get(ytHostList.pathname).reply(200, [YT_PATH_ORIGIN]);

        const ytPath = new URL(YT_PATH);
        nock(ytPath.origin)
            .put(ytPath.pathname)
            .query({
                path: `${config['yt.path.homeGeoSearchCollections']}/expert_collections`,
                encode_utf8: false
            })
            .reply(200);

        nock(ytPath.origin)
            .put(ytPath.pathname)
            .query({
                path: `<append=true>${config['yt.path.homeGeoSearchCollections']}/expert_collections`,
                encode_utf8: false
            })
            .reply(200);

        const date = new Date();
        date.setDate(date.getDate() - 1);
        const formattedDate = dateFns.format(date, 'YYYY-MM-DD');

        const ytCopy = new URL(config['hosts.ytCopy']);
        nock(ytCopy.origin)
            .post(ytCopy.pathname)
            .query({
                source_path: `${config['yt.path.homeGeoSearchCollections']}/expert_collections`,
                destination_path: [
                    config['yt.path.homeGeoSearchCollections'],
                    `/history/expert_collections/${formattedDate}`
                ].join(''),
                force: true
            })
            .reply(200);
    });

    afterEach(() => {
        nock.cleanAll();
    });

    it('should write to YT and update history', async () => {
        await executeScript();
        expect(nock.pendingMocks().length).toBe(0);
    });

    it('should return formatted data', async () => {
        const permalinksMap = rubricsPages.rows.reduce((result, rubricPage) => {
            const permalinks = rubrics.rows
                .filter((rubric) => rubric.id === rubricPage.rubric_id)
                .map((rubric) => rubric.permalink);

            const {branch, page_id: pageId} = rubricPage;
            const key = `${branch}${pageId}`;

            const existedPermalinks = result.get(key);
            if (existedPermalinks) {
                result.set(key, [...existedPermalinks, ...permalinks]);
            } else {
                result.set(key, permalinks);
            }

            return result;
        }, new Map<string, string[]>());

        const pages = pagesContent
            .filter((page) => page.branch === Branch.PUBLIC && !page.info.properties.meta.directAccess)
            .map(async (page): Promise<Page> => {
                const partner = partners[page.partner_id - 1];
                const permalinks = permalinksMap.get(`${Branch.PUBLIC}${page.page_id}`);

                return {
                    info: page.info,
                    blocks: page.blocks,
                    partnerData: partner.data,
                    partnerLinks: partner.partner_links,
                    updatedTime: await getPageUpdateTime(page.page_id),
                    partnerId: page.partner_id,
                    partnerName: partner.data.title,
                    partnerIcon: partner.data.icon,
                    rubricPermalinks: permalinks || []
                };
            });

        expect(await getYtData()).toEqual(await prepareYtData(await Promise.all(pages)));
    });

    it('should return correct schema', async () => {
        const ytData = await getYtData();
        const valid = ajv.validate(RESPONSE_SCHEMA, ytData);
        if (!valid) {
            logger.error(ajv.errorsText());
        }

        expect(valid).toBeTruthy();
    });
});
