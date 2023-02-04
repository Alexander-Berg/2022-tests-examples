/**
 * Returns event handler for given spy.
 * Handler should have signature "eventName", "handler"
 *
 * @param {jest.SpyInstance} spy
 * @param {string} eventName
 * @returns {*}
 */
export function getListener(spy: jest.SpyInstance, eventName: string): any {
    for (const [callEventName, handler] of spy.mock.calls) {
        if (callEventName !== eventName) {
            continue;
        }

        return handler;
    }

    throw new Error(`getListener: event "${eventName}" not found`);
}
