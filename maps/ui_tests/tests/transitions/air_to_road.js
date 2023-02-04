describe('air to road', function() {
    it('Serpukhovskaya zastava', async function() {
        const browser = this.browser;
        await browser.openPanorama();
        await browser.execute(function() {
            window.engine.gotoPOV('1298166417_673672454_23_1532903921');
        });
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('air_origin');
        await browser.execute(function() {
            window.engine.gotoPOV('1298166731_673670898_23_1523887207');
        });
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('road_origin');
    });
});