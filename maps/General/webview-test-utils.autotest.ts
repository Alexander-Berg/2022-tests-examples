import {OpenPageOptions} from '../../tests/commands/open-page';

type WebviewTheme = 'light' | 'dark';

function getWebviewUrl(pathname: string): string {
    return '/webview' + pathname;
}
async function openPage(
    browser: WebdriverIO.Browser,
    pathname: string,
    openPageOptions?: OpenPageOptions
): Promise<void> {
    await browser.openPage(getWebviewUrl(pathname), {ignoreMapReady: true, ...openPageOptions});
}

export {openPage, WebviewTheme};
