import * as _ from 'lodash';
import * as fs from 'fs';
import * as glob from 'glob';
import got from 'got';

const version = require('../../../.qtools.json').registry.tag.replace(/-\d+$/, '');
const TESTING_URL = `https://front-jsapi.crowdtest.maps.yandex.ru/${version}/`;

const OAUTH_ENV_VAR = 'TESTPALM_OAUTH_TOKEN';
const OAUTH_TOKEN = process.env[OAUTH_ENV_VAR];
if (!OAUTH_TOKEN) {
    console.error(`
        Unable to interact with Testpalm REST API without OAuth key.
        Provide one as ${OAUTH_ENV_VAR} environment variable.
        If you don't have one, get it at:
        https://oauth.yandex-team.ru/authorize?response_type=token&client_id=6d967b191847496a8a7077e2e636142f
    `.replace(/^\s*/gm, ''));
    process.exit(1);
}

// Testpalm REST API docs https://testpalm-api.yandex-team.ru/.
// Testpalm wiki page https://wiki.yandex-team.ru/testpalm/.
const testpalmUrl = 'https://testpalm-api.yandex-team.ru';
const authGotOptions = {headers: {'authorization': `OAuth ${OAUTH_TOKEN}`}};
const testcasesGotOptions = {...authGotOptions, prefixUrl: `${testpalmUrl}/testcases/${process.env.TESTPALM_PROJECT_ID}`};
// "definition" in api = "attributes" in testcase data = "key" in UI.
const definitionGotOptions = {...authGotOptions, prefixUrl: `${testpalmUrl}/definition/${process.env.TESTPALM_PROJECT_ID}`};

const TAGS = <const>['Smoke'];
const COMPONENTS = <const>[
    'balloon', 'hint', 'behavior', 'clusterer', 'control', 'counters', 'CSP', 'data',
    'geocode','geolink', 'geolocation', 'geometry', 'geoobject', 'geoQuery', 'geoxml', 'hotspot', 'internal',
    'lang', 'layer', 'layout', 'map', 'multiRouter', 'objectManager', 'onerror', 'other', 'overlay', 'pane', 'panorama', 'poi', 'ready', 'load',
    'regions', 'route', 'search', 'suggest', 'template', 'traffic', 'util', 'vector', 'webGL', 'xhtml', 'mobile', 'smoke'
];

const includeFields: (keyof TestpalmTestcase)[] = [
    'id',
    'name',
    'description',
    'stepsExpects',
    'properties',
    'preconditions',
    'estimatedTime',
    'attributes'
];

type Tag = typeof TAGS[number];
type Component = typeof COMPONENTS[number];

interface Property {
    key: string;
    value: string;
}

interface TestpalmTestcaseInput {
    name: string;
    description: string;
    preconditions: string;
    properties: Property[];
    stepsExpects: TestpalmStep[];
    estimatedTime: number;
    attributes: Record<string, string[]>;
}

interface TestpalmTestcase extends TestpalmTestcaseInput {
    id: string;
}

interface JsApiStep {
    action: string;
    expectation: string;
}

interface JsApiTestcase {
    title: string;
    filename: string;
    description?: string;
    preconditions: string;
    estimatedTime: string;
    stepsExpects: JsApiStep[];
    tags: Tag[];
    components: Component[];
}

interface TestpalmStep {
    step: string;
    expect: string;
    stepFormatted: string;
    expectFormatted: string;
}

// Testpalm uses generated hex (as string) ids for manualy created definitions. Bun in general it could be any string.
const tagAttributeId = 'tags';
const componentAttributeId = 'components';

