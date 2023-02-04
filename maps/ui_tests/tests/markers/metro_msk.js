describe('metro: Markers should visible', function() {
    it('visible and click metro-markers: Moscow', async function() {
        const browser = this.browser;
            await browser.openPanorama();
            await browser.execute(function() {
                window.engine.gotoPOV('1298061280_674707216_23_1531553530');
            });
            await browser.waitUntilPanoramaLoaded()
            await browser.assertViewPanorama('metro_origin_moscow')
            await browser.clickElement('.pano-engine-v2__icon-marker__icon-mskMetroStyle');
            await browser.$('.pano-engine-v2__marker_expanded');
            await browser.assertViewPanorama('metro_markers_description_msk');
    });
});
