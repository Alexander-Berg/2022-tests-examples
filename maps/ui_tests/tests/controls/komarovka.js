describe('komarovka', function() {
    it('should move camera by clicking thoroughfare pointers', async function() {
        const browser = this.browser;
        await browser.setViewportSize(1024, 768);
        await browser.openPanorama();
        await browser.execute(function() {
            window.engine.goto(27.578323581917072, 53.92208867366644);
        });
        await browser.waitUntilPanoramaLoaded();
        // await browser.gridView();
        await browser.assertViewPanorama('origin');
        await browser.clickPanorama(500, 640);
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('after_thoroughfare_move');
    });
});
