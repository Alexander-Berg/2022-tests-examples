/*global Buffer*/
const { DEFAULT_TLD } = require('../../tools/constants');

/**
 * @name browser.ensureLoggedInFast
 * @param {Object} user – user from userData
 * @param {String} [tld]
 * @returns {Browser}
 */
module.exports = function(user, tld = DEFAULT_TLD) {
    const credentials = require('../credentials'),
        login = user && credentials[user].login,
        password = user && credentials[user].password,
        passportUrl = 'https://passport.yandex.' + tld + '/passport?mode=auth&retpath=' +
            encodeURIComponent(this.options.baseUrl + tld),
        timestamp = Math.round(new Date().getTime() / 1000),
        html = `
            <html>
                <form id='form' method="POST" action="${passportUrl}">
                    <input name="login" value="${login}">
                    <input name="passwd" value="${password}">
                    <input type="checkbox" name="twoweeks" value="no">
                    <input type="hidden" name="timestamp" value="${timestamp}">
                    <button id="submit" type="submit">Login</button>
                </form>
            <html>
        `;

    return this
        .debugLog('Fast logged in')
        .url('data:text/html;base64,' + Buffer.from(html).toString('base64'))
        .waitForVisible('#submit')
        .click('#submit');
};
