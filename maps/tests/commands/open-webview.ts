import {wrapAsyncCommand} from '../lib/commands-utils';
import {OpenPageOptions} from './open-page';

async function openWebview(this: WebdriverIO.Browser, pathname: string, options: OpenPageOptions = {}): Promise<void> {
    await this.openPage('/webview' + pathname, {ignoreMapReady: true, ...options});
}

export default wrapAsyncCommand(openWebview);
