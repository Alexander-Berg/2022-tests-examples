/**
 * @name browser.verifyHrefValue
 * @param {String} selector
 * @param {String} url
 */
module.exports = function(selector, url) {
    return this
        .getAttribute(selector, 'href')
        .then((hrefUrl) => this.shouldBeEqual(hrefUrl, url))
        .debugLog('Url is expected');
};
