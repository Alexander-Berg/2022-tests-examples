/**
 *
 * @name browser.crShouldNotExist
 * @param {String} selector
 */
module.exports = function (selector) {
    return this
        .isExisting(selector)
        .then((isExisting) => assert.isFalse(isExisting, `Элемент с селектором ${selector} существует!`));
};
