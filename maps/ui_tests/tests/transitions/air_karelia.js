describe('air to water: Karelia', function() {
    it('should render properly: Karelia', async function() {
        const browser = this.browser;
        await browser.openPanorama();
        await browser.execute(function() {
            window.engine.gotoPOV('1257712306_606094271_23_1600967356');
        });
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('air_origin_karelia');
        await browser.execute(function() {
            window.engine.gotoPOV('1257733035_606090961_23_1409409632');
        });
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('water_karelia');
    });
});