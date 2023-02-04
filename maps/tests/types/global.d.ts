import {HermioneContext} from './hermione';

interface SuiteCallback {
    // eslint-disable-next-line @typescript-eslint/prefer-function-type
    (this: HermioneContext): void;
}

interface TestCallback {
    // eslint-disable-next-line @typescript-eslint/prefer-function-type,@typescript-eslint/no-explicit-any
    (this: HermioneContext): Promise<void> | Browser<any>;
}

interface SuiteFunction {
    (title: string, callback: SuiteCallback): void;
    (title: string): void;
    only: (title: string, callback?: SuiteCallback) => void;
}

interface TestFunction {
    // eslint-disable-next-line @typescript-eslint/prefer-function-type
    (title: string, callback: TestCallback): void;
    only: (expectation: string, callback?: TestCallback) => Test;
}

type HookFunction = (callback: TestCallback) => void;

declare global {
    const hermione: HermioneContext;

    // Не используем глобальные типы из @types/mocha, т.к. нет возможности переопределить `this`.
    const describe: SuiteFunction;
    const it: TestFunction;

    const before: never;
    const after: never;
    const beforeEach: HookFunction;
    const afterEach: HookFunction;
}
