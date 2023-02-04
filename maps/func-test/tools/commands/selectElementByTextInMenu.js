const pageObject = require('../../page-object.js'),
    { ANIMATION_TIMEOUT } = require('../../tools/constants');

/**
 * @name browser.selectElementByTextInMenu
 * @param {String} text
 */
module.exports = function(text) {
    return this
        .debugLog('Text of item "' + text + '"')
        .isVisible(pageObject.popup.menuFocused())
        .then(isVisible => isVisible?
            this
                .element(pageObject.popup.menuFocused())
                .click(pageObject.menuItem() + '*=' + text) :
            this
                .element(pageObject.nkPopup.menu())
                .click(pageObject.nkMenuItem() + '*=' + text)
        )
        .pause(ANIMATION_TIMEOUT);
};
