interface BrowserStubProxyParams {
    browserStub: Record<string, unknown>;
    isStubHelper: (prop: string) => boolean;
    runCommand: (command: unknown, ...args: unknown[]) => void;
    saveUnhandledCommandName?: (prop: string) => void;
}

const getBrowserStubProxy = <T>(params: BrowserStubProxyParams): T => {
    const browserStubProxy = new Proxy(params.browserStub, {
        get: function (stub: Record<string, unknown>, prop: string): unknown | ((...args: unknown[]) => object) {
            const command = stub[prop];
            if (params.isStubHelper(prop)) {
                return command;
            }

            // Выполняем команду в контексте текущего browserStub и возвращаем данный Proxy.
            return (...args: unknown[]) => {
                if (typeof command === 'function') {
                    params.runCommand(command, ...args);
                } else if (params.saveUnhandledCommandName) {
                    params.saveUnhandledCommandName(prop);
                }

                return browserStubProxy;
            };
        }
    });

    return browserStubProxy as T;
};

function getHelpersProps(stub: Record<string, unknown>): string[] {
    return Object.entries(stub)
        .filter(([, value]) => typeof value !== 'function' && value !== null)
        .map(([key]) => key);
}

export {getBrowserStubProxy, getHelpersProps};
