/**
 *
 * @name browser.csCheckText
 * @param {String} selector
 * @param {String} expected
 */
module.exports = function (selector, expected) {
    return this
        .getText(selector).then((text) => expected.should.equal(text));
};