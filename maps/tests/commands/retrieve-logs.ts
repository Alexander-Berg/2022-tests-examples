import {wrapAsyncCommand} from '../lib/commands-utils';
import unescapeJs from 'unescape-js';

function filterLogs(messages: string[], type: string): string[] {
    const prefix = `[${type}]`;
    return messages.filter((message) => message.startsWith(prefix)).map((message) => message.slice(prefix.length + 1));
}

async function retrieveLogChunk(browser: WebdriverIO.Browser, level: string | string[]): Promise<string[]> {
    const entries = await browser.log('browser');
    const fnFilter = Array.isArray(level)
        ? (entry: WebdriverIO.LogEntry): boolean => level.includes(entry.level)
        : (entry: WebdriverIO.LogEntry): boolean => entry.level === level;

    return entries.filter(fnFilter).map((entry) => {
        const [, , /* source */ /* line */ ...message] = unescapeJs(entry.message).split(' ');
        return message.join(' ').slice(1, -1);
    });
}

type LogLevel = 'OFF' | 'SEVERE' | 'WARNING' | 'INFO' | 'DEBUG' | 'ALL';

interface RetrieveLogsOptions {
    level?: LogLevel | LogLevel[];
    prefix?: string;
}

// @see https://www.selenium.dev/selenium/docs/api/javascript/module/selenium-webdriver/lib/logging.html
// Remote API логирования: команда browser.log забирает все логи из браузера, очищая от них удаленную консоль.
async function retrieveLogs(this: WebdriverIO.Browser, options?: RetrieveLogsOptions): Promise<string[]> {
    const messages: string[] = [];
    await this.waitUntil(
        async () => {
            const nextMessages = await retrieveLogChunk(this, options?.level ?? 'DEBUG');
            messages.push(...nextMessages);
            return nextMessages.length === 0;
        },
        3000,
        'Too many browser logs',
        300
    );
    if (options?.prefix) {
        return filterLogs(messages, options.prefix);
    }
    return messages;
}

export default wrapAsyncCommand(retrieveLogs);
