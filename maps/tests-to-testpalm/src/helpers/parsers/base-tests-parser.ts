/* eslint-disable no-invalid-this */
import * as glob from 'glob';
import * as path from 'path';
import {pick} from 'lodash';
import {TestcopAPI} from '@yandex-int/testcop-api';
import {
    TestCaseMeta,
    TestsGlobal,
    TestCallback,
    SuiteCallback,
    SkipNextData,
    DescribeRegisteredData,
    Config,
    SuiteFunction,
    TestFunction
} from '../../types';

interface TestcopWithSkipMeta {
    fullName: string;
    skipMeta: Required<TestCaseMeta>;
}

const SKIP_ALL_PATTERN = '/.*/';

class BaseTestsParser {
    private _registeredTests: DescribeRegisteredData[] = [];
    private _currentDescribe: DescribeRegisteredData | null = null;
    private _filePath?: string;
    private _currentComponent: string | null = null;
    private _errors: Error[] = [];
    private _unskipTickets: Set<string> = new Set();
    private _testcopTestsWithSkipMeta: TestcopWithSkipMeta[] = [];
    private _testsGlobal: TestsGlobal;
    private _config: Config;

    protected _nextComponent: string | null = null;
    protected _skipNext: SkipNextData | null = null;

    constructor(global: TestsGlobal, config: Config) {
        this._testsGlobal = global;
        this._config = config;
    }

    async parse(): Promise<void> {
        this._prepareGlobal(this._testsGlobal);
        const config = this._config;
        await this._getSkippedTestcopTestsList(config);

        config.filePatterns.forEach((pattern) => {
            glob.sync(pattern).forEach((file) => {
                this._filePath = file.replace(process.cwd() + '/', '');
                this._currentComponent = config.getComponentByFile?.(this._filePath) ?? 'auto-sync';
                require(path.resolve(file));
            });
        });
    }

    getTestCases(): DescribeRegisteredData[] {
        return this._registeredTests.filter((describe) =>
            Boolean(describe.name && describe.its && describe.its.length !== 0)
        );
    }

    getErrors(): Error[] {
        return this._errors;
    }

    getUnskipTickets(): string[] {
        return Array.from(this._unskipTickets);
    }

    protected _prepareGlobal(testGlobal: TestsGlobal): void {
        testGlobal.beforeEach = this._beforeEachStub;
        testGlobal.afterEach = this._afterEachStub;

        testGlobal.describe = this._describeStub;

        testGlobal.it = this._itStub;
        testGlobal.test = this._itStub;
    }

    private _getTicketFromReason = (reason?: string): string | undefined => {
        if (reason) {
            const ticket = reason.match(new RegExp(`${this._config.trackerQueue}-\\d+`, 'i'));
            return ticket ? ticket[0] : undefined;
        }
        return;
    };

    protected _describeStub: SuiteFunction = (title: string, callback: SuiteCallback) => {
        let skipDescribeData = {};

        if (this._skipNext) {
            skipDescribeData = {
                skipped: true,
                skipIn: this._skipNext.skipIn,
                skipReason: this._skipNext.skipReason
            };

            const ticket = this._getTicketFromReason(this._skipNext.skipReason);
            if (ticket) {
                this._unskipTickets.add(ticket);
            }
            this._skipNext = null;
        } else {
            // Запоминаем значения для вложенных сьютов.
            skipDescribeData = pick(this._currentDescribe, 'skipped', 'skipIn', 'skipReason');
        }

        const prevDescribe = this._currentDescribe;

        this._currentDescribe = {
            name: title,
            component: this._nextComponent || prevDescribe?.component || this._currentComponent!,
            its: [],
            ...skipDescribeData
        };
        this._nextComponent = null;

        callback.call({});

        this._currentDescribe.its.forEach((it) => {
            // В случае наличия хуков beforeEach или afterEach в текущем describe,
            // добавляем колбэки из этих хуков в кейсы для которых их надо будет выполнить.
            if (this._currentDescribe?.beforeEachCallback) {
                it.beforeEachCallbacks.unshift(this._currentDescribe.beforeEachCallback);
            }
            if (this._currentDescribe?.afterEachCallback) {
                it.afterEachCallbacks.unshift(this._currentDescribe.afterEachCallback);
            }

            it.name = this._currentDescribe!.name + ' ' + it.name;

            const testcopTestWithSkipMeta = this._testcopTestsWithSkipMeta.find(
                (testcopTestWithSkipMeta) => testcopTestWithSkipMeta.fullName === it.name
            );

            if (testcopTestWithSkipMeta && !it.attributes?.skip) {
                it.isAutotest = testcopTestWithSkipMeta.skipMeta.isAutotest;
                it.properties = [...testcopTestWithSkipMeta.skipMeta.properties, ...(it.properties || [])];
                it.attributes = {
                    ...testcopTestWithSkipMeta.skipMeta.attributes,
                    ...it.attributes
                };
            }
        });

        if (prevDescribe) {
            prevDescribe.its = [...prevDescribe.its, ...this._currentDescribe.its];
            this._currentDescribe = prevDescribe;
        } else {
            this._registeredTests.push(this._currentDescribe);
            this._currentDescribe = null;
        }
    };

