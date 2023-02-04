describe('house: Markers should visible', function() {
    it('visible and click house-markers: erevan', async function() {
        const browser = this.browser;
            await browser.openPanorama();
            await browser.execute(function() {
                window.engine.gotoPOV('1338603120_813252668_23_1476513119');
            });
            await browser.waitUntilPanoramaLoaded();
            await browser.assertViewPanorama('house_origin_erevan');
            await browser.clickElement('.pano-engine-v2__marker__name=56');
            await browser.$('.pano-engine-v2__marker_expanded');
            await browser.assertViewPanorama('house_markers_description_erevan');
    });
});