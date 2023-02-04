const {WAIT_FOR_UPDATE} = require('../../constants');

describe('Actions -switch vector', () => {
    it('first raster', async ({browser}) => {
        await browser.openMap({center: [60.3, 30.3], isVector: false});
        await browser.execute(addVectorControl, true).catch((err) => new Error(err));
        await browser.verifyNoErrors();
        await browser.waitForTilesLoaded();
        await browser.click(PO.map.controls.button1()) // vector;
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyNoErrors();
        await browser.deleteTilesLoaded();
        await browser.click(PO.map.controls.button1()) // raster;
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyNoErrors();
        await browser.deleteTilesLoaded();
        await browser.click(PO.map.controls.button1()) // vector;
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyNoErrors();
        await browser.waitForTilesLoaded();
        await browser.verifyScreenshot('vector-1', PO.map());
        await browser.click(PO.map.controls.button1()) // raster;
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyNoErrors();
        await browser.waitForTilesLoaded();
        await browser.verifyScreenshot('raster-1', PO.map());
    });

    it('first vector', async ({browser}) => {
        await browser.openMap({center: [60.3, 30.3], isVector: true});
        await browser.execute(addVectorControl, false).catch((err) => new Error(err));
        await browser.verifyNoErrors();
        await browser.waitForTilesLoaded();
        await browser.click(PO.map.controls.button1()) // raster;
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyNoErrors();
        await browser.click(PO.map.controls.button1()) // vector;
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyNoErrors();
        await browser.deleteTilesLoaded();
        await browser.click(PO.map.controls.button1()) // raster;
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyNoErrors();
        await browser.waitForTilesLoaded();
        await browser.verifyScreenshot('raster-2', PO.map());
        await browser.deleteTilesLoaded();
        await browser.click(PO.map.controls.button1()) // vector;
        await browser.pause(WAIT_FOR_UPDATE);
        await browser.verifyNoErrors();
        await browser.waitForTilesLoaded();
        await browser.verifyScreenshot('vector-2', PO.map());
    });

    function addVectorControl(isVector) {
        const opt = new ymaps.control.Button({
            data: {
                content: isVector ? 'vector' : 'raster'
            },
            options: {
                selectOnClick: true,
                maxWidth: [1000]
            }
        });

        opt.events
            .add('select', () => myMap.options.set('vector', !isVector))
            .add('deselect', () => myMap.options.set('vector', isVector));

        myMap.controls.add(opt);

        return true;
    }
});
