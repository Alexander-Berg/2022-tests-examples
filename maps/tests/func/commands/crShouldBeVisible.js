const _ = require('lodash');

/**
 *
 * @name browser.crShouldBeVisible
 * @param {String} selector
 */
module.exports = function (selector) {
    return this
        .isVisible(selector)
        .then((isVisible) => {
            if (_.isArray(isVisible)) {
                throw new Error(`Найдено более одного элемента: ${selector}`);
            }
            assert.isTrue(isVisible, `Элемент с селектором ${selector} не виден!`);
        });
};
