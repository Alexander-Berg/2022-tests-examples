/**
 * @name browser.shouldBeEqual
 * @param actual
 * @param expected
 * @param {String} [message]
 */
module.exports = function(actual, expected, message = `Expected "${actual}" to equal "${expected}"`) {
    if(actual !== expected) {
        this.debugLog(message);
        throw new Error(message);
    }
    return true;
};
