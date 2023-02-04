const pageObject = require('../../page-object.js'),
    credentials = require('../credentials'),
    { INIT_TIMEOUT, WAIT_FOR_LOGIN_FORM_TIMEOUT } = require('../../tools/constants');

/**
 * @name browser.ensureLoggedIn
 */
module.exports = function async(user) {
    const login = user && credentials[user].login,
        password = user && credentials[user].password;

    return this
        .debugLog('Logging in as ' + login)
        .waitForVisible(pageObject.userBarViewLoginBtn())
        .click(pageObject.userBarViewLoginBtn())
        .waitForVisible(pageObject.passpLoginForm(), WAIT_FOR_LOGIN_FORM_TIMEOUT).catch(() =>
            this.isVisible(pageObject.passpCurrentAccDispName())
                .then((isVisible) => isVisible?
                    this.pointerClick(pageObject.passpAuthHeaderLink())
                        .waitForVisible(pageObject.passpAccListSignInIcon())
                        .pointerClick(pageObject.passpAccListSignInIcon()) :
                    true
                )
        )
        .waitForVisible(pageObject.passpFieldLogin())
        .setValue(pageObject.passpFieldLogin(), login)
        .waitForVisible(pageObject.passpSignInBtn())
        .click(pageObject.passpSignInBtn.passpFormBtn())
        .waitForVisible(pageObject.passpCurrentAccDispName())
        .setValue(pageObject.passpFieldPasswd(), password)
        .waitForVisible(pageObject.passpSignInBtn())
        .click(pageObject.passpSignInBtn.passpFormBtn())
        .debugLog('Logged in, waiting for nmaps to load back')
        .waitForVisible(pageObject.ymaps(), INIT_TIMEOUT);
};
