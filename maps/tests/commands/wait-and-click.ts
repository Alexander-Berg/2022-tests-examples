import {wrapAsyncCommand} from '../lib/commands-utils';

interface WaitAndClickOptions {
    timeout?: number;
    // Delta from center of element.
    deltaX?: number;
    deltaY?: number;
}

async function waitAndClick(
    this: WebdriverIO.Browser,
    selector: string,
    options: WaitAndClickOptions = {}
): Promise<void> {
    const element = await this.$(selector);
    await element.waitForDisplayed({timeout: options.timeout});
    try {
        // await element.click({x: options.deltaX, y: options.deltaY});
        await clickInElement(this, selector);
    } catch (error) {
        if (
            !error.message.includes('is not clickable at point') &&
            !error.message.includes('move target out of bounds')
        ) {
            if (error.message === 'element not visible') {
                throw new Error(`Вероятно, элемент ${selector} закрыт другим элементом на странице`);
            }
            throw error;
        }
        await this.scrollIntoView(selector, {
            vertical: 'center'
        });
        // await element.click({x: options.deltaX, y: options.deltaY});
        await clickInElement(this, selector);
    }
}

// TODO: https://st.yandex-team.ru/MAPSUI-21174
async function clickInElement(browser: WebdriverIO.Browser, selector: string): Promise<void> {
    try {
        await browser.click(selector);
    } catch (error) {
        if (!error.message.includes('element has zero size') && !error.message.includes('is not clickable at point')) {
            throw error;
        }

        browser.execute(function (selector: string): void {
            if (selector.includes('@aria-label')) {
                (window.document.evaluate(selector, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null)
                    .singleNodeValue as HTMLElement).click();
            } else {
                window.document.querySelector<HTMLElement>(selector)!.click();
            }
        }, selector);
    }
}

export default wrapAsyncCommand(waitAndClick);
