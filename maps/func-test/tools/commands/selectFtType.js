const pageObject = require('../../page-object.js'),
    i18n = require('../../tools/i18n.js');

/**
 * @name browser.selectFtType
 * @param {String} ftTypeKey
 */
module.exports = function(ftTypeKey) {
    return this
        .debugLog('Selecting ftType "' + ftTypeKey + '"')
        .pointerClick(pageObject.geoObjEditorView.ftType())
        .waitForVisible(pageObject.nkPopup.menuFocused())
        .selectElementByTextInMenu(i18n('attr-values', ftTypeKey))
        .waitForInvisible(pageObject.nkPopup.menuFocused());
};
