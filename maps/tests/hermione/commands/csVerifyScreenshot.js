/**
 *
 * @name browser.mfrVerifyScreenshot
 * @param {String} selector
 * @param {String} filename
 * @param {Object} [options]
 */
module.exports = function (selector, filename, options) {
    return this
        .assertView(filename + '.' + this.executionContext.browserId, selector, options);
};