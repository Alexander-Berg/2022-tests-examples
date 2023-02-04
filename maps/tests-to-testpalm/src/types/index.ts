import {TestCaseAttributes, TestCaseProperty, TestCase, TestCaseStep} from '@yandex-int/testpalm-api';
import type {MockHermioneContext} from './hermione';

type Without<T, U> = {[P in Exclude<keyof T, keyof U>]?: never};

type XOR<T1, T2> = T1 | T2 extends object ? (Without<T1, T2> & T2) | (Without<T2, T1> & T1) : T1 | T2;

enum Engine {
    HERMIONE = 'hermione',
    PUPPETEER = 'puppeteer'
}

interface BrowserDescriptionStub {
    [key: string]: unknown;
    retrieveCommands: () => string[];
    getCommandsWithoutDescription: () => Set<string>;
}

interface BrowserAttributesStub {
    [key: string]: unknown;
    retrieveAttributes: () => TestCaseAttributes;
}

interface BrowserEstimateStub {
    [key: string]: unknown;
    retrieveEstimate: () => number;
    getCommandsWithoutEstimate: () => Set<string>;
}

interface BrowserPreconditionsStub {
    [key: string]: unknown;
    retrievePreconditionsData: () => string;
}

type Stub = XOR<XOR<XOR<BrowserDescriptionStub, BrowserAttributesStub>, BrowserEstimateStub>, BrowserPreconditionsStub>;

interface SuiteCallback<T = undefined> {
    (this: T): void | Promise<void>;
}

interface TestCallback<T = undefined> {
    (this: T): void | Promise<void>;
}

interface SuiteFunction {
    (title: string, callback: SuiteCallback): void;
    only?: (title: string, callback: SuiteCallback) => void | Promise<void>;
    skip?: (title: string, callback: SuiteCallback) => void | Promise<void>;
}

interface TestFunction {
    (title: string, callback: TestCallback): void;
    only?: (title: string, callback: TestCallback) => void | Promise<void>;
    skip?: (title: string, callback: TestCallback) => void | Promise<void>;
}

type HookFunction = (callback: TestCallback) => void;

type SetComponentCommand = (component: string) => void;

type TestsGlobal = Omit<NodeJS.Global, 'beforeEach' | 'afterEach' | 'describe' | 'it' | 'test'> & {
    describe?: SuiteFunction;
    it?: TestFunction;
    test?: TestFunction;

    beforeEach?: HookFunction;
    afterEach?: HookFunction;
} & XOR<{page: Stub}, {hermione: MockHermioneContext}>;

interface TestCaseMeta {
    attributes?: TestCaseAttributes;
    properties?: TestCaseProperty[];
    isAutotest?: boolean;
}

interface SkipNextData {
    skipped?: boolean;
    skipIn?: string;
    skipReason?: string;
}

interface ItRegisteredData extends TestCaseMeta {
    name: string;
    itCallback: TestCallback;
    beforeEachCallbacks: TestCallback[];
    afterEachCallbacks: TestCallback[];
}

interface DescribeRegisteredData extends SkipNextData {
    name: string;
    its: ItRegisteredData[];
    component: string;
    beforeEachCallback?: TestCallback;
    afterEachCallback?: TestCallback;
}

type LocalTestCase = Partial<TestCase> & {stepsExpects: TestCaseStep[]};

interface LocalTestCasesParseData {
    localTestCases: LocalTestCase[];
    parsingErrors: Error[];
}

type StubValue<T> = ((...args: unknown[]) => T) | boolean | null;

interface BrowserStubs {
    attributes: Partial<Record<string, StubValue<TestCaseAttributes>>>;
    description: Partial<Record<string, StubValue<string>>>;
    estimate: Partial<Record<string, StubValue<number>>>;
    preconditions?: {
        // Use `{{key}}` in tamplate from `browserStub.preconditions`.
        template: string;
    } & Partial<Record<string, string | StubValue<Record<string, string>>>>;
}

interface SelectorDescription {
    description: string;
    screenshot: string | null;
}

interface SelectorsDescription {
    [key: string]: SelectorDescription;
}

type Platform = 'desktop' | 'mobile';

interface Config {
    testPalm: {
        projectId: string;
        token: string;
    };
    browserId: string;
    filePatterns: string[];
    getComponentByFile?: (filePath: string) => string;
    browserStubs: BrowserStubs;
    minEstimate?: number;
    getSelectorsDescriptions: () => SelectorsDescription;
    getTicket: () => Promise<string | undefined>;
    report: {
        folder: string;
        name: string;
    };
    testcop: {
        project: string;
        tool: string;
        branch: string;
    };
    engine: Engine;
    trackerQueue: string;
}

type Mode = 'validation' | 'dry-run' | 'full' | 'pr';

interface RunParams {
    configPath: string;
    mode: Mode;
    issue?: string;
}

export {
    MockHermioneContext,
    TestCaseMeta,
    LocalTestCase,
    TestsGlobal,
    TestCallback,
    SuiteCallback,
    SkipNextData,
    ItRegisteredData,
    DescribeRegisteredData,
    LocalTestCasesParseData,
    BrowserStubs,
    SelectorsDescription,
    SelectorDescription,
    Config,
    Mode,
    RunParams,
    TestCaseAttributes,
    Platform,
    BrowserDescriptionStub,
    BrowserAttributesStub,
    BrowserEstimateStub,
    BrowserPreconditionsStub,
    XOR,
    Stub,
    Engine,
    SuiteFunction,
    TestFunction,
    SetComponentCommand
};
