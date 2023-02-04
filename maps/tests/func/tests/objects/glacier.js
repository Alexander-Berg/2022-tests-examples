describe('Objects -glacier', () => {
    for (let i = 12; i < 20; i++) {
        it(`zoom : ${i}`, async ({browser}) => {
            await browser.openMap({center: [43.346942, 42.452363], zoom: i});
            await browser.verifyScreenshot('zoom-' + i, PO.map());
        });
    }
});
