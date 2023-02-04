const TIMEOUT = 5000;

/**
 * @name browser.setMapCenter
 * @param {Object[]} center
 * @param {Number} zoom
 * @param {Object} [options]
 */
module.exports = function async(center, zoom, options) {
    return this
        .timeouts('script', TIMEOUT)
        .executeAsync(function(center, zoom, options, done) {
            const map = window.diContainer.get('map');
            map.setCenter(center, zoom, options).then(() => done(true));
        }, center, zoom, options)
        .then(() => this.debugLog('Map center moved to', center));
};
