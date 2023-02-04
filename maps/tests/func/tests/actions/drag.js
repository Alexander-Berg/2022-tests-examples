describe('Actions -drag', () => {
    hermione.enable.in(['chrome_50']);
    it('drag map', async ({browser}) => {
        await browser.setWindowSize(600, 600);
        await browser.openMap({filename: 'full-viewport', center: [55.805564, 37.511076], zoom: 16});
        await browser.verifyScreenshot('before-drag', PO.map());
        await browser.deleteTilesLoaded();
        await browser.mouseDrag([100, 100], [400, 400]);
        await browser.waitForTilesLoaded();
        await browser.verifyNoErrors();
        await browser.verifyScreenshot('after-drag-1', PO.map());
        await browser.deleteTilesLoaded();
        await browser.mouseDrag([100, 100], [400, 450]);
        await browser.waitForTilesLoaded();
        await browser.verifyScreenshot('after-drag-2', PO.map());
        await browser.deleteTilesLoaded();
        await browser.mouseDrag([100, 400], [400, 100], 600);
        await browser.waitForTilesLoaded();
        await browser.verifyScreenshot('after-drag-3', PO.map());
    });
});
