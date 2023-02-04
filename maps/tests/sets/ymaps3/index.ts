describe('YMaps3', () => {
    describe('Graphics', () => {
        it('Feature display', async function () {
            await this.browser.openYMaps3('main');
            await this.browser.waitAndVerifyScreenshot('#map-element', 'main', {ignoreFonts: true});
        });

        it('Multigeometries', async function () {
            await this.browser.openYMaps3('graphics-multigeometries');
            await this.browser.waitAndVerifyScreenshot('#map-element', 'graphics-multigeometries', {ignoreFonts: true});
        });

        it('Redraw after zoom and center changed', async function () {
            await this.browser.openYMaps3('graphics-perf');
            await this.browser.click('#test-drag');
            await this.browser.waitAndVerifyScreenshot('#map-element', 'graphics-display-after-zoo-and-center', {
                ignoreFonts: true
            });
        });
    });
});
