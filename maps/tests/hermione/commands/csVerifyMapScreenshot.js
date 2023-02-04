/**
 *
 * @name browser.csVerifyMapScreenshot
 * @param {String} selector
 * @param {String} filename
 * @param {Object} [options]
 */
module.exports = function (selector, filename, options = {tolerance: 20}) {
    return this.pause(1000)
        .waitTiles()
        .pause(200)
        .waitTiles()
        .pause(200)
        .waitTiles()
        .csVerifyScreenshot(selector, filename, options);
};
