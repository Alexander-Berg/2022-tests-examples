import {wrapAsyncCommand} from '../lib/commands-utils';

async function setLocalStorage<T>(this: WebdriverIO.Browser, key: string, value: T): Promise<void> {
    // https://github.com/webdriverio/webdriverio/issues/4340
    await this.execute(
        function (key: string, value: T): void {
            window.localStorage.setItem(`maps_${key}`, JSON.stringify(value));
        },
        key,
        value
    );
}

export default wrapAsyncCommand(setLocalStorage);
