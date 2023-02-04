import {HTTPRequest} from 'puppeteer-core/lib/cjs/puppeteer/common/HTTPRequest';
import {wrapAsyncCommand} from '../lib/commands-utils';

async function setRequestListener(
    this: WebdriverIO.Browser,
    requestListener: (request: HTTPRequest) => void
): Promise<void> {
    const puppeteer = await this.getPuppeteer();
    const [page] = await puppeteer.pages();
    await page.setRequestInterception(true);
    page.on('request', requestListener);
}

export default wrapAsyncCommand(setRequestListener);
