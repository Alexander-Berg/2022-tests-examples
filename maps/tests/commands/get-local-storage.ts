import {wrapAsyncCommand} from '../lib/commands-utils';

async function getLocalStorage<T>(this: WebdriverIO.Browser, key: string): Promise<T | null> {
    try {
        // https://github.com/webdriverio/webdriverio/issues/4340
        const result = await await this.execute(function (key: string): string | null {
            return window.localStorage.getItem(`maps_${key}`);
        }, key);
        return result ? JSON.parse(result) : null;
    } catch (e) {
        return null;
    }
}

export default wrapAsyncCommand(getLocalStorage);
