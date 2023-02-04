const pageObject = require('../../page-object.js');

/**
 * @name browser.skipWelcomeScreen
 */
module.exports = function async() {
    return this.isVisible(pageObject.welcomeScreenContent()).then(isWelcomeScreenVisible => {
        return !isWelcomeScreenVisible || this
            .debugLog('Skipping welcome screen')
            .pointerClick(pageObject.welcomeScreenFooter.startBtn())
            .waitForInvisible(pageObject.welcomeScreenContent())
            .execute(() => {
                window.diContainer.get('navigator').goToDefault();
            });
    });
};
