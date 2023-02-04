/* global Buffer */

const isVector = Boolean(Number(process.env.VECTOR_ALL || process.env.VECTOR));
const isNight = process.env.NIGHT;
const isHybrid = process.env.HYBRID;

const path = require('path');
const fs = require('fs');
const {WAIT_FOR_MAP} = require('../constants');
const defaults = {
    center: [0, 0],
    zoom: 5,
    controls: [],
    filename: 'basic',
    isVector: isVector
};

/**
 * @name browser.openMap
 * @param {Object} options
 */
module.exports = async function (options) {
    options = Object.assign({}, defaults, options);
    const {filename, center, zoom, controls, isVector} = options;
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
    await this.execute((isVector, center, zoom, controls, isNight, isHybrid, classTilesLoaded) => {
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
                    suppressMapOpenBlock: true,
                    layerVectorMode: isNight ? 'night' : '',
                    layerVectorHover: false,
                    layerVectorColliding: false // @see https://st.yandex-team.ru/VECTOR-763
                }
            );
            if (isHybrid) {
                myMap.setType('yandex#hybrid', {checkZoomRange: true});
            }

            myMap.layers.events.add('tileloadchange', (event) => {
                if (event.get('readyTileNumber') === event.get('totalTileNumber')) {
                    body.classList.add(classTilesLoaded);
                } else {
                    body.classList.remove(classTilesLoaded);
                }
            });

            window.myMap = myMap;

            return myMap;
        }

        return true;
    }, isVector, center, zoom, controls, isNight, isHybrid, PO.tilesLoaded());
    await this.waitForVisible(PO.map(), WAIT_FOR_MAP);
    await this.waitForTilesLoaded();
    endTime = await this.execute(() => performance.now())
    await this.setMeta('tilesLoaded', (endTime - startTime).toFixed(2));
    await this.verifyNoErrors();
    await this.pause(1000);

    function getContent(filePath) {
        return fs.readFileSync(filePath, 'utf8', (err, data) => {
            if (err) {
                throw new Error(err);
            }

            return data;
        });
    }
};
