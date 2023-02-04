import {wrapAsyncCommand} from '../lib/commands-utils';

async function waitAndDoubleClick(this: WebdriverIO.Browser, selector: string, timeout?: number): Promise<void> {
    await this.waitForVisible(selector, timeout);
    try {
        await this.doubleClick(selector);
    } catch (error) {
        if (!error.message.includes('is not clickable at point')) {
            if (error.message === 'element not visible') {
                throw new Error(`Вероятно, элемент ${selector} закрыт другим элементом на странице`);
            }
            throw error;
        }
        await this.scrollIntoView(selector, {
            vertical: 'center'
        });
        await this.doubleClick(selector);
    }
}

export default wrapAsyncCommand(waitAndDoubleClick);
