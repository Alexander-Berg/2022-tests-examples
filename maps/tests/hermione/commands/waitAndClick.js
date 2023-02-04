/**
 * Дожидаемся видимости элемента и кликаем по нему
 *
 * @name browser.waitAndClick
 * @param {String} selector - селектор
 */
module.exports = function (selector) {
    return this
        .waitForVisible(selector)
        .click(selector);
};
