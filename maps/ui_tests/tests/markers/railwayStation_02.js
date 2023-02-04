describe('railway-station-Kazan: Markers should visible', function() {
    it('visible and click railway-station-markers: Kazan', async function() {
        const browser = this.browser;
            await browser.openPanorama();
            await browser.execute(function() {
                window.engine.gotoPOV('1366636982_672856215_23_1591627452');
            });
            await browser.waitUntilPanoramaLoaded()
            await browser.assertViewPanorama('railway-station_origin_kazan')
            await browser.execute(function() {
                window.camera.azimuth += Math.PI / 4;
                window.camera.flush();
            });
            await browser.waitUntilPanoramaLoaded();
            await browser.clickElement('.pano-engine-v2__icon-marker__icon-railStyle');
            await browser.$('.pano-engine-v2__marker_expanded');
            await browser.assertViewPanorama('railway-station_markers_description_kazan');
    });
});
