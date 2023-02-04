import {wrapAsyncCommand} from '../lib/commands-utils';

type ExpectedValue = string | RegExp | string[] | RegExp[];
const DEFAULT_PARAMS = {timeout: 5000, interval: 500};
async function waitAndCheckValue(
    this: WebdriverIO.Browser,
    selector: string,
    value: ExpectedValue,
    params: {timeout?: number; interval?: number; reverse?: boolean} = DEFAULT_PARAMS
) {
    await this.waitForExist(selector);

    await this.waitUntil(
        () => verifyValue(this, selector, value, undefined, params.reverse),
        params.timeout || DEFAULT_PARAMS.interval,
        undefined,
        params.interval || DEFAULT_PARAMS.interval
    );
}

const TEXT_TAGS = ['textarea', 'select', 'input'];

function arrify<T>(value: T | T[]): T[] {
    return Array.isArray(value) ? value : [value];
}

async function shouldUseValue(element: WebdriverIO.Element): Promise<boolean> {
    const tagData = await element.getTagName();
    if (!tagData) {
        return false;
    }
    return TEXT_TAGS.includes(tagData);
}

async function verifyValue(
    browser: WebdriverIO.Browser,
    selector: string,
    expected: ExpectedValue,
    noTrim?: boolean,
    reverse?: boolean
): Promise<boolean> {
    const elements = await browser.$$(selector);
    const useValue = await shouldUseValue(elements[0]);

    const actualValues = await Promise.all(
        elements.map((element) => (useValue ? element.getValue() : element.getText()))
    );
    const expectedValues = arrify(expected);

    const isValid = expectedValues.every((expectedValue, index) => {
        const actualValue = noTrim ? actualValues[index] : actualValues[index].trim();
        return expectedValue instanceof RegExp ? expectedValue.test(actualValue) : expectedValue === actualValue;
    });
    if ((!isValid && !reverse) || (isValid && reverse)) {
        const expectedString = `Ожидается: ${expectedValues.map(String).join(', ')}`;
        const actualString = `На самом деле: ${actualValues.map((value) => `"${value}"`).join(', ')}`;

        throw new Error(`Значение "${selector}":\n${expectedString}\n${actualString}`);
    }
    return true;
}

export default wrapAsyncCommand(waitAndCheckValue);
