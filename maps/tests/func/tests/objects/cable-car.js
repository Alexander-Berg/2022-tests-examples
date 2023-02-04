describe('Objects -cable car', () => {
    for (let i = 12; i < 20; i++) {
        it(`zoom : ${i}`, async ({browser}) => {
            await browser.openMap({center: [43.633796, 40.320904], zoom: i});
            await browser.verifyScreenshot('zoom-' + i, PO.map());
        });
    }
});
