import cssSelectors from '../../common/css-selectors';
import getSelectorByText from './get-selector-by-text';

const testData = {
    comment: "Automatic test feedback message\nPlease don't reply",
    name: 'Йожин з бажин',
    email: 'geo.auto.test@yandex.ru'
};

const feedbackLabels = {
    // auto
    obstruction: 'Передвиньте метку, чтобы она указала на препятствие',
    prohibitingSign: 'Передвиньте метку на место, где запрещён манёвр',
    poorCondition: 'Передвиньте метку на участок маршрута с неровной дорогой',
    roadClosed: 'Передвиньте метку на участок маршрута, где нет дороги',

    // masstransit
    incorrectMasstransit: 'Передвиньте метку на участок маршрута, по которому нельзя проехать',

    // pedestrian
    incorrectPedestrian: 'Передвиньте метку на участок маршрута, по которому нельзя пройти',

    // bicycle
    incorrectBicycle: 'Передвиньте метку на участок маршрута, по которому нельзя проехать',

    // common
    other: 'Передвиньте метку на участок маршрута, где что-то не так',
    better: 'Нарисуйте на карте маршрут, которым вы пользуетесь'
} as const;

type FeedbackType = keyof typeof feedbackLabels | 'incorrect' | 'addObject';

type TestTypes<T> = Partial<Record<FeedbackType, T>>;
type TestMenuTree = TestTypes<string | {title: string; items: TestTypes<string>}>;

interface FeedbackTestSpec {
    skip?: boolean;
    only?: boolean;
    desc: string;
    parentPanel: 'routes';
    routePoint?: Point;
    precondition: (browser: WebdriverIO.Browser) => Promise<void>;
    backText: string;
    testMenu: TestMenuTree;
    additionalSelector?: string;
    depth?: number;
    withoutEmail: FeedbackType[];
}

async function checkFeedback(
    browser: WebdriverIO.Browser,
    feedbackType: FeedbackType,
    routePoint?: Point
): Promise<void> {
    await browser.waitAndClick(cssSelectors.routeFeedback.menu[feedbackType]);

    if (feedbackType === 'incorrect' || feedbackType === 'addObject') {
        return;
    }

    await browser.waitForVisible(cssSelectors.routeFeedback.buttons.submit.disabled);

    switch (feedbackType) {
        case 'better':
            await browser.waitForVisible(cssSelectors.feedback.header.helper);
            await browser.waitAndCheckValue(cssSelectors.feedback.header.helper, feedbackLabels[feedbackType]);
            await browser.perform(async () => {
                const [x, y] = await browser.getMapCenter();
                await browser.simulateClick({
                    x: x - 50,
                    y: y - 50,
                    description: ''
                });
            }, 'Кликнуть в произвольное место на карте');
            await browser.waitForVisible(cssSelectors.routeFeedback.routePoint);
            await browser.waitForVisible(cssSelectors.routeFeedback.buttons.submit.disabled);

            await browser.perform(async () => {
                const [x, y] = await browser.getMapCenter();
                await browser.simulateClick({
                    x: x + 50,
                    y: y + 50,
                    description: ''
                });
            }, 'Кликнуть в другое произвольное место на карте');
            await browser.waitForVisible(cssSelectors.routeFeedback.routePoint);
            break;

        default: {
            await browser.waitForVisible(cssSelectors.feedback.header.helper);
            if (routePoint) {
                await browser.simulateGeoClick({point: routePoint, description: 'Клик в нитку маршрута'});
            } else {
                await browser.clickInMap();
            }
            await browser.waitAndCheckValue(cssSelectors.feedback.header.helper, feedbackLabels[feedbackType]);
        }
    }
}

async function sendFeedback(
    browser: WebdriverIO.Browser,
    feedbackType: FeedbackType,
    withoutEmail: FeedbackType[],
    routePoint?: Point
): Promise<void> {
    await checkFeedback(browser, feedbackType, routePoint);
    await browser.waitForVisible(cssSelectors.routeFeedback.items.comment.default);
    await browser.setValueToInput(cssSelectors.routeFeedback.items.comment.default, testData.comment);
    if (!withoutEmail.includes(feedbackType)) {
        await browser.waitForVisible(cssSelectors.routeFeedback.items.email);
        await browser.setValueToInput(cssSelectors.routeFeedback.items.email, testData.email);
    }
    await browser.waitForVisible(cssSelectors.routeFeedback.items.photo);

    await browser.waitAndClick(cssSelectors.routeFeedback.buttons.submit.enabled);
    await browser.waitForVisible(cssSelectors.feedback.finish.view);
}

type ExitType = 'close' | 'backBeforeSending' | 'cancel' | 'continue';

