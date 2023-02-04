describe('Dnipro River', function() {
    it('should move camera by clicking on circle marker', async function() {
        const browser = this.browser;
        await browser.setViewportSize(1024, 768);
        await browser.openPanorama();
        await browser.execute(function() {
            window.engine.gotoPOV('1256096482_726002730_23_1470724791');
        });
        await browser.waitUntilPanoramaLoaded();
        // await browser.gridView();
        await browser.assertViewPanorama('circle_on_river_01');
        await browser.moveToPoint(470, 430);
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('circle_on_river_02');
        await browser.clickPanorama(470, 430);
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('circle_on_river_03');
    });
});