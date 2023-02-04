import {wrapAsyncCommand} from '../lib/commands-utils';

async function waitForElementsCount(
    this: WebdriverIO.Browser,
    selector: string,
    expectedCount: number,
    timeout = 5000,
    interval = 500
) {
    await this.waitForExist(selector);
    await this.waitUntil(() => verifyElementsCount(this, selector, expectedCount), timeout, undefined, interval);
}

async function verifyElementsCount(browser: WebdriverIO.Browser, selector: string, count: number): Promise<boolean> {
    const result = await browser.$$(selector);
    if (result.length !== count) {
        const expected = `Ожидается: ${count}`;
        const actual = `На самом деле: ${result.length}`;
        throw new Error(`Количество элементов "${selector}":\n${expected}\n${actual}`);
    }
    return true;
}

export default wrapAsyncCommand(waitForElementsCount);
