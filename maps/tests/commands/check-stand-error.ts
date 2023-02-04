import {wrapAsyncCommand} from '../lib/commands-utils';

const STAND_ERROR_SELECTOR = 'body > pre:only-child';

async function checkStandError(this: WebdriverIO.Browser): Promise<void> {
    const element = await this.$(STAND_ERROR_SELECTOR).catch(() => null);
    if (!element) {
        return;
    }

    const isStandError = await element.isExisting();
    if (isStandError) {
        const text = await element.getText();
        throw new Error(text);
    }
}

export default wrapAsyncCommand(checkStandError);
