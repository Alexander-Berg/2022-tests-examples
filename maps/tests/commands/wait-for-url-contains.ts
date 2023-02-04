import {URL} from 'url';
import {wrapAsyncCommand} from '../lib/commands-utils';
import {defaultParams} from '../lib/func/url';
import cssSelectors from '../common/css-selectors';

type ExpectedQuery = Record<string, string | number | RegExp | ((actual: string | undefined) => boolean)>;

interface ExpectedUrl {
    path?: string;
    query?: ExpectedQuery;
}

interface ActualUrl {
    path: string;
    query: Record<string, string>;
}

interface UrlCheckOptions {
    partial?: true;
    timeout?: number;
    inverse?: boolean;
    keepBaseUrl?: boolean;
    skipUrlControlled?: boolean;
}

async function waitForUrlContains(this: WebdriverIO.Browser, template: ExpectedUrl, options: UrlCheckOptions = {}) {
    let errorMessage = 'Ошибка неизвестна.';

    if (!options.skipUrlControlled) {
        await this.waitForExist(cssSelectors.urlControlled);
    }

    await this.verifyUrl(template, {
        ...options,
        custom: {
            validate: (actual, expected) => {
                let result: ValidationResult;
                try {
                    result = validateUrl(actual, expected, options);
                } catch (e) {
                    result = {
                        error: true,
                        message: e.toString()
                    };
                }
                errorMessage = 'message' in result ? result.message : errorMessage;
                return !result.error;
            },
            onTimeout: () => errorMessage
        }
    });
}

interface SuccessValidationResult {
    error: false;
}

interface FailureValidationResult {
    error: true;
    message: string;
}

type ValidationResult = SuccessValidationResult | FailureValidationResult;

function validateUrl(actual: string, expected: ExpectedUrl, options: UrlCheckOptions = {}): ValidationResult {
    if (!expected.path && !expected.query) {
        throw new Error('Не задан шаблон для сверки');
    }

    const results: ValidationResult[] = [];

    const {path: actualPath, query: actualQuery} = parseUrl(actual, options.keepBaseUrl);

    if (expected.path) {
        results.push(comparePath(actualPath, expected.path, options));
    }

    if (expected.query) {
        results.push(compareQueries(actualQuery, expected.query, options));
    }

    return results.reduce<ValidationResult>(
        (memo, result) => {
            if (result.error) {
                return {
                    error: true,
                    message: 'message' in memo ? memo.message + '\n' + result.message : result.message
                };
            }
            return memo;
        },
        {error: false}
    );
}

function parseUrl(url: string, keepBaseUrl?: boolean): Required<ActualUrl> {
    const {pathname, searchParams} = new URL(url);
    [
        ...defaultParams,
        'mocks',
        'mocks[today]',
        'mocks[version]',
        'debug',
        'no-interstitial',
        'pron',
        'sctx',
        'no-vector',
        'loggers'
    ].forEach((key) => searchParams.delete(key));

    return {
        path: keepBaseUrl ? pathname : pathname.replace(/\/.*?\//, '/'),
        query: [...searchParams.entries()].reduce<ActualUrl['query']>((memo, [key, value]) => {
            memo[key] = value;
            return memo;
        }, {})
    };
}

function comparePath(actual: string, expected: string, options: UrlCheckOptions): ValidationResult {
    if (options.inverse) {
        if ((options.partial && !actual.includes(expected)) || (!options.partial && actual !== expected)) {
            return {
                error: false
            };
        }

        return {
            error: true,
            message: `Ожидается что путь не будет ${
                options.partial ? 'включать' : 'равен'
            } ${expected}\nТекущий путь: ${actual}`
        };
    }
    if ((options.partial && actual.includes(expected)) || (!options.partial && actual === expected)) {
        return {
            error: false
        };
    }

    return {
        error: true,
        message: `Ожидается что путь будет ${
            options.partial ? 'включать' : 'равен'
        } ${expected}\nТекущий путь: ${actual}`
    };
}

interface QueryValidationError {
    key: string;
    actual: string;
    expected: NonNullable<ExpectedUrl['query']>[string];
}

function compareQueries(
    actualQuery: ActualUrl['query'],
    expectedQuery: NonNullable<ExpectedUrl['query']>,
    options: UrlCheckOptions
): ValidationResult {
    const errors: QueryValidationError[] = Object.keys(options.partial ? expectedQuery : actualQuery).reduce<
        QueryValidationError[]
    >((memo, key) => {
        const actual = actualQuery[key];
        const expected = expectedQuery[key];
        const isValid = isValueValid(actual, expected);
        if (isValid !== Boolean(options.inverse)) {
            return memo;
        }
        return memo.concat({key, actual, expected});
    }, []);
    if (errors.length === 0) {
        return {error: false};
    }
    return {
        error: true,
        message: errors
            .map((error) => {
                const expectedStr = `${error.key}${options.inverse ? '!' : ''}=${error.expected}`;
                const actualStr = `${error.key}=${error.actual}`;
                if (error.actual && error.expected) {
                    return `Ожидалось: ${expectedStr}, получено ${actualStr}.`;
                }
                if (options.partial) {
                    return `Ожидалось: ${expectedStr}, в полученном результате ${
                        options.inverse ? 'такой параметр есть' : 'такого параметра нет'
                    }.`;
                }
                return `Получено: ${actualStr}, в ожидаемом шаблоне ${
                    options.inverse ? 'такой параметр есть' : 'такого параметра нет'
                }.`;
            })
            .join('\n')
    };
}

function isValueValid(actual: string, expected: NonNullable<ExpectedUrl['query']>[string]): boolean {
    if (typeof expected === 'function') {
        return expected(actual);
    }
    if (expected instanceof RegExp) {
        return expected.test(actual);
    }
    if (typeof expected === 'number') {
        return actual === expected.toString();
    }
    return actual === expected;
}

export {parseUrl, validateUrl, ActualUrl, ExpectedQuery, ExpectedUrl};
export default wrapAsyncCommand(waitForUrlContains);
