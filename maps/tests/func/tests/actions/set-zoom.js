describe('Actions -setZoom', () => {
    for (let zoom = 5; zoom < 10; zoom++) {
        zoom = Math.round(zoom * 100) / 100;
        it(`zoom-${zoom}`, async ({browser}) => {
            await browser.openMap({center: [-33.920100, 18.476594], zoom: 19});
            await browser.verifyNoErrors();
            await browser.waitForTilesLoaded();
            await browser.deleteTilesLoaded();
            await browser.execute((zoom) => myMap.setZoom(zoom), zoom);
            await browser.waitForTilesLoaded();
            await browser.verifyScreenshot(`zoom-${zoom}`, PO.map());
            await browser.verifyNoErrors();
        });
    }
});
