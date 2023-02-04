const { MAP_TOLERANCE: tolerance } = require('../../tools/constants');
const apiVersion = require('../../../src/configs/testing/ymaps').default.default.url.match(/\d.+/);
const groundPane = 'ymaps-' + apiVersion.toString().replace(/\D/g, '-') + 'ground-pane';

/**
 * @name browser.verifyMapScreenshot
 * @param {Number} left
 * @param {Number} top
 * @param {Number} width
 * @param {Number} height
 * @param {String} name
 * @param {Object} [options]
 */
module.exports = function(left, top, width, height, name, options = { tolerance }) {
    const screenshotStyle = `
        position: absolute;
        top: ${top}px;
        left: ${left}px;
        width: ${width}px;
        height: ${height}px;
        pointer-events: none;
    `,
        layerStyle = `
        position: absolute;
        z-index: 160;
        top: 0;
        left: 0;
        width: 100vw;
        height: 100vh;
        background: #000;
    `;

    return this
        //.waitForYMapsTilesLoaded()
        .setClassTilesLoaded()
        .debugLog(`Verifying map screenshot "${name}"...`)
        .execute(function(screenshotStyle, layerStyle, groundPane) {
            const screenshotElement = window.document.createElement('div'),
                layersContainer = window.document.getElementsByClassName(groundPane)[0],
                satCover = window.document.createElement('div');
            screenshotElement.setAttribute('id', 'map-cover');
            screenshotElement.setAttribute('style', screenshotStyle);
            window.document.body.appendChild(screenshotElement);
            satCover.setAttribute('style', layerStyle);
            satCover.setAttribute('id', 'sat-cover');
            layersContainer.appendChild(satCover);
        }, screenshotStyle, layerStyle, groundPane)
        .catch((err) => this.reportError(err))
        .waitForExist('div#map-cover')
        .assertView(name + '.chrome', 'div#map-cover', options)
        .debugLog(`Screenshot "${name}" is verified with tolerance ` + options.tolerance)
        .execute(function(groundPane) {
            const screenshotElement = window.document.getElementById('map-cover'),
                layersContainer = window.document.getElementsByClassName(groundPane)[0],
                satCover = window.document.getElementById('sat-cover');
            window.document.body.removeChild(screenshotElement);
            layersContainer.removeChild(satCover);
        }, groundPane)
        .catch((err) => this.reportError(err));
};
