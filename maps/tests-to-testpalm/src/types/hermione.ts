/// <reference types='hermione' />

interface ExcludeReason {
    (reason?: string): never;

    in: (match: RegExp | string, reason: string) => never;
}

interface HermioneContext {
    browser: WebdriverIO.Browser;
    skip: ExcludeReason;
    only: ExcludeReason;
}

type HermionePerformCommand = (
    this: WebdriverIO.Browser,
    callback: () => void | Promise<void>,
    description: string | null
) => void | Promise<void>;

interface MockExcludeReason {
    in: (match: RegExp | string, reason: string) => void;
}

interface MockHermioneContext {
    skip: MockExcludeReason;
    only: MockExcludeReason;
    testPalm: {
        setComponent: (component: string) => void;
    };
}

export {HermioneContext, HermionePerformCommand, MockHermioneContext};
