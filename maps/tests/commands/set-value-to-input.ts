import {wrapAsyncCommand} from '../lib/commands-utils';

async function setValueToInput(
    this: WebdriverIO.Browser,
    selector: string,
    text: string,
    expectedTextResult: string | null = text
): Promise<void> {
    const isClear = text === '';
    const element = await this.$(selector);
    const currentValue = await element.getValue();
    await element.waitForDisplayed();
    const isFocused = await element.isFocused();
    if (!isFocused) {
        await element.click();
    }

    if (currentValue) {
        const backspaces = new Array(currentValue.length).fill('Backspace');
        // Клик в инпут ставит каретку посередине, поэтому двигаем ее в конец
        const moveRight = new Array(currentValue.length).fill('ArrowRight');
        await this.keys(moveRight);
        await this.keys(backspaces);
    }

    if (!isClear) {
        await this.keys(text.split(''));
    }

    if (expectedTextResult) {
        const newValue = await element.getValue();

        if (expectedTextResult !== newValue) {
            throw new Error(`Can't set value: ${text} to ${selector}`);
        }
    }
}

export default wrapAsyncCommand(setValueToInput);
