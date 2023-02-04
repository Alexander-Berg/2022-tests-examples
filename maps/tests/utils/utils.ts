import * as fs from 'fs';
import * as got from 'got';
import {URL, URLSearchParams} from 'url';
import {config} from '../config';

interface TestQuery {
    [index: string]: string;
}
export type Validator = (testCase: TestCase, response: got.Response<string>) => void;
export interface TestCase {
    title: string;
    ignore?: boolean;
    query: TestQuery;
    check: string;
    expected: string;
}

export function getTestCases(filename: string): TestCase[] {
    const caseFilePath = `src/tests/acceptance/cases/${filename}`;
    return JSON.parse(fs.readFileSync(caseFilePath).toString()) as TestCase[];
}

export interface RunTestServerOptions {
    getServerUrl: () => string;
    urlPath: string;
    testCases: TestCase[];
    checker: Validator;
}

export function runTests(options: RunTestServerOptions) {
    options.testCases.forEach((testCase) => {
        testCase.query.apikey = config.apikey;
        const searchParams = new URLSearchParams(testCase.query);

        it(testCase.title, async () => {
            const url = new URL(options.urlPath, options.getServerUrl());
            url.search = searchParams.toString();
            const response: got.Response<string> = await got.default(url);
            await options.checker(testCase, response);
        });
    });
}
