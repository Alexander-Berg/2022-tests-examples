describe('khamovniki air', function() {
    it('should move camera by clicking air marker', async function() {
        const browser = this.browser;
        await browser.setViewportSize(1024, 768);
        await browser.openPanorama();
        await browser.execute(function() {
            window.engine.gotoPOV('1297800855_673458890_23_1519898207');
        });
        await browser.waitUntilPanoramaLoaded();
        // await browser.gridView();
        await browser.assertViewPanorama('origin');
        await browser.clickPanorama(430, 460);
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('after_move_on_air_panoram');
    });
});