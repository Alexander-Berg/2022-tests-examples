describe('hot-keys forward down-up indoor: Panorama should move', function() {
    it('hot-keys forward down-up: indoor', async function() {
        const browser = this.browser;
        await browser.openPanorama();
        await browser.execute(function() {
            window.engine.gotoPOV('1298139264_673167564_23_1569228128');
        });
        await browser.waitUntilPanoramaLoaded();
        await browser.keys('Down arrow');
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('down_up_indoor_01');
        await browser.execute(function() {
            window.camera.azimuth += 2.5;
            window.camera.flush();
        });
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('down_up_indoor_02');
        await browser.keys('Space');
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('down_up_indoor_03');
        await browser.keys('Up arrow');
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('down_up_indoor_04');
    });
});