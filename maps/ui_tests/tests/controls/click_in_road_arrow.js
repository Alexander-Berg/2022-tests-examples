describe('tverskaya street', function() {
    it('should move camera by clicking thoroughfare pointers', async function() {
        const browser = this.browser;
        await browser.setViewportSize(1024, 768);
        await browser.openPanorama();
        await browser.execute(function() {
            window.engine.goto(37.6094360342, 55.7618210572963);
        });
        await browser.waitUntilPanoramaLoaded();
        // await browser.gridView();
        await browser.assertViewPanorama('origin');
        await browser.clickPanorama(515, 715);
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('after_thoroughfare_move_01');
        await browser.clickPanorama(585, 670);
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('after_thoroughfare_move_02');
    });
});
