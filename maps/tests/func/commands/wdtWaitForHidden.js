/**
 *
 * @name browser.wdtWaitForHidden
 * @param {String} selector
 * @param {Number} [timeout]
 * @param {String} [message]
 *
 */

module.exports = function (selector, timeout, message) {
    if (typeof timeout === 'string') {
        message = timeout;
    }

    if (typeof timeout !== 'number') {
        timeout = this.options.waitforTimeout;
    }

    message = (message ? message : 'Элемент виден после ' + timeout + 'ms') + `, селектор: ${selector}`;

    return this
        .waitForVisible(selector, timeout, true)
        .catch((e) => {
            assert.isFalse(message && e.type === 'WaitUntilTimeoutError', message);
            assert.isNotOk(e);
        });
};
