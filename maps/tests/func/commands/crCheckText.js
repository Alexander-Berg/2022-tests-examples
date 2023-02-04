/**
 *
 * @name browser.crCheckText
 * @param {String} selector
 * @param {String} expected
 * @param {String} error
 */
module.exports = function (selector, expected, error) {
    return this
        .getText(selector).then((text) => assert.strictEqual(text, expected, error));
};
