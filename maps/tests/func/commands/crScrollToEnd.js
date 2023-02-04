/**
 * Скролит, выравнивая нижнюю границу элемента по нижней границе экрана.
 *
 * @name browser.crScrollToEnd
 * @param {String} selector
 * @param {Number} [additionalOffset]
 *
 * @returns {Promise}
 */
module.exports = function (selector, additionalOffset) {
    return this
        .execute(function execute(selector) {
            const rect = document.querySelector(selector).getBoundingClientRect();

            return document.body.scrollTop + rect.bottom - rect.height + (additionalOffset || 0);
        }, selector)
        .then((bottomLine) => this.crScroll(bottomLine.value));
};
