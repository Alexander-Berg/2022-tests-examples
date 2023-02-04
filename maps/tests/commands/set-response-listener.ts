import {HTTPResponse} from 'puppeteer-core/lib/cjs/puppeteer/common/HTTPResponse';
import {wrapAsyncCommand} from '../lib/commands-utils';

async function setResponseListener(
    this: WebdriverIO.Browser,
    responseListener: (response: HTTPResponse) => void
): Promise<void> {
    const puppeteer = await this.getPuppeteer();
    const [page] = await puppeteer.pages();
    await page.setRequestInterception(true);
    page.on('response', responseListener);
}

export default wrapAsyncCommand(setResponseListener);
