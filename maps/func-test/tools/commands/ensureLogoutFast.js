const pageObject = require('../../page-object.js'),
    { ALERT_TIMEOUT } = require('../constants');

/**
 * @name browser.ensureLogoutFast
 * @param {Boolean} [isTrue]
 */
module.exports = function async(isTrue) {
    return isTrue?
        this
            .debugLog('Logging out')
            .pointerClick(pageObject.userIcon())
            .waitForVisible(pageObject.nkPopup.menu())
            .pointerClick(pageObject.userLogoutLink()) // log out
            .acceptPossibleAlert()
            .debugLog('Logged out, waiting for unauthenticated page"')
            .waitForVisible(pageObject.unauthenticatedView()) :
        this
            .isVisible(pageObject.geoObjEditorView.close())
            .then(cancelBtn => cancelBtn?
                this
                    .click(pageObject.geoObjEditorView.close())
                    .pause(ALERT_TIMEOUT)
                    .acceptPossibleAlert() :
                true)
            .debugLog('Fast logging out – delete cookies')
            .deleteCookie()
            .catch(() => this.debugLog('Something went wrong..'));
};
