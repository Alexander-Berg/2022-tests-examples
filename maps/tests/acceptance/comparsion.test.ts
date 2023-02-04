import {execSync} from 'child_process';
import {URL, URLSearchParams} from 'url';
import * as got from 'got';
import {expect} from 'chai';
import {diffLines} from 'diff';
import {createHostConfigLoader, Hosts} from '@yandex-int/maps-host-configs';
import {TestServer} from 'tests/utils/test-server';
import {TvmDaemon} from 'tests/utils/tvm-daemon';
import {app} from 'app/app';
import {getTestCases, runTests, TestCase, Validator} from 'tests/utils/utils';

const hostConfigLoader = createHostConfigLoader({
    env: 'testing',
    basePath: 'sandbox-resources/hosts/hosts/1.3'
});

describe('comparsion tests (compare development and testing instances)', () => {
    let testServer: TestServer;
    let tvmDaemon: TvmDaemon;
    let hosts: Hosts;

    before(async () => {
        [testServer, tvmDaemon] = await Promise.all([
            TestServer.start(app),
            TvmDaemon.start()
        ]);
        hosts = await hostConfigLoader.get();
    });

    after(async () => {
        await Promise.all([
            testServer.stop(),
            tvmDaemon.stop()
        ]);
    });

    describe('search-api (JSON)', () => {
        runTests({
            getServerUrl: () => testServer.url,
            urlPath: 'search/v1/',
            testCases: getTestCases('search-api.json'),
            checker: compareJsonWithTesting(() => `${hosts.searchApi}v1/`)
        });
    });

    describe('geocode-api (JSON & XML)', () => {
        runTests({
            getServerUrl: () => testServer.url,
            urlPath: 'geocode/1.x',
            testCases: getTestCases('geocode-api.json'),
            checker: compareJsonAndXmlWithTesting(() => testServer.url, () => `${hosts.geocodeApi}1.x/`)
        });
    });
});

function compareJsonWithTesting(getTestingUrl: () => string): Validator {
    return async (testCase: TestCase, localResponse: got.Response<string>) => {
        const testingResponse = await getResponse(getTestingUrl(), testCase);
        expect(testingResponse.statusCode).to.equal(localResponse.statusCode);

        const testingBody = JSON.parse(testingResponse.body);
        const localBody = JSON.parse(localResponse.body);
        expect(testingBody).to.deep.equal(localBody);
    };
}
function compareJsonAndXmlWithTesting(getServerUrl: () => string, getTestingUrl: () => string): Validator {
    return async (testCase: TestCase, localResponse: got.Response<string>) => {
        await compareJsonWithTesting(getTestingUrl)(testCase, localResponse);

        delete testCase.query.format;
        const localXmlResponse = await getResponse(`${getServerUrl()}/geocode/1.x`, testCase);

        const testingXmlResponse = await getResponse(getTestingUrl(), testCase);
        const formattedLocalXmlResponse = prettifyXml(localXmlResponse.body);
        const formattedTestingXmlResponse = prettifyXml(testingXmlResponse.body);
        const xmlDiff = getDiff(formattedLocalXmlResponse.toString(), formattedTestingXmlResponse.toString());
        expect(xmlDiff.expected).to.be.deep.equal(xmlDiff.actual);
    };
}

async function getResponse(serverUrl: string, testCase: TestCase): Promise<got.Response<string>> {
    const searchParams = new URLSearchParams(testCase.query);
    const url = new URL(serverUrl);
    url.search = searchParams.toString();
    return got.default(url);
}

function prettifyXml(xml: string): string {
    return execSync(`xmllint --format -`, {input: xml}).toString();
}

function getDiff(expectedXml: string, actualXml: string): {expected: string[], actual: string[]} {
    const changes = diffLines(expectedXml, actualXml);
    const expected: string[] = [];
    const actual: string[] = [];

    changes.forEach((change) => {
        if (change.added) {
            expected.push(change.value);
        }
        if (change.removed) {
            actual.push(change.value);
        }
    });

    return {expected, actual};
}
