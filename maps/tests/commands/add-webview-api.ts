import {wrapAsyncCommand} from '../lib/commands-utils';

const WEBVIEW_CONSOLE_MESSAGES = {
    close: 'closing webview'
};

async function addWebviewApi(this: WebdriverIO.Browser): Promise<void> {
    await this.execute(function (webviewConsoleMessages) {
        window.yandex = {
            mapsApp: {
                close() {
                    console.log(webviewConsoleMessages.close);
                }
            }
        };
    }, WEBVIEW_CONSOLE_MESSAGES);
}

export default wrapAsyncCommand(addWebviewApi);
export {WEBVIEW_CONSOLE_MESSAGES};
