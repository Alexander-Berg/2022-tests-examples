const {WAIT_FOR_UPDATE} = require('../../constants');
const zooms = [8, 9, 10, 12, 14, 15.5, 16, 17, 18];

describe('Layers -traffic.show', () => {
    for (let i = 0; i < zooms.length; i++) {
        const zoom = zooms[i];
        it(`zoom : ${zoom}`, async ({browser}) => {
            await browser.openMap({center: [55.753930, 37.621401], zoom: zoom});
            await browser.execute(addTrafficControl).catch((err) => new Error(err));
            await browser.waitForTilesLoaded();
            await browser.pause(WAIT_FOR_UPDATE);
            await browser.verifyScreenshot('zoom-' + zoom, PO.map());
        });
    }

    function addTrafficControl() {
        const traffic = new ymaps.control.TrafficControl();
        myMap.controls.add(traffic);
        traffic.showTraffic();

        return true;
    }
});
