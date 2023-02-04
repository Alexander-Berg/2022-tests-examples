describe('airoport-Sheremetievo: Markers should visible', function() {
    it('visible and click airoport-markers: Sheremetievo',async function() {
        const browser = this.browser;
            await browser.openPanorama();
            await browser.execute(function() {
                window.engine.gotoPOV('1296934642_670972687_23_1533453907');
            });
            await browser.waitUntilPanoramaLoaded();
            await browser.assertViewPanorama('airoport_origin_moscow');
            await browser.execute(function() {
                window.camera.azimuth -= Math.PI / 4;
                window.camera.flush();
            });
            await browser.waitUntilPanoramaLoaded();
            await browser.clickElement('.pano-engine-v2__icon-marker__icon-airStyle');
            await browser.assertViewPanorama('airoport_markers_description_sheremetievo');
    });
});
