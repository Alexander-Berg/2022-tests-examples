/* globals __PR_FILES__, __allure__, allure */
/** @type {Array} */
const PR_FILES = __PR_FILES__;

const fs = require('fs');
const { join, dirname } = require('path');
const stripAnsi = require('strip-ansi');
const { createHash } = require('crypto');
const { LabelName, Status, Stage } = require('allure-js-commons');
const {
    customSnapshotIdentifier,
    toMatchImageSnapshotFn
} = require('@realty-front/jest-utils/puppeteer/tests-setup/utils');

const { getState } = require('@vertis/allure-report/build/helpers/getState');
const { PREFIX } = require('@vertis/allure-report/build/constants/prefix');
const { STATES } = require('@vertis/allure-report/build/types/states');

const CONTEXT_NAMES = {
    TYPE: 'TYPE',
    CLICK: 'CLICK',
    YMAPS_PINS: 'YMAPS_PINS',
    WAIT_FOR: 'WAIT_FOR'
};

const SCREENSHOTS_FOLDER_NAME = '__image_snapshots__';
const contexts = {};

const addBeforeFn = (ctx, original, beforeFn) => async(...args) => {
    await beforeFn.apply(ctx, args);

    return original.apply(ctx, args);
};

const wrapContextFn = (fn, contextType) => async(...args) => {
    contexts[contextType] = true;

    const result = await fn(...args);

    contexts[contextType] = false;

    return result;
};

const getElementScreenshot = async selector => {
    try {
        const element = await page.$(selector);

        return await element.screenshot();
    } catch (e) {}

    return null;
};

/* eslint-disable-next-line no-global-assign */
__allure__ = allure;

const { name: PROJECT_NAME } = require(join(process.cwd(), 'package.json'));

function getStoryLabelByState(suiteState) {
    if (suiteState === STATES.NEW) {
        return 'Новые тесты';
    } else if (suiteState === STATES.MODIFIED) {
        return 'Изменённые тесты';
    }
    return 'Неизменённые тесты';
}

// @FIXME надо договориться об именовании сьютов и унести в @vertis/allure-report
allure.reporter.addSuiteLabelsToTestCase = function(currentTest, testPath, testParentNames = []) {
    const suiteState = getState(testPath || '', PR_FILES || []);

    // группировка по наличию изменений в тесте
    currentTest.addLabel(LabelName.STORY, getStoryLabelByState(suiteState));

    const labelNames = [ LabelName.PARENT_SUITE, LabelName.SUITE ];
    const subSuiteNameParts = [];
    const [ , topParent, ...otherParents ] = testParentNames;
    const prefix = PREFIX[suiteState];
    const parents = topParent ? [ PROJECT_NAME, `${ prefix }${topParent}`, ...otherParents ] : [];

    parents.forEach((suiteName, index) => {
        const labelName = labelNames[index];

        if (labelName) {
            currentTest.addLabel(labelName, suiteName);
        } else {
            subSuiteNameParts.push(suiteName);
        }
    });

    if (subSuiteNameParts.length) {
        currentTest.addLabel(LabelName.SUB_SUITE, subSuiteNameParts.join('->'));
    }

    return currentTest;
};

allure.reporter.startTestCase = function(test, state, testPath) {
    if (this.currentSuite === null) {
        throw new Error('startTestCase called while no suite is running');
    }

    let testParentNames;

    if (test.parent) {
        testParentNames = this.collectTestParentNames(test.parent);
    }

    const fullName = [ PROJECT_NAME, ...testParentNames.slice(1), test.name ].join(' ');

    let currentTest = this.currentSuite.startTest(test.name);

    currentTest.fullName = fullName;

    currentTest.historyId = createHash('md5')
        .update(testPath + '.' + fullName)
        .digest('hex');
    currentTest.stage = Stage.RUNNING;

    if (state.parentProcess?.env?.JEST_WORKER_ID) {
        currentTest.addLabel(LabelName.THREAD, state.parentProcess.env.JEST_WORKER_ID);
    }

    currentTest = this.addSuiteLabelsToTestCase(currentTest, testPath, testParentNames || []);
    this.pushTest(currentTest);
};

allure.reporter.failTestCase = function(error) {
    if (this.currentTest === null) {
        throw new Error('failTestCase called while no test is running');
    }
    const latestStatus = this.currentTest.status;
    // If test already has a failed/broken state, we should not overwrite it
    const isBrokenTest = latestStatus === Status.BROKEN && this.currentTest.stage !== Stage.RUNNING;

    if (latestStatus === Status.FAILED || isBrokenTest) {
        return;
    }
    const { status, message, trace } = this.handleError(error);

    const rx = /See diff for details: (.*)/g;
    const arrMessage = rx.exec(stripAnsi(message));

    if (arrMessage) {
        const diffImage = fs.readFileSync(arrMessage[1]);

        allure.attachment('diff', diffImage, 'image/png');
        this.currentTest.addLabel(LabelName.STORY, 'Ошибки');
        // Включаем плагин для сравнения скриншотов (чтобы в отчете сразу показать изображение)
        this.currentTest.addLabel('testType', 'screenshotDiff');
    }

    this.currentTest.status = status;
    this.currentTest.statusDetails = { message, trace };
};