async function synchronize(): Promise<unknown> {
    console.log('Parsing testcases from /tests/manual/cases...');
    const jsApiCases = glob
        .sync('./tests/manual/cases/**/*.html')
        .map((file, i, all) => {
            process.stdout.clearLine(0);
            process.stdout.cursorTo(0);

            const completed = 30 * (i + 1) / all.length;
            process.stdout.write(`[${'='.repeat(Math.floor(completed))}${' '.repeat(Math.ceil(30 - completed))}]: `);
            process.stdout.write(`Processing: ${file}`);
            return parseJsApiCase(file);
        })
        .filter(Boolean);
    console.log(`\n${jsApiCases.length} cases defined in jsapi git repo.`);

    console.log('Syncing \'Tags\' and \'Components\' definitions');
    await got.put('', {...definitionGotOptions, json: {title: 'Tags', id: tagAttributeId, values: TAGS}});
    await got.put('', {...definitionGotOptions, json: {title: 'Components', id: componentAttributeId, values: COMPONENTS}});

    console.log('\nLoading testcases from testpalm...');
    const testpalmCases: TestpalmTestcase[] = await got.get(`?include=${includeFields.join(',')}`, testcasesGotOptions).json();
    console.log(`${testpalmCases.length} cases found in Testpalm.\n`);

    const jsApiCasesMap = _.keyBy(jsApiCases, (jsApiCase) => jsApiCase.filename);
    const handledFilenames = new Set<string>();
    const toUpdate: TestpalmTestcase[] = [];
    const toDelete: TestpalmTestcase[] = [];

    for (const testpalmCase of testpalmCases) {
        const filename = testpalmCase.properties.find((prop) => prop.key === 'filename')?.value;

        const jsApiTestcase = jsApiCasesMap[filename];
        if (!jsApiTestcase || handledFilenames.has(filename)) {
            toDelete.push(testpalmCase);
            continue;
        }

        if (filename) {
            handledFilenames.add(filename);
        }

        const convertedCase = convertJsApiToTestpalmTestcase(jsApiTestcase);
        toUpdate.push({...convertedCase, id: testpalmCase.id, attributes: {...testpalmCase.attributes, ...convertedCase.attributes}});
    }

    const toCreate = jsApiCases.filter((jsApiCase) => !handledFilenames.has(jsApiCase.filename));

    const promises = [] as Promise<unknown>[];
    if (toDelete.length > 0) {
        promises.push(
            got.delete(`permanent`, {...testcasesGotOptions, json: toDelete.map((t) => t.id)})
                .then(() => console.log(`Removing ${toDelete.length} testcases.`))
                .catch((error) => console.log('Error in method DELETE:', error))
        );
    }
    if (toCreate.length > 0) {
        promises.push(
            got.post(`bulk`, {...testcasesGotOptions, json: toCreate.map(convertJsApiToTestpalmTestcase)})
                .then(() => console.log(`Creating ${toCreate.length} testcases.`))
                .catch((error) => console.log('Error in method POST:', error))
        );
    }
    if (toUpdate.length > 0) {
        promises.push(
            got.patch(`bulk`, {...testcasesGotOptions, json: toUpdate})
                .then(() => console.log(`Updating ${toUpdate.length} testcases.`))
                .catch((error) => console.log('Error in method PATCH:', error))
        );
    }

    return Promise.all(promises);
}

function convertJsApiToTestpalmTestcase(jsApiCase: JsApiTestcase): TestpalmTestcaseInput {
    const {title, description, stepsExpects, filename, preconditions, estimatedTime} = jsApiCase;
    const tespalmSteps: TestpalmStep[] = stepsExpects
        .map(({action: step, expectation: expect}) => ({step, expect, stepFormatted: step, expectFormatted: expect}));

    return {
        name: title,
        description: description || title,
        stepsExpects: tespalmSteps,
        preconditions,
        estimatedTime: estimatedTime ? parseFloat(estimatedTime) : 0,
        properties: [{key: 'filename', value: filename}],
        attributes: {
            [tagAttributeId]: jsApiCase.tags,
            [componentAttributeId]: jsApiCase.components
        }
    };
}

const BLOCK_RE = /(?=^\s*(?:Title|Description|Precondition|Step|Action|Expectation|Estimated time|Tags|Components)*:)/igm;

function parseJsApiCase(file: string): JsApiTestcase | null {
    const html = fs.readFileSync(file, 'utf8');

    const commentMatch = html.match(/\<\!DOCTYPE html\>\s*\<\!--\s*([\s\S]+?)-->/i);
    if (!commentMatch || !commentMatch[1] || commentMatch[1].trim().match(/^\(skip\)/i)) {
        return;
    }

    const rawComment = commentMatch[1].trim();
    const pageUrl = `${TESTING_URL}${file.slice(2)}`;
    const comment = rawComment.replace(/\$\{currentPagePath\}/g, pageUrl);

    const blocks = comment
        .split(BLOCK_RE)
        .map((txt) => txt.trim())
        .filter(Boolean)
        .map((txt) => {
            const idx = txt.indexOf(':');
            const name = txt.slice(0, idx);
            const content = txt.slice(idx + 1, txt.length);
            return {name: name.toLowerCase(), content};
        });

    const consume = (name: string, optional = false): string | undefined => {
        if (blocks.length && blocks[0].name === name) {
            return blocks.shift().content;
        }
        if (!optional) {
            console.error(`${file}: Expected ${name}, but got ${blocks.length ? blocks[0].name : 'none'}`);
            process.exit(1);
        }
    }

    const title = consume('title');
    const description = consume('description', true);
    const rawTags = consume('tags', true);
    const rawComponents = consume('components', true);

    const estimatedTime = consume('estimated time', true);
    const preconditions = consume('precondition');

    const stepsExpects = [];
    while (blocks.length) {
        consume('step');
        const action = consume('action');
        const expectation = consume('expectation');
        stepsExpects.push({action, expectation});
    }
    const tags = rawTags ? rawTags.split(',').map((tag) => tag.trim()) as Tag[] : [];
    const components = rawComponents ? rawComponents.split(',').map((c) => c.trim()) as Component[] : [];

    tags.forEach((tag) => assert(TAGS.includes(tag), `Invalid tag: ${tag}`));
    components.forEach((component) => assert(COMPONENTS.includes(component), `Invalid component: ${component}`));

    return {filename: file, title, description, preconditions, stepsExpects, estimatedTime, tags, components};
}

function assert(condition, msg) {
    if (!condition) {
        console.error(`\n\n${msg}`);
        process.exit(1);
    }
}

synchronize()
    .catch((e) => {
        console.error(e);
        process.exit(1);
    });
