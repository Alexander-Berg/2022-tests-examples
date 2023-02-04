describe('hot-keys forward down: Panorama should move back', function() {
    it('hot-keys forward down: river', async function() {
        const browser = this.browser;
        await browser.openPanorama();
        await browser.execute(function() {
            window.engine.gotoPOV('1254639585_626412129_23_1530000162');
        });
        await browser.waitUntilPanoramaLoaded();
        await browser.keys('Down arrow');
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('river_01');
        await browser.keys('S');
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('river_02');
        await browser.keys('Down arrow');
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('river_03');
        await browser.keys('S');
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('river_04');
    });
});