page.click = wrapContextFn(addBeforeFn(page, page.click, async selector => {
    const elementScreenshot = await getElementScreenshot(selector);

    allure.step(`Клик на элемент с селектором: ${selector}`, () => {
        allure.parameter('Селектор: ', selector);
        elementScreenshot && allure.attachment('Элемент', elementScreenshot, 'image/png');
    });
}), CONTEXT_NAMES.CLICK);

page.type = wrapContextFn(addBeforeFn(page, page.type, async(selector, text) => {
    const elementScreenshot = await getElementScreenshot(selector);

    allure.step(`Вводим текст в элемент с селектором: ${selector}`, () => {
        allure.parameter('Текст: ', text);
        allure.parameter('Селектор: ', selector);
        elementScreenshot && allure.attachment('Элемент', elementScreenshot, 'image/png');
    });
}), CONTEXT_NAMES.TYPE);

page.waitForYmapsPins = wrapContextFn(addBeforeFn(page, page.waitForYmapsPins, () => {
    allure.step('Дожидаемся отрисовки пинов на карте', () => {});
}), CONTEXT_NAMES.YMAPS_PINS);

page.keyboard.press = addBeforeFn(page.keyboard, page.keyboard.press, key => {
    if (contexts[CONTEXT_NAMES.TYPE]) {
        return;
    }

    allure.step(`Нажатие клавиши: ${key}`, () => {});
});

page.mouse.click = addBeforeFn(page.mouse, page.mouse.click, (x, y) => {
    if (contexts[CONTEXT_NAMES.CLICK]) {
        return;
    }

    allure.step(`Клик мыши по координатам: (${x}, ${y})`, () => {});
});

page.waitFor = wrapContextFn(addBeforeFn(page, page.waitFor, param => {
    if (contexts[CONTEXT_NAMES.YMAPS_PINS]) {
        return;
    }

    if (typeof param === 'number') {
        allure.step(`Ожидание: ${param} миллисекунд`, () => {});
    } else if (typeof param === 'string') {
        allure.step(`Ожидание элемента с селектором: ${param}`, () => {});
    } else if (typeof param === 'function') {
        allure.step('Ожидание выполнения условия заданного функцией', () => {
            allure.parameter('Функция: ', param.toString());
        });
    }
}), CONTEXT_NAMES.WAIT_FOR);

page.waitForTimeout = wrapContextFn(addBeforeFn(page, page.waitForTimeout, param => {
    if (contexts[CONTEXT_NAMES.YMAPS_PINS]) {
        return;
    }

    allure.step(`Ожидание: ${param} миллисекунд`, () => {});
}), CONTEXT_NAMES.WAIT_FOR);

page.waitForSelector = addBeforeFn(page, page.waitForSelector, selector => {
    if (contexts[CONTEXT_NAMES.YMAPS_PINS] || contexts[CONTEXT_NAMES.WAIT_FOR]) {
        return;
    }

    allure.step(`Ожидание элемента с селектором: ${selector}`, () => {});
});

page.hover = addBeforeFn(page, page.hover, async selector => {
    const elementScreenshot = await getElementScreenshot(selector);

    allure.step(`Наведение курсора на элемент с селектором: ${selector}`, () => {
        allure.parameter('Селектор: ', selector);
        elementScreenshot && allure.attachment('Элемент', elementScreenshot, 'image/png');
    });
});

page.focus = addBeforeFn(page, page.focus, async selector => {
    const elementScreenshot = await getElementScreenshot(selector);

    allure.step(`Фокус на элементе с селектором: ${selector}`, () => {
        allure.parameter('Селектор: ', selector);
        elementScreenshot && allure.attachment('Элемент', elementScreenshot, 'image/png');
    });
});

page.$eval = addBeforeFn(page, page.$eval, async(selector, fn) => {
    const elementScreenshot = await getElementScreenshot(selector);

    allure.step(`Выполнение функции над элементом с селектором: ${selector}`, () => {
        allure.parameter('Функция: ', fn.toString());
        elementScreenshot && allure.attachment('Элемент', elementScreenshot, 'image/png');
    });
});

expect.extend({
    toMatchImageSnapshot(received, options) {
        const result = toMatchImageSnapshotFn.call(this, received, {
            ...options,
            customSnapshotIdentifier
        });
        const viewport = page.viewport();

        const sizeStr = viewport ? ` (${ viewport.width }x${ viewport.height })` : '';
        const stepName = `Делаем снимок экрана${ sizeStr }`;

        const { currentTestName, testPath, snapshotState } = expect.getState();
        const counter = snapshotState._counters.get(currentTestName);
        const imagePath = customSnapshotIdentifier({
            currentTestName,
            counter
        });
        const ethalonPath = join(dirname(testPath), SCREENSHOTS_FOLDER_NAME, `${imagePath}-snap.png`);

        if (result.pass) {
            allure.step(stepName, () => {
                allure.attachment(imagePath, fs.readFileSync(ethalonPath), 'image/png');
            });
        } else {
            allure.attachment('actual', Buffer.from(received), 'image/png');
            allure.attachment('expected', fs.readFileSync(ethalonPath), 'image/png');
            const message = stripAnsi(result.message());
            const rx = /See diff for details: (.*)/g;
            const arrMessage = rx.exec(message);

            if (arrMessage) {
                const diffImage = fs.readFileSync(arrMessage[1]);

                allure.step(stepName, () => {
                    allure.attachment('diff', diffImage, 'image/png');
                });
            }
        }

        return result;
    }
});
