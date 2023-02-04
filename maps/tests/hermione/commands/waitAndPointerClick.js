/**
 * Дожидаемся видимости элемента и кликаем по нему
 *
 * @name browser.waitAndPointerClick
 * @param {String} selector - селектор
 */
module.exports = function (selector) {
    return this
        .waitForVisible(selector)
        .pointerClick(selector);
};