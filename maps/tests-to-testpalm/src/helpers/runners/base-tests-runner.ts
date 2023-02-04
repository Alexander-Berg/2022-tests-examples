/* eslint-disable no-invalid-this */
import {TestCaseAttributes} from '@yandex-int/testpalm-api';
import {createBrowserAttributesStub} from '../../browser-stubs/browser-attributes-stub';
import {createBrowserDescriptionStub} from '../../browser-stubs/browser-description-stub';
import {createBrowserEstimateStub} from '../../browser-stubs/browser-estimate-stub';
import {createBrowserPreconditionsStub} from '../../browser-stubs/browser-preconditions-stub';
import {
    TestsGlobal,
    TestCaseMeta,
    LocalTestCase,
    LocalTestCasesParseData,
    TestCallback,
    ItRegisteredData,
    DescribeRegisteredData,
    BrowserDescriptionStub,
    BrowserAttributesStub,
    BrowserEstimateStub,
    BrowserPreconditionsStub,
    Stub,
    Config
} from '../../types';

interface ItData extends TestCaseMeta {
    name: string;
    commands: string[];
    estimatedTime: number;
    preconditions: string;
}

class BaseTestsRunner {
    private _stubs: {
        attributes: BrowserAttributesStub;
        description: BrowserDescriptionStub;
        estimate: BrowserEstimateStub;
        preconditions: BrowserPreconditionsStub;
    };

    private _minEstimate: number;
    protected _testGlobal: TestsGlobal;

    constructor(global: TestsGlobal, config: Config) {
        const browserStubs = config.browserStubs;
        this._stubs = {
            attributes: createBrowserAttributesStub(browserStubs.attributes),
            description: createBrowserDescriptionStub(browserStubs.description),
            estimate: createBrowserEstimateStub(browserStubs.estimate),
            preconditions: createBrowserPreconditionsStub(browserStubs.preconditions || {template: ''})
        };
        this._testGlobal = global;
        this._minEstimate = config.minEstimate ?? 0;
    }

    protected _itCallbackRunner(itCallback: TestCallback, stub: Stub): Promise<void> {
        return itCallback.call(stub);
    }

    async run(testCases: DescribeRegisteredData[], isValidation: boolean): Promise<LocalTestCasesParseData> {
        if (testCases.length === 0) {
            throw new Error('Отсутствуют кейсы для запуска. Необходимо зарегистрировать тесты перед запуском.');
        }

        const parsingErrors: Error[] = [];

        const itData = await testCases.reduce(
            async (testsDataPromise: Promise<ItData[]>, describe: DescribeRegisteredData) => {
                const testsData = await testsDataPromise;
                const itsData = describe.its.reduce(async (itsData: Promise<ItData[]>, it) => {
                    const currentItsData = await itsData;

                    let testcaseCommands: string[] = [];
                    let testcaseAttributes: TestCaseAttributes = {};
                    let testcaseEstimate = 0;
                    let testcasePreconditions = '';

                    try {
                        testcaseEstimate = await this._getTestcaseEstimate(it);
                        testcaseCommands = await this._getTestcaseCommands(it);
                        testcaseAttributes = await this._getTestcaseAttributes(it);
                        testcasePreconditions = await this._getTestcasePreconditions(it);
                    } catch (err) {
                        if (isValidation) {
                            parsingErrors.push(err);
                        } else {
                            throw err;
                        }
                    }

                    currentItsData.push({
                        isAutotest: it.isAutotest ?? true,
                        name: it.name,
                        properties: it.properties,
                        attributes: {
                            ...it.attributes,
                            ...testcaseAttributes
                        },
                        estimatedTime: Math.max(this._minEstimate, testcaseEstimate),
                        commands: testcaseCommands,
                        preconditions: testcasePreconditions
                    });

                    return currentItsData;
                }, Promise.resolve([]));

                testsData.push(...(await itsData));

                return testsData;
            },
            Promise.resolve([])
        );
        return {
            localTestCases: convertToTestPalmFormat(itData),
            parsingErrors
        };
    }

