type Fn<T extends unknown[], R> = (...args: T) => Promise<R>;

const wrapAsyncCommand = <T extends unknown[], R>(fn: Fn<T, R>): Fn<T, R> => {
    return function (this: WebdriverIO.Browser, ...args: T): Promise<R> {
        return fn.call(this, ...args);
    };
};

export {wrapAsyncCommand};
