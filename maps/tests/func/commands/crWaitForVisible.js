/**
 * Обёртка над стандартной командой waitForVisible. Позволяет указывать произвольное сообщение об ошибке.
 *
 * @name browser.crWaitForVisible
 * @param {String} selector - Селектор для элемента, появление которого нужно ждать
 * @param {Number|String} [timeout] - Таймаут в миллисекундах
 * @param {String} [message] - Сообщение об ошибке
 *
 */

module.exports = function (selector, timeout, message) {
    if (typeof timeout === 'string') {
        message = timeout;
    }

    if (typeof timeout !== 'number') {
        timeout = this.options.waitforTimeout;
    }

    message = (message ? message : 'Элемент не виден ' + timeout + 'ms') + `, селектор: ${selector}`;

    return this.waitForVisible(selector, timeout)
        .catch((e) => {
            assert.isFalse(message && e.type === 'WaitUntilTimeoutError', message);
            assert.isNotOk(e);
        });
};
