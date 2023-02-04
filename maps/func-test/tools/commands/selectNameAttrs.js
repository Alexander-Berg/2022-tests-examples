const pageObject = require('../../page-object.js'),
    { DEFAULT_LANG } = require('../../tools/constants'),
    i18n = require('../../tools/i18n.js');

/**
 * @name browser.selectNameAttrs
 * @param {String} [langKey = DEFAULT_LANG]
 * @param {String} [typeKey]
 * @param {Number} [nameIndex = 1] - Item name sequence number
 */
module.exports = function(langKey = DEFAULT_LANG, typeKey, nameIndex = 1) {
    return this
        .debugLog('Selecting lang of name ' + langKey)
        .isVisible(pageObject.fieldsetLang())
        .then(isVisible => isVisible?
            this.pointerClick(pageObject.listCtrlItem() + ':nth-child(' + (nameIndex + 1) + ') ' + pageObject.fieldsetLang()) :
            this.pointerClick(pageObject.listCtrlItem() + ':nth-child(' + nameIndex + ') ' + pageObject.addrNameLang())
        )
        .waitForVisible(pageObject.nkPopup.menuFocused())
        .selectElementByTextInMenu(i18n('attr-values', 'lang__' + langKey))
        .waitForInvisible(pageObject.nkPopup.menuFocused())
        .then(() => typeKey?
            this
                .debugLog('Selecting type of name')
                .pointerClick(pageObject.listCtrlItem() + ':nth-child(' + (nameIndex + 1) + ') ' + pageObject.fieldsetType())
                .waitForVisible(pageObject.nkPopup.menuFocused())
                .selectElementByTextInMenu(i18n('attr-values', 'name_type__' + typeKey))
                .waitForInvisible(pageObject.nkPopup.menuFocused()) :
            true
        );
};
