const pageObject = require('../../page-object.js'),
    { DEFAULT_TLD, INIT_TIMEOUT, INIT_RETRY_COUNT } = require('../../tools/constants');

/**
 * @name browser.prepareNmaps
 * @param {String} [tld]
 */
module.exports = function async(tld = DEFAULT_TLD) {
    const url = this.options.baseUrl + tld;
    return this
        .debugLog('Opening nmaps at ' + url)
        .retry(
            () => this.waitForVisible(pageObject.ymaps(), INIT_TIMEOUT),
            () => this.url(url),
            INIT_RETRY_COUNT)
        .debugLog('Map visible')
        .setClassTilesLoaded()
        .skipWelcomeScreen();
};
