import nock from 'nock';
import Ajv from 'ajv';
import {URL} from 'url';
import {TestDb} from 'tests/test-db';
import {fixtures} from 'tests/fixtures/db';
import {executeScript, getYtData} from 'schedulers/scripts/rewrite-yt-snippets';
import {prepareYtData} from 'schedulers/scripts/rewrite-yt-snippets/helpers/prepare-yt-data';
import {PublishedPage} from 'schedulers/scripts/rewrite-yt-snippets/helpers/get-published-pages';
import {config} from 'app/config';
import {rows as pagesContent} from 'tests/fixtures/db/pages-content';
import {rows as partners} from 'tests/fixtures/db/partnerts';
import {Branch, PageBlockType} from 'app/types/consts';
import {Organization} from 'app/types/db/pages-content';
import {logger} from 'app/lib/logger';
import RESPONSE_SCHEMA from 'tests/scripts/ajv-schemas/rewrite-yt-snippets.json';

const SCHEDULER = 'rewrite-yt-snippets';
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
                path: `${config['yt.path.homeGeoSearchCollections']}/discovery_snippets`,
                encode_utf8: false
            })
            .reply(200);

        nock(ytPath.origin)
            .put(ytPath.pathname)
            .query({
                path: `<append=true>${config['yt.path.homeGeoSearchCollections']}/discovery_snippets`,
                encode_utf8: false
            })
            .reply(200);
    });

    afterEach(() => {
        nock.cleanAll();
    });

    it('should write to YT', async () => {
        await executeScript();
    });

    it('should return formatted data', async () => {
        const pages = pagesContent
            .filter((page) => page.branch === Branch.PUBLIC && !page.info.properties.meta.directAccess)
            .map((page): PublishedPage => {
                const orgBlocks = page.blocks.filter(
                    (block): block is Organization => block.type === PageBlockType.ORGANIZATION
                );

                return {
                    id: page.page_id,
                    alias: page.info.alias,
                    type: page.info.type,
                    title: page.info.title,
                    schemaVersion: page.info.schemaVersion,
                    description: page.info.description,
                    placeNumber: page.info.placeNumber,
                    image: page.info.image,
                    icon: page.info.icon,
                    boundingBox: page.info.boundingBox,
                    copyrights: page.info.copyrights || null,
                    rubric: page.info.rubric.value,
                    permalinks: orgBlocks.map(({oid}) => oid),
                    orgDescription: orgBlocks.map(({description}) => description),
                    partner: partners[page.partner_id - 1].data
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
