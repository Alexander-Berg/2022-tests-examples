describe('metro: Markers should visible', function() {
    it('visible and click metro-markers: Kiev', async function() {
        const browser = this.browser;
            await browser.openPanorama();
            await browser.execute(function() {
                window.engine.gotoPOV('1255803674_726149114_23_1434189251');
            });
            await browser.waitUntilPanoramaLoaded();
            await browser.assertViewPanorama('metro_origin_kiev');
            await browser.clickElement('.pano-engine-v2__icon-marker__icon-kieMetroStyle');
            await browser.$('.pano-engine-v2__marker_expanded');
            await browser.assertViewPanorama('metro_markers_description_kiev');
    });
});