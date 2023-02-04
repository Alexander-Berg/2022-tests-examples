const cleanup = require('../cleanup'),
    sessionNumber = require('../get-session-counter')(),
    { CLEANUP_LATITUDE_STEP } = require('../constants');

/**
 * @name browser.cleanupByTestNumber
 */
module.exports = function async() {
    return this
        .getMeta('testNumber').then((testNumber = 1) => {
            const centerPointX = testNumber * 0.01,
                centerPointY = CLEANUP_LATITUDE_STEP * sessionNumber * -1;

            return this
                .debugLog('Cleaning up vicinity of', [centerPointX, sessionNumber * -0.01])
                .then(() => cleanup([
                    [centerPointX - CLEANUP_LATITUDE_STEP, centerPointY],
                    [centerPointX + CLEANUP_LATITUDE_STEP, centerPointY],
                    [centerPointX + CLEANUP_LATITUDE_STEP, centerPointY * 2],
                    [centerPointX - CLEANUP_LATITUDE_STEP, centerPointY * 2]
                ]))
                .then((res) => {
                    this.debugLog('Cleanup complete, ' + res);
                    return true;
                });
        });
};