    protected _itStub: TestFunction = async (title: string, callback: TestCallback): Promise<void> => {
        let skipData: TestCaseMeta = {};
        if (!this._currentDescribe) {
            this._errors.push(new Error(`Тесткейс '${title}' должен быть расположены внутри блока describe.`));
            return;
        }

        // Если заскипан и кейс и сьют, в котором этот кейс расположен,
        // то считаем информацию о заскипе кейса приоритетной.
        if (this._skipNext) {
            skipData = this._getItSkipData(this._skipNext);

            this._skipNext = null;
        } else if (this._currentDescribe.skipped) {
            skipData = this._getItSkipData(this._currentDescribe);
        }

        const testCaseMeta: TestCaseMeta = {
            ...skipData,
            properties: [
                ...(skipData.properties || []),
                {
                    key: 'filePath',
                    title: 'filePath',
                    value: this._filePath || ''
                }
            ],
            attributes: {
                ...skipData.attributes,
                // Для фильтрации
                generated: ['true'],
                components: [this._nextComponent || this._currentDescribe.component]
            }
        };

        this._currentDescribe.its.push({
            name: title,
            itCallback: callback,
            beforeEachCallbacks: [],
            afterEachCallbacks: [],
            ...testCaseMeta
        });
        this._nextComponent = null;
    };

    private _beforeEachStub = (cb: TestCallback): void => {
        this._setHookCb(cb, 'before');
    };

    private _afterEachStub = (cb: TestCallback): void => {
        this._setHookCb(cb, 'after');
    };

    private _setHookCb = (cb: TestCallback, hookType: 'before' | 'after'): void => {
        if (!this._currentDescribe) {
            this._errors.push(new Error('Хуки beforeEach и afterEach должны быть расположены внутри блока describe.'));
            return;
        }

        if (hookType === 'before') {
            this._currentDescribe.beforeEachCallback = cb;
        } else {
            this._currentDescribe.afterEachCallback = cb;
        }
    };

    private _getItSkipData = (skipData: SkipNextData): TestCaseMeta => {
        const ticket = this._getTicketFromReason(skipData.skipReason);
        return {
            isAutotest: false,
            attributes: {
                skip: ['true'],
                unskipTicket: ticket ? [ticket] : []
            },
            properties: [
                {
                    key: 'skipIn',
                    title: 'skipIn',
                    value: String(skipData.skipIn)
                },
                {
                    key: 'skipReason',
                    title: 'skipReason',
                    value: skipData.skipReason || ''
                }
            ]
        };
    };

    private _getSkippedTestcopTestsList = (config: Config): Promise<void> => {
        return new TestcopAPI('')
            .getTestSkips({
                project: config.testcop.project,
                tool: config.testcop.tool,
                skipsBranch: config.testcop.branch,
                isFlakyTestsStatsRequired: false
            })
            .then((response) => {
                this._testcopTestsWithSkipMeta = Object.entries(response.skips)
                    .reduce((acc: TestcopWithSkipMeta[], [fullName, data]) => {
                        if (!fullName.endsWith('.' + config.browserId)) {
                            return acc;
                        }

                        acc.push({
                            fullName: fullName.replace(new RegExp(`\\.${config.browserId}$`), ''),
                            skipMeta: {...this._formatSkipDataByReason(data.reason)}
                        });
                        return acc;
                    }, []);
            })
            .catch((e) => {
                console.warn('Failed to get skip-list. With params:');
                console.warn({
                    project: config.testcop.project,
                    tool: config.testcop.tool,
                    branch: config.testcop.branch
                });
                console.warn(e.message ?? e);
            });
    };

    private _formatSkipDataByReason = (skipReason: string): Required<TestCaseMeta> => {
        return (this._getItSkipData({
            skipIn: SKIP_ALL_PATTERN,
            skipReason
        }) as unknown) as Required<TestCaseMeta>;
    };
}

export {SKIP_ALL_PATTERN};
export default BaseTestsParser;
