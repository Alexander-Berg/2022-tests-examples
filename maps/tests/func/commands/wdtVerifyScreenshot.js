/**
 *
 * @name browser.wdtVerifyScreenshot
 * @param {String} selector
 * @param {String} filename
 * @param {Object} [options]
 */
module.exports = function (selector, filename, options = {tolerance: 10}) {
    return this.executionContext.browserId === 'edge' ? true :
        this.assertView(filename, selector, options);
};
