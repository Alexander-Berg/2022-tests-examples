const {WAIT_FOR_UPDATE} = require('../../constants');

describe('Layers -switch layers', () => {
    it('Satellite – Hybrid – Map', async ({browser}) => {
        await browser.openMap({center: [55.753930, 37.621401], zoom: 15, controls: ['default']});
        await browser.waitForTilesLoaded();
        await browser.waitForVisible(PO.ymaps.layersBtn());
        await browser.click(PO.ymaps.layersBtn());
        await browser.waitForVisible(PO.ymaps.layersPanel());
        await browser.click(PO.yamapsLayerItem() + '=Спутник');
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyNoErrors();
        await browser.waitForTilesLoaded();
        await browser.verifyScreenshot('satellite', PO.map());
        await browser.deleteTilesLoaded();
        await browser.click(PO.ymaps.layersBtn());
        await browser.waitForVisible(PO.ymaps.layersPanel());
        await browser.click(PO.yamapsLayerItem() + '=Гибрид');
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyNoErrors();
        await browser.waitForTilesLoaded();
        await browser.verifyScreenshot('hybrid', PO.map());
        await browser.deleteTilesLoaded();
        await browser.click(PO.ymaps.layersBtn());
        await browser.waitForVisible(PO.ymaps.layersPanel());
        await browser.click(PO.yamapsLayerItem() + '=Схема');
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyNoErrors();
        await browser.waitForTilesLoaded();
        await browser.verifyScreenshot('vector', PO.map());
    });

    it('Panorama', async ({browser}) => {
        await browser.openMap({center: [55.753930, 37.621401], zoom: 15, controls: ['default']});
        await browser.waitForTilesLoaded();
        await browser.waitForVisible(PO.ymaps.layersBtn());
        await browser.click(PO.ymaps.layersBtn());
        await browser.waitForVisible(PO.ymaps.layersPanel());
        await browser.click(PO.yamapsLayerItem() + '=Панорамы');
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyNoErrors();
        await browser.waitForTilesLoaded();
        await browser.verifyScreenshot('panorama-layer-switch-on', PO.map());
        await browser.click(PO.ymaps.layersBtn());
        await browser.waitForVisible(PO.ymaps.layersPanel());
        await browser.click(PO.yamapsLayerItem() + '=Панорамы');
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyNoErrors();
        await browser.waitForTilesLoaded();
        await browser.verifyScreenshot('panorama-layer-switch-off', PO.map());
    });
});
