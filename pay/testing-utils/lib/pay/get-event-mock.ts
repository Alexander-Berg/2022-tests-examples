import { IConnectionMessage } from '@trust/utils/connections/types/connection-message';

/**
 * Returns stringified event for sending for `window.postMessage`
 *
 * @param {string} eventName
 * @param {string} orderId
 * @param {*} data
 * @returns {string}
 */
export function getEventMock<T>(eventName: string, orderId: string, data: T): string {
    const eventMock: IConnectionMessage<T> = {
        payload: {
            type: eventName,
            ...data,
        },
        channel: `YandexPay-${orderId}`,
        sourceUrl: 'http://localhost/',
    };

    return JSON.stringify(eventMock);
}
