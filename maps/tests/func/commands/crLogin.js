const users = require('../credentials.js');

/**
 *
 * @name browser.crLogin
 * @param {String} user
 */
module.exports = function (user) {
    const passportVisibleError = 'Паспорт отображается после сабмита';
    const us = users[user];
    return this
        .waitForVisible(PO.passport())
        .waitForVisible(PO.passport.login(), 1000).catch(() =>
            this
                .isVisible(PO.passport.account())
                .then((isVisible) => isVisible ?
                    this
                        .click(PO.passport.account())
                        .waitForVisible(PO.passport.add())
                        .click(PO.passport.add()) :
                    this
                        .click(PO.passport.add())
                )
        )
        .waitForVisible(PO.passport.login())
        .setValue(PO.passport.login(), us.login)
        .click(PO.passport.submit())
        .waitForVisible(PO.passport.pass())
        .setValue(PO.passport.pass(), us.pass)
        .click(PO.passport.submit())
        .crWaitForHidden(PO.passport(), passportVisibleError)
        .catch(() =>
            this.isVisible(PO.passport.routeDone())
                .then((isVisible) => isVisible ?
                    this
                        .click(PO.passport.routeDone.back())
                        .crWaitForHidden(PO.passport(), passportVisibleError) :
                    this.crWaitForHidden(PO.passport(), 0, passportVisibleError)
                )
        );
};
