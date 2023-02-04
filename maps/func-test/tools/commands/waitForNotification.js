const pageObject = require('../../page-object.js');
const i18n = require('../../tools/i18n.js');
const { DEFAULT_LANG } = require('../../tools/constants');

/**
 * @name browser.waitForNotification
 * @param {String} notificationKey
 * @param {String} lang
 */
module.exports = function(notificationKey, lang = DEFAULT_LANG) {
    const text = i18n('editor', notificationKey, lang);

    return this
        .debugLog('Waiting for notification "' + text + '"')
        .waitForVisible(pageObject.notificationContent() + '=' + text)
        .debugLog('Notification is visible');
};
