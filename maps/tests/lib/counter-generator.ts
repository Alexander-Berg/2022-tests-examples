import {OpenPageOptions} from '../commands/open-page';

const DEFAULT_URL = '?loggers=analytics';

const isCI = Boolean(process.env.CI);

interface SpecOptions {
    skip?: string;
    only?: boolean;
    isMainMetric?: boolean;
}

type EventType = 'click' | 'show' | 'hide' | 'change_state' | 'enter';

interface EventSpec extends SpecOptions {
    type: EventType;
    description?: string;
    options?: LogTestingOptions;
    state?: Record<string, string | boolean>;
    // Подготовка, логи которой не стираются.
    setup?: (browser: WebdriverIO.Browser) => Promise<void>;
}

interface CaseSpec extends SpecOptions {
    name: string | RegExp;
    description: string;
    events: EventSpec[];
    url?: string;
    openPageOptions?: OpenPageOptions;
    selector?: string;
    isMobile?: boolean;
    login?: boolean;
    // Подготовка, логи которой стираются.
    preparation?: (browser: WebdriverIO.Browser) => Promise<void>;
    // Подготовка, логи которой не стираются.
    setup?: (browser: WebdriverIO.Browser) => Promise<void>;
}

interface CaseSpecSet extends SpecOptions {
    name: string;
    specs: CaseSpec[];
}

function run(caseSpec: CaseSpec, options: SpecOptions): void {
    const isMainMetric = options.isMainMetric || caseSpec.isMainMetric;
    const fn = caseSpec.only ? describe.only : describe;
    if (caseSpec.only && isCI) {
        throw new Error(`Exclusive test in CI: ${caseSpec.description}`);
    }
    if (caseSpec.skip) {
        hermione.skip.in(/.*/, caseSpec.skip);
    }
    fn(caseSpec.description, () => {
        beforeEach(async function () {
            await this.browser.openPage(caseSpec.url || DEFAULT_URL, {
                isMobile: caseSpec.isMobile,
                userId: caseSpec.login ? 'common' : undefined,
                logAnalytics: true,
                ...caseSpec.openPageOptions
            });

            if (caseSpec.preparation) {
                await caseSpec.preparation(this.browser);
                // Если была подготовка - не берем в счет ее логи.
                await this.browser.retrieveLogs();
            }
            if (caseSpec.setup) {
                await caseSpec.setup(this.browser);
            }
        });
        caseSpec.events.forEach((event) => {
            const fn = event.only ? it.only : it;
            const description = getDescription(event);
            if (event.only && isCI) {
                throw new Error(`Exclusive test in CI: ${caseSpec.description} -> ${description}`);
            }
            if (event.skip) {
                hermione.skip.in(/.*/, event.skip);
            }
            fn(description, async function () {
                // Если событие `hide`, то сначала надо дождаться показа.
                if (caseSpec.selector && event.type === 'hide') {
                    await this.browser.waitForVisible(caseSpec.selector);
                }

                if (event.setup) {
                    await event.setup(this.browser);
                }

                if (caseSpec.selector) {
                    // Если событие `show`, то ждет элемент автоматически.
                    if (event.type === 'show') {
                        await this.browser.waitForVisible(caseSpec.selector);
                    }
                    // Если событие `click`, кликает автоматически.
                    if (event.type === 'click') {
                        await this.browser.waitAndClick(caseSpec.selector);
                    }
                }
                const options = event.options || {};

                const actual = parseAnalyticsLogs(await this.browser.retrieveLogs({prefix: 'analytics'}));

                const errors: string[] = [];
                const report = validateLogs({name: caseSpec.name, type: event.type, state: event.state}, actual);

                if (options.matchesAmount && report.matches !== options.matchesAmount) {
                    errors.push(
                        `Invalid number of matches.\nExpected: ${options.matchesAmount}, actual: ${report.matches}`
                    );
                }
                if (!options.matchesAmount && !options.multiple && report.matches > 1) {
                    errors.push(`Invalid number of matches.\nExpected: 1. Actual: ${report.matches}`);
                }
                if (report.matches < 1 && report.errors.length === 0) {
                    errors.push('No matches found');
                }

                errors.push(...report.errors);

                if (errors.length !== 0) {
                    const mainMetricksMessage = isMainMetric
                        ? 'Упал тест на основные метрики Карт. ' +
                          'https://a.yandex-team.ru/arc_vcs/maps/front/services/maps/tests/sets/counters/main-metrics/README.md\n'
                        : '';

                    const errorMessage = isRegexpUndefined(caseSpec.name)
                        ? 'RegExp is undefined'
                        : `Failed to validate ${caseSpec.name}:${event.type}:\n` + errors.join('\n');
                    throw new Error(`${mainMetricksMessage}${errorMessage}`);
                }
            });
        });
    });
}