    getCommandsWithoutDescription = (): Set<string> => {
        return this._stubs.description.getCommandsWithoutDescription();
    };

    getCommandsWithoutEstimate = (): Set<string> => {
        return this._stubs.estimate.getCommandsWithoutEstimate();
    };

    private _getTestcaseCommands = async (it: ItRegisteredData): Promise<string[]> => {
        const beforeEachCommands = await it.beforeEachCallbacks.reduce(
            this._getCommandsDescription,
            Promise.resolve([])
        );
        const afterEachCommands = await it.afterEachCallbacks.reduce(this._getCommandsDescription, Promise.resolve([]));

        await this._itCallbackRunner(it.itCallback, this._stubs.description);

        const itCommands = this._stubs.description.retrieveCommands();

        return [...beforeEachCommands, ...itCommands, ...afterEachCommands].filter((command) => command.length !== 0);
    };

    private _getCommandsDescription = async (
        commandsPromise: Promise<string[]>,
        cb: TestCallback
    ): Promise<string[]> => {
        const commands = await commandsPromise;
        await this._itCallbackRunner(cb, this._stubs.description);

        return commands.concat(this._stubs.description.retrieveCommands());
    };

    private _getTestcaseAttributes = async (it: ItRegisteredData): Promise<TestCaseAttributes> => {
        const beforeEachRealatedAttributes = await it.beforeEachCallbacks.reduce(
            this._getCommandsRelatedAttributes,
            Promise.resolve({})
        );
        const afterEachRealatedAttributes = await it.afterEachCallbacks.reduce(
            this._getCommandsRelatedAttributes,
            Promise.resolve({})
        );

        await this._itCallbackRunner(it.itCallback, this._stubs.attributes);

        const itAttributes = this._stubs.attributes.retrieveAttributes();

        return {
            ...beforeEachRealatedAttributes,
            ...itAttributes,
            ...afterEachRealatedAttributes
        };
    };

    private _getCommandsRelatedAttributes = async (
        commandsPromise: Promise<TestCaseAttributes>,
        cb: TestCallback
    ): Promise<TestCaseAttributes> => {
        const attributes = await commandsPromise;

        await this._itCallbackRunner(cb, this._stubs.attributes);

        return {
            ...attributes,
            ...this._stubs.attributes.retrieveAttributes()
        };
    };

    private _getTestcaseEstimate = async (it: ItRegisteredData): Promise<number> => {
        const beforeEachCommandsEstimate = await it.beforeEachCallbacks.reduce(
            this._getCommandsEstimate,
            Promise.resolve(0)
        );
        const afterEachCommandsEstimate = await it.afterEachCallbacks.reduce(
            this._getCommandsEstimate,
            Promise.resolve(0)
        );

        await this._itCallbackRunner(it.itCallback, this._stubs.estimate);

        const itCommandsEstimate = this._stubs.estimate.retrieveEstimate();

        return beforeEachCommandsEstimate + itCommandsEstimate + afterEachCommandsEstimate;
    };

    private _getCommandsEstimate = async (estimatePromise: Promise<number>, cb: TestCallback): Promise<number> => {
        const estimate = await estimatePromise;

        await this._itCallbackRunner(cb, this._stubs.estimate);

        return estimate + this._stubs.estimate.retrieveEstimate();
    };

    private _getTestcasePreconditions = async (it: ItRegisteredData): Promise<string> => {
        // Выполняем только для колбэков beforeEach, т.к. afterEach это уже не прекондишн.
        await it.beforeEachCallbacks.reduce<Promise<void>>(async (promise, cb) => {
            await promise;

            await this._itCallbackRunner(cb, this._stubs.preconditions);
        }, Promise.resolve());

        await this._itCallbackRunner(it.itCallback, this._stubs.preconditions);

        return this._stubs.preconditions.retrievePreconditionsData();
    };
}

function convertToTestPalmFormat(itData: ItData[]): LocalTestCase[] {
    return itData.map(({commands, ...restItData}) => {
        return {
            ...restItData,
            stepsExpects: commands.map((str) => ({step: str, stepFormatted: str}))
        };
    });
}

export default BaseTestsRunner;
