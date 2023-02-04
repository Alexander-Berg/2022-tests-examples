import {TestCaseStep} from '@yandex-int/testpalm-api';
import {LocalTestCase, SelectorsDescription, SelectorDescription} from '../types';

interface UpdateSelectorsParams {
    testCases: LocalTestCase[];
    selectorsData: SelectorsDescription;
}

interface UpdateSelectorsOutput {
    testCasesWithDescription: LocalTestCase[];
    selectorsWithoutDescription: Set<string>;
}

const SELECTORS_REG_EXPS = [
    // Регулярное выражение для матчинга селекторов соответствующих паттерну `.class-name whatever`
    /`\.[^`]+`/g,
    // `//*[contains(@class, 'some-class')]//*[(text() | @aria-label)='elements text']`
    /`\/\/[^`]+`/g,
    // `body` или `img[attribute*=\"attribute value\"]`
    /`(img|a|body|div|iframe)[^`]*`/g
];

async function updateSelectorsDescription(params: UpdateSelectorsParams): Promise<UpdateSelectorsOutput> {
    const selectorsWithoutDescription = new Set<string>();
    const stepUpdater = getStepsUpdater(params.selectorsData, selectorsWithoutDescription);

    const testCasesWithDescription = params.testCases.map((testCase) => {
        const updatedSteps = testCase.stepsExpects.map(stepUpdater);

        if (isStepsUpdated(updatedSteps)) {
            testCase = {
                ...testCase,
                stepsExpects: updatedSteps
            };
        }

        return updateDecryptionAttribute(testCase);
    });

    return {
        testCasesWithDescription,
        selectorsWithoutDescription
    };
}

function getStepsUpdater(
    selectorsData: SelectorsDescription,
    selectorsWithoutDescription: Set<string>
): (testCase: TestCaseStep) => TestCaseStep {
    return (data: TestCaseStep): TestCaseStep => {
        const selectors: string[] = [];

        SELECTORS_REG_EXPS.forEach((regExp) => {
            const matchingSelectors = data.step.match(regExp);

            if (matchingSelectors) {
                selectors.push(...matchingSelectors);
            }
        });

        if (selectors.length === 0) {
            return data;
        }

        // Не копируем весь объект data, т.к. нас интересует только поле step.
        // Поле stepFormatted для обновленных кейсов будет создано в пальме при записи.
        const updatedData = {step: data.step, stepFormatted: ''};
        selectors.forEach((selector) => {
            const selectorKey = selector.replace(/`/g, '');
            const regExp = new RegExp(
                // Экранируем все спецсимволы RegExp.
                selector.replace(/[()[\]*+.?^$\\|]/g, (char) => `\\${char}`),
                'g'
            );
            if (selectorsData[selectorKey]) {
                const selectorDescription = getSelectorDescription(selector, selectorsData[selectorKey]);
                updatedData.step = updatedData.step.replace(regExp, selectorDescription);
            } else {
                selectorsWithoutDescription.add(selectorKey);
            }
        });

        return updatedData;
    };
}

function isStepsUpdated(steps: Partial<TestCaseStep>[]): boolean {
    return steps.some((step) => !step.stepFormatted);
}

function getSelectorDescription(selector: string, selectorData: SelectorDescription): string {
    const screenshot = selectorData.screenshot;
    const screenshotMarkdown = screenshot ? ` <details><summary>screenshot</summary>![](${screenshot})</details>` : '';
    const selectorMarkdown = ` <details><summary>selector</summary>${selector}</details>`;
    return `${selectorData.description}${screenshotMarkdown}${selectorMarkdown}`;
}

function isStepsCompletelyDecrypted(stepsExpects: TestCaseStep[]): boolean {
    // Проверяем что все селекторы в шагах тесткейса находятся внутри описания с расшифровкой.
    return stepsExpects.every(
        // Регулярка взята из SELECTORS_REG_EXPS
        (stepExpects) => !/(?<!summary>)`(\.|\/\/|img|a|body|div|iframe)[^`]+`(?!<)/g.test(stepExpects.step)
    );
}

function needScreenshot(stepsExpects: TestCaseStep[]): boolean {
    return stepsExpects.some(
        (stepExpects) => /selector/.test(stepExpects.step) && !/screenshot/.test(stepExpects.step)
    );
}

function updateDecryptionAttribute(testCase: LocalTestCase): LocalTestCase {
    return {
        ...testCase,
        attributes: {
            ...testCase.attributes,
            selectorsDecrypted: [isStepsCompletelyDecrypted(testCase.stepsExpects) ? 'true' : 'false'],
            needScreenshot: [needScreenshot(testCase.stepsExpects) ? 'true' : 'false']
        }
    };
}

export {updateSelectorsDescription, SelectorsDescription};
