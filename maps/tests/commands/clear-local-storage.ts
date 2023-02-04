import {wrapAsyncCommand} from '../lib/commands-utils';

async function clearLocalStorage(this: WebdriverIO.Browser): Promise<void> {
    // https://github.com/webdriverio/webdriverio/issues/4340
    await this.execute(function (): void {
        window.localStorage.clear();
    });
}

export default wrapAsyncCommand(clearLocalStorage);
