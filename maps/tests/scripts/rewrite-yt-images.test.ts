import nock from 'nock';
import Ajv from 'ajv';
import {URL} from 'url';
import {TestDb} from 'tests/test-db';
import {fixtures} from 'tests/fixtures/db';
import {config} from 'app/config';
import {executeScript, getYtData} from 'schedulers/scripts/rewrite-yt-images';
import {prepareYtData} from 'schedulers/scripts/rewrite-yt-images/helpers/prepare-yt-data';
import {rows as pagesContent} from 'tests/fixtures/db/pages-content';
import {Branch, PageBlockType} from 'app/types/consts';
import {Organization} from 'app/types/db/pages-content';
import {logger} from 'app/lib/logger';
import RESPONSE_SCHEMA from 'tests/scripts/ajv-schemas/rewrite-yt-images.json';

const SCHEDULER = 'rewrite-yt-images';
const YT_PATH_ORIGIN = 'yt-path';
const YT_PATH = `http://${YT_PATH_ORIGIN}/api/v3/write_table`;

const ajv = new Ajv();

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
                path: `${config['yt.path.homeDiscoveryInt']}/orgs-photos`,
                encode_utf8: false
            })
            .reply(200);

        nock(ytPath.origin)
            .put(ytPath.pathname)
            .query({
                path: `<append=true>${config['yt.path.homeDiscoveryInt']}/orgs-photos`,
                encode_utf8: false
            })
            .reply(200);
    });

    afterEach(() => {
        nock.cleanAll();
    });

    it('should write to YT', async () => {
        await executeScript();
        expect(nock.pendingMocks().length).toBe(0);
    });

    it('should return formatted data', async () => {
        const pages = pagesContent
            .filter((page) => page.branch === Branch.PUBLIC)
            .map((page) => {
                const orgBlocks = page.blocks.filter(
                    (block): block is Organization => block.type === PageBlockType.ORGANIZATION
                );
                const oids = orgBlocks.map((block) => block.oid);
                const images = orgBlocks.map((block) => block.images);

                return {
                    pageId: page.page_id,
                    branch: page.branch,
                    oids,
                    images
                };
            });

        expect(await getYtData()).toEqual(prepareYtData(pages));
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
