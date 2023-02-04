describe('hot-keys forward up: Panorama should move forward', function() {
    it('hot-keys forward up: Saint-Petersburg', async function() {
        const browser = this.browser;
        await browser.openPanorama();
        await browser.execute(function() {
            window.engine.gotoPOV('1254852232_626438697_23_1559389362');
        });
        await browser.waitUntilPanoramaLoaded();
        await browser.keys('Space');
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('ligovskiy_01');
        await browser.keys('Up arrow');
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('ligovskiy_02');
        await browser.keys('Up arrow');
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('ligovskiy_03');
        await browser.keys('W');
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('ligovskiy_04');
    });
});