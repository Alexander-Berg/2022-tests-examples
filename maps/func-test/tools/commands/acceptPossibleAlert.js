/**
 * @name browser.acceptPossibleAlert
 */
module.exports = function async() {
    return this
        .debugLog('Accept possible alert')
        .alertAccept()
        .then(() => this.debugLog('Alert is accepted'), () => true);
};
