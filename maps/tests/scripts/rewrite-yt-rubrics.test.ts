import nock from 'nock';
import {TestDb} from 'tests/test-db';
import {fixtures} from 'tests/fixtures/db';
import {executeScript} from 'schedulers/scripts/rewrite-yt-rubrics';
import {config} from 'app/config';
import {altayRubrics} from 'tests/fixtures/backend/yt/home-altay-rubrics';
import {getAllRubrics, Rubric} from 'schedulers/scripts/rewrite-yt-rubrics/helpers/get-all-rubrics';

const SCHEDULER = 'rewrite-yt-rubrics';
const RAW_ALTAY_RUBRICS = altayRubrics.map((rubric) => JSON.stringify(rubric)).join('\n');

async function getRubric(id: number): Promise<Rubric | undefined> {
    const rubrics = await getAllRubrics();
    return rubrics.find((rubric) => rubric.id === id);
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

        nock(config['hosts.ytProxy'])
            .get('/api/v3/get')
            .query({
                path: `${config['yt.path.homeAltayRubircs']}/@`
            })
            .reply(200, {
                row_count: altayRubrics.length
            });

        nock(config['hosts.ytProxy'])
            .get('/api/v3/read_table')
            .query({
                path: [
                    config['yt.path.homeAltayRubircs'],
                    '{id,permalink,rubric_class,publishing_status,names,homepage_class}[#0:#40]'
                ].join('')
            })
            .reply(200, RAW_ALTAY_RUBRICS);
    });

    afterEach(() => {
        nock.cleanAll();
    });

    it('should update rubric if permalinks are not equal', async () => {
        const rubricBefore = await getRubric(20269);
        expect(rubricBefore).not.toBeUndefined();

        await executeScript();

        const rubricAfter = await getRubric(20269);

        expect(rubricAfter).not.toBeUndefined();
        expect(rubricBefore).not.toEqual(rubricAfter);
    });

    it('should update rubric content if names are not equal', async () => {
        const rubricBefore = await getRubric(20273);
        expect(rubricBefore).not.toBeUndefined();

        await executeScript();

        const rubricAfter = await getRubric(20273);
        expect(rubricAfter).not.toBeUndefined();

        expect(rubricBefore).not.toEqual(rubricAfter);
    });

    it("should create rubric if it doesn't exist in database", async () => {
        const rubricBefore = await getRubric(20270);
        expect(rubricBefore).toBeUndefined();

        await executeScript();

        const rubricAfter = await getRubric(20270);
        expect(rubricAfter).not.toBeUndefined();
    });

    it("should remove rubric if it doesn't exist in YT altay rubrics", async () => {
        const rubricBefore = await getRubric(30065);
        expect(rubricBefore).not.toBeUndefined();

        await executeScript();

        const rubricAfter = await getRubric(30065);
        expect(rubricAfter).toBeUndefined();
    });
});