interface CommonLogData {
    type: string;
}

interface ActualLog extends CommonLogData {
    name: string;
    state?: Record<string, string | boolean>;
}

interface ExpectedLog extends CommonLogData {
    name: string | RegExp;
    state?: Record<string, string | boolean | RegExp>;
}

interface LogCheckResult {
    matches: number;
    errors: string[];
}

interface LogTestingOptions {
    matchesAmount?: number;
    multiple?: boolean;
}

function parseAnalyticsLogs(messages: string[]): ActualLog[] {
    return messages
        .map<ActualLog | null>((log) => {
            const parts = log.match(/^([._a-z]+):([_\-a-z]+)(?:\((.*)\))?$/);
            if (!parts) {
                console.log('Warning!\nUnable to parse analytics log:', log);
                return null;
            }
            const [name, type, state] = parts.slice(1);
            if (!name || !type) {
                console.log('Warning!\nNo name or event type in log:', log);
                return null;
            }
            return {name, type, state: JSON.parse(state || '{}')};
        })
        .filter((log): log is ActualLog => Boolean(log));
}

function isRegexpUndefined(regexp: string | RegExp): boolean {
    return regexp instanceof RegExp && regexp.toString() === /(?:)/.toString();
}

function validateLogs(expected: ExpectedLog, actuals: ActualLog[]): LogCheckResult {
    return actuals.reduce<LogCheckResult>(
        (report, actual) => {
            if (
                expected.type !== actual.type ||
                // Защита от ложно-положительного срабатывания при expected.name === new RegExp(undefined)
                // new RegExp(undefined) возвращает валидный регексп /(?:)/, который матчится на любую строку.
                isRegexpUndefined(expected.name) ||
                (expected.name instanceof RegExp ? !expected.name.test(actual.name) : expected.name !== actual.name)
            ) {
                return report;
            }
            const stateErrors = getStateErrors(expected.state, actual.state);
            if (stateErrors.length === 0) {
                return {
                    ...report,
                    matches: report.matches + 1
                };
            }
            return {
                ...report,
                errors: [...report.errors, ...stateErrors]
            };
        },
        {
            matches: 0,
            errors: []
        }
    );
}

function getStateErrors(expectedState: ExpectedLog['state'], actualState: ActualLog['state']): string[] {
    if (!expectedState) {
        return [];
    }
    if (!actualState) {
        return [`No state found. Expected state: ${JSON.stringify(expectedState)}`];
    }
    return Object.entries(expectedState)
        .map<string | null>(([propName, expectedValue]) => {
            const actualValue = actualState[propName];
            if (expectedValue === '*') {
                if (typeof actualState === 'undefined') {
                    return `No state property found. Expected to have a '${propName}' prop.`;
                }
                return null;
            }
            if (
                expectedValue instanceof RegExp
                    ? !expectedValue.test(String(actualValue))
                    : expectedValue !== actualValue
            ) {
                return `'${propName}' property values are not equal.\nExpected: ${String(
                    expectedValue
                )}.\nActual: ${actualValue}`;
            }
            return null;
        })
        .filter((error): error is string => Boolean(error));
}

function getDescription(eventSpec: EventSpec): string {
    const TYPE_TO_STRING = {
        show: 'Показ',
        hide: 'Скрытие',
        click: 'Клик',
        change_state: 'Изменение состояние',
        enter: 'Поиск'
    };

    return `${TYPE_TO_STRING[eventSpec.type]}${eventSpec.description ? ` ${eventSpec.description}` : ''}`;
}

function counterGenerator(caseSet: CaseSpecSet): void {
    const fn = caseSet.only ? describe.only : describe;
    if (caseSet.only && isCI) {
        throw new Error(`Exclusive test in CI: ${caseSet.name}`);
    }
    if (caseSet.skip) {
        hermione.skip.in(/.*/, caseSet.skip);
    }
    fn(`Логирование. ${caseSet.name}`, () =>
        caseSet.specs.forEach((spec) => run(spec, {isMainMetric: caseSet.isMainMetric}))
    );
}

export default counterGenerator;
export {CaseSpec, EventSpec};
