describe('railway-station-Moscow: Markers should visible', function() {
    it('visible and click railway-station-markers: Moscow', async function() {
        const browser = this.browser;
            await browser.openPanorama();
            await browser.execute(function() {
                window.engine.gotoPOV('1298232784_672813899_23_1586168883');
            });
            await browser.waitUntilPanoramaLoaded()
            await browser.assertViewPanorama('railway-station_origin_moscow')
            await browser.execute(function() {
                window.camera.azimuth -= Math.PI / 4;
                window.camera.flush();
            });
            await browser.waitUntilPanoramaLoaded();
            await browser.clickElement('.pano-engine-v2__icon-marker__icon-railStyle');
            await browser.$('.pano-engine-v2__marker_expanded');
            await browser.assertViewPanorama('railway-station_markers_description_moscow');
    });
});
