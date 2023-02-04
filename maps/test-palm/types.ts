import {Page} from 'puppeteer';

enum Platform {
    DESKTOP = 'desktop'
};

type BrowserCommands = Omit<
    Page,
    | 'executionContext'
    | 'desiredCapabilities'
    | 'sessionId'
    | 'options'
    | 'isIOS'
    | 'isAndroid'
    | 'isPhone'
    | 'then'
    | 'waitForVisible'
> & {
    waitForVisible: (selector: string, milliseconds?: number, reverse?: boolean) => Promise<null>;
};

type TestCommandName = keyof BrowserCommands;

type BrowserStub<T> = {
    [K in TestCommandName]?:
        | null
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        | ((...args: BrowserCommands[K] extends (...args: any) => any ? Parameters<BrowserCommands[K]> : never[]) => T);
} & {
    isPhone: boolean;
};

type GetBrowserStub<T, A extends Record<string, unknown> = {}> = (platform: Platform) => BrowserStub<T> & A;

export {Platform, BrowserStub, GetBrowserStub};
