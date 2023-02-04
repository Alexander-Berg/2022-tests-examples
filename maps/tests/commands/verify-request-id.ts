import {wrapAsyncCommand} from '../lib/commands-utils';

type RequestIdType = 'ugc' | 'search' | 'stories' | 'router';

interface Verification {
    type?: RequestIdType;
    lastId?: string;
    amount?: number;
}

interface RequestIdLog {
    type: RequestIdType;
    id: string;
}

async function verifyRequestId(this: WebdriverIO.Browser, verification: Verification): Promise<void> {
    let errors: string[] = [];
    try {
        await this.waitUntil(
            async () => {
                const logs = await this.retrieveLogs({prefix: 'requestId'});
                const parsedLogs = parseLogs(logs);
                const nextRequestIds = await addRequestIds(this, parsedLogs);
                errors = getVerificationErrors(nextRequestIds, verification);
                return errors.length === 0;
            },
            undefined,
            'timeout'
        );
    } catch (e) {
        if (e.message === 'timeout') {
            // eslint-disable-next-line max-len
            throw new Error(`Ошибки верификации запросов:\n${errors.join('\n')}`);
        }
        throw e;
    }
}

function parseLogs(logs: string[]): RequestIdLog[] {
    return logs
        .map((log) => {
            try {
                return JSON.parse(log) as RequestIdLog;
            } catch (e) {
                return;
            }
        })
        .filter((x): x is RequestIdLog => Boolean(x));
}

async function addRequestIds(browser: WebdriverIO.Browser, idLogs: RequestIdLog[]): Promise<RequestIdLog[]> {
    const requestIds = ((await browser.getMeta('requestIds')) as RequestIdLog[] | undefined) || [];
    const nextIds = [...requestIds, ...idLogs];
    if (requestIds.length !== nextIds.length) {
        await browser.setMeta('requestIds', nextIds);
    }
    return nextIds;
}

/* eslint-disable max-len */
function getVerificationErrors(idLogs: RequestIdLog[], verification: Verification): string[] {
    const typedLogs = idLogs.filter((log) => log.type === (verification.type || 'search'));
    const errors = [];
    if (verification.lastId) {
        const lastLog = typedLogs[typedLogs.length - 1];
        const lastId = lastLog && lastLog.id;
        if (lastId !== verification.lastId) {
            errors.push(
                `Не совпадает id последнего запроса!\nОжидалось: ${verification.lastId}, на самом деле: ${lastId}`
            );
        }
    }
    if (typeof verification.amount === 'number' && verification.amount !== typedLogs.length) {
        errors.push(
            `Не совпадает количество запросов!\nОжидалось: ${verification.amount}, на самом деле: ${typedLogs.length}`
        );
    }
    return errors;
}
/* eslint-enable max-len */

export default wrapAsyncCommand(verifyRequestId);
