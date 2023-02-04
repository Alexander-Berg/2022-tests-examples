/* global Buffer */

const isVector = true;
const isNight = process.env.NIGHT;
const vectorBundle =  process.env.VECTOR_BUNDLE

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
    const {filename, center, zoom, controls, isVector, custom} = options;
    const filePath = path.resolve('./templates/' + filename + '.html');
    const fileContent = getContent(filePath);
    const base64 = Buffer.from(fileContent).toString('base64');
    let startTime;
    let endTime;

    const apiUrl = `https://api-maps.tst.c.maps.yandex.ru/2.1-dev/?lang=ru_RU&mode=debug&host_config[hosts][vectorIndex]=https://yastatic.net/s3/mapsapi-v3/vector/${vectorBundle}/out/vector.min.js`;

    await this.url('data:text/html;base64,' + base64);
    await this.setMeta('center', center.join(', '));
    await this.setMeta('zoom', zoom);
    await this.setMeta('customization', JSON.stringify(custom));
    await this.setMeta('apiUrl', apiUrl);
    await this.execute((apiUrl) => {
        const head = document.getElementsByTagName('head')[0];
        const script = document.createElement('script');

        script.type = 'text/javascript';
        script.charset = 'utf-8';
        script.src = apiUrl;

        head.appendChild(script);

        return true;
    }, apiUrl);
    startTime = await this.execute(() => performance.now());
    await this.execute((isVector, center, zoom, controls, isNight, classTilesLoaded, custom) => {;
        ymaps.ready(init);

        function init() {
            const body = document.body;
            const myMap = new ymaps.Map('map',
                {
                    center: center,
                    zoom: zoom
                }, {
                    vector: isVector,
                    layerVectorMode: isNight ? 'night' : '',
                    layerVectorCustomization: custom
                }
            );

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
    }, isVector, center, zoom, controls, isNight, PO.tilesLoaded(), custom);
    await this.waitForVisible(PO.map(), WAIT_FOR_MAP);
    await this.waitForTilesLoaded();
    endTime = await this.execute(() => performance.now());
    await this.setMeta('tilesLoaded', (endTime - startTime).toFixed(2));
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
