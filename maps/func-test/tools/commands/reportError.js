/**
 * @name browser.reportError
 * @param {String} text
 */
module.exports = function async(text) {
    throw new Error(text);
};
