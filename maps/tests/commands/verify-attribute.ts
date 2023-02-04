import {wrapAsyncCommand} from '../../tests/lib/commands-utils';

async function verifyAttribute(
    this: WebdriverIO.Browser,
    selector: string,
    attribute: string,
    expected: string | RegExp | string[] | RegExp[]
): Promise<void> {
    await this.waitForExist(selector);
    const elements = await this.$$(selector);
    const actualAttributes = await Promise.all(elements.map((element) => element.getAttribute(attribute)));
    const expectedAttributes = Array.isArray(expected) ? expected : [expected];
    const expectedString = `Ожидается: ${expectedAttributes.map(String).join(', ')}`;
    const actualString = `На самом деле: ${actualAttributes.join(', ')}`;
    if (actualAttributes.length !== expectedAttributes.length) {
        throw new Error(
            `Ожидалось ${expectedAttributes.length} элементов "${selector}" с атрибутом "${attribute}", получено ${actualAttributes.length}.\nИх значения:\n${expectedString}\n${actualString}`
        );
    }
    const isValid = expectedAttributes.every((expectedAttribute, index) => {
        const actualValue = actualAttributes[index];
        return expectedAttribute instanceof RegExp
            ? expectedAttribute.test(actualValue)
            : expectedAttribute === actualValue;
    });
    if (!isValid) {
        throw new Error(`Значение атрибута "${attribute}" в "${selector}":\n${expectedString}\n${actualString}`);
    }
}

export default wrapAsyncCommand(verifyAttribute);
