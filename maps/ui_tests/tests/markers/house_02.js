describe('house: Markers should visible', function() {
    it('visible and click house-markers: Moscow', async function() {
        const browser = this.browser;
            await browser.openPanorama();
            await browser.execute(function() {
                window.engine.gotoPOV('1298097316_673653590_23_1528096864');
            });
            await browser.waitUntilPanoramaLoaded();
            await browser.assertViewPanorama('house_origin_moscow');
            await browser.clickElement('.pano-engine-v2__marker__name=15к2');
            await browser.$('.pano-engine-v2__marker_expanded');
            await browser.assertViewPanorama('house_markers_description_moscow');
    });
});