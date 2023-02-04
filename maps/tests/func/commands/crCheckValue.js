/**
 *
 * @name browser.crCheckValue
 * @param {String} selector
 * @param {String} expected
 * @param {String} error
 */
module.exports = function (selector, expected, error) {
    return this
        .getValue(selector).then((text) => assert.strictEqual(text, expected, error));
};
