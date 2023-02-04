describe('road_maxZoom', function() {
    it('should render properly after maxZoom', async function() {
        const browser = this.browser;
        await browser.openPanorama();
        await browser.execute(function() {
            window.engine.goto(37.542413, 55.709175);
        });
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('origin');
        await browser.execute(function() {
            window.camera.zoom = 1;
            window.camera.flush();
        });
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('zoom_01');
        await browser.execute(function() {
            window.camera.zoom = 2;
            window.camera.flush();
        });
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('zoom_02');
    });
});
