const EPSILON = 0.001,
    TIMEOUT = 5000;

/**
 * @name browser.waitForMapAt
 * @param coords
 */
module.exports = function async(coords) {
    return this
        .timeouts('script', TIMEOUT)
        .executeAsync(function(coords, EPSILON, done) {
            const map = window.diContainer.get('map'),
                verify = function() {
                    const center = map.getCenter({ useMargin: false });
                    if(Math.abs(center[0] - coords[0]) < EPSILON && Math.abs(center[1] - coords[1]) < EPSILON) {
                        map.un('bounds-changed', verify);
                        done();
                        return true;
                    }
                    return false;
                };

            verify() || map.on('bounds-changed', verify);
        }, coords, EPSILON)
        .catch(function(e) {
            if(e.seleniumStack && e.seleniumStack.type === 'ScriptTimeout') {
                throw new Error('Map is still not at [' + coords + '] after ' + TIMEOUT + ' ms');
            }
            throw new Error(e);
        });
};
