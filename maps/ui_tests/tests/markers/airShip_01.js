describe('airShip-markers: Markers should visible', function() {
    it('visible and click airShip-markers: Murmansk', async function() {
        const browser = this.browser;
            await browser.openPanorama();
            await browser.execute(function() {
                window.engine.gotoPOV('1283253202_496972153_23_1608658401');
            });
            await browser.waitUntilPanoramaLoaded();
            await browser.assertViewPanorama('airShip_origin_murmansk');
            await browser.clickElement('.pano-engine-v2__marker');
            await browser.waitUntilPanoramaLoaded();
            await browser.assertViewPanorama('air_panorama_murmansk_01');
            await browser.execute(function() {
                window.camera.azimuth -= Math.PI / 4;
                window.camera.flush();
            });
            await browser.waitUntilPanoramaLoaded();
            await browser.clickElement('.pano-engine-v2__marker');
            await browser.waitUntilPanoramaLoaded();
            await browser.assertViewPanorama('air_panorama_murmansk_02');
    });
});

