import {execSync} from 'child_process';
import * as fs from 'fs';
import * as path from 'path';
import {URL, URLSearchParams} from 'url';
import * as got from 'got';
import * as jspath from 'jspath';
import * as tv4 from 'tv4';
import {safeLoad as parseYamlToJson} from 'js-yaml';
import {expect} from 'chai';
import {app} from 'app/app';
import {TestServer} from 'tests/utils/test-server';
import {TvmDaemon} from 'tests/utils/tvm-daemon';
import {getTestCases, runTests, TestCase, Validator} from 'tests/utils/utils';

const searchSchema = loadJsonSchema('docs/search/v1/response.yaml', 'FeatureCollection');
const geocodeSchema = loadJsonSchema('docs/geocode/1.x/response.yaml', 'GeocoderResponse');
const geocodeCatalogXmlFile = 'docs/geocode/1.x/xml/catalog.xml';
const geocodeXsdFilePath = 'docs/geocode/1.x/xml/geocoder.xsd';

// based on https://a.yandex-team.ru/arc/trunk/arcadia/extsearch/geo/meta/tests/behavioral/features/
describe('acceptance tests (real geosearch instance will be used)', () => {
    let testServer: TestServer;
    let tvmDaemon: TvmDaemon;

    before(async () => {
        [testServer, tvmDaemon] = await Promise.all([
            TestServer.start(app),
            TvmDaemon.start()
        ]);
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
            checker: validateJsonSchema(searchSchema)
        });
    });

    describe('geocode-api (JSON & XML)', () => {
        runTests({
            getServerUrl: () => testServer.url,
            urlPath: 'geocode/1.x',
            testCases: getTestCases('geocode-api.json'),
            checker: async (testCase, responseRaw) => {
                await validateJsonSchema(geocodeSchema)(testCase, responseRaw);
                await validateXsdSchema(() => testServer.url)(testCase, responseRaw);
            }
        });
    });
});

function loadJsonSchema(filePath: string, rootDefinition: string): JSON {
    const fullFilePath = path.join(__dirname, '../../../', filePath);
    const schema = parseYamlToJson(fs.readFileSync(fullFilePath, 'utf8'));
    schema.oneOf = [
        {$ref: `#/definitions/${rootDefinition}`}
    ];
    return schema;
}

function validateJsonSchema(schema: JSON): Validator {
    return (testCase: TestCase, response: got.Response<string>) => {
        const responseBody = JSON.parse(response.body);
        expect(jspath.apply(testCase.check, responseBody)).to.deep.equal(testCase.expected);

        const result = tv4.validateResult(responseBody, schema);

        const errorMessage = `JSON Schema validation is failed ${formatJsonSchemaError(result.error)}`;
        expect(result.valid, errorMessage).to.equal(true);
    };
}

function validateXsdSchema(getServerUrl: () => string): Validator {
    return async (testCase: TestCase, response: got.Response<string>) => {
        delete testCase.query.format;

        const searchParams = new URLSearchParams(testCase.query);
        const url = new URL('geocode/1.x', getServerUrl());
        url.search = searchParams.toString();
        const xmlResponse = await got.default(url);
        try {
            execSync(`XML_CATALOG_FILES=${geocodeCatalogXmlFile} xmllint --schema ${geocodeXsdFilePath} --noout -`, {
                input: xmlResponse.body.toString()
            });
        } catch (e: any) {
            throw Error(`XSD validation error: ${e.stderr.toString()}`);
        }
    };
}

function formatJsonSchemaError(error: tv4.ValidationError, tab: number = 0): string {
    if (!error) {
        return '';
    }

    const indent = new Array(tab).join(' ');
    let errorMessage = [
        `\n${indent}${error.message}`,
        `dataPath: ${error.dataPath}`,
        `schemaPath: ${error.schemaPath}`
    ].join(`\n${indent}  `);

    if (error.subErrors) {
        error.subErrors.forEach((error) => {
            errorMessage += `${formatJsonSchemaError(error, ++tab)}`;
        });
    }

    return `\n${errorMessage}\n`;
}
