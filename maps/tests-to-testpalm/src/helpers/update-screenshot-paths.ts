import {TestCaseProperty, TestCaseStep} from '@yandex-int/testpalm-api';
import path from 'path';
import {LocalTestCase} from '../types';

function getScreenshotElementTemplate(browserId: string, properties?: TestCaseProperty[]): string | undefined {
    const filePath = properties?.find((p) => p.key === 'filePath')?.value;
    if (!filePath) {
        return;
    }

    const {dir, name} = path.parse(filePath);
    const imagePath = encodeURIComponent(
        `maps/front/services/maps/${dir}/screenshots/${browserId}/${name === 'index' ? '' : `${name}/`}`
    );

    return `<details><summary>Latest screenshot</summary><img src="https://a.yandex-team.ru/api/v2/repos/arc/downloads?at=trunk&path=${imagePath}`;
}

function updateStep(testCaseStep: TestCaseStep, screenshotElementTemplate: string): TestCaseStep {
    const step = testCaseStep.stepFormatted || testCaseStep.step;
    const updatedStep = step
        .replace('{{SCREENSHOT_START}}', screenshotElementTemplate)
        .replace('{{SCREENSHOT_END}}', '"/></details>');
    return {...testCaseStep, stepFormatted: updatedStep, step: updatedStep};
}

function updateScreenshotPaths(cases: LocalTestCase[], browserId: string): LocalTestCase[] {
    return cases.map((localCase) => {
        const screenshotElementTemplate = getScreenshotElementTemplate(browserId, localCase.properties);
        const stepsExpects = screenshotElementTemplate ?
            localCase.stepsExpects.map((step) => updateStep(step, screenshotElementTemplate)) :
            localCase.stepsExpects;
        return {
            ...localCase,
            stepsExpects
        };
    });
}

export {updateScreenshotPaths};
