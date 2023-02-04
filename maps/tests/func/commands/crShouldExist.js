const _ = require('lodash');

/**
 *
 * @name browser.crShouldBeExist
 * @param {String} selector
 */
module.exports = function (selector) {
    return this
        .isExisting(selector)
        .then((isExisting) => {
            if (_.isArray(isExisting)) {
                throw new Error(`Найдено более одного элемента: ${selector}`);
            }
            assert.isTrue(isExisting, `Элемент с селектором ${selector} не существует!`);
        });
};
