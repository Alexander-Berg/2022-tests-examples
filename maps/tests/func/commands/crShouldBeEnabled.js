const _ = require('lodash');

/**
 *
 * @name browser.crShouldBeEnabled
 * @param {String} selector
 */
module.exports = function (selector) {
    return this
        .isEnabled(selector)
        .then((isEnabled) => {
            if (_.isArray(isEnabled)) {
                throw new Error(`Найдено более одного элемента: ${selector}`);
            }
            assert.isTrue(isEnabled, `Элемент с селектором ${selector} не включен!`);
        });
};
