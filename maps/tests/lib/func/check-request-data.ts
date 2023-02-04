import {isEqual, omit, isPlainObject} from 'lodash';
import {updatedDiff, addedDiff} from 'deep-object-diff';
import {HTTPRequest} from 'puppeteer-core/lib/cjs/puppeteer/common/HTTPRequest';

const checkRequestData = async (
    browser: WebdriverIO.Browser,
    expectedData: Object,
    actualData: Object,
    prepareData: (data: Object) => Object = (data) => data
) => {
    await browser.perform(async () => {
        await browser.waitUntil(
            () => {
                return Promise.resolve(Object.keys(actualData).length > 0);
            },
            undefined,
            'Запрос не был отправлен по неизвестной причине'
        );

        const preparedExpectedData = prepareData(expectedData);
        const preparedActualData = prepareData(actualData);

        await browser.waitUntil(
            () => Promise.resolve(isEqual(preparedExpectedData, preparedActualData)),
            undefined,
            getErrorMessage(preparedExpectedData, preparedActualData)
        );
    }, 'Дождаться пока отправится запрос.');
};

function getErrorMessage(expectedPostData: Object, actualPostData: Object): string {
    const deltaAdd = addedDiff(expectedPostData, actualPostData);
    const deltaDelete = addedDiff(actualPostData, expectedPostData);
    const deltaUpd = updatedDiff(expectedPostData, actualPostData);

    const getPrettyJson = (data: object) => JSON.stringify(data, null, 4);

    return [
        'Отправляемые данные отличаются от эталона ->',
        'ожидалось:',
        JSON.stringify(expectedPostData),
        '',
        'получено:',
        JSON.stringify(actualPostData),
        '',
        'В полученных данных:',
        Object.keys(deltaAdd).length > 0 ? `Добавили:\n${getPrettyJson(deltaAdd)}\n` : '',
        Object.keys(deltaDelete).length > 0 ? `Удалили:\n${getPrettyJson(deltaDelete)}\n` : '',
        Object.keys(deltaUpd).length > 0 ? `Модифицировали:\n${getPrettyJson(deltaUpd)}` : ''
    ].join('\n');
}

function roundPoints<T extends unknown>(input: T, toFixed: number): T {
    if (isPoint(input)) {
        return input.map((num) => num?.toFixed(toFixed)) as T;
    }

    if (Array.isArray(input)) {
        return input.map((item) => roundPoints(item, toFixed)) as T;
    }

    if (isPlainObject(input)) {
        return Object.entries(input as {}).reduce<Record<string, unknown>>((result, [key, value]) => {
            result[key] = roundPoints(value, toFixed);
            return result;
        }, {}) as T;
    }

    return input;
}

function isPoint(input: unknown): input is Point {
    if (!Array.isArray(input) || input.length !== 2) {
        return false;
    }
    return input.every((num) => Number.isFinite(num));
}

async function getClearPostData(request: HTTPRequest): Promise<Object> {
    return omit(await JSON.parse(request.postData()!), [
        'csrfToken',
        'sessionId',
        'ajax',
        'no-mail',
        'no-cache',
        'debug',
        'mocks',
        'fingerprint'
    ]);
}

export default checkRequestData;
// eslint-disable-next-line jest/no-export
export {roundPoints, getClearPostData};