// TODO: придумать, как проверять исчезновение нарисованной ломанной.
async function checkBetterRouteExit(
    browser: WebdriverIO.Browser,
    parentPanel: FeedbackTestSpec['parentPanel'],
    exitType: ExitType
): Promise<void> {
    switch (exitType) {
        case 'close':
            await browser.waitAndClick(cssSelectors.feedback.header.close);
            await browser.waitForHidden(cssSelectors.routeFeedback.buttons.submit.disabled);
            await browser.waitForHidden(cssSelectors.routeFeedback.view);
            break;
        case 'backBeforeSending':
            await browser.back();
            await browser.waitForHidden(cssSelectors.routeFeedback.buttons.submit.disabled);
            await browser.waitForVisible(cssSelectors.routeFeedback.view);
            break;
        case 'continue':
            await browser.waitAndClick(cssSelectors.feedback.finish.button);
            await browser.waitForHidden(cssSelectors.feedback.finish.view);
            await browser.waitForVisible(cssSelectors[parentPanel].sidebarPanel);
            break;
    }
}

async function checkExitPoint(
    browser: WebdriverIO.Browser,
    parentPanel: FeedbackTestSpec['parentPanel'],
    feedbackType: FeedbackType,
    exitType: ExitType
): Promise<void> {
    if (feedbackType === 'better') {
        await checkBetterRouteExit(browser, parentPanel, exitType);
        return;
    }
    switch (exitType) {
        case 'close':
            await browser.waitAndClick(cssSelectors.feedback.header.close);
            await browser.waitForHidden(cssSelectors.routeFeedback.form);
            await browser.waitForHidden(cssSelectors.routeFeedback.view);
            break;
        case 'backBeforeSending':
            await browser.back();
            await browser.waitForHidden(cssSelectors.routeFeedback.form);
            await browser.waitForVisible(cssSelectors.routeFeedback.view);
            break;
        case 'cancel':
            await browser.waitAndClick(cssSelectors.routeFeedback.buttons.cancel);
            await browser.waitForHidden(cssSelectors.routeFeedback.form);
            await browser.waitForVisible(cssSelectors[parentPanel].sidebarPanel);
            break;
        case 'continue':
            await browser.waitAndClick(cssSelectors.feedback.finish.button);
            await browser.waitForHidden(cssSelectors.feedback.finish.view);
            await browser.waitForVisible(cssSelectors[parentPanel].sidebarPanel);
            break;
    }
}
function run(spec: FeedbackTestSpec): void {
    if (spec.skip === true) {
        return;
    }

    const describeFn = spec.only ? describe.only : describe;
    describeFn(spec.desc, () => {
        beforeEach(function () {
            return spec.precondition(this.browser);
        });

        it(spec.backText, async function () {
            for (let i = 0; i <= (spec.depth ?? 0); i++) {
                await this.browser.back();
            }
            await this.browser.waitForVisible(cssSelectors[spec.parentPanel].sidebarPanel);
        });
        Object.entries(spec.testMenu).forEach(([type, menu]) => {
            if (typeof menu === 'object') {
                run({
                    ...spec,
                    desc: menu.title,
                    testMenu: menu.items,
                    depth: (spec.depth ?? 0) + 1,
                    precondition: async (browser) => {
                        await spec.precondition(browser);
                        await browser.waitAndClick(getSelectorByText(menu.title, undefined, {seekOccurrence: true}));
                    }
                });

                return;
            }

            const feedbackType = type as FeedbackType;
            describe(menu, () => {
                if (feedbackType === 'addObject') {
                    it('Переход по пунктам.', async function () {
                        await this.browser.waitAndClick(getSelectorByText(menu));
                    });

                    return;
                }

                it('Отправка фидбэка', function () {
                    return sendFeedback(this.browser, feedbackType, spec.withoutEmail, spec.routePoint);
                });

                describe('Выход из фидбэка без отправки', () => {
                    beforeEach(function () {
                        return checkFeedback(this.browser, feedbackType, spec.routePoint);
                    });

                    it('кнопкой "Закрыть"', async function () {
                        await checkExitPoint(this.browser, spec.parentPanel, feedbackType, 'close');
                    });

                    it('кнопкой "Назад"', async function () {
                        await checkExitPoint(this.browser, spec.parentPanel, feedbackType, 'backBeforeSending');
                    });
                });

                describe('Выход из фидбэка после отправки', () => {
                    beforeEach(function () {
                        return sendFeedback(this.browser, feedbackType, spec.withoutEmail, spec.routePoint);
                    });

                    it('кнопкой "Закрыть"', async function () {
                        await checkExitPoint(this.browser, spec.parentPanel, feedbackType, 'close');
                        if (spec.additionalSelector !== undefined) {
                            await this.browser.waitForVisible(spec.additionalSelector);
                        }
                    });
                });
            });
        });
    });
}

// eslint-disable-next-line jest/no-export
export {run};
