const sessionNumber = require('../get-session-counter')();

/**
 * @name browser.setMapCenterByTestNumber
 * @param {Number} [zoom]
 */
module.exports = function async(zoom = 18) {
    return this.getMeta('testNumber').then((testNumber = 1) =>
        this.setMapCenter([testNumber * 0.01, sessionNumber * -0.01], zoom, { useMargin: false }));
};
