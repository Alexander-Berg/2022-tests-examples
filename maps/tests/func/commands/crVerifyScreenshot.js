/**
 *
 * @name browser.crVerifyScreenshot
 * @param {String} selector
 * @param {String} filename
 * @param {Object} [options]
 */
module.exports = function (selector, filename, options) {
    return this.assertView(filename, selector, options);
};
