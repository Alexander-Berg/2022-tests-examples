const pageObject = require('../../page-object.js');

/**
 * @name browser.setClassTilesLoaded
 */
module.exports = function() {
    return this
        .executeAsync((classTilesLoaded, done) => {
            const map = window.diContainer.get('map').getYMap(),
                body = document.body;

            body.classList.add(classTilesLoaded);

            map.layers.events.add('tileloadchange', (event) => {
                if(event.get('readyTileNumber') === event.get('totalTileNumber')) {
                    body.classList.add(classTilesLoaded);
                } else {
                    body.classList.remove(classTilesLoaded);
                }
            });
            done(true);
        }, pageObject.tilesLoaded())
        .then(() => this.debugLog('Event listener for tiles loaded is set'));
};
