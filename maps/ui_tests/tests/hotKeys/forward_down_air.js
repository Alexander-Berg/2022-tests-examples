describe('hot-keys forward down air: Panorama should move back', function() {
    it('hot-keys forward down: air', async function() {
        const browser = this.browser;
        await browser.openPanorama();
        await browser.execute(function() {
            window.engine.gotoPOV('1300002109_777104168_23_1595938769');
        });
        await browser.waitUntilPanoramaLoaded();
        await browser.keys('Down arrow');
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('down_air_01');
    });
});