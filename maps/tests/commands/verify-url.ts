import {wrapAsyncCommand} from '../lib/commands-utils';

interface UrlCheckOptions<T> {
    timeout?: number;
    inverse?: boolean;
    custom?: {
        validate?: (actual: string, expected: T) => boolean;
        onTimeout?: () => string | undefined;
    };
}

async function verifyUrl<T = string | RegExp>(
    this: WebdriverIO.Browser,
    expected: T,
    options: UrlCheckOptions<T> = {}
): Promise<void> {
    let lastUrl = 'unknown';
    const inverse = options.inverse || false;

    function defaultOnTimeout(): string | undefined {
        if (inverse) {
            return `Ожидается что url не будет равен ${expected}.`;
        }
        return `Ожидаемый url: ${expected}\nПолученный url: ${lastUrl}.`;
    }

    function defaultValidate<T>(actual: string, expected: T): boolean {
        let isValid;
        if (expected instanceof RegExp) {
            isValid = expected.test(actual);
        } else {
            isValid = actual === String(expected);
        }
        return isValid && !inverse;
    }

    try {
        await this.waitUntil(
            async () => {
                const actual = await this.getUrl();
                lastUrl = actual;
                return ((options.custom && options.custom.validate) || defaultValidate)(actual, expected);
            },
            options.timeout || undefined,
            'timeout'
        );
    } catch (err) {
        const onTimeout = (options.custom && options.custom.onTimeout) || defaultOnTimeout;
        const message = err.message === 'timeout' ? onTimeout() : err.message;
        throw new Error(message);
    }
}

export default wrapAsyncCommand(verifyUrl);
