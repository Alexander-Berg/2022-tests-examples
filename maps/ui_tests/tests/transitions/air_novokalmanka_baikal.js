describe('air to water: Novokalmanka-Baikal', function() {
    it('should render properly: Novokalmanka-Baikal', async function() {
        const browser = this.browser;
        await browser.openPanorama();
        await browser.execute(function() {
            window.engine.gotoPOV('1570849208_712054593_23_1591947990');
        });
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('air_Novokalmanka');
        await browser.execute(function() {
            window.engine.gotoPOV('1693573441_713719432_23_1404453031');
        });
        await browser.waitUntilPanoramaLoaded();
        await browser.assertViewPanorama('water_Baikal');
    });
});