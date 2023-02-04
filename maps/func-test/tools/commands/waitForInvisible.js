/**
 * @name browser.waitForInvisible
 * @param {String} selector
 * @param {Number} [timeout]
 */
module.exports = function async(selector, timeout = null) {
    return this.waitForVisible(selector, timeout, true);
};
