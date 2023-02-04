const isFast = !!process.env.FAST;
const { DEFAULT_TLD } = require('../../tools/constants');

/**
 *
 * @name browser.initNmaps
 * @param {String} [user]
 * @param {String} [tld]
 */
module.exports = function async(user, tld = DEFAULT_TLD) {
    return user?
        this
            .ensureLogoutFast()
            .then(() => isFast? this.cleanupByTestNumber() : true)
            .ensureLoggedInFast(user, tld)
            .setLocalStorage()
            .prepareNmaps(tld)
            .setMapCenterByTestNumber() :
        this
            .ensureLogoutFast()
            .prepareNmaps(tld);
};
