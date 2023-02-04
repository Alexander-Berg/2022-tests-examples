describe('Bryusov lane', function() {
    it('should move camera by clicking on circle marker', async function() {
        const browser = this.browser;
        await browser.setViewportSize(1024, 768);
        await browser.openPanorama();
        await browser.execute(function() {
            window.engine.gotoPOV('1298095430_673155343_23_1585898442');
        });
        await browser.waitUntilPanoramaLoaded();
        // await browser.gridView();
        await browser.assertViewPanorama('circle_marker_01');
        await browser.moveToPoint(510, 480);
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('circle_marker_02');
        await browser.clickPanorama(510, 480);
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('circle_marker_03');
    });
});