const _ = require('lodash');

/**
 *
 * @name browser.crShouldNotBeEnabled
 * @param {String} selector
 */
module.exports = function (selector) {
    return this
        .isEnabled(selector)
        .then((isEnabled) => {
            if (_.isArray(isEnabled)) {
                throw new Error(`Найдено более одного элемента: ${selector}`);
            }
            assert.isFalse(isEnabled, `Элемент с селектором ${selector} включен!`);
        });
};
