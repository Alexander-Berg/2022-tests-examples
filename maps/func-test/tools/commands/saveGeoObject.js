const pageObject = require('../../page-object.js'),
    { VALIDATOR_TIMEOUT, COMMENTS_TIMEOUT, WAIT_FOR_NOTIFICATION_SUCCESS } = require('../../tools/constants');

/**
 * @name browser.saveGeoObject
 */
module.exports = function() {
    return this
        .debugLog('Saving...')
        .waitForInvisible(pageObject.notificationError()) // To avoid failing due to previous error notification
        .waitForInvisible(pageObject.submitDisabled(), VALIDATOR_TIMEOUT)
        .catch(() => this.reportError('Save button is still disabled at a moment we expect to save a geoobject'))
        .pointerClick(pageObject.geoObjEditorView.submit())
        .waitForVisible(pageObject.notification()).catch(() => true)
        .then(checkAndReportSavingError)
        .waitForVisible(pageObject.notificationSuccess(), WAIT_FOR_NOTIFICATION_SUCCESS)
        .catch((err) => checkAndReportSavingError.call(this).then(() => this.reportError(err.message)))
        .waitForVisible(pageObject.geoObjViewerView())
        .waitForVisible(pageObject.geoObjViewerView.commentsLink())
        .pause(COMMENTS_TIMEOUT)
        .debugLog('Saved');
};

function checkAndReportSavingError() {
    return this
        .isVisible(pageObject.notificationError())
        .then(isError => isError?
            this.getText(pageObject.notificationContent())
                .then(text => this.reportError('Error while saving: ' + text)) :
            true
        );
}
