/**
 * Дожидаемся чтобы элемент пропал
 *
 * @name browser.waitForInvisible
 * @param {String} selector - селектор
 */
module.exports = function (selector) {
    return this.waitForVisible(selector, null, true);
};