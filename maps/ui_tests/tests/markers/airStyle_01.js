describe('airoport-Pulkovo: Markers should visible', function() {
    it('visible and click airoport-markers: Pulkovo', async function() {
        const browser = this.browser;
            await browser.openPanorama();
            await browser.execute(function() {
                window.engine.gotoPOV('1254334034_627994628_23_1563784452');
            });
            await browser.waitUntilPanoramaLoaded();
            await browser.assertViewPanorama('airoport_origin_spb');
            await browser.execute(function() {
                window.camera.azimuth -= Math.PI / 4;
                window.camera.flush();
            });
            await browser.waitUntilPanoramaLoaded();
            await browser.clickElement('.pano-engine-v2__icon-marker__icon-airStyle');
            await browser.assertViewPanorama('airoport_markers_description_pulkovo');
    });
});
