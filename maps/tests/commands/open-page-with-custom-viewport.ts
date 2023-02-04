import cssSelectors from '../common/css-selectors';
import {wrapAsyncCommand} from '../lib/commands-utils';
import {OpenPageOptions} from './open-page';
import {getUrl} from '../lib/func/url';

interface OpenPageWithCustomViewportOptions extends OpenPageOptions {
    width: number;
    height: number;
}

async function openPageWithCustomViewport(
    this: WebdriverIO.Browser,
    pathname: string,
    {width, height, ...options}: OpenPageWithCustomViewportOptions
): Promise<void> {
    const url = getUrl(pathname, options);
    const mock = await this.mock(url);
    mock.respondOnce(
        `
        <!DOCTYPE html>
        <html>
            <head>
                <style>
                    html,
                    body {
                        width: 100%;
                        height: 100%;
                        margin: 0;
                        padding: 0;
                    }
                </style>
            </head>
            <body>
                <iframe id="iframe" width="${width}" height="${height}" src="${url}" />
            </body>
        </html>
        `,
        {
            headers: () => ({'content-type': 'text/html; charset=utf-8'})
        }
    );
    await this.url(url);
    // set context to frame`s context see https://webdriver.io/docs/api/webdriver/#switchtoframe
    await this.switchToFrame(await this.$('iframe'));

    await this.waitForExist(options.readySelector || cssSelectors.appReady);
    if (!options.ignoreMapReady) {
        await this.waitForExist(cssSelectors.mapReady);

        if (options.enableVector) {
            await this.waitForExist(cssSelectors.vectorReady);
        }
    }
}

export default wrapAsyncCommand(openPageWithCustomViewport);
export {OpenPageOptions};
