describe('komarovka-rotated', function() {
    it('should render properly after rotation', async function() {
        const browser = this.browser;
        await browser.openPanorama();
        await browser.execute(function() {
            window.engine.goto(27.578323581917072, 53.92208867366644);
        });
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('origin');
        await browser.execute(function() {
            window.camera.azimuth += Math.PI / 4;
            window.camera.flush();
        });
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('rotated');
    });
});
