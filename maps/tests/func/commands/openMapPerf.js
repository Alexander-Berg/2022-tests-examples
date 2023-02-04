/* global Buffer */

const isVector = Boolean(Number(process.env.VECTOR_ALL || process.env.VECTOR));
const path = require('path');
const fs = require('fs');
const isPerformanceStartTime = process.env.GET_TILES_LOADED_TIME;
const {WAIT_FOR_MAP} = require('../constants');
const defaults = {
    center: [0, 0],
    zoom: 5,
    controls: [],
    filename: 'basic',
    isVector: isVector,
    secondCenter: [53.907177, 27.558434]
};

/**
 * @name browser.openMapPerf
 * @param {Object} options
 */
module.exports = async function (options) {
    options = Object.assign({}, defaults, options);
    const {filename, center, zoom, controls, isVector, secondCenter} = options;
    const filePath = path.resolve('./templates/' + filename + '.html');
    const fileContent = getContent(filePath);
    const base64 = Buffer.from(fileContent).toString('base64');
    let startTime;
    let endTime;

    await this.url('data:text/html;base64,' + base64);
    await this.setMeta('center', center.join(', '));
    await this.setMeta('zoom', zoom);
    await this.prepareMap(options);
    startTime = await this.execute(() => performance.now());
    await this.execute((isVector, center, zoom, controls, classTilesLoaded) => {
        ymaps.ready(init);

        function init() {
            const body = document.body;
            const myMap = new ymaps.Map('map',
                {
                    center: center,
                    zoom: zoom,
                    controls: controls
                }, {
                    searchControlProvider: 'yandex#search',
                    vector: isVector,
                    layerVectorRevealThreshold: 0,
                    suppressMapOpenBlock: true
                }
            );

            myMap.layers.events.add('tileloadchange', (event) => {
                if (event.get('readyTileNumber') === event.get('totalTileNumber')) {
                    body.setAttribute('endTime', performance.now());
                    body.classList.add(classTilesLoaded);
                } else {
                    body.classList.remove(classTilesLoaded);
                }
            });

            window.myMap = myMap;

            return myMap;
        }

        return true;
    }, isVector, center, zoom, controls, PO.tilesLoaded());
    await this.waitForVisible(PO.map(), WAIT_FOR_MAP);
    await this.waitForTilesLoaded();
    endTime = await this.getAttribute('body', 'endTime');
    await this.setMeta('tilesLoaded', endTime - startTime);
    if (isPerformanceStartTime) {
        await this.saveTileLoadedTime(endTime - startTime);
    }
    await this.deleteTilesLoaded();
    await this.pause(1500);
    startTime = await this.execute(() => performance.now());
    await this.execute((secondCenter) => myMap.setCenter(secondCenter), secondCenter)
    await this.waitForTilesLoaded();
    endTime = await this.getAttribute('body', 'endTime');
    await this.setMeta('tilesLoaded-2', endTime - startTime);
    if (isPerformanceStartTime) {
        await this.saveTileLoadedTime(endTime - startTime, 2);
    }
    await this.verifyNoErrors();
    if (isPerformanceStartTime) {
        throw new Error('Stop test');
    }

    function getContent(filePath) {
        return fs.readFileSync(filePath, 'utf8', (err, data) => {
            if (err) {
                throw new Error(err);
            }

            return data;
        });
    }
};
